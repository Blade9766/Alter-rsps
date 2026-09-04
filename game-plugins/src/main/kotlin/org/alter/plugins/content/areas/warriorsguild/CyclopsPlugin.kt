package org.alter.plugins.content.areas.warriorsguild

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.rscm.RSCM.getRSCM

/**
 * The guild's cyclopes, and the defender drops that are the only reason anyone fights them.
 *
 * ## Stats
 *
 * All three combat levels share one stat block - the wiki lists 56, 76 and 106 with identical
 * hitpoints, Attack, Strength and Defence, differing only in the level shown. 75 hitpoints,
 * 47/50/26, four-tick crush attacks, aggressive, and every defence bonus at 0.
 *
 * The published max hit of 6 is what pins the strength bonus at 0: with a Strength level of 50 the
 * engine's own formula gives `floor(0.5 + 59 * 64 / 640) = 6` exactly, so no bonus is needed and
 * adding one would overshoot.
 *
 * ## Animations
 *
 * Every one of the twelve cyclops ids observes exactly `{4651, 4652, 4653}` in this project's
 * `openosrs-animations.json`, and that set is exactly the `GIANT` entry of
 * `named-combat-media.json` - cyclopes ride the giant rig. The assignment is taken from that named
 * entry (attack 4652, block 4651, death 4653) rather than from the observed list's order, which
 * varies id to id and carries no meaning. Sounds come from the same entry.
 *
 * ## Spawns
 *
 * The eleven top-floor tiles are the wiki's own `{{LocLine}}` pins, minus one at (2842, 3550) that
 * sits *west* of Kamfreena's doors - outside the gated room - and would have put a cyclops where
 * players walk in unarmed.
 *
 * The basement tiles are **this project's placement**, not the wiki's. See
 * [WarriorsGuild.BASEMENT_CYCLOPS] for why the published coordinates could not be used: they point
 * at a region this cache has no map data for at all.
 */
class CyclopsPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        (TOP_FLOOR_IDS + BASEMENT_IDS).forEach { npcKey ->
            setCombatDef(npcKey) {
                configs {
                    attackSpeed = ATTACK_SPEED
                    respawnDelay = RESPAWN_CYCLES
                }
                aggro {
                    radius = AGGRO_RADIUS
                    searchDelay = AGGRO_SEARCH_DELAY
                }
                stats {
                    hitpoints = HITPOINTS
                    attack = ATTACK_LEVEL
                    strength = STRENGTH_LEVEL
                    defence = DEFENCE_LEVEL
                    magic = 1
                    ranged = 1
                }
                bonuses {
                    attackBonus = 0
                    strengthBonus = 0
                    defenceStab = 0
                    defenceSlash = 0
                    defenceCrush = 0
                    defenceMagic = 0
                    defenceRanged = 0
                }
                anims {
                    attack = ATTACK_ANIM
                    block = BLOCK_ANIM
                    death = DEATH_ANIM
                }
                sound {
                    attackSound = ATTACK_SOUND
                    blockSound = BLOCK_SOUND
                    deathSound = DEATH_SOUND
                }
            }

            // The engine never copies a style out of the combat def, so it is set on spawn - the
            // same reason `content/npcs/dungeon` and `content/npcs/critters` do it here.
            onNpcSpawn(npc = npcKey) { npc.combatStyle = CombatStyle.CRUSH }

            onNpcDeath(npcKey) { onDeath(npc, basement = npcKey in BASEMENT_IDS) }
        }

        TOP_FLOOR_SPAWNS.forEachIndexed { index, tile ->
            // Alternating the ids spreads the two combat levels through the room the way the
            // wiki's own pin list does, rather than filling it with one variant.
            spawnNpc(npc = TOP_FLOOR_IDS[index % TOP_FLOOR_IDS.size], tile = tile, walkRadius = WALK_RADIUS)
        }

        BASEMENT_SPAWNS.forEachIndexed { index, tile ->
            spawnNpc(npc = BASEMENT_IDS[index % BASEMENT_IDS.size], tile = tile, walkRadius = WALK_RADIUS)
        }
    }

    /**
     * Big bones always, then the ladder roll.
     *
     * The defender is a **pre-roll**, not a row in a table: the wiki quotes it at its own rate
     * alongside everything else, so a kill can produce a defender and the kill's ordinary drop
     * both. Only the next rung is ever rolled - see [DefenderLadder] for the leave-and-re-enter
     * rule that gates it.
     *
     * The rest of the cyclops table - coins, the odd steel chainbody, the clue scroll - is not
     * built. It is a long table of low-value rows, and the guild's whole point is the defenders.
     */
    private fun onDeath(
        npc: Npc,
        basement: Boolean,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world
        val loot = mutableListOf(getRSCM(BIG_BONES) to 1)

        val defender = rollDefender(killer, world.randomDouble(), basement)
        if (defender != null) {
            loot.add(getRSCM(defender) to 1)
            DefenderLadder.award(killer)
            killer.message("The cyclops drops a defender!")
            if (!basement) {
                killer.message("You will need to leave the room and come back for the next one.")
            }
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }

    /**
     * The defender this kill drops, or null.
     *
     * Split out from [onDeath] so the roll is one expression with the random value passed in - the
     * ladder rule is fiddly enough that it is worth being able to read it on its own.
     */
    private fun rollDefender(
        player: Player,
        roll: Double,
        basement: Boolean,
    ): String? {
        if (basement) {
            /*
             * The basement drops only the dragon defender, and only to someone who already has the
             * rune one - which is also what Lorelai's door checks, so this is belt and braces
             * rather than the only guard.
             */
            if (!DefenderLadder.hasRuneDefender(player)) {
                return null
            }
            return DefenderLadder.DRAGON_DEFENDER.takeIf { roll < DefenderLadder.DRAGON_CHANCE }
        }

        if (!DefenderLadder.canReceive(player)) {
            return null
        }
        return DefenderLadder.nextRung(player)?.takeIf { roll < DefenderLadder.RUNG_CHANCE }
    }

    private companion object {
        /** Levels 56 and 76, interleaved as the wiki lists them. */
        val TOP_FLOOR_IDS =
            listOf(
                "npc.cyclops_2463", "npc.cyclops_2464", "npc.cyclops_2465",
                "npc.cyclops_2466", "npc.cyclops_2467", "npc.cyclops_2468",
            )

        /** The level 106 basement variants. */
        val BASEMENT_IDS =
            listOf(
                "npc.cyclops_2137", "npc.cyclops_2138", "npc.cyclops_2139",
                "npc.cyclops_2140", "npc.cyclops_2141", "npc.cyclops_2142",
            )

        const val HITPOINTS = 75
        const val ATTACK_LEVEL = 47
        const val STRENGTH_LEVEL = 50
        const val DEFENCE_LEVEL = 26
        const val ATTACK_SPEED = 4

        /** 30 seconds, the value every other monster package in this project uses. */
        const val RESPAWN_CYCLES = 50

        const val AGGRO_RADIUS = 5
        const val AGGRO_SEARCH_DELAY = 4

        /** Cyclopes are 2x2; a small radius keeps them in the room without them clumping. */
        const val WALK_RADIUS = 3

        // The GIANT rig - see this class's comment.
        const val ATTACK_ANIM = 4652
        const val BLOCK_ANIM = 4651
        const val DEATH_ANIM = 4653
        const val ATTACK_SOUND = 448
        const val BLOCK_SOUND = 451
        const val DEATH_SOUND = 450

        const val BIG_BONES = "item.big_bones"

        /** The wiki's `{{LocLine}}` pins for plane 2, minus the one outside Kamfreena's doors. */
        val TOP_FLOOR_SPAWNS =
            listOf(
                Tile(2848, 3551, 2),
                Tile(2851, 3535, 2),
                Tile(2851, 3540, 2),
                Tile(2854, 3542, 2),
                Tile(2855, 3535, 2),
                Tile(2858, 3550, 2),
                Tile(2862, 3550, 2),
                Tile(2866, 3544, 2),
                Tile(2868, 3552, 2),
                Tile(2870, 3541, 2),
                Tile(2872, 3552, 2),
            )

        /**
         * This project's placement, spread across the room east of Lorelai's door.
         *
         * Twelve rather than the wiki's twenty-three: every one of these tiles is chosen here
         * rather than sourced, and inventing twelve is less wrong than inventing twenty-three.
         */
        val BASEMENT_SPAWNS =
            listOf(
                Tile(2916, 9960, 0),
                Tile(2916, 9968, 0),
                Tile(2916, 9976, 0),
                Tile(2922, 9962, 0),
                Tile(2922, 9970, 0),
                Tile(2922, 9978, 0),
                Tile(2928, 9960, 0),
                Tile(2928, 9968, 0),
                Tile(2928, 9976, 0),
                Tile(2934, 9962, 0),
                Tile(2934, 9970, 0),
                Tile(2934, 9978, 0),
            )
    }
}
