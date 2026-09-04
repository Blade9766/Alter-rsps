package org.alter.plugins.content.magic

/**
 * The client's real, native Autocast interface - the spell grid that replaces the
 * Combat Options tab when you click "Choose spell" on the Autocast or Defensive
 * Autocast row with a staff equipped.
 *
 * ## Why the server barely has to do anything
 *
 * An earlier attempt at this assumed the server had to *populate* the grid, tried to
 * push an item array into it with `sendItemContainer` and silently killed the
 * connection (no real inventory-type id exists for it, and this server runs rsprot with
 * `inventoryObjCheck`/`clientscriptVerification` on). That assumption was simply wrong.
 * Dumping interface 201 straight out of cache index 3 shows it has only **six**
 * components, and not one of them is an item grid:
 *
 * ```
 * 201:0  layer, parent -1, onLoad = script235(event_com, 201:1, 201:2)
 * 201:1  layer, full width x 175           <- the spell grid
 * 201:2  layer, full width x (parent-180)  <- the hovered-spell info panel
 * 201:3  rect  0x000000 filled             <- background
 * 201:4  rect  0x726451 outline            <- border
 * 201:5  rect  0x2e2b23 outline            <- inner border
 * ```
 *
 * The grid is built **entirely client-side**. Component 201:0's onLoad handler
 * (clientscript 235) forwards to clientscript 2098, which loops `local2 = 1..58` and
 * for every index calls `cc_create(201:1, type 5 /* graphic */, local2)`, looks the
 * spell's icon up in **enum 1986** (`i -> obj`, keys 1..58), asks clientscript 243 for
 * the icon's grid position, hides the component when either lookup returns -1, and
 * attaches `cc_setop(1, ...)` to the ones that survive. It also creates a text child
 * with id [CANCEL_SLOT] labelled "Cancel", and registers refresh hooks on inventory 93
 * and 94, on the Magic stat, and on seven varps, so the grid re-renders itself whenever
 * the player's gear, level or settings change. The server is never consulted.
 *
 * So all the server does is open the interface, and handle the op that comes back.
 * Because the spell components are dynamic children of 201:1, the click arrives as an
 * ordinary button op on `201:1` with the created child's id in
 * [org.alter.game.model.attr.INTERACTING_SLOT_ATTR] - and that id **is** the autocast
 * spell index, the same numbering as
 * [org.alter.plugins.content.combat.strategy.magic.CombatSpell.autoCastId] and as
 * enum 1986's keys (verified: our 36 combat spells use exactly 1-16, 31-46 and 48-51,
 * all of which are real enum 1986 keys).
 *
 * ## The one thing the server *must* provide
 *
 * Clientscript 243 decides which spells appear, and where, by switching on
 * **varp [AUTOCAST_WEAPON_VARP]**. Its cases are staff item ids - the staves with a
 * bespoke autocast set - and its **default case returns (-1, -1) for every spell**,
 * i.e. an empty grid. The `-1` case is the generic one: it delegates to clientscript
 * 4512, whose own switch covers spell indices 1-16 and 48-51 and hides everything else,
 * which is precisely the twenty Standard elemental spells (Strike/Bolt/Blast/Wave plus
 * the four Surges) an ordinary staff is allowed to autocast.
 *
 * So the varp has to hold the equipped weapon's item id when that weapon is one of
 * [SPECIAL_AUTOCAST_WEAPONS], and **-1 for anything else** - leave it at 0 and the
 * player gets a blank panel. That is what [weaponVarpValue] is for.
 */
object AutocastInterface {
    /** Cache index 3, archive 201. */
    const val INTERFACE_ID = 201

    /**
     * The layer clientscript 2098 creates the spell icons on, so the component every
     * spell click is reported against.
     */
    const val SPELL_GRID_COMPONENT = 1

    /**
     * The "Cancel" text child clientscript 2098 creates first, before the spell loop
     * starts at 1. Selecting it switches autocast back off.
     */
    const val CANCEL_SLOT = 0

    /**
     * The highest spell index clientscript 2098 creates a child for - its loop runs
     * `local2 = 1..58`, and enum 1986 has keys over exactly that range.
     */
    const val MAX_SPELL_SLOT = 58

    /**
     * Every dynamic child the grid can produce, Cancel included. This is the sub-range
     * that has to be handed to
     * [org.alter.api.ext.setInterfaceEvents]: the client sets each icon's op text with
     * `cc_setop(1, ...)`, but an op only actually fires at the server once the server
     * has enabled it, so without this the grid draws perfectly and every click, Cancel
     * included, does nothing at all.
     */
    val SPELL_SLOTS = CANCEL_SLOT..MAX_SPELL_SLOT

    /**
     * Read by clientscript 243 to pick the autocast spell set. Holds a
     * [SPECIAL_AUTOCAST_WEAPONS] item id, or -1 for the standard elemental set.
     */
    const val AUTOCAST_WEAPON_VARP = 664

    /**
     * The Combat Options tab component for **Defensive** Autocast, the upper of the two
     * rows. It is the one drawn with a shield sprite beside the spell icon (`593:25`,
     * sprite 760), and it passes `1` as the last argument of its clientscript 329 hook,
     * which that script compares against `Combat.DEFENSIVE_MAGIC_CAST_VARBIT` to decide
     * which row shows the chosen spell and gets highlighted.
     */
    const val DEFENSIVE_ROW_COMPONENT = 22

    /**
     * The Combat Options tab component for plain Autocast, the lower row, with the spell
     * icon centred and no shield. It passes `0` to clientscript 329.
     *
     * These two are easy to get backwards, and getting them backwards looks exactly like
     * a broken toggle: the spell is set, but the highlight and the icon land on the other
     * row. The row order is not guessable from the component numbers - `22` is above `27`
     * because `22` has `yMode=0` and `27` has `yMode=2`.
     */
    const val AUTOCAST_ROW_COMPONENT = 27

    /**
     * `Varp.WEAPON_ATTACK_STYLE` values for the two spell rows, as
     * [org.alter.plugins.content.combat.WeaponStyles] indexes them for
     * `WeaponType.MAGIC_STAFF` and `WeaponType.STAFF_HALBERD`: slots 0/1/3 are the
     * melee Bash/Pound/Focus buttons, slot 4 is `CAST` and slot 5 is `DEFENSIVE_CAST`.
     *
     * These have to be right or the wrong experience is awarded, because
     * `CombatConfigs.getXpMode` resolves the mode through that same table:
     * slot 4 gives `XpMode.MAGIC` and slot 5 gives `XpMode.SHARED`, which is what earns
     * the Defence experience that makes defensive casting defensive. Pointing the varp
     * at a melee slot instead silently drops it. Values 4 and 5 also match no case in
     * the client's own melee-highlight script 324, which only handles 0-3, so the melee
     * buttons correctly show nothing selected.
     */
    const val AUTOCAST_STYLE = 4
    const val DEFENSIVE_AUTOCAST_STYLE = 5

    /**
     * Every weapon clientscript 243 has a dedicated `switch` case for, and therefore
     * the only ids worth writing into [AUTOCAST_WEAPON_VARP] verbatim. Names resolved
     * from the cache's own item definitions.
     */
    val SPECIAL_AUTOCAST_WEAPONS =
        setOf(
            9013, // Skull sceptre
            21276, // Skull sceptre (i)
            4675, // Ancient staff
            4170, // Slayer's staff
            8841, // Void knight mace
            1409, // Iban's staff
            11791, // Staff of the dead
            22296, // Staff of light
            24144, // Staff of balance
            27785, // Thammaron's sceptre (au)
            27788, // Thammaron's sceptre (a)
            27676, // Accursed sceptre (au)
            27679, // Accursed sceptre (a)
            21006, // Kodai wand
            4710, // Ahrim's staff
        )

    /**
     * What [AUTOCAST_WEAPON_VARP] should hold for a player holding [weaponId] (-1 for
     * an empty weapon slot).
     */
    fun weaponVarpValue(weaponId: Int): Int = if (weaponId in SPECIAL_AUTOCAST_WEAPONS) weaponId else -1
}
