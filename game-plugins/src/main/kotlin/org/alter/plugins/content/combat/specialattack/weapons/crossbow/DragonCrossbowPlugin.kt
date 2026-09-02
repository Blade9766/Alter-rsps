package org.alter.plugins.content.combat.specialattack.weapons.crossbow

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.combat.PawnHit
import org.alter.game.model.entity.AreaSound
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo
import org.alter.plugins.content.combat.strategy.ranged.RangedAoe

/**
 * Dragon crossbow - **Annihilate** (60%).
 *
 * Hits everything in the 3x3 around the target, up to nine enemies. The target the
 * player actually clicked takes 20% more damage; everything caught in the blast takes
 * 20% less.
 *
 * One bolt is consumed for the whole special, but a projectile is drawn to every target
 * so the spread is visible.
 */
class DragonCrossbowPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.register("item.dragon_crossbow", SPECIAL_REQUIREMENT) {
            player.animate(CROSSBOW_ATTACK_ANIMATION)
            world.spawn(AreaSound(tile = player.tile, id = CROSSBOW_SOUND, radius = 10, volume = 1))

            val targets = RangedAoe.targetsAround(player, target, radius = BLAST_RADIUS, max = MAX_TARGETS)

            /*
             * The bolt is spent once. Its projectile is drawn towards the primary
             * target; each additional target gets one of its own below.
             */
            val ammoDropAction = RangedAmmo.fire(player, target)

            val noDrop: (PawnHit).() -> Unit = {}

            targets.forEachIndexed { index, victim ->
                val primary = index == 0
                if (!primary) {
                    RangedAmmo.drawProjectile(player, victim)
                }
                RangedCombatStrategy.shoot(
                    player = player,
                    target = victim,
                    damageMultiplier = if (primary) PRIMARY_MULTIPLIER else SPLASH_MULTIPLIER,
                    // Only the primary target's hit drops the single spent bolt.
                    onHit = if (primary) ammoDropAction else noDrop,
                )
            }
        }
    }

    private companion object {
        const val SPECIAL_REQUIREMENT = 60

        /** 3x3 centred on the clicked target. */
        const val BLAST_RADIUS = 1
        const val MAX_TARGETS = 9

        const val PRIMARY_MULTIPLIER = 1.2
        const val SPLASH_MULTIPLIER = 0.8

        /** Matches CombatConfigs.getAttackAnimation's crossbow case. */
        const val CROSSBOW_ATTACK_ANIMATION = 4230
        const val CROSSBOW_SOUND = 2695
    }
}
