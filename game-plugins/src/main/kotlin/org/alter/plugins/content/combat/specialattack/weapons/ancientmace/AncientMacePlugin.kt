package org.alter.plugins.content.combat.specialattack.weapons.ancientmace

import org.alter.api.Skills
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
 * Ancient mace - **Favour of the War God**: hits through Protect from Melee and siphons Prayer
 * points equal to the damage dealt.
 *
 * The Prayer gained can take the player above their Prayer level, which is the whole reason the
 * mace is carried, so the cap is the damage rather than zero.
 */
class AncientMacePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Favour of the War God") {
            player.animate(Animation.ANCIENT_MACE_SPECIAL)
            player.graphic(Graphic.ANCIENT_MACE_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHitPiercingPrayer(player, target)
            val landHit = MeleeCombatFormula.getAccuracy(player, target) >= world.randomDouble()

            val hit = player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = landHit)
            hit.hit.addAction {
                val damage = hit.hit.hitmarks.sumOf { it.damage }
                if (damage > 0) {
                    player.getSkills().alterCurrentLevel(skill = Skills.PRAYER, value = damage, capValue = damage)
                }
            }
        }
    }
}
