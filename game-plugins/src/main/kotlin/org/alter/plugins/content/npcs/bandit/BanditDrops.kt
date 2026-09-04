package org.alter.plugins.content.npcs.bandit

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The two bandit drop tables - one per `dropversion` the page publishes.
 *
 * ## Both sum exactly to their published denominator, on the members reading
 *
 * - **Level 22**: weapons 7, runes 15, herbs 37, coins 58, other 8, gem 3 - **128**.
 * - **Level 130**: weapons 23, runes 15, herbs 41, coins 52, other 8, gem 4 - **143**, which is the
 *   denominator every row on that section is published against.
 *
 * Both totals count the `{{HerbDropTableInfo}}` and `{{GemDropTable}}` lines as **rows** rather than
 * extra rolls, which is the reading `content/npcs/zombie` established and [MonsterDropTable]
 * generalises. Reaching them requires the members column on the level 22 coins - dropping its
 * free-to-play-only `10 coins, 37/128` row, without which the section comes to 165. That the two
 * tables then land on 128 and 143 to the unit is what makes it a fact about the source rather than a
 * house preference.
 *
 * The level 130 table is the only one in this tree published against something other than 128, which
 * is why [MonsterDropTable] takes the denominator rather than assuming it.
 *
 * ## What is not modelled
 *
 * - **The Wilderness Slayer tertiary tables** on both versions - `{{WildernessSlayerDropTable}}`,
 *   and on the level 130 the **Larran's key** (1/90) and **Slayer's enchantment** (1/196) rows. All
 *   of it is gated on holding a task from **Krystilia**, who does not exist on this server, so the
 *   whole route is unreachable and deliberately unmodelled - the same call `content/npcs/chaosdruid`
 *   makes.
 * - **The hard clue's ring of wealth (i) rate**, 1/64 against the base 1/128. There is no
 *   ring-of-wealth behaviour in this codebase, which
 *   [org.alter.plugins.content.npcs.GemDropTable] already documents for the gem table's own
 *   ring-of-wealth column.
 */
internal object BanditDrops {
    /** Wiki tertiary on the level 130 only. */
    const val HARD_CLUE_ONE_IN = 128

    /** `Level 22 drops`. Rows 88, herb 37, gem 3 - 128. */
    val LEVEL_22 =
        MonsterDropTable(
            denominator = 128,
            herbWeight = 37,
            gemWeight = 3,
            rows =
                listOf(
                    // Weapons and armour - 7.
                    WeightedDrop(getRSCM("item.iron_scimitar"), 1, weight = 4),
                    WeightedDrop(getRSCM("item.steel_sq_shield"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.steel_axe"), 1, weight = 1),
                    // Runes - 15.
                    WeightedDrop(getRSCM("item.chaos_rune"), 6, weight = 3),
                    WeightedDrop(getRSCM("item.water_rune"), 9, weight = 3),
                    WeightedDrop(getRSCM("item.air_rune"), 10, weight = 2),
                    WeightedDrop(getRSCM("item.death_rune"), 2, weight = 2),
                    WeightedDrop(getRSCM("item.law_rune"), 3, weight = 2),
                    WeightedDrop(getRSCM("item.blood_rune"), 2, weight = 1),
                    WeightedDrop(getRSCM("item.mind_rune"), 2, weight = 1),
                    WeightedDrop(getRSCM("item.nature_rune"), 2, weight = 1),
                    // Coins - 58. The free-to-play 10-coin row is deliberately absent; see above.
                    WeightedDrop(getRSCM("item.coins_995"), 35, weight = 26),
                    WeightedDrop(getRSCM("item.coins_995"), 12, weight = 13),
                    WeightedDrop(getRSCM("item.coins_995"), 53, weight = 10),
                    WeightedDrop(getRSCM("item.coins_995"), 1, weight = 7),
                    WeightedDrop(getRSCM("item.coins_995"), 80, weight = 2),
                    // Other - 8.
                    WeightedDrop(getRSCM("item.coal"), 1, weight = 6),
                    WeightedDrop(item = null, weight = 2),
                ),
        )

    /**
     * `Level 130 drops (members only)`. Rows 98, herb 41, gem 4 - 143.
     *
     * The rune full helm, rune med helm, rune scimitar and dragon longsword rows are the 10 April
     * 2024 "Undead Pirates, Colosseum Changes & more!" additions, which is what pushed this
     * version's denominator to 143.
     */
    val LEVEL_130 =
        MonsterDropTable(
            denominator = 143,
            herbWeight = 41,
            gemWeight = 4,
            rows =
                listOf(
                    // Weapons and armour - 23.
                    WeightedDrop(getRSCM("item.rune_full_helm"), 1, weight = 5),
                    WeightedDrop(getRSCM("item.rune_med_helm"), 1, weight = 5),
                    WeightedDrop(getRSCM("item.rune_scimitar"), 1, weight = 5),
                    WeightedDrop(getRSCM("item.adamant_scimitar"), 1, weight = 4),
                    WeightedDrop(getRSCM("item.mithril_sq_shield"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.mithril_axe"), 1, weight = 1),
                    WeightedDrop(getRSCM("item.dragon_longsword"), 1, weight = 1),
                    // Runes - 15.
                    WeightedDrop(getRSCM("item.chaos_rune"), 10, weight = 3),
                    WeightedDrop(getRSCM("item.water_rune"), 11, weight = 3),
                    WeightedDrop(getRSCM("item.air_rune"), 25, weight = 2),
                    WeightedDrop(getRSCM("item.death_rune"), 4, weight = 2),
                    WeightedDrop(getRSCM("item.law_rune"), 6, weight = 2),
                    WeightedDrop(getRSCM("item.mind_rune"), 12, weight = 1),
                    WeightedDrop(getRSCM("item.blood_rune"), 6, weight = 1),
                    WeightedDrop(getRSCM("item.nature_rune"), 3, weight = 1),
                    // Coins - 52.
                    WeightedDrop(getRSCM("item.coins_995"), 35, weight = 26),
                    WeightedDrop(getRSCM("item.coins_995"), 120, weight = 13),
                    WeightedDrop(getRSCM("item.coins_995"), 53, weight = 10),
                    WeightedDrop(getRSCM("item.coins_995"), 250, weight = 2),
                    WeightedDrop(getRSCM("item.coins_995"), 10, weight = 1),
                    // Other - 8. This version has no `Nothing` row at all.
                    WeightedDrop(getRSCM("item.coal"), 5, weight = 6),
                    WeightedDrop(getRSCM("item.dark_fishing_bait"), 10, 24, weight = 2),
                ),
        )

    /** The table for [id]. */
    fun tableFor(id: BanditTableId): MonsterDropTable =
        when (id) {
            BanditTableId.LEVEL_22 -> LEVEL_22
            BanditTableId.LEVEL_130 -> LEVEL_130
        }
}
