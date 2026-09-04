package org.alter.plugins.content.combat.specialattack.weapons.barrelchestanchor

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Barrelchest anchor - **Sunder**: double accuracy, 10% higher max hit, and 10% of the damage dealt
 * taken off one of the target's combat stats.
 *
 * The drained stat is picked at random from Attack, Defence, Ranged and Magic, exactly as the wiki
 * describes it - unlike the Bandos godsword, which works down a fixed order.
 */
class BarrelchestAnchorPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Sunder") {
            player.animate(Animation.BARRELCHEST_ANCHOR_SPECIAL)
            player.graphic(Graphic.BARRELCHEST_ANCHOR_SPECIAL)

            val victim = target
            val maxHit = MeleeCombatFormula.getMaxHit(player, victim, specialAttackMultiplier = DAMAGE_MULTIPLIER)
            val accuracy = MeleeCombatFormula.getAccuracy(player, victim, specialAttackMultiplier = ACCURACY_MULTIPLIER)

            val hit = player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = accuracy >= world.randomDouble())
            hit.hit.addAction {
                val damage = hit.hit.hitmarks.sumOf { it.damage }
                if (damage > 0) {
                    SpecialAttackEffects.drain(victim, DRAINABLE.random(), (damage * DRAIN_FRACTION).toInt())
                }
            }
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 2.0
        const val DAMAGE_MULTIPLIER = 1.1
        const val DRAIN_FRACTION = 0.10
        val DRAINABLE = listOf(Skills.ATTACK, Skills.DEFENCE, Skills.RANGED, Skills.MAGIC)
    }
}
