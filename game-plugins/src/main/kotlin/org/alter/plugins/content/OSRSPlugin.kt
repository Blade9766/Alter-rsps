package org.alter.plugins.content

import org.alter.api.*
import org.alter.api.CommonClientScripts
import org.alter.api.InterfaceDestination
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*

class OSRSPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        /**
         * Closing main modal for players.
         */
        setModalCloseLogic {
            val modal = player.interfaces.getModal()
            if (modal != -1) {
                player.closeInterface(modal)
                player.interfaces.setModal(-1)
            }
        }
        /**
         * Check if the player has a menu opened.
         */
        setMenuOpenCheck {
            player.getInterfaceAt(dest = InterfaceDestination.MAIN_SCREEN) != -1
        }

        /**
         * Execute when a player logs in.
         */
        onLogin {
            with(player) {
                /**
                 * @TODO Inspect, uhh seems that this logic is being repeated, not removing it yet as im unsure rn if it's needed or not
                 */
                // Skill-related logic.
                calculateAndSetCombatLevel()
                if (getSkills().getBaseLevel(Skills.HITPOINTS) < 10) {
                    getSkills().setBaseLevel(Skills.HITPOINTS, 10)
                }
                calculateAndSetCombatLevel()
                sendWeaponComponentInformation()
                sendCombatLevelText()
                /*
                 * The inventory's ops. An item's five options do NOT map to ops 2..6 - **op 5 is
                 * skipped**, so they arrive as ops 2, 3, 4, 6, 7, with op10 for Examine. Measured
                 * from live packet logs against two items across all five indices; the mapping and
                 * the evidence live on `KotlinPlugin.inventoryOpOf`, which binds against it.
                 *
                 * ClickOp5 is kept in the mask even though no item option lands there, because it
                 * costs nothing and the op is only presumed - not proven - to be reserved for Use.
                 * Ops 1, 8 and 9 are deliberately absent: no item option can reach them.
                 */
                setInterfaceEvents(
                    interfaceId = 149,
                    component = 0,
                    range = 0..27,
                    setting =
                        arrayOf(
                            InterfaceEvent.ClickOp2,
                            InterfaceEvent.ClickOp3,
                            InterfaceEvent.ClickOp4,
                            InterfaceEvent.ClickOp5,
                            InterfaceEvent.ClickOp6,
                            InterfaceEvent.ClickOp7,
                            InterfaceEvent.ClickOp10,
                            InterfaceEvent.UseOnGroundItem,
                            InterfaceEvent.UseOnNpc,
                            InterfaceEvent.UseOnObject,
                            InterfaceEvent.UseOnPlayer,
                            InterfaceEvent.UseOnInventory,
                            InterfaceEvent.UseOnComponent,
                            InterfaceEvent.DRAG_DEPTH1,
                            InterfaceEvent.DragTargetable,
                            InterfaceEvent.ComponentTargetable,
                        ),
                )
                player.openDefaultInterfaces()
                setVarbit(Varbit.COMBAT_LEVEL_VARBIT, combatLevel)
                setVarbit(Varbit.CHATBOX_UNLOCKED, 1)
                runClientScript(CommonClientScripts.INTRO_MUSIC_RESTORE)
                if (getVarp(Varp.PLAYER_HAS_DISPLAY_NAME) == 0 && username.isNotBlank()) {
                    syncVarp(Varp.PLAYER_HAS_DISPLAY_NAME)
                }
                // Sync attack priority options.
                syncVarp(Varp.NPC_ATTACK_PRIORITY_VARP)
                syncVarp(Varp.PLAYER_ATTACK_PRIORITY_VARP)
                // Send player interaction options.
                sendOption("Follow", 3)
                /*
                 * "Trade with" and "Report" are sent by SettingsEffects instead, because the player
                 * can turn either of them off in Settings and sending them unconditionally here
                 * would put them back on every login.
                 */
                // Game-related logic.
                sendRunEnergy(player.runEnergy.toInt())
                message("Welcome to ${world.gameContext.name}.", ChatMessageType.GAME_MESSAGE)
                // player.social.pushFriends(player)
                // player.social.pushIgnores(player)
                /*
                 * Esc-closes-interface is settings entry 57 and the player is allowed to switch it
                 * off. Forcing it on here turned it back on at every login, so the default now lands
                 * once, from SettingsEffects, and the player's own choice survives after that.
                 */

                /**
                 * @TODO
                 * As for now these varbit's disable Black bar on right side for Native client,
                 * The black bar is for loot tracker n whatnot
                 */
                setVarbit(13982, 1)
                setVarbit(13981, 1)
            }
        }



        // TODO Whats this for:?
        onButton(245, 20) {
            player.openInterface(interfaceId = 626, dest = InterfaceDestination.MAIN_SCREEN)
        }
    }

    fun Player.openDefaultInterfaces() {
        openOverlayInterface(interfaces.displayMode)
        openModals(this)
        setInterfaceEvents(interfaceId = 239, component = 3, range = 0..665, setting = 6) // enable music buttons
        initInterfaces(interfaces.displayMode)
    }

    fun openModals(
        player: Player,
        fullscreen: Boolean = false,
    ) {
        InterfaceDestination.getModals().forEach { pane ->
            if (pane == InterfaceDestination.XP_COUNTER && player.getVarbit(Varbit.XP_DROPS_VISIBLE_VARBIT) == 0) {
                return@forEach
            }
            /*
             * The minimap is opened unconditionally, where it used to be withheld whenever varbit
             * 4084 was 1.
             *
             * That test was backwards - 4084 is "Show data orbs", so 1 is the *on* state - but
             * making it read the right way round would not have been right either. Interface 160
             * carries the compass and world-map button as well as the four orbs, and the client
             * already hides just the orbs off that varbit; withholding the whole interface takes
             * the compass with them. Leaving the decision to the client also removes a login-order
             * race, since [org.alter.plugins.content.interfaces.gameframe.tabs.settings.SettingsEffects]
             * seeds the varbit's default from its own login hook, which may run after this one.
             */
            player.openInterface(pane.interfaceId, pane, fullscreen)
        }
    }

}
