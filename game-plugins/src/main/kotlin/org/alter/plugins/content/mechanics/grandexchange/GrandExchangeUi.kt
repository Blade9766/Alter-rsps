package org.alter.plugins.content.mechanics.grandexchange

import net.rsprot.protocol.game.outgoing.inv.UpdateInvFull
import net.rsprot.protocol.game.outgoing.misc.player.UpdateStockMarketSlot
import org.alter.api.ClientScript
import org.alter.api.InterfaceDestination
import org.alter.api.ext.openInterface
import org.alter.api.ext.runClientScript
import org.alter.api.ext.setInterfaceEvents
import org.alter.api.ext.setInterfaceUnderlay
import org.alter.api.ext.setVarbit
import org.alter.api.ext.setVarp
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.rsprot.RsModObjectProvider
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.COLLECT_BOXES
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.CONFIRM_BUTTON
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.HISTORY_BUTTON
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.INDEX_PANEL
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.INDEX_TOP
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.INTERFACE_ID
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.OFFER_INVS
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.ROOT
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_PANEL
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SETUP_PRICE_ICON
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SIDE_INTERFACE_ID
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.SLOTS
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.STATUS_PANEL
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.STATUS_PRICE_ICON
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.STATUS_PROGRESS
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.TOOLTIP_LAYER
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.VARBIT_OFFER_PRICE
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.VARBIT_OFFER_QUANTITY
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.VARBIT_OFFER_TYPE
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.VARBIT_SELECTED_SLOT
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.VARP_OFFER_ITEM
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.WINDOW
import org.alter.plugins.content.mechanics.grandexchange.GrandExchange.component

private val GE_OFFERS_INIT = ClientScript("ge_offers_init")
private val GE_SIDE_INIT = ClientScript("ge_offer_side_init")
private val PLAYER_MEMBER = ClientScript("playermember")

/**
 * Ops 1 to 10 on the exchange's dynamic components, so the client is allowed to send them. Op N is
 * bit N in this mask - bit 0 is the pause-button flag, not op 1 - and the exchange never uses an
 * item on anything, so the target flags stay off.
 */
private const val GE_EVENTS = 0x7FE

/**
 * Marks the player as a member client-side.
 *
 * This exists because of the exchange: `[proc,ge_offers_index_drawslot]` only puts "Create offer" on
 * boxes 4 to 8 when `%varcint103` is 1 (or the world is deadman), so without it five of the eight
 * slots render but cannot be used. `%varcint103` is a client var - there is no packet that sets one,
 * so the only way to raise it is to run `[clientscript,playermember]`, which is what the live game
 * does at login.
 *
 * It is not confined to the exchange: the same flag lets the stats panel show members skills and
 * stops the quest list greying members quests out. That is the intended state for this server, where
 * members content is simply available, and setting it at login rather than when the exchange opens
 * keeps every interface consistent from the first tick.
 */
fun Player.sendMembersClientFlag() {
    runClientScript(PLAYER_MEMBER, 1)
}

/**
 * Opens the exchange. The window itself has to be built by the client, so this hands
 * `[clientscript,ge_offers_init]` the fourteen components it expects and then pushes the
 * player's eight slots and collect boxes.
 */
fun Player.openGrandExchange() {
    openInterface(interfaceId = INTERFACE_ID, dest = InterfaceDestination.MAIN_SCREEN)
    openInterface(interfaceId = SIDE_INTERFACE_ID, dest = InterfaceDestination.TAB_AREA)

    runClientScript(
        GE_OFFERS_INIT,
        component(INTERFACE_ID, ROOT),
        component(INTERFACE_ID, WINDOW),
        component(INTERFACE_ID, HISTORY_BUTTON),
        component(INTERFACE_ID, GrandExchange.BACK_BUTTON),
        component(INTERFACE_ID, INDEX_PANEL),
        component(INTERFACE_ID, INDEX_TOP),
        component(INTERFACE_ID, STATUS_PANEL),
        component(INTERFACE_ID, STATUS_PRICE_ICON),
        component(INTERFACE_ID, STATUS_PROGRESS),
        component(INTERFACE_ID, COLLECT_BOXES),
        component(INTERFACE_ID, SETUP_PANEL),
        component(INTERFACE_ID, SETUP_PRICE_ICON),
        component(INTERFACE_ID, CONFIRM_BUTTON),
        component(INTERFACE_ID, TOOLTIP_LAYER),
    )
    runClientScript(
        GE_SIDE_INIT,
        component(SIDE_INTERFACE_ID, 1),
        component(SIDE_INTERFACE_ID, 0),
    )

    // The buttons are all dynamic children, so ops only reach us for sub-component ranges we
    // explicitly enable. The setup editor creates around fifty children and each index box around
    // thirty; the ranges below cover them with room to spare.
    for (slot in 0 until SLOTS) {
        setInterfaceEvents(INTERFACE_ID, GrandExchange.slotComponent(slot), 0..32, GE_EVENTS)
    }
    setInterfaceEvents(INTERFACE_ID, SETUP_PANEL, 0..64, GE_EVENTS)
    setInterfaceEvents(INTERFACE_ID, STATUS_PANEL, 0..64, GE_EVENTS)
    setInterfaceEvents(INTERFACE_ID, STATUS_PROGRESS, 0..8, GE_EVENTS)
    setInterfaceEvents(INTERFACE_ID, COLLECT_BOXES, 0..8, GE_EVENTS)
    setInterfaceEvents(SIDE_INTERFACE_ID, 1, 0 until inventory.capacity, GE_EVENTS)

    clearOfferEditor()
    setVarbit(VARBIT_SELECTED_SLOT, 0)
    refreshGrandExchange()
}

/** Pushes all eight slots and their collect boxes. Cheap enough to do wholesale. */
fun Player.refreshGrandExchange() {
    for (slot in 0 until SLOTS) {
        sendGrandExchangeSlot(slot)
    }
}

/**
 * Sends one slot's offer and the two-item inventory the client draws its collect boxes from.
 *
 * `UpdateStockMarketSlot` is what drives every number on the offer box - the client has no other
 * source for them - and the collect boxes are a separate per-slot inv, so both have to go out
 * together or the box and the goods disagree.
 */
fun Player.sendGrandExchangeSlot(slot: Int) {
    val offer = GrandExchangeMarket.offer(username, slot)
    if (offer == null) {
        write(UpdateStockMarketSlot(slot, UpdateStockMarketSlot.ResetStockMarketSlot))
    } else {
        write(
            UpdateStockMarketSlot(
                slot,
                UpdateStockMarketSlot.SetStockMarketSlot(
                    status = offer.status,
                    obj = offer.item,
                    price = offer.price,
                    count = offer.amount,
                    completedCount = offer.completed,
                    completedGold = offer.spent.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                ),
            ),
        )
    }
    sendGrandExchangeCollectBox(slot)
}

/**
 * Slot 0 of the per-slot inv holds goods, slot 1 holds coins - the order `[proc,script818]` reads
 * them back in when it decides which of "Collect-items", "Collect-notes" and "Collect" to offer.
 */
fun Player.sendGrandExchangeCollectBox(slot: Int) {
    val offer = GrandExchangeMarket.offer(username, slot)
    val items = arrayOfNulls<Item>(2)
    if (offer != null) {
        if (offer.collectedItem > 0) {
            items[0] = Item(offer.collectedItemId, offer.collectedItem)
        }
        if (offer.collectedCoins > 0) {
            items[1] = Item(COINS, offer.collectedCoins.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        }
    }
    write(
        UpdateInvFull(
            inventoryId = OFFER_INVS[slot],
            capacity = items.size,
            provider = RsModObjectProvider(items),
        ),
    )
}

/**
 * Loads the offer editor with an item. The client mirrors these four vars locally as the player
 * clicks, but ours are the authoritative copies - they are what [GrandExchangePlugin] reads when
 * Confirm comes back.
 */
fun Player.setOfferEditor(
    item: Int,
    quantity: Int,
    price: Int,
) {
    setVarp(VARP_OFFER_ITEM, item)
    setVarbit(VARBIT_OFFER_QUANTITY, quantity)
    setVarbit(VARBIT_OFFER_PRICE, price)
}

fun Player.clearOfferEditor() {
    setVarp(VARP_OFFER_ITEM, 0)
    setVarbit(VARBIT_OFFER_QUANTITY, 0)
    setVarbit(VARBIT_OFFER_PRICE, 0)
    setVarbit(VARBIT_OFFER_TYPE, 0)
}

const val COINS = 995

private val GE_COLLECT_INIT = ClientScript("ge_collect_init")

/**
 * Opens the collection box - the eight-box summary a booth's or a clerk's "Collect" option leads to.
 *
 * `[clientscript,ge_collect_init]` finds its eight slot components through an enum rather than being
 * handed them, so the only components it needs from us are the two "collect everything" buttons and
 * the tooltip layer.
 */
fun Player.openGrandExchangeCollectionBox() {
    setInterfaceUnderlay(color = -1, transparency = -1)
    openInterface(interfaceId = GrandExchange.COLLECT_INTERFACE_ID, dest = InterfaceDestination.MAIN_SCREEN)

    runClientScript(
        GE_COLLECT_INIT,
        0,
        GrandExchange.COLLECT_ENUM,
        component(GrandExchange.COLLECT_INTERFACE_ID, GrandExchange.COLLECT_ALL_TO_INVENTORY),
        component(GrandExchange.COLLECT_INTERFACE_ID, GrandExchange.COLLECT_ALL_TO_BANK),
        component(GrandExchange.COLLECT_INTERFACE_ID, GrandExchange.COLLECT_TOOLTIP_LAYER),
    )

    for (slot in 0 until SLOTS) {
        setInterfaceEvents(
            GrandExchange.COLLECT_INTERFACE_ID,
            GrandExchange.collectComponent(slot),
            0..16,
            GE_EVENTS,
        )
    }
    setInterfaceEvents(GrandExchange.COLLECT_INTERFACE_ID, GrandExchange.COLLECT_ALL_TO_INVENTORY, 0..4, GE_EVENTS)
    setInterfaceEvents(GrandExchange.COLLECT_INTERFACE_ID, GrandExchange.COLLECT_ALL_TO_BANK, 0..4, GE_EVENTS)

    refreshGrandExchange()
}
