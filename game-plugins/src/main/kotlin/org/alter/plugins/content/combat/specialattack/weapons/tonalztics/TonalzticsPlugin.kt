package org.alter.plugins.content.combat.specialattack.weapons.tonalztics

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy

/**
 * Tonalztics of ralos - **Division**: two independent throws, each cutting the target's Defence by
 * an eighth of their *Magic* level.
 *
 * The Defence reduction keying off Magic rather than off the damage is the weapon's signature, and
 * because both hits resolve in order the first one's drain is already in force when the second
 * rolls - which is what the wiki describes against npcs.
 *
 * The glaives return to the wielder, so nothing is consumed and there is no ammo to fire.
 */
class TonalzticsPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Division") {
            player.animate(Animation.HUMAN_THROWN_ATTACK)

            val victim = target
            val baseDelay =
                RangedCombatStrategy.getHitDelay(
                    player.getCentreTile(),
                    victim.tile.transform(victim.getSize() / 2, victim.getSize() / 2),
                )

            repeat(THROWS) { throwIndex ->
                RangedCombatStrategy.shoot(
                    player = player,
                    target = victim,
                    hitDelay = baseDelay + throwIndex,
                ) {
                    if (hit.hitmarks.sumOf { it.damage } > 0) {
                        val magic = SpecialAttackEffects.currentLevel(victim, Skills.MAGIC)
                        SpecialAttackEffects.drain(victim, Skills.DEFENCE, magic / DEFENCE_DIVISOR)
                    }
                }
            }
        }
    }

    private companion object {
        const val THROWS = 2

        /** An eighth of the target's Magic level, per hit. */
        const val DEFENCE_DIVISOR = 8
    }
}
