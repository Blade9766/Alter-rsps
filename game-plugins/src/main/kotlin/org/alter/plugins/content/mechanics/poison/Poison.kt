package org.alter.plugins.content.mechanics.poison

import org.alter.api.EquipmentType
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.setVarp
import org.alter.game.model.attr.POISON_TICKS_LEFT_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.POISON_TIMER

/**
 * @author Tom <rspsmods@gmail.com>
 */
object Poison {
    private const val HP_ORB_VARP = 102

    fun getDamageForTicks(ticks: Int) = (ticks / 5) + 1

    /**
     * Whether [pawn] cannot be poisoned right now.
     *
     * The antipoison branch is easy to miss: [cure] records immunity as a *negative*
     * [POISON_TICKS_LEFT_ATTR] that the poison timer counts back up toward zero, and
     * nothing used to read it here. An antipoison therefore cured the poison and then let
     * the very next poisoned hit re-apply it, because [poison] compares damage figures and
     * `getDamageForTicks` of a negative count is 0 - lower than any incoming poison.
     */
    fun isImmune(pawn: Pawn): Boolean {
        if ((pawn.attr[POISON_TICKS_LEFT_ATTR] ?: 0) < 0) {
            return true
        }
        return when (pawn) {
            is Player -> pawn.hasEquipped(EquipmentType.HEAD, "item.serpentine_helm", "item.tanzanite_helm", "item.magma_helm")
            is Npc -> pawn.combatDef.immunePoison
            else -> false
        }
    }

    fun poison(
        pawn: Pawn,
        initialDamage: Int,
    ): Boolean {
        val ticks = (initialDamage * 5) - 4
        val oldDamage = getDamageForTicks(pawn.attr[POISON_TICKS_LEFT_ATTR] ?: 0)
        val newDamage = getDamageForTicks(ticks)
        if (oldDamage > newDamage) {
            return false
        }
        pawn.timers[POISON_TIMER] = 1
        pawn.attr[POISON_TICKS_LEFT_ATTR] = ticks
        return true
    }

    /**
     * Poison ticks once per cycle, so both remaining poison and immunity are counted in cycles
     * rather than in game ticks.
     */
    const val TICKS_PER_CYCLE = 25

    private const val SECONDS_PER_CYCLE = TICKS_PER_CYCLE * 3 / 5

    fun cyclesForSeconds(seconds: Int): Int = seconds / SECONDS_PER_CYCLE

    /**
     * Clears any active poison and grants immunity for [immunityCycles] poison cycles.
     *
     * Immunity is held as a negative tick count on [POISON_TICKS_LEFT_ATTR], which the poison timer
     * counts back up toward zero.
     */
    fun cure(
        pawn: Pawn,
        immunityCycles: Int,
    ) {
        val existing = pawn.attr[POISON_TICKS_LEFT_ATTR] ?: 0
        // Never shorten an immunity that already runs longer than this one would.
        val immunity = Math.min(existing, -immunityCycles)
        pawn.attr[POISON_TICKS_LEFT_ATTR] = immunity
        pawn.timers[POISON_TIMER] = TICKS_PER_CYCLE
        if (pawn is Player) {
            setHpOrb(pawn, OrbState.NONE)
        }
    }

    fun setHpOrb(
        player: Player,
        state: OrbState,
    ) {
        val value =
            when (state) {
                OrbState.NONE -> 0
                OrbState.POISON -> 1
                OrbState.VENOM -> 1_000_000
            }
        player.setVarp(HP_ORB_VARP, value)
    }

    enum class OrbState {
        NONE,
        POISON,
        VENOM,
    }
}
