package org.alter.plugins.content.npcs.wolf

/**
 * Every wolf the OSRS Wiki publishes - the five versions of `Wolf` and the two of `White wolf` -
 * and every place one stands.
 *
 * Both pages are modelled here rather than in two packages because they are the same animal: one
 * Slayer category (`Wolves`), one drop table (wolf bones and tertiaries, nothing else), one rig,
 * and the same two locations that carry both. See [WolfPlugin] for the wiring and [WolfSpawnPlugin]
 * for the placement.
 *
 * ## Why there is no `setCombatDef` in this package
 *
 * The reason `content/npcs/zombie` and `content/npcs/dwarf` give: every id below already carries its
 * exact wiki stat block in `data/cfg/npcs/monsterStats.json`, including the 25% Fire elemental
 * weakness both pages gained in the 25 June 2025 "Summer Sweep Up: Combat" update, and
 * `World.setNpcDefaults` reads that table **only** for npcs no plugin declares a def for.
 *
 * ## The animations were wrong before this package existed
 *
 * `named-combat-media.json`'s `WOLF` entry held **6581 / 6574 / 6576**, which is the *hellhound*
 * animation set - the only npc in `openosrs-animations.json` that has ever been observed playing
 * any of the three is npc 3133, `Hellhound`. Every wolf id in the cache is observed playing
 * **6559 / 6557 / 6558** instead, and by the priority convention every other entry follows
 * (attack 6, block 5, death 10) those are attack, block and death respectively.
 *
 * Both sets are frame group 1662, so the wrong ones would have played rather than failing visibly -
 * wolves would simply have lunged like hellhounds. The entry is corrected as part of this package,
 * which also fixes `White wolf`: `MonsterAnimationsPlugin.findNamedCombatMedia` falls back to a
 * suffix match, so `WHITE_WOLF` was picking up the same broken row.
 */
internal data class WolfVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    val combatLevel: Int,
    val npcKeys: List<String>,
    val slayerXp: Double,
    /** Wiki `aggressive`. The two Stronghold of Security wolves are the only ones that are not. */
    val aggressive: Boolean,
    /** True for the `White wolf` page, whose looting-bag rate is 1/3 rather than 1/6. */
    val whiteWolf: Boolean = false,
)

/** One published `LocLine`: a place, the ids that stand there, and the tiles. */
internal data class WolfCamp(
    val location: String,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object Wolves {
    /** Wiki `respawn = 90` on both pages, in game ticks, used as published. */
    const val RESPAWN_CYCLES = 90

    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a wolf stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`.
     *
     * This has to be stated rather than left alone. A def built from `monsterStats.json` starts from
     * `NpcCombatDef.DEFAULT`, whose `aggressiveTimer` is **0**, and `NpcAggroPlugin`'s default
     * aggressiveness reads a zero timer as "this player has been here longer than 0 cycles, stop
     * being aggressive" - so an aggression radius alone never fires once. The zombie package found
     * this the hard way.
     *
     * Left at the ordinary timer rather than `alwaysAggro()`, because the same function's
     * `p.combatLevel <= npcLvl * 2` check is exactly the real rule that stops a level 64 wolf
     * bothering anyone above combat level 128.
     */
    const val AGGRO_TIMER = 1000

    /**
     * How far a spawned wolf wanders from its pin.
     *
     * Wolves are size 2 and travel in packs of four to fifteen pins a few tiles apart; a wide
     * radius on an aggressive pack turns a mountain path into a moving wall of them.
     */
    const val WALK_RADIUS = 4

    // ------------------------------------------------------------------ the ids

    /** `Wolf` level 11 and 14 - `id1` and `id2`, the Stronghold of Security pair. */
    const val LEVEL_11 = "npc.wolf_2491"

    const val LEVEL_14 = "npc.wolf_2490"

    /**
     * `Wolf` level 25 - `id3`.
     *
     * The infobox annotates this one `<!--Also unused IDs 116,117,231-->`. All three are `Wolf`,
     * level 25, size 2, with an `Attack` option in this cache and all three carry the level 25 stat
     * row, so they are included as variants (a hand-spawned one then behaves) but deliberately not
     * placed by [WolfSpawnPlugin] - "unused" is the wiki saying they stand nowhere.
     */
    val LEVEL_25 = listOf("npc.wolf_110", "npc.wolf_116", "npc.wolf_117", "npc.wolf_231")

    /** The one level 25 id the wiki actually places. */
    const val LEVEL_25_PLACED = "npc.wolf_110"

    /** `Wolf` level 64 - `id4`, and `id5` for the Rellekka coat. */
    const val LEVEL_64 = "npc.wolf"

    const val LEVEL_64_RELLEKKA = "npc.wolf_3912"

    /** `White wolf` `id1` and `id2` - levels 25 and 38. */
    const val WHITE_25 = "npc.white_wolf"

    const val WHITE_38 = "npc.white_wolf_108"

    val VARIANTS: List<WolfVariant> =
        listOf(
            WolfVariant("Wolf (level 11)", 11, listOf(LEVEL_11), slayerXp = 10.0, aggressive = false),
            WolfVariant("Wolf (level 14)", 14, listOf(LEVEL_14), slayerXp = 15.0, aggressive = false),
            WolfVariant("Wolf (level 25)", 25, LEVEL_25, slayerXp = 34.0, aggressive = true),
            WolfVariant("Wolf (level 64)", 64, listOf(LEVEL_64), slayerXp = 69.0, aggressive = true),
            WolfVariant("Wolf (level 64, Rellekka)", 64, listOf(LEVEL_64_RELLEKKA), slayerXp = 69.0, aggressive = true),
            WolfVariant("White wolf (level 25)", 25, listOf(WHITE_25), slayerXp = 34.0, aggressive = true, whiteWolf = true),
            WolfVariant("White wolf (level 38)", 38, listOf(WHITE_38), slayerXp = 44.0, aggressive = true, whiteWolf = true),
        )

    /**
     * Every `LocLine` on both pages.
     *
     * Wolves are size 2, so a pin needs a standable 2x2 rather than a standable tile - `BestiaryVerify`
     * checks the whole footprint, which matters on mountain paths and the Feldip Hills coast where
     * a published pin an inch off the walkable strip would leave a wolf nobody can reach.
     *
     * Varlamore and Kourend locations are included. This cache carries those maps and the server
     * already runs members content; a spawn there is a placement in a built area rather than a
     * speculative one.
     */
    val CAMPS: List<WolfCamp> =
        listOf(
            // ------------------------------------------------------------------ Kandarin
            WolfCamp(
                location = "Arandar entrance",
                npcKeys = listOf(LEVEL_64),
                // The published 2395,3340 stands on floor itself, but a wolf is size 2 and the two
                // tiles north of it are `BLOCK_WALK`; moved one south so the whole 2x2 fits.
                tiles = listOf(2379 to 3344, 2395 to 3339, 2398 to 3330, 2406 to 3329),
            ),
            WolfCamp(
                location = "Ardougne Zoo",
                npcKeys = listOf(LEVEL_64),
                tiles = listOf(2616 to 3283, 2620 to 3283),
            ),
            WolfCamp(
                location = "Path between Seers' Village and Rellekka",
                npcKeys = listOf(LEVEL_64),
                tiles = listOf(2647 to 3584),
            ),
            WolfCamp(
                location = "South of the Outpost",
                npcKeys = listOf(LEVEL_64),
                tiles = listOf(2437 to 3336, 2439 to 3333, 2439 to 3335, 2440 to 3333, 2440 to 3335),
            ),
            WolfCamp(
                location = "West of Sinclair Mansion",
                npcKeys = listOf(LEVEL_64),
                tiles = listOf(2717 to 3569, 2706 to 3580, 2680 to 3585, 2682 to 3592),
            ),
            WolfCamp(
                location = "South of West Ardougne",
                npcKeys = listOf(LEVEL_64),
                tiles =
                    listOf(
                        2462 to 3274, 2465 to 3272, 2465 to 3275,
                        2470 to 3272, 2472 to 3274, 2477 to 3275,
                    ),
            ),
            WolfCamp(
                location = "Feldip Hills",
                npcKeys = listOf(LEVEL_64),
                tiles =
                    listOf(
                        2602 to 2955, 2605 to 2963, 2607 to 2967, 2610 to 2958, 2610 to 2961,
                        2610 to 2965, 2482 to 2923, 2491 to 2922, 2491 to 2927, 2491 to 2931,
                        2496 to 2959, 2498 to 2968, 2500 to 2959, 2502 to 2969, 2509 to 2964,
                    ),
            ),
            WolfCamp(
                location = "Gnome maze",
                npcKeys = listOf(LEVEL_64),
                tiles = listOf(2516 to 3197, 2548 to 3179),
            ),
            // ------------------------------------------------------------------ Fremennik
            WolfCamp(
                location = "South of Rellekka",
                npcKeys = listOf(LEVEL_64_RELLEKKA),
                tiles =
                    listOf(
                        2626 to 3633, 2630 to 3631, 2630 to 3637,
                        2641 to 3626, 2642 to 3622, 2644 to 3627,
                    ),
            ),
            // ------------------------------------------------------------------ Asgarnia
            /*
             * The only place a grey and a white wolf stand together: the wiki gives the mountain a
             * level 25 `Wolf` line and a level 25 `White wolf` line over different pins, so the two
             * id sets are kept on their own published tiles rather than merged.
             */
            WolfCamp(
                location = "White Wolf Mountain (grey)",
                npcKeys = listOf(LEVEL_25_PLACED),
                tiles =
                    listOf(
                        2836 to 3495, 2837 to 3499, 2839 to 3502,
                        2839 to 3506, 2842 to 3504, 2844 to 3507,
                    ),
            ),
            WolfCamp(
                location = "White Wolf Mountain (white)",
                npcKeys = listOf(WHITE_25),
                tiles =
                    listOf(
                        2864 to 3453, 2870 to 3438, 2872 to 3445, 2842 to 3450, 2846 to 3477,
                        2847 to 3474, 2848 to 3487, 2850 to 3484, 2854 to 3509, 2856 to 3508,
                        2860 to 3492, 2866 to 3498, 2831 to 3515, 2833 to 3513,
                    ),
            ),
            // ------------------------------------------------------------------ Wilderness
            WolfCamp(
                location = "Wilderness, south-east of the Deserted Keep",
                npcKeys = listOf(LEVEL_64),
                tiles = listOf(3207 to 3919, 3218 to 3924, 3229 to 3917, 3241 to 3921),
            ),
            WolfCamp(
                location = "South of Wilderness Agility Course",
                npcKeys = listOf(WHITE_38),
                tiles =
                    listOf(
                        2988 to 3921, 2991 to 3924, 2992 to 3917, 2994 to 3921,
                        3002 to 3924, 3004 to 3917, 3005 to 3926, 3006 to 3921,
                    ),
            ),
            // ------------------------------------------------------------------ Misthalin
            /*
             * The only free-to-play wolves, and the only two versions that are not aggressive. The
             * page publishes one pin list for both levels, so the two ids are dealt across it.
             */
            WolfCamp(
                location = "Stronghold of Security, first level",
                npcKeys = listOf(LEVEL_11, LEVEL_14),
                tiles =
                    listOf(
                        1887 to 5221, 1889 to 5214, 1889 to 5217, 1890 to 5224, 1892 to 5217,
                        1892 to 5220, 1870 to 5226, 1871 to 5229, 1871 to 5236, 1872 to 5233,
                        1873 to 5239, 1886 to 5188, 1889 to 5187, 1889 to 5191, 1894 to 5193,
                        1897 to 5189,
                    ),
            ),
            // ------------------------------------------------------------------ Kourend
            WolfCamp(
                location = "South of Forthos Dungeon, near the saltpetre mines",
                npcKeys = listOf(LEVEL_64),
                tiles = listOf(1665 to 3553, 1667 to 3549, 1669 to 3556, 1672 to 3552),
            ),
            // ------------------------------------------------------------------ Varlamore
            WolfCamp(
                location = "Northern side of the Hailstorm Mountains",
                npcKeys = listOf(LEVEL_64),
                tiles =
                    listOf(
                        1498 to 3306, 1512 to 3312, 1516 to 3315,
                        1525 to 3310, 1609 to 3268, 1612 to 3272,
                    ),
            ),
            WolfCamp(
                location = "North of Gloomthorn Trail",
                npcKeys = listOf(LEVEL_64),
                tiles = listOf(1399 to 3215, 1371 to 3324, 1388 to 3239, 1377 to 3255),
            ),
            WolfCamp(
                location = "The Proudspire",
                npcKeys = listOf(WHITE_38),
                tiles =
                    listOf(
                        1551 to 3215, 1557 to 3248, 1585 to 3247, 1575 to 3204, 1548 to 3222,
                        1554 to 3244, 1568 to 3210, 1584 to 3226, 1540 to 3244, 1593 to 3256,
                        1547 to 3218, 1582 to 3222,
                    ),
            ),
        )

    /** Every wolf key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }
}
