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
            // Nothing. Pads the table to the 128 its own rows are published out of.
            WeightedDrop(item = null, weight = 19),
        )

    /** Skeleton (level 22). Wiki rarities are out of 6/5000. */
    val SKELETON_LEVEL_22: List<WeightedDrop> = emptyList()

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
            // Nothing. Pads the table to the 128 its own rows are published out of.
            WeightedDrop(item = null, weight = 10),
        )

    /** Skeleton (level 25). Wiki rarities are out of 6/5000. */
    val SKELETON_LEVEL_25: List<WeightedDrop> = emptyList()

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
            // Nothing. Pads the table to the 128 its own rows are published out of.
            WeightedDrop(item = null, weight = 10),
        )

    /** Skeleton (level 45). Wiki rarities are out of 6/5000. */
    val SKELETON_LEVEL_45: List<WeightedDrop> = emptyList()

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
            // Nothing. Pads the table to the 128 its own rows are published out of.
            WeightedDrop(item = null, weight = 24),
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
            // Nothing. Pads the table to the 128 its own rows are published out of.
            WeightedDrop(item = null, weight = 5),
        )

    // The chaos druid's own table moved to `content/npcs/chaosdruid` along with the monster.

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
            // Nothing. Pads the table to the 128 its own rows are published out of.
            WeightedDrop(item = null, weight = 82),
        )

    /** Poison scorpion. Wiki rarities are out of 25. */
    val POISON_SCORPION: List<WeightedDrop> = emptyList()


    /**
     * Hill giant - the regular table, dropped everywhere except the Giants' Plateau.
     *
     * Weighted **out of 128**, which is how the wiki publishes every row on this page, and the
     * filler at the end is what makes that denominator real: the named rows come to 93, so 35
     * slots are nothing.
     *
     * Those 35 are not all "Nothing" on the wiki - most of them are the herb (7/128), seed
     * (18/128) and gem (3/128) tables, which OSRS rolls out of this same 128. This engine rolls
     * those three independently of the weighted table, in [DungeonMonsterPlugin] and in
     * `content/npcs/slayer` alike, so they are additive here rather than carved out of it. The
     * consequence is honest and small: a hill giant on this server drops a herb, seed or gem
     * slightly more often than one in OSRS, and every named row above lands at exactly its
     * published rate.
     *
     * Three things this table used to get wrong:
     *
     * 1. **It carried both sides of a location-exclusive split.** `iron med helm` (5/128) and
     *    `steel scimitar` (2/128) are flagged *"Only dropped by giants on the Giants' Plateau"*,
     *    and `iron full helm` (5/128) and `steel longsword` (2/128) *"Only dropped by giants not
     *    on the Giants' Plateau"*. All four were here, so any giant could roll either side and
     *    the table weighed 7/128 too much, thinning every other row. The Plateau half is now
     *    [HILL_GIANT_PLATEAU].
     * 2. **Tertiaries were folded in as ordinary rows, at weight 1 each.** In a table of ~94 that
     *    turns a 1/5000 champion scroll into roughly 1/94 - fifty times too common - the 1/400
     *    long bone into 1/94, and the 1/25 ensouled head into 1/94. They are independent rolls
     *    and now live in [DungeonMonsters]' `tertiaryDrops`.
     * 3. **The coin rows were an older version of the page.** They were 5 (18/128), 8 (6/128) and
     *    88 (2/128); the current table is 38 (14/128), 52 (10/128), 15 (8/128) and 88 (2/128),
     *    plus two rows - 5 coins at 18/128 and 10 coins at 7/128 - the wiki marks `{{(f)}}`,
     *    free-to-play only. Those two are **excluded**: this server runs members content
     *    throughout, and in a members world a hill giant does not drop them.
     */
    val HILL_GIANT: List<WeightedDrop> =
        listOf(
            // The wiki calls this a pre-roll rather than a table row, and doubles it to 2/128 in
            // the Wilderness. The doubling is handled as a Wilderness-only tertiary in
            // DungeonMonsters; the pre-roll ordering - key instead of a table roll, rather than as
            // well as - is not modelled.
            WeightedDrop(getRSCM("item.giant_key"), 1, 1, weight = 1),  // 1/128
            WeightedDrop(getRSCM("item.iron_dagger"), 1, 1, weight = 4),  // 4/128
            WeightedDrop(getRSCM("item.iron_full_helm"), 1, 1, weight = 5),  // 5/128
            WeightedDrop(getRSCM("item.iron_kiteshield"), 1, 1, weight = 3),  // 3/128
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
            WeightedDrop(getRSCM("item.coins_995"), 38, 38, weight = 14),  // 14/128
            WeightedDrop(getRSCM("item.coins_995"), 52, 52, weight = 10),  // 10/128
            WeightedDrop(getRSCM("item.coins_995"), 15, 15, weight = 8),  // 8/128
            WeightedDrop(getRSCM("item.coins_995"), 88, 88, weight = 2),  // 2/128
            WeightedDrop(getRSCM("item.limpwurt_root"), 1, 1, weight = 11),  // 11/128
            WeightedDrop(getRSCM("item.beer"), 1, 1, weight = 6),  // 6/128
            WeightedDrop(getRSCM("item.body_talisman"), 1, 1, weight = 2),  // 2/128
            // Nothing. See the note above on what these 35 stand for.
            WeightedDrop(item = null, weight = 35),
        )

    /**
     * The independent tertiary rolls both hill giant tables share, at their published chances.
     *
     * `Key (medium)` is **not** here: the wiki gives it "Always", conditional on holding a medium
     * clue step that asks for a hill giant. There are no clue steps on this server, so it would
     * have to drop on every kill or never - the same call `content/npcs/critters` made for the
     * chicken's copy of that key. It drops never.
     */
    val HILL_GIANT_TERTIARY: List<Pair<String, Double>> =
        listOf(
            "item.ensouled_giant_head" to 1.0 / 25.0,
            "item.clue_scroll_beginner" to 1.0 / 50.0,
            "item.long_bone" to 1.0 / 400.0,
            "item.giant_champion_scroll" to 1.0 / 5000.0,
            "item.curved_bone" to 1.0 / 5012.5,
        )

    /**
     * The Giants' Plateau hill giants, from the Ferox Enclave.
     *
     * Identical to [HILL_GIANT] but for the four rows the wiki splits on location: the Plateau
     * giants drop `iron med helm` (5/128) and `steel scimitar` (2/128) where the rest drop
     * `iron full helm` (5/128) and `steel longsword` (2/128). Same rarities on both sides, so
     * the tables have the same total weight - only the two item ids differ.
     */
    val HILL_GIANT_PLATEAU: List<WeightedDrop> =
        HILL_GIANT.map { drop ->
            when (drop.item) {
                getRSCM("item.iron_full_helm") -> drop.copy(item = getRSCM("item.iron_med_helm"))
                getRSCM("item.steel_longsword") -> drop.copy(item = getRSCM("item.steel_scimitar"))
                else -> drop
            }
        }

    /**
     * Magic axe - the **Catacombs of Kourend** battleaxe table, out of 500.
     *
     * This table belongs to npc 7269 alone. The wiki gives the magic axe two entirely separate
     * drop sections, and this is the second one: the Catacombs version rolls its battleaxe out of
     * these five, where the normal version (2844) simply drops an iron battleaxe every kill and
     * has no weighted table at all.
     *
     * Both were previously conflated onto both ids, which produced two wrong outcomes at once. A
     * normal magic axe dropped an iron battleaxe *twice* on most kills - once as its guaranteed
     * drop and again as the 475/500 row - and the Catacombs one paid out its guaranteed axe on
     * top of a roll that was supposed to replace it.
     */
    val MAGIC_AXE_CATACOMBS: List<WeightedDrop> =
        listOf(
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
            // Nothing. Pads the table to the 128 its own rows are published out of.
            WeightedDrop(item = null, weight = 42),
        )

    /** Poison spider. Wiki rarities are out of 3. */
    val POISON_SPIDER: List<WeightedDrop> = emptyList()


    /** Ice spider. Wiki rarities are out of 3/964. */
    val ICE_SPIDER: List<WeightedDrop> = emptyList()




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
            // Nothing. Pads the table to the 128 its own rows are published out of.
            WeightedDrop(item = null, weight = 29),
        )

    /** Ogre chieftain. Wiki rarities are out of 4/30/400. */
    val OGRE_CHIEFTAIN: List<WeightedDrop> = emptyList()


    /**
     * The independent tertiary rolls, by monster, at the chances their pages publish.
     *
     * These used to be rows in the weighted tables above, at `weight = 1` each, which is not what
     * a tertiary is. A weighted row's chance is its weight over the table's total, so a 1/5000
     * champion scroll sitting in a 127-weight table dropped at about 1/127 - and where a table
     * held nothing *but* tertiaries, the single row was picked every single kill. The measured
     * damage before this was:
     *
     * - Ice spider's `tooth half of key`: 1/964 published, **1/2 actual** - 482x too common.
     * - Ogre chieftain's `long bone`: 1/400 published, 1/3 actual - 133x.
     * - Lesser demon's champion scroll: 1/5000 published, 1/127 actual - 39x.
     * - Poison scorpion's ensouled head and poison spider's looting bag: **every kill**, because
     *   each was the only row in its table.
     *
     * And in the other direction, the rarer-than-published half: every looting bag was rolled out
     * of its monster's whole table (1/87 to 1/127) rather than at 1/3 or 1/9, and was dropped
     * anywhere rather than only in the Wilderness.
     */
    val DWARF_TERTIARY: List<Triple<String, Double, Boolean>> = emptyList()
    val CHAOS_DWARF_TERTIARY = listOf(Triple("item.looting_bag", 1.0 / 3.0, true))
    val CHAOS_DRUID_WARRIOR_TERTIARY = listOf(Triple("item.ensouled_chaos_druid_head", 1.0 / 25.0, false))
    val POISON_SCORPION_TERTIARY = listOf(Triple("item.ensouled_scorpion_head", 1.0 / 25.0, false))
    val BLACK_KNIGHT_TERTIARY = listOf(Triple("item.looting_bag", 1.0 / 9.0, true))
    val POISON_SPIDER_TERTIARY = listOf(Triple("item.looting_bag", 1.0 / 3.0, true))
    val ICE_SPIDER_TERTIARY =
        listOf(
            Triple("item.looting_bag", 1.0 / 3.0, true),
            Triple("item.tooth_half_of_key", 1.0 / 964.0, false),
        )
    val BLACK_DEMON_TERTIARY =
        listOf(
            Triple("item.looting_bag", 1.0 / 3.0, true),
            Triple("item.ensouled_demon_head", 1.0 / 35.0, false),
        )
    val OGRE_CHIEFTAIN_TERTIARY =
        listOf(
            Triple("item.ogre_ribs", 1.0 / 4.0, false),
            Triple("item.ensouled_ogre_head", 1.0 / 30.0, false),
            Triple("item.long_bone", 1.0 / 400.0, false),
        )

    // Skeleton tertiaries, moved out of the weighted tables above. The level 22 and level 25
    // and level 45 unarmed tables held nothing else at all, so their champion scroll - published
    // at 1/5000 - was dropping on one kill in two.
    val SKELETON_LEVEL_22_UNARMED_TERTIARY = listOf(Triple("item.looting_bag", 1.0 / 14, true), Triple("item.skeleton_champion_scroll", 1.0 / 5000, false))
    val SKELETON_LEVEL_22_TERTIARY = listOf(Triple("item.looting_bag", 1.0 / 6, true), Triple("item.skeleton_champion_scroll", 1.0 / 5000, false))
    val SKELETON_LEVEL_25_ARMED_TERTIARY = listOf(Triple("item.looting_bag", 1.0 / 14, true), Triple("item.skeleton_champion_scroll", 1.0 / 5000, false))
    val SKELETON_LEVEL_25_TERTIARY = listOf(Triple("item.looting_bag", 1.0 / 6, true), Triple("item.skeleton_champion_scroll", 1.0 / 5000, false))
    val SKELETON_LEVEL_45_ARMED_TERTIARY = listOf(Triple("item.looting_bag", 1.0 / 14, true), Triple("item.skeleton_champion_scroll", 1.0 / 5000, false))
    val SKELETON_LEVEL_45_TERTIARY = listOf(Triple("item.looting_bag", 1.0 / 6, true), Triple("item.skeleton_champion_scroll", 1.0 / 5000, false))
}
