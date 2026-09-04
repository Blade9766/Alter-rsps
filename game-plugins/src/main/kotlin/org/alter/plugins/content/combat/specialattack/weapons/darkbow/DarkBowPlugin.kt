package org.alter.plugins.content.combat.specialattack.weapons.darkbow

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
import org.alter.plugins.content.combat.strategy.ranged.ammo.Arrows
import org.alter.rscm.RSCM.getRSCM

/**
 * Dark bow - **Descent of Darkness** / **Descent of Dragons** (55%).
 *
 * Fires two arrows. With dragon arrows each deals 1.5x damage with a floor of 8; with
 * any other arrow, 1.3x with a floor of 5. Each arrow is capped at 48 damage.
 *
 * The special replaces the arrow's own projectile with the dark bow's shadow (or
 * dragon) arrow, so ammo is consumed through [RangedAmmo.fire] with its projectile
 * suppressed and the special draws its own.
 */
class DarkBowPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        // Every dark bow: the four recoloured ones, the untradeable, the (bh), the deadman and
        // the corrupted - nine ids, where the hand-written list this replaces named five.
        run {
            SpecialAttacks.registerByName("Descent of Darkness") {
                val dragonArrows = player.getEquipment(EquipmentType.AMMO)?.id in DRAGON_ARROW_IDS

                val projectileGfx = if (dragonArrows) Graphic.DARK_BOW_DRAGON_PROJECTILE else Graphic.DARK_BOW_DARKNESS_PROJECTILE
                val impactGfx = if (dragonArrows) Graphic.DARK_BOW_DRAGON_HIT else Graphic.DARK_BOW_DARKNESS_HIT
                val damageMultiplier = if (dragonArrows) DRAGON_ARROW_MULTIPLIER else STANDARD_MULTIPLIER
                val minimumDamage = if (dragonArrows) DRAGON_ARROW_MINIMUM else STANDARD_MINIMUM

                player.animate(Animation.HUMAN_BOW_ATTACK)
                world.spawn(AreaSound(tile = player.tile, id = DARK_BOW_SOUND, radius = 10, volume = 1))

                val baseDelay =
                    RangedCombatStrategy.getHitDelay(
                        player.getCentreTile(),
                        target.tile.transform(target.getSize() / 2, target.getSize() / 2),
                    )

                repeat(2) { shot ->
                    if (!RangedAmmo.hasAmmo(player)) {
                        return@repeat
                    }
                    val ammoDropAction = RangedAmmo.fire(player, target, spawnProjectile = false)

                    val projectile = player.createProjectile(target, projectileGfx, ProjectileType.ARROW)
                    world.spawn(projectile)
                    target.graphic(impactGfx, 96, projectile.impactDelay + shot)

                    RangedCombatStrategy.shoot(
                        player = player,
                        target = target,
                        damageMultiplier = damageMultiplier,
                        minimumDamage = minimumDamage,
                        damageCap = DAMAGE_CAP,
                        hitDelay = baseDelay + shot,
                        onHit = ammoDropAction,
                    )
                }
            }
        }
    }

    private companion object {

        const val DRAGON_ARROW_MULTIPLIER = 1.5
        const val STANDARD_MULTIPLIER = 1.3
        const val DRAGON_ARROW_MINIMUM = 8
        const val STANDARD_MINIMUM = 5

        /** Each arrow of the special is capped at 48. */
        const val DAMAGE_CAP = 48

        /** Reuses the ordinary bow-fire effect - see CombatConfigs.getWeaponAttackSound. */
        const val DARK_BOW_SOUND = 2700

        val DRAGON_ARROW_IDS = Arrows.DRAGON_ARROWS.toSet() + getRSCM("item.dragon_arrow")
    }
}
