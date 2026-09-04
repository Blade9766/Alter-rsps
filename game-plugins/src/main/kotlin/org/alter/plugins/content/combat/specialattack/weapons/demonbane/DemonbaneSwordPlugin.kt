package org.alter.plugins.content.combat.specialattack.weapons.demonbane

import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.getEquipment
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttackDefs
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

/**
 * Darklight, Arclight and Emberlight - **Weaken**: drains the target's Attack, Strength and Defence
 * by 5%, several times over against a demon.
 *
 * No damage: the special is the drain, so there is no accuracy roll and it always lands.
 *
 * The demon multiplier is the one thing that differs between the three swords - Darklight and
 * Arclight are "twice as effective against demons", Emberlight three times - and rather than keep a
 * table of which item is which, the effect reads the wielded weapon's own description out of
 * [SpecialAttackDefs]. That is the same text the client shows the player, so the two can't drift.
 */
class DemonbaneSwordPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Weaken") {
            player.animate(Animation.DARKLIGHT_SPECIAL)
            player.graphic(Graphic.DARKLIGHT_SPECIAL)

            val weapon = player.getEquipment(EquipmentType.WEAPON)?.id ?: return@registerByName
            val description = SpecialAttackDefs.description(weapon).orEmpty()
            val multiplier =
                when {
                    !SpecialAttackEffects.isDemon(target) -> 1
                    "three times as effective" in description -> 3
                    else -> 2
                }

            val drain = BASE_DRAIN * multiplier
            DRAINED.forEach { skill -> SpecialAttackEffects.drainPercent(target, skill, drain) }
        }
    }

    private companion object {
        const val BASE_DRAIN = 0.05
        val DRAINED = listOf(Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE)
    }
}
