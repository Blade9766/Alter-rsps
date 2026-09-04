package org.alter.plugins.content.combat.specialattack.weapons.godsword

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.freeze
import org.alter.api.ext.heal
import org.alter.api.ext.secondsToTicks
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * The Bandos, Saradomin and Zamorak godswords.
 *
 * All three are the same shape - double accuracy plus a damage bonus - and differ only in what they
 * do afterwards, so they share a file. The Armadyl godsword's **The Judgement** lives with the rest
 * of the Armadyl weapons.
 *
 * - **Warstrike** (Bandos): 21% more damage, and one of the target's combat stats drained by the
 *   damage dealt. The real weapon picks the first stat with anything left to take in the order
 *   Defence, Strength, Prayer, Attack, Magic, Ranged; Prayer is skipped here because npcs have no
 *   Prayer level for it to come off.
 * - **Healing Blade** (Saradomin): 10% more damage, healing the wielder for half the damage dealt
 *   and restoring a quarter of it as Prayer points.
 * - **Ice Cleave** (Zamorak): 10% more damage and, on a hit, a twenty second freeze.
 *
 * All three drain and heal from the damage that was actually dealt, which is why the effects hang
 * off the hit rather than running before it.
 */
class GodswordPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Warstrike") {
            player.animate(Animation.BANDOS_GODSWORD_SPECIAL)
            player.graphic(Graphic.BANDOS_GODSWORD_SPECIAL)

            val victim = target
            val hit = strike(BANDOS_DAMAGE_MULTIPLIER)
            hit.hit.addAction {
                val damage = hit.hit.hitmarks.sumOf { it.damage }
                if (damage > 0) {
                    val drained = DRAIN_ORDER.firstOrNull { SpecialAttackEffects.currentLevel(victim, it) > 0 }
                    if (drained != null) {
                        SpecialAttackEffects.drain(victim, drained, damage)
                    }
                }
            }
        }

        SpecialAttacks.registerByName("Healing Blade") {
            player.animate(Animation.SARADOMIN_GODSWORD_SPECIAL)
            player.graphic(Graphic.SARADOMIN_GODSWORD_SPECIAL)

            val hit = strike(SARADOMIN_DAMAGE_MULTIPLIER)
            hit.hit.addAction {
                val damage = hit.hit.hitmarks.sumOf { it.damage }
                if (damage > 0) {
                    player.heal(damage * SARADOMIN_HEAL_NUMERATOR / SARADOMIN_HEAL_DENOMINATOR)
                    player.getSkills().alterCurrentLevel(
                        skill = Skills.PRAYER,
                        value = damage * SARADOMIN_PRAYER_NUMERATOR / SARADOMIN_PRAYER_DENOMINATOR,
                    )
                }
            }
        }

        SpecialAttacks.registerByName("Ice Cleave") {
            player.animate(Animation.ZAMORAK_GODSWORD_SPECIAL)
            player.graphic(Graphic.ZAMORAK_GODSWORD_SPECIAL)

            val victim = target
            val hit = strike(ZAMORAK_DAMAGE_MULTIPLIER)
            hit.hit.addAction {
                if (hit.landed) {
                    victim.freeze(ZAMORAK_FREEZE_SECONDS.secondsToTicks())
                }
            }
        }
    }

    /** The half every godsword special shares: double accuracy, and its own damage multiplier. */
    private fun org.alter.plugins.content.combat.specialattack.CombatContext.strike(damageMultiplier: Double) =
        player.dealMeleeSpecialHit(
            target = target,
            maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = damageMultiplier),
            landHit = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = ACCURACY_MULTIPLIER) >= world.randomDouble(),
        )

    private companion object {
        const val ACCURACY_MULTIPLIER = 2.0

        const val BANDOS_DAMAGE_MULTIPLIER = 1.21
        val DRAIN_ORDER = listOf(Skills.DEFENCE, Skills.STRENGTH, Skills.ATTACK, Skills.MAGIC, Skills.RANGED)

        const val SARADOMIN_DAMAGE_MULTIPLIER = 1.1
        const val SARADOMIN_HEAL_NUMERATOR = 1
        const val SARADOMIN_HEAL_DENOMINATOR = 2
        const val SARADOMIN_PRAYER_NUMERATOR = 1
        const val SARADOMIN_PRAYER_DENOMINATOR = 4

        const val ZAMORAK_DAMAGE_MULTIPLIER = 1.1
        const val ZAMORAK_FREEZE_SECONDS = 20
    }
}
