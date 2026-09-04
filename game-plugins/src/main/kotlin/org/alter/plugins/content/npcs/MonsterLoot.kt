package org.alter.plugins.content.npcs

import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.plugins.content.items.jewellery.RingOfWealth

/**
 * Where a monster's rolled loot actually goes.
 *
 * Every monster plugin used to end its death handler by spawning a `GroundItem` owned by the killer,
 * the same line copied two dozen times. They all go through here now, which is what gives the ring of
 * wealth's currency collection a single place to live instead of two dozen.
 *
 * This is the right level for it rather than `World.spawn`: the effect is specific to loot from a
 * monster the player killed, and intercepting every ground item in the game would also swallow the
 * player's own drops, duel-arena stake returns and search-box rewards.
 */
internal object MonsterLoot {
    /** Drops one rolled item, or hands it to [killer] when the ring of wealth collects it. */
    fun drop(
        world: World,
        killer: Player,
        item: Int,
        amount: Int,
        tile: Tile,
    ) {
        var remaining = amount

        if (RingOfWealth.collects(killer, item)) {
            /*
             * A partial insertion is deliberately allowed rather than an all-or-nothing one: with a
             * nearly full inventory the player should still get what fits, and whatever does not is
             * dropped as normal. Coins and the other two currencies stack, so this only bites when
             * the inventory is full and the player holds none of that currency already.
             */
            val added = killer.inventory.add(item = item, amount = remaining, assureFullInsertion = false).completed
            remaining -= added
            if (remaining <= 0) {
                return
            }
        }

        world.spawn(GroundItem(item = item, amount = remaining, tile = tile, owner = killer))
    }

    /** Drops a whole rolled loot list. */
    fun drop(
        world: World,
        killer: Player,
        loot: Iterable<Pair<Int, Int>>,
        tile: Tile,
    ) {
        loot.forEach { (item, amount) -> drop(world, killer, item, amount, tile) }
    }
}
