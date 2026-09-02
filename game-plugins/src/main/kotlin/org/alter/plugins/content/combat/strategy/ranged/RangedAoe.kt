package org.alter.plugins.content.combat.strategy.ranged

import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.Combat

/**
 * Picking the extra targets for an area-of-effect ranged attack.
 *
 * **Npcs only.** This codebase has no engine-level single/multi-combat zone enforcement
 * - `setMultiCombatRegion` and `Tile.isMulti` only drive the client's multi-combat icon
 * varbit, and `Combat.canEngage` has no such check - so an area attack that also caught
 * players would let one player splash damage across bystanders anywhere in the world.
 * Until multi-combat zones actually gate combat, area attacks stop at monsters.
 */
object RangedAoe {
    /**
     * [primary] followed by up to [max] - 1 other attackable npcs within [radius] tiles
     * of it, nearest first.
     */
    fun targetsAround(
        player: Player,
        primary: Pawn,
        radius: Int,
        max: Int,
    ): List<Pawn> {
        val extra = mutableListOf<Npc>()
        player.world.npcs.forEach { npc ->
            if (npc != primary &&
                npc.isAlive() &&
                npc.tile.isWithinRadius(primary.tile, radius) &&
                Combat.canEngage(player, npc)
            ) {
                extra.add(npc)
            }
        }
        extra.sortBy { it.tile.getDistance(primary.tile) }
        return (listOf(primary) + extra).take(max)
    }
}
