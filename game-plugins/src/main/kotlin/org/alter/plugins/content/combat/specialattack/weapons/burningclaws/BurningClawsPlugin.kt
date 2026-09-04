package org.alter.plugins.content.combat.specialattack.weapons.burningclaws

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealExactMeleeSpecialHit

/**
 * Burning claws - **Burning barrage**: three hits, and a chance to set the target alight on each.
 *
 * Built the same way as the dragon claws: three accuracy rolls, and the first one to succeed
 * decides the whole sequence, choosing both how much damage there is to share out and how it is
 * split across the three splats. The later the roll that lands, the less there is and the more of
 * it lands on the last splat.
 *
 * - roll 1: 75-175% of max hit, split 25/25/50
 * - roll 2: 50-150%, split 0/50/50
 * - roll 3: 25-125%, all on the third splat
 * - all three miss: 0-2 damage and no burn
 *
 * The burn chance rises the later the sequence lands - 15%, 30%, 45% - which is the wiki's
 * compensation for the smaller damage band.
 */
class BurningClawsPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Burning barrage") {
            player.animate(Animation.DRAGON_CLAWS_SPECIAL)
            player.graphic(Graphic.DRAGON_CLAWS_SPECIAL)

            val victim = target
            val maxHit = MeleeCombatFormula.getMaxHit(player, victim)
            val accuracy = MeleeCombatFormula.getAccuracy(player, victim)
            val landed = (1..3).firstOrNull { accuracy >= world.randomDouble() }

            /** A roll in `[low, high]` percent of the max hit. */
            fun band(
                low: Int,
                high: Int,
            ): Int = world.random((maxHit * low / 100)..(maxHit * high / 100))

            val damages =
                when (landed) {
                    1 -> band(75, 175).let { total -> listOf(total / 4, total / 4, total - total / 4 - total / 4) }
                    2 -> band(50, 150).let { total -> listOf(0, total / 2, total - total / 2) }
                    3 -> listOf(0, 0, band(25, 125))
                    else -> listOf(0, 0, world.random(2))
                }

            val burnChance =
                when (landed) {
                    1 -> FIRST_ROLL_BURN
                    2 -> SECOND_ROLL_BURN
                    3 -> THIRD_ROLL_BURN
                    else -> 0
                }

            damages.forEachIndexed { index, damage ->
                player.dealExactMeleeSpecialHit(
                    target = victim,
                    damage = damage,
                    landHit = damage > 0,
                    delay = index / 2,
                )
                if (damage > 0 && world.chance(burnChance, 100)) {
                    SpecialAttackEffects.burn(player, victim)
                }
            }
        }
    }

    private companion object {
        const val FIRST_ROLL_BURN = 15
        const val SECOND_ROLL_BURN = 30
        const val THIRD_ROLL_BURN = 45
    }
}
