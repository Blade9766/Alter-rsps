package org.alter.plugins.content.skills.cooking

import dev.openrune.cache.CacheManager
import org.alter.api.Skills
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
 * The dairy churn: milk in, cream, butter or cheese out.
 *
 * This exists for the potato toppings - a potato with butter needs a pat of butter and a
 * potato with cheese needs cheese, and neither had any other source on this server, which
 * would have left the whole topping tree in [CookingRecipePlugin] unreachable.
 *
 * Churns are found by scanning the cache for objects named "Dairy churn" carrying a real
 * "Churn" action, the discipline every cache-scanning plugin here uses. All four in the
 * cache match, so no ids are named.
 *
 * **All six conversions the wiki documents are here**, each with its own level, experience
 * and churning time from that item's recipe block. The three routes to cheese pay very
 * differently - 64 from milk, 46 from cream, 23.5 from butter - because the experience is
 * for the work still left to do, so churning milk all the way to cheese in one go pays the
 * same as doing it in stages.
 *
 * **One inference, stated plainly:** the wiki's recipe blocks list a bucket of milk as the
 * only material for cream, butter and cheese, and the Pat of butter article says the bucket
 * comes back empty. It says nothing about the pot a pot of cream arrives in. Since the
 * bucket is documented as returned in the butter case and the material list is identical in
 * all three, the bucket is returned for every milk conversion here and the pot is treated as
 * something the churn provides. The reverse - a bucket silently becoming a pot - would be an
 * item conversion no source describes. The pot itself is *not* handed back when cream is
 * churned onward into butter or cheese, for the same reason: nothing documents it.
 */
class DairyChurnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    /**
     * One churn conversion.
     *
     * [returnsBucket] is true only for the three that start from a bucket of milk.
     */
    private data class Conversion(
        val input: String,
        val output: String,
        val level: Int,
        val experience: Double,
        val ticks: Int,
        val returnsBucket: Boolean,
        val message: String,
    ) {
        val inputId: Int by lazy { getRSCM(input) }
        val outputId: Int by lazy { getRSCM(output) }
    }

    init {
        onWorldInit {
            CacheManager.getObjects().forEach { (id, def) ->
                if (!def.name.equals(CHURN_NAME, ignoreCase = true)) return@forEach
                val churn =
                    def.actions.filterNotNull().firstOrNull { it.equals(CHURN_OPTION, ignoreCase = true) }
                        ?: return@forEach

                onObjOption(obj = id, option = churn) {
                    player.queue(TaskPriority.STANDARD) { churnMenu(this, player, null) }
                }
                // Using a dairy item straight on the churn narrows the menu to what that
                // item can become, which is how the real thing behaves.
                INPUTS.forEach { input ->
                    onItemOnObj(obj = id, item = getRSCM(input)) {
                        player.queue(TaskPriority.STANDARD) { churnMenu(this, player, getRSCM(input)) }
                    }
                }
            }
        }
    }

    /**
     * Opens the "what would you like to make?" chatbox.
     *
     * [only] restricts it to conversions from that one input, for the use-item-on-churn
     * route; a plain click on the churn offers everything the player is actually holding
     * the ingredients for.
     */
    private suspend fun churnMenu(
        task: QueueTask,
        player: Player,
        only: Int?,
    ) {
        val available =
            CONVERSIONS
                .filter { only == null || it.inputId == only }
                .filter { player.inventory.contains(it.inputId) }

        if (available.isEmpty()) {
            player.message("You have nothing here that you can churn.")
            return
        }

        // Several inputs can reach the same output - cheese comes from milk, cream or
        // butter - so the chatbox lists each product once, and the conversion actually used
        // is the first one whose input the player has.
        val offered = available.distinctBy { it.outputId }

        var chosen: Conversion? = null
        var requested = 0

        task.produceItemBox(
            player,
            *offered.map { it.outputId }.toIntArray(),
            title = "What would you like to make?",
            maxProducable = offered.maxOf { player.inventory.getItemCount(it.inputId) },
        ) { item, qty ->
            chosen = available.firstOrNull { it.outputId == item }
            requested = qty
        }

        val conversion = chosen ?: return
        if (requested <= 0) {
            return
        }
        if (player.getSkills().getCurrentLevel(Skills.COOKING) < conversion.level) {
            player.message("You need a Cooking level of ${conversion.level} to make that.")
            return
        }

        churn(task, player, conversion, requested)
    }

    private suspend fun churn(
        task: QueueTask,
        player: Player,
        conversion: Conversion,
        requested: Int,
    ) {
        val bucket = getRSCM(BUCKET)
        var made = 0

        while (made < requested && !player.hasMoveDestination()) {
            if (!player.inventory.contains(conversion.inputId)) {
                break
            }

            player.animate(CHURN_ANIMATION)
            task.wait(conversion.ticks)

            // Re-checked after the wait: the input may have gone while the animation ran.
            if (player.inventory.remove(item = conversion.inputId, amount = 1).hasFailed()) {
                break
            }
            player.inventory.add(item = conversion.outputId, amount = 1)
            if (conversion.returnsBucket) {
                player.inventory.add(item = bucket, amount = 1)
            }
            player.addXp(Skills.COOKING, conversion.experience)
            player.message(conversion.message)
            made++
        }
    }

    private companion object {
        const val CHURN_NAME = "Dairy churn"
        const val CHURN_OPTION = "Churn"
        const val BUCKET = "item.bucket"

        /** The generic "use item on object" animation, as the water containers use. */
        const val CHURN_ANIMATION = 832

        val CONVERSIONS =
            listOf(
                // From a bucket of milk. The bucket comes back on all three.
                Conversion(
                    "item.bucket_of_milk", "item.pot_of_cream", 21, 18.0, 10, true,
                    "You churn the milk into a pot of cream.",
                ),
                Conversion(
                    "item.bucket_of_milk", "item.pat_of_butter", 38, 40.5, 20, true,
                    "You churn the milk into a pat of butter.",
                ),
                Conversion(
                    "item.bucket_of_milk", "item.cheese", 48, 64.0, 26, true,
                    "You churn the milk into some cheese.",
                ),
                // Carrying a part-churned product further pays only for the work left.
                Conversion(
                    "item.pot_of_cream", "item.pat_of_butter", 38, 22.5, 10, false,
                    "You churn the cream into a pat of butter.",
                ),
                Conversion(
                    "item.pot_of_cream", "item.cheese", 48, 46.0, 19, false,
                    "You churn the cream into some cheese.",
                ),
                Conversion(
                    "item.pat_of_butter", "item.cheese", 48, 23.5, 10, false,
                    "You churn the butter into some cheese.",
                ),
            )

        /** The distinct items that can go into a churn. */
        val INPUTS = CONVERSIONS.map { it.input }.distinct()
    }
}
