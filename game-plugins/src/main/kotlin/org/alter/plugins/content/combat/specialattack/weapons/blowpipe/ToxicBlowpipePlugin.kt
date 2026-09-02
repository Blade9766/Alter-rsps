package org.alter.plugins.content.combat.specialattack.weapons.blowpipe

import org.alter.api.ProjectileType
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.heal
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.createProjectile
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo

/**
 * Toxic blowpipe - **Toxic Siphon** (50%).
 *
 * One shot at double accuracy and 1.5x damage that heals the attacker for half the
 * damage dealt, rounded down.
 *
 * Consumes a dart and rolls a Zulrah's scale like any other blowpipe shot, through
 * [RangedAmmo], which knows to take the dart out of the weapon rather than the quiver
 * (see [org.alter.plugins.content.items.blowpipe.Blowpipe]). The dart's own projectile
 * is suppressed because the special draws its own.
 *
 * The projectile and animation are the real ones - [Graphic.TOXIC_BLOWPIPE_SPECIAL_PROJECTILE]
 * is named for this exact special.
 */
class ToxicBlowpipePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        /*
         * Only the loaded forms get a special - an empty blowpipe has no darts to fire.
         * The blazing blowpipe is the Trailblazer Reloaded variant and behaves
         * identically.
         */
        for (blowpipe in listOf("item.toxic_blowpipe", "item.blazing_blowpipe")) {
            SpecialAttacks.register(blowpipe, SPECIAL_REQUIREMENT) {
                player.animate(Animation.HUMAN_BLOWPIPE_ATTACK)

                // Consumes a dart and rolls a scale exactly as an ordinary blowpipe
                // shot does; the special draws its own projectile, so the dart's is
                // suppressed.
                val ammoDropAction = RangedAmmo.fire(player, target, spawnProjectile = false)

                val projectile =
                    player.createProjectile(
                        target,
                        Graphic.TOXIC_BLOWPIPE_SPECIAL_PROJECTILE,
                        ProjectileType.THROWN,
                    )
                world.spawn(projectile)

                RangedCombatStrategy.shoot(
                    player = player,
                    target = target,
                    accuracyMultiplier = ACCURACY_MULTIPLIER,
                    damageMultiplier = DAMAGE_MULTIPLIER,
                    hitDelay =
                        RangedCombatStrategy.getHitDelay(
                            player.getCentreTile(),
                            target.tile.transform(target.getSize() / 2, target.getSize() / 2),
                        ),
                ) {
                    ammoDropAction()

                    /*
                     * Heals half the damage actually dealt, rounded down - so a miss
                     * (0 damage) heals nothing, and the heal is capped at the player's
                     * Hitpoints level by heal() itself.
                     */
                    val damage = hit.hitmarks.sumOf { it.damage }
                    if (damage > 0) {
                        player.heal(damage / 2)
                    }
                }
            }
        }
    }

    private companion object {
        const val SPECIAL_REQUIREMENT = 50

        /** Wiki: "increases accuracy by 100% and damage by 50%". */
        const val ACCURACY_MULTIPLIER = 2.0
        const val DAMAGE_MULTIPLIER = 1.5
    }
}
