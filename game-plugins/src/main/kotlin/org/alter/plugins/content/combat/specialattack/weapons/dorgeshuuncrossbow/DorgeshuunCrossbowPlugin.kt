package org.alter.plugins.content.combat.specialattack.weapons.dorgeshuuncrossbow

import org.alter.api.Skills
import org.alter.api.ProjectileType
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.message
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.createProjectile
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo

/**
 * Dorgeshuun crossbow - **Snipe**: the bone dagger's Backstab at range.
 *
 * Guaranteed against an unsuspecting target - one that has not yet been drawn into the fight, see
 * [SpecialAttackEffects.isUnsuspecting] - and the target's Defence drops by whatever it deals,
 * which is what makes it the standard opener on a Defence-heavy boss.
 */
class DorgeshuunCrossbowPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Snipe") {
            if (!RangedAmmo.hasAmmo(player)) {
                player.message("You have no ammunition left in your quiver.")
                return@registerByName
            }

            player.animate(Animation.DORGESHUUN_CROSSBOW_SPECIAL)
            player.graphic(Graphic.DORGESHUUN_CROSSBOW_SPECIAL)
            val ammoDropAction = RangedAmmo.fire(player, target, spawnProjectile = false)
            world.spawn(player.createProjectile(target, Graphic.DORGESHUUN_SPECIAL_PROJECTILE, ProjectileType.BOLT))

            val victim = target
            RangedCombatStrategy.shoot(
                player = player,
                target = victim,
                forcedLandHit = if (SpecialAttackEffects.isUnsuspecting(victim)) true else null,
            ) {
                ammoDropAction()
                SpecialAttackEffects.drain(victim, Skills.DEFENCE, hit.hitmarks.sumOf { it.damage })
            }
        }
    }
}
