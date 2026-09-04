package org.alter.plugins.content.combat.specialattack.weapons.voidwaker

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.dealExactHit
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

/**
 * Voidwaker - **Disrupt**: a bolt of lightning that cannot miss, for 50-150% of the wielder's max
 * *melee* hit, dealt as magic damage.
 *
 * There is no accuracy roll at all, which is the entire point of the weapon: the damage is rolled
 * straight out of the melee max hit and lands regardless of the target's Defence or protection
 * prayers. Because it counts as magic it pays Magic experience - 2 per damage, as the wiki has it -
 * rather than the melee experience [org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit]
 * would grant, so it deals its hit directly.
 */
class VoidwakerPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Disrupt") {
            player.animate(Animation.ARMADYL_GODSWORD_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHitPiercingPrayer(player, target)
            val damage = world.random((maxHit * MINIMUM_FRACTION).toInt()..(maxHit * MAXIMUM_FRACTION).toInt())

            player.dealExactHit(target = target, damage = damage, landHit = true, delay = 2)
            player.addXp(Skills.MAGIC, damage * MAGIC_XP_PER_DAMAGE)
        }
    }

    private companion object {
        const val MINIMUM_FRACTION = 0.5
        const val MAXIMUM_FRACTION = 1.5
        const val MAGIC_XP_PER_DAMAGE = 2.0
    }
}
