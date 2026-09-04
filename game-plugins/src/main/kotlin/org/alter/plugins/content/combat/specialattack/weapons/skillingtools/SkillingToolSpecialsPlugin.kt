package org.alter.plugins.content.combat.specialattack.weapons.skillingtools

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

/**
 * The three skilling-tool specials: **Lumber Up** (axes), **Rock Knocker** (pickaxes) and
 * **Fishstabber** (harpoons). Each burns the whole bar for three levels of its skill.
 *
 * All `executeInstantly` - they target nobody, so the spec bar fires them on the spot. Boosted
 * above the base level, so holding one at +3 and pressing again does nothing.
 *
 * The `extraItems` lists are the ids that carry a cost in the cache but no description to bind them
 * by: the ornamented and uncharged infernal harpoons, pickaxes and axes, which the game gives a
 * special attack without ever describing it.
 */
class SkillingToolSpecialsPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Lumber Up", executeInstantly = true, extraItems = ORNAMENT_AXES) {
            player.animate(Animation.DRAGON_AXE_SPECIAL)
            player.graphic(Graphic.DRAGON_AXE_SPECIAL)
            boost(Skills.WOODCUTTING)
        }

        SpecialAttacks.registerByName("Rock Knocker", executeInstantly = true, extraItems = ORNAMENT_PICKAXES) {
            player.animate(Animation.DRAGON_PICKAXE_SPECIAL)
            boost(Skills.MINING)
        }

        SpecialAttacks.registerByName("Fishstabber", executeInstantly = true, extraItems = ORNAMENT_HARPOONS) {
            player.animate(Animation.DRAGON_HARPOON_SPECIAL)
            boost(Skills.FISHING)
        }
    }

    private fun org.alter.plugins.content.combat.specialattack.CombatContext.boost(skill: Int) {
        player.getSkills().alterCurrentLevel(skill = skill, value = BOOST, capValue = BOOST)
        player.forceChat(BATTLE_CRY)
    }

    private companion object {
        const val BOOST = 3
        const val BATTLE_CRY = "Smashing!"

        /** Infernal axe (or)/(uncharged), Dragon axe (or) - across both of the cache's id blocks. */
        val ORNAMENT_AXES = listOf(25066, 25371, 25378, 30347, 30348, 30352)

        /** Infernal pickaxe (or)/(uncharged), Dragon pickaxe (or). */
        val ORNAMENT_PICKAXES = listOf(25063, 25369, 25376, 30345, 30346, 30351)

        /** Infernal harpoon (or)/(uncharged), Dragon harpoon (or). */
        val ORNAMENT_HARPOONS = listOf(25059, 25367, 25373, 30342, 30343, 30349)
    }
}
