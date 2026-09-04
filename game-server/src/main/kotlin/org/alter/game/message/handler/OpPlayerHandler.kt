package org.alter.game.message.handler

import net.rsprot.protocol.game.incoming.players.OpPlayer
import org.alter.game.model.move.PawnPathAction
import org.alter.game.message.MessageHandler
import org.alter.game.model.attr.INTERACTING_OPT_ATTR
import org.alter.game.model.attr.INTERACTING_PLAYER_ATTR
import org.alter.game.model.entity.Client
import java.lang.ref.WeakReference

/**
 * @author Triston Plummer ("Dread")
 */
class OpPlayerHandler : MessageHandler<OpPlayer> {
    override fun consume(
        client: Client,
        message: OpPlayer,
    ) {
        val index = message.index
        // The interaction option id.
        val option = message.op
        // The index of the option in the player's option array.
        val optionIndex = option - 1

        if (!client.lock.canPlayerInteract()) {
            return
        }

        val other = client.world.players[index] ?: return

        if (client.options[optionIndex] == null || other == client) {
            return
        }

        log(client, "Player option: name=%s, opt=%d", other.username, option)

        client.closeInterfaceModal()
        client.interruptQueues()
        client.resetInteractions()

        client.attr[INTERACTING_PLAYER_ATTR] = WeakReference(other)
        client.attr[INTERACTING_OPT_ATTR] = option

        /*
         * Attacking is the one player option that must not walk to the target first.
         *
         * [PawnPathAction.walkPlugin] routes all the way to the other player's tile and then
         * waits out `while (pawn.hasMoveDestination())` before running the option, so every
         * option it carries is resolved from an adjacent tile. That is right for Follow and
         * Trade with, and wrong for Attack: a ranged or magic attacker was walked into melee
         * distance before the first shot, because the range the attack actually needs is only
         * known to the combat loop, which had not started yet.
         *
         * Combat is handed straight to the option instead, exactly as [OpNpcHandler] does for
         * an npc's Attack (which is always op2 - checked against the cache, monsters carry it
         * in the second slot). `Pawn.attack` faces the target and starts the combat loop, and
         * that loop does its own approach against `CombatStrategy.getAttackRange`, stopping as
         * soon as the target is in range with line of sight.
         *
         * Keeping the approach in the combat loop also keeps it honest about freezes and stuns,
         * which `walkRoute` now refuses outright - see `Pawn.isRooted`.
         */
        if (client.options[optionIndex].equals(ATTACK_OPTION, ignoreCase = true)) {
            client.world.plugins.executePlayerOption(client, ATTACK_OPTION)
            return
        }

        client.executePlugin(PawnPathAction.walkPlugin)
    }

    private companion object {
        /**
         * The option name combat is bound to, by
         * `org.alter.plugins.content.combat.CombatPlugin`'s `onPlayerOption("Attack")`. Sent on
         * a slot of the server's choosing - the Duel Arena uses slot 1 as a left-click option -
         * so it is matched by name rather than by op number.
         */
        const val ATTACK_OPTION = "Attack"
    }
}
