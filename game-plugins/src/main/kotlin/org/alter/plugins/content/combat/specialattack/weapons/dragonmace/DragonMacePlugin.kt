package org.alter.plugins.content.combat.specialattack.weapons.dragonmace

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
 * Dragon mace - **Shatter**: 50% more damage and 25% more accuracy on one hit.
 */
class DragonMacePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Shatter") {
            player.animate(Animation.DRAGON_MACE_SPECIAL)
            player.graphic(Graphic.DRAGON_MACE_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = DAMAGE_MULTIPLIER)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = ACCURACY_MULTIPLIER)
            player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = accuracy >= world.randomDouble())
        }
    }

    private companion object {
        const val DAMAGE_MULTIPLIER = 1.5
        const val ACCURACY_MULTIPLIER = 1.25
    }
}
