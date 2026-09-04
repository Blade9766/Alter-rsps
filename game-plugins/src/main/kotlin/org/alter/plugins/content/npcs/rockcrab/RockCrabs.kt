package org.alter.plugins.content.npcs.rockcrab

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The Rock Crabs of Rellekka, Waterbirth and Mount Quidamortem - 103 of them, and the reason
 * anybody's first Slayer task is bearable.
 *
 * See [RockCrabPlugin] for the wiring and [RockCrabSpawnPlugin] for the placement. Stats come from
 * `data/cfg/npcs/monsterStats.json` - 50 hitpoints on 1/1/1 combat stats, which is the whole joke of
 * the monster - and animations from the existing `ROCK_CRAB` entry in `named-combat-media.json`,
 * 1312 / 1313 / 1314, which the second bestiary audit checked and left alone.
 *
 * ## The dormant `Rocks` half is not modelled, and this is what that means
 *
 * The page publishes two versions: `Active` (ids 100 and 102, the crab) and `Hidden` (ids 101 and
 * 103, named `Rocks` in the cache, which is what a crab looks like until you walk past it).
 *
 * Only the active ones are spawned. The transform needs an npc to change its id in place, and
 * `Npc.id` is a `val` - there is no transmogrify path in this engine at all, and building one
 * touches npc synchronisation, respawn and the death handler rather than this package. So a crab
 * here is a crab from the moment it spawns.
 *
 * What that costs, stated plainly: the crabs are visible and attackable from across the beach
 * instead of waking as you reach them. What it does not cost is the behaviour that matters - they
 * are aggressive, so walking into one still gets you attacked, which is how the fight starts either
 * way.
 *
 * The `Rocks` ids also have **no row in `monsterStats.json`** and only a single observed animation,
 * so the animation resolver bails on them and leaves them on the human fallback set. That is
 * recorded in `npc-animations/README.md` rather than fixed, because a rock never fights.
 */
internal object RockCrabs {
    private fun drop(
        item: String,
        min: Int = 1,
        max: Int = min,
        weight: Int,
    ) = WeightedDrop(getRSCM(item), min, max, weight)

    private fun coins(
        min: Int,
        weight: Int,
    ) = WeightedDrop(getRSCM("item.coins_995"), min, min, weight)

    /** `id1` - the `Active` version, the only one spawned. */
    val NPC_KEYS = listOf("npc.rock_crab", "npc.rock_crab_102")

    /** `id2` - the `Hidden` version. Named here so the omission above is checkable, not spawned. */
    val DORMANT_KEYS = listOf("npc.rocks", "npc.rocks_103")

    const val COMBAT_LEVEL = 13

    /** Wiki `respawn = 50`, in game ticks, which are this engine's cycles one-for-one. */
    const val RESPAWN_CYCLES = 50

    /** Wiki `slayxp = 50` - the same as its hitpoints, and generous for a level 13. */
    const val SLAYER_XP = 50.0

    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a crab stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`, stated
     * because a def built from `monsterStats.json` starts with a **0** timer, which
     * `NpcAggroPlugin` reads as "stop being aggressive".
     *
     * Left at the ordinary timer rather than `alwaysAggro()`, and that is the right call for this
     * monster in particular: the default `p.combatLevel <= npcLvl * 2` check is exactly the real
     * rule that makes crabs stop attacking a player above combat level 26, which is the whole reason
     * people run past them to reset aggression.
     */
    const val AGGRO_TIMER = 1000

    const val WALK_RADIUS = 4

    /** Wiki `DropsLineClue|type=easy`. */
    const val EASY_CLUE_ONE_IN = 128

    /**
     * The Rock Crab table - rows 127, gem 1, summing to the published 128.
     *
     * There is **no `100%` section**: a rock crab leaves no bones, which is right for something made
     * of shell.
     *
     * **The elite clue scroll and its reward casket** are published as `Always` with the note that
     * they come from completing an elite clue step that asks you to kill one. That is a clue
     * condition rather than a rate, and there is no Treasure Trails step system here - handing them
     * out unconditionally would give an elite casket per crab. Dropped, the same call
     * `content/npcs/hellhound` makes about the identical rows.
     */
    val TABLE =
        MonsterDropTable(
            denominator = 128,
            gemWeight = 1,
            rows =
                listOf(
                    // Weapons - 11.
                    drop("item.bronze_pickaxe", weight = 6),
                    drop("item.iron_pickaxe", weight = 5),
                    // Seaweed - 12, a section of its own on this page.
                    drop("item.seaweed", weight = 4),
                    drop("item.seaweed", 2, weight = 4),
                    drop("item.seaweed", 5, weight = 2),
                    drop("item.edible_seaweed", 2, weight = 2),
                    // Ores - 10.
                    drop("item.tin_ore", 3, weight = 4),
                    drop("item.iron_ore", weight = 2),
                    drop("item.coal", 2, weight = 2),
                    drop("item.copper_ore", 3, weight = 2),
                    // Oyster - 26.
                    drop("item.oyster", 2, weight = 12),
                    drop("item.oyster", weight = 9),
                    drop("item.empty_oyster", weight = 3),
                    drop("item.empty_oyster", 3, weight = 1),
                    drop("item.oyster_pearl", weight = 1),
                    // Coins - 43.
                    coins(4, weight = 29),
                    coins(36, weight = 8),
                    coins(8, weight = 6),
                    // Other - 25, including the page's own explicit Nothing row.
                    WeightedDrop(item = null, weight = 19),
                    drop("item.fishing_bait", 10, weight = 2),
                    drop("item.opal_bolt_tips", 5, weight = 2),
                    drop("item.spinach_roll", weight = 1),
                    drop("item.casket", weight = 1),
                ),
        )

    /** One published `LocLine`: a place and its tiles. All five are plane 0. */
    internal data class Camp(
        val location: String,
        val tiles: List<Pair<Int, Int>>,
    )

    /**
     * Every published `LocLine` bar one. `Charred Island` publishes no coordinates at all - the
     * `LocLine` is there with an empty pin list - so there is nothing to place.
     */
    val CAMPS: List<Camp> =
        listOf(
            Camp(
                location = "North of Rellekka",
                tiles =
                    listOf(
                        2694 to 3724, 2700 to 3718, 2701 to 3728, 2702 to 3720, 2703 to 3716,
                        2704 to 3727, 2705 to 3725, 2708 to 3719, 2711 to 3715, 2712 to 3719,
                        2712 to 3725, 2715 to 3729, 2716 to 3721, 2719 to 3719, 2663 to 3716,
                        2666 to 3715, 2667 to 3727, 2669 to 3718, 2670 to 3727, 2671 to 3724,
                        2672 to 3732, 2673 to 3716, 2675 to 3729, 2676 to 3713, 2677 to 3717,
                        2677 to 3721, 2677 to 3733, 2678 to 3729, 2681 to 3727, 2681 to 3733,
                        2683 to 3716, 2684 to 3720, 2686 to 3713, 2687 to 3724,
                    ),
            ),
            Camp(
                location = "Waterbirth Island",
                tiles =
                    listOf(
                        2529 to 3740, 2512 to 3765, 2512 to 3766, 2514 to 3766, 2521 to 3756,
                        2530 to 3762, 2532 to 3760, 2537 to 3765, 2541 to 3757, 2543 to 3754,
                        2548 to 3734, 2553 to 3746, 2522 to 3716, 2523 to 3723, 2524 to 3724,
                        2531 to 3721, 2534 to 3719, 2501 to 3755, 2502 to 3754,
                    ),
            ),
            Camp(
                location = "West of Mount Quidamortem",
                tiles =
                    listOf(
                        1197 to 3587, 1199 to 3584, 1200 to 3588, 1204 to 3590, 1213 to 3596,
                        1199 to 3562, 1200 to 3565, 1200 to 3579, 1201 to 3553, 1201 to 3559,
                        1202 to 3562, 1202 to 3577, 1202 to 3581, 1203 to 3546, 1203 to 3551,
                        1203 to 3554, 1204 to 3557, 1204 to 3563, 1205 to 3544, 1207 to 3538,
                        1208 to 3541, 1209 to 3533, 1210 to 3535, 1210 to 3543, 1211 to 3538,
                        1212 to 3532, 1213 to 3539, 1214 to 3535,
                    ),
            ),
            Camp(
                location = "Waterbirth Island Dungeon",
                tiles =
                    listOf(
                        2442 to 10159, 2443 to 10158, 2443 to 10159, 2449 to 10160, 2449 to 10164,
                        2451 to 10165, 2454 to 10159, 2455 to 10164, 2456 to 10161, 2458 to 10160,
                        2459 to 10166, 2460 to 10153, 2460 to 10155, 2461 to 10154, 2464 to 10165,
                        2464 to 10167, 2470 to 10164, 2472 to 10162,
                    ),
            ),
            Camp(
                location = "Brine Rat Cavern",
                tiles = listOf(2748 to 10166, 2739 to 10153, 2706 to 10159, 2710 to 10168),
            ),
        )
}
