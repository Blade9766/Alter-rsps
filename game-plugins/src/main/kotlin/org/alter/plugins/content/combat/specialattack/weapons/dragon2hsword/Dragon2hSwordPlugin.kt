package org.alter.plugins.content.combat.specialattack.weapons.dragon2hsword

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit
import org.alter.plugins.content.combat.strategy.ranged.RangedAoe

/**
 * Dragon 2h sword - **Powerstab**: one sweep that hits everything standing around you.
 *
 * Every target within a tile of the player is rolled separately at ordinary accuracy and damage -
 * there is no bonus, the special is purely the extra bodies it reaches.
 *
 * Extra targets come from [RangedAoe], which is npc-only. That is not a ranged-versus-melee
 * oversight: this codebase has no engine-level multi-combat gating, so an area attack that also
 * caught players would let one player splash damage over bystanders anywhere in the world. The
 * fourteen-target cap is the real game's.
 */
class Dragon2hSwordPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Powerstab") {
            player.animate(Animation.DRAGON_2H_SWORD_SPECIAL)
            player.graphic(Graphic.DRAGON_2H_SWORD_SPECIAL)

            RangedAoe.targetsAround(player, target, radius = SWEEP_RADIUS, max = MAX_TARGETS).forEach { victim ->
                val maxHit = MeleeCombatFormula.getMaxHit(player, victim)
                val landHit = MeleeCombatFormula.getAccuracy(player, victim) >= world.randomDouble()
                player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = landHit)
            }
        }
    }

    private companion object {
        /** Adjacent only - the sweep reaches the ring of tiles around the swordsman. */
        const val SWEEP_RADIUS = 1
        const val MAX_TARGETS = 14
    }
}
