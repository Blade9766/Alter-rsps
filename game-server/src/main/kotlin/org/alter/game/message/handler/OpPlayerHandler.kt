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
         * Attack and Follow are the player options that must not be walked to the target first.
         *
         * [PawnPathAction.walkPlugin] routes all the way to the other player's tile and then
         * waits out `while (pawn.hasMoveDestination())` before running the option, so every
         * option it carries is resolved from an adjacent tile. That is right for Trade with and
         * Challenge, which are one-shot interactions that need to happen face to face.
         *
         * It is wrong for Attack: a ranged or magic attacker was walked into melee distance
         * before the first shot, because the range the attack actually needs is only known to
         * the combat loop, which had not started yet.
         *
         * It is wrong for Follow too, for two reasons. The walk is one-shot - it routes to the
         * tile the target stood on when the option was clicked and never re-routes, because the
         * `if (!other.tile.sameAs(other.tile))` re-walk guard in [PawnPathAction] compares a tile
         * to itself and so can never fire - which is fine for an interaction that ends on arrival
         * and useless for one whose whole job is to keep up with a moving player. And on the way
         * out, [PawnPathAction] calls `resetFacePawn()` on any option that did not leave combat
         * focus set; the follow loop reads exactly that facing as its "still following" signal,
         * so a followed-through-walkPlugin follow would be cancelled the tick after it started.
         *
         * Both are handed straight to the option instead, exactly as [OpNpcHandler] does for an
         * npc's Attack (which is always op2 - checked against the cache, monsters carry it in the
         * second slot). `Pawn.attack` faces the target and starts the combat loop, and that loop
         * does its own approach against `CombatStrategy.getAttackRange`, stopping as soon as the
         * target is in range with line of sight; `Follow.start` does the same against a fixed
         * range of one tile.
         *
         * Keeping the approach in the option also keeps it honest about freezes and stuns, which
         * `walkRoute` now refuses outright - see `Pawn.isRooted`.
         */
        val selfApproaching = SELF_APPROACHING_OPTIONS.firstOrNull { it.equals(client.options[optionIndex], ignoreCase = true) }
        if (selfApproaching != null) {
            client.world.plugins.executePlayerOption(client, selfApproaching)
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

        /**
         * The option name following is bound to, by
         * `org.alter.plugins.content.mechanics.follow.FollowPlugin`. Sent on slot 3 by
         * `OSRSPlugin`, but matched by name for the same reason [ATTACK_OPTION] is.
         */
        const val FOLLOW_OPTION = "Follow"

        /**
         * The options that walk themselves. Matched case-insensitively against what the client
         * was sent, but dispatched under the constant, because `executePlayerOption` looks its
         * binding up by exact name.
         */
        val SELF_APPROACHING_OPTIONS = arrayOf(ATTACK_OPTION, FOLLOW_OPTION)
    }
}
