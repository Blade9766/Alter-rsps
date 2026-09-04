package org.alter.plugins.content.mechanics.grandexchange

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * The world's Grand Exchange order book.
 *
 * Offers fill in two stages. A new offer is first matched **against other players** the instant it
 * is placed, at the resting offer's price - that path is untouched, and it still takes precedence,
 * so on a populated world real orders trade with each other before the house sees them. Whatever is
 * left over then fills **from the exchange's own supply**, gradually, on [fillFromSupply]. That
 * second stage is what makes the exchange work at all on a world with one player on it.
 *
 * Supply only trades on the right side of the guide price - at or above it to buy, at or below it
 * to sell - and always executes *at* the guide price, so an offer pushed past the guide fills faster
 * without costing anything. [GrandExchangeSupply] holds the rates.
 *
 * The book is world state, not player state. An offer stays live while its owner is logged out and
 * keeps filling, so the goods and coins it earns accumulate on the offer itself and are handed over
 * the next time the owner opens the exchange. That is also why it persists to its own file rather
 * than riding along in the player save.
 */
object GrandExchangeMarket {

    private const val PRICES_FILE = "data/cfg/grandexchange/prices.json"

    /**
     * Where the order book is persisted. Settable so a test can point it at a scratch file - the
     * book is world state and every mutation writes it out, so a test running against the default
     * would leave its own offers in the live save.
     */
    var bookFile: String = "data/saves/grandexchange.json"

    /**
     * The server runs out of `game-server/` while the data directory sits at the repo root, which is
     * why everything else here reaches for `../data/...`. Resolving both keeps the exchange working
     * whichever of the two a launcher picks; an absolute path is taken as given.
     */
    private fun dataFile(path: String): File {
        val direct = File(path)
        if (direct.isAbsolute) {
            return direct
        }
        val relative = File("../$path")
        return if (relative.exists() || !direct.exists()) relative else direct
    }

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /** Guide price per item id, baked by `gradlew :game-server:gePriceDump`. */
    private val guidePrices = HashMap<Int, Int>()

    /** Offers by lowercased owner name; the array is always [GrandExchange.SLOTS] long. */
    private val offers = HashMap<String, Array<GrandExchangeOffer?>>()

    private var nextSequence = 1L

    /** Set while [load] replays the file so the replay does not write it back out again. */
    private var loading = false

    /**
     * Set by [fillFromSupply] instead of writing the book out.
     *
     * Placing, aborting and collecting all move items and are rare, so they save immediately. Supply
     * fills happen every couple of ticks for every open offer, and writing the whole book that often
     * would be a needless amount of disk churn for state that is only ever a few seconds stale.
     */
    private var pendingSave = false

    // -------------------------------------------------------------------------------------------
    // Prices
    // -------------------------------------------------------------------------------------------

    /**
     * The guide price the "Guide price" button and the initial price of a new offer use.
     * Zero means the item has no entry, which is how [isTradeable] decides what may be offered:
     * the price file is generated from the cache's own tradeable set.
     */
    fun guidePrice(item: Int): Int = guidePrices[item] ?: 0

    fun isTradeable(item: Int): Boolean = guidePrices.containsKey(item)

    // -------------------------------------------------------------------------------------------
    // Book access
    // -------------------------------------------------------------------------------------------

    /** True when the player has anything at all on the book - an open offer or something to collect. */
    fun hasOffers(owner: String): Boolean = offers[owner.lowercase()]?.any { it != null } == true

    fun slots(owner: String): Array<GrandExchangeOffer?> =
        offers.getOrPut(owner.lowercase()) { arrayOfNulls(GrandExchange.SLOTS) }

    fun offer(
        owner: String,
        slot: Int,
    ): GrandExchangeOffer? = if (slot in 0 until GrandExchange.SLOTS) slots(owner)[slot] else null

    /**
     * Places an offer and immediately matches it against the book. Returns the offer so the caller
     * can push it to the client; the caller is responsible for having already taken the coins or
     * the goods off the player.
     */
    fun place(
        owner: String,
        slot: Int,
        sell: Boolean,
        item: Int,
        price: Int,
        amount: Int,
    ): GrandExchangeOffer {
        val offer =
            GrandExchangeOffer(
                owner = owner.lowercase(),
                slot = slot,
                sell = sell,
                item = item,
                price = price,
                amount = amount,
                sequence = nextSequence++,
            )
        slots(owner)[slot] = offer
        val touched = match(offer)
        save()
        touched.remove(offer)
        notifyOwners(touched)
        return offer
    }

    /**
     * Cancels an offer. Whatever has not traded is moved into the collect box - coins for a buy,
     * goods for a sell - and the offer stops matching.
     */
    fun abort(offer: GrandExchangeOffer) {
        if (offer.finished) {
            return
        }
        val remaining = offer.remaining
        if (offer.sell) {
            offer.giveItem(offer.item, remaining)
        } else {
            offer.giveCoins(remaining.toLong() * offer.price)
        }
        offer.aborted = true
        save()
    }

    /** Empties a slot once its offer has nothing left to trade or collect. */
    fun release(offer: GrandExchangeOffer) {
        if (!offer.exhausted) {
            return
        }
        val slots = slots(offer.owner)
        if (slots.getOrNull(offer.slot) === offer) {
            slots[offer.slot] = null
        }
        save()
    }

    fun markDirty() {
        save()
    }

    /** Writes the book out if a supply fill has moved anything since the last write. */
    fun flush() {
        if (pendingSave) {
            save()
        }
    }

    // -------------------------------------------------------------------------------------------
    // Matching
    // -------------------------------------------------------------------------------------------

    /**
     * Trades [incoming] against every compatible resting offer until one side runs out.
     *
     * The resting offer sets the price, which is what makes a buyer who overpaid get the difference
     * back: bid 200 into a resting 150 ask and the trade executes at 150, with 50 per item landing
     * in the buyer's collect box as change. Ties in price go to the older offer.
     *
     * @return every offer this touched, including [incoming].
     */
    private fun match(incoming: GrandExchangeOffer): MutableSet<GrandExchangeOffer> {
        val touched = linkedSetOf(incoming)
        if (incoming.remaining <= 0) {
            return touched
        }

        val candidates =
            offers.values
                .asSequence()
                .flatMap { it.asSequence() }
                .filterNotNull()
                .filter { it !== incoming }
                .filter { it.item == incoming.item }
                .filter { it.sell != incoming.sell }
                .filter { it.remaining > 0 }
                .filter { if (incoming.sell) it.price >= incoming.price else it.price <= incoming.price }
                .sortedWith(
                    // Best price for the incoming side first, then oldest first.
                    if (incoming.sell) {
                        compareByDescending<GrandExchangeOffer> { it.price }.thenBy { it.sequence }
                    } else {
                        compareBy<GrandExchangeOffer> { it.price }.thenBy { it.sequence }
                    },
                ).toList()

        for (resting in candidates) {
            if (incoming.remaining <= 0) {
                break
            }
            val quantity = minOf(incoming.remaining, resting.remaining)
            if (quantity <= 0) {
                continue
            }

            // The offer that was already on the book when the other arrived sets the price.
            val executed = if (resting.sequence < incoming.sequence) resting.price else incoming.price
            val buyer = if (incoming.sell) resting else incoming
            val seller = if (incoming.sell) incoming else resting

            val value = quantity.toLong() * executed
            buyer.completed += quantity
            buyer.spent += value
            buyer.giveItem(buyer.item, quantity)
            // The buyer escrowed coins at their own asking price; anything the trade did not use is
            // theirs to take back.
            buyer.giveCoins(quantity.toLong() * (buyer.price - executed))

            seller.completed += quantity
            seller.spent += value
            seller.giveCoins(value)

            touched.add(resting)
        }
        return touched
    }

    /**
     * Re-runs matching across the whole book. Only used after [load], where crossing offers should
     * be impossible but a hand-edited or half-written file could still produce them.
     */
    fun matchAll() {
        val touched = linkedSetOf<GrandExchangeOffer>()
        offers.values
            .flatMap { it.asSequence().filterNotNull().toList() }
            .sortedBy { it.sequence }
            .forEach { if (it.remaining > 0) touched.addAll(match(it)) }
        if (touched.isNotEmpty()) {
            logger.info { "Grand Exchange: matched ${touched.size} offers on startup." }
            save()
        }
    }

    // -------------------------------------------------------------------------------------------
    // Supply
    // -------------------------------------------------------------------------------------------

    /**
     * Fills every open offer a little further from the exchange's own supply, and returns the ones
     * that moved so their owners can be told.
     *
     * An offer is only eligible while its price is on the right side of the guide - a buy at or
     * above it, a sell at or below it - and the trade always executes at the guide price itself.
     * That is what makes a generous price a pure speed control: bid 200 on something worth 150 and
     * you still pay 150, with the other 50 per item coming back as change, exactly as if a player
     * selling at 150 had filled you. The sell side mirrors it - undercut and you are still paid what
     * the item is worth.
     *
     * An item with no guide price cannot be offered in the first place, so a zero here means the
     * price file is missing or stale rather than that the item is worthless; such offers are left
     * alone rather than being filled for nothing.
     */
    fun fillFromSupply(): Set<GrandExchangeOffer> {
        val touched = linkedSetOf<GrandExchangeOffer>()
        for (slots in offers.values) {
            for (offer in slots) {
                if (offer == null || offer.remaining <= 0) {
                    continue
                }
                val guide = guidePrice(offer.item)
                if (guide <= 0) {
                    continue
                }

                // How far past the guide price the offer is, as a percentage. Negative means the
                // player is asking for a better deal than the item is worth, which never fills.
                val advantage =
                    if (offer.sell) {
                        (guide - offer.price).toLong() * 100 / guide
                    } else {
                        (offer.price - guide).toLong() * 100 / guide
                    }
                if (advantage < 0) {
                    continue
                }

                val quantity =
                    GrandExchangeSupply.fillSize(
                        amount = offer.amount,
                        remaining = offer.remaining,
                        advantagePercent = advantage.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    )
                if (quantity <= 0) {
                    continue
                }

                val value = quantity.toLong() * guide
                offer.completed += quantity
                offer.spent += value
                if (offer.sell) {
                    offer.giveCoins(value)
                } else {
                    offer.giveItem(offer.item, quantity)
                    // The buyer escrowed coins at their own asking price; the trade only used the
                    // guide, so the rest is theirs back.
                    offer.giveCoins(quantity.toLong() * (offer.price - guide))
                }
                touched.add(offer)
            }
        }
        if (touched.isNotEmpty()) {
            pendingSave = true
            notifyOwners(touched)
        }
        return touched
    }

    /**
     * Hook the plugin installs so a fill can refresh an owner who happens to be online. Offline
     * owners need nothing - their client is rebuilt from the book when they next open the exchange.
     */
    var onOfferChanged: ((GrandExchangeOffer) -> Unit)? = null

    private fun notifyOwners(touched: Collection<GrandExchangeOffer>) {
        val callback = onOfferChanged ?: return
        touched.forEach(callback)
    }

    // -------------------------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------------------------

    fun load() {
        loadPrices()
        loadBook()
    }

    private fun loadPrices() {
        guidePrices.clear()
        val file = dataFile(PRICES_FILE)
        if (!file.exists()) {
            logger.warn {
                "Grand Exchange price file $PRICES_FILE is missing - no item can be offered. " +
                    "Generate it with: gradlew :game-server:gePriceDump"
            }
            return
        }
        try {
            val root = JsonParser.parseString(file.readText()).asJsonObject
            val prices = root.getAsJsonObject("prices")
            for ((key, value) in prices.entrySet()) {
                val id = key.toIntOrNull() ?: continue
                val price = value.asInt
                if (price > 0) {
                    guidePrices[id] = price
                }
            }
            logger.info { "Loaded ${guidePrices.size} Grand Exchange guide prices." }
        } catch (e: Exception) {
            logger.error(e) { "Unable to read Grand Exchange prices from $PRICES_FILE." }
        }
    }

    private fun loadBook() {
        offers.clear()
        nextSequence = 1L
        val file = dataFile(bookFile)
        if (!file.exists()) {
            return
        }
        loading = true
        try {
            val root = JsonParser.parseString(file.readText()).asJsonObject
            val array = root.getAsJsonArray("offers") ?: return
            for (element in array) {
                val json = element.asJsonObject
                val offer = readOffer(json) ?: continue
                slots(offer.owner)[offer.slot] = offer
                if (offer.sequence >= nextSequence) {
                    nextSequence = offer.sequence + 1
                }
            }
            logger.info { "Loaded ${array.size()} Grand Exchange offers." }
        } catch (e: Exception) {
            logger.error(e) { "Unable to read the Grand Exchange book from $bookFile." }
        } finally {
            loading = false
        }
    }

    private fun readOffer(json: JsonObject): GrandExchangeOffer? {
        val owner = json.get("owner")?.asString ?: return null
        val slot = json.get("slot")?.asInt ?: return null
        if (slot !in 0 until GrandExchange.SLOTS) {
            return null
        }
        return GrandExchangeOffer(
            owner = owner.lowercase(),
            slot = slot,
            sell = json.get("sell")?.asBoolean ?: false,
            item = json.get("item")?.asInt ?: return null,
            price = json.get("price")?.asInt ?: return null,
            amount = json.get("amount")?.asInt ?: return null,
            completed = json.get("completed")?.asInt ?: 0,
            spent = json.get("spent")?.asLong ?: 0L,
            aborted = json.get("aborted")?.asBoolean ?: false,
            collectedItem = json.get("collectedItem")?.asInt ?: 0,
            collectedItemId = json.get("collectedItemId")?.asInt ?: 0,
            collectedCoins = json.get("collectedCoins")?.asLong ?: 0L,
            sequence = json.get("sequence")?.asLong ?: 0L,
        )
    }

    /**
     * Writes the whole book out. It is a few hundred rows at most on any realistic world, and an
     * offer that survives a crash but loses the coins it escrowed is a duplication bug, so this runs
     * on every mutation rather than on a timer.
     */
    fun save() {
        if (loading) {
            return
        }
        val list =
            offers.values
                .asSequence()
                .flatMap { it.asSequence() }
                .filterNotNull()
                .sortedBy { it.sequence }
                .map { offer ->
                    linkedMapOf(
                        "owner" to offer.owner,
                        "slot" to offer.slot,
                        "sell" to offer.sell,
                        "item" to offer.item,
                        "price" to offer.price,
                        "amount" to offer.amount,
                        "completed" to offer.completed,
                        "spent" to offer.spent,
                        "aborted" to offer.aborted,
                        "collectedItem" to offer.collectedItem,
                        "collectedItemId" to offer.collectedItemId,
                        "collectedCoins" to offer.collectedCoins,
                        "sequence" to offer.sequence,
                    )
                }.toList()

        try {
            val file = dataFile(bookFile)
            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(mapOf("offers" to list)))
            pendingSave = false
        } catch (e: Exception) {
            logger.error(e) { "Unable to write the Grand Exchange book to $bookFile." }
        }
    }
}
