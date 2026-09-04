package org.alter.plugins.content.npcs.scorpion

/**
 * The four versions of `Scorpion` this rev-228 cache has, and every place the OSRS Wiki puts one.
 *
 * See [ScorpionPlugin] for the wiring and [ScorpionSpawnPlugin] for the placement. Stats come from
 * `data/cfg/npcs/monsterStats.json` and animations from the existing `SCORPION` entry in
 * `named-combat-media.json`, 6254 / 6255 / 6256, which the second bestiary audit checked and left
 * alone.
 *
 * `content/npcs/dungeon` keeps the **poison scorpion**, a different page with a different level and
 * its own dungeon spawns. This package is the ordinary `Scorpion` page only.
 *
 * ## The level 1 scorpion is not in this cache
 *
 * `id1` is **14940**, and `data/cfg/rscm/npc.rscm` has no such npc - it postdates this revision, and
 * so do the three locations that carry it (`Sunbleak Island`, `Abalone Cliffs`, `The Great Conch`),
 * all of which publish empty pin lists anyway. There is nothing to spawn, so the version is absent
 * from [VARIANTS] entirely rather than defined and left empty.
 *
 * ## Scorpions drop no bones
 *
 * The `100%` section's only row is `Book page 1`, footnoted "only dropped from the Dwarven Mine
 * scorpions during the quest Between a Rock...". There is no such quest here, so nothing is
 * guaranteed at all - a scorpion leaves three tertiaries and an empty patch of sand, which is
 * exactly what the page says.
 */
internal data class ScorpionVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    val combatLevel: Int,
    /** Cache footprint. The published sizes are 2, 2, 1 and 2 for these four versions. */
    val size: Int,
    val npcKeys: List<String>,
    /** Wiki `respawn`, in game ticks, which are this engine's cycles one-for-one. */
    val respawnCycles: Int,
    val slayerXp: Double,
    /**
     * The wiki's `poisonous = Yes (N)` value: the damage this version's poison starts at, or 0.
     * Only the level 38 Ape Atoll scorpion is poisonous, at 1.
     */
    val poisonDamage: Int,
)

/** One published `LocLine`: a place, the version that stands there, and the tiles. */
internal data class ScorpionCamp(
    val location: String,
    val plane: Int,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object Scorpions {
    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a scorpion stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`,
     * stated because a def built from `monsterStats.json` starts with a **0** timer, which
     * `NpcAggroPlugin` reads as "stop being aggressive".
     */
    const val AGGRO_TIMER = 1000

    const val WALK_RADIUS = 4

    /** `id2` - the level 14, the one nearly every location carries. */
    val LEVEL_14_IDS = listOf("npc.scorpion_3024", "npc.scorpion_13001")

    /** `id3` - the level 37 Pit of Pestilence version. */
    val LEVEL_37_IDS = listOf("npc.scorpion_2480")

    /** `id4` - the level 38 Ape Atoll version, the only poisonous one and the only size 1. */
    val LEVEL_38_IDS = listOf("npc.scorpion_5242")

    /** `id5` - the level 59 Pit of Pestilence version. */
    val LEVEL_59_IDS = listOf("npc.scorpion")

    val VARIANTS: List<ScorpionVariant> =
        listOf(
            ScorpionVariant("Scorpion (level 14)", 14, 2, LEVEL_14_IDS, respawnCycles = 25, slayerXp = 17.0, poisonDamage = 0),
            ScorpionVariant("Scorpion (level 37)", 37, 2, LEVEL_37_IDS, respawnCycles = 25, slayerXp = 37.0, poisonDamage = 0),
            ScorpionVariant("Scorpion (level 38)", 38, 1, LEVEL_38_IDS, respawnCycles = 50, slayerXp = 15.0, poisonDamage = 1),
            ScorpionVariant("Scorpion (level 59)", 59, 2, LEVEL_59_IDS, respawnCycles = 22, slayerXp = 55.0, poisonDamage = 0),
        )

    /** Wiki tertiary, Wilderness only - and notably rarer than most looting bags at 1/10. */
    const val LOOTING_BAG_ONE_IN = 10

    /** Wiki tertiary. */
    const val ENSOULED_HEAD_ONE_IN = 25

    /** Wiki `DropsLineClue|type=beginner`. */
    const val BEGINNER_CLUE_ONE_IN = 100

    /**
     * The published `LocLine`s this rev-228 cache can hold, with ids dealt across the camps by
     * [org.alter.plugins.content.npcs.SpawnDealer].
     *
     * **Not here**: `Ralos' Rise mine` and `South-west of Stonecutter Outpost` (Varlamore, later than
     * this cache); `Kruk's Dungeon` and the `Pit of Pestilence`, whose mapsquares this cache does not
     * ship; and the three level 1 lines, which publish no coordinates and whose npc does not exist
     * here.
     */
    val CAMPS: List<ScorpionCamp> =
        listOf(
            // ------------------------------------------------------------------- Kharidian
            ScorpionCamp(
                location = "Al Kharid mine",
                plane = 0,
                npcKeys = LEVEL_14_IDS,
                tiles =
                    listOf(
                        3292 to 3297, 3298 to 3304, 3298 to 3311, 3299 to 3288, 3300 to 3315,
                        3301 to 3278, 3302 to 3306, 3303 to 3292, 3298 to 3299,
                    ),
            ),
            ScorpionCamp(
                location = "Al Kharid, south of the bank",
                plane = 0,
                npcKeys = LEVEL_14_IDS,
                tiles = listOf(3292 to 3142),
            ),
            ScorpionCamp(
                location = "Ruins of Unkah",
                plane = 0,
                npcKeys = LEVEL_14_IDS,
                tiles = listOf(3164 to 2862, 3178 to 2852, 3180 to 2856, 3182 to 2852, 3187 to 2862, 3193 to 2855),
            ),
            // -------------------------------------------------------------------- Asgarnia
            ScorpionCamp(
                location = "Dwarven Mine",
                plane = 0,
                npcKeys = LEVEL_14_IDS,
                tiles =
                    listOf(
                        3038 to 9771, 3039 to 9790, 3041 to 9781, 3044 to 9787, 3045 to 9777,
                        3046 to 9767, 3047 to 9784, 3049 to 9777, 3054 to 9773, 3056 to 9778,
                        3039 to 9802, 3042 to 9793, 3042 to 9800,
                    ),
            ),
            // ------------------------------------------------------------------- Misthalin
            ScorpionCamp(
                location = "Varrock Sewers",
                plane = 0,
                npcKeys = LEVEL_14_IDS,
                tiles = listOf(3251 to 9908, 3253 to 9903),
            ),
            // ------------------------------------------------------------------ Wilderness
            ScorpionCamp(
                location = "Scorpion Pit",
                plane = 0,
                npcKeys = LEVEL_14_IDS,
                tiles = listOf(3225 to 3943, 3231 to 3940, 3235 to 3946, 3239 to 3942, 3246 to 3945, 3249 to 3949, 3256 to 3954),
            ),
            ScorpionCamp(
                location = "West of the Air Obelisk",
                plane = 0,
                npcKeys = LEVEL_14_IDS,
                tiles =
                    listOf(
                        3025 to 3568, 3028 to 3577, 3032 to 3570, 3040 to 3581, 3041 to 3572,
                        3047 to 3567, 3055 to 3574, 3056 to 3566, 3062 to 3555, 3062 to 3570,
                    ),
            ),
            // --------------------------------------------------------------------- Karamja
            ScorpionCamp(
                location = "Karamja Volcano",
                plane = 0,
                npcKeys = LEVEL_14_IDS,
                tiles = listOf(2843 to 3156, 2853 to 3159, 2861 to 3165, 2862 to 3180, 2863 to 3169, 2865 to 3156, 2870 to 3159),
            ),
            ScorpionCamp(
                location = "Kharazi Jungle",
                plane = 0,
                npcKeys = LEVEL_14_IDS,
                tiles = listOf(2785 to 2955, 2786 to 2947, 2788 to 2948),
            ),
            ScorpionCamp(
                location = "Cairn Isle",
                plane = 0,
                npcKeys = LEVEL_14_IDS,
                tiles = listOf(2765 to 2978, 2768 to 2980),
            ),
            // -------------------------------------------------------------------- Kandarin
            ScorpionCamp(
                location = "Ardougne Zoo",
                plane = 0,
                npcKeys = LEVEL_14_IDS,
                tiles = listOf(2602 to 3267, 2603 to 3270, 2607 to 3268),
            ),
            ScorpionCamp(
                location = "Temple of Ikov",
                plane = 0,
                npcKeys = LEVEL_14_IDS,
                tiles = listOf(2637 to 9809, 2639 to 9814, 2641 to 9811, 2641 to 9819, 2644 to 9822),
            ),
            // -------------------------------------------------------------------- Ape Atoll
            ScorpionCamp(
                location = "Ape Atoll",
                plane = 0,
                npcKeys = LEVEL_38_IDS,
                tiles =
                    listOf(
                        2752 to 2762, 2766 to 2761, 2776 to 2764, 2762 to 2749, 2769 to 2734,
                        2772 to 2740, 2781 to 2743, 2761 to 2803, 2769 to 2806, 2894 to 2715,
                        2899 to 2734, 2901 to 2721, 2902 to 2727, 2792 to 2796, 2793 to 2778,
                        2804 to 2797,
                    ),
            ),
        )

    /** Every scorpion key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }
}
