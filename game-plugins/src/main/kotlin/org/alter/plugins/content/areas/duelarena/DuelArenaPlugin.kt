package org.alter.plugins.content.areas.duelarena

import org.alter.api.ChatMessageType
import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.DEATH_HANDLED_ATTR
import org.alter.game.model.attr.EQUIP_REQUIREMENT_ITEM_ID
import org.alter.game.model.move.moveTo
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The Duel Arena.
 *
 * Two players in the arena's staging area challenge each other, stake items, agree a set of rules
 * and fight in one of the four walled arenas until one of them dies or forfeits; the winner takes
 * both stakes. See [DuelArena] for where all of this comes from in the cache, [DuelSession] for the
 * flow, and [DuelRules] for how each rule is enforced.
 */
class DuelArenaPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        // ------------------------------------------------------------------------------------
        // Challenging
        // ------------------------------------------------------------------------------------

        /*
         * "Challenge" is only offered inside the arena, the way the real one did it - there is no
         * duelling anywhere else, so an option that always sat in the right-click menu would be a
         * standing invitation to a message that always says no.
         */
        onEnterRegion(DuelArena.LOBBY_REGION) {
            player.sendOption("Challenge", DuelArena.CHALLENGE_OPTION_SLOT)
            player.message("You enter the Duel Arena. Right-click another player to Challenge them.")
        }

        onExitRegion(DuelArena.LOBBY_REGION) {
            player.removeOption(DuelArena.CHALLENGE_OPTION_SLOT)
            player.getDuelRequests().clear()
        }

        onPlayerOption(option = "Challenge") {
            val opponent = player.getInteractingPlayer()
            challenge(player, opponent)
        }

        /*
         * The stake screen's own buttons are NOT bound here.
         *
         * It is the trade screen, and a component may only be bound to one plugin - binding it in
         * both makes the second plugin to load throw in its constructor and register nothing at
         * all, silently taking player trading down with it. So TradingPlugin owns 335/336 and
         * routes the click to whichever session the player is in.
         */

        // ------------------------------------------------------------------------------------
        // Options screen
        // ------------------------------------------------------------------------------------

        /*
         * The twelve rule rows. `[clientscript,6169]` gives each one a "Toggle" op and enum 4209
         * says which component belongs to which rule, so the binding is one per component rather
         * than a single handler picking the rule apart.
         */
        DuelRule.values.forEach { rule ->
            onButton(DuelArena.OPTIONS_INTERFACE, rule.component) {
                player.getDuel()?.toggleRule(player, rule)
            }
        }

        /*
         * The worn slots. These are sub-components of 755:42 built by `duel_initworn`, so the click
         * arrives on the one component with the icon's index in the interacting slot.
         */
        onButton(DuelArena.OPTIONS_INTERFACE, DuelSession.WORN_ICON_COMPONENT) {
            val duel = player.getDuel() ?: return@onButton
            val duelSlot = DuelSlot.byIconIndex(player.getInteractingSlot()) ?: return@onButton
            duel.toggleSlot(player, duelSlot)
        }

        onButton(DuelArena.OPTIONS_INTERFACE, DuelSession.WHIP_PRESET) {
            player.getDuel()?.applyPreset(DuelRule.WHIP_PRESET)
        }
        onButton(DuelArena.OPTIONS_INTERFACE, DuelSession.BOXING_PRESET) {
            /*
             * A boxing duel is a whip duel with both hands empty, so the preset also locks the
             * weapon and shield slots - which is the part that actually makes it boxing.
             */
            player.getDuel()?.let { duel ->
                duel.applyPreset(DuelRule.BOXING_PRESET)
                duel.toggleSlot(player, DuelSlot.WEAPON)
                duel.toggleSlot(player, DuelSlot.SHIELD)
            }
        }

        onButton(DuelArena.OPTIONS_INTERFACE, DuelSession.OPTIONS_ACCEPT) { player.getDuel()?.accept(player) }
        onButton(DuelArena.OPTIONS_INTERFACE, DuelSession.OPTIONS_DECLINE) { declined(player) }

        // ------------------------------------------------------------------------------------
        // Confirm screen
        // ------------------------------------------------------------------------------------

        onButton(DuelArena.CONFIRM_INTERFACE, DuelSession.CONFIRM_ACCEPT) { player.getDuel()?.accept(player) }
        onButton(DuelArena.CONFIRM_INTERFACE, DuelSession.CONFIRM_DECLINE) { declined(player) }

        /*
         * Closing either of these calls the duel off; the stake screen's close is handled in
         * TradingPlugin along with its buttons. Only meaningful before the fight - once the players
         * are in the arena the session has taken their stakes and there is no screen to close.
         */
        listOf(DuelArena.OPTIONS_INTERFACE, DuelArena.CONFIRM_INTERFACE)
            .forEach { screen ->
                onInterfaceClose(screen) {
                    val duel = player.getDuel() ?: return@onInterfaceClose
                    if (duel.stage.isAbandonedBy(screen)) {
                        duel.abort("${player.username} declined the duel.")
                    }
                }
            }

        // ------------------------------------------------------------------------------------
        // The fight
        // ------------------------------------------------------------------------------------

        onTimer(DuelArena.COUNTDOWN_TIMER) {
            val duel = player.getDuel() ?: return@onTimer
            val remaining = player.attr[DuelArena.COUNTDOWN_LEFT] ?: 0
            if (remaining > 0) {
                player.message("$remaining...")
                player.attr[DuelArena.COUNTDOWN_LEFT] = remaining - 1
                player.timers[DuelArena.COUNTDOWN_TIMER] = 1
            } else {
                player.attr.remove(DuelArena.COUNTDOWN_LEFT)
                /*
                 * Both players run their own countdown, but only one of them may start the fight -
                 * startFighting is a no-op the second time, which is what keeps that safe.
                 */
                duel.startFighting()
            }
        }

        onTimer(DuelArena.WATCH_TIMER) {
            val duel = player.getActiveDuel()
            if (duel == null) {
                player.timers.remove(DuelArena.WATCH_TIMER)
                return@onTimer
            }
            duel.watch(player)
            if (player.getActiveDuel() != null) {
                player.timers[DuelArena.WATCH_TIMER] = 1
            }
        }

        /*
         * Death ends the duel. Claimed here so the normal respawn does not also fire and send the
         * loser to Lumbridge with the arena still holding their stake.
         */
        onPlayerPreDeath {
            val duel = player.getActiveDuel() ?: return@onPlayerPreDeath
            player.attr[DEATH_HANDLED_ATTR] = true
            // The winner is paid now, while the session is still there to pay from; the loser is
            // left in the arena so they are seen to die, and moved by the handler below.
            duel.finish(duel.other(player).player, "You have been defeated!", teleportLoser = false)
        }

        onPlayerDeath {
            if (player.attr[DUEL_AWAITING_RETURN_ATTR] != true) return@onPlayerDeath
            player.attr.remove(DUEL_AWAITING_RETURN_ATTR)
            player.moveTo(DuelArena.LOBBY_TILE)
            player.getSkills().restoreAll()
            player.getSkills().setCurrentLevel(Skills.HITPOINTS, player.getSkills().getBaseLevel(Skills.HITPOINTS))
            player.message("You have been carried to the hospital.")
        }

        /*
         * Logging out mid-duel is a forfeit, whatever the rules say about forfeiting - the
         * alternative is a duel that never ends and a stake nobody can get back.
         */
        onLogout {
            val duel = player.getDuel() ?: return@onLogout
            if (duel.isCommitted()) {
                duel.finish(duel.other(player).player, "Your opponent left the duel.")
            } else {
                duel.abort("The other player left.", notify = true)
            }
            player.getDuelRequests().clear()
        }

        onLogin { player.getDuelRequests().clear() }

        // ------------------------------------------------------------------------------------
        // Rule enforcement that has to live here
        // ------------------------------------------------------------------------------------

        /*
         * Equipment rules. This is the one gate that could not go at the action's own call site:
         * equipping is per-item everywhere else in the plugin API, and these rules are about the
         * slot, so [canEquipAnyItem] was added for them.
         */
        canEquipAnyItem {
            val item = player.attr[EQUIP_REQUIREMENT_ITEM_ID] ?: return@canEquipAnyItem true
            DuelRules.canEquip(player, item)
        }

        /*
         * Forfeiting: walking out of the arena. There are no trapdoors in this cache's arenas, so
         * leaving the floor is what ends the duel.
         */
        onCommand("forfeit", description = "forfeit the duel you are in") {
            val duel = player.getActiveDuel()
            if (duel == null) {
                player.message("You are not in a duel.")
                return@onCommand
            }
            if (!DuelRules.canForfeit(player)) return@onCommand
            duel.finish(duel.other(player).player, "You forfeited the duel.")
        }
    }

    /**
     * Sends a challenge, or accepts one already sent. Mirrors the way trade requests pair up: the
     * second of two matching challenges is what actually opens the duel.
     */
    private fun challenge(
        player: org.alter.game.model.entity.Player,
        opponent: org.alter.game.model.entity.Player,
    ) {
        if (player == opponent) return

        if (!DuelArena.inLobby(player.tile) || !DuelArena.inLobby(opponent.tile)) {
            player.message("You can only challenge someone in the Duel Arena.")
            return
        }
        if (player.hasDuel()) {
            player.message("You are already arranging a duel.")
            return
        }
        if (opponent.hasDuel() || opponent.isLocked()) {
            player.message("Other player is busy at the moment.")
            return
        }

        if (!player.getDuelRequests().contains(opponent)) {
            opponent.getDuelRequests().add(player)
            player.message("Sending duel challenge...")
            opponent.message(
                "${player.username} wishes to duel with you.",
                ChatMessageType.TRADE_REQ,
                player.username,
            )
            return
        }

        player.getDuelRequests().remove(opponent)
        opponent.getDuelRequests().remove(player)

        val session = DuelSession(player, opponent)
        player.attr[DUEL_SESSION_ATTR] = session
        opponent.attr[DUEL_SESSION_ATTR] = session
        session.openStakeScreen()
    }

    private fun declined(player: org.alter.game.model.entity.Player) {
        val duel = player.getDuel() ?: return
        if (duel.isCommitted()) return
        duel.abort("${player.username} declined the duel.")
    }

    companion object {
    }
}
