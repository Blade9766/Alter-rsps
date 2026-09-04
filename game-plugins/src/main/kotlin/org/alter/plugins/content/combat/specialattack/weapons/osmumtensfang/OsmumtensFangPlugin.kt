package org.alter.plugins.content.combat.specialattack.weapons.osmumtensfang

import org.alter.api.cfg.Animation
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Osmumten's fang - **Eviscerate**: 50% more accuracy, rolling the fang's true maximum hit.
 *
 * "True maximum" is a contrast with the fang's *ordinary* attack, which rolls only 15-85% of the max
 * hit. That narrowed band is a property of the fang's normal swing and is not modelled in this
 * codebase - every weapon here rolls the full range - so on this server the special is the accuracy
 * bonus alone, and Eviscerate reads as slightly weaker relative to the fang's basic attack than it
 * should. Implementing the band belongs with the fang's ordinary attack, not here.
 */
class OsmumtensFangPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Eviscerate") {
            player.animate(Animation.DRAGON_DAGGER_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = ACCURACY_MULTIPLIER)
            player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = accuracy >= world.randomDouble())
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 1.5
    }
}
