package org.alter.plugins.content.skills.smithing

import dev.openrune.cache.CacheManager
import org.alter.api.Skills
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
import org.alter.plugins.content.items.jewellery.JewelleryPerks

/**
 * Smithing, half one: smelting ore into bars at a furnace.
 *
 * Recipes, level requirements and XP are the OSRS Wiki's, loaded from
 * `data/cfg/smithing/bars.json` through [SmithingService]. All eight bars fit inside the
 * real `produceItemBox` skill-multi chatbox (it holds ten items), so this uses the game's
 * own "What would you like to smelt?" interface rather than an invented one.
 *
 * **Iron really does fail half the time.** The wiki is explicit that smelting iron ore
 * fails 50% of the time; that is the only recipe with a `successChance` below 1.0. The
 * ore is consumed on a failure, as in OSRS. The three real exemptions - ring of forging,
 * the Superheat Item spell, and the Blast Furnace - are **not** implemented: the ring
 * carries 140 persisted charges and the Blast Furnace is a minigame, and a ring that
 * never degrades would be strictly better than the real one rather than merely missing.
 *
 * Furnaces are bound by scanning the cache for every object carrying a real "Smelt"
 * action whose name mentions a furnace, so this works at every furnace already in the map
 * data with no per-area configuration - the same approach Mining uses for rocks.
 * Specialty furnaces caught by that scan (the Blast Furnace's coffer, Lovakite, Volcanic)
 * get plain standard smelting rather than their own mechanics.
 *
 * Deliberately does not call [org.alter.game.model.entity.Pawn.lock], for the reason
 * documented on `WoodcuttingPlugin`: a full lock stops `walkTo()` from processing a
 * movement click, stranding the player mid-skill.
 */
class SmeltingPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        loadService(SmithingService())

        onWorldInit {
            val service = world.getService(SmithingService::class.java) ?: return@onWorldInit

            CacheManager.getObjects().forEach { (id, def) ->
                val name = def.name ?: return@forEach
                if (!name.contains("furnace", ignoreCase = true)) return@forEach
                val smelt = def.actions.filterNotNull().firstOrNull { it.equals("Smelt", ignoreCase = true) } ?: return@forEach

                onObjOption(obj = id, option = smelt) {
                    player.queue(TaskPriority.STANDARD) { smeltMenu(this, player, service) }
                }
            }
        }
    }

    private suspend fun smeltMenu(
        task: QueueTask,
        player: Player,
        service: SmithingService,
    ) {
        val bars = service.bars
        var chosen: BarEntry? = null
        var requested = 0

        task.produceItemBox(
            player,
            *bars.map { it.barItemId }.toIntArray(),
            title = "What would you like to smelt?",
            maxProducable = player.inventory.capacity,
        ) { item, qty ->
            chosen = bars.firstOrNull { it.barItemId == item }
            requested = qty
        }

        val entry = chosen ?: return
        if (requested <= 0) {
            return
        }
        smelt(task, player, entry, requested)
    }

    private suspend fun smelt(
        task: QueueTask,
        player: Player,
        entry: BarEntry,
        requested: Int,
    ) {
        if (player.getSkills().getCurrentLevel(Skills.SMITHING) < entry.level) {
            player.message("You need a Smithing level of ${entry.level} to smelt ${entry.name.lowercase()}s.")
            return
        }

        var made = 0
        while (made < requested && !player.hasMoveDestination()) {
            val missing = entry.ingredients.firstOrNull { player.inventory.getItemCount(it.itemId) < it.amount }
            if (missing != null) {
                val oreName = CacheManager.getItem(missing.itemId)?.name?.lowercase() ?: "ore"
                player.message("You need ${missing.amount} $oreName to smelt ${entry.name.lowercase()}s.")
                break
            }

            player.animate(Animation.FURNACE_SMELT)
            player.playSound(Sound.FURNACE)
            task.wait(SMELT_CYCLE_TICKS)

            // Re-checked after the wait: the player may have dropped or used the ore while
            // the animation was playing.
            if (entry.ingredients.any { player.inventory.getItemCount(it.itemId) < it.amount }) {
                break
            }
            entry.ingredients.forEach { player.inventory.remove(item = it.itemId, amount = it.amount) }

            /*
             * A ring of forging makes an iron smelt certain. It is asked *before* the roll rather
             * than only on a failure because a charge goes on every iron ore smelted while the ring
             * is worn, whether or not the ore would have refined anyway.
             */
            val forged = JewelleryPerks.ringOfForgingGuarantees(player, entry.barItemId)

            if (forged || player.world.randomDouble() <= entry.successChance) {
                player.inventory.add(item = entry.barItemId, amount = 1)
                player.addXp(Skills.SMITHING, entry.experience)
                player.message("You retrieve a bar of ${entry.name.removeSuffix(" bar").lowercase()}.")
            } else {
                // OSRS keeps the ore consumed on a failed iron smelt.
                player.message("The ore is too impure and you fail to refine it.")
            }

            made++
        }
    }

    private companion object {
        /** Ticks per smelt, matching the furnace animation's length in OSRS. */
        const val SMELT_CYCLE_TICKS = 5
    }
}
