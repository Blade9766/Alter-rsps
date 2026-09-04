package org.alter.plugins.content.mechanics.appearance

import net.rsprot.protocol.game.outgoing.interfaces.IfSetPlayerModelSelf
import org.alter.api.InterfaceDestination
import org.alter.api.ext.InterfaceEvent
import org.alter.api.ext.closeInterface
import org.alter.api.ext.getInteractingOption
import org.alter.api.ext.getInteractingSlot
import org.alter.api.ext.openInterface
import org.alter.api.ext.player
import org.alter.api.ext.setInterfaceEvents
import org.alter.api.ext.setVarbit
import org.alter.game.Server
import org.alter.game.info.PlayerInfo
import org.alter.game.model.World
import org.alter.game.model.appearance.Gender
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.mechanics.appearance.Makeover.ALL_STEPPERS
import org.alter.plugins.content.mechanics.appearance.Makeover.BODY_TYPE_A
import org.alter.plugins.content.mechanics.appearance.Makeover.BODY_TYPE_B
import org.alter.plugins.content.mechanics.appearance.Makeover.CONFIRM
import org.alter.plugins.content.mechanics.appearance.Makeover.INTERFACE_ID
import org.alter.plugins.content.mechanics.appearance.Makeover.PREVIEW
import org.alter.plugins.content.mechanics.appearance.Makeover.PRONOUN_DROPDOWN_ROWS
import org.alter.plugins.content.mechanics.appearance.Makeover.PRONOUN_ROW_SUBS
import org.alter.plugins.content.mechanics.appearance.Makeover.VARBIT_BODY_TYPE
import org.alter.plugins.content.mechanics.appearance.Makeover.VARBIT_PRONOUN
import org.alter.plugins.content.mechanics.appearance.Makeover.asBodyType
import org.alter.plugins.content.mechanics.appearance.Makeover.pronounForSub

/**
 * Run when the player presses Confirm on the makeover window, and dropped if they close it any
 * other way. This is how the Makeover Mage gets their line in after the spell "finishes"; nothing
 * about the window itself needs it.
 */
val MAKEOVER_ON_CONFIRM = AttributeKey<(Player) -> Unit>()

/** Backs [Player.pronoun]. Stored as [Pronoun.ordinal] so it saves as a plain int. */
val PRONOUN_ATTR = AttributeKey<Int>(persistenceKey = "pronoun")

/**
 * The makeover window: interface 679, the screen the Makeover Mage opens and the same one the
 * game uses to design a brand new character.
 *
 * Edits apply as they are made rather than being held back until Confirm, which is what the window
 * itself expects - the model at [Makeover.PREVIEW] is the player's own, so the only way to preview
 * a change is to make it. Confirm therefore just shuts the window; there is no cancel, exactly as
 * in the live game.
 *
 * See [Makeover] for where the component numbers and the two vars behind the body type and pronoun
 * rows came from.
 */
class AppearanceInterfacePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {

    init {
        // The client never learns of a body type or pronoun choice on its own, so a returning
        // player's saved appearance is pushed back into the vars the window reads.
        onLogin {
            player.syncMakeoverVars()
        }

        onInterfaceClose(INTERFACE_ID) {
            player.attr.remove(MAKEOVER_ON_CONFIRM)
        }

        // --- The twelve steppers --------------------------------------------------------------
        ALL_STEPPERS.forEach { stepper ->
            onButton(interfaceId = INTERFACE_ID, component = stepper.previous) {
                player.step(stepper, -1)
            }
            onButton(interfaceId = INTERFACE_ID, component = stepper.next) {
                player.step(stepper, 1)
            }
        }

        // --- Body type ------------------------------------------------------------------------
        onButton(interfaceId = INTERFACE_ID, component = BODY_TYPE_A) {
            player.setBodyType(Gender.MALE)
        }
        onButton(interfaceId = INTERFACE_ID, component = BODY_TYPE_B) {
            player.setBodyType(Gender.FEMALE)
        }

        // --- Pronouns -------------------------------------------------------------------------
        // Only the open list is bound. The closed control's own clicks just open that list, and
        // treating one of those as a selection would set a pronoun the player never picked.
        onButton(interfaceId = INTERFACE_ID, component = PRONOUN_DROPDOWN_ROWS) {
            val choice = pronounForSub(player.getInteractingSlot())
            if (choice != null && player.getInteractingOption() == 1) {
                player.pronoun = choice
            }
        }

        // --- Confirm --------------------------------------------------------------------------
        onButton(interfaceId = INTERFACE_ID, component = CONFIRM) {
            val onConfirm = player.attr[MAKEOVER_ON_CONFIRM]
            player.attr.remove(MAKEOVER_ON_CONFIRM)
            player.closeInterface(INTERFACE_ID)
            onConfirm?.invoke(player)
        }
    }
}

/**
 * Opens the makeover window, optionally running [onConfirm] when the player presses Confirm.
 *
 * The window is opened as a modal on the main screen; the vars behind the body type and pronoun
 * rows are pushed first so it draws the player's current choices ticked, and the preview model is
 * pointed at the player.
 */
fun Player.openMakeover(onConfirm: ((Player) -> Unit)? = null) {
    if (onConfirm != null) {
        attr[MAKEOVER_ON_CONFIRM] = onConfirm
    } else {
        attr.remove(MAKEOVER_ON_CONFIRM)
    }
    syncMakeoverVars()
    openInterface(interfaceId = INTERFACE_ID, dest = InterfaceDestination.MAIN_SCREEN)
    // The pronoun rows do not exist yet - the client builds them when the list is opened - and
    // nothing it builds itself is clickable through to us unless the sub range is opened up here.
    setInterfaceEvents(
        interfaceId = INTERFACE_ID,
        component = PRONOUN_DROPDOWN_ROWS,
        range = PRONOUN_ROW_SUBS,
        setting = InterfaceEvent.ClickOp1,
    )
    sendMakeoverPreview()
}

/**
 * Points the window's model component at this player.
 *
 * Worn equipment is deliberately left off it: the window exists to preview identikits and colours,
 * and a platebody would hide most of what the arrows change.
 */
fun Player.sendMakeoverPreview() {
    write(IfSetPlayerModelSelf(interfaceId = INTERFACE_ID, componentId = PREVIEW, copyObjs = false))
}

/**
 * Restates the two vars the window reads but does not own - body type and pronouns - from the
 * values the server holds.
 */
fun Player.syncMakeoverVars() {
    setVarbit(VARBIT_BODY_TYPE, if (appearance.gender == Gender.MALE) 0 else 1)
    setVarbit(VARBIT_PRONOUN, pronoun.ordinal)
}

/**
 * The pronouns this player has chosen, or the ones that go with their body type until they choose.
 *
 * The server's own copy is the attribute; varbit [Makeover.VARBIT_PRONOUN] is the mirror the client
 * reads, refreshed at login and on every change - the same split body type already has between
 * [org.alter.game.model.appearance.Appearance.gender] and varbit [Makeover.VARBIT_BODY_TYPE]. It
 * is the attribute rather than the varbit that is authoritative because a var cannot say "not
 * chosen yet": 0 is a real pronoun choice, so a player who has never visited the mage would be
 * indistinguishable from one who picked he/him.
 */
var Player.pronoun: Pronoun
    get() =
        attr[PRONOUN_ATTR]?.let { Pronoun.of(it) }
            ?: if (appearance.gender == Gender.MALE) Pronoun.HE else Pronoun.SHE
    set(value) {
        attr[PRONOUN_ATTR] = value.ordinal
        setVarbit(VARBIT_PRONOUN, value.ordinal)
    }

/** Steps one row of the window by [direction], wrapping around at either end. */
private fun Player.step(
    stepper: Makeover.Stepper,
    direction: Int,
) {
    val size = stepper.size(appearance)
    if (size <= 0) {
        return
    }
    val next = Math.floorMod(stepper.get(appearance) + direction, size)
    stepper.set(appearance, next)
    refreshAppearance()
}

/**
 * Switches body type, carrying as much of the current design across as the other body type's
 * identikit tables can hold.
 */
private fun Player.setBodyType(gender: Gender) {
    if (appearance.gender == gender) {
        return
    }
    appearance = appearance.asBodyType(gender)
    // Pronouns a player has never set follow their body type, so the dropdown's label has to be
    // restated alongside the body type itself.
    syncMakeoverVars()
    refreshAppearance()
}

private fun Player.refreshAppearance() {
    PlayerInfo(this).syncAppearance()
    // The preview is a separate model from the one in the world and does not follow the appearance
    // update block, so it is re-pointed at the player after every change.
    if (interfaces.isVisible(INTERFACE_ID)) {
        sendMakeoverPreview()
    }
}
