package org.alter.plugins.content.combat.specialattack.weapons.whip

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.freeze
import org.alter.api.ext.secondsToTicks
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * The whips - **Energy Drain** (abyssal whip) and **Binding Tentacle** (abyssal tentacle).
 *
 * Both are one ordinary-damage hit with an extra effect, and both are registered by name so every
 * recoloured, ornamented and volcanic/frozen variant is covered by the one binding.
 *
 * - **Energy Drain**: 25% more accuracy, and 10% of the target's run energy siphoned into the
 *   attacker's. Only meaningful against another player, since npcs have no run energy - the drain
 *   is skipped rather than faked for them, and the accuracy bonus still applies.
 * - **Binding Tentacle**: a five second bind. The poison half is left to the tentacle's ordinary
 *   poison chance rather than modelled separately here.
 */
class AbyssalWhipPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Energy Drain") {
            player.animate(Animation.HUMAN_WHIP_SWING)
            player.graphic(Graphic.WHIP_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = ACCURACY_MULTIPLIER)
            val landHit = accuracy >= world.randomDouble()

            val victim = target
            if (landHit && victim is Player) {
                val siphoned = victim.runEnergy * ENERGY_SIPHON
                victim.runEnergy -= siphoned
                player.runEnergy = (player.runEnergy + siphoned).coerceAtMost(MAX_RUN_ENERGY)
            }

            player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = landHit)
        }

        SpecialAttacks.registerByName("Binding Tentacle") {
            player.animate(Animation.HUMAN_WHIP_SWING)
            player.graphic(Graphic.WHIP_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target)
            val landHit = MeleeCombatFormula.getAccuracy(player, target) >= world.randomDouble()

            val victim = target
            player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = landHit).hit.addAction {
                if (landHit) {
                    victim.graphic(Graphic.BIND_HIT, 96)
                    victim.freeze(BIND_SECONDS.secondsToTicks())
                }
            }
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 1.25
        const val ENERGY_SIPHON = 0.10

        /** `Player.runEnergy` counts in hundredths of a percent, so a full bar is 10000. */
        const val MAX_RUN_ENERGY = 10000.0

        const val BIND_SECONDS = 5
    }
}
