package org.alter.plugins.content.npcs.critters

/**
 * Where the chickens stand. Every pin the OSRS Wiki's Chicken page publishes, from its
 * `Locations` table's `LocLine` rows, with one deliberate omission each way - see below.
 *
 * Chickens have had combat stats and a drop table here since [Critters] was written, but
 * nothing in the world ever spawned one: the only chicken a player could meet was the one
 * the Goblin Cave's boxes throw at them (`areas/goblincave/objs/SearchBoxesPlugin`). That is
 * why `SlayerService.markAvailable` named them as its example of a definition with no spawn -
 * with none in the world, Turael's **Birds** task was unassignable.
 *
 * **Why these live here and not in `areas/<name>/spawns`.** The usual rule is that an area
 * owns its own population, and the goblins, guards and giant bats all follow it. Chickens do
 * not fit it: they are spread over 28 published locations on four continents, and only four
 * of those - Lumbridge, Falador, Ardougne and Varrock's Champions' Guild - have an area
 * package at all. Twenty-odd new packages holding three lines each would bury the shape of
 * the thing. This is the same call `content/npcs/banker` made for the bankers of every town
 * in the game, and the table below is the same shape as its `Banks`.
 *
 * **The tiles are cache-verified, not transcribed and hoped for.** `ChickenVerify` reads
 * every one of them out of this project's own map files and asserts it has a floor and is not
 * flagged BLOCK_WALK. That check is what removed the one omission going out:
 *
 * - **Wyrmscraig** (2584,2265 and two more) - mapsquare 40_35 is not in this cache at all. It
 *   is 2025 Varlamore content and this is a 228 cache, so those tiles decode to nothing.
 *
 * And two more left out for reasons the wiki row states itself:
 *
 * - **The Goblin Cave pin** (2622,3393) is a published location but not a spawn point - the
 *   row reads "when searching boxes or crates". `SearchBoxesPlugin` already does that at
 *   runtime, so a chicken standing there permanently would be wrong.
 * - **The Tyras Camp cutscene pin** (`mapID = 10160`) is an instance, not the world map.
 *
 * **Which id.** The wiki groups twelve ids into four versions that share one unversioned stat
 * block, so the choice is cosmetic and every version is mechanically identical - see
 * [Critters]. Each version is used where its own name says: the Falador Farm chickens at
 * South Falador Farm, the Miscellania chickens on Miscellania and Etceteria, Gordon and
 * Mary's at their farm, and the normal ones everywhere else. Within a version the ids are
 * dealt round-robin, the same stable-but-arbitrary choice the bankers and goblins make.
 *
 * The one id held back is **9488**. The wiki files it under version1 alongside 1173 and 1174,
 * all three combat level 1 - but in this cache 9488 is combat level **3**, so it is not
 * interchangeable with the other two whatever the page says. It keeps its combat def in
 * [Critters] and stays out of the rotation until there is something better than a guess about
 * where it belongs.
 */
internal data class ChickenFlock(
    /** The wiki's own `location` text, kept verbatim so a row can be found again. */
    val location: String,
    /** The version's ids, dealt round-robin across [tiles]. */
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
    val height: Int = 0,
    val walkRadius: Int = ChickenSpawns.PENNED_RADIUS,
)

internal object ChickenSpawns {
    /**
     * How far a penned chicken strays from its spawn tile.
     *
     * Deliberately tight, for the reason `CowPlugin` records: this server's random walk
     * ([org.alter.plugins.content.mechanics.npcwalk.NpcRandomWalkPlugin]) picks a tile in a
     * square around the spawn point without checking whether anything can walk there, so a
     * generous radius on a penned animal picks tiles on the far side of the fence. Two tiles
     * keeps every coop bird inside its coop.
     */
    const val PENNED_RADIUS = 2

    /** For the flocks whose pins are scattered over open ground rather than inside a pen. */
    const val OPEN_RADIUS = 4

    /** Version 1, minus 9488 - see [ChickenFlock]. */
    val NORMAL_IDS = listOf(1173, 1174).map { "npc.chicken_$it" }

    /** Version 2, Throne of Miscellania. */
    val MISCELLANIA_IDS = listOf(3661, 3662).map { "npc.chicken_$it" }

    /** Version 3, added with Farming in 2005. */
    val FALADOR_FARM_IDS = listOf(2804, 2805, 2806).map { "npc.chicken_$it" }

    /** Version 4, added with Getting Ahead. */
    val GORDON_AND_MARY_IDS = listOf(10495, 10496, 10494, 10497).map { "npc.chicken_$it" }

    val FLOCKS: List<ChickenFlock> =
        listOf(
            // ---- Misthalin ----
            ChickenFlock(
                location = "Lumbridge West Farm",
                npcKeys = NORMAL_IDS,
                // The big coop north of the road (28 pins), plus the five that stand loose in
                // the field south-east of it at z 3277-3279. One LocLine on the wiki, so one
                // flock here.
                tiles =
                    listOf(
                        3171 to 3293, 3171 to 3298, 3172 to 3291, 3172 to 3293, 3173 to 3296,
                        3173 to 3302, 3174 to 3291, 3174 to 3297, 3174 to 3300, 3174 to 3305,
                        3175 to 3293, 3175 to 3298, 3176 to 3289, 3176 to 3301, 3177 to 3296,
                        3177 to 3304, 3177 to 3305, 3178 to 3292, 3178 to 3295, 3178 to 3298,
                        3179 to 3301, 3180 to 3298, 3181 to 3290, 3181 to 3296, 3182 to 3301,
                        3183 to 3290, 3183 to 3296, 3183 to 3299, 3185 to 3277, 3186 to 3279,
                        3187 to 3278, 3189 to 3278, 3191 to 3277,
                    ),
            ),
            ChickenFlock(
                location = "North of Lumbridge West Farm",
                npcKeys = NORMAL_IDS,
                tiles = listOf(3237 to 3323),
                walkRadius = OPEN_RADIUS,
            ),
            ChickenFlock(
                location = "Lumbridge East Farm",
                npcKeys = NORMAL_IDS,
                tiles =
                    listOf(
                        3225 to 3300, 3226 to 3296, 3228 to 3297, 3228 to 3299, 3229 to 3296,
                        3230 to 3298, 3230 to 3299, 3231 to 3297, 3231 to 3300, 3232 to 3299,
                        3233 to 3294, 3234 to 3297, 3235 to 3298,
                    ),
            ),
            ChickenFlock(
                location = "Champions' Guild",
                npcKeys = NORMAL_IDS,
                tiles = listOf(3196 to 3352, 3197 to 3352, 3197 to 3353, 3198 to 3354, 3198 to 3355),
            ),
            ChickenFlock(
                // Underground, but plane 0: the basement is its own patch of map at z 9569,
                // not an upper floor. One chicken, alone down there.
                location = "Wizards' Tower Basement",
                npcKeys = NORMAL_IDS,
                tiles = listOf(3109 to 9569),
                walkRadius = OPEN_RADIUS,
            ),

            // ---- Asgarnia ----
            ChickenFlock(
                location = "South Falador Farm",
                npcKeys = FALADOR_FARM_IDS,
                tiles =
                    listOf(
                        3015 to 3287, 3016 to 3294, 3018 to 3283, 3018 to 3289, 3018 to 3294,
                        3019 to 3285, 3027 to 3288, 3029 to 3286, 3031 to 3286, 3031 to 3288,
                    ),
            ),
            ChickenFlock(
                // The lone chicken in the White Knights' courtyard gets the normal ids, not
                // the Falador Farm ones: that version belongs to the farm, and this bird is a
                // long way north of it, inside the castle walls.
                location = "White Knights Castle Courtyard",
                npcKeys = NORMAL_IDS,
                tiles = listOf(2966 to 3346),
            ),
            ChickenFlock(
                location = "West of Warriors' Guild (Tenzing's House)",
                npcKeys = NORMAL_IDS,
                tiles = listOf(2816 to 3561, 2818 to 3559, 2819 to 3561, 2819 to 3562),
            ),
            ChickenFlock(
                location = "Entrana",
                npcKeys = NORMAL_IDS,
                tiles =
                    listOf(
                        2846 to 3374, 2850 to 3368, 2850 to 3371, 2852 to 3370, 2853 to 3368,
                        2853 to 3373,
                    ),
            ),

            // ---- Kandarin ----
            ChickenFlock(
                location = "Between Ardougne and Witchaven",
                npcKeys = NORMAL_IDS,
                tiles = listOf(2691 to 3273, 2691 to 3277, 2692 to 3271, 2695 to 3274),
            ),
            ChickenFlock(
                location = "Entrance of the Ranging Guild",
                npcKeys = NORMAL_IDS,
                tiles = listOf(2650 to 3441, 2650 to 3442, 2651 to 3441, 2651 to 3442),
            ),
            ChickenFlock(
                location = "Sinclair Mansion",
                npcKeys = NORMAL_IDS,
                tiles = listOf(2735 to 3559, 2736 to 3562, 2738 to 3560),
                walkRadius = OPEN_RADIUS,
            ),
            ChickenFlock(
                // The Barbarian Outpost course, not Barbarian Village.
                location = "Barbarian Agility Course",
                npcKeys = NORMAL_IDS,
                tiles = listOf(2553 to 3562, 2553 to 3565, 2554 to 3563),
                walkRadius = OPEN_RADIUS,
            ),

            // ---- Fremennik ----
            ChickenFlock(
                location = "Rellekka",
                npcKeys = NORMAL_IDS,
                tiles = listOf(2679 to 3663, 2679 to 3665, 2680 to 3664, 2681 to 3663),
            ),
            ChickenFlock(
                location = "Miscellania Castle Courtyard",
                npcKeys = MISCELLANIA_IDS,
                tiles = listOf(2512 to 3862, 2512 to 3867, 2514 to 3859, 2516 to 3865),
            ),
            ChickenFlock(
                location = "Miscellania Blacksmith area",
                npcKeys = MISCELLANIA_IDS,
                tiles = listOf(2537 to 3892, 2539 to 3893, 2545 to 3893, 2547 to 3893),
            ),
            ChickenFlock(
                // Etceteria gets the Miscellania ids. The wiki names that version after one
                // island of the pair, but both arrived in the same update - Throne of
                // Miscellania - and the choice is cosmetic either way.
                location = "Etceteria Castle Courtyard",
                npcKeys = MISCELLANIA_IDS,
                tiles = listOf(2601 to 3874, 2601 to 3878, 2602 to 3877, 2606 to 3867),
            ),
            ChickenFlock(
                location = "North of Etceteria Castle",
                npcKeys = MISCELLANIA_IDS,
                tiles = listOf(2612 to 3897, 2612 to 3900, 2617 to 3897),
            ),

            // ---- Karamja ----
            ChickenFlock(
                location = "Tai Bwo Wannai",
                npcKeys = NORMAL_IDS,
                // Four pins spread across 28 tiles of village, not a pen.
                tiles = listOf(2780 to 3064, 2788 to 3061, 2805 to 3064, 2808 to 3067),
                walkRadius = OPEN_RADIUS,
            ),

            // ---- Tirannwn ----
            ChickenFlock(
                location = "Tyras Camp",
                npcKeys = NORMAL_IDS,
                tiles = listOf(2184 to 3152, 2185 to 3152, 2186 to 3153),
                walkRadius = OPEN_RADIUS,
            ),
            ChickenFlock(
                location = "Prifddinas, Crwys district",
                npcKeys = NORMAL_IDS,
                tiles = listOf(3299 to 6106, 3300 to 6108, 3305 to 6104, 3310 to 6104),
                walkRadius = OPEN_RADIUS,
            ),

            // ---- Kourend and Varlamore ----
            ChickenFlock(
                location = "Gordon and Mary's farm",
                npcKeys = GORDON_AND_MARY_IDS,
                tiles =
                    listOf(
                        1250 to 3688, 1250 to 3692, 1251 to 3689, 1252 to 3692, 1252 to 3687,
                        1253 to 3693, 1254 to 3690, 1255 to 3692,
                    ),
            ),
            ChickenFlock(
                location = "North of Land's End",
                npcKeys = NORMAL_IDS,
                tiles = listOf(1502 to 3469, 1504 to 3465, 1508 to 3465, 1508 to 3470, 1511 to 3468),
                walkRadius = OPEN_RADIUS,
            ),
            ChickenFlock(
                location = "South of Kourend Castle",
                npcKeys = NORMAL_IDS,
                tiles =
                    listOf(
                        1669 to 3636, 1669 to 3637, 1669 to 3639, 1669 to 3640, 1670 to 3637,
                        1670 to 3639,
                    ),
            ),
            ChickenFlock(
                location = "South western Outer Fortis",
                npcKeys = NORMAL_IDS,
                tiles = listOf(1653 to 3067),
                walkRadius = OPEN_RADIUS,
            ),
            ChickenFlock(
                // The only flock that is not on the ground floor. `plane = 3` on the wiki row,
                // and the tiles do decode as walkable at that height in this cache.
                location = "Aldarin windmill (3rd floor)",
                npcKeys = NORMAL_IDS,
                tiles =
                    listOf(
                        1395 to 2896, 1396 to 2895, 1397 to 2894, 1398 to 2895, 1399 to 2894,
                        1400 to 2893, 1398 to 2892, 1400 to 2891,
                    ),
                height = 3,
            ),
            ChickenFlock(
                // The wiki row has a typo in its third pin - "x:1241,3139", with the "y=" lost -
                // so that tile is unusable and only the four well-formed pins are placed.
                location = "Tal Teklan",
                npcKeys = NORMAL_IDS,
                tiles = listOf(1236 to 3138, 1237 to 3136, 1242 to 3136, 1244 to 3137),
                walkRadius = OPEN_RADIUS,
            ),

            // ---- Isle of Souls ----
            ChickenFlock(
                location = "Isle of Souls",
                npcKeys = NORMAL_IDS,
                tiles =
                    listOf(
                        2164 to 2795, 2165 to 2790, 2169 to 2792, 2170 to 2797, 2173 to 2793,
                        2173 to 2800, 2175 to 2788,
                    ),
                walkRadius = OPEN_RADIUS,
            ),
        )
}
