package org.alter.plugins.content.areas.duelarena

import org.alter.api.ext.removeOption
import org.alter.api.ext.sendOption
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player

/**
 * The duel a player is currently part of, at any stage from the stake screen to the fight itself.
 */
val DUEL_SESSION_ATTR = AttributeKey<DuelSession>()

/**
 * The players who have recently challenged this one. Mirrors the way trade requests work: a
 * challenge from someone already on this set is what actually opens the duel.
 */
val DUEL_REQUESTS_ATTR = AttributeKey<HashSet<Player>>()

/**
 * Set on the loser of a duel that ended in a death, and read once the death animation has run.
 *
 * The payout has to happen the moment the killing blow lands - that is the only point the session
 * still exists - but moving the loser then would teleport them out before they visibly die, so the
 * two are split and this carries the intent across the death sequence.
 */
val DUEL_AWAITING_RETURN_ATTR = AttributeKey<Boolean>()

fun Player.getDuel(): DuelSession? = attr[DUEL_SESSION_ATTR]

fun Player.hasDuel(): Boolean = attr.has(DUEL_SESSION_ATTR)

/**
 * The duel this player is actually *fighting*, as opposed to one still being negotiated. Rule
 * enforcement asks for this, so that nothing is blocked while the two are still on the stake screen.
 */
fun Player.getActiveDuel(): DuelSession? = getDuel()?.takeIf { it.isFighting() }

fun Player.getDuelRequests(): HashSet<Player> =
    attr[DUEL_REQUESTS_ATTR] ?: HashSet<Player>().also { attr[DUEL_REQUESTS_ATTR] = it }

/**
 * Whether [rule] applies to this player right now. False whenever they are not in a live duel, so
 * every caller can use it as a plain "is this forbidden" test.
 */
fun Player.duelForbids(rule: DuelRule): Boolean = getActiveDuel()?.hasRule(rule) == true

/**
 * Gives this player the "Challenge" option if they are standing in the arena, and takes it away if
 * they are not.
 *
 * Called from the region hooks, from login, and from the end of a duel. Relying on the region hook
 * alone left a player without the option after a fight: they are teleported out of the arena and
 * back to the lobby, and whether that reads as a region change depends on where they happened to be
 * standing. Setting it from the tile is true regardless of how they got there.
 */
fun Player.refreshChallengeOption() {
    if (DuelArena.inLobby(tile)) {
        sendOption("Challenge", DuelArena.CHALLENGE_OPTION_SLOT)
    } else {
        removeOption(DuelArena.CHALLENGE_OPTION_SLOT)
    }
}
