package org.alter.plugins.content.combat.specialattack.weapons.dragonhasta

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit
import org.alter.plugins.content.interfaces.attack.AttackTab

/**
 * Dragon hasta - **Unleash**: the only special in the game that scales with how much bar you spend,
 * and it spends all of it.
 *
 * Every 5% of the bar buys 5% accuracy and 2.5% strength, so a full bar is +100% accuracy and +50%
 * strength and an almost-empty one is barely worth pressing. [SpecialAttacks] charges a fixed cost
 * up front - 5%, the smallest step - and this drains whatever is left itself, the same way the rune
 * thrownaxe charges per bounce.
 */
class DragonHastaPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Unleash") {
            player.animate(Animation.DRAGON_HASTA_SPECIAL)
            player.graphic(Graphic.DRAGON_HASTA_SPECIAL)

            /*
             * The registered 5% has already been taken, so it counts toward the total along with
             * everything still on the bar.
             */
            val remaining = AttackTab.getEnergy(player)
            AttackTab.setEnergy(player, 0)
            val steps = (remaining + STEP) / STEP

            val accuracy = 1.0 + steps * ACCURACY_PER_STEP
            val strength = 1.0 + steps * STRENGTH_PER_STEP

            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = strength)
            val roll = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = accuracy)
            player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = roll >= world.randomDouble())
        }
    }

    private companion object {
        const val STEP = 5
        const val ACCURACY_PER_STEP = 0.05
        const val STRENGTH_PER_STEP = 0.025
    }
}
