package org.alter.plugins.content.items.jewellery

import org.alter.api.ext.hit
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player

/**
 * The ring of recoil - the Lvl-1 Enchant sapphire ring - reflecting part of every hit taken.
 *
 * Wired into [org.alter.plugins.content.combat.dealExactHit], the one place every attack in the game
 * funnels through, for the same reason [org.alter.plugins.content.mechanics.poison.CombatPoison] is:
 * specials and multi-projectile attacks then get it for free, and each of their hits recoils
 * separately, which is how OSRS behaves.
 *
 * The rules, from the wiki:
 *
 *  - 10% + 1 of the damage received, rounded down, is dealt back to whatever caused it - `damage /
 *    10 + 1`. Both players and NPCs recoil.
 *  - The attack has to actually deal damage; a blocked hit recoils nothing.
 *  - The ring can deal 40 damage in total before it shatters, and that budget is tracked **per
 *    player, not per ring**, which is [PerkJewellery]'s shared rule and where the count lives.
 *
 * @see <a href="https://oldschool.runescape.wiki/w/Ring_of_recoil">Ring of recoil - OSRS Wiki</a>
 */
object RingOfRecoil {
    /**
     * Reflects [damage] taken by [target] back onto [attacker], if [target] is a player wearing a
     * ring of recoil. Called when a hit resolves, not when it is thrown.
     */
    fun apply(
        attacker: Pawn,
        target: Pawn,
        damage: Int,
    ) {
        if (damage <= 0 || attacker === target) {
            return
        }
        if (target !is Player || !JewelleryPerks.isActive(target, PerkJewellery.RING_OF_RECOIL)) {
            return
        }

        val left = JewelleryPerks.remaining(target, PerkJewellery.RING_OF_RECOIL)
        val recoil = (damage / 10 + 1).coerceAtMost(left)

        attacker.hit(damage = recoil, delay = 0, attackersIndex = target.index)
        // Spends exactly what was reflected, so a ring with 3 damage left reflects 3 and shatters
        // rather than reflecting a full 10% it does not have.
        JewelleryPerks.consume(target, PerkJewellery.RING_OF_RECOIL, recoil)
    }
}
