package org.alter.plugins.content.npcs.demon

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The demon drop tables - four of them, one per published drop version.
 *
 * ## Both `Regular` tables need the members reading to reach 128
 *
 * The lesser demon's `Coins` section publishes six rows: five ordinary ones and a sixth marked
 * `{{(f)}}`, free-to-play-only, at 1/128. Counting all six the table comes to **129**; dropping the
 * free-to-play row it comes to **128 exactly**. That is the same members-column reading
 * [MonsterDropTable] documents and that every table in this tree has needed, and it is a fact about
 * the source rather than a house preference: one reading lands on the published denominator and the
 * other lands on nothing.
 *
 * The greater demon's table needs no such choice - it sums to 128 as written, including its own
 * explicit `Nothing` row of 2/128.
 *
 * ## The Wilderness Slayer Cave tables are out of 39, not 128
 *
 * Both are published against a denominator of 39 and both reach it exactly. Nothing needed to be
 * rescaled; [MonsterDropTable] takes the denominator as a parameter for exactly this.
 *
 * ## What is not modelled
 *
 * - **The second set of vile ashes**: `quantity = 1;2` with the note "a second set of ashes is
 *   dropped when the demon is killed with preferred method in the Chasm of Fire". The Chasm of Fire
 *   is not built, so the preferred-method condition can never be met and one set is dropped.
 * - **The five Chasm of Fire contracts**, at 1/123 and 1/81. They are the reward economy of an area
 *   that does not exist here, keyed off sigils that do not exist either.
 * - **The Catacombs of Kourend and Wilderness Slayer tertiary tables**, whole shared tables nothing
 *   in this codebase implements - the gap `content/npcs/mossgiant` records.
 * - **The brimstone key** on the greater demon, conditioned on a Konar quo Maten Slayer task.
 *   `data/cfg/slayer/masters.json` has six masters and Konar is not one; there is no Brimstone chest
 *   to spend a key in either.
 *
 * The **lesser demon champion scroll** is kept at its real 1/5000 even though there is no Champions'
 * Challenge to hand it in to, the same call `content/npcs/goblin` makes about the goblin scroll.
 * There is no greater demon equivalent: that item does not exist in this cache, and the page does
 * not publish one.
 */
internal object DemonDrops {
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

    /** Wiki tertiary, Wilderness only on the `Regular` versions. */
    const val LOOTING_BAG_ONE_IN = 3

    /** Lesser demon tertiary. */
    const val LESSER_ENSOULED_HEAD_ONE_IN = 50

    /** Greater demon tertiary. */
    const val GREATER_ENSOULED_HEAD_ONE_IN = 40

    /** Lesser demon tertiary. The greater demon publishes no champion scroll. */
    const val LESSER_CHAMPION_SCROLL_ONE_IN = 5000

    /** Greater demon tertiary, `DropsLineClue|type=hard`. The lesser demon has no clue row at all. */
    const val GREATER_HARD_CLUE_ONE_IN = 128

    /** The greater demon clue's `altrarity`, on a worn ring of wealth (i). */
    const val GREATER_HARD_CLUE_WEALTH_ONE_IN = 64

    /** Lesser demon, `Regular` - rows 123, herbs 1, gem 4. */
    val LESSER =
        MonsterDropTable(
            denominator = 128,
            herbWeight = 1,
            gemWeight = 4,
            rows =
                listOf(
                    // Weapons and armour - 14.
                    drop("item.steel_full_helm", weight = 4),
                    drop("item.steel_axe", weight = 4),
                    drop("item.steel_scimitar", weight = 3),
                    drop("item.mithril_sq_shield", weight = 1),
                    drop("item.mithril_chainbody", weight = 1),
                    drop("item.rune_med_helm", weight = 1),
                    // Runes - 17.
                    drop("item.fire_rune", 60, weight = 8),
                    drop("item.chaos_rune", 12, weight = 5),
                    drop("item.death_rune", 3, weight = 3),
                    drop("item.fire_rune", 30, weight = 1),
                    // Coins - 87. The sixth published row, 10 coins at 1/128, is free-to-play only
                    // and is what the members reading drops; see the file doc.
                    coins(120, weight = 40),
                    coins(40, weight = 29),
                    coins(200, weight = 10),
                    coins(10, weight = 7),
                    coins(450, weight = 1),
                    // Other - 5.
                    drop("item.jug_of_wine", weight = 3),
                    drop("item.gold_ore", weight = 2),
                ),
        )

    /** Lesser demon, `Wilderness Slayer Cave` - published out of 39 and reaching it exactly. */
    val LESSER_WILDERNESS =
        MonsterDropTable(
            denominator = 39,
            herbWeight = 1,
            gemWeight = 5,
            rows =
                listOf(
                    // Weapons and armour - 12.
                    drop("item.black_full_helm", weight = 4),
                    drop("item.mithril_axe", weight = 4),
                    drop("item.adamant_dagger", weight = 1),
                    drop("item.adamant_chainbody", weight = 1),
                    drop("item.adamant_platebody", weight = 1),
                    drop("item.rune_med_helm", weight = 1),
                    // Runes - 10.
                    drop("item.chaos_rune", 20, weight = 5),
                    drop("item.death_rune", 10, weight = 3),
                    drop("item.death_rune", 30, weight = 1),
                    drop("item.blood_rune", 10, weight = 1),
                    // Coins - 6.
                    coins(100, weight = 6),
                    // Other - 5. The gold ore row is published noted; there is no note-on-drop
                    // mechanic here, so it drops as five ore.
                    drop("item.jug_of_wine", weight = 3),
                    drop("item.gold_ore", 5, weight = 2),
                ),
        )

    /** Greater demon, `Regular` - rows 123, gem 5, and no herb row at all. */
    val GREATER =
        MonsterDropTable(
            denominator = 128,
            gemWeight = 5,
            rows =
                listOf(
                    // Weapons and armour - 13.
                    drop("item.steel_2h_sword", weight = 4),
                    drop("item.steel_axe", weight = 3),
                    drop("item.steel_battleaxe", weight = 3),
                    drop("item.mithril_kiteshield", weight = 1),
                    drop("item.adamant_platelegs", weight = 1),
                    drop("item.rune_full_helm", weight = 1),
                    // Runes - 15.
                    drop("item.fire_rune", 75, weight = 8),
                    drop("item.chaos_rune", 15, weight = 3),
                    drop("item.death_rune", 5, weight = 3),
                    drop("item.fire_rune", 37, weight = 1),
                    // Coins - 87.
                    coins(132, weight = 40),
                    coins(44, weight = 29),
                    coins(220, weight = 10),
                    coins(11, weight = 7),
                    coins(460, weight = 1),
                    // Other - 8, including the page's own explicit Nothing row.
                    drop("item.tuna", weight = 3),
                    WeightedDrop(item = null, weight = 2),
                    drop("item.gold_bar", weight = 2),
                    drop("item.thread", 10, weight = 1),
                ),
        )

    /**
     * Greater demon, `Wilderness Slayer Cave` - published out of **41**, not 39 like the lesser
     * demon's, and reaching it exactly. Defined so the variant is complete; the area has no entrance
     * built, so nothing rolls it yet.
     */
    val GREATER_WILDERNESS =
        MonsterDropTable(
            denominator = 41,
            gemWeight = 5,
            rows =
                listOf(
                    // Weapons and armour - 12.
                    drop("item.adamant_2h_sword", weight = 4),
                    drop("item.adamant_axe", weight = 3),
                    drop("item.adamant_battleaxe", weight = 2),
                    drop("item.adamant_kiteshield", weight = 1),
                    drop("item.adamant_platelegs", weight = 1),
                    drop("item.rune_full_helm", weight = 1),
                    // Runes - 13.
                    drop("item.chaos_rune", 40, weight = 5),
                    drop("item.death_rune", 5, weight = 5),
                    drop("item.death_rune", 15, weight = 2),
                    drop("item.blood_rune", 20, weight = 1),
                    // Coins - 7.
                    coins(150, weight = 7),
                    // Other - 4. Both rows are published noted and drop unnoted, as everywhere here.
                    drop("item.gold_bar", 10, weight = 3),
                    drop("item.gold_ore", 10, weight = 1),
                ),
        )

    /** Every table by the label [DemonVariant.dropTable] names. */
    val BY_LABEL: Map<String, MonsterDropTable> =
        mapOf(
            "lesser" to LESSER,
            "lesser wilderness" to LESSER_WILDERNESS,
            "greater" to GREATER,
            "greater wilderness" to GREATER_WILDERNESS,
        )
}
