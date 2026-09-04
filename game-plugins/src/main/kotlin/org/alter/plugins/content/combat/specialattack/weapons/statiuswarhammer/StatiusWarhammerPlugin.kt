package org.alter.plugins.content.combat.specialattack.weapons.statiuswarhammer

import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.getEquipment
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackDefs
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealExactMeleeSpecialHit

/**
 * Statius's warhammer - **Smash**: a *minimum* of 25% extra damage, and the target's Defence cut.
 *
 * The other **Smash**. The dragon warhammer shares the name and does something different, so both
 * bindings filter on the description - see
 * [org.alter.plugins.content.combat.specialattack.weapons.dragonwarhammer.DragonWarhammerPlugin].
 *
 * "Minimum 25% extra" is a floor under the *rolled* damage rather than a multiplier on the max hit,
 * which is why this rolls its own damage: an ordinary roll can come out at 1, and this cannot.
 *
 * The Bounty Hunter hammer takes 75% of Defence where the ordinary one takes 30%, read off its own
 * cache description rather than kept in a second list of ids.
 */
class StatiusWarhammerPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Smash", matching = { "minimum of 25% extra damage" in it }) {
            player.animate(Animation.DRAGON_WARHAMMER_SPECIAL)
            player.graphic(Graphic.DRAGON_WARHAMMER_SPECIAL)

            val description = player.getEquipment(EquipmentType.WEAPON)?.id?.let { SpecialAttackDefs.description(it) }.orEmpty()
            val drain = if ("by 75%" in description) BOUNTY_HUNTER_DRAIN else DEFENCE_DRAIN

            val maxHit = MeleeCombatFormula.getMaxHit(player, target)
            val landHit = MeleeCombatFormula.getAccuracy(player, target) >= world.randomDouble()
            val damage = if (landHit) world.random((maxHit * MINIMUM_FRACTION).toInt()..maxHit) else 0

            SpecialAttackEffects.drainPercent(target, Skills.DEFENCE, drain)
            player.dealExactMeleeSpecialHit(target = target, damage = damage, landHit = landHit)
        }
    }

    private companion object {
        /** The roll starts a quarter of the way up the max hit rather than at zero. */
        const val MINIMUM_FRACTION = 0.25
        const val DEFENCE_DRAIN = 0.30
        const val BOUNTY_HUNTER_DRAIN = 0.75
    }
}
