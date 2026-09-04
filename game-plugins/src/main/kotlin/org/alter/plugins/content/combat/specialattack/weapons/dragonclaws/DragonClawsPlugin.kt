package org.alter.plugins.content.combat.specialattack.weapons.dragonclaws

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealExactMeleeSpecialHit

/**
 * Dragon claws - **Slice and Dice**: four hits in quick succession.
 *
 * The claws are the one special whose damage is not "roll a hit, maybe twice". Four accuracy rolls
 * are made, and *the first one that succeeds* decides the whole sequence: it rolls a damage figure
 * from a band that gets lower the later it lands, and the remaining hits are that figure halved,
 * halved again, and repeated. Nothing after the first success is rolled or can miss - which is why
 * the claws are so much more consistent than four separate attacks would be.
 *
 * - roll 1 lands: `d = 50%..100%` of max, then `d/2`, `d/4`, `d/4`
 * - roll 1 misses, roll 2 lands: `0`, then `d = 37.5%..87.5%` of max, `d/2`, `d/2`
 * - rolls 1-2 miss, roll 3 lands: `0`, `0`, then `d = 25%..75%` of max, `d`
 * - rolls 1-3 miss, roll 4 lands: `0`, `0`, `0`, then `d = 25%..125%` of max
 * - all four miss: `0`, `0`, `1`, `1` - the claws' consolation prize, and the reason a "failed"
 *   spec still shows two splats
 *
 * The bands are the wiki's, expressed in eighths of the max hit to keep the integer arithmetic
 * exact.
 */
class DragonClawsPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Slice and Dice") {
            player.animate(Animation.DRAGON_CLAWS_SPECIAL)
            player.graphic(Graphic.DRAGON_CLAWS_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target)

            /** A roll in `[low, high]` eighths of the max hit. */
            fun band(
                lowEighths: Int,
                highEighths: Int,
            ): Int {
                val low = maxHit * lowEighths / 8
                val high = maxHit * highEighths / 8
                return low + world.random(0..(high - low).coerceAtLeast(0))
            }

            val landed = (1..4).firstOrNull { accuracy >= world.randomDouble() }

            val damages =
                when (landed) {
                    1 -> band(4, 8).let { d -> listOf(d, d / 2, d / 4, d - d / 2 - d / 4) }
                    2 -> band(3, 7).let { d -> listOf(0, d, d / 2, d - d / 2) }
                    3 -> band(2, 6).let { d -> listOf(0, 0, d, d) }
                    4 -> listOf(0, 0, 0, band(2, 10))
                    else -> listOf(0, 0, 1, 1)
                }

            damages.forEachIndexed { index, damage ->
                /*
                 * `landHit` is per-splat: a zero from a landed sequence still renders as a block,
                 * which is what the real claws show for the first hits of a late-landing roll.
                 */
                player.dealExactMeleeSpecialHit(
                    target = target,
                    damage = damage,
                    landHit = damage > 0,
                    delay = index / 2,
                )
            }
        }
    }
}
