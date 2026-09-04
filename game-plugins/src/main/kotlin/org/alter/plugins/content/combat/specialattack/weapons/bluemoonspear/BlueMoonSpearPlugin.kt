package org.alter.plugins.content.combat.specialattack.weapons.bluemoonspear

import org.alter.api.cfg.Animation
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.timer.FROZEN_TIMER
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Blue moon spear - **Break Shackles**: the longer the target has left on a binding spell, the
 * harder this hits - and it ends the bind in the process.
 *
 * The bonus scales linearly with the freeze the target still has to serve, topping out at double
 * accuracy and damage against a freshly cast twenty-second bind and falling to nothing against an
 * unbound target. Reading [FROZEN_TIMER] is the whole measurement; the same timer is then cleared,
 * which is the "ending said effect" half.
 *
 * The cache's Frostmoon set requirement is not enforced - there is no set effect here to require.
 */
class BlueMoonSpearPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Break Shackles") {
            player.animate(Animation.HUMAN_SPECIAL_SPEAR_STAB)

            val victim = target
            val remaining = if (victim.timers.has(FROZEN_TIMER)) victim.timers[FROZEN_TIMER] else 0
            val bonus = 1.0 + (remaining.toDouble() / FULL_BIND_TICKS).coerceAtMost(1.0)
            victim.timers.remove(FROZEN_TIMER)

            val maxHit = MeleeCombatFormula.getMaxHit(player, victim, specialAttackMultiplier = bonus)
            val accuracy = MeleeCombatFormula.getAccuracy(player, victim, specialAttackMultiplier = bonus)
            player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = accuracy >= world.randomDouble())
        }
    }

    private companion object {
        /** A full-strength bind is twenty seconds, which is thirty-three ticks. */
        const val FULL_BIND_TICKS = 33
    }
}
