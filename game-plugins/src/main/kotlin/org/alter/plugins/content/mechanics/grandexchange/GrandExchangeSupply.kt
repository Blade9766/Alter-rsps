package org.alter.plugins.content.mechanics.grandexchange

/**
 * How fast the exchange's own supply fills an offer, and what it charges.
 *
 * These are the tuning knobs for the whole supply side. Nothing else in the exchange hard-codes a
 * rate, so changing a number here is the whole edit.
 *
 * **The model.** An offer only fills from supply when its price is on the right side of the guide
 * price - at or above it to buy, at or below it to sell. How far past the guide it is decides the
 * speed: matching the guide exactly is the slowest fill, and pushing further past it is faster, in
 * the three bands below. The trade itself always executes at the guide price, so pushing the price
 * to fill faster never costs anything; see [GrandExchangeMarket.fillFromSupply].
 *
 * **Rates are a percentage of the offer's size**, not a flat count, so a 10,000-item order and a
 * 10-item order take about the same wall-clock time to fill rather than the big one taking a
 * thousand times longer. A minimum of one item per pump keeps small offers moving.
 *
 * With the defaults below and a pump every 2 ticks (1.2 seconds), an offer takes roughly:
 *  - 60 seconds at exactly the guide price,
 *  - 24 seconds at 5% past it,
 *  - 11 seconds at 10% or more past it.
 */
object GrandExchangeSupply {

    /** Ticks between fill passes. One tick is 600ms. */
    const val PUMP_INTERVAL_TICKS = 2

    /** How often the book is written out while offers are only being filled by supply. */
    const val SAVE_INTERVAL_TICKS = 50

    /** Percent past the guide price at which an offer moves into the middle speed band. */
    const val KEEN_THRESHOLD_PERCENT = 5

    /** Percent past the guide price at which an offer moves into the fastest band. */
    const val VERY_KEEN_THRESHOLD_PERCENT = 10

    /** Percentage of the offer filled per pump when its price merely matches the guide. */
    const val RATE_AT_GUIDE_PERCENT = 2

    /** Percentage of the offer filled per pump once it is [KEEN_THRESHOLD_PERCENT] past the guide. */
    const val RATE_KEEN_PERCENT = 5

    /** Percentage per pump once it is [VERY_KEEN_THRESHOLD_PERCENT] past the guide. */
    const val RATE_VERY_KEEN_PERCENT = 12

    /**
     * How many items one pump moves for an offer whose price is [advantagePercent] past the guide.
     * Never returns more than [remaining], and never less than one so an offer cannot stall.
     */
    fun fillSize(
        amount: Int,
        remaining: Int,
        advantagePercent: Int,
    ): Int {
        val rate =
            when {
                advantagePercent >= VERY_KEEN_THRESHOLD_PERCENT -> RATE_VERY_KEEN_PERCENT
                advantagePercent >= KEEN_THRESHOLD_PERCENT -> RATE_KEEN_PERCENT
                else -> RATE_AT_GUIDE_PERCENT
            }
        val step = (amount.toLong() * rate / 100).toInt().coerceAtLeast(1)
        return step.coerceAtMost(remaining)
    }
}
