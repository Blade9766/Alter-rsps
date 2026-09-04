package org.alter.plugins.content.combat.specialattack.weapons.crystaldagger

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.secondsToTicks
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.timer.PROTECTION_PRAYER_BLOCK_TIMER
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Crystal dagger (perfected) - **Crystalline Severance**: pierces the target's prayers and keeps
 * them from working for a few seconds afterwards.
 *
 * The Gauntlet's answer to the dragon scimitar's Sever, and built on the same
 * [PROTECTION_PRAYER_BLOCK_TIMER]. It also pierces the prayer on the hit that applies it, which
 * Sever does not - the cache says "pierces", not "prevents".
 *
 * Not gated to the Crystalline Hunllef: there is no Gauntlet in this codebase to gate on.
 */
class CrystalDaggerPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Crystalline Severance") {
            player.animate(Animation.DRAGON_DAGGER_SPECIAL)
            player.graphic(Graphic.DRAGON_DAGGER_SPECIAL, 92)

            val victim = target
            val maxHit = MeleeCombatFormula.getMaxHitPiercingPrayer(player, victim)
            val landHit = MeleeCombatFormula.getAccuracy(player, victim) >= world.randomDouble()

            player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = landHit).hit.addAction {
                if (landHit) {
                    victim.timers[PROTECTION_PRAYER_BLOCK_TIMER] = BLOCK_SECONDS.secondsToTicks()
                }
            }
        }
    }

    private companion object {
        const val BLOCK_SECONDS = 5
    }
}
