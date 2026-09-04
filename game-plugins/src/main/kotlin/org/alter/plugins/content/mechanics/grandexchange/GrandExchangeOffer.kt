package org.alter.plugins.content.mechanics.grandexchange

/**
 * The status byte carried by `UpdateStockMarketSlot`. The client derives everything it shows from
 * this - `stockmarket_getoffertype` reads buy vs sell out of it, `stockmarket_isofferfinished`
 * reads the finished flag - so the numbering has to match the client's, not ours.
 */
object OfferStatus {
    const val EMPTY = 0
    const val BUYING = 1
    const val BOUGHT = 2
    const val SELLING = 3
    const val SOLD = 4
    const val CANCELLED_BUY = 5
    const val CANCELLED_SELL = 6
}

/**
 * One Grand Exchange offer, owned by one player in one of their eight slots.
 *
 * An offer outlives its owner's session: it stays in [GrandExchangeMarket]'s book while they are
 * logged out and can be matched by anyone, which is why the goods and coins it has earned live here
 * in [collectedItem] / [collectedCoins] rather than in a player container.
 *
 * @property owner the owner's username, lowercased - the book is keyed by it and the owner may be
 *   offline when a match lands.
 * @property slot which of the owner's eight slots this occupies.
 * @property sell false for a buy offer, true for a sell offer.
 * @property item the unnoted item id being traded.
 * @property price the price per item the owner asked for. Matching may execute better than this,
 *   never worse.
 * @property amount how many were offered.
 * @property completed how many have changed hands so far.
 * @property spent for a buy offer, the coins actually paid out so far; for a sell offer, the coins
 *   earned. This is what the client shows as "completedGold".
 * @property aborted set when the owner cancels; the remainder is refunded and no further matching
 *   happens, but the slot stays occupied until it is collected.
 * @property collectedItem items waiting in the collect box - bought goods, or the unsold remainder
 *   of an aborted sell.
 * @property collectedCoins coins waiting in the collect box - sale proceeds, the refund from an
 *   aborted buy, or the difference when a buy executed below its asking price.
 * @property sequence creation order across the whole book. Matching resolves price ties in favour
 *   of the older offer, and the older offer's price is the one the trade executes at.
 */
class GrandExchangeOffer(
    val owner: String,
    val slot: Int,
    val sell: Boolean,
    val item: Int,
    val price: Int,
    val amount: Int,
    var completed: Int = 0,
    var spent: Long = 0,
    var aborted: Boolean = false,
    var collectedItem: Int = 0,
    var collectedItemId: Int = 0,
    var collectedCoins: Long = 0,
    val sequence: Long = 0,
) {

    /** True once nothing more can trade - either it filled or the owner cancelled it. */
    val finished: Boolean get() = aborted || completed >= amount

    /** How many are still open to matching. */
    val remaining: Int get() = if (aborted) 0 else amount - completed

    /** Nothing left to trade and nothing left to collect: the slot can be released. */
    val exhausted: Boolean get() = finished && collectedItem <= 0 && collectedCoins <= 0L

    val status: Int
        get() = when {
            aborted && sell -> OfferStatus.CANCELLED_SELL
            aborted -> OfferStatus.CANCELLED_BUY
            completed >= amount && sell -> OfferStatus.SOLD
            completed >= amount -> OfferStatus.BOUGHT
            sell -> OfferStatus.SELLING
            else -> OfferStatus.BUYING
        }

    /**
     * Puts items into the collect box. A slot only ever holds one item id - the one being traded -
     * so this only has to accumulate a count.
     */
    fun giveItem(
        id: Int,
        count: Int,
    ) {
        if (count <= 0) return
        collectedItemId = id
        collectedItem += count
    }

    fun giveCoins(count: Long) {
        if (count <= 0) return
        collectedCoins += count
    }
}
