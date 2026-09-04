package org.alter.plugins.content.combat.specialattack.weapons.brinesabre

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Brine sabre - **Liquify**: double accuracy, and a quarter of the damage dealt paid back as
 * Attack, Strength and Defence levels.
 *
 * The cache's "can only be used underwater" is not enforced - there is no underwater state in this
 * codebase to test, so gating on it would mean gating on nothing.
 */
class BrineSabrePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Liquify") {
            player.animate(Animation.BRINE_SABRE_SPECIAL)
            player.graphic(Graphic.BRINE_SABRE_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = ACCURACY_MULTIPLIER)

            val hit = player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = accuracy >= world.randomDouble())
            hit.hit.addAction {
                val boost = (hit.hit.hitmarks.sumOf { it.damage } * BOOST_FRACTION).toInt()
                if (boost > 0) {
                    BOOSTED.forEach { skill ->
                        player.getSkills().alterCurrentLevel(skill = skill, value = boost, capValue = boost)
                    }
                }
            }
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 2.0
        const val BOOST_FRACTION = 0.25
        val BOOSTED = listOf(Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE)
    }
}
