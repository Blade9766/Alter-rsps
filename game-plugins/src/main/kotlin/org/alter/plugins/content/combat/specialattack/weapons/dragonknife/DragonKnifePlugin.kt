package org.alter.plugins.content.combat.specialattack.weapons.dragonknife

import org.alter.api.EquipmentType
import org.alter.api.ProjectileType
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.getEquipment
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.AreaSound
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.createProjectile
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo
import org.alter.rscm.RSCM.getRSCM

/**
 * Dragon knife - **Duality** (25%).
 *
 * Throws two knives at the target, each with its own accuracy and damage roll. Both
 * knives are consumed - the knife *is* the ammo, so [RangedAmmo.fire] draws them from
 * the weapon slot.
 */
class DragonKnifePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        // The plain knife, its three poisoned grades and the untradeable 27157.
        run {
            SpecialAttacks.registerByName("Duality") {
                val poisoned = player.getEquipment(EquipmentType.WEAPON)?.id != getRSCM("item.dragon_knife")
                player.animate(if (poisoned) Animation.POISONED_DRAGON_KNIFE_SPECIAL else Animation.DRAGON_KNIFE_SPECIAL)
                world.spawn(AreaSound(tile = player.tile, id = THROWN_SOUND, radius = 10, volume = 1))

                val baseDelay =
                    RangedCombatStrategy.getHitDelay(
                        player.getCentreTile(),
                        target.tile.transform(target.getSize() / 2, target.getSize() / 2),
                    )

                repeat(2) { throwIndex ->
                    if (!RangedAmmo.hasAmmo(player)) {
                        return@repeat
                    }
                    val ammoDropAction = RangedAmmo.fire(player, target, spawnProjectile = false)
                    world.spawn(player.createProjectile(target, Graphic.DRAGON_KNIFE_SPECIAL_PROJECTILE, ProjectileType.THROWN))

                    RangedCombatStrategy.shoot(
                        player = player,
                        target = target,
                        hitDelay = baseDelay + throwIndex,
                        onHit = ammoDropAction,
                    )
                }
            }
        }
    }

    private companion object {

        /** Reuses the ordinary thrown-weapon effect - see CombatConfigs.getWeaponAttackSound. */
        const val THROWN_SOUND = 2696
    }
}
