package org.alter.plugins.content.npcs.slayer

import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * Drop tables for the Slayer Tower and Fremennik Slayer Dungeon monsters.
 *
 * Same conventions as `content/npcs/dungeon`'s `DungeonDrops`:
 *
 * - **Weights are the wiki's rarity numerators, and every table adds up to its own published
 *   denominator.** That is not cosmetic: [org.alter.plugins.content.npcs.DropRoll] weights each
 *   row against the *table's* total, so a table that stops short inflates every row on it by the
 *   ratio. These tables used to stop short - the crawling hand summed to 80, so its leather gloves
 *   dropped at 21/80 (26%) rather than the published 21/128 (16%), and the aberrant spectre's 26
 *   made every row on it nearly five times too common. [filler] rows close the gap.
 * - **Denominators are per-page, not always 128.** The kurask publishes out of 124 and the
 *   nechryael out of 116 (doubled here, see below). Both add up exactly on their own scale.
 * - **A `Nothing` row appears only where the wiki publishes one.** The infernal mage, cave crawler,
 *   cockatrice and aberrant spectre have one, and [nothing] is used for those. It is deliberately a
 *   different function from [filler]: one is a drop outcome the page states, the other is bookkeeping
 *   for rows that are rolled elsewhere.
 * - **The gem and herb tables are rolled separately**, from
 *   [org.alter.plugins.content.npcs.GemDropTable] and
 *   [org.alter.plugins.content.npcs.HerbDropTable], at the per-monster rate on
 *   [SlayerMonster.gemTableChance] / [SlayerMonster.herbTableChance]. They are not folded in here.
 *
 * ## What is deliberately left out
 *
 * - Nothing seed-shaped. The seed tables live in
 *   [org.alter.plugins.content.npcs.SeedDropTable] and are reached through
 *   [SlayerMonster.seedRoll], the same way the gem and herb tables are - seven of these monsters
 *   roll one. `limpwurt seed` is the exception that stays here: the nechryael and kurask drop it as
 *   a *named row* rather than through a table.
 * - **`Necklace of fangs`** from the turoth: the item does not exist in this cache.
 * - **`Fire ruby`** from the infernal mage and **`Crystal shard`** from the kurask: both belong to
 *   content (Ruins of Camdozaal, Prifddinas) that is not built.
 * - **Brimstone keys, ensouled heads, ecumenical keys, elite clues and the wilderness looting
 *   bag.** Every one is conditional on something this server has no notion of - a Konar task, an
 *   Arceuus altar, the Wilderness - and several were reported by the wiki parse as "Always", which
 *   they plainly are not.
 * - **Noted quantities are dropped unnoted.** There is no note-on-drop mechanic here; a gargoyle's
 *   "15 steel bars (noted)" is fifteen steel bars.
 * - **The bloodveld's companion bones.** Its page lists `Bones 10/128` beside `Big bones 7/128`
 *   and `Big bones (3) 3/128`, with a footnote saying the bones come *together with* the big bones
 *   - the numerators are the same 10, so it is one outcome, not two. Held here as a weighted row it
 *   was both a phantom eleventh outcome and the reason that table summed to 133. The row is gone;
 *   the extra bones alongside a big-bones drop are not modelled, because a row cannot drop two
 *   items.
 *
 * The **nechryael's weights are the wiki's doubled**. Its table is published out of 116 and two coin
 * rows carry fractional numerators (10.5 and 2.5); doubling every row makes them integers without
 * changing a single relative rate, which beats rounding two rows and silently shifting the rest.
 */
internal object SlayerMonsterDrops {
    private fun drop(
        item: String,
        min: Int = 1,
        max: Int = min,
        weight: Int,
    ) = WeightedDrop(getRSCM(item), min, max, weight)

    private fun coins(
        min: Int,
        max: Int = min,
        weight: Int,
    ) = WeightedDrop(getRSCM("item.coins_995"), min, max, weight)

    private fun nothing(weight: Int) = WeightedDrop(item = null, weight = weight)

    /**
     * The share of a table's published denominator that the rows below do **not** account for,
     * as a no-drop row.
     *
     * [org.alter.plugins.content.npcs.DropRoll] weights each row against the table's own total,
     * so a table that stops short of its denominator inflates every row left in it. That is what
     * these are for: the gem, herb, seed and rare tables are rows *within* the published 128 but
     * are rolled separately here (see [SlayerMonsterPlugin]), so without a row standing in for
     * their share the main table would over-drop everything else by the ratio - a crawling hand's
     * leather gloves at 21/80 rather than the published 21/128.
     *
     * Distinct from [nothing], which is a `Nothing` row the wiki actually publishes.
     */
    private fun filler(weight: Int) = WeightedDrop(item = null, weight = weight)

    /** Shared by all three crawling hand levels - the wiki publishes one table for the family. */
    val CRAWLING_HAND: List<WeightedDrop> =
        listOf(
            coins(8, weight = 23),
            drop("item.leather_gloves", weight = 21),
            coins(5, weight = 21),
            drop("item.gold_ring", weight = 3),
            drop("item.purple_gloves", weight = 2),
            drop("item.yellow_gloves", weight = 2),
            drop("item.red_gloves", weight = 2),
            drop("item.teal_gloves", weight = 2),
            drop("item.sapphire_ring", weight = 2),
            drop("item.emerald_ring", weight = 2),
            // the page publishes only 82/128 - 2 of it the gem table - and never says what the other 46 is
            filler(weight = 48),
        )

    val BANSHEE: List<WeightedDrop> =
        listOf(
            drop("item.pure_essence", 13, weight = 22),
            drop("item.fishing_bait", 15, weight = 22),
            coins(13, weight = 10),
            coins(26, weight = 8),
            coins(35, weight = 8),
            drop("item.fishing_bait", 7, weight = 5),
            drop("item.air_rune", 3, weight = 3),
            drop("item.cosmic_rune", 2, weight = 3),
            drop("item.iron_mace", weight = 2),
            drop("item.iron_dagger", weight = 2),
            drop("item.chaos_rune", 3, weight = 2),
            drop("item.iron_kiteshield", weight = 1),
            drop("item.fire_rune", 7, weight = 1),
            drop("item.chaos_rune", 7, weight = 1),
            drop("item.iron_ore", weight = 1),
            drop("item.eye_of_newt", weight = 1),
            // herb table 34/128 + gem table 2/128
            filler(weight = 36),
        )

    val INFERNAL_MAGE: List<WeightedDrop> =
        listOf(
            coins(1, weight = 21),
            drop("item.death_rune", 7, weight = 18),
            coins(2, weight = 16),
            nothing(weight = 16),
            coins(4, weight = 9),
            drop("item.staff", weight = 8),
            drop("item.earth_rune", 10, weight = 6),
            drop("item.fire_rune", 10, weight = 6),
            drop("item.earth_rune", 36, weight = 4),
            drop("item.air_rune", 10, weight = 3),
            drop("item.water_rune", 10, weight = 3),
            coins(29, weight = 3),
            drop("item.air_rune", 18, weight = 2),
            drop("item.water_rune", 18, weight = 2),
            drop("item.earth_rune", 18, weight = 2),
            drop("item.fire_rune", 18, weight = 2),
            drop("item.mind_rune", 18, weight = 2),
            drop("item.body_rune", 18, weight = 2),
            drop("item.blood_rune", 4, weight = 2),
            drop("item.staff_of_fire", weight = 1),
        )

    val BLOODVELD: List<WeightedDrop> =
        listOf(
            coins(120, weight = 30),
            coins(40, weight = 29),
            coins(200, weight = 10),
            drop("item.fire_rune", 60, weight = 8),
            coins(10, weight = 7),
            drop("item.big_bones", weight = 7),
            drop("item.blood_rune", 10, weight = 5),
            drop("item.steel_axe", weight = 4),
            drop("item.steel_full_helm", weight = 4),
            drop("item.blood_rune", 3, weight = 3),
            drop("item.big_bones", 3, weight = 3),
            drop("item.meat_pizza", weight = 3),
            drop("item.steel_scimitar", weight = 2),
            drop("item.gold_ore", weight = 2),
            drop("item.black_boots", weight = 1),
            drop("item.mithril_sq_shield", weight = 1),
            drop("item.mithril_chainbody", weight = 1),
            drop("item.rune_med_helm", weight = 1),
            drop("item.blood_rune", 30, weight = 1),
            coins(450, weight = 1),
            // gem table 4/128 (1/32) + herb table 1/128
            filler(weight = 5),
        )

    val ABERRANT_SPECTRE: List<WeightedDrop> =
        listOf(
            nothing(weight = 18),
            drop("item.steel_axe", weight = 3),
            drop("item.mithril_kiteshield", weight = 1),
            drop("item.lava_battlestaff", weight = 1),
            drop("item.adamant_platelegs", weight = 1),
            drop("item.rune_full_helm", weight = 1),
            coins(460, weight = 1),
            // herb table 78/128 + seed table 19/128 + gem table 5/128
            filler(weight = 102),
        )

    val GARGOYLE: List<WeightedDrop> =
        listOf(
            coins(400, 800, weight = 28),
            coins(500, 1000, weight = 20),
            drop("item.fire_rune", 75, weight = 10),
            drop("item.gold_ore", 10, 20, weight = 10),
            drop("item.chaos_rune", 30, weight = 8),
            drop("item.fire_rune", 150, weight = 6),
            drop("item.pure_essence", 150, weight = 6),
            drop("item.steel_bar", 15, weight = 6),
            drop("item.death_rune", 15, weight = 5),
            coins(10000, weight = 5),
            drop("item.adamant_platelegs", weight = 4),
            drop("item.rune_full_helm", weight = 3),
            drop("item.gold_bar", 10, 15, weight = 3),
            drop("item.rune_2h_sword", weight = 2),
            drop("item.mithril_bar", 15, weight = 2),
            drop("item.runite_ore", weight = 2),
            drop("item.adamant_boots", weight = 1),
            drop("item.rune_battleaxe", weight = 1),
            drop("item.rune_platelegs", weight = 1),
            // gem table 5/128
            filler(weight = 5),
        )

    /** Published out of 116 with two fractional coin rows - every weight here is the wiki's x2. */
    val NECHRYAEL: List<WeightedDrop> =
        listOf(
            coins(1000, 1499, weight = 26),
            coins(1500, 2000, weight = 21),
            drop("item.chaos_rune", 37, weight = 16),
            drop("item.death_rune", 5, weight = 12),
            drop("item.death_rune", 10, weight = 12),
            coins(2500, 2999, weight = 12),
            drop("item.limpwurt_seed", weight = 12),
            drop("item.law_rune", 25, 35, weight = 10),
            drop("item.adamant_platelegs", weight = 8),
            drop("item.rune_2h_sword", weight = 8),
            drop("item.blood_rune", 15, 20, weight = 8),
            drop("item.soft_clay", 25, weight = 8),
            drop("item.rune_full_helm", weight = 6),
            coins(3000, 3500, weight = 6),
            drop("item.tuna", weight = 6),
            coins(500, 999, weight = 5),
            drop("item.adamant_kiteshield", weight = 4),
            drop("item.rune_boots", weight = 2),
            coins(5000, weight = 2),
            // seed table 18/116 + gem table 5/116 + rare table 1/116, doubled with the rest of this table
            filler(weight = 48),
        )

    val ABYSSAL_DEMON: List<WeightedDrop> =
        listOf(
            coins(132, weight = 35),
            coins(220, weight = 9),
            drop("item.air_rune", 50, weight = 8),
            drop("item.chaos_rune", 10, weight = 7),
            coins(30, weight = 7),
            coins(44, weight = 6),
            drop("item.pure_essence", 60, weight = 5),
            drop("item.black_sword", weight = 4),
            drop("item.blood_rune", 7, weight = 4),
            drop("item.steel_battleaxe", weight = 3),
            drop("item.black_axe", weight = 2),
            drop("item.adamantite_bar", weight = 2),
            drop("item.lobster", weight = 2),
            drop("item.mithril_kiteshield", weight = 1),
            drop("item.rune_chainbody", weight = 1),
            drop("item.rune_med_helm", weight = 1),
            drop("item.law_rune", 3, weight = 1),
            coins(460, weight = 1),
            drop("item.cosmic_talisman", weight = 1),
            drop("item.chaos_talisman", weight = 1),
            drop("item.defence_potion3", weight = 1),
            // herb table 19/128 + gem table 5/128 + rare table 2/128
            filler(weight = 26),
        )

    val CAVE_CRAWLER: List<WeightedDrop> =
        listOf(
            nothing(weight = 29),
            drop("item.vial_of_water", weight = 13),
            drop("item.nature_rune", 3, 4, weight = 6),
            drop("item.fire_rune", 12, weight = 5),
            coins(3, weight = 5),
            drop("item.white_berries", weight = 5),
            coins(8, weight = 3),
            coins(29, weight = 3),
            drop("item.earth_rune", 9, weight = 2),
            drop("item.unicorn_horn_dust", weight = 2),
            drop("item.bronze_boots", weight = 1),
            coins(10, weight = 1),
            drop("item.eye_of_newt", weight = 1),
            drop("item.red_spiders_eggs", weight = 1),
            drop("item.limpwurt_root", weight = 1),
            drop("item.snape_grass", weight = 1),
            // seed table 26/128 + herb table 22/128 + gem table 1/128
            filler(weight = 49),
        )

    val ROCKSLUG: List<WeightedDrop> =
        listOf(
            drop("item.earth_rune", 5, weight = 30),
            drop("item.iron_ore", weight = 22),
            drop("item.coal", weight = 13),
            drop("item.dwarven_stout", weight = 13),
            drop("item.hammer", weight = 10),
            drop("item.tin_ore", weight = 8),
            drop("item.earth_rune", 42, weight = 4),
            drop("item.chaos_rune", 2, weight = 4),
            drop("item.iron_bar", weight = 3),
            drop("item.copper_ore", weight = 3),
            drop("item.bronze_bar", weight = 2),
            drop("item.mithril_ore", weight = 1),
            // seed table 9/128 + gem table 6/128
            filler(weight = 15),
        )

    val COCKATRICE: List<WeightedDrop> =
        listOf(
            drop("item.limpwurt_root", weight = 21),
            coins(15, weight = 16),
            coins(5, weight = 12),
            coins(28, weight = 12),
            drop("item.nature_rune", 2, weight = 6),
            drop("item.nature_rune", 4, weight = 4),
            coins(62, weight = 4),
            drop("item.iron_sword", weight = 3),
            drop("item.steel_dagger", weight = 3),
            drop("item.law_rune", 2, weight = 3),
            coins(42, weight = 3),
            drop("item.nature_rune", 6, weight = 2),
            drop("item.water_rune", 2, weight = 2),
            drop("item.fire_rune", 7, weight = 2),
            drop("item.iron_boots", weight = 1),
            drop("item.iron_javelin", 5, weight = 1),
            drop("item.steel_longsword", weight = 1),
            coins(1, weight = 1),
            nothing(weight = 1),
            // seed table 18/128 + herb table 10/128 + gem table 2/128
            filler(weight = 30),
        )

    val PYREFIEND: List<WeightedDrop> =
        listOf(
            coins(40, weight = 24),
            drop("item.fire_rune", 30, weight = 21),
            coins(120, weight = 20),
            coins(200, weight = 10),
            drop("item.fire_rune", 60, weight = 8),
            drop("item.gold_ore", weight = 8),
            coins(10, weight = 7),
            drop("item.chaos_rune", 12, weight = 5),
            drop("item.steel_axe", weight = 4),
            drop("item.steel_full_helm", weight = 4),
            drop("item.staff_of_fire", weight = 3),
            drop("item.death_rune", 3, weight = 3),
            drop("item.mithril_chainbody", weight = 2),
            coins(450, weight = 2),
            drop("item.jug_of_wine", weight = 2),
            drop("item.steel_boots", weight = 1),
            drop("item.adamant_med_helm", weight = 1),
            // gem table 3/128
            filler(weight = 3),
        )

    val BASILISK: List<WeightedDrop> =
        listOf(
            coins(44, weight = 29),
            coins(200, weight = 17),
            drop("item.water_rune", 75, weight = 8),
            drop("item.nature_rune", 15, weight = 5),
            coins(132, weight = 5),
            coins(11, weight = 5),
            drop("item.mithril_axe", weight = 3),
            drop("item.steel_battleaxe", weight = 3),
            drop("item.law_rune", 3, weight = 3),
            drop("item.adamantite_ore", weight = 3),
            drop("item.mithril_spear", weight = 2),
            drop("item.adamant_full_helm", weight = 1),
            drop("item.mithril_kiteshield", weight = 1),
            drop("item.rune_dagger", weight = 1),
            drop("item.nature_rune", 37, weight = 1),
            coins(440, weight = 1),
            // herb table 35/128 + gem table 5/128
            filler(weight = 40),
        )

    val JELLY: List<WeightedDrop> =
        listOf(
            coins(102, weight = 39),
            coins(44, weight = 30),
            drop("item.steel_battleaxe", weight = 11),
            coins(220, weight = 10),
            drop("item.steel_2h_sword", weight = 7),
            coins(11, weight = 7),
            drop("item.chaos_rune", 15, weight = 5),
            drop("item.steel_axe", weight = 3),
            drop("item.death_rune", 5, weight = 3),
            drop("item.mithril_kiteshield", weight = 2),
            coins(460, weight = 2),
            drop("item.gold_bar", weight = 2),
            drop("item.mithril_boots", weight = 1),
            drop("item.rune_full_helm", weight = 1),
            drop("item.thread", 10, weight = 1),
            // gem table 4/128
            filler(weight = 4),
        )

    /** Shared by all four published turoth levels. */
    val TUROTH: List<WeightedDrop> =
        listOf(
            coins(44, weight = 29),
            coins(132, weight = 12),
            drop("item.steel_platelegs", weight = 7),
            drop("item.limpwurt_root", weight = 7),
            drop("item.law_rune", 3, weight = 6),
            drop("item.nature_rune", 15, weight = 5),
            drop("item.mithril_axe", weight = 3),
            drop("item.mithril_kiteshield", weight = 1),
            drop("item.adamant_full_helm", weight = 1),
            drop("item.rune_dagger", weight = 1),
            drop("item.nature_rune", 37, weight = 1),
            coins(440, weight = 1),
            // herb table 31/128 + seed table 18/128 + gem table 5/128
            filler(weight = 54),
        )

    val KURASK: List<WeightedDrop> =
        listOf(
            coins(2000, 3000, weight = 16),
            drop("item.nature_rune", 10, weight = 10),
            drop("item.nature_rune", 15, weight = 7),
            drop("item.limpwurt_seed", weight = 6),
            drop("item.flax", 100, weight = 6),
            drop("item.white_berries", 12, weight = 6),
            coins(10000, weight = 5),
            drop("item.big_bones", 20, weight = 5),
            drop("item.nature_rune", 30, weight = 4),
            drop("item.papaya_fruit", 10, weight = 4),
            drop("item.coconut", 10, weight = 4),
            drop("item.mithril_kiteshield", weight = 3),
            drop("item.rune_longsword", weight = 3),
            drop("item.adamant_platebody", weight = 3),
            drop("item.rune_axe", weight = 3),
            // herb table 18/124 + seed table 15/124 + gem table 6/124; this page publishes out of 124
            filler(weight = 39),
        )
}
