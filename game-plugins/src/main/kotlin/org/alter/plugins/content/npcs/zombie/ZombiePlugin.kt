package org.alter.plugins.content.npcs.zombie

import org.alter.api.ext.inWilderness
import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.items.jewellery.RingOfWealth
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.rscm.RSCM.getRSCM

/**
 * Makes zombies aggressive, worth Slayer experience, and worth killing.
 *
 * Stats live in `data/cfg/npcs/monsterStats.json`, tables in [ZombieDrops], variants and tiles in
 * [Zombies]; this file is the wiring. [ZombieSpawnPlugin] puts them on the map.
 *
 * ## What this actually changes, per zombie
 *
 * Four fields are layered onto the def the engine already built - the pattern
 * `areas/wilderness/bosses/WildernessBossPlugin` documents, and see [Zombies] for why a
 * `setCombatDef` would have been the wrong tool:
 *
 * - **`respawnDelay`** 25 -> [Zombies.RESPAWN_CYCLES]. Read by `NpcDeathAction` at death, so
 *   patching it at spawn is enough.
 * - **`aggressiveRadius`** and **`aggroTargetDelay`**, without which `NpcAggroPlugin` never arms
 *   its timer at all. Every zombie in the game is `aggressive = Yes`.
 * - **`aggressiveTimer`**, which had to be stated because `NpcCombatDef.DEFAULT` leaves it at 0 and
 *   a zero timer reads as "never aggressive". See [Zombies.AGGRO_TIMER].
 * - **`slayerXp`**, which `Slayer.onKill` reads off the dying npc and is the only place Slayer
 *   experience comes from. Zeroed before this, so a zombie task could be assigned and completed
 *   for no experience at all.
 *
 * The patch is applied in an `onNpcSpawn` hook rather than once at load because a zombie that dies
 * is re-defaulted: `NpcDeathAction` calls `World.setNpcDefaults` and then `executeNpcSpawn` again,
 * which would otherwise hand the respawned zombie back its unpatched def. Per-npc spawn hooks run
 * before global ones, so both `NpcAggroPlugin` and
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] see the patched version.
 *
 * ## Drops
 *
 * Rolled here rather than through the combat DSL's `drops { }` block, for the reason every monster
 * package in this tree repeats: that block builds a `LootTable` which `NpcDeathAction` never rolls,
 * so a zombie killed before this file existed dropped literally nothing - not even the guaranteed
 * bones.
 *
 * ## The Slayer task
 *
 * `data/cfg/slayer/tasks.json`'s `Zombies` category shipped with an **empty** monster list, which
 * `SlayerService.markAvailable` reads as "not assignable" - so Turael and Mazchna could never hand
 * the task out despite both carrying it in their assignment tables. It now names the monsters the
 * wiki's `Slayer task/Zombies` page lists, and the ids resolve by cache **name**, so every zombie
 * variant in the cache counts and not just the ones this package spawns.
 *
 * Two names on that page are deliberately left out of the list: **Undead chicken**, already claimed
 * by `Birds`, and **Undead cow**, already claimed by `Cows`. `SlayerService` maps an npc id to a
 * task with `putIfAbsent`, so listing either here would be a silent no-op decided by file order
 * rather than a second task actually counting them.
 */
class ZombiePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Zombies.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                onNpcSpawn(npcKey) {
                    npc.combatDef =
                        npc.combatDef.copy(
                            respawnDelay = Zombies.RESPAWN_CYCLES,
                            aggressiveRadius = Zombies.AGGRO_RADIUS,
                            aggroTargetDelay = Zombies.AGGRO_SEARCH_DELAY,
                            aggressiveTimer = Zombies.AGGRO_TIMER,
                            slayerXp = variant.slayerXp,
                        )
                }

                onNpcDeath(npcKey) { onDeath(npc, variant) }
            }
        }
    }

    /**
     * Guaranteed bones, then one roll on the variant's table, then the two tertiaries - each
     * independent of the table and of each other.
     */
    private fun onDeath(
        npc: Npc,
        variant: ZombieVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)

        ZombieDrops.tableFor(variant.dropTable)?.let { table ->
            ZombieDrops.roll(table, world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }
        }

        if (world.chance(1, ZombieDrops.CHAMPION_SCROLL_ONE_IN)) {
            loot.add(getRSCM("item.zombie_champion_scroll") to 1)
        }

        if (killer.inWilderness() && world.chance(1, ZombieDrops.LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }
}
