package org.alter.plugins.content.npcs

import org.alter.game.model.World

/**
 * A weighted drop-table entry. A null [item] is the wiki's "Nothing" row.
 *
 * Quantities may be a range ([min]..[max]) - the White Knight and guard tables both use
 * them ("9-13 body runes", "25-30 water runes"), which the older fixed-amount tables in
 * `barbarian` and `darkwizard` didn't need.
 */
internal data class WeightedDrop(
    val item: Int?,
    val min: Int = 1,
    val max: Int = min,
    val weight: Int,
)

/**
 * Shared weighted-table rolling for the monster drop tables.
 *
 * Weights are the wiki's rarity numerators used as *relative* weights, the same
 * approximation the existing barbarian and dark wizard tables make. That matters here
 * because several sub-tables are deliberately not modelled (see the plugins' comments):
 * the numerators no longer sum to 128, so treating them as relative weights just
 * rescales the remaining rows proportionally rather than silently inflating any one
 * item's real rarity.
 */
internal object DropRoll {
    /** Picks one entry from [table] by relative weight. */
    fun pick(
        table: List<WeightedDrop>,
        world: World,
    ): WeightedDrop? {
        val total = table.sumOf { it.weight }
        var roll = world.randomDouble() * total
        for (drop in table) {
            if (roll < drop.weight) {
                return drop
            }
            roll -= drop.weight
        }
        return null
    }

    /** Rolls a quantity for [drop], inclusive of both ends of its range. */
    fun amount(
        drop: WeightedDrop,
        world: World,
    ): Int = if (drop.max <= drop.min) drop.min else drop.min + world.random(drop.max - drop.min)
}
