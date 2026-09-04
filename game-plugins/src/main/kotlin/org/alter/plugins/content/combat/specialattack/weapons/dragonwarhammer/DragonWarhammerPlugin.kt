package org.alter.plugins.content.combat.specialattack.weapons.dragonwarhammer

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Dragon warhammer - **Smash**: 50% more damage and 30% off the target's Defence.
 *
 * The drain lands whether or not the hit does - the wiki is explicit that the warhammer's Defence
 * reduction applies on a miss too, which is exactly why it is the standard opener - and it comes
 * off the *current* Defence level, so successive specials each take less than the last.
 */
class DragonWarhammerPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        /*
         * Statius's warhammer's special is also called "Smash" and is a different attack - a
         * minimum 25% damage increase rather than a flat 50% multiplier - so this is the one place
         * a name alone is not specific enough. The descriptions do differ, so the filter picks the
         * dragon warhammer's out of the pair.
         */
        SpecialAttacks.registerByName("Smash", matching = { "50% more damage" in it }) {
            player.animate(Animation.DRAGON_WARHAMMER_SPECIAL)
            player.graphic(Graphic.DRAGON_WARHAMMER_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = DAMAGE_MULTIPLIER)
            val landHit = MeleeCombatFormula.getAccuracy(player, target) >= world.randomDouble()

            SpecialAttackEffects.drainPercent(target, Skills.DEFENCE, DEFENCE_DRAIN)
            player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = landHit)
        }
    }

    private companion object {
        const val DAMAGE_MULTIPLIER = 1.5
        const val DEFENCE_DRAIN = 0.30
    }
}
