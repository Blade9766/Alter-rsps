package org.alter.plugins.content.combat.specialattack.weapons.runeclaws

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
 * Rune claws - **Impale**: one hit at 10% more Attack and Strength.
 *
 * The wiki's "slower speed" is the attack that follows, which this codebase has no per-attack speed
 * override for, so only the accuracy and damage halves are modelled.
 */
class RuneClawsPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Impale") {
            player.animate(Animation.RUNE_CLAWS_SPECIAL)
            player.graphic(Graphic.RUNE_CLAWS_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = MULTIPLIER)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = MULTIPLIER)
            player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = accuracy >= world.randomDouble())
        }
    }

    private companion object {
        const val MULTIPLIER = 1.1
    }
}
