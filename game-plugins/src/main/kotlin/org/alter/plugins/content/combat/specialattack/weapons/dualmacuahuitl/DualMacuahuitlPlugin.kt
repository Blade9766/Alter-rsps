package org.alter.plugins.content.combat.specialattack.weapons.dualmacuahuitl

import org.alter.api.cfg.Animation
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Dual macuahuitl - **Blood infusion**: a quarter of the wielder's own hitpoints spent for 25% more
 * accuracy and damage.
 *
 * The self-damage is taken up front and always - it is the price of the special, not a consequence
 * of it landing - and is floored so it can never kill the wielder outright.
 *
 * The cache's "requires the full Bloodrager set" is not enforced, and the guaranteed set-effect
 * activation is not modelled: there is no Bloodrager set effect in this codebase for the special to
 * guarantee.
 */
class DualMacuahuitlPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Blood infusion") {
            player.animate(Animation.DRAGON_DAGGER_SPECIAL)

            val sacrifice = (player.getCurrentHp() * HP_COST).toInt().coerceAtMost(player.getCurrentHp() - 1)
            if (sacrifice > 0) {
                player.getSkills().setCurrentLevel(org.alter.api.Skills.HITPOINTS, player.getCurrentHp() - sacrifice)
            }

            repeat(2) {
                val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = MULTIPLIER)
                val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = MULTIPLIER)
                player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = accuracy >= world.randomDouble())
            }
        }
    }

    private companion object {
        const val HP_COST = 0.25
        const val MULTIPLIER = 1.25
    }
}
