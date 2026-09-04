package org.alter.plugins.content.interfaces.gameframe.tabs.settings

import org.alter.api.ext.getVarbit
import org.alter.api.ext.removeOption
import org.alter.api.ext.sendOption
import org.alter.api.ext.setVarbit
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player

/**
 * Records which round of first-login defaults a player has had.
 *
 * Stored as a version rather than a flag so a later default can be added without re-applying the
 * earlier ones over a choice the player has since made. A scalar, because that is what the attribute
 * persistence can carry.
 */
val SETTINGS_DEFAULTS_ATTR = AttributeKey<Int>(persistenceKey = "settings_defaults")

/**
 * The settings that mean something to the server, and what they do.
 *
 * Most of the catalogue is the client describing itself to itself - highlight colours, respawn
 * timers, roof hiding - and needs nothing here beyond the var being remembered. This holds the few
 * where the server is the one that has to act, and gives other content a way to read a setting
 * without knowing which var backs it.
 */
object SettingsEffects {

    /**
     * Settings whose value has to be pushed somewhere the moment it changes, rather than being read
     * when it is next needed.
     */
    private val APPLIED_IMMEDIATELY =
        setOf(
            Setting.PLAYER_TRADE_OPTIONS,
            Setting.PLAYER_REPORT_OPTIONS,
        )

    /**
     * Applies the settings that have to be pushed to the client, and gives a brand new player the
     * defaults the live game ships with.
     *
     * The defaults matter because an untouched varp is 0, which for these would mean a new account
     * started with no Trade option, no Report option, an escape key that did nothing, and no
     * hitpoints/prayer/run/special-attack orbs around the minimap.
     */
    fun onLogin(player: Player) {
        if ((player.attr[SETTINGS_DEFAULTS_ATTR] ?: 0) < DEFAULTS_VERSION) {
            player.setVarbit(Setting.PLAYER_TRADE_OPTIONS.varbit, 1)
            player.setVarbit(Setting.PLAYER_REPORT_OPTIONS.varbit, 1)
            player.setVarbit(Setting.ESC_CLOSES_INTERFACE.varbit, 1)
            player.setVarbit(Setting.SHOW_DATA_ORBS.varbit, 1)
            player.attr[SETTINGS_DEFAULTS_ATTR] = DEFAULTS_VERSION
        }
        APPLIED_IMMEDIATELY.forEach { apply(player, it) }
    }

    /** Called after the player changes a setting, from wherever they changed it. */
    fun onChanged(
        player: Player,
        settingId: Int,
    ) {
        val setting = Setting.byId(settingId) ?: return
        if (setting in APPLIED_IMMEDIATELY) {
            apply(player, setting)
        }
    }

    /** Whether [setting] is switched on for this player. */
    fun isEnabled(
        player: Player,
        setting: Setting,
    ): Boolean = player.getVarbit(setting.varbit) != 0

    private fun apply(
        player: Player,
        setting: Setting,
    ) {
        when (setting) {
            // The right-click options on other players. The server is what puts them there, so
            // hiding one means taking it back off rather than simply not sending it again.
            Setting.PLAYER_TRADE_OPTIONS -> player.setPlayerOption(TRADE_OPTION_SLOT, "Trade with", isEnabled(player, setting))
            Setting.PLAYER_REPORT_OPTIONS -> player.setPlayerOption(REPORT_OPTION_SLOT, "Report", isEnabled(player, setting))
            else -> Unit
        }
    }

    private fun Player.setPlayerOption(
        slot: Int,
        option: String,
        shown: Boolean,
    ) {
        if (shown) {
            sendOption(option, slot)
        } else {
            removeOption(slot)
        }
    }

    /**
     * Bump when a new entry is added to the first-login defaults above, so existing players pick it
     * up once without their other choices being reset.
     */
    private const val DEFAULTS_VERSION = 2

    private const val TRADE_OPTION_SLOT = 4
    private const val REPORT_OPTION_SLOT = 5
}

/**
 * The settings the server acts on by name.
 *
 * The id is the cache's own setting id (struct param 1077) and the varbit is the one
 * `[clientscript,3960]` maps it to, so these agree with `data/cfg/settings/settings.json` by
 * construction rather than by coincidence - [SettingsService] checks that on startup.
 */
enum class Setting(
    val settingId: Int,
    val varbit: Int,
) {
    PLAYER_TRADE_OPTIONS(settingId = 312, varbit = 6580),
    PLAYER_REPORT_OPTIONS(settingId = 372, varbit = 1334),
    ESC_CLOSES_INTERFACE(settingId = 57, varbit = 4681),
    MUSIC_UNLOCK_MESSAGE(settingId = 33, varbit = 10078),
    MUSIC_LOOPING(settingId = 348, varbit = 4137),
    ACCEPT_AID(settingId = 59, varbit = 4180),
    SHIFT_CLICK_DROP(settingId = 51, varbit = 11556),
    AMMO_PICKING(settingId = 305, varbit = 5697),
    RUNE_PICKING(settingId = 306, varbit = 5698),

    /**
     * The hitpoints, prayer, run energy and special attack orbs around the minimap.
     *
     * **1 shows them, 0 hides them** - the catalogue entry is titled "Show data orbs" ("when
     * enabled, data orbs are shown"), so an untouched varbit of 0 means a new account logs in with
     * no orbs at all, and the settings that depend on them refuse to change ("You must have data
     * orbs turned on to change this setting"). `api/cfg/Varbit` carries this same varbit twice
     * under contradictory names, `SHOW_DATA_ORBS` and `HIDE_DATA_ORBS_VARBIT`; the catalogue is
     * what settles which way round it goes.
     */
    SHOW_DATA_ORBS(settingId = 8, varbit = 4084),
    ;

    companion object {
        private val byId = entries.associateBy { it.settingId }

        fun byId(settingId: Int): Setting? = byId[settingId]
    }
}

/** Reads a named setting straight off the player, for content that does not want the var id. */
fun Player.settingEnabled(setting: Setting): Boolean = SettingsEffects.isEnabled(this, setting)

/** The raw value, for the settings that are not simply on or off. */
fun Player.settingValue(setting: Setting): Int = getVarbit(setting.varbit)
