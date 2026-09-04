package org.alter.plugins.content.combat.specialattack.weapons.eclipseatlatl

import org.alter.api.cfg.Animation
import org.alter.api.ext.message
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.dealExactHit
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo

/**
 * Eclipse atlatl - **Eclipse**: 50% more accuracy, and every point of burn still owed by the target
 * cashed in at once.
 *
 * The burn is why [SpecialAttackEffects] tracks burn as a counter rather than as a set of queued
 * hits: this has to be able to take the remainder off the target before it lands.
 *
 * **Where this differs from the real thing.** The wiki folds the consumed burn into the max hit, so
 * it is rolled against rather than added on. There is no way to raise a max hit by a flat amount
 * through the ranged formula, so the burn lands as its own splat alongside the shot - the same
 * total damage, arriving as two numbers rather than one.
 *
 * The cache's full Eclipse set requirement is not enforced; there is no set effect here to require.
 */
class EclipseAtlatlPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Eclipse") {
            if (!RangedAmmo.hasAmmo(player)) {
                player.message("You have no darts left.")
                return@registerByName
            }

            player.animate(Animation.HUMAN_THROWN_ATTACK)
            val ammoDropAction = RangedAmmo.fire(player, target)

            val victim = target
            val consumed = SpecialAttackEffects.consumeBurn(victim)

            RangedCombatStrategy.shoot(
                player = player,
                target = victim,
                accuracyMultiplier = ACCURACY_MULTIPLIER,
            ) {
                ammoDropAction()
                if (consumed > 0 && hit.hitmarks.sumOf { it.damage } > 0) {
                    player.dealExactHit(target = victim, damage = consumed, landHit = true, delay = 0)
                }
            }
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 1.5
    }
}
