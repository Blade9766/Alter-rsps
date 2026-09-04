package org.alter.plugins.content.combat.specialattack.weapons.dogsword

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.freeze
import org.alter.api.ext.heal
import org.alter.api.ext.secondsToTicks
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * The dogsword - **Echo slash**: every godsword effect at once.
 *
 * So the Bandos drain, the Saradomin heal and prayer restore and the Zamorak freeze all fire off one
 * hit, on top of the double accuracy and 10% damage the godswords share. The numbers are each
 * godsword's own - see
 * [org.alter.plugins.content.combat.specialattack.weapons.godsword.GodswordPlugin], which this
 * deliberately mirrors rather than shares code with: the dogsword stacks the effects, it does not
 * choose between them.
 */
class DogswordPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Echo slash") {
            player.animate(Animation.ARMADYL_GODSWORD_SPECIAL)
            player.graphic(Graphic.ARMADYL_GODSWORD_SPECIAL)

            val victim = target
            val maxHit = MeleeCombatFormula.getMaxHit(player, victim, specialAttackMultiplier = DAMAGE_MULTIPLIER)
            val accuracy = MeleeCombatFormula.getAccuracy(player, victim, specialAttackMultiplier = ACCURACY_MULTIPLIER)
            val landHit = accuracy >= world.randomDouble()

            val hit = player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = landHit)
            hit.hit.addAction {
                val damage = hit.hit.hitmarks.sumOf { it.damage }
                if (damage <= 0) {
                    return@addAction
                }

                // Bandos: drain the first combat stat with anything left in it.
                DRAIN_ORDER.firstOrNull { SpecialAttackEffects.currentLevel(victim, it) > 0 }?.let { skill ->
                    SpecialAttackEffects.drain(victim, skill, damage)
                }

                // Saradomin: half the damage back as hitpoints, a quarter as prayer.
                player.heal(damage / 2)
                player.getSkills().alterCurrentLevel(skill = Skills.PRAYER, value = damage / 4)

                // Zamorak: the freeze.
                victim.freeze(FREEZE_SECONDS.secondsToTicks())
            }
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 2.0
        const val DAMAGE_MULTIPLIER = 1.1
        const val FREEZE_SECONDS = 20
        val DRAIN_ORDER = listOf(Skills.DEFENCE, Skills.STRENGTH, Skills.ATTACK, Skills.MAGIC, Skills.RANGED)
    }
}
