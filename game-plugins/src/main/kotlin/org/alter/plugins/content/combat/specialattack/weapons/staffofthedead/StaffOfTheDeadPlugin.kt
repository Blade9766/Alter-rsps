package org.alter.plugins.content.combat.specialattack.weapons.staffofthedead

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.message
import org.alter.api.ext.secondsToTicks
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

/**
 * The staff of the dead family - **Power of Death**: half of all incoming melee damage, for a
 * minute.
 *
 * Attacks nobody, so it is `executeInstantly` and fires from the bar where the player stands. The
 * reduction is melee-only and stacks with Protect from Melee, which falls out of the design for
 * free: they are two separate multipliers on the same damage. See
 * `Combat.MELEE_DAMAGE_TAKE_MULTIPLIER` for why a melee-only key exists at all.
 *
 * The real effect ends early if the staff is unequipped. That is not modelled - it runs the full
 * minute regardless - because nothing else in this codebase watches an equipment change to cancel a
 * timed effect, and the honest fix is a general one rather than a special case here.
 */
class StaffOfTheDeadPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Power of Death", executeInstantly = true) {
            player.animate(Animation.STAFF_OF_THE_DEAD_SPECIAL)
            player.graphic(Graphic.STAFF_OF_THE_DEAD_SPECIAL)
            player.forceChat(INCANTATION)

            SpecialAttackEffects.setMeleeDamageTaken(
                target = player,
                multiplier = DAMAGE_TAKEN,
                ticks = DURATION_SECONDS.secondsToTicks(),
            )
            player.message("You are shrouded from melee attacks.")
        }
    }

    private companion object {
        const val DAMAGE_TAKEN = 0.5
        const val DURATION_SECONDS = 60
        const val INCANTATION = "Turmoil!"
    }
}
