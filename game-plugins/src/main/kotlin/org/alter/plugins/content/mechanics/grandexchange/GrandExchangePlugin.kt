package org.alter.plugins.content.mechanics.grandexchange

import org.alter.api.InterfaceDestination
import org.alter.api.ext.closeInterface
import org.alter.api.ext.player
import org.alter.api.ext.getInteractingOption
import org.alter.api.ext.getInteractingSlot
import org.alter.api.ext.getVarbit
import org.alter.api.ext.getVarp
import org.alter.api.ext.inputInt
import org.alter.api.ext.message
import org.alter.api.ext.searchItemInput
import org.alter.api.ext.setVarbit
import org.alter.game.Server
import org.alter.game.model.ExamineEntityType
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.TaskPriority
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.COLLECT_BOXES
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.COLLECT_BOX_SUB_COINS
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.COLLECT_BOX_SUB_ITEM
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.COLLECT_INTERFACE_ID
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.COLLECT_SUB_COINS
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.COLLECT_SUB_ITEM
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.CONFIRM_BUTTON
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.INTERFACE_ID
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_PANEL
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_CHOOSE_ITEM
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_PRICE_ENTER
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_PRICE_GUIDE
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_PRICE_MINUS_1
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_PRICE_MINUS_5_PERCENT
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_PRICE_PLUS_1
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_PRICE_PLUS_5_PERCENT
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_QUANTITY_BULK
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_QUANTITY_ENTER
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_QUANTITY_MINUS_1
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_QUANTITY_PLUS_1
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_QUANTITY_PLUS_100
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_QUANTITY_PLUS_10
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_SUB_QUANTITY_PLUS_1_ARROW
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SIDE_INTERFACE_ID
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SLOTS
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SLOT_SUB_BUY
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SLOT_SUB_SELL
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SLOT_SUB_VIEW
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.STATUS_PROGRESS
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.STATUS_SUB_ABORT
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.VARBIT_OFFER_PRICE
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.VARBIT_OFFER_QUANTITY
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.VARBIT_OFFER_TYPE
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.VARBIT_SELECTED_SLOT
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.VARP_OFFER_ITEM
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.slotComponent

/**
 * Which slot the offer being written belongs to. The client tracks this too, in varbit 4439, but it
 * writes that locally and never tells us, so the server keeps its own copy.
 */
private val GE_EDIT_SLOT = AttributeKey<Int>()

/** Drives the gradual supply-side fill. A world timer, not a player one - offers fill while offline. */
private val GE_SUPPLY_TIMER = TimerKey()

/**
 * Drives the Grand Exchange window.
 *
 * The client owns the *look* of the exchange: it switches panels, animates buttons and previews
 * quantity and price changes without asking us. It does not own the offer. Every button that alters
 * the offer also sends its op to the server, so this plugin re-runs the same arithmetic the client
 * just previewed and keeps the authoritative copy in varp 1151 and varbits 4396/4397/4398 - and
 * those are the values read when Confirm arrives. The client-side preview only exists so the numbers
 * move instantly; if the two ever disagree, the server's varp update overwrites the client's.
 */
class GrandExchangePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {

    /** Ticks since the book was last written out by the supply pass. */
    private var supplyTicks = 0

    init {
        onWorldInit {
            GrandExchangeMarket.load()
            GrandExchangeMarket.matchAll()
            // A fill that lands while the owner is online has to reach their client; one that lands
            // while they are offline needs nothing, because opening the exchange resyncs all eight
            // slots from the book.
            GrandExchangeMarket.onOfferChanged = { offer ->
                world.getPlayerForName(offer.owner)?.sendGrandExchangeSlot(offer.slot)
            }
            world.timers[GE_SUPPLY_TIMER] = GrandExchangeSupply.PUMP_INTERVAL_TICKS
        }

        /**
         * Nudges every open offer a little further along. This is a world timer rather than a player
         * one on purpose: an offer keeps filling while its owner is logged out, so the pass has to
         * run whether or not anyone is around to watch it.
         */
        onTimer(GE_SUPPLY_TIMER) {
            GrandExchangeMarket.fillFromSupply()
            world.timers[GE_SUPPLY_TIMER] = GrandExchangeSupply.PUMP_INTERVAL_TICKS

            // Supply fills only mark the book dirty, so something has to actually write it out.
            supplyTicks += GrandExchangeSupply.PUMP_INTERVAL_TICKS
            if (supplyTicks >= GrandExchangeSupply.SAVE_INTERVAL_TICKS) {
                supplyTicks = 0
                GrandExchangeMarket.flush()
            }
        }

        // A player logging out is the last chance to catch fills that have not been written yet.
        onLogout {
            GrandExchangeMarket.flush()
        }

        onLogin {
            // Without this the client refuses to put "Create offer" on five of the eight boxes.
            player.sendMembersClientFlag()
            // Offers fill while their owner is logged out, so a returning player's client is told
            // about them up front rather than only when they next walk up to a booth.
            if (GrandExchangeMarket.hasOffers(player.username)) {
                player.refreshGrandExchange()
            }
        }

        onInterfaceClose(INTERFACE_ID) {
            player.closeInterface(dest = InterfaceDestination.TAB_AREA)
            player.clearOfferEditor()
            player.attr.remove(GE_EDIT_SLOT)
        }

        // --- The eight index boxes -----------------------------------------------------------
        for (slot in 0 until SLOTS) {
            onButton(interfaceId = INTERFACE_ID, component = slotComponent(slot)) {
                val option = player.getInteractingOption()
                when (player.getInteractingSlot()) {
                    SLOT_SUB_BUY -> player.beginOffer(slot, sell = false)
                    SLOT_SUB_SELL -> player.beginOffer(slot, sell = true)
                    SLOT_SUB_VIEW ->
                        when (option) {
                            1 -> player.setVarbit(VARBIT_SELECTED_SLOT, slot + 1)
                            2 -> player.abortOffer(slot)
                        }
                }
            }
        }

        // --- The offer editor ----------------------------------------------------------------
        onButton(interfaceId = INTERFACE_ID, component = SETUP_PANEL) {
            val sub = player.getInteractingSlot()
            // The "..." quantity button carries a second option on a buy offer - "All", meaning as
            // many as the coins in the inventory can pay for at the current price
            // (`[clientscript,script5156]`). Every other button here is op 1 only.
            if (player.getInteractingOption() == 2 && sub == SETUP_SUB_QUANTITY_ENTER) {
                player.quantityAffordable()
                return@onButton
            }
            if (player.getInteractingOption() != 1) {
                return@onButton
            }
            when (sub) {
                SETUP_SUB_CHOOSE_ITEM -> player.chooseOfferItem()
                SETUP_SUB_QUANTITY_ENTER -> player.enterQuantity()
                SETUP_SUB_PRICE_ENTER -> player.enterPrice()
                SETUP_SUB_PRICE_GUIDE -> player.setGuidePrice()
                SETUP_SUB_QUANTITY_MINUS_1 -> player.adjustQuantity(-1)
                SETUP_SUB_QUANTITY_PLUS_1_ARROW, SETUP_SUB_QUANTITY_PLUS_1 -> player.adjustQuantity(1)
                SETUP_SUB_QUANTITY_PLUS_10 -> player.adjustQuantity(10)
                SETUP_SUB_QUANTITY_PLUS_100 -> player.adjustQuantity(100)
                SETUP_SUB_QUANTITY_BULK -> player.adjustQuantity(Int.MAX_VALUE)
                SETUP_SUB_PRICE_MINUS_1 -> player.adjustPrice(-1)
                SETUP_SUB_PRICE_PLUS_1 -> player.adjustPrice(1)
                SETUP_SUB_PRICE_MINUS_5_PERCENT -> player.adjustPrice(-5)
                SETUP_SUB_PRICE_PLUS_5_PERCENT -> player.adjustPrice(5)
                else -> if (world.devContext.debugButtons) player.message("Unhandled GE setup sub $sub.")
            }
        }

        onButton(interfaceId = INTERFACE_ID, component = CONFIRM_BUTTON) {
            player.confirmOffer()
        }

        onButton(interfaceId = INTERFACE_ID, component = GrandExchange.BACK_BUTTON) {
            player.clearOfferEditor()
            player.attr.remove(GE_EDIT_SLOT)
            player.setVarbit(VARBIT_SELECTED_SLOT, 0)
        }

        onButton(interfaceId = INTERFACE_ID, component = GrandExchange.HISTORY_BUTTON) {
            player.message("Your offer history is not available yet.")
        }

        // --- Aborting from the offer status panel ---------------------------------------------
        onButton(interfaceId = INTERFACE_ID, component = STATUS_PROGRESS) {
            if (player.getInteractingSlot() == STATUS_SUB_ABORT && player.getInteractingOption() == 1) {
                player.abortOffer(player.getVarbit(VARBIT_SELECTED_SLOT) - 1)
            }
        }

        // --- Collecting -----------------------------------------------------------------------
        onButton(interfaceId = INTERFACE_ID, component = COLLECT_BOXES) {
            val slot = player.getVarbit(VARBIT_SELECTED_SLOT) - 1
            val option = player.getInteractingOption()
            when (player.getInteractingSlot()) {
                COLLECT_SUB_ITEM -> player.collect(slot, coins = false, option = option)
                COLLECT_SUB_COINS -> player.collect(slot, coins = true, option = option)
            }
        }

        // --- The collection box ----------------------------------------------------------------
        for (slot in 0 until SLOTS) {
            onButton(interfaceId = COLLECT_INTERFACE_ID, component = GrandExchange.collectComponent(slot)) {
                val option = player.getInteractingOption()
                when (player.getInteractingSlot()) {
                    COLLECT_BOX_SUB_ITEM -> player.collect(slot, coins = false, option = option)
                    COLLECT_BOX_SUB_COINS -> player.collect(slot, coins = true, option = option)
                }
            }
        }

        onButton(interfaceId = COLLECT_INTERFACE_ID, component = GrandExchange.COLLECT_ALL_TO_INVENTORY) {
            player.collectEverything(toBank = false)
        }

        onButton(interfaceId = COLLECT_INTERFACE_ID, component = GrandExchange.COLLECT_ALL_TO_BANK) {
            player.collectEverything(toBank = true)
        }

        // --- "Offer" on an inventory item ------------------------------------------------------
        onButton(interfaceId = SIDE_INTERFACE_ID, component = 1) {
            val slot = player.getInteractingSlot()
            val item = player.inventory[slot] ?: return@onButton
            when (player.getInteractingOption()) {
                1 -> player.offerFromInventory(item)
                10 -> world.sendExamine(player, item.id, ExamineEntityType.ITEM)
            }
        }
    }
}

// -------------------------------------------------------------------------------------------
// Offer editing
// -------------------------------------------------------------------------------------------

private fun Player.beginOffer(
    slot: Int,
    sell: Boolean,
) {
    if (GrandExchangeMarket.offer(username, slot) != null) {
        return
    }
    attr[GE_EDIT_SLOT] = slot
    setVarbit(VARBIT_SELECTED_SLOT, slot + 1)
    setVarbit(VARBIT_OFFER_TYPE, if (sell) 1 else 0)
    setOfferEditor(item = 0, quantity = 0, price = 0)
}

/**
 * Starts a sell offer for an inventory item, the way "Offer" on the side panel does. The item is
 * only being pointed at here - it is taken off the player when the offer is confirmed.
 */
private fun Player.offerFromInventory(item: Item) {
    val unnoted = item.toUnnoted().id
    if (!GrandExchangeMarket.isTradeable(unnoted)) {
        message("You can't sell that on the Grand Exchange.")
        return
    }
    val slot = firstFreeGrandExchangeSlot()
    if (slot == -1) {
        message("You have no free Grand Exchange slots.")
        return
    }
    attr[GE_EDIT_SLOT] = slot
    setVarbit(VARBIT_SELECTED_SLOT, slot + 1)
    setVarbit(VARBIT_OFFER_TYPE, 1)
    setOfferEditor(item = unnoted, quantity = 1, price = GrandExchangeMarket.guidePrice(unnoted))
}

private fun Player.firstFreeGrandExchangeSlot(): Int {
    val slots = GrandExchangeMarket.slots(username)
    return (0 until SLOTS).firstOrNull { slots[it] == null } ?: -1
}

private fun Player.chooseOfferItem() {
    val self = this
    queue(TaskPriority.STRONG) {
        val picked = searchItemInput(self, "Grand Exchange Item Search")
        if (picked <= 0) {
            return@queue
        }
        if (!GrandExchangeMarket.isTradeable(picked)) {
            self.message("You can't trade that item on the Grand Exchange.")
            return@queue
        }
        self.setOfferEditor(
            item = picked,
            quantity = 1,
            price = GrandExchangeMarket.guidePrice(picked),
        )
    }
}

private fun Player.enterQuantity() {
    if (getVarp(VARP_OFFER_ITEM) <= 0) return
    val self = this
    queue(TaskPriority.STRONG) {
        val entered = inputInt(self, "Enter quantity:")
        if (entered > 0) {
            self.setVarbit(VARBIT_OFFER_QUANTITY, entered)
        }
    }
}

private fun Player.enterPrice() {
    if (getVarp(VARP_OFFER_ITEM) <= 0) return
    val self = this
    queue(TaskPriority.STRONG) {
        val entered = inputInt(self, "Set a price for each item:")
        if (entered > 0) {
            self.setVarbit(VARBIT_OFFER_PRICE, entered)
        }
    }
}

private fun Player.setGuidePrice() {
    val item = getVarp(VARP_OFFER_ITEM)
    if (item <= 0) return
    setVarbit(VARBIT_OFFER_PRICE, GrandExchangeMarket.guidePrice(item).coerceAtLeast(1))
}

/**
 * Mirrors `[clientscript,ge_offers_setup_changequantity]`.
 *
 * The odd `if (delta > 1 && quantity == 1) delta--` step is the client's, not ours: it makes the
 * first press of +10 on a fresh offer land on 10 rather than 11.
 */
private fun Player.adjustQuantity(delta: Int) {
    val item = getVarp(VARP_OFFER_ITEM)
    if (item <= 0) return

    val selling = getVarbit(VARBIT_OFFER_TYPE) == 1
    val ceiling = if (selling) availableToSell(item) else Int.MAX_VALUE
    var quantity = getVarbit(VARBIT_OFFER_QUANTITY)
    var step = delta

    if (step >= Int.MAX_VALUE) {
        // "All" on a sell offer means everything you are holding; "+1K" on a buy offer is a step.
        if (selling) {
            setVarbit(VARBIT_OFFER_QUANTITY, ceiling.coerceAtLeast(1))
            return
        }
        step = 1000
    }

    quantity =
        if (step > 0) {
            if (step > 1 && quantity == 1) step--
            if (ceiling - step < quantity) ceiling else quantity + step
        } else {
            if (quantity <= -step) 1 else quantity + step
        }
    setVarbit(VARBIT_OFFER_QUANTITY, quantity.coerceIn(1, ceiling.coerceAtLeast(1)))
}

/**
 * "All" on a buy offer: as many as the coins on hand will buy at the current price. Mirrors
 * `[clientscript,script5156]`, including its floor of one when the player cannot afford even that.
 */
private fun Player.quantityAffordable() {
    if (getVarp(VARP_OFFER_ITEM) <= 0 || getVarbit(VARBIT_OFFER_TYPE) != 0) {
        return
    }
    val price = getVarbit(VARBIT_OFFER_PRICE)
    if (price <= 0) {
        return
    }
    val coins = inventory.getItemCount(COINS)
    setVarbit(VARBIT_OFFER_QUANTITY, if (coins <= 0) 1 else (coins / price).coerceAtLeast(1))
}

/** Mirrors `[clientscript,ge_offers_setup_changeprice]`; 5% steps round down and never reach zero. */
private fun Player.adjustPrice(delta: Int) {
    if (getVarp(VARP_OFFER_ITEM) <= 0) return
    val price = getVarbit(VARBIT_OFFER_PRICE)
    val updated =
        when (delta) {
            1 -> if (price < Int.MAX_VALUE) price + 1 else price
            -1 -> if (price > 1) price - 1 else price
            5 -> {
                val step = (price / 20).coerceAtLeast(1)
                if (Int.MAX_VALUE - step < price) Int.MAX_VALUE else price + step
            }
            -5 -> {
                val step = (price / 20).coerceAtLeast(1)
                if (step >= price) 1 else price - step
            }
            else -> price
        }
    setVarbit(VARBIT_OFFER_PRICE, updated.coerceAtLeast(1))
}

/**
 * How many of an item the player could put up for sale - `[proc,ge_offers_checkavailable]` counts
 * the noted form alongside the loose one, and so do we, because the offer accepts either.
 */
private fun Player.availableToSell(item: Int): Int {
    val noted = Item(item).toNoted().id
    var total = inventory.getItemCount(item)
    if (noted != item) {
        total += inventory.getItemCount(noted)
    }
    return total
}

// -------------------------------------------------------------------------------------------
// Placing, aborting and collecting
// -------------------------------------------------------------------------------------------

private fun Player.confirmOffer() {
    val slot = attr[GE_EDIT_SLOT] ?: return
    val item = getVarp(VARP_OFFER_ITEM)
    val quantity = getVarbit(VARBIT_OFFER_QUANTITY)
    val price = getVarbit(VARBIT_OFFER_PRICE)
    val selling = getVarbit(VARBIT_OFFER_TYPE) == 1

    if (item <= 0 || quantity <= 0 || price <= 0) {
        return
    }
    if (!GrandExchangeMarket.isTradeable(item)) {
        message("You can't trade that item on the Grand Exchange.")
        return
    }
    if (GrandExchangeMarket.offer(username, slot) != null) {
        message("That Grand Exchange slot is already in use.")
        return
    }
    // The client greys Confirm out at this point too, with "Too much money!" - a total that does not
    // fit in an int cannot be escrowed or shown.
    val total = quantity.toLong() * price
    if (total > Int.MAX_VALUE) {
        message("You can't make an offer worth that much.")
        return
    }

    if (selling) {
        if (!takeForSale(item, quantity)) {
            message("You don't have enough of that item.")
            return
        }
    } else {
        if (inventory.getItemCount(COINS) < total) {
            message("You don't have enough coins to make that offer.")
            return
        }
        inventory.remove(COINS, total.toInt(), assureFullRemoval = true)
    }

    GrandExchangeMarket.place(username, slot, selling, item, price, quantity)
    attr.remove(GE_EDIT_SLOT)
    clearOfferEditor()
    setVarbit(VARBIT_SELECTED_SLOT, slot + 1)
    refreshGrandExchange()
}

/**
 * Takes the goods for a sell offer, spending loose items first and then unpacking notes. Returns
 * false without removing anything if the player cannot cover the whole amount.
 */
private fun Player.takeForSale(
    item: Int,
    quantity: Int,
): Boolean {
    if (availableToSell(item) < quantity) {
        return false
    }
    var left = quantity
    val loose = minOf(left, inventory.getItemCount(item))
    if (loose > 0) {
        inventory.remove(item, loose, assureFullRemoval = true)
        left -= loose
    }
    if (left > 0) {
        val noted = Item(item).toNoted().id
        inventory.remove(noted, left, assureFullRemoval = true)
    }
    return true
}

private fun Player.abortOffer(slot: Int) {
    val offer = GrandExchangeMarket.offer(username, slot) ?: return
    if (offer.finished) {
        return
    }
    GrandExchangeMarket.abort(offer)
    message("Aborting offer.")
    sendGrandExchangeSlot(slot)
}

/** Decodes a collect box click into what the player actually asked for. */
private fun Player.collect(
    slot: Int,
    coins: Boolean,
    option: Int,
) {
    val offer = GrandExchangeMarket.offer(username, slot) ?: return
    val amount = if (coins) offer.collectedCoins.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() else offer.collectedItem
    if (amount <= 0) {
        return
    }

    // The option numbering comes from `[proc,script818]` and is not what you would guess: with a
    // single item op 1 is "Collect-item" and op 2 is "Collect-note", but with more than one the two
    // swap round, so op 1 becomes "Collect-notes". Op 3 is always "Bank", op 10 "Examine". Coins
    // have no noted form and only ever carry op 2.
    val wantsNotes = !coins && if (amount == 1) option == 2 else option == 1
    collect(slot, coins, toBank = option == 3, noted = wantsNotes, examine = option == 10)
}

/**
 * Moves a collect box into the inventory or the bank. Anything that will not fit is left behind for
 * a second attempt rather than being dropped.
 */
private fun Player.collect(
    slot: Int,
    coins: Boolean,
    toBank: Boolean,
    noted: Boolean,
    examine: Boolean = false,
) {
    val offer = GrandExchangeMarket.offer(username, slot) ?: return

    val id = if (coins) COINS else offer.collectedItemId
    val amount = if (coins) offer.collectedCoins.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() else offer.collectedItem
    if (id <= 0 || amount <= 0) {
        return
    }

    if (examine) {
        world.sendExamine(this, id, ExamineEntityType.ITEM)
        return
    }

    // Notes only exist in the inventory; the bank unnotes anything put into it anyway.
    val given = if (noted && !toBank) Item(id).toNoted().id else id

    val container = if (toBank) bank else inventory
    val moved = container.add(given, amount, assureFullInsertion = false).completed
    if (moved <= 0) {
        message(if (toBank) "Your bank is full." else "You don't have enough inventory space.")
        return
    }

    if (coins) {
        offer.collectedCoins -= moved.toLong()
    } else {
        offer.collectedItem -= moved
        if (offer.collectedItem <= 0) {
            offer.collectedItemId = 0
        }
    }

    if (offer.exhausted) {
        GrandExchangeMarket.release(offer)
        setVarbit(VARBIT_SELECTED_SLOT, 0)
    } else {
        GrandExchangeMarket.markDirty()
    }
    sendGrandExchangeSlot(slot)
}

/**
 * The collection box's "Collect to inventory" / "Collect to bank" buttons: everything waiting in
 * every slot, goods unnoted. Anything that will not fit is left where it is.
 */
private fun Player.collectEverything(toBank: Boolean) {
    for (slot in 0 until SLOTS) {
        val offer = GrandExchangeMarket.offer(username, slot) ?: continue
        if (offer.collectedItem > 0) {
            collect(slot, coins = false, toBank = toBank, noted = false)
        }
        if (offer.collectedCoins > 0) {
            collect(slot, coins = true, toBank = toBank, noted = false)
        }
    }
}
