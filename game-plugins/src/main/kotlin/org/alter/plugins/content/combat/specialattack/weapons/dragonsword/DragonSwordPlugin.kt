package org.alter.plugins.content.combat.specialattack.weapons.dragonsword

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
 * Dragon sword - **Wild Stab**: 25% more accuracy and damage, straight through Protect from Melee.
 *
 * The prayer-piercing half is [MeleeCombatFormula.getMaxHitPiercingPrayer]: the target keeps the
 * prayer and its icon, this attack simply skips the 40% reduction it would otherwise apply.
 *
 * Shares its animation and graphic with the dragon hasta, released alongside it in the same set of
 * Wilderness dragon weapons.
 */
class DragonSwordPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Wild Stab") {
            player.animate(Animation.DRAGON_HASTA_SPECIAL)
            player.graphic(Graphic.DRAGON_HASTA_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHitPiercingPrayer(player, target, specialAttackMultiplier = MULTIPLIER)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = MULTIPLIER)
            player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = accuracy >= world.randomDouble())
        }
    }

    private companion object {
        const val MULTIPLIER = 1.25
    }
}
