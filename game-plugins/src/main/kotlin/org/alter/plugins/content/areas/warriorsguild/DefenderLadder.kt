package org.alter.plugins.content.areas.warriorsguild

import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player

/**
 * The defender ladder: which defender a cyclops is allowed to drop next, and the rule that makes
 * it a ladder rather than a table.
 *
 * A cyclops does not roll seven defenders independently. It drops **the next one the player has
 * not got**, at 1/50, and - the part that is easy to miss and that this models explicitly -
 * "every time the player obtains a defender, they have to leave the room, and then re-enter the
 * room to be able to receive the next defender". So getting a bronze defender stops the drops
 * dead until the player steps out through Kamfreena's doors and comes back.
 *
 * The dragon defender is not on this ladder. It comes from the level 106 cyclopes in the basement,
 * at 1/100, and only once the rune defender is already held - which is also what Lorelai checks
 * before opening her door.
 */
object DefenderLadder {
    /**
     * The rungs, in order. Index into this with [progress]; a player who has none is at 0 and is
     * owed the bronze defender.
     */
    val RUNGS =
        listOf(
            "item.bronze_defender",
            "item.iron_defender",
            "item.steel_defender",
            "item.black_defender",
            "item.mithril_defender",
            "item.adamant_defender",
            "item.rune_defender",
        )

    const val DRAGON_DEFENDER = "item.dragon_defender"

    /** 1/50 on the top floor, per the wiki's `2/100`. */
    const val RUNG_CHANCE = 1.0 / 50.0

    /** 1/100 for the dragon defender in the basement. */
    const val DRAGON_CHANCE = 1.0 / 100.0

    /**
     * How far up the ladder the player has climbed: 0 for none, 7 once the rune defender is held.
     *
     * Persisted, and a plain Int - persistent attributes here have to be scalars.
     */
    val PROGRESS = AttributeKey<Int>(persistenceKey = "warriors_guild_defender")

    /**
     * Set the moment a defender drops, cleared when the player leaves the cyclops room.
     *
     * This is the leave-and-re-enter rule. It is deliberately *not* persisted: logging out and
     * back in is not "leaving the room", but a player who does so lands outside it anyway, and the
     * room-exit check clears it on the first tick either way.
     */
    val AWAITING_REENTRY = AttributeKey<Boolean>()

    fun progress(player: Player): Int = (player.attr[PROGRESS] ?: 0).coerceIn(0, RUNGS.size)

    /** The rscm key of the defender this player is owed next, or null once the ladder is done. */
    fun nextRung(player: Player): String? = RUNGS.getOrNull(progress(player))

    fun hasRuneDefender(player: Player): Boolean = progress(player) >= RUNGS.size

    /**
     * Whether a cyclops may drop [nextRung] on this kill.
     *
     * False while the player still owes the room a lap after their last defender, and false once
     * the ladder is finished.
     */
    fun canReceive(player: Player): Boolean = nextRung(player) != null && player.attr[AWAITING_REENTRY] != true

    fun award(player: Player) {
        player.attr[PROGRESS] = progress(player) + 1
        player.attr[AWAITING_REENTRY] = true
    }

    /** Called when the player steps out of a cyclops room, which re-arms the ladder. */
    fun leftRoom(player: Player) {
        player.attr.remove(AWAITING_REENTRY)
    }
}
