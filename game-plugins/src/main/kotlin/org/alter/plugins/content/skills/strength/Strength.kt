package org.alter.plugins.content.skills.strength

import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player

/**
 * The parts of Strength that are not combat.
 *
 * Almost all of the skill is trained by hitting things, and that half already lives in
 * [org.alter.plugins.content.combat.strategy.MeleeCombatStrategy] - aggressive style pays four
 * times the damage dealt, controlled a third of that in each of four skills. What is left is the
 * handful of places the level is spent rather than earned, and this object holds the state those
 * places share.
 */
object Strength {
    /**
     * Whether Otto Godblessed has taught the player to fish without a harpoon.
     *
     * Persisted as a bare boolean rather than as part of any larger record: a save attribute has
     * to be a scalar to survive a round trip, and this is a one-way unlock that never needs to
     * hold anything more than "yes".
     */
    val BAREHAND_FISHING_ATTR = AttributeKey<Boolean>(persistenceKey = "barehand_fishing_unlocked")

    fun hasBarehandFishing(player: Player): Boolean = player.attr[BAREHAND_FISHING_ATTR] == true

    fun unlockBarehandFishing(player: Player) {
        player.attr[BAREHAND_FISHING_ATTR] = true
    }
}
