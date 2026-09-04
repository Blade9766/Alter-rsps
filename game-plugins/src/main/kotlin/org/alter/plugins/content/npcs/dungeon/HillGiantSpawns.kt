package org.alter.plugins.content.npcs.dungeon

/**
 * Where the hill giants stand. Every pin the OSRS Wiki's Hill Giant page publishes, from its
 * `Locations` table's `LocLine` rows.
 *
 * Hill giants already had a complete, correct combat definition in [DungeonMonsters] and a drop
 * table in [DungeonDrops] - 35 hitpoints, 18/22/26, +18/+16, Crush on a 6-tick attack, 35 Slayer
 * xp, Earth weakness at 25%, aggressive, big bones guaranteed. What they did not have was
 * anywhere to be. Five of them stood in Taverley Dungeon and that was the entire population of
 * the species; the Edgeville Dungeon giants that most players actually train on did not exist.
 *
 * **Why these live here rather than in `areas/<name>/spawns`.** Same call as
 * `content/npcs/Cows` and `critters/ChickenSpawns`: thirteen locations across four continents,
 * of which exactly one - Taverley Dungeon - has an area package today. The Taverley five are
 * **moved** here from `areas/taverleydungeon/spawns/SpawnPlugin` rather than left in place, so
 * the species has one spawn table and Taverley does not end up with ten giants.
 *
 * **The tiles are cache-verified.** `HillGiantVerify` reads every one out of this project's own
 * map files and asserts it has a floor and is not flagged BLOCK_WALK across the full **2x2
 * footprint** - a hill giant is `size = 2`, so a clear pin is not the same as a clear square.
 *
 * **The Catacombs of Kourend is the one partial location.** The wiki publishes 13 pins there and
 * only 5 have a clear 2x2 in this cache. The 8 that do not are not scattered: every one of them
 * is in the western half, between x 1634 and 1655 - (1634, 10011), (1634, 10015), (1637, 10008),
 * (1638, 10012), (1647, 10036), (1650, 10034), (1650, 10038) and (1655, 10038) - while all five
 * that verify sit together in the east around (1662-1665, 10063-10073). A clean split like that
 * says something systematic about those chambers rather than eight independent bad pins.
 *
 * They are left out rather than nudged onto a neighbouring tile, because a nudged spawn is an
 * invented one. The likeliest explanation is untested: that those rows are anchored on a corner
 * other than the south-west one this server spawns a size-2 npc from. That is the first thing to
 * check if someone wants the missing 8.
 *
 * **Which id.** The page is a `Multi Infobox` with four groups, and unlike the cows these are
 * not interchangeable - two of them differ in their *drops*:
 *
 * - **Regular** (2098, 2099, 2100, 2101, 2102, 2103, 13502, 13503, 13504) - everywhere that is
 *   not one of the three below.
 * - **Giants' Plateau** (10374, 10375, 10376) - the Ferox Enclave giants. The wiki's drop table
 *   splits four rows on exactly this line: `iron med helm` and `steel scimitar` are flagged
 *   *"Only dropped by giants on the Giants' Plateau"*, while `iron full helm` and
 *   `steel longsword` are *"Only dropped by giants not on the Giants' Plateau"*. That is why
 *   they are their own [DungeonMonsters] entry with their own table, and not just three more ids
 *   on the regular one.
 * - **Kourend** (7261) - the Catacombs giants, `dropversion = Catacombs`.
 * - **Varlamore** (12848, 12849, 12850) - used in the Giants' Den and the Avium Savannah, the
 *   two Varlamore locations. The page gives them no drop differences, so they share the regular
 *   table.
 *
 * Two further ids carry the name "Hill Giant" in this cache and are **not** on the wiki's list,
 * so neither is spawned or defined: **11195** is combat level 0 with no options at all - a prop -
 * and **11467** is level 28 but its only option is "Strike" rather than "Attack", so it is not a
 * monster you fight in the ordinary way.
 */
internal data class GiantCamp(
    /** The wiki's own `location` text, kept verbatim so a row can be found again. */
    val location: String,
    /** The ids for this camp, dealt round-robin across [tiles]. */
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
    val walkRadius: Int = HillGiantSpawns.WALK_RADIUS,
)

internal object HillGiantSpawns {
    /**
     * Hill giants are aggressive and roam; 6 is what the Taverley five already used and it is
     * kept, rather than the tight radius the penned farm animals need.
     */
    const val WALK_RADIUS = 6

    /**
     * Version group 1-6 on the page's Regular infobox. Note the first: `npc.hill_giant` is the
     * bare, unsuffixed rscm name for 2098, so it does not follow the `_id` pattern the rest do.
     */
    val REGULAR_IDS =
        listOf(
            "npc.hill_giant",
            "npc.hill_giant_2099",
            "npc.hill_giant_2100",
            "npc.hill_giant_2101",
            "npc.hill_giant_2102",
            "npc.hill_giant_2103",
            "npc.hill_giant_13502",
            "npc.hill_giant_13503",
            "npc.hill_giant_13504",
        )

    /** The Catacombs of Kourend giants. */
    val KOUREND_IDS = listOf("npc.hill_giant_7261")

    /** Varlamore: the Giants' Den and the Avium Savannah. */
    val VARLAMORE_IDS = listOf("npc.hill_giant_12848", "npc.hill_giant_12849", "npc.hill_giant_12850")

    /** Giants' Plateau, Ferox Enclave - the group with its own drop rows. */
    val PLATEAU_IDS = listOf("npc.hill_giant_10374", "npc.hill_giant_10375", "npc.hill_giant_10376")

    val CAMPS: List<GiantCamp> =
        listOf(
            // ---- Misthalin and the Wilderness ----
            GiantCamp(
                location = "Edgeville Dungeon",
                npcKeys = REGULAR_IDS,
                tiles =
                    listOf(
                        3099 to 9832, 3100 to 9836, 3106 to 9827, 3107 to 9835, 3110 to 9845,
                        3111 to 9841, 3113 to 9831, 3116 to 9832, 3117 to 9835, 3117 to 9843,
                        3117 to 9850, 3122 to 9844,
                    ),
            ),
            GiantCamp(
                location = "Lava Maze",
                npcKeys = REGULAR_IDS,
                tiles = listOf(3094 to 3849, 3110 to 3854, 3117 to 3854, 3104 to 3875),
            ),
            GiantCamp(
                location = "Bone Yard Hunter area",
                npcKeys = REGULAR_IDS,
                tiles = listOf(3307 to 3669, 3300 to 3649),
            ),
            GiantCamp(
                location = "Deep Wilderness Dungeon",
                npcKeys = REGULAR_IDS,
                tiles =
                    listOf(
                        3042 to 10317, 3044 to 10306, 3045 to 10308, 3045 to 10311, 3045 to 10321,
                        3046 to 10315,
                    ),
            ),
            GiantCamp(
                location = "Giants' Plateau",
                npcKeys = PLATEAU_IDS,
                tiles =
                    listOf(
                        3372 to 3152, 3373 to 3148, 3375 to 3144, 3376 to 3150, 3379 to 3144,
                        3380 to 3152, 3381 to 3147,
                    ),
            ),

            // ---- Asgarnia ----
            GiantCamp(
                // Moved here from areas/taverleydungeon/spawns - see the class doc.
                location = "Taverley Dungeon",
                npcKeys = REGULAR_IDS,
                tiles = listOf(2902 to 9736, 2905 to 9732, 2907 to 9735, 2913 to 9732, 2913 to 9741),
            ),

            // ---- Kandarin ----
            GiantCamp(
                location = "Gnome Maze",
                npcKeys = REGULAR_IDS,
                tiles = listOf(2545 to 3144, 2548 to 3147, 2507 to 3149),
            ),
            GiantCamp(
                location = "North of the Observatory",
                npcKeys = REGULAR_IDS,
                tiles =
                    listOf(
                        2438 to 3208, 2438 to 3218, 2439 to 3212, 2441 to 3217, 2443 to 3211,
                        2445 to 3209,
                    ),
            ),
            GiantCamp(
                location = "South-west of Tree Gnome Stronghold",
                npcKeys = REGULAR_IDS,
                tiles =
                    listOf(
                        2369 to 3394, 2369 to 3401, 2369 to 3404, 2371 to 3398, 2372 to 3395,
                        2372 to 3401,
                    ),
            ),

            // ---- Kourend and Varlamore ----
            GiantCamp(
                location = "Giant Pit",
                npcKeys = REGULAR_IDS,
                tiles =
                    listOf(
                        1446 to 3614, 1448 to 3609, 1440 to 3606, 1440 to 3618, 1441 to 3614,
                        1448 to 3612, 1444 to 3607, 1447 to 3606, 1439 to 3610, 1448 to 3617,
                        1444 to 3613, 1442 to 3610, 1446 to 3618,
                    ),
            ),
            GiantCamp(
                // 5 of the wiki's 13 pins. The other 8 read BLOCK_WALK in this cache - see the
                // class doc for why they are left out rather than moved.
                location = "Catacombs of Kourend",
                npcKeys = KOUREND_IDS,
                tiles =
                    listOf(
                        1662 to 10070, 1662 to 10073, 1663 to 10063, 1663 to 10066, 1665 to 10071,
                    ),
            ),
            GiantCamp(
                location = "Giants' Den",
                npcKeys = VARLAMORE_IDS,
                tiles =
                    listOf(
                        1426 to 9887, 1428 to 9884, 1429 to 9880, 1432 to 9888, 1433 to 9879,
                        1436 to 9883, 1439 to 9886, 1444 to 9881,
                    ),
            ),
            GiantCamp(
                location = "Avium Savannah",
                npcKeys = VARLAMORE_IDS,
                tiles =
                    listOf(
                        1614 to 2964, 1619 to 2953, 1617 to 2957, 1620 to 2963, 1613 to 2959,
                        1623 to 2957,
                    ),
            ),
        )
}
