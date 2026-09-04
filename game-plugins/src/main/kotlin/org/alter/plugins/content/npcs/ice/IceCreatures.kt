package org.alter.plugins.content.npcs.ice

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.SeedTableId
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The ice warriors and ice giants - two wiki pages, one package, because they are the same
 * population: every location that has one has the other, and the four ice warrior versions and seven
 * ice giant versions are pinned by which of those shared places they stand in.
 *
 * See [IcePlugin] for the wiring and [IceSpawnPlugin] for the placement. Stats come from
 * `data/cfg/npcs/monsterStats.json`, including the **100% Fire elemental weakness** both pages lead
 * with - the highest in the game and the whole reason a fire spell is the answer here.
 *
 * ## The animations were wrong before this package
 *
 * `ICE_GIANT` was already correct. `Ice warrior` had **all three roles scrambled**: the resolver was
 * playing `ICE_WARRIOR_ATTACK` (391) as the death, `ICE_WARRIOR_HIT` (389) as the attack, and 843 -
 * the shared earth/ice warrior rig's death - as the block. The Ice Queen's Lair version (2851) has
 * only two observations, so the resolver bailed on it entirely and it fell to the human 422/424/836,
 * and the Varlamore version (13802) has none at all. All four are pinned now; see
 * `npc-animations/README.md`.
 *
 * ## Both tables need the members reading, and both say so out loud
 *
 * The ice warrior's adamant arrow row is footnoted "adamant arrows drop rate decreases to 2/128 in
 * members worlds" and the ice giant's black kiteshield "drop rate is decreased to 4/128 in members
 * worlds" - so unlike every earlier table in this tree, where the members reading had to be inferred
 * from the arithmetic, these two publish it. Take the `altrarity` on those rows, drop the
 * free-to-play-only coin rows, and both tables land on 128 exactly. Keep the free-to-play column and
 * the ice warrior comes to 157 and the ice giant to 137.
 */
internal data class IceVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    /** The `Infobox Monster` `name`, which is what the cache calls these ids. */
    val cacheName: String,
    val combatLevel: Int,
    /** Cache footprint. Ice warriors are 1, ice giants 2. */
    val size: Int,
    val npcKeys: List<String>,
    val slayerXp: Double,
    /** Which of the two tables this version rolls. */
    val giant: Boolean,
)

/** One published `LocLine`: a place, the ids that stand there, and the tiles. */
internal data class IceCamp(
    val location: String,
    val plane: Int,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object IceCreatures {
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

    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long one of these stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`,
     * stated because a def built from `monsterStats.json` starts with a **0** timer, which
     * `NpcAggroPlugin` reads as "stop being aggressive".
     */
    const val AGGRO_TIMER = 1000

    /** Wiki `respawn = 30` on every version of both pages. */
    const val RESPAWN_CYCLES = 30

    const val WARRIOR_WALK_RADIUS = 5

    /** Ice giants are size 2; a smaller radius keeps the Asgarnian Ice Dungeon walkable. */
    const val GIANT_WALK_RADIUS = 4

    // ------------------------------------------------------------------------- ice warrior ids

    /** `Ice warrior` `id1` - the Frozen Waste Plateau and White Wolf Mountain version. */
    val WARRIOR_NORMAL_IDS = listOf("npc.ice_warrior")

    /** `Ice warrior` `id2`. */
    val WARRIOR_ICE_DUNGEON_IDS = listOf("npc.ice_warrior_2842")

    /** `Ice warrior` `id3`. */
    val WARRIOR_QUEENS_LAIR_IDS = listOf("npc.ice_warrior_2851")

    /** `Ice warrior` `id4` - Varlamore. Defined but not placed; see [CAMPS]. */
    val WARRIOR_VARLAMORE_IDS = listOf("npc.ice_warrior_13802")

    // -------------------------------------------------------------------------- ice giant ids

    /** `Ice giant` `id1`..`id3` - the version everywhere but the Asgarnian Ice Dungeon. */
    val GIANT_NORMAL_IDS = listOf("npc.ice_giant", "npc.ice_giant_2086", "npc.ice_giant_2087")

    /** `Ice giant` `id4`..`id5`. */
    val GIANT_ICE_DUNGEON_IDS = listOf("npc.ice_giant_2088", "npc.ice_giant_2089")

    /** `Ice giant` `id6`..`id7` - Varlamore. Defined but not placed; see [CAMPS]. */
    val GIANT_VARLAMORE_IDS = listOf("npc.ice_giant_13796", "npc.ice_giant_13797")

    val VARIANTS: List<IceVariant> =
        listOf(
            IceVariant("Ice warrior (Normal)", "Ice warrior", 57, 1, WARRIOR_NORMAL_IDS, 59.0, giant = false),
            IceVariant("Ice warrior (Asgarnian Ice Dungeon)", "Ice warrior", 57, 1, WARRIOR_ICE_DUNGEON_IDS, 59.0, false),
            IceVariant("Ice warrior (Ice Queen's Lair)", "Ice warrior", 57, 1, WARRIOR_QUEENS_LAIR_IDS, 59.0, false),
            IceVariant("Ice warrior (Varlamore)", "Ice warrior", 57, 1, WARRIOR_VARLAMORE_IDS, 59.0, false),
            IceVariant("Ice giant", "Ice giant", 53, 2, GIANT_NORMAL_IDS, 70.0, giant = true),
            IceVariant("Ice giant (Asgarnian Ice Dungeon)", "Ice giant", 53, 2, GIANT_ICE_DUNGEON_IDS, 70.0, true),
            IceVariant("Ice giant (Varlamore)", "Ice giant", 53, 2, GIANT_VARLAMORE_IDS, 70.0, true),
        )

    // ------------------------------------------------------------------------------ tertiaries

    /** Both pages, Wilderness only. */
    const val LOOTING_BAG_ONE_IN = 3

    /** Ice warrior `DropsLineClue|type=medium`. */
    const val MEDIUM_CLUE_ONE_IN = 128

    /**
     * The ice warrior clue's `altrarity`: "increases to 1/64 if a ring of wealth (i) is worn **and**
     * fought in the Wilderness" - both halves required, exactly like the hellhound's hard clue and
     * unlike the dragons', whose footnote names no place.
     */
    const val MEDIUM_CLUE_WEALTH_ONE_IN = 64

    /** Ice giant tertiary. */
    const val GIANT_ENSOULED_HEAD_ONE_IN = 21

    /** Ice giant `DropsLineClue|type=beginner`. */
    const val GIANT_BEGINNER_CLUE_ONE_IN = 40

    /** Ice giant tertiary. */
    const val GIANT_LONG_BONE_ONE_IN = 400

    /** Ice giant tertiary - there is no Champions' Challenge, but the scroll keeps its real rate. */
    const val GIANT_CHAMPION_SCROLL_ONE_IN = 5000

    /** Ice giant tertiary, the one non-integer rate here - a Double, rolled on `randomDouble`. */
    const val GIANT_CURVED_BONE_ONE_IN = 5012.5

    /**
     * The ice warrior table - rows 97, herbs 10, seeds 18, gem 3, summing to the published 128 on
     * the members reading.
     *
     * `Ice warriors drop no bones`: the page has no `100%` section at all, which is right for a
     * suit of animated armour.
     */
    val WARRIOR_TABLE =
        MonsterDropTable(
            denominator = 128,
            herbWeight = 10,
            seedWeight = 18,
            seedTable = SeedTableId.UNCOMMON,
            gemWeight = 3,
            rows =
                listOf(
                    // Weapons - 4.
                    drop("item.iron_battleaxe", weight = 3),
                    drop("item.mithril_mace", weight = 1),
                    // Runes and ammunition - 41, the adamant arrow row at its members altrarity of 2.
                    drop("item.nature_rune", 4, weight = 10),
                    drop("item.chaos_rune", 3, weight = 8),
                    drop("item.law_rune", 2, weight = 7),
                    drop("item.cosmic_rune", 2, weight = 5),
                    drop("item.mithril_arrow", 3, weight = 5),
                    drop("item.death_rune", 2, weight = 3),
                    drop("item.adamant_arrow", 2, weight = 2),
                    drop("item.blood_rune", 2, weight = 1),
                    // Coins - 39. The two free-to-play-only rows are absent.
                    coins(15, weight = 39),
                    // Other - 13, the page's own explicit Nothing row.
                    WeightedDrop(item = null, weight = 13),
                ),
        )

    /** The ice giant table - rows 116, seeds 8, gem 4, and no herb row at all. */
    val GIANT_TABLE =
        MonsterDropTable(
            denominator = 128,
            seedWeight = 8,
            seedTable = SeedTableId.UNCOMMON,
            gemWeight = 4,
            rows =
                listOf(
                    // Weapons and armour - 20, the black kiteshield at its members altrarity of 4.
                    drop("item.iron_2h_sword", weight = 5),
                    drop("item.black_kiteshield", weight = 4),
                    drop("item.steel_axe", weight = 4),
                    drop("item.steel_sword", weight = 4),
                    drop("item.iron_platelegs", weight = 1),
                    drop("item.mithril_mace", weight = 1),
                    drop("item.mithril_sq_shield", weight = 1),
                    // Runes and ammunition - 22.
                    drop("item.adamant_arrow", 5, weight = 6),
                    drop("item.nature_rune", 6, weight = 4),
                    drop("item.mind_rune", 24, weight = 3),
                    drop("item.body_rune", 37, weight = 3),
                    drop("item.law_rune", 3, weight = 2),
                    drop("item.water_rune", 12, weight = 1),
                    drop("item.cosmic_rune", 4, weight = 1),
                    drop("item.death_rune", 3, weight = 1),
                    drop("item.blood_rune", 2, weight = 1),
                    // Coins - 69. The free-to-play-only 5-coin row is absent.
                    coins(117, weight = 32),
                    coins(53, weight = 12),
                    coins(196, weight = 10),
                    coins(8, weight = 7),
                    coins(22, weight = 6),
                    coins(400, weight = 2),
                    // Other - 5.
                    drop("item.jug_of_wine", weight = 3),
                    drop("item.mithril_ore", weight = 1),
                    drop("item.banana", weight = 1),
                ),
        )

    /**
     * Every published `LocLine` this rev-228 cache can hold, with ids dealt across the camps by
     * [org.alter.plugins.content.npcs.SpawnDealer]. Which id stands where is not a guess: each
     * version's wiki label names its own location.
     *
     * **Not here**: the two **Ruins of Tapoyauik** lines and the Varlamore versions they carry,
     * later content whose mapsquares this cache does not ship; and the ice giants' **Wilderness
     * Slayer Cave** line, whose `levels = 67` matches no version in the infobox at all - the page
     * publishes no id for it, so there is nothing to spawn.
     */
    val CAMPS: List<IceCamp> =
        listOf(
            // ----------------------------------------------------------------- ice warriors
            IceCamp(
                location = "Southern Frozen Waste Plateau",
                plane = 0,
                npcKeys = WARRIOR_NORMAL_IDS,
                tiles =
                    listOf(
                        2945 to 3866, 2946 to 3857, 2948 to 3878, 2948 to 3886, 2952 to 3874,
                        2953 to 3858, 2954 to 3877, 2954 to 3883, 2957 to 3887, 2958 to 3866,
                        2959 to 3876, 2962 to 3883, 2963 to 3874,
                    ),
            ),
            IceCamp(
                location = "Northern Frozen Waste Plateau",
                plane = 0,
                npcKeys = WARRIOR_NORMAL_IDS,
                tiles =
                    listOf(
                        2947 to 3934, 2948 to 3917, 2949 to 3926, 2952 to 3913, 2952 to 3936,
                        2954 to 3921, 2956 to 3930, 2964 to 3944, 2970 to 3947, 2971 to 3938,
                        2977 to 3953, 2978 to 3942, 2984 to 3933,
                    ),
            ),
            IceCamp(
                location = "White Wolf Mountain",
                plane = 0,
                npcKeys = WARRIOR_NORMAL_IDS,
                tiles = listOf(2847 to 3514, 2850 to 3512),
            ),
            IceCamp(
                location = "Asgarnian Ice Dungeon",
                plane = 0,
                npcKeys = WARRIOR_ICE_DUNGEON_IDS,
                tiles =
                    listOf(
                        3044 to 9581, 3046 to 9575, 3048 to 9583, 3049 to 9590, 3053 to 9576,
                        3056 to 9572, 3056 to 9583, 3056 to 9587, 3060 to 9572, 3062 to 9576,
                        3062 to 9581,
                    ),
            ),
            IceCamp(
                location = "Ice Queen's Lair",
                plane = 0,
                npcKeys = WARRIOR_QUEENS_LAIR_IDS,
                tiles =
                    listOf(
                        2834 to 9940, 2836 to 9953, 2844 to 9944, 2864 to 9950, 2865 to 9951,
                        2866 to 9948, 2867 to 9948, 2867 to 9951, 2868 to 9950, 2822 to 9901,
                        2836 to 9905, 2838 to 9917, 2847 to 9919, 2848 to 9912,
                    ),
            ),
            // ------------------------------------------------------------------- ice giants
            IceCamp(
                location = "Frozen Waste Plateau",
                plane = 0,
                npcKeys = GIANT_NORMAL_IDS,
                tiles =
                    listOf(
                        2947 to 3921, 2950 to 3932, 2955 to 3945, 2978 to 3956, 2947 to 3895,
                        2952 to 3902, 2953 to 3889, 2954 to 3894, 2958 to 3898,
                    ),
            ),
            IceCamp(
                location = "Settlement Ruins",
                plane = 0,
                npcKeys = GIANT_NORMAL_IDS,
                tiles = listOf(1551 to 3885, 1552 to 3881, 1554 to 3889, 1557 to 3894, 1562 to 3887),
            ),
            IceCamp(
                location = "White Wolf Mountain, on top",
                plane = 0,
                npcKeys = GIANT_NORMAL_IDS,
                tiles = listOf(2817 to 3514, 2824 to 3510, 2804 to 3507, 2811 to 3506),
            ),
            IceCamp(
                location = "Ice Queen's Lair",
                plane = 0,
                npcKeys = GIANT_NORMAL_IDS,
                tiles = listOf(2880 to 9927, 2883 to 9932, 2883 to 9965, 2884 to 9959, 2887 to 9955, 2890 to 9950, 2891 to 9941),
            ),
            IceCamp(
                location = "Asgarnian Ice Dungeon",
                plane = 0,
                npcKeys = GIANT_ICE_DUNGEON_IDS,
                tiles =
                    listOf(
                        3057 to 9573, 3061 to 9573, 3065 to 9571, 2962 to 9570, 2965 to 9568,
                        2966 to 9565, 2963 to 9563, 2960 to 9565, 2960 to 9568,
                    ),
            ),
        )

    /** Every key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }
}
