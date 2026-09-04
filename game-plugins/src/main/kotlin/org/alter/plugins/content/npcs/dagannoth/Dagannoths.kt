package org.alter.plugins.content.npcs.dagannoth

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.SeedTableId
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The four published versions of `Dagannoth` and every place the OSRS Wiki puts one.
 *
 * See [DagannothPlugin] for the wiring and [DagannothSpawnPlugin] for the placement. Stats come from
 * `data/cfg/npcs/monsterStats.json` and animations from the existing `DAGANNOTH` entry in
 * `named-combat-media.json` (1341 / 1340 / 1342), which the second bestiary audit checked and left
 * alone.
 *
 * These are the ordinary dagannoth of the Lighthouse, not the Dagannoth Kings.
 *
 * ## The level 74 shoots, and it is wired to
 *
 * `Level 74 (1)` publishes `attack style = [[Ranged]]` where every other version publishes `Stab` -
 * that is the split between the spine-throwing dagannoth at the back of the Lighthouse cave and the
 * biting ones at the front, and it is a real difference in how the fight plays.
 *
 * `monsterStats.json` cannot express it. Its own doc says so: *"Notably absent, and deliberately:
 * NpcCombatDef.combatClass... ranged and magic monsters therefore keep their real levels and bonuses
 * while still swinging as melee."* So every ranged monster in this server melees, and the dagannoth
 * would have too.
 *
 * It is fixed here rather than left, because the two pieces it needs are both **sourced rather than
 * invented** - `Animation.DAGANNOTH_SPINES_ATTACK` (1343) and `Graphic.DAGANNOTH_SPINES` (294), two
 * named constants that exist for no other monster and are used by nothing else in this codebase.
 * Without a real projectile id this would have stayed melee: `RangedCombatStrategy.fireNpcProjectile`
 * silently draws nothing when `rangedProjectileGfx` is -1, and an invisible shot is worse than an
 * honest melee swing.
 *
 * [DagannothPlugin] patches `combatClass`, `combatStyle`, the attack animation and the projectile
 * onto the def in an `onNpcSpawn` hook, which runs *after* `World.setNpcDefaults` has copied
 * `combatClass` onto the npc - so the npc's own field is set directly too.
 */
internal data class DagannothVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    val combatLevel: Int,
    /** Cache footprint. The level 74 is 1 and the level 92 is 2. */
    val size: Int,
    val npcKeys: List<String>,
    val slayerXp: Double,
    /** True for `Level 74 (1)`, the one version whose published attack style is Ranged. */
    val ranged: Boolean,
)

/** One published `LocLine`: a place, the ids that stand there, and the tiles. */
internal data class DagannothCamp(
    val location: String,
    val plane: Int,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object Dagannoths {
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
     * How long a dagannoth stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`,
     * stated because a def built from `monsterStats.json` starts with a **0** timer, which
     * `NpcAggroPlugin` reads as "stop being aggressive".
     */
    const val AGGRO_TIMER = 1000

    /** Wiki `respawn = 30` on all four versions. */
    const val RESPAWN_CYCLES = 30

    const val WALK_RADIUS = 4

    /** How far the spine-throwing version attacks from, in tiles - the engine's ranged default. */
    const val SPINE_RANGE = 7

    /** `id1` - the level 74, the one that throws spines. */
    val LEVEL_74_IDS = listOf("npc.dagannoth_970", "npc.dagannoth_971", "npc.dagannoth_972")

    /** `id2` - the level 92, size 2, with 50 magic defence. */
    val LEVEL_92_IDS = listOf("npc.dagannoth_973", "npc.dagannoth_974", "npc.dagannoth_975")

    /** `id3` - the Catacombs of Kourend level 74. Defined but not placed; see [CAMPS]. */
    val CATACOMBS_74_IDS = listOf("npc.dagannoth_7259")

    /** `id4` - the Catacombs of Kourend level 92. Defined but not placed. */
    val CATACOMBS_92_IDS = listOf("npc.dagannoth_7260")

    val VARIANTS: List<DagannothVariant> =
        listOf(
            DagannothVariant("Dagannoth (level 74)", 74, 1, LEVEL_74_IDS, 70.0, ranged = true),
            DagannothVariant("Dagannoth (level 92)", 92, 2, LEVEL_92_IDS, 120.0, ranged = false),
            DagannothVariant("Dagannoth (level 74, Catacombs)", 74, 1, CATACOMBS_74_IDS, 70.0, ranged = false),
            DagannothVariant("Dagannoth (level 92, Catacombs)", 92, 2, CATACOMBS_92_IDS, 120.0, ranged = false),
        )

    /** Wiki tertiary. */
    const val ENSOULED_HEAD_ONE_IN = 40

    /** Wiki `DropsLineClue|type=medium`, with no altrarity published. */
    const val MEDIUM_CLUE_ONE_IN = 128

    /**
     * The one table, shared by all four versions - the page publishes a single `Drops` section
     * rather than one per version.
     *
     * Rows 75, seeds 18, gem 1, and the fishing section is eleven ordinary rows rather than a
     * sub-table, summing to the published 128.
     *
     * **`Dagannoth ribs` at 1/4 is not modelled**: "only dropped during Rag and Bone Man II", a
     * quest this server does not have, and a 1/4 drop is far too common to hand out just because its
     * condition is unbuilt. **The brimstone key** is not modelled either - it is conditioned on a
     * Konar quo Maten Slayer task, and Konar is not one of the six masters in
     * `data/cfg/slayer/masters.json`. Nor is the **Catacombs of Kourend tertiary table**.
     */
    val TABLE =
        MonsterDropTable(
            denominator = 128,
            seedWeight = 18,
            seedTable = SeedTableId.RARE,
            gemWeight = 1,
            rows =
                listOf(
                    // Weapons - 12.
                    drop("item.iron_spear", weight = 6),
                    drop("item.bronze_spear", weight = 5),
                    drop("item.mithril_spear", weight = 1),
                    // Runes and ammunition - 7.
                    drop("item.water_rune", 15, weight = 4),
                    drop("item.steel_arrow", 15, weight = 2),
                    drop("item.mithril_javelin", 3, weight = 1),
                    // Fishing - 35. A whole section of its own on this page, and the largest one.
                    drop("item.lobster_pot", weight = 12),
                    drop("item.raw_herring", 3, weight = 4),
                    drop("item.raw_sardine", 5, weight = 4),
                    drop("item.harpoon", weight = 3),
                    drop("item.feather", 15, weight = 2),
                    drop("item.fishing_bait", 50, weight = 2),
                    drop("item.raw_lobster", weight = 2),
                    drop("item.raw_tuna", weight = 2),
                    drop("item.seaweed", 10, weight = 2),
                    drop("item.oyster_pearls", weight = 1),
                    drop("item.oyster_pearl", 2, weight = 1),
                    // Coins - 52.
                    coins(56, weight = 29),
                    coins(25, weight = 9),
                    coins(44, weight = 8),
                    coins(41, weight = 6),
                    // Other - 3.
                    drop("item.opal_bolt_tips", 12, weight = 2),
                    drop("item.casket", weight = 1),
                ),
        )

    /**
     * The published `LocLine`s this rev-228 cache can hold - which is one of the three.
     *
     * The Lighthouse line carries `levels = 74, 92`, so both live versions stand in it and the pins
     * are dealt across the two id pools together, alternating. That is the wiki's own reading: it
     * gives one list of pins for two levels and does not say which is which.
     *
     * **Not here**: **Jormungand's Prison**, later Fremennik content whose mapsquare this cache does
     * not ship; and the **Catacombs of Kourend**, whose two versions keep their variants - stats,
     * respawn, Slayer experience and the shared table are all wired - but which has no entrance
     * built.
     */
    val CAMPS: List<DagannothCamp> =
        listOf(
            DagannothCamp(
                location = "Lighthouse",
                plane = 0,
                npcKeys = LEVEL_74_IDS + LEVEL_92_IDS,
                tiles =
                    listOf(
                        2511 to 10020, 2513 to 10018, 2514 to 10012, 2514 to 10027, 2515 to 10016,
                        2515 to 10030, 2518 to 10013, 2518 to 10027, 2519 to 10031, 2520 to 10018,
                        2521 to 10015, 2522 to 10023, 2522 to 10028, 2523 to 10019, 2524 to 10014,
                        2524 to 10033, 2526 to 10035, 2527 to 10019, 2527 to 10024, 2532 to 10023,
                        2534 to 10025, 2535 to 10030,
                    ),
            ),
        )

    /** Every dagannoth key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }
}
