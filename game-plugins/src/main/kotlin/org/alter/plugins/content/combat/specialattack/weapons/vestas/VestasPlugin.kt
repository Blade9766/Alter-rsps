package org.alter.plugins.content.combat.specialattack.weapons.vestas

import org.alter.api.cfg.Animation
import org.alter.api.ext.message
import org.alter.api.ext.secondsToTicks
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealExactMeleeSpecialHit
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit
import org.alter.plugins.content.combat.strategy.ranged.RangedAoe

/**
 * Vesta's longsword and spear, the two Wilderness set-piece specials.
 *
 * - **Feint** (longsword): the target's Defence is treated as though it were 75% lower for this one
 *   attack, and the damage has a floor 20% up the max hit. The Defence reduction is applied as a
 *   four-times accuracy multiplier rather than by draining the target, because the real effect
 *   lasts exactly one hit and leaves the target's levels untouched afterwards - a drain would not.
 * - **Spear Wall** (spear): everything around the wielder is hit for half damage, and the wielder
 *   takes no melee damage at all for the next five seconds.
 *
 * Spear Wall's immunity is a melee-only damage multiplier of zero - see
 * `Combat.MELEE_DAMAGE_TAKE_MULTIPLIER` for why melee-only matters. Its extra targets come from
 * [RangedAoe], npcs only, for the reason given there.
 */
class VestasPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Feint") {
            player.animate(Animation.DRAGON_LONGSWORD_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = FEINT_ACCURACY)
            val landHit = accuracy >= world.randomDouble()
            val damage = if (landHit) world.random((maxHit * FEINT_MINIMUM).toInt()..maxHit) else 0

            player.dealExactMeleeSpecialHit(target = target, damage = damage, landHit = landHit)
        }

        SpecialAttacks.registerByName("Spear Wall") {
            player.animate(Animation.HUMAN_SPECIAL_SPEAR_SWING)

            RangedAoe.targetsAround(player, target, radius = WALL_RADIUS, max = WALL_TARGETS).forEach { victim ->
                val maxHit = MeleeCombatFormula.getMaxHit(player, victim, specialAttackMultiplier = WALL_DAMAGE)
                val landHit = MeleeCombatFormula.getAccuracy(player, victim) >= world.randomDouble()
                player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = landHit)
            }

            SpecialAttackEffects.setMeleeDamageTaken(player, multiplier = 0.0, ticks = IMMUNITY_SECONDS.secondsToTicks())
            player.message("You brace behind a wall of spears.")
        }
    }

    private companion object {
        /** Treating the target's defence as 75% lower is the same roll as quadrupling the attack. */
        const val FEINT_ACCURACY = 4.0
        const val FEINT_MINIMUM = 0.2

        const val WALL_DAMAGE = 0.5
        const val WALL_RADIUS = 1
        const val WALL_TARGETS = 16
        const val IMMUNITY_SECONDS = 5
    }
}
