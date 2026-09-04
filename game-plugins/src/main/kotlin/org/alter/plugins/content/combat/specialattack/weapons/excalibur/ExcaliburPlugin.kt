package org.alter.plugins.content.combat.specialattack.weapons.excalibur

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.ext.message
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

/**
 * Excalibur - **Sanctuary**: eight levels of Defence, for the whole bar.
 *
 * Attacks nobody, so it is registered `executeInstantly` - the spec bar fires it where the player
 * stands rather than waiting for a target. Boosted above the base level like any other Defence
 * boost, so a second cast while the first is still up does nothing.
 */
class ExcaliburPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Sanctuary", executeInstantly = true) {
            player.animate(Animation.EXCALIBUR_SPECIAL)
            player.graphic(Graphic.EXCALIBUR_SPECIAL)
            player.getSkills().alterCurrentLevel(skill = Skills.DEFENCE, value = BOOST, capValue = BOOST)
            player.message("You feel a surge of power.")
        }
    }

    private companion object {
        const val BOOST = 8
    }
}
