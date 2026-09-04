package org.alter.plugins.content.combat.specialattack.weapons.ivandis

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.message
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Npc
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

/**
 * Rod of Ivandis, Ivandis flail and Blisterwood flail - **Retainer**: traps a weakened vampyre
 * juvinate rather than killing it.
 *
 * Deals no damage. The target has to be a juvenile or juvinate at half health or less; anything
 * else refuses and the bar is still spent, which is what the real rod does.
 *
 * Identified by name rather than by npc id because there is no vampyre content in this codebase yet
 * to hold a proper list - when Meiyerditch arrives this should become an npc id set. A trapped
 * vampyre is simply removed from the world; there is no Guthix balance / trapped-vampyre item chain
 * here for it to feed into.
 */
class IvandisFlailPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Retainer") {
            player.animate(Animation.IVANDIS_SPECIAL)
            player.graphic(Graphic.IVANDIS_SPECIAL_CAST)

            val victim = target
            if (victim !is Npc || TRAPPABLE.none { it in victim.name.lowercase() }) {
                player.message("That creature cannot be trapped.")
                return@registerByName
            }

            if (victim.getCurrentHp() * 2 > victim.getMaxHp()) {
                player.message("The creature is still too strong to be trapped.")
                return@registerByName
            }

            victim.graphic(Graphic.IVANDIS_SPECIAL_CAPTURE, 96)
            world.remove(victim)
            player.message("You trap the vampyre.")
        }
    }

    private companion object {
        val TRAPPABLE = listOf("juvinate", "juvenile")
    }
}
