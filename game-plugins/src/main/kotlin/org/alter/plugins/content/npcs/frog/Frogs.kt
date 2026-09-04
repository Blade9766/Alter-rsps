package org.alter.plugins.content.npcs.frog

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The frogs of Lumbridge Swamp and the caves below it - three wiki pages, five published versions,
 * one package.
 *
 * `Frog`, `Big frog` and `Giant frog` are the same creature at three sizes, with one table between
 * them that only the two adult versions roll, and they share every location. Splitting them would
 * have produced three copies of the Lumbridge Swamp pin list.
 *
 * See [FrogPlugin] for the wiring and [FrogSpawnPlugin] for the placement. Stats come from
 * `data/cfg/npcs/monsterStats.json`.
 *
 * ## The giant frogs were fighting with a hill giant's animations
 *
 * `Giant frog` normalises to `GIANT_FROG`, which starts with `GIANT_` - so the name lookup's prefix
 * rule handed it the hill giant's 4652 / 4651 / 4653. That is the same failure the
 * `SKELETON_HELLHOUND` entry in `npc-animations/README.md` was added for, on a monster a quarter the
 * size. All five frogs are pinned to the toad rig now (1793 / 1794 / 1795).
 *
 * ## Every frog here is passive
 *
 * `aggressive = No` on all five versions, which is why none of them gets an aggression radius - and
 * why, unlike most of this bestiary pass, there is no `aggressiveTimer` note to make.
 *
 * ## None of them gives Slayer experience
 *
 * No version publishes `slayxp` or `cat`. That is not a gap in the page: there is no frog Slayer
 * category, so `slayerXp` is left at zero rather than being invented from the hitpoints.
 */
internal data class FrogVariant(
    /** The wiki page and version, kept verbatim so a row can be found again. */
    val name: String,
    /** The `Infobox Monster` `name`, which is what the cache calls this id. */
    val cacheName: String,
    val combatLevel: Int,
    /** Cache footprint. Frogs and big frogs are 1; giant frogs are 2. */
    val size: Int,
    val npcKey: String,
    /** rscm item keys dropped on every kill. */
    val guaranteed: List<String>,
    /** Which of the two tables this version rolls, or null for the three that publish none. */
    val dropTable: String?,
    /** `DropsLineClue|type=beginner` rate, or 0 where the version publishes no clue row. */
    val beginnerClueOneIn: Int,
    /** True for the two giant frogs, which publish the long bone and curved bone rows. */
    val bones: Boolean,
)

/** One published `LocLine`: a place, the version that stands there, and the tiles. */
internal data class FrogCamp(
    val location: String,
    val plane: Int,
    val npcKey: String,
    val tiles: List<Pair<Int, Int>>,
)

internal object Frogs {
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

    /** Wiki `respawn = 50` on every version of all three pages. */
    const val RESPAWN_CYCLES = 50

    const val WALK_RADIUS = 5

    /** Both giant frogs, `1/400`. */
    const val LONG_BONE_ONE_IN = 400

    /** Both giant frogs, `1/5012.5` - a non-integer rate, so a Double rolled on `randomDouble`. */
    const val CURVED_BONE_ONE_IN = 5012.5

    val VARIANTS: List<FrogVariant> =
        listOf(
            FrogVariant(
                name = "Frog (level 5)",
                cacheName = "Frog",
                combatLevel = 5,
                size = 1,
                npcKey = "npc.frog_8702",
                guaranteed = listOf("item.bones"),
                dropTable = null,
                beginnerClueOneIn = 90,
                bones = false,
            ),
            FrogVariant(
                name = "Big frog (level 10)",
                cacheName = "Big frog",
                combatLevel = 10,
                size = 1,
                npcKey = "npc.big_frog_8701",
                // The big frog leg is published `rarity = Always`, so it is a guaranteed row rather
                // than a tertiary - both versions of the page carry it.
                guaranteed = listOf("item.bones", "item.big_frog_leg"),
                dropTable = null,
                beginnerClueOneIn = 70,
                bones = false,
            ),
            FrogVariant(
                name = "Big frog (level 24)",
                cacheName = "Big frog",
                combatLevel = 24,
                size = 1,
                npcKey = "npc.big_frog",
                guaranteed = listOf("item.bones", "item.big_frog_leg"),
                dropTable = "big frog",
                // The level 24 publishes no clue row at all, unlike the level 10.
                beginnerClueOneIn = 0,
                bones = false,
            ),
            FrogVariant(
                name = "Giant frog (level 13)",
                cacheName = "Giant frog",
                combatLevel = 13,
                size = 2,
                npcKey = "npc.giant_frog_8700",
                guaranteed = listOf("item.big_bones"),
                dropTable = null,
                beginnerClueOneIn = 64,
                bones = true,
            ),
            FrogVariant(
                name = "Giant frog (level 99)",
                cacheName = "Giant frog",
                combatLevel = 99,
                size = 2,
                npcKey = "npc.giant_frog",
                guaranteed = listOf("item.big_bones"),
                dropTable = "giant frog",
                beginnerClueOneIn = 0,
                bones = true,
            ),
        )

    /**
     * The level 24 big frog's table.
     *
     * The page's published rows come to **86 of 128** - four rune rows at 33 and four others at 53 -
     * and it publishes no `Nothing` row for the other 42. Those 42 slots are written as one here
     * rather than the rows being rescaled to fill them, because rescaling would silently make every
     * published rate a third more common than the page says: 10/128 water runes would become
     * 15/128. An explicit `Nothing` row keeps every rate exactly what it is written as, and lets
     * `BestiaryVerify` check the arithmetic like every other table.
     */
    val BIG_FROG =
        MonsterDropTable(
            denominator = 128,
            rows =
                listOf(
                    // Runes - 33.
                    drop("item.water_rune", 12, weight = 10),
                    drop("item.earth_rune", 12, weight = 10),
                    drop("item.nature_rune", 4, weight = 7),
                    drop("item.cosmic_rune", 2, weight = 6),
                    // Other - 53.
                    coins(5, weight = 32),
                    coins(15, weight = 19),
                    drop("item.water_talisman", weight = 1),
                    drop("item.earth_talisman", weight = 1),
                    // The 42 slots the page leaves unaccounted for; see the doc above.
                    WeightedDrop(item = null, weight = 42),
                ),
        )

    /**
     * The level 99 giant frog's table - 128 exactly as published, with no gaps and no sub-tables.
     *
     * Its largest row by far is `Giant frog legs` at 64/128: half of all kills, which is what makes
     * this the monster people farm for them.
     */
    val GIANT_FROG =
        MonsterDropTable(
            denominator = 128,
            rows =
                listOf(
                    // Weapons - 2.
                    drop("item.mithril_spear", weight = 2),
                    // Runes and ammunition - 37.
                    drop("item.nature_rune", weight = 10),
                    drop("item.nature_rune", 3, weight = 10),
                    drop("item.nature_rune", 9, weight = 10),
                    drop("item.cosmic_rune", 5, weight = 3),
                    drop("item.iron_arrow", 22, weight = 2),
                    drop("item.blood_rune", weight = 1),
                    drop("item.steel_arrow", 45, weight = 1),
                    // Other - 89.
                    drop("item.giant_frog_legs", weight = 64),
                    coins(30, weight = 10),
                    coins(2, weight = 8),
                    coins(37, weight = 5),
                    drop("item.coal", weight = 1),
                    drop("item.spinach_roll", weight = 1),
                ),
        )

    /** Both tables by the label [FrogVariant.dropTable] names. */
    val BY_LABEL: Map<String, MonsterDropTable> = mapOf("big frog" to BIG_FROG, "giant frog" to GIANT_FROG)

    /**
     * Every published `LocLine` this rev-228 cache can hold.
     *
     * **Not here**: `The Great Conch` and `Tlati Rainforest` (Varlamore, later than this cache) and
     * `Limbo`, whose `LocLine` publishes no coordinates at all.
     */
    val CAMPS: List<FrogCamp> =
        listOf(
            FrogCamp(
                location = "Lumbridge Swamp",
                plane = 0,
                npcKey = "npc.frog_8702",
                tiles =
                    listOf(
                        3157 to 3191, 3158 to 3172, 3158 to 3186, 3159 to 3176, 3163 to 3167,
                        3163 to 3197, 3166 to 3177, 3167 to 3163, 3170 to 3198, 3172 to 3167,
                        3173 to 3177, 3174 to 3189, 3175 to 3157, 3175 to 3171, 3177 to 3166,
                        3181 to 3196, 3182 to 3156, 3184 to 3183, 3185 to 3178, 3187 to 3162,
                        3191 to 3195, 3192 to 3179, 3193 to 3161, 3194 to 3166, 3194 to 3190,
                        3195 to 3175, 3197 to 3187, 3198 to 3181, 3205 to 3195, 3211 to 3162,
                        3212 to 3191, 3214 to 3186, 3214 to 3195, 3215 to 3165, 3217 to 3160,
                        3219 to 3179, 3221 to 3183, 3222 to 3157, 3222 to 3174, 3225 to 3195,
                        3226 to 3187, 3227 to 3161, 3230 to 3169, 3233 to 3178, 3233 to 3191,
                    ),
            ),
            FrogCamp(
                location = "Lumbridge Swamp (big frogs)",
                plane = 0,
                npcKey = "npc.big_frog_8701",
                tiles =
                    listOf(
                        3175 to 3198, 3176 to 3165, 3177 to 3158, 3177 to 3191, 3179 to 3195,
                        3180 to 3173, 3181 to 3187, 3182 to 3177, 3185 to 3180, 3185 to 3182,
                        3185 to 3199, 3187 to 3161, 3188 to 3167, 3193 to 3157, 3203 to 3176,
                        3204 to 3188, 3204 to 3193, 3206 to 3190, 3207 to 3173, 3207 to 3176,
                        3209 to 3165, 3211 to 3185, 3213 to 3168, 3213 to 3190, 3217 to 3181,
                        3218 to 3184, 3221 to 3164, 3227 to 3173, 3229 to 3164,
                    ),
            ),
            FrogCamp(
                location = "Lumbridge Swamp (giant frogs)",
                plane = 0,
                npcKey = "npc.giant_frog_8700",
                tiles =
                    listOf(
                        3190 to 3180, 3194 to 3194, 3196 to 3170, 3196 to 3176,
                        3197 to 3189, 3200 to 3177, 3201 to 3186, 3203 to 3174,
                    ),
            ),
            FrogCamp(
                location = "Mount Quidamortem",
                plane = 0,
                npcKey = "npc.big_frog",
                tiles = listOf(1308 to 3494, 1310 to 3510, 1311 to 3498, 1311 to 3519, 1313 to 3504, 1307 to 3529),
            ),
            FrogCamp(
                location = "Lumbridge Swamp Caves",
                plane = 0,
                npcKey = "npc.big_frog",
                tiles =
                    listOf(
                        3244 to 9570, 3248 to 9567, 3250 to 9573, 3183 to 9543, 3151 to 9550,
                        3151 to 9562, 3154 to 9556, 3159 to 9554, 3221 to 9557, 3224 to 9552,
                        3224 to 9559,
                    ),
            ),
            FrogCamp(
                location = "Lumbridge Swamp Caves (giant frogs)",
                plane = 0,
                npcKey = "npc.giant_frog",
                tiles = listOf(3221 to 9549, 3224 to 9544, 3231 to 9547),
            ),
            FrogCamp(
                location = "Dorgesh-Kaan South Dungeon",
                plane = 0,
                npcKey = "npc.big_frog",
                tiles =
                    listOf(
                        2696 to 5227, 2697 to 5223, 2698 to 5219, 2701 to 5217, 2727 to 5200,
                        2729 to 5189, 2729 to 5196, 2729 to 5202, 2741 to 5194, 2741 to 5205,
                        2743 to 5199, 2744 to 5203, 2745 to 5198,
                    ),
            ),
            FrogCamp(
                location = "Dorgesh-Kaan South Dungeon (giant frogs)",
                plane = 0,
                npcKey = "npc.giant_frog",
                tiles = listOf(2731 to 5192, 2731 to 5205, 2745 to 5206),
            ),
        )

    /** Every frog key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.map { it.npcKey } }
}
