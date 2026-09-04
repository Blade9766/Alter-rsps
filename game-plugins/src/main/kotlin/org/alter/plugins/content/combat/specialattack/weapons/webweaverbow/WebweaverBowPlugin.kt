package org.alter.plugins.content.combat.specialattack.weapons.webweaverbow

import org.alter.api.cfg.Animation
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo
import org.alter.plugins.content.mechanics.poison.poison

/**
 * Webweaver bow - **Swarm**: four shots at double accuracy, each for up to 40% of the max hit, each
 * able to poison.
 *
 * Four separate shots rather than one split four ways, so each rolls its own accuracy and its own
 * ammo - a quiver with three arrows in it fires three times. The poison starts at four damage, per
 * the wiki.
 *
 * The bow's revenant ether charge is not modelled; there is no ether system here to spend.
 */
class WebweaverBowPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Swarm") {
            player.animate(Animation.HUMAN_BOW_ATTACK)

            val victim = target
            val baseDelay =
                RangedCombatStrategy.getHitDelay(
                    player.getCentreTile(),
                    victim.tile.transform(victim.getSize() / 2, victim.getSize() / 2),
                )

            repeat(SHOTS) { shot ->
                if (!RangedAmmo.hasAmmo(player)) {
                    return@repeat
                }
                val ammoDropAction = RangedAmmo.fire(player, victim)

                RangedCombatStrategy.shoot(
                    player = player,
                    target = victim,
                    accuracyMultiplier = ACCURACY_MULTIPLIER,
                    damageMultiplier = DAMAGE_MULTIPLIER,
                    hitDelay = baseDelay + shot / 2,
                ) {
                    ammoDropAction()
                    if (hit.hitmarks.sumOf { it.damage } > 0 && world.chance(POISON_CHANCE, 100)) {
                        victim.poison(POISON_DAMAGE) {}
                    }
                }
            }
        }
    }

    private companion object {
        const val SHOTS = 4
        const val ACCURACY_MULTIPLIER = 2.0

        /** Up to 40% of the max hit per shot, rounded up by the formula's own flooring. */
        const val DAMAGE_MULTIPLIER = 0.4

        const val POISON_CHANCE = 25
        const val POISON_DAMAGE = 4
    }
}
