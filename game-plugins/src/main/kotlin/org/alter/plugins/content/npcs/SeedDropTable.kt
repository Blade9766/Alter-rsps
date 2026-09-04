package org.alter.plugins.content.npcs

import org.alter.game.model.World
import org.alter.rscm.RSCM.getRSCM

/**
 * The seed drop tables - the third and largest of the game's shared drop sub-tables, alongside
 * [GemDropTable] and [HerbDropTable].
 *
 * Monsters never list seeds individually. A drop table carries a single templated row naming which
 * of these tables it reaches and how often (`{{AllotmentSeedDropTableInfo|26/128}}`,
 * `{{RareSeedDropTableInfo|18/116}}`), so the contents live here and every monster just points at
 * one. Seven of the sixteen Slayer Tower and Fremennik Slayer Dungeon monsters roll one, several at
 * large rates - a cave crawler reaches its table on better than one kill in five.
 *
 * Weights are the wiki's numerators, used as relative weights the way [DropRoll] expects. Six of the
 * eight tables sum exactly to their published denominator; [UNCOMMON] comes to 1046 against a stated
 * 1048, a two-unit gap in the source that changes no row's relative rate and is left as published
 * rather than fudged to fit.
 *
 * ## The general table is not one table
 *
 * [ALLOTMENT], [RARE], [UNCOMMON] and [TREE_HERB] are flat lists. The **general** seed drop table is
 * six sub-tables and a monster's own combat level decides which one it reaches:
 *
 * ```
 * roll = random(combatLevel * 10)
 * roll < 485 -> allotments      485..727 -> hops        728..849 -> flowers
 * 850..946   -> bushes          947..994 -> herbs       >= 995   -> special
 * ```
 *
 * That is the published mechanic, quoted from the wiki's own description, and it is implemented
 * exactly in [rollGeneral] rather than flattened. It matters: a level 29 rockslug can only ever roll
 * `random(290)`, which never reaches 485, so it drops allotment seeds and nothing else - while a
 * level 100+ monster reaches every tier. Flattening the six into one list would have handed the
 * rockslug herb and cactus seeds it can never actually drop.
 */
internal object SeedDropTable {
    private fun seed(
        item: String,
        min: Int = 1,
        max: Int = min,
        weight: Int,
    ) = WeightedDrop(getRSCM(item), min, max, weight)

    /** Published out of 128. Reached by the cave crawler. */
    val ALLOTMENT: List<WeightedDrop> =
        listOf(
            seed("item.potato_seed", 1, 4, weight = 64),
            seed("item.onion_seed", 1, 3, weight = 32),
            seed("item.cabbage_seed", 1, 3, weight = 16),
            seed("item.tomato_seed", 1, 2, weight = 8),
            seed("item.sweetcorn_seed", 1, 2, weight = 4),
            seed("item.strawberry_seed", weight = 2),
            seed("item.watermelon_seed", weight = 1),
            seed("item.snape_grass_seed", weight = 1),
        )

    /** Published out of 1090. Reached by the aberrant spectre, nechryael, turoth and kurask. */
    val RARE: List<WeightedDrop> =
        listOf(
            seed("item.toadflax_seed", weight = 216),
            seed("item.irit_seed", weight = 148),
            seed("item.belladonna_seed", weight = 143),
            seed("item.avantoe_seed", weight = 103),
            seed("item.poison_ivy_seed", weight = 101),
            seed("item.cactus_seed", weight = 96),
            seed("item.potato_cactus_seed", weight = 70),
            seed("item.kwuarm_seed", weight = 69),
            seed("item.snapdragon_seed", weight = 46),
            seed("item.cadantine_seed", weight = 32),
            seed("item.lantadyme_seed", weight = 23),
            seed("item.snape_grass_seed", 3, weight = 20),
            seed("item.dwarf_weed_seed", weight = 14),
            seed("item.torstol_seed", weight = 9),
        )

    /**
     * Published out of 1048; the rows here come to 1046. Nothing in this roster reaches it yet - it
     * is included because it is part of the same published section and a monster that wants it
     * should not have to add a table to use one.
     */
    val UNCOMMON: List<WeightedDrop> =
        listOf(
            seed("item.limpwurt_seed", weight = 137),
            seed("item.strawberry_seed", weight = 131),
            seed("item.marrentill_seed", weight = 125),
            seed("item.jangerberry_seed", weight = 92),
            seed("item.tarromin_seed", weight = 85),
            seed("item.wildblood_seed", weight = 83),
            seed("item.watermelon_seed", weight = 63),
            seed("item.harralander_seed", weight = 56),
            seed("item.snape_grass_seed", weight = 40),
            seed("item.ranarr_seed", weight = 39),
            seed("item.whiteberry_seed", weight = 34),
            seed("item.mushroom_spore", weight = 29),
            seed("item.toadflax_seed", weight = 27),
            seed("item.belladonna_seed", weight = 18),
            seed("item.irit_seed", weight = 18),
            seed("item.poison_ivy_seed", weight = 13),
            seed("item.avantoe_seed", weight = 12),
            seed("item.cactus_seed", weight = 12),
            seed("item.kwuarm_seed", weight = 9),
            seed("item.potato_cactus_seed", weight = 8),
            seed("item.snapdragon_seed", weight = 5),
            seed("item.cadantine_seed", weight = 4),
            seed("item.lantadyme_seed", weight = 3),
            seed("item.dwarf_weed_seed", weight = 2),
            seed("item.torstol_seed", weight = 1),
        )

    /** Published out of 250. Nothing in this roster reaches it - included for completeness. */
    val TREE_HERB: List<WeightedDrop> =
        listOf(
            seed("item.ranarr_seed", weight = 30),
            seed("item.snapdragon_seed", weight = 28),
            seed("item.torstol_seed", weight = 22),
            seed("item.watermelon_seed", 15, weight = 21),
            seed("item.willow_seed", weight = 20),
            seed("item.mahogany_seed", weight = 18),
            seed("item.maple_seed", weight = 18),
            seed("item.teak_seed", weight = 18),
            seed("item.yew_seed", weight = 18),
            seed("item.papaya_tree_seed", weight = 14),
            seed("item.magic_seed", weight = 11),
            seed("item.palm_tree_seed", weight = 10),
            seed("item.spirit_seed", weight = 8),
            seed("item.dragonfruit_tree_seed", weight = 6),
            seed("item.celastrus_seed", weight = 4),
            seed("item.redwood_tree_seed", weight = 4),
        )

    /** General table, tier 1 - allotments. Published out of 1008. */
    private val GENERAL_ALLOTMENTS: List<WeightedDrop> =
        listOf(
            seed("item.potato_seed", 4, weight = 368),
            seed("item.onion_seed", 4, weight = 276),
            seed("item.cabbage_seed", 4, weight = 184),
            seed("item.tomato_seed", 3, weight = 92),
            seed("item.sweetcorn_seed", 3, weight = 46),
            seed("item.strawberry_seed", 2, weight = 23),
            seed("item.watermelon_seed", 2, weight = 11),
            seed("item.snape_grass_seed", 2, weight = 8),
        )

    /** General table, tier 2 - hops. Published out of 1000. */
    private val GENERAL_HOPS: List<WeightedDrop> =
        listOf(
            seed("item.barley_seed", 4, weight = 229),
            seed("item.hammerstone_seed", 3, weight = 228),
            seed("item.asgarnian_seed", 3, weight = 172),
            seed("item.jute_seed", 2, weight = 171),
            seed("item.yanillian_seed", 2, weight = 114),
            seed("item.krandorian_seed", 2, weight = 57),
            seed("item.wildblood_seed", weight = 29),
        )

    /** General table, tier 3 - flowers. Published out of 1000. */
    private val GENERAL_FLOWERS: List<WeightedDrop> =
        listOf(
            seed("item.marigold_seed", weight = 376),
            seed("item.nasturtium_seed", weight = 249),
            seed("item.rosemary_seed", weight = 161),
            seed("item.woad_seed", weight = 119),
            seed("item.limpwurt_seed", weight = 95),
        )

    /** General table, tier 4 - bushes. Published out of 1000. */
    private val GENERAL_BUSHES: List<WeightedDrop> =
        listOf(
            seed("item.redberry_seed", weight = 400),
            seed("item.cadavaberry_seed", weight = 280),
            seed("item.dwellberry_seed", weight = 200),
            seed("item.jangerberry_seed", weight = 80),
            seed("item.whiteberry_seed", weight = 29),
            seed("item.poison_ivy_seed", weight = 11),
        )

    /** General table, tier 5 - herbs. Published out of 1000. */
    private val GENERAL_HERBS: List<WeightedDrop> =
        listOf(
            seed("item.guam_seed", weight = 320),
            seed("item.marrentill_seed", weight = 218),
            seed("item.tarromin_seed", weight = 149),
            seed("item.harralander_seed", weight = 101),
            seed("item.ranarr_seed", weight = 69),
            seed("item.toadflax_seed", weight = 47),
            seed("item.irit_seed", weight = 32),
            seed("item.avantoe_seed", weight = 22),
            seed("item.kwuarm_seed", weight = 15),
            seed("item.snapdragon_seed", weight = 10),
            seed("item.cadantine_seed", weight = 7),
            seed("item.lantadyme_seed", weight = 5),
            seed("item.dwarf_weed_seed", weight = 3),
            seed("item.torstol_seed", weight = 2),
        )

    /** General table, tier 6 - special. Published out of 1100. */
    private val GENERAL_SPECIAL: List<WeightedDrop> =
        listOf(
            seed("item.mushroom_spore", weight = 500),
            seed("item.belladonna_seed", weight = 300),
            seed("item.cactus_seed", weight = 200),
            seed("item.potato_cactus_seed", weight = 100),
        )

    /**
     * Roll the general seed drop table for a monster of [combatLevel].
     *
     * The tier is chosen first, by the published `random(combatLevel * 10)` roll against the
     * thresholds, and only then is a seed picked out of that tier. A weak monster simply cannot
     * reach the upper tiers: anything below combat level 49 never rolls 485 and is locked to
     * allotments, which is the mechanic doing its job, not a bug.
     */
    fun rollGeneral(
        combatLevel: Int,
        world: World,
    ): WeightedDrop? = DropRoll.pick(generalTier(combatLevel, world), world)

    /**
     * Touch every table so their rscm keys resolve now.
     *
     * These are `object` fields, so without this the first time any of them is read is inside an
     * `onNpcDeath` handler - and a mistyped item key would surface there as an exception during a
     * kill, long after start-up, rather than as a plugin that refuses to load. Called from
     * [org.alter.plugins.content.npcs.slayer.SlayerMonsterPlugin]'s constructor.
     *
     * @return the total number of rows across all eight tables, so a caller can log it.
     */
    fun warmUp(): Int =
        listOf(
            ALLOTMENT, RARE, UNCOMMON, TREE_HERB,
            GENERAL_ALLOTMENTS, GENERAL_HOPS, GENERAL_FLOWERS,
            GENERAL_BUSHES, GENERAL_HERBS, GENERAL_SPECIAL,
        ).sumOf { it.size }

    private fun generalTier(
        combatLevel: Int,
        world: World,
    ): List<WeightedDrop> =
        when (world.random(combatLevel * 10)) {
            in 0..484 -> GENERAL_ALLOTMENTS
            in 485..727 -> GENERAL_HOPS
            in 728..849 -> GENERAL_FLOWERS
            in 850..946 -> GENERAL_BUSHES
            in 947..994 -> GENERAL_HERBS
            else -> GENERAL_SPECIAL
        }
}
