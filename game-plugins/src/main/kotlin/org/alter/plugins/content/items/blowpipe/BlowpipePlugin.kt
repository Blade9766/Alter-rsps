package org.alter.plugins.content.items.blowpipe

import dev.openrune.cache.CacheManager.getItem
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * Loading, checking and emptying the toxic blowpipe.
 *
 * The inventory options bound here are the item's real ones, read out of the cache
 * rather than guessed: a charged blowpipe offers `Wield`, `Check`, `Unload`, `Uncharge`
 * (an empty one only offers `Dismantle`, which is a separate feature and not handled).
 * `Unload` and `Uncharge` mean different things and are deliberately kept apart:
 * - **Unload** takes the darts out and leaves the scales in.
 * - **Uncharge** empties it completely and turns it back into the empty shell.
 *
 * See [Blowpipe] for where the charges are stored and why.
 */
class BlowpipePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        for (blowpipe in listOf("item.toxic_blowpipe", "item.blazing_blowpipe")) {
            for (dart in Blowpipe.LOADABLE_DARTS) {
                onItemOnItem(dart, blowpipe) { loadDarts(player, getRSCM(dart)) }
            }

            onItemOnItem("item.zulrahs_scales", blowpipe) { loadScales(player) }

            onItemOption(blowpipe, "Check") { check(player) }
            onItemOption(blowpipe, "Unload") { unloadDarts(player) }
            onItemOption(blowpipe, "Uncharge") { uncharge(player) }
        }

        /*
         * Scales on an empty shell charge it back up - how a drained blowpipe is
         * brought back into service.
         */
        for (empty in listOf("item.toxic_blowpipe_empty", "item.blazing_blowpipe_empty")) {
            onItemOnItem("item.zulrahs_scales", empty) { chargeEmpty(player, getRSCM(empty)) }
        }
    }

    /**
     * The blowpipe being acted on. Prefers the item the player actually clicked, and
     * only falls back to a search when the interaction reported the *other* item -
     * using darts on a blowpipe reports the darts as the interacting item.
     */
    private fun blowpipeOf(
        player: Player,
        predicate: (Item) -> Boolean,
    ): Item? {
        val interacting = runCatching { player.getInteractingItem() }.getOrNull()
        if (interacting != null && predicate(interacting)) {
            return interacting
        }
        return player.inventory.rawItems.filterNotNull().firstOrNull(predicate)
    }

    private fun plural(count: Int): String = if (count == 1) "" else "s"

    private fun loadDarts(
        player: Player,
        dartId: Int,
    ) {
        val blowpipe = blowpipeOf(player) { Blowpipe.isCharged(it) } ?: return
        val held = player.inventory.getItemCount(dartId)
        if (held <= 0) {
            return
        }

        val loadedId = Blowpipe.dartId(blowpipe)
        val loadedCount = Blowpipe.dartCount(blowpipe)

        /*
         * "Players can swap a different type of dart into the toxic blowpipe without
         * having to unload the darts inside first" - but the old darts still need
         * somewhere to go, so a swap requires a free slot.
         */
        if (loadedId != -1 && loadedId != dartId && loadedCount > 0) {
            if (!player.inventory.hasSpace) {
                player.message("You don't have enough inventory space to swap the darts out.")
                return
            }
            player.inventory.add(item = loadedId, amount = loadedCount)
            Blowpipe.setDarts(blowpipe, dartId, 0)
        }

        val already = if (loadedId == dartId) loadedCount else 0
        val room = Blowpipe.MAX_DARTS - already
        if (room <= 0) {
            player.message("Your blowpipe is already full of darts.")
            return
        }

        val loading = minOf(held, room)
        player.inventory.remove(item = dartId, amount = loading)
        Blowpipe.setDarts(blowpipe, dartId, already + loading)
        player.message("You load $loading dart${plural(loading)} into your blowpipe.")
    }

    private fun loadScales(player: Player) {
        val blowpipe = blowpipeOf(player) { Blowpipe.isCharged(it) } ?: return
        val held = player.inventory.getItemCount(Blowpipe.SCALES)
        if (held <= 0) {
            return
        }
        val current = Blowpipe.scaleCount(blowpipe)
        val room = Blowpipe.MAX_SCALES - current
        if (room <= 0) {
            player.message("Your blowpipe is already fully charged with scales.")
            return
        }
        val loading = minOf(held, room)
        player.inventory.remove(item = Blowpipe.SCALES, amount = loading)
        Blowpipe.setScales(blowpipe, current + loading)
        player.message("You add $loading scale${plural(loading)} to your blowpipe.")
    }

    private fun chargeEmpty(
        player: Player,
        emptyId: Int,
    ) {
        val shell = blowpipeOf(player) { it.id == emptyId } ?: return
        val held = player.inventory.getItemCount(Blowpipe.SCALES)
        if (held <= 0) {
            return
        }
        val slot = player.inventory.rawItems.indexOfFirst { it === shell }
        if (slot == -1) {
            return
        }
        val loading = minOf(held, Blowpipe.MAX_SCALES)
        player.inventory.remove(item = Blowpipe.SCALES, amount = loading)

        val charged = Item(Blowpipe.chargedFormOf(emptyId))
        Blowpipe.setScales(charged, loading)
        player.inventory[slot] = charged
        player.message("You add $loading scale${plural(loading)} to your blowpipe.")
    }

    private fun check(player: Player) {
        val blowpipe = blowpipeOf(player) { Blowpipe.isCharged(it) } ?: return
        val darts = Blowpipe.dartCount(blowpipe)
        val dartId = Blowpipe.dartId(blowpipe)
        val dartName = if (dartId != -1) getItem(dartId).name?.lowercase() ?: "unknown" else "no"
        player.message(
            "Your blowpipe has $darts $dartName dart${plural(darts)} and ${Blowpipe.scaleCount(blowpipe)} scales.",
        )
    }

    private fun unloadDarts(player: Player) {
        val blowpipe = blowpipeOf(player) { Blowpipe.isCharged(it) } ?: return
        val darts = Blowpipe.dartCount(blowpipe)
        val dartId = Blowpipe.dartId(blowpipe)
        if (darts <= 0 || dartId == -1) {
            player.message("Your blowpipe has no darts in it.")
            return
        }
        if (!player.inventory.hasSpace) {
            player.message("You don't have enough inventory space to unload the darts.")
            return
        }
        player.inventory.add(item = dartId, amount = darts)
        Blowpipe.setDarts(blowpipe, dartId, 0)
        player.message("You unload the darts from your blowpipe.")
    }

    private fun uncharge(player: Player) {
        val blowpipe = blowpipeOf(player) { Blowpipe.isCharged(it) } ?: return
        val darts = Blowpipe.dartCount(blowpipe)
        val dartId = Blowpipe.dartId(blowpipe)
        val scales = Blowpipe.scaleCount(blowpipe)

        /*
         * Darts and scales both come back, so this needs a slot for each stack that
         * actually has something in it - checked before anything is removed, so a
         * failed uncharge leaves the blowpipe untouched.
         */
        val stacksNeeded = (if (darts > 0 && dartId != -1) 1 else 0) + (if (scales > 0) 1 else 0)
        if (player.inventory.freeSlotCount < stacksNeeded) {
            player.message("You don't have enough inventory space to uncharge your blowpipe.")
            return
        }

        val slot = player.inventory.rawItems.indexOfFirst { it === blowpipe }
        if (slot == -1) {
            return
        }

        if (darts > 0 && dartId != -1) {
            player.inventory.add(item = dartId, amount = darts)
        }
        if (scales > 0) {
            player.inventory.add(item = Blowpipe.SCALES, amount = scales)
        }
        player.inventory[slot] = Item(Blowpipe.emptyFormOf(blowpipe.id))
        player.message("You empty your blowpipe.")
    }
}
