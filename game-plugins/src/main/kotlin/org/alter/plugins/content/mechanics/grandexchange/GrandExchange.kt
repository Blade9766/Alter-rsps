package org.alter.plugins.content.mechanics.grandexchange

/**
 * Interface, component, sub-component and var ids for the Grand Exchange.
 *
 * Interface 465 is almost entirely empty in the cache - it holds a handful of panels and two
 * buttons, and every box, icon and number inside it is built at runtime by the client script
 * `[clientscript,ge_offers_init]` (script 803), which the server has to call itself and hand the
 * fourteen components it should build into. Everything below was read out of the rev-228 cache and
 * out of that script family, so the sub-component indices are the ones `cc_create` actually assigns
 * rather than guesses:
 *
 *  - `[proc,ge_offers_index_initslot]` builds each of the eight index boxes. Its create order gives
 *    sub 2 the click region ("View offer" / "Abort offer") and subs 3 and 4 the two halves of an
 *    empty box ("Create Buy offer" / "Create Sell offer"). Subs 5..15 are consumed by
 *    `[proc,stonepanel]`, which is why the icons land at 26 and 27 rather than 15 and 16.
 *  - `[proc,ge_offers_setup_init]` builds the offer editor; its first sixteen creates are the
 *    buttons, in the fixed order captured by [SETUP_SUB_CHOOSE_ITEM] onwards.
 *  - `[proc,script816]` builds the two collect boxes at subs 2 and 3.
 *
 * The client mirrors quantity and price in varbits 4396/4398 as you click, purely so the numbers
 * move without a round trip - there is no packet that carries a client var back to us. The server
 * therefore has to run the same arithmetic on the same button presses and stays authoritative; see
 * [GrandExchangePlugin].
 */
object GrandExchange {

    /** The Grand Exchange window itself. */
    const val INTERFACE_ID = 465

    /** The inventory panel shown beside it, drawn by `[clientscript,ge_offer_side_init]`. */
    const val SIDE_INTERFACE_ID = 467

    /**
     * The collection box - the eight-box summary reachable from a booth's or a clerk's "Collect"
     * option. Built by `[clientscript,ge_collect_init]`, which takes an enum of its slot components
     * rather than being told them one by one; [COLLECT_ENUM] is the one that holds 402:5..402:12.
     */
    const val COLLECT_INTERFACE_ID = 402
    const val COLLECT_ENUM = 1011
    const val COLLECT_TOOLTIP_LAYER = 2
    const val COLLECT_ALL_TO_INVENTORY = 3
    const val COLLECT_ALL_TO_BANK = 4
    const val COLLECT_FIRST_SLOT = 5

    /**
     * `[proc,ge_collect_initslot]` puts the two slot backgrounds at subs 1 and 2 and the two item
     * icons - the ones that carry the collect ops - at 3 and 4.
     */
    const val COLLECT_BOX_SUB_ITEM = 3
    const val COLLECT_BOX_SUB_COINS = 4

    const val SLOTS = 8

    // ---------------------------------------------------------------------------------------
    // Static components of 465, in the roles ge_offers_init expects to be handed them.
    // ---------------------------------------------------------------------------------------

    /** Hosts the panel-switch listeners. */
    const val ROOT = 0

    /** The bordered window; its steelborder holds the "Grand Exchange" title. */
    const val WINDOW = 1

    /** Full-window overlay the client draws offer tooltips onto. */
    const val TOOLTIP_LAYER = 2

    /** Bottom-left button, labelled by `[proc,ge_offers_bigbutton]`. */
    const val HISTORY_BUTTON = 3

    /** Returns to the eight-box index. Panel switching itself is client side. */
    const val BACK_BUTTON = 4

    /** The eight-box overview. */
    const val INDEX_PANEL = 5

    /** Strip above the boxes. */
    const val INDEX_TOP = 6

    /** Slot boxes occupy [FIRST_SLOT] until `FIRST_SLOT + SLOTS`. */
    const val FIRST_SLOT = 7

    /** Shown while an offer is running. */
    const val STATUS_PANEL = 15

    /** 150px text the coin icon is fitted against. */
    const val STATUS_PRICE_ICON = 17

    /** Progress bar plus the "Abort offer" button at sub 0. */
    const val STATUS_PROGRESS = 23

    /** The two boxes holding what is waiting to be collected. */
    const val COLLECT_BOXES = 24

    /** Shown while an offer is being written. */
    const val SETUP_PANEL = 25

    const val SETUP_PRICE_ICON = 27

    const val CONFIRM_BUTTON = 29

    // ---------------------------------------------------------------------------------------
    // Sub-components. These are indices into the dynamic children the client scripts create.
    // ---------------------------------------------------------------------------------------

    /** Index box: the click region over a running offer. */
    const val SLOT_SUB_VIEW = 2

    /** Index box: left half of an empty box. */
    const val SLOT_SUB_BUY = 3

    /** Index box: right half of an empty box. */
    const val SLOT_SUB_SELL = 4

    const val SETUP_SUB_CHOOSE_ITEM = 0
    const val SETUP_SUB_QUANTITY_MINUS_1 = 1
    const val SETUP_SUB_QUANTITY_PLUS_1_ARROW = 2
    const val SETUP_SUB_QUANTITY_PLUS_1 = 3
    const val SETUP_SUB_QUANTITY_PLUS_10 = 4
    const val SETUP_SUB_QUANTITY_PLUS_100 = 5

    /** "All" on a sell offer, "+1K" on a buy offer. */
    const val SETUP_SUB_QUANTITY_BULK = 6

    /** The "..." button - opens the enter-quantity dialog. */
    const val SETUP_SUB_QUANTITY_ENTER = 7

    const val SETUP_SUB_PRICE_MINUS_1 = 8
    const val SETUP_SUB_PRICE_PLUS_1 = 9
    const val SETUP_SUB_PRICE_MINUS_5_PERCENT = 10
    const val SETUP_SUB_PRICE_GUIDE = 11
    const val SETUP_SUB_PRICE_ENTER = 12
    const val SETUP_SUB_PRICE_PLUS_5_PERCENT = 13

    /** Collect boxes: item side and coin side. */
    const val COLLECT_SUB_ITEM = 2
    const val COLLECT_SUB_COINS = 3

    /** `[proc,script819]` puts "Abort offer" at sub 0 of the progress bar. */
    const val STATUS_SUB_ABORT = 0

    // ---------------------------------------------------------------------------------------
    // Vars. The client writes the first four itself as you click; we write them too, and ours
    // is the value that survives, because a server varp update overwrites the local one.
    // ---------------------------------------------------------------------------------------

    /** The item the offer being written is for; null (0) means "Choose an item...". */
    const val VARP_OFFER_ITEM = 1151

    const val VARBIT_OFFER_QUANTITY = 4396

    /** 0 buying, 1 selling. */
    const val VARBIT_OFFER_TYPE = 4397

    const val VARBIT_OFFER_PRICE = 4398

    /** Selected slot + 1; 0 means the index panel is showing. */
    const val VARBIT_SELECTED_SLOT = 4439

    /**
     * The per-slot inventories the client draws the collect boxes from, in slot order.
     * `enum_150` maps GE slot to inv id and these are the eight it holds.
     */
    val OFFER_INVS = intArrayOf(518, 519, 520, 521, 522, 523, 539, 540)

    /** Packs an interface and component into the single int the client scripts take. */
    fun component(
        interfaceId: Int,
        componentId: Int,
    ): Int = (interfaceId shl 16) or componentId

    /** The index box component for a slot. */
    fun slotComponent(slot: Int): Int = FIRST_SLOT + slot

    /** The collection box component for a slot. */
    fun collectComponent(slot: Int): Int = COLLECT_FIRST_SLOT + slot
}
