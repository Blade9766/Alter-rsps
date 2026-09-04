package org.alter.plugins.content.combat.specialattack.weapons.dragonthrownaxe

import org.alter.api.ProjectileType
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.AreaSound
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.createProjectile
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo

/**
 * Dragon thrownaxe - **Momentum Throw** (25%).
 *
 * A throw with 25% increased accuracy that also guarantees the next attack lands on the
 * following game tick rather than after the weapon's usual speed.
 *
 * The follow-up is granted through [Combat.INSTANT_NEXT_ATTACK] rather than by setting
 * the attack delay here, because `Combat.postAttack` runs after a special resolves and
 * would overwrite anything this set.
 */
class DragonThrownaxePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        // Both thrownaxe ids - the ordinary one and the untradeable 21207.
        SpecialAttacks.registerByName("Momentum Throw") {
            player.animate(Animation.DRAGON_THROWNAXE_SPECIAL)
            player.graphic(id = Graphic.DRAGON_THROWNAXE_SPECIAL_DRAWBACK, height = 96)
            world.spawn(AreaSound(tile = player.tile, id = THROWN_SOUND, radius = 10, volume = 1))

            val ammoDropAction = RangedAmmo.fire(player, target, spawnProjectile = false)
            world.spawn(player.createProjectile(target, Graphic.DRAGON_THROWNAXE_SPECIAL_PROJECTILE, ProjectileType.THROWN))

            RangedCombatStrategy.shoot(
                player = player,
                target = target,
                accuracyMultiplier = ACCURACY_MULTIPLIER,
                onHit = ammoDropAction,
            )

            player.attr[Combat.INSTANT_NEXT_ATTACK] = true
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 1.25

        /** Reuses the ordinary thrown-weapon effect - see CombatConfigs.getWeaponAttackSound. */
        const val THROWN_SOUND = 2696
    }
}
