package org.alter.plugins.content.combat.specialattack.weapons.noxioushalberd

import org.alter.api.cfg.Animation
import org.alter.api.ext.message
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.POISON_TICKS_LEFT_ATTR
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealExactMeleeSpecialHit
import org.alter.plugins.content.mechanics.poison.Poison

/**
 * Noxious halberd - **Virulence**: the wielder burns off their own poison and puts it into the hit.
 *
 * The bonus is the damage the poison still had left to deal, so this is only worth pressing while
 * actually poisoned - and it cures the poison in the process, which is half the appeal. Unpoisoned
 * it is an ordinary halberd swing.
 *
 * Poison strength is held as a tick count, so the outstanding damage is the per-tick figure
 * multiplied by the cycles still to run - the same arithmetic [Poison] uses to decide what each
 * cycle deals.
 */
class NoxiousHalberdPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Virulence") {
            player.animate(Animation.HALBERD_SPECIAL)

            val ticks = (player.attr[POISON_TICKS_LEFT_ATTR] ?: 0).coerceAtLeast(0)
            val metabolised = if (ticks > 0) Poison.getDamageForTicks(ticks) * (ticks / POISON_CYCLE) else 0
            if (ticks > 0) {
                Poison.cure(player, immunityCycles = 0)
                player.message("You metabolise the poison in your veins.")
            }

            val maxHit = MeleeCombatFormula.getMaxHit(player, target)
            val landHit = MeleeCombatFormula.getAccuracy(player, target) >= world.randomDouble()
            val damage = if (landHit) world.random(maxHit) + metabolised else 0

            player.dealExactMeleeSpecialHit(target = target, damage = damage, landHit = landHit)
        }
    }

    private companion object {
        /** Poison deals once every five ticks of the count it carries. */
        const val POISON_CYCLE = 5
    }
}
