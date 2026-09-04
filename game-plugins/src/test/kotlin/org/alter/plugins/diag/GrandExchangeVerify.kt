package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange
import org.alter.plugins.content.mechanics.grandexchange.GrandExchangeMarket
import org.alter.plugins.content.mechanics.grandexchange.GrandExchangeSupply
import org.alter.plugins.content.mechanics.grandexchange.OfferStatus
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.Before
import org.junit.BeforeClass
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Checks for the Grand Exchange that do not need a running world: the guide prices parse and cover
 * the cache's tradeable set, the client ids the window is driven by are the ones actually in the
 * cache, and the order book matches, prices and refunds the way it claims to.
 *
 * The matching cases are the ones worth having. Every coin and every item that enters an offer has
 * to come back out of it, and a rounding slip in the refund is a silent duplication bug.
 */
class GrandExchangeVerify {

    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
            // Every mutation of the book writes it out, so point it at a scratch file first or these
            // tests would leave their own offers sitting in the live save.
            GrandExchangeMarket.bookFile =
                Files.createTempFile("ge-verify", ".json").toAbsolutePath().toString()
            GrandExchangeMarket.load()
        }

        const val YEW_LOGS = 1515
        const val ABYSSAL_WHIP = 4151
        const val COINS = 995
    }

    /**
     * The book is world state shared by every test in the class, and JUnit does not promise an
     * order, so each test starts from an empty one. Every mutation persists, so the scratch file has
     * to go as well - reloading alone would just read the previous test's offers straight back in.
     */
    @Before
    fun emptyBook() {
        File(GrandExchangeMarket.bookFile).delete()
        GrandExchangeMarket.load()
    }

    private var traders = 0

    private fun trader(): String = "getest${traders++}"

    // ---------------------------------------------------------------------------------------
    // Price data
    // ---------------------------------------------------------------------------------------

    @Test
    fun `guide prices cover the common tradeables`() {
        assertTrue(GrandExchangeMarket.isTradeable(YEW_LOGS), "Yew logs should be offerable.")
        assertTrue(GrandExchangeMarket.isTradeable(ABYSSAL_WHIP), "An abyssal whip should be offerable.")
        assertTrue(GrandExchangeMarket.guidePrice(YEW_LOGS) > 0)
        assertTrue(GrandExchangeMarket.guidePrice(ABYSSAL_WHIP) > 0)
    }

    /** Coins are the currency, not a commodity - offering them would let a player launder a stack. */
    @Test
    fun `coins are not offerable`() {
        assertFalse(GrandExchangeMarket.isTradeable(COINS))
        assertEquals(0, GrandExchangeMarket.guidePrice(COINS))
    }

    /**
     * The noted form of an item is a different id with the same tradeable flag. The exchange only
     * ever deals in unnoted ids, so the noted one must not have its own price entry - otherwise a
     * player could run two independent books for the same goods.
     */
    @Test
    fun `noted items are not offerable in their own right`() {
        // The repo's ItemType exposes the note link as noteLinkId, which is also what Item.toNoted
        // follows; notedId is left unset by this decoder.
        val noted = CacheManager.getItem(YEW_LOGS).noteLinkId
        assertTrue(noted > 0 && noted != YEW_LOGS, "Yew logs should have a noted form to test against.")
        assertFalse(GrandExchangeMarket.isTradeable(noted), "Noted yew logs ($noted) must not be offerable.")
    }

    // ---------------------------------------------------------------------------------------
    // Client ids
    // ---------------------------------------------------------------------------------------

    /**
     * `enum_150` is where the client reads the per-slot collect inventories from, and
     * `[proc,script816]` binds its redraw to exactly those ids. If the cache ever renumbers them the
     * collect boxes silently stop updating, so the constant is checked against the cache itself.
     */
    @Test
    fun `offer inventories match the client's enum`() {
        val enum = CacheManager.getEnum(150)
        assertEquals(GrandExchange.SLOTS, enum.getSize(), "enum 150 should hold one inv per Grand Exchange slot.")
        for (slot in 0 until GrandExchange.SLOTS) {
            assertEquals(
                GrandExchange.OFFER_INVS[slot],
                enum.getInt(slot),
                "Slot $slot's collect inventory does not match enum 150.",
            )
        }
    }

    /** `enum_245` maps slot index to the packed index-box component the ops arrive on. */
    @Test
    fun `index box components match the client's enum`() {
        val enum = CacheManager.getEnum(245)
        assertEquals(GrandExchange.SLOTS, enum.getSize())
        for (slot in 0 until GrandExchange.SLOTS) {
            assertEquals(
                GrandExchange.component(GrandExchange.INTERFACE_ID, GrandExchange.slotComponent(slot)),
                enum.getInt(slot),
                "Slot $slot's index box component does not match enum 245.",
            )
        }
    }

    /** The collection box finds its own eight slots through `enum_1011`. */
    @Test
    fun `collection box components match the client's enum`() {
        val enum = CacheManager.getEnum(GrandExchange.COLLECT_ENUM)
        assertEquals(GrandExchange.SLOTS, enum.getSize())
        for (slot in 0 until GrandExchange.SLOTS) {
            assertEquals(
                GrandExchange.component(GrandExchange.COLLECT_INTERFACE_ID, GrandExchange.collectComponent(slot)),
                enum.getInt(slot),
                "Slot $slot's collection box component does not match enum ${GrandExchange.COLLECT_ENUM}.",
            )
        }
    }

    /**
     * The clerk and booth names the plugin spawns and binds against.
     *
     * `getRSCM` throws on a name it does not know, and a KotlinPlugin whose constructor throws
     * registers nothing at all - silently. A typo here would take the whole exchange offline with no
     * log line to show for it, so the names are checked rather than trusted.
     */
    @Test
    fun `the clerk and booth names resolve`() {
        assertEquals(2148, getRSCM("npc.grand_exchange_clerk"))
        assertEquals(2149, getRSCM("npc.grand_exchange_clerk_2149"))
        assertEquals(2150, getRSCM("npc.grand_exchange_clerk_2150"))
        assertEquals(2151, getRSCM("npc.grand_exchange_clerk_2151"))
        assertEquals(10060, getRSCM("object.grand_exchange_booth"))
        assertEquals(10061, getRSCM("object.grand_exchange_booth_10061"))
    }

    /** The four clerks and both booth variants have to exist, and carry the options wired to them. */
    @Test
    fun `clerks and booths carry the options the plugin binds`() {
        for (id in 2148..2151) {
            val npc = CacheManager.getNpc(id)
            assertEquals("Grand Exchange Clerk", npc.name)
            val options = npc.actions.filterNotNull().map { it.lowercase() }
            assertTrue("exchange" in options, "Clerk $id has no Exchange option.")
            assertTrue("sets" in options, "Clerk $id has no Sets option.")
        }

        val exchangeBooth = CacheManager.getObject(10061).actions.filterNotNull().map { it.lowercase() }
        assertTrue("exchange" in exchangeBooth && "collect" in exchangeBooth)

        val bankBooth = CacheManager.getObject(10060).actions.filterNotNull().map { it.lowercase() }
        assertTrue("bank" in bankBooth && "collect" in bankBooth)
    }

    // ---------------------------------------------------------------------------------------
    // Matching
    // ---------------------------------------------------------------------------------------

    /**
     * Placing an offer never fills it by itself. With nobody to match, everything it gets comes from
     * the supply pass, which runs on a world timer - so the offer rests until that next comes round.
     */
    @Test
    fun `placing an offer does not fill it instantly`() {
        val buyer = trader()
        val offer = GrandExchangeMarket.place(buyer, 0, sell = false, item = YEW_LOGS, price = 200, amount = 100)
        assertEquals(0, offer.completed)
        assertEquals(OfferStatus.BUYING, offer.status)
        assertEquals(0, offer.collectedItem)
        assertEquals(0L, offer.collectedCoins)
    }

    /** A buy at or above a resting ask fills, and fills at the resting price. */
    @Test
    fun `a buy fills against a cheaper resting sell and the change comes back`() {
        val seller = trader()
        val buyer = trader()

        val ask = GrandExchangeMarket.place(seller, 0, sell = true, item = YEW_LOGS, price = 150, amount = 100)
        val bid = GrandExchangeMarket.place(buyer, 0, sell = false, item = YEW_LOGS, price = 200, amount = 100)

        assertEquals(100, bid.completed, "The whole bid should have filled.")
        assertEquals(100, ask.completed, "The whole ask should have filled.")

        // Executed at the resting 150, not the bid's 200.
        assertEquals(100, bid.collectedItem)
        assertEquals(YEW_LOGS, bid.collectedItemId)
        assertEquals(100L * 50, bid.collectedCoins, "The buyer should get 50 per item back as change.")
        assertEquals(100L * 150, ask.collectedCoins, "The seller should be paid the price they asked.")
        assertEquals(0, ask.collectedItem)

        assertEquals(OfferStatus.BOUGHT, bid.status)
        assertEquals(OfferStatus.SOLD, ask.status)
    }

    /** A bid below every ask sits alongside them without trading. */
    @Test
    fun `a buy below the resting ask does not fill`() {
        val seller = trader()
        val buyer = trader()

        GrandExchangeMarket.place(seller, 0, sell = true, item = YEW_LOGS, price = 150, amount = 100)
        val bid = GrandExchangeMarket.place(buyer, 0, sell = false, item = YEW_LOGS, price = 100, amount = 100)

        assertEquals(0, bid.completed)
        assertEquals(0L, bid.collectedCoins)
    }

    /** Partial fills leave the remainder on the book for the next counterparty. */
    @Test
    fun `a larger offer fills across several counterparties and keeps its remainder`() {
        val first = trader()
        val second = trader()
        val buyer = trader()

        GrandExchangeMarket.place(first, 0, sell = true, item = ABYSSAL_WHIP, price = 1_000, amount = 3)
        GrandExchangeMarket.place(second, 0, sell = true, item = ABYSSAL_WHIP, price = 1_200, amount = 4)
        val bid = GrandExchangeMarket.place(buyer, 0, sell = false, item = ABYSSAL_WHIP, price = 1_500, amount = 10)

        assertEquals(7, bid.completed, "Both asks should have been consumed.")
        assertEquals(3, bid.remaining, "The rest of the bid should still be open.")
        assertEquals(OfferStatus.BUYING, bid.status)
        assertEquals(7, bid.collectedItem)
        // 3 at 1000 and 4 at 1200, with the change on a 1500 bid coming back per item.
        assertEquals(3L * 1_000 + 4L * 1_200, bid.spent)
        assertEquals(3L * 500 + 4L * 300, bid.collectedCoins)
    }

    /** The cheapest ask is taken first, so a buyer never overpays while a cheaper one is open. */
    @Test
    fun `matching takes the best price first`() {
        val expensive = trader()
        val cheap = trader()
        val buyer = trader()

        GrandExchangeMarket.place(expensive, 0, sell = true, item = ABYSSAL_WHIP, price = 2_000, amount = 5)
        GrandExchangeMarket.place(cheap, 0, sell = true, item = ABYSSAL_WHIP, price = 1_000, amount = 5)
        val bid = GrandExchangeMarket.place(buyer, 0, sell = false, item = ABYSSAL_WHIP, price = 2_000, amount = 5)

        assertEquals(5, bid.completed)
        assertEquals(5L * 1_000, bid.spent, "The cheaper ask should have been consumed first.")
    }

    /** Aborting a buy hands back the coins that never traded, and only those. */
    @Test
    fun `aborting a part-filled buy refunds only the untraded coins`() {
        val seller = trader()
        val buyer = trader()

        GrandExchangeMarket.place(seller, 0, sell = true, item = YEW_LOGS, price = 100, amount = 40)
        val bid = GrandExchangeMarket.place(buyer, 0, sell = false, item = YEW_LOGS, price = 100, amount = 100)
        assertEquals(40, bid.completed)

        GrandExchangeMarket.abort(bid)

        assertEquals(OfferStatus.CANCELLED_BUY, bid.status)
        assertEquals(40, bid.collectedItem)
        assertEquals(60L * 100, bid.collectedCoins, "The 60 that never traded should be refunded.")
        assertEquals(0, bid.remaining)
    }

    /** Aborting a sell hands back the goods that never traded. */
    @Test
    fun `aborting a part-filled sell returns the untraded goods`() {
        val buyer = trader()
        val seller = trader()

        GrandExchangeMarket.place(buyer, 0, sell = false, item = YEW_LOGS, price = 100, amount = 30)
        val ask = GrandExchangeMarket.place(seller, 0, sell = true, item = YEW_LOGS, price = 100, amount = 100)
        assertEquals(30, ask.completed)

        GrandExchangeMarket.abort(ask)

        assertEquals(OfferStatus.CANCELLED_SELL, ask.status)
        assertEquals(70, ask.collectedItem, "The 70 that never sold should come back.")
        assertEquals(YEW_LOGS, ask.collectedItemId)
        assertEquals(30L * 100, ask.collectedCoins)
    }

    /** An aborted offer is out of the market - a later counterparty must not revive it. */
    @Test
    fun `an aborted offer no longer matches`() {
        val seller = trader()
        val buyer = trader()

        val ask = GrandExchangeMarket.place(seller, 0, sell = true, item = ABYSSAL_WHIP, price = 500, amount = 5)
        GrandExchangeMarket.abort(ask)

        val bid = GrandExchangeMarket.place(buyer, 0, sell = false, item = ABYSSAL_WHIP, price = 900, amount = 5)
        assertEquals(0, bid.completed, "The aborted ask should not have traded.")
        assertEquals(0, ask.completed)
    }

    /** Two offers for different items never see each other. */
    @Test
    fun `offers for different items do not cross`() {
        val seller = trader()
        val buyer = trader()

        GrandExchangeMarket.place(seller, 0, sell = true, item = YEW_LOGS, price = 1, amount = 10)
        val bid = GrandExchangeMarket.place(buyer, 0, sell = false, item = ABYSSAL_WHIP, price = 1_000_000, amount = 10)
        assertEquals(0, bid.completed)
    }

    /** A slot is only free again once its offer has been drained of goods and coins alike. */
    @Test
    fun `a slot is released only when nothing is left to collect`() {
        val seller = trader()
        val buyer = trader()

        GrandExchangeMarket.place(seller, 0, sell = true, item = YEW_LOGS, price = 100, amount = 10)
        val bid = GrandExchangeMarket.place(buyer, 1, sell = false, item = YEW_LOGS, price = 100, amount = 10)

        assertTrue(bid.finished)
        assertFalse(bid.exhausted, "The bought logs are still waiting to be collected.")
        GrandExchangeMarket.release(bid)
        assertTrue(GrandExchangeMarket.offer(buyer, 1) === bid, "The slot must stay occupied until collected.")

        bid.collectedItem = 0
        bid.collectedCoins = 0
        GrandExchangeMarket.release(bid)
        assertNull(GrandExchangeMarket.offer(buyer, 1), "An emptied offer should free its slot.")
    }

    // ---------------------------------------------------------------------------------------
    // Supply-side filling
    // ---------------------------------------------------------------------------------------

    /** Runs the supply pass until the offer stops moving, so a gradual fill can be asserted on. */
    private fun drain(limit: Int = 500): Int {
        var passes = 0
        while (passes < limit) {
            if (GrandExchangeMarket.fillFromSupply().isEmpty()) {
                break
            }
            passes++
        }
        return passes
    }

    /** The whole point of the supply side: an offer fills with nobody else on the world. */
    @Test
    fun `an offer at the guide price fills from supply with no counterparty`() {
        val guide = GrandExchangeMarket.guidePrice(YEW_LOGS)
        val bid = GrandExchangeMarket.place(trader(), 0, sell = false, item = YEW_LOGS, price = guide, amount = 100)
        assertEquals(0, bid.completed, "Nothing should fill before the supply pass runs.")

        val passes = drain()
        assertEquals(100, bid.completed)
        assertTrue(passes > 1, "The fill should be gradual, not a single jump - took $passes passes.")
        assertEquals(100, bid.collectedItem)
        assertEquals(100L * guide, bid.spent)
        assertEquals(0L, bid.collectedCoins, "Paying exactly the guide leaves no change.")
    }

    /** A price below what the item is worth is not a slow fill - it is no fill. */
    @Test
    fun `a buy below the guide price never fills from supply`() {
        val guide = GrandExchangeMarket.guidePrice(ABYSSAL_WHIP)
        val bid = GrandExchangeMarket.place(trader(), 0, sell = false, item = ABYSSAL_WHIP, price = guide / 2, amount = 5)
        drain(limit = 20)
        assertEquals(0, bid.completed)
    }

    /** Nor is asking above the guide on a sell. */
    @Test
    fun `a sell above the guide price never fills from supply`() {
        val guide = GrandExchangeMarket.guidePrice(ABYSSAL_WHIP)
        val ask = GrandExchangeMarket.place(trader(), 0, sell = true, item = ABYSSAL_WHIP, price = guide * 2, amount = 5)
        drain(limit = 20)
        assertEquals(0, ask.completed)
    }

    /** Pushing past the guide is a speed control, and has to actually be faster. */
    @Test
    fun `a keener price fills in fewer passes`() {
        val guide = GrandExchangeMarket.guidePrice(YEW_LOGS)

        val patient = GrandExchangeMarket.place(trader(), 0, sell = false, item = YEW_LOGS, price = guide, amount = 1000)
        val patientPasses = drain()

        emptyBook()
        val keen =
            GrandExchangeMarket.place(
                trader(), 0, sell = false, item = YEW_LOGS,
                price = guide + guide * GrandExchangeSupply.VERY_KEEN_THRESHOLD_PERCENT / 100, amount = 1000,
            )
        val keenPasses = drain()

        assertEquals(1000, patient.completed)
        assertEquals(1000, keen.completed)
        assertTrue(
            keenPasses < patientPasses,
            "A price past the guide should fill faster: $keenPasses passes vs $patientPasses at the guide.",
        )
    }

    /** Overpaying costs nothing - the trade executes at the guide and the rest comes back. */
    @Test
    fun `a buy above the guide pays the guide and refunds the difference`() {
        val guide = GrandExchangeMarket.guidePrice(YEW_LOGS)
        val bid = GrandExchangeMarket.place(trader(), 0, sell = false, item = YEW_LOGS, price = guide * 2, amount = 50)
        drain()

        assertEquals(50, bid.completed)
        assertEquals(50L * guide, bid.spent, "The buyer should only have paid the guide price.")
        assertEquals(50L * guide, bid.collectedCoins, "The overpayment should have come back as change.")
        assertEquals(50, bid.collectedItem)
    }

    /** And the sell side mirrors it: undercutting fills faster but still pays what the item is worth. */
    @Test
    fun `a sell below the guide is still paid the guide`() {
        val guide = GrandExchangeMarket.guidePrice(YEW_LOGS)
        val ask = GrandExchangeMarket.place(trader(), 0, sell = true, item = YEW_LOGS, price = guide / 2, amount = 50)
        drain()

        assertEquals(50, ask.completed)
        assertEquals(50L * guide, ask.collectedCoins)
        assertEquals(0, ask.collectedItem)
    }

    /** Supply must not resurrect an offer the player has cancelled. */
    @Test
    fun `supply does not touch an aborted offer`() {
        val guide = GrandExchangeMarket.guidePrice(YEW_LOGS)
        val bid = GrandExchangeMarket.place(trader(), 0, sell = false, item = YEW_LOGS, price = guide, amount = 100)
        GrandExchangeMarket.abort(bid)

        drain(limit = 20)
        assertEquals(0, bid.completed)
        assertEquals(100L * guide, bid.collectedCoins, "The refund should be all that ever landed.")
    }

    /**
     * Player orders still trade with each other first. Two crossing offers settle at the resting
     * price the moment the second is placed, before the supply pass has run at all.
     */
    @Test
    fun `player offers still match each other before supply sees them`() {
        val guide = GrandExchangeMarket.guidePrice(YEW_LOGS)
        val seller = trader()
        val buyer = trader()

        // Deliberately below the guide, so supply would never have filled this ask on its own.
        val ask = GrandExchangeMarket.place(seller, 0, sell = true, item = YEW_LOGS, price = guide * 3, amount = 10)
        val bid = GrandExchangeMarket.place(buyer, 0, sell = false, item = YEW_LOGS, price = guide * 3, amount = 10)

        assertEquals(10, ask.completed, "The two player offers should have matched on placement.")
        assertEquals(10, bid.completed)
        assertEquals(10L * guide * 3, bid.spent, "A player match settles at the resting price, not the guide.")
    }

    /** A supply fill is a real fill: it finishes the offer and the slot frees once collected. */
    @Test
    fun `a supply-filled offer completes and reports as bought`() {
        val guide = GrandExchangeMarket.guidePrice(YEW_LOGS)
        val bid = GrandExchangeMarket.place(trader(), 0, sell = false, item = YEW_LOGS, price = guide, amount = 20)
        drain()

        assertTrue(bid.finished)
        assertEquals(OfferStatus.BOUGHT, bid.status)
        assertEquals(0, bid.remaining)
    }
}
