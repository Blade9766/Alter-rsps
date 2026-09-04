package org.alter.plugins.content.combat.specialattack

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.SPECIAL_ATTACK_BURN_ATTR
import org.alter.game.model.attr.SPECIAL_ATTACK_BURN_SOURCE_ATTR
import org.alter.game.model.entity.Pawn
import org.alter.game.model.timer.DAMAGE_TAKEN_MODIFIER_TIMER
import org.alter.game.model.timer.MELEE_DAMAGE_TAKEN_MODIFIER_TIMER
import org.alter.game.model.timer.SPECIAL_ATTACK_BURN_TIMER
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.dealExactHit

/**
 * The bookkeeping behind the special attacks that leave something running on a pawn after the hit:
 * the two temporary damage modifiers, and burn.
 *
 * All three are timers rather than anything held in the special itself, because the effect has to
 * outlive the attack and has to work on npcs as well as players - `onTimer` fires for any pawn, so
 * one plugin covers both.
 *
 * **Burn** is the Varlamore demonbane effect: damage dealt slowly, which the eclipse atlatl can cut
 * short and cash in. That is why it is a counter that ticks rather than a run of queued hits like
 * [SpecialAttackEffects.damageOverTime] - a queued hit cannot be called back, and Eclipse has to be
 * able to take the remainder off the target and put it into its own max hit.
 */
class SpecialAttackTemporariesPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onTimer(DAMAGE_TAKEN_MODIFIER_TIMER) {
            (ctx as? Pawn)?.attr?.remove(Combat.DAMAGE_TAKE_MULTIPLIER)
        }

        onTimer(MELEE_DAMAGE_TAKEN_MODIFIER_TIMER) {
            (ctx as? Pawn)?.attr?.remove(Combat.MELEE_DAMAGE_TAKE_MULTIPLIER)
        }

        onTimer(SPECIAL_ATTACK_BURN_TIMER) {
            val pawn = ctx as? Pawn ?: return@onTimer
            val remaining = pawn.attr[SPECIAL_ATTACK_BURN_ATTR] ?: 0
            if (remaining <= 0 || !pawn.isAlive()) {
                SpecialAttackEffects.clearBurn(pawn)
                return@onTimer
            }

            val damage = minOf(SpecialAttackEffects.BURN_DAMAGE_PER_TICK, remaining)
            /*
             * Burn is credited to whoever applied it, so its damage still counts toward the kill -
             * but the source is a weak reference and the attacker may be long gone, in which case
             * the burn simply stops rather than dealing unattributed damage.
             */
            val source = pawn.attr[SPECIAL_ATTACK_BURN_SOURCE_ATTR]?.get()
            if (source == null || !source.isAlive()) {
                SpecialAttackEffects.clearBurn(pawn)
                return@onTimer
            }

            source.dealExactHit(target = pawn, damage = damage, landHit = true, delay = 0)
            pawn.attr[SPECIAL_ATTACK_BURN_ATTR] = remaining - damage

            if (remaining - damage > 0) {
                pawn.timers[SPECIAL_ATTACK_BURN_TIMER] = SpecialAttackEffects.BURN_TICKS_BETWEEN_SPLATS
            } else {
                SpecialAttackEffects.clearBurn(pawn)
            }
        }
    }
}
