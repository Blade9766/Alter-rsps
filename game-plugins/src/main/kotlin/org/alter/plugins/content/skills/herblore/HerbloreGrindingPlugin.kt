package org.alter.plugins.content.skills.herblore

import dev.openrune.cache.CacheManager.getItem
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * The preparation half of Herblore: turning horns, bones, scales and shards into the dusts
 * the potions want, and cracking a coconut into a vial of coconut milk.
 *
 * Both are Herblore in spirit rather than in the skill panel - none of it needs a level or
 * pays any experience, exactly as in OSRS, where the level is charged on the potion the
 * dust ends up in. They live here because nothing else in the game produces unicorn horn
 * dust or coconut milk, and half of [HerblorePlugin]'s recipe table would be unmakeable
 * without them.
 *
 * **Grindings** come from `data/cfg/herblore/grinding.json` through [HerbloreService].
 * Nine of them turn one item into one; the two exceptions are the wiki's own: a crystal
 * shard makes ten crystal dust, and a lava scale makes "between 3-6 lava scale shards",
 * rolled per scale. Both pestles in the cache are wired - the ordinary one and the crystal
 * one sold in Prifddinas - since either grinds.
 *
 * **Coconut milk** is two steps, per the item's wiki recipe block: a hammer cracks a
 * coconut into a half coconut, and pouring that into an empty vial gives coconut milk plus
 * a coconut shell. The shell is kept rather than dropped because it composts.
 *
 * **Deliberately not implemented:** silver dust, which the wiki's recipe block puts on the
 * Ectofuntus bone grinder rather than a pestle and mortar; chocolate dust's alternative
 * knife route, since the pestle already makes it; and the hard Wilderness Diary's better
 * lava scale yield.
 */
class HerbloreGrindingPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onWorldInit {
            // Registered by HerblorePlugin, which owns the service; this plugin only reads it.
            val service = world.getService(HerbloreService::class.java) ?: return@onWorldInit

            val pestles = PESTLES.map { getRSCM(it) }
            service.grindings.forEach { entry ->
                pestles.forEach { pestle ->
                    onItemOnItem(pestle, entry.inputItemId) {
                        player.queue(TaskPriority.STANDARD) { grind(this, player, entry) }
                    }
                }
            }

            onItemOnItem(getRSCM(HAMMER), getRSCM(COCONUT)) {
                player.queue(TaskPriority.STANDARD) { crack(this, player) }
            }

            onItemOnItem(getRSCM(VIAL), getRSCM(HALF_COCONUT)) {
                player.queue(TaskPriority.STANDARD) { pour(this, player) }
            }
        }
    }

    private suspend fun grind(
        task: QueueTask,
        player: Player,
        entry: GrindEntry,
    ) {
        var requested = 0
        task.produceItemBox(
            player,
            entry.productItemId,
            title = "What would you like to make?",
            maxProducable = player.inventory.getItemCount(entry.inputItemId),
        ) { _, qty ->
            requested = qty
        }
        if (requested <= 0) {
            return
        }

        var made = 0
        while (made < requested && !player.hasMoveDestination()) {
            if (!player.inventory.contains(entry.inputItemId)) {
                break
            }

            player.animate(Animation.GRIND)
            player.playSound(Sound.GRIND)
            task.wait(GRIND_TICKS)

            if (player.inventory.remove(item = entry.inputItemId, amount = 1).hasFailed()) {
                break
            }
            // Rolled per item rather than once for the batch, so a stack of lava scales
            // gives a spread of yields the way it does in game.
            val amount = if (entry.high > entry.low) player.world.random(entry.low..entry.high) else entry.low
            player.inventory.add(item = entry.productItemId, amount = amount)
            player.message("You grind the ${getItem(entry.inputItemId).name.lowercase()} into a fine powder.")
            made++
        }

        if (made > 0) {
            player.animate(-1)
        }
    }

    /**
     * A hammer on a coconut: one blow, one half coconut. The coconut chain has no level and
     * no experience, so there is nothing to check before swinging.
     */
    private suspend fun crack(
        task: QueueTask,
        player: Player,
    ) {
        val coconut = getRSCM(COCONUT)
        var requested = 0
        task.produceItemBox(
            player,
            getRSCM(HALF_COCONUT),
            title = "What would you like to make?",
            maxProducable = player.inventory.getItemCount(coconut),
        ) { _, qty ->
            requested = qty
        }

        repeat(requested) {
            if (player.inventory.remove(item = coconut, amount = 1).hasFailed()) {
                return
            }
            player.inventory.add(item = HALF_COCONUT, amount = 1)
        }
        if (requested > 0) {
            player.message("You crack open the coconut.")
        }
    }

    /**
     * A half coconut into an empty vial: coconut milk, and the emptied shell back.
     *
     * Two items go in and two come out, so this never needs a spare slot. The removals
     * aren't atomic, though, so a vial that fails to leave puts the half coconut back
     * rather than destroying it.
     */
    private suspend fun pour(
        task: QueueTask,
        player: Player,
    ) {
        val half = getRSCM(HALF_COCONUT)
        val vial = getRSCM(VIAL)
        var requested = 0

        task.produceItemBox(
            player,
            getRSCM(COCONUT_MILK),
            title = "What would you like to make?",
            maxProducable = minOf(player.inventory.getItemCount(half), player.inventory.getItemCount(vial)),
        ) { _, qty ->
            requested = qty
        }

        repeat(requested) {
            if (!player.inventory.contains(half) || !player.inventory.contains(vial)) {
                return
            }
            if (player.inventory.remove(item = half, amount = 1, assureFullRemoval = true).hasFailed()) {
                return
            }
            if (player.inventory.remove(item = vial, amount = 1, assureFullRemoval = true).hasFailed()) {
                player.inventory.add(item = half, amount = 1)
                return
            }
            player.inventory.add(item = COCONUT_MILK, amount = 1)
            player.inventory.add(item = COCONUT_SHELL, amount = 1)
        }
        if (requested > 0) {
            player.message("You pour the coconut milk into the vial.")
        }
    }

    private companion object {
        /** The ordinary pestle and mortar, and the one Prifddinas sells; either grinds. */
        val PESTLES = arrayOf("item.pestle_and_mortar", "item.pestle_and_mortar_23865")

        const val HAMMER = "item.hammer"
        const val COCONUT = "item.coconut"
        const val HALF_COCONUT = "item.half_coconut"
        const val COCONUT_MILK = "item.coconut_milk"
        const val COCONUT_SHELL = "item.coconut_shell"

        /** The empty vial a half coconut is poured into. */
        const val VIAL = "item.vial"

        /**
         * The wiki gives grinding as "0 ticks, then 2, then 3" - the first is free and the
         * rest settle into a rhythm. One tick apiece is the closest single figure that
         * doesn't make a stack of scales slower here than in the real game.
         */
        const val GRIND_TICKS = 1
    }
}
