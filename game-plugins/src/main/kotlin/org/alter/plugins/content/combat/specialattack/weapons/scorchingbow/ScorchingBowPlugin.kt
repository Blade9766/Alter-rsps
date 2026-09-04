package org.alter.plugins.content.combat.specialattack.weapons.scorchingbow

import org.alter.api.cfg.Animation
import org.alter.api.ext.freeze
import org.alter.api.ext.secondsToTicks
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
 * Scorching bow - **Scorching shackles**: 30% more accuracy, and a demon target bound and burning
 * for twelve seconds.
 *
 * The shackles are demons-only, which the cache is explicit about; against anything else this is
 * just an accurate shot. Both halves need the shot to land.
 */
class ScorchingBowPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Scorching shackles") {
            if (!RangedAmmo.hasAmmo(player)) {
                player.message("You have no ammunition left in your quiver.")
                return@registerByName
            }

            player.animate(Animation.HUMAN_BOW_ATTACK)
            val ammoDropAction = RangedAmmo.fire(player, target)

            val victim = target
            RangedCombatStrategy.shoot(
                player = player,
                target = victim,
                accuracyMultiplier = ACCURACY_MULTIPLIER,
            ) {
                ammoDropAction()
                if (hit.hitmarks.sumOf { it.damage } > 0 && SpecialAttackEffects.isDemon(victim)) {
                    victim.freeze(SHACKLE_SECONDS.secondsToTicks())
                    SpecialAttackEffects.burn(player, victim)
                }
            }
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 1.3
        const val SHACKLE_SECONDS = 12
    }
}
