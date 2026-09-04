package org.alter.plugins.content.interfaces.gameframe.tabs.settings

import org.alter.api.ext.InterfaceEvent
import org.alter.api.ext.closeInterface
import org.alter.api.ext.getInteractingSlot
import org.alter.api.ext.getVarbit
import org.alter.api.ext.getVarp
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.api.ext.setInterfaceEvents
import org.alter.api.ext.setVarbit
import org.alter.api.ext.setVarp
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The category the player last selected in the "All Settings" panel.
 *
 * The client keeps this in varbit 9656 and sets it itself when a tab is clicked, so the server would
 * normally have no reason to hold it. It needs it here because the category decides what the rows
 * are, and therefore what a sub index on `134:19` means.
 */
val SETTINGS_CATEGORY_ATTR = AttributeKey<Int>()

/**
 * Set while the player is searching, because a search rebuilds the list from matches across every
 * category and the server cannot see the typed text.
 *
 * Row clicks are ignored while it is set rather than guessed at - the client still applies them
 * locally, so the player sees the toggle move; only the server's copy is skipped, and selecting a
 * category tab clears the flag and puts the two back in step.
 */
val SETTINGS_SEARCHING_ATTR = AttributeKey<Boolean>()

/**
 * Makes the "All Settings" panel (interface 134) work.
 *
 * ### The problem this solves
 *
 * Interface 134 is drawn entirely by the client. Its onload runs `[clientscript,3826]`, which reads
 * enum 422 and the category structs and `cc_create`s a row per setting. Clicking a row runs
 * `[clientscript,3847]`, which flips the setting's varbit **in the client's own copy** for instant
 * feedback. Nothing in the protocol carries a setting id, so all the server receives is a button
 * click with a sub index on `134:19`.
 *
 * That is why settings did not survive a logout before this plugin: the client changed them, the
 * server never learned, and the next login pushed the server's untouched varps back over the top.
 *
 * The server therefore reproduces the client's row allocation - see [SettingsLayout] - resolves the
 * sub back to a setting, and applies the same change to its own copy. Because both sides compute the
 * same new value, the varp the server sends back agrees with what the client already drew.
 *
 * ### Safety
 *
 * A wrong mapping would silently toggle the wrong setting, so this refuses rather than guesses: a sub
 * with no row, a row the server has no var for, and any click made while the search box is in use are
 * all dropped. `::settingsdebug` reports what the server computed so a mismatch can be seen directly.
 */
class SettingsPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {

    init {
        loadService(SettingsService())

        onLogin {
            player.attr[SETTINGS_CATEGORY_ATTR] = 0
            player.attr[SETTINGS_SEARCHING_ATTR] = false
            player.openSettingsEvents()
            SettingsEffects.onLogin(player)
        }

        /**
         * Category tabs. The client sets varbit 9656 itself; the server mirrors it so that its idea
         * of the visible list and the client's cannot drift, and so a relog restores the same tab.
         */
        onButton(interfaceId = INTERFACE_ID, component = COMPONENT_CATEGORY_TABS) {
            val index = player.getInteractingSlot()
            val category = player.settings()?.category(index) ?: return@onButton
            player.attr[SETTINGS_CATEGORY_ATTR] = category.index
            player.attr[SETTINGS_SEARCHING_ATTR] = false
            player.setVarbit(VARBIT_CATEGORY, category.index)
        }

        /**
         * A settings row. [SettingsLayout] turns the sub back into the setting the client drew there.
         */
        onButton(interfaceId = INTERFACE_ID, component = COMPONENT_ROWS) {
            val sub = player.getInteractingSlot()
            player.applySettingAt(sub)
        }

        /**
         * The search box. The list stops being the plain category list from here on, so row clicks
         * are parked until a category tab is selected again.
         */
        onButton(interfaceId = INTERFACE_ID, component = COMPONENT_SEARCH) {
            player.attr[SETTINGS_SEARCHING_ATTR] = true
        }

        /**
         * Close. `[clientscript,3828]` puts the "Close" op on this component and leaves its second op
         * blank, and it carries no handler of its own, so the window stays open until the server
         * shuts it.
         */
        onButton(interfaceId = INTERFACE_ID, component = COMPONENT_CLOSE) {
            player.closeInterface(INTERFACE_ID)
        }

        /**
         * A freshly opened window is always showing a plain category list, so this is where the
         * search flag is cleared.
         *
         * Deliberately on open rather than on close. The window is opened as a sub of 161:18 and not
         * as a modal, so pressing Esc closes it in the client without the server's interface set ever
         * hearing about it - `[CloseModal]` only reaches whatever `interfaces.getModal()` holds, which
         * is not this. Resetting on close would therefore be skipped for exactly the case that needs
         * it: search, press Esc, reopen, and every row click would go on being ignored.
         */
        onInterfaceOpen(interfaceId = INTERFACE_ID) {
            player.attr[SETTINGS_SEARCHING_ATTR] = false
        }

        onCommand("settingsdebug", "dev") {
            player.reportSettingsLayout()
        }
    }

    /**
     * Opens the ops the client needs to report back.
     *
     * The rows of 134 are `cc_create`d, so they carry no click mask of their own and stay silent
     * unless the server opens the parent's sub range - the same rule the pronoun list on the makeover
     * window runs into. The upper bounds are the layer sizes the client's builders can reach.
     */
    private fun Player.openSettingsEvents() {
        setInterfaceEvents(INTERFACE_ID, COMPONENT_CATEGORY_TABS, 0..9, InterfaceEvent.ClickOp1)
        setInterfaceEvents(INTERFACE_ID, COMPONENT_ROWS, 0..449, InterfaceEvent.ClickOp1)
        setInterfaceEvents(INTERFACE_ID, COMPONENT_LABELS, 0..219, InterfaceEvent.ClickOp1)
        setInterfaceEvents(INTERFACE_ID, COMPONENT_DROPDOWNS, 0..41, InterfaceEvent.ClickOp1)
    }

    private fun Player.settings(): SettingsService? = world.getService(SettingsService::class.java)

    private fun Player.layout(): SettingsLayout? {
        val service = settings() ?: return null
        val category = service.category(attr[SETTINGS_CATEGORY_ATTR] ?: 0) ?: return null
        return service.layout(category, getVarbit(VARBIT_MEMBERS))
    }

    private fun Player.applySettingAt(sub: Int) {
        if (attr[SETTINGS_SEARCHING_ATTR] == true) {
            return
        }
        val layout = layout() ?: return
        val entry = layout.settingAt(sub) ?: return
        if (!entry.isLive) {
            // Colour pickers, keybind rows and the settings whose value `[clientscript,3960]`
            // computes rather than stores. The client handles these on its own.
            return
        }
        if (!entry.isToggle) {
            // Sliders, dropdowns and buttons carry their new value in the click, and each type reads
            // it differently; only the on/off rows are safe to derive from the current value alone.
            return
        }
        writeSetting(entry, if (readSetting(entry) == 0) 1 else 0)
        SettingsEffects.onChanged(this, entry.settingId)
    }

    private fun Player.readSetting(entry: SettingEntry): Int =
        when (entry.varKind) {
            "varbit" -> getVarbit(entry.varId)
            else -> getVarp(entry.varId)
        }

    private fun Player.writeSetting(
        entry: SettingEntry,
        value: Int,
    ) {
        when (entry.varKind) {
            "varbit" -> setVarbit(entry.varId, value)
            else -> setVarp(entry.varId, value)
        }
    }

    /**
     * Prints what the server believes the open category looks like. The one thing that cannot be
     * checked from the cache alone is whether this client hides the same rows the profile does, and
     * comparing a row's printed sub against the one its click reports is how that gets settled.
     */
    private fun Player.reportSettingsLayout() {
        val service = settings()
        if (service == null) {
            message("The settings catalogue is not loaded.")
            return
        }
        val category = service.category(attr[SETTINGS_CATEGORY_ATTR] ?: 0)
        if (category == null) {
            message("No settings category is selected.")
            return
        }
        val layout = service.layout(category, getVarbit(VARBIT_MEMBERS))
        message("Settings category ${category.index} (${category.name}): ${layout.size} rows on $INTERFACE_ID:$COMPONENT_ROWS.")
        layout.rows().forEachIndexed { sub, entry ->
            val target = if (entry.isLive) "${entry.varKind} ${entry.varId} = ${readSetting(entry)}" else "client-side"
            message("  sub $sub: ${entry.title.ifEmpty { "(untitled)" }} [$target]")
        }
    }

    private companion object {
        /** The "All Settings" panel. */
        const val INTERFACE_ID = 134

        /** The layer the client `cc_create`s the clickable part of each row onto. */
        const val COMPONENT_ROWS = 19

        /** The layer the labels and descriptions go on; headers use only this one. */
        const val COMPONENT_LABELS = 18

        const val COMPONENT_CATEGORY_TABS = 23
        const val COMPONENT_DROPDOWNS = 28
        const val COMPONENT_SEARCH = 10
        const val COMPONENT_CLOSE = 4

        /** The client's own record of which category tab is showing, read by `[clientscript,3840]`. */
        const val VARBIT_CATEGORY = 9656

        /** Membership, tested by `[clientscript,3955]` for the members-only rows. */
        const val VARBIT_MEMBERS = 1777
    }
}
