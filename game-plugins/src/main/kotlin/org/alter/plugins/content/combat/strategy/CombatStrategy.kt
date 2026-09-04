package org.alter.plugins.content.combat.strategy

import org.alter.game.model.entity.Pawn

/**
 * @author Tom <rspsmods@gmail.com>
 */
interface CombatStrategy {
    /**
     * Whether this strategy's attack travels, and so tests *line of sight* against the target
     * rather than line of walk.
     *
     * [org.alter.plugins.content.combat.CombatPlugin] used to decide this by comparing the chosen
     * strategy against [MeleeCombatStrategy] by identity, which silently made every strategy that
     * is not that exact object a projectile attack - fine while the only others were magic, ranged
     * and the salamander, all of which do throw something, but wrong for any future melee-range
     * strategy. Declaring it here keeps that decision with the strategy that knows the answer.
     */
    val usesProjectile: Boolean
        get() = true

    fun getAttackRange(pawn: Pawn): Int

    fun canAttack(
        pawn: Pawn,
        target: Pawn,
    ): Boolean

    fun attack(
        pawn: Pawn,
        target: Pawn,
    )
}
