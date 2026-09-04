package org.alter.plugins.content.combat.specialattack.weapons.dragonhalberd

import org.alter.api.cfg.Animation
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit
import org.alter.plugins.content.combat.strategy.ranged.RangedAoe

/**
 * Dragon and crystal halberd - **Sweep**.
 *
 * The special reads two different ways depending on what it is pointed at, which is the whole point
 * of it:
 *
 * - **A large target** (anything bigger than one tile) is hit **twice** - the second swing at 25%
 *   reduced accuracy. This is why the halberd special is the standard opener on big bosses.
 * - **A small target** takes one hit, and so may everything adjacent to it.
 *
 * Either way the damage is raised by 10% of the max hit.
 *
 * The animation is the shared polearm special sequence; the direction-specific sweep graphics in
 * the cache (`DRAGON_HALBERD_SPECIAL_NORTH`/`SOUTH`/`EAST`/`WEST`) need the player's facing to pick
 * between them, so none is played rather than always drawing the wrong one.
 */
class DragonHalberdPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Sweep") {
            player.animate(Animation.HALBERD_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = DAMAGE_MULTIPLIER)

            if (target.getSize() > 1) {
                val first = MeleeCombatFormula.getAccuracy(player, target)
                player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = first >= world.randomDouble())

                val second = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = SECOND_HIT_ACCURACY)
                player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = second >= world.randomDouble(), delay = 1)
            } else {
                RangedAoe.targetsAround(player, target, radius = SWEEP_RADIUS, max = MAX_TARGETS).forEach { victim ->
                    val hitMax = MeleeCombatFormula.getMaxHit(player, victim, specialAttackMultiplier = DAMAGE_MULTIPLIER)
                    val landHit = MeleeCombatFormula.getAccuracy(player, victim) >= world.randomDouble()
                    player.dealMeleeSpecialHit(target = victim, maxHit = hitMax, landHit = landHit)
                }
            }
        }
    }

    private companion object {
        const val DAMAGE_MULTIPLIER = 1.1
        const val SECOND_HIT_ACCURACY = 0.75

        /** The halberd reaches two tiles, so its sweep does too. */
        const val SWEEP_RADIUS = 2
        const val MAX_TARGETS = 10
    }
}
