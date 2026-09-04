package org.alter.plugins.content.combat.specialattack.weapons.morrigans

import org.alter.api.cfg.Animation
import org.alter.api.ext.message
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo

/**
 * Morrigan's throwing axe and javelin, the two Wilderness thrown specials. Both are written for
 * player targets and say so:
 *
 * - **Hamstring** (axe): a minimum of 20% extra damage, and a player target left unable to run.
 * - **Phantom Strike** (javelin): "does nothing vs non-player targets" - against another player it
 *   deals its damage a second time, bled out afterwards. Against an npc it is an ordinary throw,
 *   which is exactly what the cache promises.
 *
 * The axe's "run energy drained six times faster for a minute" is a rate change with nowhere to
 * live in this codebase, so it lands as the run-energy wipe [SpecialAttackEffects.stopRunning]
 * gives - the same practical outcome a few seconds later.
 */
class MorrigansPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Hamstring") {
            player.animate(Animation.RUNE_THROWNAXE_SPECIAL)
            val ammoDropAction = RangedAmmo.fire(player, target)

            val victim = target
            RangedCombatStrategy.shoot(
                player = player,
                target = victim,
                damageMultiplier = DAMAGE_MULTIPLIER,
            ) {
                ammoDropAction()
                if (hit.hitmarks.sumOf { it.damage } > 0 && victim is Player) {
                    SpecialAttackEffects.stopRunning(victim)
                    victim.message("Your legs have been hamstrung!")
                }
            }
        }

        SpecialAttacks.registerByName("Phantom Strike") {
            player.animate(Animation.RUNE_THROWNAXE_SPECIAL)
            val ammoDropAction = RangedAmmo.fire(player, target)

            val victim = target
            RangedCombatStrategy.shoot(
                player = player,
                target = victim,
            ) {
                ammoDropAction()
                val damage = hit.hitmarks.sumOf { it.damage }
                if (damage > 0 && victim is Player) {
                    SpecialAttackEffects.damageOverTime(player, victim, total = damage, ticks = BLEED_TICKS)
                }
            }
        }
    }

    private companion object {
        /** A floor 20% up the max hit, which the formula expresses as a raised max. */
        const val DAMAGE_MULTIPLIER = 1.2
        const val BLEED_TICKS = 8
    }
}
