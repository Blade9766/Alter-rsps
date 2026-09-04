package org.alter.plugins.content.combat.specialattack.weapons.dragonscimitar

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.message
import org.alter.api.ext.secondsToTicks
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.PROTECTION_PRAYER_BLOCK_TIMER
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Dragon scimitar - **Sever**: a slash with 25% more accuracy that, if it lands, stops the target
 * using protection prayers for five seconds.
 *
 * The block is [PROTECTION_PRAYER_BLOCK_TIMER], which every damage formula consults through
 * `Combat.protectionPrayersActive`. Deliberately *not* a prayer deactivation: in the real game the
 * target's overhead stays up and stays drawn for the whole five seconds, it simply stops working -
 * turning the prayer off instead would both look wrong and hand back the prayer points.
 *
 * Only applied on a successful hit, per the cache's own wording ("if it successfully hits").
 */
class DragonScimitarPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Sever") {
            player.animate(Animation.DRAGON_SCIMITAR_SPECIAL)
            player.graphic(Graphic.DRAGON_SCIMITAR_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = ACCURACY_MULTIPLIER)
            val landHit = accuracy >= world.randomDouble()

            val victim = target
            player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = landHit).hit.addAction {
                if (landHit) {
                    victim.timers[PROTECTION_PRAYER_BLOCK_TIMER] = BLOCK_SECONDS.secondsToTicks()
                    if (victim is Player) {
                        victim.message("Your protection prayers have been severed!")
                    }
                }
            }
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 1.25
        const val BLOCK_SECONDS = 5
    }
}
