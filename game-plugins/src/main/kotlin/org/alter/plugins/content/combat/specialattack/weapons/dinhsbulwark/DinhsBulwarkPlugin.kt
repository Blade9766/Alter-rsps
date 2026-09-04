package org.alter.plugins.content.combat.specialattack.weapons.dinhsbulwark

import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit
import org.alter.plugins.content.combat.strategy.ranged.RangedAoe

/**
 * Dinh's bulwark - **Shield Bash**: everything in a 10x10 around the player, at 20% more accuracy.
 *
 * The bulwark has no special attack animation of its own in this cache, so the special plays only
 * its impact graphic over the wielder rather than borrowing another weapon's swing.
 */
class DinhsBulwarkPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Shield Bash") {
            player.graphic(Graphic.DINHS_BULWARK_SPECIAL)

            RangedAoe.targetsAround(player, target, radius = BASH_RADIUS, max = MAX_TARGETS).forEach { victim ->
                val maxHit = MeleeCombatFormula.getMaxHit(player, victim)
                val accuracy = MeleeCombatFormula.getAccuracy(player, victim, specialAttackMultiplier = ACCURACY_MULTIPLIER)
                player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = accuracy >= world.randomDouble())
            }
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 1.2

        /** A 10x10 area around the player is five tiles in every direction. */
        const val BASH_RADIUS = 5
        const val MAX_TARGETS = 10
    }
}
