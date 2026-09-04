package org.alter.plugins.content.combat.specialattack.weapons.dragonspear

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.secondsToTicks
import org.alter.api.ext.stun
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

/**
 * Dragon spear, Zamorakian spear and Zamorakian hasta - **Shove**: a three second stun and no
 * damage at all.
 *
 * The only special in the game that deals nothing, so there is no accuracy roll and no hit: it
 * simply lands. The push-back half is not modelled - shoving a target one tile needs a forced
 * movement the target does not control, which this codebase only has for agility obstacles - so
 * this is the stun alone.
 */
class DragonSpearPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Shove") {
            player.animate(Animation.SHOVE_SPEAR_SPECIAL)
            player.graphic(Graphic.DRAGON_SPEAR_SPECIAL)
            target.stun(STUN_SECONDS.secondsToTicks())
        }
    }

    private companion object {
        const val STUN_SECONDS = 3
    }
}
