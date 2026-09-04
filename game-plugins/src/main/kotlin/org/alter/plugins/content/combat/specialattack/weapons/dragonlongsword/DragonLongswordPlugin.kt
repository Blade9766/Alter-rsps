package org.alter.plugins.content.combat.specialattack.weapons.dragonlongsword

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
 * Dragon longsword - **Cleave**.
 *
 * The simplest special in the game: one swing at 25% more damage and ordinary accuracy.
 *
 * Registered by name, so it covers the plain longsword, the `(cr)` ornament and the Bounty Hunter
 * `(bh)` longsword - which is priced at 15% against the others' 25% and whose extra accuracy and
 * speed are deliberately not modelled, since neither attack speed nor a per-item accuracy override
 * has anywhere to live in `SpecialAttacks` yet.
 */
class DragonLongswordPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Cleave") {
            player.animate(Animation.DRAGON_LONGSWORD_SPECIAL)
            player.graphic(Graphic.DRAGON_LONGSWORD_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = DAMAGE_MULTIPLIER)
            val landHit = MeleeCombatFormula.getAccuracy(player, target) >= world.randomDouble()
            player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = landHit)
        }
    }

    private companion object {
        const val DAMAGE_MULTIPLIER = 1.25
    }
}
