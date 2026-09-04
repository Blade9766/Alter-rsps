package org.alter.plugins.content.npcs.hobgoblin

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.SeedTableId
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The two hobgoblin drop tables - one per `dropversion` the page publishes.
 *
 * ## Both sum to exactly 128, on the members reading
 *
 * - **Unarmed**: weapons 14, runes 14, herbs 7, seeds 18, coins 48, other 25, gem 2 - **128**.
 * - **Armed**: weapons 7, runes 15, herbs 7, seeds 12, coins 61, other 24, gem 2 - **128**.
 *
 * Both totals count the herb, seed and gem lines as **rows** rather than extra rolls, which is the
 * reading `content/npcs/zombie` established and [MonsterDropTable] generalises. Reaching 128
 * requires the members column everywhere it is published - dropping the free-to-play-only coin rows
 * and taking every `altrarity`:
 *
 * | row | free-to-play | members |
 * | --- | --- | --- |
 * | unarmed, 28 coins | 16/128 | **12/128** |
 * | unarmed, goblin mail | 5/128 | **2/128** |
 * | armed, 5 coins | 24/128 | **12/128** |
 * | armed, Nothing | 2/128 | **1/128** |
 *
 * Two independent tables landing on 128 to the unit under that reading, and on 145 and 156 under the
 * free-to-play one, is what makes it a fact about the source rather than a house preference.
 *
 * ## The two seed tables are different tables
 *
 * The unarmed hobgoblin reaches `{{GeneralSeedDropTableInfo|18/128}}` and the armed one
 * `{{UncommonSeedDropTableInfo|12/128}}`. Those are genuinely different lists, and the general one
 * is six sub-tables picked by the monster's own combat level - see
 * [org.alter.plugins.content.npcs.SeedDropTable]. At combat level 28 the tier roll is
 * `random(280)`, which never reaches 485, so an unarmed hobgoblin can only ever drop allotment
 * seeds. That is the published mechanic working, not a bug: flattening the six tiers into one list
 * would hand it herb and cactus seeds it cannot drop in the real game.
 *
 * ## What is not modelled
 *
 * - **Trading sticks.** Six rows, "Only dropped by Hobgoblins on Karamja, **in place of** the
 *   regular coin drops". Implementing it needs a per-kill test of whether the corpse is on Karamja,
 *   and Karamja plus Crandor is two disjoint boxes that nothing else in this codebase defines. Every
 *   hobgoblin drops the coin rows instead, which is what the other thirteen locations publish; the
 *   Tai Bwo Wannai and Crandor spawns are the only ones affected.
 * - **The ring of wealth column** on the gem table, which
 *   [org.alter.plugins.content.npcs.GemDropTable] already documents as absent.
 *
 * The **hobgoblin champion scroll** is kept at its real 1/5000 even though there is no Champions'
 * Challenge to hand it in to - the same call `content/npcs/goblin` and `content/npcs/zombie` make,
 * and for the same reason: faking a rarity to hide a missing system is worse than dropping an item
 * that currently only sits in a bank.
 */
internal object HobgoblinDrops {
    /** Wiki tertiary on both tables. */
    const val CHAMPION_SCROLL_ONE_IN = 5000

    /** Wiki tertiary on the unarmed table, Wilderness only. */
    const val LOOTING_BAG_ONE_IN = 5

    /** Wiki tertiary on the unarmed table. Not dropped in the God Wars Dungeon. */
    const val BEGINNER_CLUE_ONE_IN = 70

    /**
     * `Unarmed hobgoblin drops`. Rows 101, herb 7, seeds 18, gem 2 - 128.
     *
     * At 22/128 the limpwurt root is the commonest single item on it, which is exactly what the page
     * shows and why hobgoblins were ever a limpwurt farm.
     */
    val UNARMED =
        MonsterDropTable(
            denominator = 128,
            herbWeight = 7,
            seedWeight = 18,
            seedTable = SeedTableId.GENERAL,
            // `{{GeneralSeedDropLines|18/128|28}}` - the second argument is the combat level the
            // tier roll uses, and the unarmed versions are all level 28.
            combatLevel = 28,
            gemWeight = 2,
            rows =
                listOf(
                    // Weapons - 14.
                    WeightedDrop(getRSCM("item.bronze_spear"), 1, weight = 3),
                    WeightedDrop(getRSCM("item.iron_sword"), 1, weight = 3),
                    WeightedDrop(getRSCM("item.steel_dagger"), 1, weight = 3),
                    WeightedDrop(getRSCM("item.iron_spear"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.steel_spear"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.steel_longsword"), 1, weight = 1),
                    // Runes - 14.
                    WeightedDrop(getRSCM("item.law_rune"), 2, weight = 3),
                    WeightedDrop(getRSCM("item.water_rune"), 2, weight = 2),
                    WeightedDrop(getRSCM("item.fire_rune"), 7, weight = 2),
                    WeightedDrop(getRSCM("item.body_rune"), 6, weight = 2),
                    WeightedDrop(getRSCM("item.chaos_rune"), 3, weight = 2),
                    WeightedDrop(getRSCM("item.nature_rune"), 4, weight = 2),
                    WeightedDrop(getRSCM("item.cosmic_rune"), 2, weight = 1),
                    // Coins - 48. The two free-to-play-only rows are absent; the 28-coin row takes
                    // its members altrarity of 12.
                    WeightedDrop(getRSCM("item.coins_995"), 15, weight = 16),
                    WeightedDrop(getRSCM("item.coins_995"), 28, weight = 12),
                    WeightedDrop(getRSCM("item.coins_995"), 5, weight = 12),
                    WeightedDrop(getRSCM("item.coins_995"), 62, weight = 4),
                    WeightedDrop(getRSCM("item.coins_995"), 42, weight = 3),
                    WeightedDrop(getRSCM("item.coins_995"), 1, weight = 1),
                    // Other - 25. Goblin mail takes its members altrarity of 2.
                    WeightedDrop(getRSCM("item.limpwurt_root"), 1, weight = 22),
                    WeightedDrop(getRSCM("item.goblin_mail"), 1, weight = 2),
                    WeightedDrop(item = null, weight = 1),
                ),
        )

    /** `Armed hobgoblin drops`. Rows 107, herb 7, seeds 12, gem 2 - 128. */
    val ARMED =
        MonsterDropTable(
            denominator = 128,
            herbWeight = 7,
            seedWeight = 12,
            seedTable = SeedTableId.UNCOMMON,
            gemWeight = 2,
            rows =
                listOf(
                    // Weapons - 7. An armed hobgoblin drops no spears, which is the odd part: it is
                    // holding one.
                    WeightedDrop(getRSCM("item.iron_sword"), 1, weight = 3),
                    WeightedDrop(getRSCM("item.steel_dagger"), 1, weight = 3),
                    WeightedDrop(getRSCM("item.steel_longsword"), 1, weight = 1),
                    // Runes and ammunition - 15.
                    WeightedDrop(getRSCM("item.law_rune"), 2, weight = 3),
                    WeightedDrop(getRSCM("item.water_rune"), 2, weight = 2),
                    WeightedDrop(getRSCM("item.fire_rune"), 7, weight = 2),
                    WeightedDrop(getRSCM("item.body_rune"), 6, weight = 2),
                    WeightedDrop(getRSCM("item.chaos_rune"), 3, weight = 2),
                    WeightedDrop(getRSCM("item.nature_rune"), 4, weight = 2),
                    WeightedDrop(getRSCM("item.cosmic_rune"), 2, weight = 1),
                    WeightedDrop(getRSCM("item.iron_javelin"), 5, weight = 1),
                    // Coins - 61. The free-to-play 10-coin row is absent and the 5-coin row takes
                    // its members altrarity of 12. The page publishes two separate 1-coin rows, a
                    // members-only 3/128 and a plain 1/128; both are kept as published rather than
                    // merged, so the section's arithmetic can be checked against it line for line.
                    WeightedDrop(getRSCM("item.coins_995"), 15, weight = 34),
                    WeightedDrop(getRSCM("item.coins_995"), 5, weight = 12),
                    WeightedDrop(getRSCM("item.coins_995"), 28, weight = 4),
                    WeightedDrop(getRSCM("item.coins_995"), 62, weight = 4),
                    WeightedDrop(getRSCM("item.coins_995"), 42, weight = 3),
                    WeightedDrop(getRSCM("item.coins_995"), 1, weight = 3),
                    WeightedDrop(getRSCM("item.coins_995"), 1, weight = 1),
                    // Other - 24. Goblin mail and Nothing both take their members altrarity.
                    WeightedDrop(getRSCM("item.limpwurt_root"), 1, weight = 21),
                    WeightedDrop(getRSCM("item.goblin_mail"), 1, weight = 2),
                    WeightedDrop(item = null, weight = 1),
                ),
        )

    /** The table for [id]. */
    fun tableFor(id: HobgoblinTableId): MonsterDropTable =
        when (id) {
            HobgoblinTableId.UNARMED -> UNARMED
            HobgoblinTableId.ARMED -> ARMED
        }
}
