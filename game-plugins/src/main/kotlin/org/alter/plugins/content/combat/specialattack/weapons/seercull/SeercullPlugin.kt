package org.alter.plugins.content.combat.specialattack.weapons.seercull

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
 * Seercull - **Soulshot**: a shot that cannot miss, and takes the target's Magic level down by the
 * damage it deals.
 *
 * The whole bar for one guaranteed hit. Draining Magic is what the bow is actually for - it is the
 * classic way to strip a Dagannoth Prime or an Aviansie of its magic attack.
 */
class SeercullPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Soulshot") {
            if (!RangedAmmo.hasAmmo(player)) {
                player.message("You have no ammunition left in your quiver.")
                return@registerByName
            }

            player.animate(Animation.HUMAN_BOW_ATTACK)
            player.graphic(Graphic.SEERCULL_SPECIAL_DRAWBACK, 96)
            val ammoDropAction = RangedAmmo.fire(player, target, spawnProjectile = false)
            world.spawn(player.createProjectile(target, Graphic.SEERCULL_SPECIAL_PROJECTILE, ProjectileType.ARROW))

            val victim = target
            RangedCombatStrategy.shoot(
                player = player,
                target = victim,
                forcedLandHit = true,
            ) {
                ammoDropAction()
                victim.graphic(Graphic.SEERCULL_SPECIAL_HIT, 96)
                SpecialAttackEffects.drain(victim, Skills.MAGIC, hit.hitmarks.sumOf { it.damage })
            }
        }
    }
}
