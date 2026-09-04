package org.alter.plugins.content.interfaces.gameframe.tabs.settings.options.tabs

import org.alter.api.ClientScript
import org.alter.api.cfg.Varbit
import org.alter.api.cfg.Varp
import org.alter.api.ext.InterfaceEvent
import org.alter.api.ext.getInteractingSlot
import org.alter.api.ext.getVarbit
import org.alter.api.ext.getVarp
import org.alter.api.ext.openInterface
import org.alter.api.ext.player
import org.alter.api.ext.runClientScript
import org.alter.api.ext.setInterfaceEvents
import org.alter.api.ext.setVarbit
import org.alter.api.ext.setVarp
import org.alter.api.ext.toggleDisplayInterface
import org.alter.api.ext.toggleVarbit
import org.alter.api.ext.toggleVarp
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.attr.DISPLAY_MODE_CHANGE_ATTR
import org.alter.game.model.attr.INTERACTING_SLOT_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.model.interf.DisplayMode
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.Plugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.interfaces.options.OptionsTab

/**
 * The Settings side tab, interface 116: the Controls / Audio / Display panels and the button that
 * opens the full "All Settings" window.
 *
 * ### Component numbers
 *
 * These were read back out of the cache rather than carried over, because most of the ones this file
 * used before did not exist as buttons in this revision. Interface 116 has only fifteen components
 * with a click mask; everything else is a type-5 graphic, and a binding on one of those can never
 * fire. The three tab buttons, all four mute buttons and the music unlock toggle were all bound to
 * graphics, so none of them did anything, and the Display tab was additionally bound to the value for
 * Audio.
 *
 * The tab buttons are identified by `[clientscript,3910]`, which labels 59 "Controls", 67 "Audio" and
 * 68 "Display" and hands each one an index for varbit [Varbit.SETTINGS_TAB_FOCUS]. The four volume
 * groups are identified by their own onload scripts - 7099 on component 84 is Master, 3933 on 98 is
 * Music, 3934 on 112 is Sound Effects and 3935 on 126 is Area Sound - each of which sets its mute
 * button's text with `if_setop`. The mute button of each group is the first child.
 *
 * ### What is deliberately not bound
 *
 * The four volume *bars* and the brightness and camera-zoom bars are dragged, not clicked: they carry
 * an `onMouseRepeat` handler (`[clientscript,526]`) and no op at all, so there is no click for the
 * server to receive. The bindings that used to exist for them pointed at the slider's background
 * sprites. House Options (31) and the Bond Pouch (33) are left unbound because neither has content
 * behind it yet; they are live buttons, so they can be picked up as soon as there is. Toggle Run
 * (30) belongs to [org.alter.plugins.content.mechanics.run.RunEnergyPlugin], which is where the
 * "not enough run energy" case lives.
 */
class OptionsTabFirstPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {

    init {
        onLogin {
            // The shared dropdown's rows are populated at open time and carry no click mask of their
            // own, so they are the one part of this interface the server has to open itself. Every
            // button below already has op 1 in its cache mask.
            player.setInterfaceEvents(
                interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB,
                component = DROPDOWN_ROWS,
                range = 0..DROPDOWN_MAX_ROWS,
                setting = InterfaceEvent.ClickOp1,
            )
        }

        /**
         * Changing display modes (fixed, resizable).
         */
        setWindowStatusLogic {
            val change = player.attr[DISPLAY_MODE_CHANGE_ATTR]
            val mode =
                when (change) {
                    2 ->
                        if (player.getVarbit(Varbit.SIDESTONES_ARRAGEMENT_VARBIT) == 1) {
                            DisplayMode.RESIZABLE_LIST
                        } else {
                            DisplayMode.RESIZABLE_NORMAL
                        }
                    else -> DisplayMode.FIXED
                }
            player.toggleDisplayInterface(mode)
        }

        bindSetting(DISPLAY_MODE) {
            val slot = player.attr[INTERACTING_SLOT_ATTR]!!
            val mode =
                when (slot) {
                    2 -> {
                        player.setVarbit(Varbit.SIDESTONES_ARRAGEMENT_VARBIT, 0)
                        DisplayMode.RESIZABLE_NORMAL
                    }
                    3 -> {
                        player.setVarbit(Varbit.SIDESTONES_ARRAGEMENT_VARBIT, 1)
                        DisplayMode.RESIZABLE_LIST
                    }
                    else -> DisplayMode.FIXED
                }
            if (!(mode.isResizable() && player.interfaces.displayMode.isResizable())) {
                player.runClientScript(ClientScript("settings_client_mode"), slot - 1)
            }
            player.toggleDisplayInterface(mode)
        }

        bindSetting(PLAYER_ATTACK_OPTION) {
            player.setVarp(Varp.PLAYER_ATTACK_PRIORITY_VARP, player.getInteractingSlot() - 1)
        }

        bindSetting(NPC_ATTACK_OPTION) {
            player.setVarp(Varp.NPC_ATTACK_PRIORITY_VARP, player.getInteractingSlot() - 1)
        }

        bindSetting(ACCEPT_AID_BUTTON) {
            player.toggleVarp(Varp.ACCEPT_AID_VARP)
        }

        bindSetting(SKULL_PROTECTION_BUTTON) {
            player.toggleVarbit(Varbit.PK_PREVENT_SKULL)
        }

        bindSetting(ZOOM_TOGGLE_BUTTON) {
            player.toggleVarbit(Varbit.DISABLE_ZOOM)
        }

        bindSetting(MUSIC_UNLOCK_MESSAGE) {
            player.toggleVarbit(OptionsTab.MUSIC_UNLOCK_MESSAGE_VARBIT)
        }

        /**
         * The three tab buttons. The client switches the visible panel itself from
         * `[clientscript,3915]`; mirroring it here is what makes the tab survive a relog.
         */
        bindSetting(TAB_CONTROLS) { player.setVarbit(Varbit.SETTINGS_TAB_FOCUS, 0) }
        bindSetting(TAB_AUDIO) { player.setVarbit(Varbit.SETTINGS_TAB_FOCUS, 1) }
        bindSetting(TAB_DISPLAY) { player.setVarbit(Varbit.SETTINGS_TAB_FOCUS, 2) }

        bindSetting(MUTE_MASTER_SOUND) { player.toggleVolume(Varp.MASTER_SOUND_VOLUME, MASTER_SOUND_VOLUME) }
        bindSetting(MUTE_MUSIC) { player.toggleVolume(Varp.AUDIO_MUSIC_VOLUME, AUDIO_MUSIC_VOLUME) }
        bindSetting(MUTE_SOUND) { player.toggleVolume(Varp.AUDIO_SOUND_EFFECT_VOLUME, SOUND_EFFECT_VOLUME) }
        bindSetting(MUTE_AREA_SOUND) { player.toggleVolume(Varp.AUDIO_AREA_SOUND_VOLUME, AREA_SOUND_VOLUME) }

        /**
         * Opens the "All Settings" window. The ops its rows need are opened once at login by
         * [org.alter.plugins.content.interfaces.gameframe.tabs.settings.SettingsPlugin], which is
         * also what handles the clicks that come back.
         */
        bindSetting(ALL_SETTINGS_BUTTON) {
            player.openInterface(parent = 161, child = 18, interfaceId = OptionsTab.ALL_SETTINGS_INTERFACE_ID)
        }
    }

    /**
     * Mute restores the volume the player had rather than a fixed level, which is what the client's
     * own "Unmute" op implies. The remembered level is per session; a mute that outlives a logout
     * comes back as a mute, because the volume varp itself is what persists.
     */
    private fun Player.toggleVolume(
        varp: Int,
        remembered: AttributeKey<Int>,
    ) {
        if (getVarp(varp) == 0) {
            setVarp(varp, attr[remembered] ?: FULL_VOLUME)
        } else {
            attr[remembered] = getVarp(varp)
            setVarp(varp, 0)
        }
    }

    private fun bindSetting(
        child: Int,
        logic: Plugin.() -> Unit,
    ) {
        onButton(interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB, component = child) {
            logic(this)
        }
    }

    private companion object {
        /** Controls panel. */
        const val SKULL_PROTECTION_BUTTON = 5
        const val ACCEPT_AID_BUTTON = 29
        const val ALL_SETTINGS_BUTTON = 32

        /** Tab strip, labelled by `[clientscript,3910]`. */
        const val TAB_CONTROLS = 59
        const val TAB_AUDIO = 67
        const val TAB_DISPLAY = 68

        /** Audio panel: the mute button of each volume group. */
        const val MUTE_MASTER_SOUND = 85
        const val MUTE_MUSIC = 99
        const val MUTE_SOUND = 113
        const val MUTE_AREA_SOUND = 128
        const val MUSIC_UNLOCK_MESSAGE = 127

        /** Display panel. Op 1 toggles scroll-wheel zoom; op 2 is the client's own reset. */
        const val ZOOM_TOGGLE_BUTTON = 44

        /**
         * The rows of the one dropdown this interface shares between its settings. Which setting a
         * row belongs to depends on the dropdown that opened it, which is why they are bound
         * individually rather than as a range.
         */
        const val DROPDOWN_ROWS = 36
        const val DROPDOWN_MAX_ROWS = 5
        const val PLAYER_ATTACK_OPTION = 38
        const val NPC_ATTACK_OPTION = 39
        const val DISPLAY_MODE = 41

        const val FULL_VOLUME = 100

        val AUDIO_MUSIC_VOLUME = AttributeKey<Int>()
        val SOUND_EFFECT_VOLUME = AttributeKey<Int>()
        val AREA_SOUND_VOLUME = AttributeKey<Int>()
        val MASTER_SOUND_VOLUME = AttributeKey<Int>()
    }
}
