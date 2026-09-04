package org.alter.plugins.content.combat.specialattack

import org.alter.api.Skills
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.combat.PawnHit
import org.alter.plugins.content.combat.dealExactHit
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.strategy.MeleeCombatStrategy

/**
 * A melee special attack's damage, with the combat experience an ordinary swing would have given.
 *
 * [org.alter.plugins.content.combat.CombatPlugin] runs a special *instead of*
 * [MeleeCombatStrategy.attack], and the strategy is the only thing that awards melee experience -
 * so every special that reached for [dealHit] on its own gave none. Specials should go through
 * these two rather than [dealHit] directly.
 *
 * Experience is awarded from the hitmarks the moment the hit is built, matching what the ordinary
 * swing does: a special that queues several hits therefore pays out per hit, which is correct.
 *
 * The default delay is 0 for the same reason [org.alter.plugins.content.combat.strategy.MeleeCombatStrategy]
 * uses it - melee has no travel time, so the splat belongs on the cycle of the swing. Multi-hit
 * specials space their later hits out from there.
 *
 * The default delay is 0 for the same reason [MeleeCombatStrategy.attack] uses 0 - melee has no
 * travel time, so the hit lands on the cycle of the swing. Specials that deliberately stagger
 * several splats pass their own delays.
 */
fun Player.dealMeleeSpecialHit(
    target: Pawn,
    maxHit: Int,
    landHit: Boolean,
    delay: Int = 0,
): PawnHit = dealHit(target = target, maxHit = maxHit, landHit = landHit, delay = delay).awardMeleeXp(this, target)

/** As [dealMeleeSpecialHit], for a special that has already rolled its own damage. */
fun Player.dealExactMeleeSpecialHit(
    target: Pawn,
    damage: Int,
    landHit: Boolean,
    delay: Int = 0,
): PawnHit =
    dealExactHit(target = target, damage = damage, landHit = landHit, delay = delay).awardMeleeXp(this, target)

private fun PawnHit.awardMeleeXp(
    player: Player,
    target: Pawn,
): PawnHit {
    val damage = hit.hitmarks.sumOf { it.damage }
    if (damage > 0) {
        MeleeCombatStrategy.addCombatXp(player, target, damage)
    }
    return this
}

/**
 * A magic special attack's damage, with the Magic experience it earns.
 *
 * The staff specials and the Voidwaker deal magic damage without casting a spell, so nothing in
 * [org.alter.plugins.content.combat.strategy.MagicCombatStrategy] pays them out. Two Magic
 * experience per point of damage, the rate the wiki gives for the Voidwaker and the powered staves.
 */
fun Player.dealMagicSpecialHit(
    target: Pawn,
    damage: Int,
    landHit: Boolean,
    delay: Int = 2,
): PawnHit {
    val hit = dealExactHit(target = target, damage = damage, landHit = landHit, delay = delay)
    val dealt = hit.hit.hitmarks.sumOf { it.damage }
    if (dealt > 0) {
        addXp(Skills.MAGIC, dealt * MAGIC_XP_PER_DAMAGE)
    }
    return hit
}

private const val MAGIC_XP_PER_DAMAGE = 2.0

