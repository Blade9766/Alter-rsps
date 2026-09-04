package org.alter.plugins.content.items.skillcapes

import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.Plugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.items.consumables.Boost

/**
 * Skillcapes: the level 99 requirement to wear one, and the Boost option on the worn cape.
 *
 * Neither existed. The cache carries no requirement params on any skillcape, so every one of them
 * was wearable at level 1, and the `Boost` option each cape advertises was bound to nothing.
 *
 * The requirement is checked against the **base** level, not the current one, so a Zamorak brew
 * cannot get a player into a cape they have not earned - and, more to the point, so that a cape
 * whose own Boost has just raised the skill to 100 does not stop being wearable when it wears off.
 *
 * Hoods are not gated. A hood carries no requirement in the cache and none on the wiki; it comes
 * with the cape rather than being earned separately.
 */
class SkillCapePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SkillCape.values.forEach { cape ->
            cape.capes.forEach { item ->
                canEquipItem(item) {
                    val level = player.getSkills().getBaseLevel(cape.skill)
                    if (level >= SkillCape.REQUIRED_LEVEL) {
                        true
                    } else {
                        player.message(
                            "You need level ${SkillCape.REQUIRED_LEVEL} ${skillName(cape.skill)} to wear that.",
                        )
                        false
                    }
                }
            }

            if (!cape.hasBoost) {
                return@forEach
            }

            cape.capes.forEach { item ->
                onEquipmentOption(item, option = "Boost") {
                    boost(cape)
                }
            }
        }
    }

    /**
     * Raises the cape's skill by one, capped at one above the base level.
     *
     * [Boost] already caps at `base + boost` and does nothing once the stat is there, which is
     * exactly the behaviour wanted: clicking Boost again while it is still up tops nothing further
     * up rather than stacking.
     */
    private fun Plugin.boost(cape: SkillCape) {
        val skills = player.getSkills()
        val before = skills.getCurrentLevel(cape.skill)
        Boost(cape.skill, SkillCape.BOOST_LEVELS, 0).apply(player)
        val after = skills.getCurrentLevel(cape.skill)

        if (after > before) {
            player.message("You feel a surge of ${skillName(cape.skill)} power.")
        } else {
            player.message("Your ${skillName(cape.skill)} level is already boosted.")
        }
    }

    private fun skillName(skill: Int): String = Skills.getSkillName(world, skill).lowercase()
}
