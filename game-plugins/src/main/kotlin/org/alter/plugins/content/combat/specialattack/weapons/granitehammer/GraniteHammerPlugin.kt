package org.alter.plugins.content.combat.specialattack.weapons.granitehammer

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
 * Granite hammer - **Hammer Blow**: 50% more accuracy and a guaranteed +5 damage.
 *
 * The +5 is added to the *rolled* damage, not to the max hit, which is why this rolls its own hit
 * and deals it exactly rather than handing a multiplier to the formula. It is added on a landed hit
 * only - a miss is still a miss, and stays a zero.
 */
class GraniteHammerPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Hammer Blow") {
            player.animate(Animation.DRAGON_WARHAMMER_SPECIAL)
            player.graphic(Graphic.GRANITE_HAMMER_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = ACCURACY_MULTIPLIER)
            val landHit = accuracy >= world.randomDouble()
            val damage = if (landHit) world.random(maxHit) + BONUS_DAMAGE else 0

            player.dealExactMeleeSpecialHit(target = target, damage = damage, landHit = landHit)
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 1.5
        const val BONUS_DAMAGE = 5
    }
}
