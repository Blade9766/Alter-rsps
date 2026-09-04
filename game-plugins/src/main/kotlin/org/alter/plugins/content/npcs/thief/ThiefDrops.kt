package org.alter.plugins.content.npcs.thief

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The thief drop table - rolled by the `Standard` thief and the four `Varrock Gang` thieves only.
 * The other four attackable versions drop bones and nothing else; see [ThiefVariant.rollsTable].
 *
 * ## It sums to exactly 128, on the members reading
 *
 * Rows 105 plus the `{{HerbDropTableInfo|23/128}}` line - 128 to the unit, with the herb line
 * counted as a **row** rather than an extra roll, which is the reading `content/npcs/zombie`
 * established and [MonsterDropTable] generalises.
 *
 * Reaching 128 requires dropping the **10-coin, 23/128** row, which the page marks "Only dropped in
 * free-to-play". With it the section comes to 151. That is not a house preference: 151 is not a
 * denominator anything else on the page uses, and 128 exactly is, so the members column is the
 * internally consistent set - the same argument the three zombie tables settled.
 *
 * ## What is not modelled
 *
 * - **Energy potion(3)**, added to several human npcs in the 19 August 2026 "Summer Sweep Up -
 *   Hunter & Skilling" update and published here with an **empty `rarity`**. There is no rate to
 *   implement, and the rest of the section already totals 128 without it, so it is left out rather
 *   than given an invented weight. Worth revisiting once the wiki fills the number in.
 * - **The ring of wealth column** on the shared sub-tables, which
 *   [org.alter.plugins.content.npcs.GemDropTable] already documents as absent for want of any
 *   ring-of-wealth behaviour to hang it on. (This table has no gem row of its own.)
 *
 * The **bronze bolts** row is the single commonest outcome at 22/128, which is exactly what the
 * page shows: a thief is mostly a bolt dispenser with a small chance of a bronze med helm.
 */
internal object ThiefDrops {
    /** Wiki tertiary, on the table-rolling versions. */
    const val BEGINNER_CLUE_ONE_IN = 90

    /** Wiki tertiary, `f2p=yes`, on the table-rolling versions. */
    const val EASY_CLUE_ONE_IN = 128

    /** Published out of 128 and reaching it exactly: rows 105, herb 23. */
    val TABLE =
        MonsterDropTable(
            denominator = 128,
            herbWeight = 23,
            rows =
                listOf(
                    // Weapons and armour - 3.
                    WeightedDrop(getRSCM("item.bronze_med_helm"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.iron_dagger"), 1, weight = 1),
                    // Runes and ammunition - 32.
                    WeightedDrop(getRSCM("item.bronze_bolts"), 2, 12, weight = 22),
                    WeightedDrop(getRSCM("item.bronze_arrow"), 7, weight = 3),
                    WeightedDrop(getRSCM("item.earth_rune"), 4, weight = 2),
                    WeightedDrop(getRSCM("item.fire_rune"), 6, weight = 2),
                    WeightedDrop(getRSCM("item.mind_rune"), 9, weight = 2),
                    WeightedDrop(getRSCM("item.chaos_rune"), 2, weight = 1),
                    // Coins - 52. The free-to-play 10-coin row is deliberately absent; see above.
                    WeightedDrop(getRSCM("item.coins_995"), 3, weight = 38),
                    WeightedDrop(getRSCM("item.coins_995"), 5, weight = 9),
                    WeightedDrop(getRSCM("item.coins_995"), 15, weight = 4),
                    WeightedDrop(getRSCM("item.coins_995"), 25, weight = 1),
                    // Other - 18.
                    WeightedDrop(item = null, weight = 8),
                    WeightedDrop(getRSCM("item.fishing_bait"), 1, weight = 5),
                    WeightedDrop(getRSCM("item.copper_ore"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.earth_talisman"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.cabbage"), 1, weight = 1),
                ),
        )
}
