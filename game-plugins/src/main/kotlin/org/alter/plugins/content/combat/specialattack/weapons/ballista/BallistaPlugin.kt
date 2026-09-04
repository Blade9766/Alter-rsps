package org.alter.plugins.content.combat.specialattack.weapons.ballista

import org.alter.api.cfg.Animation
import org.alter.api.ext.message
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo

/**
 * Light and heavy ballista - **Concentrated Shot**: 25% more accuracy and damage, and 2.4 seconds
 * added to the wait before the next attack.
 *
 * The delay is the interesting half and the reason the ballista's special is a trade rather than a
 * gift. It goes through [SpecialAttackEffects.slowNextAttack] because `Combat.postAttack` sets the
 * attack delay after the special has already run.
 */
class BallistaPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Concentrated Shot") {
            if (!RangedAmmo.hasAmmo(player)) {
                player.message("You have no ammunition left in your quiver.")
                return@registerByName
            }

            player.animate(Animation.BALLISTA_SPECIAL)
            val ammoDropAction = RangedAmmo.fire(player, target)

            RangedCombatStrategy.shoot(
                player = player,
                target = target,
                accuracyMultiplier = MULTIPLIER,
                damageMultiplier = MULTIPLIER,
                onHit = ammoDropAction,
            )

            SpecialAttackEffects.slowNextAttack(player, EXTRA_DELAY_TICKS)
        }
    }

    private companion object {
        const val MULTIPLIER = 1.25

        /** 2.4 seconds is four ticks. */
        const val EXTRA_DELAY_TICKS = 4
    }
}
