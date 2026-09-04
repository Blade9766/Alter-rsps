package org.alter.plugins.content.areas.wilderness

import dev.openrune.cache.CacheManager.getItem
import org.alter.api.SkullIcon
import org.alter.api.ext.hasSkullIcon
import org.alter.api.ext.inWilderness
import org.alter.api.ext.message
import org.alter.api.ext.skull
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.attr.LAST_HIT_BY_ATTR
import org.alter.game.model.attr.PROTECT_ITEM_ATTR
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.timer.ACTIVE_COMBAT_TIMER
import org.alter.plugins.content.mechanics.grandexchange.GrandExchangeMarket

/**
 * What the Wilderness actually costs you: the skull you get for starting a fight, and the items
 * you lose when you lose one.
 *
 * Before this, dying did nothing at all - [org.alter.game.action.PlayerDeathAction] restores your
 * stats, walks you back to the respawn point and leaves your inventory untouched - so there was no
 * risk anywhere in the game and nothing for a Wilderness kill to be worth.
 *
 * ## Keeping items
 *
 * Straight from the wiki: unskulled you keep your three most valuable items, skulled you keep
 * none, and Protect Item adds one to whichever of those applies. Value order uses the Grand
 * Exchange guide prices this project already bakes out of the wiki
 * ([GrandExchangeMarket.guidePrice]), falling back to the cache's own shop value for anything the
 * exchange has no entry for, which is the closest available stand-in for the real game's ordering.
 *
 * **Stacks are protected one item at a time**, matching the real game: a protected slot spent on a
 * stack of coins keeps a single coin and drops the rest. That falls out of [protect] walking units
 * rather than slots.
 *
 * ## Untradeables are always kept
 *
 * In the real game an unprotected untradeable is lost to Death and bought back from their office
 * for a fee. There is no Death's Office here and no reclaim of any kind, so the only two options
 * were to destroy untradeables outright or to keep them, and keeping them is the one that cannot
 * permanently delete a fire cape over a single misstep. It is a deliberate deviation, not an
 * oversight - if a reclaim system is ever built, this is the rule to revisit.
 */
object WildernessRisk {
    /** Items kept by an unskulled player, before Protect Item. */
    const val KEEP_UNSKULLED = 3

    /** Items kept by a skulled player, before Protect Item. */
    const val KEEP_SKULLED = 0

    /**
     * How long the skull lasts. The wiki gives 30 minutes for a skull earned by attacking another
     * player (as against 10 for the Abyss), and a tick is 0.6 seconds.
     */
    const val SKULL_DURATION_CYCLES = 30 * 60 * 1000 / 600

    /**
     * Skulls [attacker] for opening on [target], unless this swing is retaliation.
     *
     * Retaliation is "the person I am hitting is the person who last hit me, and that fight is
     * still live" - the [ACTIVE_COMBAT_TIMER] half is what stops a fight from ten minutes ago
     * excusing a fresh attack on the same person.
     *
     * Called from [org.alter.plugins.content.combat.Combat.postAttack], which is the one place
     * every attack of every style already funnels through.
     */
    fun onPlayerAttackedPlayer(
        attacker: Player,
        target: Player,
    ) {
        if (!attacker.inWilderness()) {
            return
        }

        val lastAttacker = attacker.attr[LAST_HIT_BY_ATTR]?.get()
        val retaliating = lastAttacker == target && attacker.timers.has(ACTIVE_COMBAT_TIMER)
        if (retaliating) {
            return
        }

        if (!attacker.hasSkullIcon(SkullIcon.WHITE)) {
            attacker.message("<col=4f006f>You are now skulled. You will lose all your items if you die.</col>")
        }
        // Re-skulling refreshes the timer, which is what the real game does too.
        attacker.skull(SkullIcon.WHITE, SKULL_DURATION_CYCLES)
    }

    /** How many items [player] would keep if they died right now. */
    fun keepCount(player: Player): Int {
        val base = if (player.hasSkullIcon(SkullIcon.WHITE)) KEEP_SKULLED else KEEP_UNSKULLED
        return base + if (player.attr[PROTECT_ITEM_ATTR] == true) 1 else 0
    }

    /**
     * Strips [player] down to what they keep and drops the rest where they fell.
     *
     * Run from the pre-death hook, while the player is still standing on the tile they died on and
     * before the respawn moves them. The death is deliberately *not* claimed - the normal respawn
     * is exactly what should happen next.
     */
    fun handleDeath(
        player: Player,
        world: World,
    ) {
        val deathTile = player.tile
        val killer = player.attr[KILLER_ATTR]?.get() as? Player

        val carried = ArrayList<Item>()
        player.inventory.rawItems.filterNotNull().forEach { carried.add(Item(it)) }
        player.equipment.rawItems.filterNotNull().forEach { carried.add(Item(it)) }
        if (carried.isEmpty()) {
            return
        }

        val (untradeable, riskable) = carried.partition { !getItem(it.id).isTradeable }
        val (kept, lost) = protect(riskable, keepCount(player))

        player.inventory.removeAll()
        player.equipment.removeAll()

        /*
         * Untradeables go back first: they are guaranteed to be returned (see this object's doc
         * comment), so they get first claim on the 28 slots if a player somehow carried more than
         * that between worn and carried gear. Anything that still will not fit is dropped for the
         * player rather than quietly vanishing.
         */
        (untradeable + kept).forEach { item ->
            val transaction = player.inventory.add(item.id, item.amount, assureFullInsertion = false)
            /*
             * `add` is documented as taking nothing from the item but its id and amount - "including
             * its attributes" - so charges, degradation and the like would be wiped off everything a
             * player kept. The attributes are copied back onto the instances the container actually
             * stored, which is what the returned slots point at.
             */
            transaction.items.forEach { slot -> player.inventory[slot.slot]?.copyAttr(item) }

            val leftover = item.amount - transaction.completed
            if (leftover > 0) {
                world.spawn(
                    GroundItem(item = item.id, amount = leftover, tile = deathTile, owner = player)
                        .copyAttr(item.attr),
                )
            }
        }

        /*
         * The killer owns the pile, so it is theirs for the private window before it goes public -
         * which is the whole point of a kill. A death to a monster (or to nothing at all) leaves
         * the pile to the player, so they can run back for it.
         */
        val owner = killer ?: player
        lost.forEach { item ->
            world.spawn(
                GroundItem(item = item.id, amount = item.amount, tile = deathTile, owner = owner)
                    .copyAttr(item.attr),
            )
        }

        killer?.message("You have defeated ${player.username}!")
        player.message("<col=4f006f>You have lost your items.</col>")
    }

    /**
     * Splits [items] into the [keep] most valuable individual units and everything else.
     *
     * Walks units rather than slots so that a stack contributes one unit at a time - protecting a
     * stack of runes keeps one rune, not the stack.
     */
    private fun protect(
        items: List<Item>,
        keep: Int,
    ): Pair<List<Item>, List<Item>> {
        if (keep <= 0) {
            return emptyList<Item>() to items
        }

        val byValue = items.sortedByDescending { unitValue(it.id) }
        val kept = ArrayList<Item>()
        val lost = ArrayList<Item>()
        var remaining = keep

        byValue.forEach { item ->
            val protectedUnits = minOf(remaining, item.amount)
            remaining -= protectedUnits
            if (protectedUnits > 0) {
                kept.add(Item(item.id, protectedUnits).copyAttr(item))
            }
            val dropped = item.amount - protectedUnits
            if (dropped > 0) {
                lost.add(Item(item.id, dropped).copyAttr(item))
            }
        }
        return kept to lost
    }

    /** Guide price where the exchange knows the item, the cache's shop value where it does not. */
    private fun unitValue(item: Int): Int {
        val guide = GrandExchangeMarket.guidePrice(item)
        return if (guide > 0) guide else getItem(item).cost
    }
}
