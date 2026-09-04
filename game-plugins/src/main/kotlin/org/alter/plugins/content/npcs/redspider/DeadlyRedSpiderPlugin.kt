package org.alter.plugins.content.npcs.redspider

import org.alter.api.ext.inWilderness
import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.plugins.content.npcs.SpawnFacings
import org.alter.rscm.RSCM.getRSCM

/**
 * The deadly red spiders - one id, seven locations, 60 pins.
 *
 * One plugin rather than three files: one npc, no weighted table at all, and two tertiaries.
 * Stats come from `data/cfg/npcs/monsterStats.json` (35 hitpoints, 30/25/30, 40% Fire weakness) and
 * animations resolve on their own to the `GIANT_SPIDER` rig, 5327 / 5328 / 5329 - the second
 * bestiary audit checked that and found nothing to pin.
 *
 * ## It drops almost nothing, and that is the page
 *
 * Its `Drops` section has **no `100%` row and no weighted table** - just two tertiaries. A deadly
 * red spider is killed for Slayer experience and for the red spiders' eggs that spawn on the ground
 * near it, not for its loot, and the page reflects that. Nothing is missing here.
 *
 * `data/cfg/slayer/tasks.json` already names `Deadly red spider` in its `Spiders` category, so the
 * task was assignable and awarded **nothing** before this file existed: `Slayer.onKill` reads
 * `slayerXp` off the dying npc and there is nowhere else it comes from.
 *
 * ## Despite the name, it is not poisonous
 *
 * `poisonous = No` on the infobox. `content/npcs/dungeon` wires the *poison* spider and the *poison*
 * scorpion with real `poisonDamage`; this one gets none, because the page gives it none.
 */
class DeadlyRedSpiderPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onNpcSpawn(NPC_KEY) {
            npc.combatDef =
                npc.combatDef.copy(
                    respawnDelay = RESPAWN_CYCLES,
                    aggressiveRadius = AGGRO_RADIUS,
                    aggroTargetDelay = AGGRO_SEARCH_DELAY,
                    aggressiveTimer = AGGRO_TIMER,
                    slayerXp = SLAYER_XP,
                )
        }

        onNpcDeath(NPC_KEY) { onDeath(npc) }

        var index = 0
        CAMPS.forEach { (_, plane, tiles) ->
            tiles.forEach { (x, z) ->
                spawnNpc(
                    npc = NPC_KEY,
                    x = x,
                    z = z,
                    height = plane,
                    walkRadius = WALK_RADIUS,
                    direction = SpawnFacings.at(index++),
                )
            }
        }
    }

    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world
        val loot = mutableListOf<Pair<Int, Int>>()

        // "Looting bags are only dropped by those found in the Wilderness" - gated on where the
        // killer stands, matching every other position-gated drop in this tree. Two of these seven
        // locations are Wilderness, so the test really discriminates.
        if (killer.inWilderness() && world.chance(1, LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }
        if (world.chance(1, BEGINNER_CLUE_ONE_IN)) {
            loot.add(getRSCM("item.clue_scroll_beginner") to 1)
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }

    private companion object {
        const val NPC_KEY = "npc.deadly_red_spider"

        const val COMBAT_LEVEL = 34

        /** Wiki `respawn = 30`, in game ticks. */
        const val RESPAWN_CYCLES = 30

        /** Wiki `slayxp = 35`. */
        const val SLAYER_XP = 35.0

        const val AGGRO_RADIUS = 4

        const val AGGRO_SEARCH_DELAY = 4

        /**
         * The engine's own `DEFAULT_AGGRO_TIMER`, stated because a def built from
         * `monsterStats.json` starts with a **0** timer, which `NpcAggroPlugin` reads as "stop being
         * aggressive".
         */
        const val AGGRO_TIMER = 1000

        const val WALK_RADIUS = 4

        const val LOOTING_BAG_ONE_IN = 4

        const val BEGINNER_CLUE_ONE_IN = 128

        /**
         * Seven of the nine published `LocLine`s.
         *
         * **Not here**: `Arandar during Song of the Elves`, an instanced copy of the Arandar line
         * that is already placed; and `Pandemonium Cave`, whose `LocLine` publishes no coordinates
         * at all.
         */
        val CAMPS: List<Triple<String, Int, List<Pair<Int, Int>>>> =
            listOf(
                Triple(
                    "North of the Lava Maze",
                    0,
                    listOf(
                        3046 to 3882, 3046 to 3884, 3047 to 3874, 3047 to 3876, 3047 to 3890,
                        3047 to 3892, 3049 to 3896, 3050 to 3871, 3052 to 3871, 3052 to 3896,
                        3058 to 3897, 3061 to 3897, 3067 to 3896, 3070 to 3896,
                    ),
                ),
                Triple(
                    "Eastern Ruins",
                    0,
                    listOf(
                        3145 to 3730, 3149 to 3734, 3150 to 3744, 3157 to 3740, 3164 to 3734,
                        3168 to 3736, 3170 to 3739, 3174 to 3733, 3178 to 3737, 3156 to 3737,
                        3167 to 3728,
                    ),
                ),
                Triple(
                    "Crandor and Karamja Dungeon",
                    0,
                    listOf(2834 to 9580, 2834 to 9584, 2836 to 9576, 2838 to 9582, 2842 to 9583),
                ),
                Triple(
                    "Varrock Sewers",
                    0,
                    listOf(
                        3171 to 9884, 3174 to 9891, 3175 to 9881, 3176 to 9883, 3178 to 9880,
                        3178 to 9888, 3180 to 9883, 3183 to 9885, 3186 to 9892,
                    ),
                ),
                Triple(
                    "Edgeville Dungeon",
                    0,
                    listOf(
                        3118 to 9950, 3118 to 9956, 3120 to 9952, 3122 to 9955, 3124 to 9951,
                        3125 to 9958, 3126 to 9948, 3127 to 9956, 3128 to 9954,
                    ),
                ),
                Triple(
                    "Arandar",
                    0,
                    listOf(
                        2337 to 3311, 2337 to 3314, 2338 to 3310, 2341 to 3309, 2342 to 3311,
                        2343 to 3309, 2344 to 3318, 2345 to 3310, 2345 to 3312, 2350 to 3310,
                    ),
                ),
                Triple("Myreque Hideout", 0, listOf(3466 to 9815, 3471 to 9806)),
            )
    }
}
