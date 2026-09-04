package org.alter.plugins.content.npcs.dwarf

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The dwarf drop table - one table shared by all fourteen versions, which is how the page publishes
 * it: a single `Drops` section, no `dropversion` splits.
 *
 * ## It sums to exactly 128
 *
 * Weapons 20, runes and ammunition 15, coins 37, other 55, gem 1 - 128 to the unit, with the
 * `{{GemDropTable|1/128}}` line counted as a **row** rather than an extra roll. That is the reading
 * `content/npcs/zombie` established and [MonsterDropTable] generalises, and the exact sum is what
 * makes it right here: rolling the gem table separately would have inflated every other row.
 *
 * Both coin rows carrying a `raritynotes` about the level 20 variant ("Rate increases to 1/5.57",
 * "Rate decreases to 1/10.7") are used at their **published** 20/128 and 15/128. The notes describe
 * a different distribution for one of the fourteen versions and give no second full table to build
 * it from; 20 and 15 are the numbers that make the section total 128, so they are what is modelled,
 * and the level 20 dwarf gets the same coin rates as the rest.
 *
 * ## What is not modelled
 *
 * - **The beginner clue scroll's exemption.** `1/100`, "Not dropped by the level 20 variant" - that
 *   one *is* modelled, in [DwarfPlugin], because it is a plain per-variant condition rather than a
 *   distribution the page does not publish.
 * - **The ring of wealth column** on the gem table, which [org.alter.plugins.content.npcs.GemDropTable]
 *   already documents as absent for want of any ring-of-wealth behaviour to hang it on.
 *
 * The **bronze bolts** row is members-only (`{{(m)}}`, "Only dropped in members worlds") and is
 * included, the call every monster package in this tree makes: this server already runs members
 * content, and the row is one of the fifteen that make the section reach 128.
 */
internal object DwarfDrops {
    /** Wiki tertiary. Not dropped by `Standard (Level 20)` - see [DwarfPlugin]. */
    const val BEGINNER_CLUE_ONE_IN = 100

    /** The one table, published out of 128 and reaching it exactly. */
    val TABLE =
        MonsterDropTable(
            denominator = 128,
            gemWeight = 1,
            rows =
                listOf(
                    // Weapons and armour - 20.
                    WeightedDrop(getRSCM("item.bronze_pickaxe"), 1, weight = 13),
                    WeightedDrop(getRSCM("item.bronze_med_helm"), 1, weight = 4),
                    WeightedDrop(getRSCM("item.bronze_battleaxe"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.iron_battleaxe"), 1, weight = 1),
                    // Runes and ammunition - 15.
                    WeightedDrop(getRSCM("item.bronze_bolts"), 2, 12, weight = 7),
                    WeightedDrop(getRSCM("item.chaos_rune"), 2, weight = 4),
                    WeightedDrop(getRSCM("item.nature_rune"), 2, weight = 4),
                    // Coins - 37.
                    WeightedDrop(getRSCM("item.coins_995"), 4, weight = 20),
                    WeightedDrop(getRSCM("item.coins_995"), 10, weight = 15),
                    WeightedDrop(getRSCM("item.coins_995"), 30, weight = 2),
                    // Other - 55.
                    WeightedDrop(item = null, weight = 23),
                    WeightedDrop(getRSCM("item.hammer"), 1, weight = 10),
                    WeightedDrop(getRSCM("item.bronze_bar"), 1, weight = 7),
                    WeightedDrop(getRSCM("item.iron_ore"), 1, weight = 4),
                    WeightedDrop(getRSCM("item.tin_ore"), 1, weight = 3),
                    WeightedDrop(getRSCM("item.copper_ore"), 1, weight = 3),
                    WeightedDrop(getRSCM("item.iron_bar"), 1, weight = 3),
                    WeightedDrop(getRSCM("item.coal"), 1, weight = 2),
                ),
        )
}
