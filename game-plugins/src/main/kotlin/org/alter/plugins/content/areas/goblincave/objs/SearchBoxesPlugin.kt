package org.alter.plugins.content.areas.goblincave.objs

import org.alter.api.cfg.Animation
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.EntityType
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.getCombatTarget
import org.alter.plugins.content.combat.isBeingAttacked
import org.alter.plugins.content.npcs.goblin.Goblins
import org.alter.rscm.RSCM.getRSCM

/**
 * The boxes and crates of the Goblin Cave, and the trouble searching them causes.
 *
 * Three pieces of scenery share one behaviour and one wiki description: Boxes (15598),
 * Crate (15599) and Crates (15600). All are `Search`-able, all can yield an uncut red
 * topaz at 1/1000, all can spawn an aggressive critter, and all anger the goblins. They
 * are wired together here rather than split per id because the wiki gives them the same
 * text word for word.
 *
 * These are **map scenery, already in the cache** - unlike the goblins there is nothing to
 * spawn, only an option to bind. The wiki publishes their tiles; this file does not repeat
 * them, since binding by object id covers every instance wherever it stands.
 *
 * **What the search does**, in the wiki's own order:
 * 1. **Uncut red topaz, 1/1000.** The only published rarity on either page, added in the
 *    14 June 2023 update. Goes to the inventory, or to the ground if it is full.
 * 2. **A chicken, rat or spider may spawn, aggressive to the searcher.** Note the ids:
 *    the level 1 Spider is **3019**, not `npc.spider` (2478), which is the Stronghold of
 *    Security spider at combat level 24 - spawning that one on a low-level player
 *    searching a box would be a nasty surprise. Chicken and Rat use their level 1 regular
 *    variants (1173/1174 and 2854/2855).
 * 3. **A nearby goblin turns on the searcher, every time.** The two wiki pages disagree
 *    slightly on scale: the scenery pages say "Nearby Goblins will also become aggressive",
 *    while the Goblin page is more specific - "one random nearby goblin being selected to
 *    become aggressive every time the boxes are searched". The specific wording wins here,
 *    so one goblin comes at you per search rather than the whole room. This is also the
 *    kinder reading: the cave holds 24 goblins and this server has no single-way combat
 *    enforcement (see `content/npcs/darkwizard`'s notes), so "all nearby" would mean a
 *    genuine dogpile.
 *
 * **The critter chance is not published anywhere.** The pages say only that there "is a
 * chance" and that "searching the same box repeatedly can cause the critters to spawn",
 * which reads as common rather than rare. [CRITTER_CHANCE] is an approximation, flagged
 * as such - the same treatment the goblin drop tables give the energy potions.
 *
 * **Spawned critters are temporary.** They do not respawn ([Npc.respawns] defaults to
 * false, so killing one removes it for good) and they despawn on their own after roughly
 * a minute if nobody is fighting them, re-arming the timer while combat is live so one
 * cannot vanish mid-fight. Without that, a repeatable object option would be an unbounded
 * npc factory - a player could stand at one crate and fill the cave.
 *
 * The critters' own stats, animations and drops live in `content/npcs/critters`, which
 * exists because of this file: before it they fought with `NpcCombatDef.DEFAULT`'s 10
 * hitpoints and zeroed stats, which made a box-spawned rat tougher than the goblin next
 * to it.
 */
class SearchBoxesPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    private val goblinIds: Set<Int> = Goblins.ALL_IDS.map { getRSCM(it) }.toSet()

    init {
        SEARCHABLES.forEach { obj ->
            onObjOption(obj = obj, option = "search") {
                val name = player.getInteractingGameObj().getDef().name?.lowercase() ?: "boxes"
                player.queue { search(this, player, name) }
            }
        }

        onTimer(CRITTER_DESPAWN) {
            val critter = npc
            if (critter.isBeingAttacked() || critter.getCombatTarget() != null) {
                critter.timers[CRITTER_DESPAWN] = DESPAWN_CYCLES
            } else {
                world.remove(critter)
            }
        }
    }

    private suspend fun search(
        task: QueueTask,
        p: Player,
        obj: String,
    ) {
        p.lock()
        p.message("You search the $obj...")
        p.animate(Animation.BEND_DOWN)
        task.wait(3)
        p.unlock()

        if (world.randomDouble() <= TOPAZ_CHANCE) {
            val topaz = getRSCM("item.uncut_red_topaz")
            if (p.inventory.add(item = "item.uncut_red_topaz").hasFailed()) {
                world.spawn(GroundItem(item = topaz, amount = 1, tile = p.tile, owner = p))
            }
            p.message("You find an uncut red topaz among the contents.")
        } else {
            p.message("You find nothing of interest.")
        }

        if (world.randomDouble() <= CRITTER_CHANCE) {
            spawnCritter(p)
        }

        angerNearbyGoblin(p)
    }

    /**
     * Spawns one critter on the searcher's own tile - guaranteed walkable, since the
     * player is standing on it - and sets it straight onto them.
     */
    private fun spawnCritter(p: Player) {
        val critter = Npc(getRSCM(CRITTERS.random()), Tile(p.tile.x, p.tile.z, p.tile.height), world)
        if (!world.spawn(critter)) {
            return
        }
        critter.timers[CRITTER_DESPAWN] = DESPAWN_CYCLES
        critter.attack(p)
    }

    /**
     * Picks one goblin within [GOBLIN_ANGER_RADIUS] of the searcher and sets it on them.
     * Goblins already fighting somebody are skipped so a search cannot yank one off
     * another player mid-fight.
     */
    private fun angerNearbyGoblin(p: Player) {
        val candidates = mutableListOf<Npc>()
        for (x in -GOBLIN_ANGER_RADIUS..GOBLIN_ANGER_RADIUS) {
            for (z in -GOBLIN_ANGER_RADIUS..GOBLIN_ANGER_RADIUS) {
                val tile = p.tile.transform(x, z)
                val chunk = world.chunks.get(tile, createIfNeeded = false) ?: continue
                chunk.getEntities<Npc>(tile, EntityType.NPC)
                    .filterTo(candidates) { it.id in goblinIds && !it.isBeingAttacked() && it.getCombatTarget() == null }
            }
        }
        candidates.randomOrNull()?.attack(p)
    }

    private companion object {
        val SEARCHABLES = listOf("object.boxes_15598", "object.crate_15599", "object.crates_15600")

        /**
         * Level 1 regular variants only. `npc.spider` (2478) is deliberately not in here -
         * see this class's doc comment.
         */
        val CRITTERS =
            listOf(
                "npc.chicken_1173",
                "npc.chicken_1174",
                "npc.rat_2854",
                "npc.rat_2855",
                "npc.spider_3019",
            )

        /** The wiki's published rarity for both the boxes and the crates. */
        const val TOPAZ_CHANCE = 1.0 / 1000.0

        /** Unpublished - an approximation. See this class's doc comment. */
        const val CRITTER_CHANCE = 1.0 / 5.0

        /** Matches the goblins' own walk radius, so "nearby" means "could have wandered here". */
        const val GOBLIN_ANGER_RADIUS = 8

        /** Roughly a minute at 0.6s per cycle. */
        const val DESPAWN_CYCLES = 100

        val CRITTER_DESPAWN = TimerKey()
    }
}
