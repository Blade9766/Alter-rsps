package org.alter.plugins.content.npcs.dungeon

import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The dungeon monsters' weighted drop tables, one per drop version. See [DungeonMonsters]
 * for how these attach and why the split is by drop version rather than combat level.
 *
 * Weights are the wiki's rarity numerators used as relative weights, the treatment
 * [org.alter.plugins.content.npcs.DropRoll] documents. Each table's comment records the
 * denominators its rows were published against; where a table mixes denominators the
 * numerators are still comparable, because every row on a given page is quoted against that
 * page's own roll.
 *
 * The `// n/d` comment on each row is the wiki's published rarity, kept so a row can be
 * checked against the source without opening it.
 *
 * **What is not here**, flagged rather than faked - consistent with the goblin, guard and
 * critter tables:
 * - **Herbs and seeds** (`HerbDropLines`, `GeneralSeedDropLines`) - template-expanded
 *   sub-tables whose contents are not on the monster pages, and there is no Herblore or
 *   Farming content to use them.
 * - **The mega-rare drop table**, reachable through the gem table - see
 *   [org.alter.plugins.content.npcs.GemDropTable].
 * - **Quest-gated rows**: rat bones and rat tails (Rag and Bone Man II, Witch's Potion),
 *   giant rat bones and skeleton bones (Rag and Bone Man I), the jailer's jail key, and
 *   clue-step keys. Each needs content that does not exist, so each would have to drop
 *   always or never; they drop never.
 * - **Clue scrolls**, which the wiki emits through a different template
 *   (`DropsLineClue`) than the rows parsed here. Worth adding once there is a clue system
 *   to receive them.
 *
 * **Five monsters have no weighted table at all**, and that is correct rather than missing:
 * the ghost, the suit of armour and the jailer publish none, and the baby dragons and the
 * hellhound publish only tertiary rows - clue scrolls, ensouled heads and the rare drop
 * table - none of which is modelled here. The hellhound is the one where that bites: its
 * real draw is the rare drop table, so on this server it drops only its guaranteed ashes.
 */
internal object DungeonDrops {
    /** Skeleton (level 22, unarmed). Wiki rarities are out of 14/128/5000. */
    val SKELETON_LEVEL_22_UNARMED: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.bronze_arrow"), 2, 2, weight = 7),  // 7/128
            WeightedDrop(getRSCM("item.bronze_arrow"), 5, 5, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.iron_arrow"), 1, 1, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.air_rune"), 15, 15, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.earth_rune"), 3, 3, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.fire_rune"), 2, 2, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.chaos_rune"), 3, 3, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.nature_rune"), 3, 3, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.steel_arrow"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.coins_995"), 10, 10, weight = 21),  // 21/128
            WeightedDrop(getRSCM("item.coins_995"), 2, 2, weight = 18),  // 18/128
            WeightedDrop(getRSCM("item.coins_995"), 12, 12, weight = 15),  // 15/128
            WeightedDrop(getRSCM("item.coins_995"), 4, 4, weight = 7),  // 7/128
            WeightedDrop(getRSCM("item.coins_995"), 16, 16, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.coins_995"), 25, 25, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.coins_995"), 33, 33, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.coins_995"), 48, 48, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.iron_dagger"), 1, 1, weight = 6),  // 6/128
            WeightedDrop(getRSCM("item.fire_talisman"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.iron_ore"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.grain"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/14
            WeightedDrop(getRSCM("item.skeleton_champion_scroll"), 1, 1, weight = 1),  // 1/5000
        )

    /** Skeleton (level 22). Wiki rarities are out of 6/5000. */
    val SKELETON_LEVEL_22: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/6
            WeightedDrop(getRSCM("item.skeleton_champion_scroll"), 1, 1, weight = 1),  // 1/5000
        )

    /** Skeleton (level 25, armed). Wiki rarities are out of 14/128/5000. */
    val SKELETON_LEVEL_25_ARMED: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.iron_med_helm"), 1, 1, weight = 6),  // 6/128
            WeightedDrop(getRSCM("item.iron_sword"), 1, 1, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.iron_axe"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.iron_scimitar"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.air_rune"), 1215, 1215, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.water_rune"), 9, 9, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.chaos_rune"), 5, 5, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.iron_arrow"), 12, 12, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.law_rune"), 2, 2, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.cosmic_rune"), 2, 2, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.coins_995"), 10, 10, weight = 44),  // 44/128
            WeightedDrop(getRSCM("item.coins_995"), 5, 5, weight = 25),  // 25/128
            WeightedDrop(getRSCM("item.coins_995"), 25, 25, weight = 8),  // 8/128
            WeightedDrop(getRSCM("item.coins_995"), 45, 45, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.coins_995"), 65, 65, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.coins_995"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.bronze_bar"), 1, 1, weight = 5),  // 5/128
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/14
            WeightedDrop(getRSCM("item.skeleton_champion_scroll"), 1, 1, weight = 1),  // 1/5000
        )

    /** Skeleton (level 25). Wiki rarities are out of 6/5000. */
    val SKELETON_LEVEL_25: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/6
            WeightedDrop(getRSCM("item.skeleton_champion_scroll"), 1, 1, weight = 1),  // 1/5000
        )

    /** Skeleton (level 45, armed). Wiki rarities are out of 14/128/5000. */
    val SKELETON_LEVEL_45_ARMED: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.iron_med_helm"), 1, 1, weight = 6),  // 6/128
            WeightedDrop(getRSCM("item.iron_sword"), 1, 1, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.iron_axe"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.iron_scimitar"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.air_rune"), 1215, 1215, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.water_rune"), 9, 9, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.chaos_rune"), 5, 5, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.iron_arrow"), 12, 12, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.law_rune"), 2, 2, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.cosmic_rune"), 2, 2, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.coins_995"), 10, 10, weight = 44),  // 44/128
            WeightedDrop(getRSCM("item.coins_995"), 5, 5, weight = 25),  // 25/128
            WeightedDrop(getRSCM("item.coins_995"), 25, 25, weight = 8),  // 8/128
            WeightedDrop(getRSCM("item.coins_995"), 45, 45, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.coins_995"), 65, 65, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.coins_995"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.bronze_bar"), 1, 1, weight = 5),  // 5/128
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/14
            WeightedDrop(getRSCM("item.skeleton_champion_scroll"), 1, 1, weight = 1),  // 1/5000
        )

    /** Skeleton (level 45). Wiki rarities are out of 6/5000. */
    val SKELETON_LEVEL_45: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/6
            WeightedDrop(getRSCM("item.skeleton_champion_scroll"), 1, 1, weight = 1),  // 1/5000
        )

    /** Ghost - the wiki publishes no weighted table for it. */
    val GHOST: List<WeightedDrop> = emptyList()

    /** Suit of armour - the wiki publishes no weighted table for it. */
    val SUIT_OF_ARMOUR: List<WeightedDrop> = emptyList()

    /** Dwarf. Wiki rarities are out of 128. */
    val DWARF: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.bronze_pickaxe"), 1, 1, weight = 13),  // 13/128
            WeightedDrop(getRSCM("item.bronze_med_helm"), 1, 1, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.bronze_battleaxe"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.iron_battleaxe"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.bronze_bolts"), 2, 12, weight = 7),  // 7/128
            WeightedDrop(getRSCM("item.chaos_rune"), 2, 2, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.nature_rune"), 2, 2, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.coins_995"), 4, 4, weight = 20),  // 20/128
            WeightedDrop(getRSCM("item.coins_995"), 10, 10, weight = 15),  // 15/128
            WeightedDrop(getRSCM("item.coins_995"), 30, 30, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.hammer"), 1, 1, weight = 10),  // 10/128
            WeightedDrop(getRSCM("item.bronze_bar"), 1, 1, weight = 7),  // 7/128
            WeightedDrop(getRSCM("item.iron_ore"), 1, 1, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.tin_ore"), 1, 1, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.copper_ore"), 1, 1, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.iron_bar"), 1, 1, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.coal"), 1, 1, weight = 2),  // 2/128
        )

    /** Chaos dwarf. Wiki rarities are out of 3/128. */
    val CHAOS_DWARF: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.steel_full_helm"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.mithril_longsword"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.mithril_sq_shield"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.law_rune"), 3, 3, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.air_rune"), 24, 24, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.chaos_rune"), 10, 10, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.mind_rune"), 37, 37, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.nature_rune"), 9, 9, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.cosmic_rune"), 3, 3, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.death_rune"), 3, 3, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.water_rune"), 10, 10, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.coins_995"), 92, 92, weight = 40),  // 40/128
            WeightedDrop(getRSCM("item.coins_995"), 47, 47, weight = 18),  // 18/128
            WeightedDrop(getRSCM("item.coins_995"), 25, 25, weight = 11),  // 11/128
            WeightedDrop(getRSCM("item.coins_995"), 150, 150, weight = 10),  // 10/128
            WeightedDrop(getRSCM("item.coins_995"), 350, 350, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.coins_995"), 15, 15, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.coal"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.muddy_key"), 1, 1, weight = 7),  // 7/128
            WeightedDrop(getRSCM("item.cheese"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.mithril_bar"), 1, 1, weight = 6),  // 6/128
            WeightedDrop(getRSCM("item.tomato"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/3
        )

    /** Chaos druid. Wiki rarities are out of 11/35/128. */
    val CHAOS_DRUID: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.law_rune"), 2, 2, weight = 7),  // 7/128
            WeightedDrop(getRSCM("item.mithril_bolts"), 2, 12, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.air_rune"), 36, 36, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.body_rune"), 9, 9, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.earth_rune"), 9, 9, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.mind_rune"), 12, 12, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.nature_rune"), 3, 3, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.coins_995"), 3, 3, weight = 5),  // 5/128
            WeightedDrop(getRSCM("item.coins_995"), 8, 8, weight = 5),  // 5/128
            WeightedDrop(getRSCM("item.coins_995"), 29, 29, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.coins_995"), 35, 35, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.vial_of_water"), 1, 1, weight = 10),  // 10/128
            WeightedDrop(getRSCM("item.bronze_longsword"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.snape_grass"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.unholy_mould"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/11
            WeightedDrop(getRSCM("item.ensouled_chaos_druid_head"), 1, 1, weight = 1),  // 1/35
        )

    /** Chaos druid warrior. Wiki rarities are out of 25/128. */
    val CHAOS_DRUID_WARRIOR: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.black_dagger"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.fire_rune"), 12, 12, weight = 5),  // 5/128
            WeightedDrop(getRSCM("item.law_rune"), 2, 2, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.earth_rune"), 9, 9, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.air_rune"), 36, 36, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.nature_rune"), 3, 3, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.white_berries"), 1, 1, weight = 5),  // 5/128
            WeightedDrop(getRSCM("item.unicorn_horn_dust"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.limpwurt_root"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.limpwurt_root"), 2, 2, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.snape_grass"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.vial_of_water"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.coins_995"), 3, 3, weight = 15),  // 15/128
            WeightedDrop(getRSCM("item.coins_995"), 29, 29, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.coins_995"), 10, 10, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.limpwurt_seed"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.super_defence1"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.ensouled_chaos_druid_head"), 1, 1, weight = 1),  // 1/25
        )

    /** Poison scorpion. Wiki rarities are out of 25. */
    val POISON_SCORPION: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.ensouled_scorpion_head"), 1, 1, weight = 1),  // 1/25
        )

    /** Hill giant. Wiki rarities are out of 5/25/128/400/5000. */
    val HILL_GIANT: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.giant_key"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.iron_dagger"), 1, 1, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.iron_med_helm"), 1, 1, weight = 5),  // 5/128
            WeightedDrop(getRSCM("item.iron_full_helm"), 1, 1, weight = 5),  // 5/128
            WeightedDrop(getRSCM("item.iron_kiteshield"), 1, 1, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.steel_scimitar"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.steel_longsword"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.iron_arrow"), 3, 3, weight = 6),  // 6/128
            WeightedDrop(getRSCM("item.fire_rune"), 15, 15, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.water_rune"), 7, 7, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.law_rune"), 2, 2, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.steel_arrow"), 10, 10, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.mind_rune"), 3, 3, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.cosmic_rune"), 2, 2, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.nature_rune"), 6, 6, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.chaos_rune"), 2, 2, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.death_rune"), 2, 2, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.coins_995"), 5, 5, weight = 18),  // 18/128
            WeightedDrop(getRSCM("item.coins_995"), 8, 8, weight = 6),  // 6/128
            WeightedDrop(getRSCM("item.coins_995"), 88, 88, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.limpwurt_root"), 1, 1, weight = 11),  // 11/128
            WeightedDrop(getRSCM("item.beer"), 1, 1, weight = 6),  // 6/128
            WeightedDrop(getRSCM("item.body_talisman"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/5
            WeightedDrop(getRSCM("item.ensouled_giant_head"), 1, 1, weight = 1),  // 1/25
            WeightedDrop(getRSCM("item.long_bone"), 1, 1, weight = 1),  // 1/400
            WeightedDrop(getRSCM("item.giant_champion_scroll"), 1, 1, weight = 1),  // 1/5000
        )

    /** Magic axe. Wiki rarities are out of 3/500. */
    val MAGIC_AXE: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/3
            WeightedDrop(getRSCM("item.iron_battleaxe"), 1, 1, weight = 475),  // 475/500
            WeightedDrop(getRSCM("item.steel_battleaxe"), 1, 1, weight = 10),  // 10/500
            WeightedDrop(getRSCM("item.mithril_battleaxe"), 1, 1, weight = 10),  // 10/500
            WeightedDrop(getRSCM("item.adamant_battleaxe"), 1, 1, weight = 4),  // 4/500
            WeightedDrop(getRSCM("item.rune_battleaxe"), 1, 1, weight = 1),  // 1/500
        )

    /** Jailer - the wiki publishes no weighted table for it. */
    val JAILER: List<WeightedDrop> = emptyList()

    /** Black Knight. Wiki rarities are out of 9/128. */
    val BLACK_KNIGHT: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.iron_sword"), 1, 1, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.iron_full_helm"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.steel_mace"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.mithril_arrow"), 3, 3, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.body_rune"), 9, 9, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.chaos_rune"), 6, 6, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.earth_rune"), 10, 10, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.death_rune"), 2, 2, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.law_rune"), 3, 3, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.cosmic_rune"), 7, 7, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.mind_rune"), 2, 2, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.steel_bar"), 1, 1, weight = 6),  // 6/128
            WeightedDrop(getRSCM("item.tin_ore"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.pot_of_flour"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.coins_995"), 35, 35, weight = 21),  // 21/128
            WeightedDrop(getRSCM("item.coins_995"), 6, 6, weight = 11),  // 11/128
            WeightedDrop(getRSCM("item.coins_995"), 58, 58, weight = 10),  // 10/128
            WeightedDrop(getRSCM("item.coins_995"), 12, 12, weight = 9),  // 9/128
            WeightedDrop(getRSCM("item.bread"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/9
        )

    /** Baby blue dragon - the wiki publishes no weighted table for it. */
    val BABY_BLUE_DRAGON: List<WeightedDrop> = emptyList()

    /** Baby black dragon - the wiki publishes no weighted table for it. */
    val BABY_BLACK_DRAGON: List<WeightedDrop> = emptyList()

    /** Poison spider. Wiki rarities are out of 3. */
    val POISON_SPIDER: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/3
        )

    /** Ice spider. Wiki rarities are out of 3/964. */
    val ICE_SPIDER: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/3
            WeightedDrop(getRSCM("item.tooth_half_of_key"), 1, 1, weight = 1),  // 1/964
        )

    /** Lesser demon. Wiki rarities are out of 3/50/128/5000. */
    val LESSER_DEMON: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.steel_full_helm"), 1, 1, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.steel_axe"), 1, 1, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.steel_scimitar"), 1, 1, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.mithril_sq_shield"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.mithril_chainbody"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.rune_med_helm"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.fire_rune"), 60, 60, weight = 8),  // 8/128
            WeightedDrop(getRSCM("item.chaos_rune"), 12, 12, weight = 5),  // 5/128
            WeightedDrop(getRSCM("item.death_rune"), 3, 3, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.fire_rune"), 30, 30, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.coins_995"), 120, 120, weight = 40),  // 40/128
            WeightedDrop(getRSCM("item.coins_995"), 40, 40, weight = 29),  // 29/128
            WeightedDrop(getRSCM("item.coins_995"), 200, 200, weight = 10),  // 10/128
            WeightedDrop(getRSCM("item.coins_995"), 10, 10, weight = 7),  // 7/128
            WeightedDrop(getRSCM("item.coins_995"), 450, 450, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.coins_995"), 10, 10, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.jug_of_wine"), 1, 1, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.gold_ore"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/3
            WeightedDrop(getRSCM("item.ensouled_demon_head"), 1, 1, weight = 1),  // 1/50
            WeightedDrop(getRSCM("item.lesser_demon_champion_scroll"), 1, 1, weight = 1),  // 1/5000
        )

    /** Greater demon. Wiki rarities are out of 3/40/128. */
    val GREATER_DEMON: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.steel_2h_sword"), 1, 1, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.steel_axe"), 1, 1, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.steel_battleaxe"), 1, 1, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.mithril_kiteshield"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.adamant_platelegs"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.rune_full_helm"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.fire_rune"), 75, 75, weight = 8),  // 8/128
            WeightedDrop(getRSCM("item.chaos_rune"), 15, 15, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.death_rune"), 5, 5, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.fire_rune"), 37, 37, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.coins_995"), 132, 132, weight = 40),  // 40/128
            WeightedDrop(getRSCM("item.coins_995"), 44, 44, weight = 29),  // 29/128
            WeightedDrop(getRSCM("item.coins_995"), 220, 220, weight = 10),  // 10/128
            WeightedDrop(getRSCM("item.coins_995"), 11, 11, weight = 7),  // 7/128
            WeightedDrop(getRSCM("item.coins_995"), 460, 460, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.tuna"), 1, 1, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.gold_bar"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.thread"), 10, 10, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/3
            WeightedDrop(getRSCM("item.ensouled_demon_head"), 1, 1, weight = 1),  // 1/40
        )

    /** Black demon. Wiki rarities are out of 3/35/128. */
    val BLACK_DEMON: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.black_sword"), 1, 1, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.steel_battleaxe"), 1, 1, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.black_axe"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.mithril_kiteshield"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.rune_med_helm"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.rune_chainbody"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.air_rune"), 50, 50, weight = 8),  // 8/128
            WeightedDrop(getRSCM("item.chaos_rune"), 10, 10, weight = 7),  // 7/128
            WeightedDrop(getRSCM("item.blood_rune"), 7, 7, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.fire_rune"), 37, 37, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.law_rune"), 3, 3, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.coins_995"), 132, 132, weight = 40),  // 40/128
            WeightedDrop(getRSCM("item.coins_995"), 30, 30, weight = 7),  // 7/128
            WeightedDrop(getRSCM("item.coins_995"), 44, 44, weight = 6),  // 6/128
            WeightedDrop(getRSCM("item.coins_995"), 220, 220, weight = 6),  // 6/128
            WeightedDrop(getRSCM("item.coins_995"), 460, 460, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.lobster"), 1, 1, weight = 3),  // 3/128
            WeightedDrop(getRSCM("item.adamantite_bar"), 1, 1, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.defence_potion3"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.looting_bag"), 1, 1, weight = 1),  // 1/3
            WeightedDrop(getRSCM("item.ensouled_demon_head"), 1, 1, weight = 1),  // 1/35
        )

    /** Hellhound - the wiki publishes no weighted table for it. */
    val HELLHOUND: List<WeightedDrop> = emptyList()

    /** Ogre chieftain. Wiki rarities are out of 4/30/400. */
    val OGRE_CHIEFTAIN: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.ogre_ribs"), 1, 1, weight = 1),  // 1/4
            WeightedDrop(getRSCM("item.ensouled_ogre_head"), 1, 1, weight = 1),  // 1/30
            WeightedDrop(getRSCM("item.long_bone"), 1, 1, weight = 1),  // 1/400
        )
}
