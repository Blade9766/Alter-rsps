package org.alter.plugins.content.combat.specialattack.weapons.crossbow

import org.alter.api.ProjectileType
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.AreaSound
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.createProjectile
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo

/**
 * Armadyl crossbow - **Armadyl Eye** (50%).
 *
 * One shot at double accuracy that also doubles the base activation chance of whatever
 * enchanted bolt is loaded - the two halves of the special work together, so it is worth
 * saving for ruby or diamond bolts.
 */
class ArmadylCrossbowPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.register("item.armadyl_crossbow", SPECIAL_REQUIREMENT) {
            player.animate(CROSSBOW_ATTACK_ANIMATION)
            world.spawn(AreaSound(tile = player.tile, id = CROSSBOW_SOUND, radius = 10, volume = 1))

            /*
             * The special has its own bolt projectile, so the loaded ammo is consumed
             * without drawing its usual one.
             */
            val ammoDropAction = RangedAmmo.fire(player, target, spawnProjectile = false)
            world.spawn(player.createProjectile(target, Graphic.ARMADYL_CROSSBOW_SPECIAL_PROJECTILE, ProjectileType.BOLT))

            RangedCombatStrategy.shoot(
                player = player,
                target = target,
                accuracyMultiplier = 2.0,
                boltChanceMultiplier = 2.0,
                onHit = ammoDropAction,
            )
        }
    }

    private companion object {
        const val SPECIAL_REQUIREMENT = 50

        /** Matches CombatConfigs.getAttackAnimation's crossbow case. */
        const val CROSSBOW_ATTACK_ANIMATION = 4230

        /** Reuses the ordinary crossbow-fire effect - see CombatConfigs.getWeaponAttackSound. */
        const val CROSSBOW_SOUND = 2695
    }
}
