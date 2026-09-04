package org.alter.plugins.content.combat.specialattack.weapons.candledagger

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Dragon candle dagger - **Celebrate**: "Perform an attack to leave the area feeling more festive."
 *
 * The joke special, and the cache means it literally: it is an ordinary dragon dagger stab with no
 * bonus of any kind, and the only thing the 25% bar buys is the fireworks.
 */
class DragonCandleDaggerPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Celebrate") {
            player.animate(Animation.DRAGON_DAGGER_SPECIAL)
            player.graphic(Graphic.DRAGON_DAGGER_SPECIAL, 92)
            player.forceChat(CHEER)

            repeat(2) {
                val maxHit = MeleeCombatFormula.getMaxHit(player, target)
                val landHit = MeleeCombatFormula.getAccuracy(player, target) >= world.randomDouble()
                player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = landHit)
            }
        }
    }

    private companion object {
        const val CHEER = "Happy holidays!"
    }
}
