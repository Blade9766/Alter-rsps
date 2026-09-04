package org.alter.plugins.content.combat.specialattack.weapons.zarytecrossbow

import org.alter.api.cfg.Animation
import org.alter.api.ext.message
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo

/**
 * Zaryte crossbow - **Evoke**: double accuracy, and a guaranteed enchanted bolt proc on a hit.
 *
 * The bolt effect is the point of it - a guaranteed diamond or ruby bolt proc is worth far more
 * than the accuracy. `RangedCombatStrategy.shoot` already rolls the bolt through a chance
 * multiplier, so a large enough multiplier makes the roll a certainty without a second code path.
 */
class ZaryteCrossbowPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Evoke") {
            if (!RangedAmmo.hasAmmo(player)) {
                player.message("You have no ammunition left in your quiver.")
                return@registerByName
            }

            player.animate(Animation.HUMAN_CROSSBOW_ATTACK)
            val ammoDropAction = RangedAmmo.fire(player, target)

            RangedCombatStrategy.shoot(
                player = player,
                target = target,
                accuracyMultiplier = ACCURACY_MULTIPLIER,
                boltChanceMultiplier = GUARANTEED_BOLT,
                onHit = ammoDropAction,
            )
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 2.0

        /** Large enough that the bolt's activation roll always succeeds. */
        const val GUARANTEED_BOLT = 100.0
    }
}
