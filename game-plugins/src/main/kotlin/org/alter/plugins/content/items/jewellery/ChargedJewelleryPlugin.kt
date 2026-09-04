package org.alter.plugins.content.items.jewellery

import org.alter.api.ext.getInteractingItemSlot
import org.alter.api.ext.message
import org.alter.api.ext.options
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.fs.ObjectExamineHolder
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.magic.TeleportType
import org.alter.plugins.content.magic.canTeleport
import org.alter.plugins.content.magic.teleport
import org.alter.rscm.RSCM.getRSCM

/**
 * Charged teleport jewellery: rubbing it, wearing it and operating it, and what a charge costs.
 *
 * This replaces two earlier plugins. `AmuletOfGloryPlugin` was a class with an empty `init` and its
 * entire body commented out, so the glory did nothing at all. `RingOfWealthPlugin` did run, but
 * every one of its four destinations was the same tile (3004, 3361) except the Grand Exchange, it
 * never spent a charge when rubbed from the inventory, and it left the uncharged ring - the item a
 * spent one turns into - completely unhandled.
 *
 * The three ways a piece is used all funnel into [useCharge]:
 *
 *  - a worn destination option, which teleports straight away;
 *  - "Rub" on an inventory piece, which asks where first (or teleports at once when the piece has
 *    only the one destination, which is the ring of returning);
 *  - "Rub" on a *worn* piece, which only the uncharged dragonstone four carry - the cache replaces
 *    that option with the destination list as soon as the piece holds a charge.
 *
 * Everything about which options exist, and which item is which charge, comes from
 * [ChargedJewellery]; nothing here knows about any individual piece.
 */
class ChargedJewelleryPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    /** One resolved charge ladder, so a runtime lookup never re-resolves an RSCM key. */
    private class Ladder(val entry: ChargedJewellery, val charged: IntArray, val uncharged: Int?)

    init {
        ChargedJewellery.values.forEach { entry ->
            val ladder =
                Ladder(
                    entry = entry,
                    charged = entry.chargeItems.map { getRSCM(it) }.toIntArray(),
                    uncharged = entry.uncharged?.let { getRSCM(it) },
                )

            entry.chargeItems.forEachIndexed { index, item ->
                val charges = index + 1

                entry.destinations.forEach { destination ->
                    bindEquipmentOption(item, destination.option) {
                        useCharge(player, ladder, charges, destination.tile, worn = true)
                    }
                }

                onItemOption(item, option = RUB) {
                    rubFromInventory(player, ladder, charges)
                }
            }

            entry.uncharged?.let { uncharged ->
                bindEquipmentOption(uncharged, RUB) { player.message(entry.depletedMessage) }
                onItemOption(uncharged, option = RUB) { player.message(entry.depletedMessage) }
            }
        }
    }

    /**
     * Binds an equipment option only if the item actually carries it.
     *
     * `onEquipmentOption` throws when the option is missing, and a [KotlinPlugin] whose constructor
     * throws registers *nothing* - one renamed option in a future cache would silently take every
     * piece of jewellery in this file down with it. `JewelleryVerify` asserts the full set exists,
     * so a missing option is caught in the build rather than swallowed here.
     */
    private fun bindEquipmentOption(
        item: String,
        option: String,
        logic: org.alter.game.plugin.Plugin.() -> Unit,
    ) {
        val menu = ObjectExamineHolder.EQUIPMENT_MENU.get(getRSCM(item)) ?: return
        if (menu.equipmentMenu.none { it?.equals(option, ignoreCase = true) == true }) {
            return
        }
        onEquipmentOption(item, option, logic)
    }

    /**
     * "Rub" on an inventory piece. A piece with a single destination goes there at once; anything
     * else asks, with a "Nowhere" entry that spends nothing.
     */
    private fun rubFromInventory(
        player: Player,
        ladder: Ladder,
        charges: Int,
    ) {
        val destinations = ladder.entry.destinations
        val slot = player.getInteractingItemSlot()

        if (destinations.size == 1) {
            useCharge(player, ladder, charges, destinations[0].tile, worn = false, inventorySlot = slot)
            return
        }

        player.queue(TaskPriority.STRONG) {
            val labels = destinations.map { it.option } + NOWHERE
            val choice = options(player, *labels.toTypedArray(), title = "Where would you like to teleport to?")
            if (choice !in 1..destinations.size) {
                return@queue
            }
            useCharge(player, ladder, charges, destinations[choice - 1].tile, worn = false, inventorySlot = slot)
        }
    }

    /**
     * Spends one charge and teleports.
     *
     * The wilderness check happens *first*: a teleport that a mysterious force blocks costs nothing,
     * exactly as it does for a teleport spell. [TeleportType.GLORY] is the right type for every piece
     * here, not just the glory - it carries the level 30 limit that applies to teleport items, as
     * opposed to the level 20 limit on teleport spells.
     *
     * A null [tile] means the player's respawn point (the ring of returning).
     */
    private fun useCharge(
        player: Player,
        ladder: Ladder,
        charges: Int,
        tile: Tile?,
        worn: Boolean,
        inventorySlot: Int = -1,
    ) {
        if (!player.canTeleport(TeleportType.GLORY)) {
            return
        }

        val entry = ladder.entry
        val remaining = charges - 1
        val replacement =
            when {
                remaining > 0 -> Item(ladder.charged[remaining - 1])
                ladder.uncharged != null -> Item(ladder.uncharged)
                else -> null
            }

        if (worn) {
            // Guard against the option firing for a piece that is no longer worn - the charge would
            // otherwise be spent against whatever happens to be in the slot now.
            if (player.equipment[entry.slot.id]?.id != ladder.charged[charges - 1]) {
                return
            }
            player.equipment[entry.slot.id] = replacement
        } else {
            if (player.inventory[inventorySlot]?.id != ladder.charged[charges - 1]) {
                return
            }
            player.inventory[inventorySlot] = replacement
        }

        player.message("You rub the ${entry.noun}...")
        if (remaining > 0) {
            val noun = if (remaining == 1) entry.chargeNoun.removeSuffix("s") else entry.chargeNoun
            player.message("<col=7f007f>Your ${entry.displayName} has ${spellOut(remaining)} $noun left.</col>")
        } else {
            player.message("<col=7f007f>${entry.depletedMessage}</col>")
        }

        player.teleport(tile ?: world.gameContext.home, TeleportType.GLORY)
    }

    private fun spellOut(count: Int): String = NUMBER_WORDS.getOrElse(count) { count.toString() }

    private companion object {
        private const val RUB = "Rub"
        private const val NOWHERE = "Nowhere"

        /**
         * OSRS spells the remaining count out in words ("has four uses left"), so this does too.
         * Eight is the largest charge count any piece here holds (the ring of dueling).
         */
        private val NUMBER_WORDS =
            arrayOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight")
    }
}
