package org.alter.plugins.content.npcs.mossgiant

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.SeedTableId
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The moss giant drop table - one table shared by all three versions, which is how the page
 * publishes it: every version is `dropversion = Regular`.
 *
 * ## It sums to exactly 128, on the members reading
 *
 * Weapons 14, runes and ammunition 23, herbs 5, seeds 35, coins 39, other 8, gem 4 - **128**, with
 * the herb, seed and gem lines counted as **rows** rather than extra rolls. That is the reading
 * `content/npcs/zombie` established and [MonsterDropTable] generalises.
 *
 * Reaching 128 requires the members column on the coins: dropping the two free-to-play-only rows
 * (5 coins at 35/128 and 10 coins at 5/128) and taking the 2-coin row's `altrarity` of 8 rather than
 * its published 11. Under the free-to-play reading the section comes to 171. 128 exactly on one
 * reading and nothing meaningful on the other is what makes this a fact about the source rather than
 * a house preference.
 *
 * ## What is not modelled
 *
 * - **Moss giant bone, 1/4.** "Only dropped during Rag and Bone Man II" - a quest this server does
 *   not have. A 1/4 drop is far too common to hand out unconditionally just because its condition is
 *   unbuilt, so it drops never rather than always; `content/npcs/zombie` makes the same call about
 *   the zombie bone.
 * - **The Catacombs tertiary table**, `{{CatacombsDropTable}}`, which the Catacombs of Kourend and
 *   Giants' Den spawns reach. It is a whole shared table with its own ancient-shard and totem
 *   economy that nothing in this codebase implements yet.
 * - **The Wilderness Slayer tertiary table**, which needs Krystilia's larder and the Wilderness
 *   emblem chain, none of which is built - the same gap `content/npcs/chaosdruid` records.
 * - **The ring of wealth column** on the gem table, which
 *   [org.alter.plugins.content.npcs.GemDropTable] already documents as absent.
 *
 * The **giant champion scroll** is kept at its real 1/5000 even though there is no Champions'
 * Challenge to hand it in to, and the **ensouled giant head** at 1/24 with no Arceuus reanimation
 * spell to use it on - the same call `content/npcs/goblin` makes about the goblin scroll: faking a
 * rarity to hide a missing system is worse than dropping an item that currently only sits in a bank.
 */
internal object MossGiantDrops {
    /** Wiki tertiary, Wilderness only. */
    const val LOOTING_BAG_ONE_IN = 3

    /** Wiki tertiary. */
    const val ENSOULED_HEAD_ONE_IN = 24

    /** Wiki tertiary. */
    const val BEGINNER_CLUE_ONE_IN = 45

    /** Wiki tertiary. */
    const val LONG_BONE_ONE_IN = 400

    /** Wiki tertiary. */
    const val GIANT_CHAMPION_SCROLL_ONE_IN = 5000

    /**
     * Wiki tertiary, published as **1/5012.5** - the one non-integer rate in this tree, which is why
     * it is a Double and rolled against `World.randomDouble` rather than through `World.chance`.
     */
    const val CURVED_BONE_ONE_IN = 5012.5

    /**
     * Mossy key, the three published rates, quoted by the wiki from Mod Ash: "If on task, including
     * Wildy tasks, 1/75. Otherwise, if in the Wilderness, 1/100. Otherwise 1/150."
     *
     * All three are implemented - see [MossGiantPlugin], which has both a `Slayer.isOnTask` check
     * and the killer's position to hand. The Prifddinas exception in the same quote is moot: that
     * giant is not spawned, because the page says it cannot be fought.
     */
    const val MOSSY_KEY_ON_TASK_ONE_IN = 75

    const val MOSSY_KEY_WILDERNESS_ONE_IN = 100

    const val MOSSY_KEY_ONE_IN = 150

    /** Published out of 128 and reaching it exactly: rows 84, herb 5, seeds 35, gem 4. */
    val TABLE =
        MonsterDropTable(
            denominator = 128,
            herbWeight = 5,
            seedWeight = 35,
            seedTable = SeedTableId.UNCOMMON,
            gemWeight = 4,
            rows =
                listOf(
                    // Weapons and armour - 14.
                    WeightedDrop(getRSCM("item.black_sq_shield"), 1, weight = 5),
                    WeightedDrop(getRSCM("item.magic_staff"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.steel_med_helm"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.mithril_sword"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.mithril_spear"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.steel_kiteshield"), 1, weight = 1),
                    // Runes and ammunition - 23.
                    WeightedDrop(getRSCM("item.law_rune"), 3, weight = 4),
                    WeightedDrop(getRSCM("item.air_rune"), 18, weight = 3),
                    WeightedDrop(getRSCM("item.earth_rune"), 27, weight = 3),
                    WeightedDrop(getRSCM("item.chaos_rune"), 7, weight = 3),
                    WeightedDrop(getRSCM("item.nature_rune"), 6, weight = 3),
                    WeightedDrop(getRSCM("item.cosmic_rune"), 3, weight = 2),
                    WeightedDrop(getRSCM("item.iron_arrow"), 15, weight = 2),
                    WeightedDrop(getRSCM("item.steel_arrow"), 30, weight = 1),
                    WeightedDrop(getRSCM("item.death_rune"), 3, weight = 1),
                    WeightedDrop(getRSCM("item.blood_rune"), 1, weight = 1),
                    // Coins - 39. The two free-to-play-only rows are absent and the 2-coin row takes
                    // its members altrarity of 8.
                    WeightedDrop(getRSCM("item.coins_995"), 37, weight = 19),
                    WeightedDrop(getRSCM("item.coins_995"), 119, weight = 10),
                    WeightedDrop(getRSCM("item.coins_995"), 2, weight = 8),
                    WeightedDrop(getRSCM("item.coins_995"), 300, weight = 2),
                    // Other - 8. This table has no `Nothing` row at all.
                    WeightedDrop(getRSCM("item.steel_bar"), 1, weight = 6),
                    WeightedDrop(getRSCM("item.coal"), 1, weight = 1),
                    WeightedDrop(getRSCM("item.spinach_roll"), 1, weight = 1),
                ),
        )
}
