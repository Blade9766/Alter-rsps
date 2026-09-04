package org.alter.plugins.content.combat.specialattack.weapons.eldermaul

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Elder maul - **Pulverize**: 25% more accuracy and 35% off the target's Defence.
 *
 * The dragon warhammer's bigger sibling, and like it the drain applies whether or not the hit does.
 *
 * Plays the maul's ordinary swing: this cache carries no separate elder maul special sequence, and
 * borrowing an unrelated weapon's special animation would look worse than the right weapon's normal
 * one.
 */
class ElderMaulPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Pulverize") {
            player.animate(Animation.HUMAN_ELDER_MAUL_SWING)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = ACCURACY_MULTIPLIER)

            SpecialAttackEffects.drainPercent(target, Skills.DEFENCE, DEFENCE_DRAIN)
            player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = accuracy >= world.randomDouble())
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 1.25
        const val DEFENCE_DRAIN = 0.35
    }
}
