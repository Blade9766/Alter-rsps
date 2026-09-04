package org.alter.plugins.content.combat.specialattack.weapons.accursedsceptre

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.ext.getMagicDamageBonus
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMagicSpecialHit

/**
 * Accursed sceptre - **Condemn**: 50% more accuracy and 50% more damage, and the target's Defence
 * and Magic each cut by 15%.
 *
 * The sceptre is a powered staff, so its damage comes off the Magic level directly -
 * `Magic / 3 - 6` - rather than out of a spell, and Condemn multiplies that.
 *
 * The drain is capped at 15% *total*, not 15% per cast: a second Condemn on an already-drained
 * target does nothing more, which is why this measures against the base level rather than simply
 * taking another 15% off whatever is left.
 *
 * The sceptre's revenant ether cost is not modelled; there is no ether system here to spend.
 */
class AccursedSceptrePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Condemn") {
            player.animate(Animation.NIGHTMARE_STAFF_SPECIAL)

            val magic = player.getSkills().getCurrentLevel(Skills.MAGIC)
            val base = (magic / 3.0 - 6).coerceAtLeast(1.0)
            val maxHit = (base * DAMAGE_MULTIPLIER * (1.0 + player.getMagicDamageBonus() / 100.0)).toInt()

            val accuracy = MagicCombatFormula.getAccuracy(player, target, specialAttackMultiplier = ACCURACY_MULTIPLIER)
            val landHit = accuracy >= world.randomDouble()

            val victim = target
            player.dealMagicSpecialHit(
                target = victim,
                damage = if (landHit) world.random(maxHit) else 0,
                landHit = landHit,
            )

            if (landHit) {
                DRAINED.forEach { skill -> drainToFloor(victim, skill) }
            }
        }
    }

    /** Takes [skill] down to 85% of where it started, and no further on a repeat cast. */
    private fun drainToFloor(
        victim: org.alter.game.model.entity.Pawn,
        skill: Int,
    ) {
        val current = SpecialAttackEffects.currentLevel(victim, skill)
        val floor = (current * (1.0 - DRAIN)).toInt()
        SpecialAttackEffects.drain(victim, skill, current - floor)
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 1.5
        const val DAMAGE_MULTIPLIER = 1.5
        const val DRAIN = 0.15
        val DRAINED = listOf(Skills.DEFENCE, Skills.MAGIC)
    }
}
