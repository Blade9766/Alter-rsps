package org.alter.plugins.content.npcs.imp

import org.alter.plugins.content.npcs.imp.Imps.GWD_ID
import org.alter.plugins.content.npcs.imp.Imps.NORMAL_ID

/**
 * Where the imps stand. Every `LocLine` the OSRS Wiki's Imp page publishes, transcribed
 * from raw wikitext and then checked tile by tile against this cache - 120 imps over 29
 * locations, five continents and one basement.
 *
 * Imps get their own spawn table for the same reason the chickens do (see
 * `content/npcs/critters/ChickenSpawns`): they are scattered across more of the map than
 * any one area package covers - Kourend, Varlamore, Karamja, Kandarin, Misthalin,
 * Asgarnia, the Isle of Souls - and only a handful of those places have an area package to
 * put them in. One table beats thirty edits to files that mostly do not exist yet.
 *
 * **The one imp already in the world was in `areas/lumbridge/spawns/SpawnPlugin`** and has
 * been removed from there in favour of the "Lumbridge Castle" row below, which is the same
 * tile (3217, 3226). Two sources for one spawn is how you end up with two imps.
 *
 * Three wiki rows are **not** here, each for a reason the verify test also enforces:
 *
 * - **Wilderness God Wars Dungeon** (3 pins around 2960, 10102). Mapsquare 46_157 is not
 *   in this cache at all, so those imps would spawn into a region that never loads and no
 *   player would ever see. Same call `ChickenSpawns` made for its Wyrmscraig row. This is
 *   also why [ImpDrops] does not model the ecumenical key, which only drops there.
 * - **Gloomthorn Trail (1388, 3224)**, one pin of nine. It is flagged BLOCK_WALK in this
 *   cache; the other eight rows of that trail are fine and are kept.
 * - **The God Wars Dungeon rows are moved from plane 0 to plane 2.** The wiki writes
 *   `plane = 0` because its GWD map (`mapID = 7`) is its own layer, not because the
 *   dungeon is at ground level. In this cache all seven of those tiles have no floor at
 *   all on planes 0, 1 and 3, and are walkable on plane 2. Taking `plane` at face value
 *   would have put seven imps in the void.
 *
 * Which id goes where is the wiki's `levels` field, not a guess: every row is level 2
 * ([NORMAL_ID], npc 5007) except the God Wars Dungeon, which is level 7 ([GWD_ID], npc
 * 3134).
 */
internal data class ImpHaunt(
    /** The wiki's own `location` text, so a row can be traced back to its `LocLine`. */
    val location: String,
    val npcKey: String,
    val tiles: List<Pair<Int, Int>>,
    val height: Int = 0,
    val walkRadius: Int = ImpSpawns.WALK_RADIUS,
)

internal object ImpSpawns {
    /**
     * How far an imp may wander from where it spawned. **Unpublished** - the wiki says
     * nothing about roaming range - so this is a judgement call, not a fact: wide enough
     * that imps drift the way they do in the real game and are not lined up in rows, tight
     * enough that a Varrock imp stays in Varrock.
     *
     * It is only ever a *radius from the spawn tile*;
     * [org.alter.plugins.content.mechanics.npcwalk.NpcRandomWalkPlugin] pathfinds to the
     * destination, so no imp walks through a wall no matter how large this gets.
     */
    const val WALK_RADIUS = 8

    /** Tighter, because Citlalli's basement is a room rather than a field. */
    const val BASEMENT_WALK_RADIUS = 3

    val HAUNTS: List<ImpHaunt> =
        listOf(
            ImpHaunt(
                location = "South of Stonecutter Outpost",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1728 to 2938,
                        1729 to 2941,
                        1742 to 2937,
                        1772 to 2943,
                        1765 to 2949,
                        1758 to 2936,
                    ),
            ),
            ImpHaunt(
                location = "South-west Civitas illa Fortis",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1651 to 3086,
                        1626 to 3091,
                        1627 to 3096,
                        1636 to 3088,
                    ),
            ),
            ImpHaunt(
                location = "North of Kourend Castle",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1616 to 3704,
                    ),
            ),
            ImpHaunt(
                location = "Around the Monk camp",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1798 to 3491,
                        1750 to 3506,
                        1758 to 3510,
                        1734 to 3511,
                        1740 to 3480,
                        1744 to 3484,
                        1748 to 3480,
                        1759 to 3481,
                        1762 to 3476,
                        1770 to 3478,
                    ),
            ),
            ImpHaunt(
                location = "Outside the Chasm of Fire",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1429 to 3663,
                        1431 to 3675,
                        1432 to 3672,
                        1435 to 3660,
                        1439 to 3680,
                        1442 to 3672,
                    ),
            ),
            ImpHaunt(
                location = "South of Port Piscarilius",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1792 to 3648,
                        1794 to 3654,
                    ),
            ),
            ImpHaunt(
                location = "Draynor Village",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        3073 to 3250,
                    ),
            ),
            ImpHaunt(
                location = "Rimmington",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        2941 to 3236,
                    ),
            ),
            ImpHaunt(
                location = "Scattered around Varrock",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        3262 to 3514,
                        3246 to 3458,
                        3177 to 3405,
                        3205 to 3355,
                        3234 to 3506,
                        3134 to 3487,
                        3238 to 3390,
                        3213 to 3502,
                    ),
            ),
            ImpHaunt(
                location = "Edgeville",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        3073 to 3498,
                        3078 to 3499,
                        3078 to 3461,
                    ),
            ),
            ImpHaunt(
                location = "East of Ardougne Monastery",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        2625 to 3203,
                        2625 to 3217,
                        2629 to 3233,
                        2630 to 3210,
                        2632 to 3202,
                        2633 to 3222,
                        2633 to 3243,
                        2639 to 3206,
                        2639 to 3230,
                        2629 to 3210,
                        2631 to 3200,
                        2635 to 3213,
                    ),
            ),
            ImpHaunt(
                location = "Lumbridge Castle",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        3217 to 3226,
                    ),
            ),
            ImpHaunt(
                location = "North of Lumbridge",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        3214 to 3281,
                        3240 to 3307,
                    ),
            ),
            ImpHaunt(
                location = "Western Falador",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        2951 to 3360,
                        2947 to 3329,
                    ),
            ),
            ImpHaunt(
                location = "East of Kingstown",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1734 to 3700,
                        1749 to 3702,
                        1750 to 3708,
                        1696 to 3683,
                        1703 to 3688,
                    ),
            ),
            ImpHaunt(
                location = "Farming Guild",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1283 to 3720,
                    ),
            ),
            ImpHaunt(
                location = "Al Kharid mine",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        3299 to 3273,
                    ),
            ),
            ImpHaunt(
                location = "South of Falador",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        3009 to 3307,
                        3011 to 3314,
                        3015 to 3314,
                    ),
            ),
            ImpHaunt(
                location = "North of the Woodcutting Guild",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1623 to 3527,
                        1639 to 3524,
                        1634 to 3553,
                    ),
            ),
            ImpHaunt(
                location = "Karamja Volcano",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        2832 to 3170,
                        2832 to 3177,
                        2837 to 3184,
                        2841 to 3163,
                        2849 to 3186,
                        2850 to 3165,
                        2857 to 3179,
                        2859 to 3177,
                    ),
            ),
            ImpHaunt(
                location = "North of Yanille",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        2595 to 3131,
                        2597 to 3120,
                        2606 to 3126,
                        2608 to 3134,
                        2614 to 3116,
                        2616 to 3126,
                        2621 to 3133,
                        2622 to 3118,
                    ),
            ),
            // Level 7, and plane 2 rather than the wiki's `plane = 0` - see [ImpSpawns].
            ImpHaunt(
                location = "God Wars Dungeon",
                npcKey = GWD_ID,
                height = 2,
                tiles =
                    listOf(
                        2839 to 5344,
                        2886 to 5313,
                        2898 to 5313,
                        2902 to 5357,
                        2907 to 5348,
                        2920 to 5356,
                        2925 to 5352,
                    ),
            ),
            ImpHaunt(
                location = "Isle of Souls",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        2269 to 2853,
                        2267 to 2842,
                        2275 to 2833,
                        2258 to 2831,
                    ),
            ),
            ImpHaunt(
                location = "Sunset Coast",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1534 to 2997,
                    ),
            ),
            ImpHaunt(
                location = "Avium Savannah",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1620 to 3032,
                        1629 to 3004,
                    ),
            ),
            ImpHaunt(
                location = "Outer Fortis",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1745 to 3040,
                        1784 to 3072,
                        1671 to 3073,
                    ),
            ),
            ImpHaunt(
                location = "Fortis Aqueduct",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1537 to 3136,
                        1591 to 3176,
                        1618 to 3135,
                    ),
            ),
            // Eight of the wiki's nine pins; (1388, 3224) is BLOCK_WALK here.
            ImpHaunt(
                location = "Gloomthorn Trail",
                npcKey = NORMAL_ID,
                tiles =
                    listOf(
                        1379 to 3218,
                        1370 to 3229,
                        1367 to 3216,
                        1363 to 3207,
                        1357 to 3202,
                        1366 to 3189,
                        1353 to 3193,
                        1359 to 3177,
                    ),
            ),
            ImpHaunt(
                location = "Citlalli's basement",
                npcKey = NORMAL_ID,
                walkRadius = BASEMENT_WALK_RADIUS,
                tiles =
                    listOf(
                        1381 to 9451,
                        1380 to 9454,
                        1384 to 9449,
                        1378 to 9450,
                    ),
            ),
        )
}
