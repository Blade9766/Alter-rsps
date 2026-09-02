package org.alter.plugins.content.skills.cooking

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The half of Cooking that happens before anything meets a fire: flour and water into
 * dough, dough into a pie shell, fillings into a shell, and a bowl of water into a stew.
 *
 * Every combination is loaded from `data/cfg/cooking/recipes.json` through
 * [CookingService] - this plugin only reads it, since [CookingPlugin] is the one that
 * registers the service. None of these steps grant Cooking experience or check a Cooking
 * level: in OSRS the gate is on baking the result, so a level 1 player can assemble a raw
 * summer pie and then be told they need 95 to cook it.
 *
 * **Ingredient order is fixed for the three-part pies**, exactly as in OSRS. Garden pie
 * really does want tomato, then onion, then cabbage; fish pie wants trout, cod, potato;
 * admiral wants salmon, tuna, potato; wild wants bear meat, chompy, rabbit; summer wants
 * strawberry, watermelon, apple; mud wants compost, water, clay. The cache only has two
 * generic "part X pie" items per pie, so it can't represent a half-built pie whose
 * ingredients went in out of order - which is why the real game fixes the order too. Each
 * order above is from that pie's own wiki `Instructions` block, not from the summary
 * table.
 *
 * Stew is the one place the wiki gives two different orders on two different pages. Its
 * own article's step list - bowl of water, then raw potato, then cooked meat or chicken -
 * is the one implemented here, for the same reason: `Incomplete stew` is a single item
 * that can't record which half of the recipe it already holds.
 *
 * **Not implemented:** curry from three curry leaves (spice, the usual route, works),
 * pizzas, and cakes. Those are the remaining dough-descended chains and want their own
 * pass.
 *
 * **Known upstream gap, not this plugin's to fix:** nothing on this server currently
 * fills a water container. `mechanics/water/WaterPlugin` is commented out in its entirety
 * (it still refers to an `Objs` constants class that predates RSCM), so while general
 * stores sell empty buckets and bowls, a bucket of water has no in-game source at all -
 * and a pot of flour only drops from White Knights, there being no windmill. The recipes
 * below are correct and will work the moment those exist; until then the chain can only
 * be started from spawned or dropped ingredients. The Lumbridge cooking tutor already
 * teaches this recipe, so the gap predates this plugin.
 */
class CookingRecipePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onWorldInit {
            val service = world.getService(CookingService::class.java) ?: return@onWorldInit

            service.recipesByPair().forEach { (pair, recipes) ->
                val (first, second) = pair
                onItemOnItem(first, second) {
                    player.queue(TaskPriority.STANDARD) { make(this, player, recipes) }
                }
            }
        }
    }

    private suspend fun make(
        task: QueueTask,
        player: Player,
        recipes: List<RecipeEntry>,
    ) {
        val recipe =
            if (recipes.size == 1) {
                recipes.single()
            } else {
                // Flour on water is the only pair with a choice behind it, and the game
                // asks with its own skill-multi chatbox rather than a bespoke dialogue.
                var chosen: RecipeEntry? = null
                task.produceItemBox(
                    player,
                    *recipes.map { it.productItemId }.toIntArray(),
                    title = "What would you like to make?",
                    maxProducable = player.inventory.capacity,
                ) { item, _ ->
                    chosen = recipes.firstOrNull { it.productItemId == item }
                }
                chosen ?: return
            }

        // The quantity the chatbox returns is ignored on purpose: OSRS makes a single
        // dough per click, and repeating it here would need a per-ingredient loop that
        // the other recipes (a one-off pie shell, a one-off filling) have no use for.
        combine(player, recipe)
    }

    private fun combine(
        player: Player,
        recipe: RecipeEntry,
    ) {
        if (!player.inventory.contains(recipe.primaryItemId) || !player.inventory.contains(recipe.secondaryItemId)) {
            return
        }

        // Two ingredients go in, but a dough recipe hands back three items - the dough
        // plus the emptied pot and bucket - so it needs a spare slot. Checked before
        // anything is removed, since removing first and failing to add would quietly
        // destroy the ingredients.
        val produced = 1 + listOf(recipe.primaryReplacementId, recipe.secondaryReplacementId).count { it != -1 }
        if (produced - 2 > player.inventory.freeSlotCount) {
            player.message("You don't have enough inventory space to do that.")
            return
        }

        if (player.inventory.remove(item = recipe.primaryItemId, amount = 1).hasFailed()) {
            return
        }
        if (player.inventory.remove(item = recipe.secondaryItemId, amount = 1).hasFailed()) {
            // Put the first one back rather than eating it: the two removals aren't
            // atomic, and a half-applied recipe would silently destroy an ingredient.
            player.inventory.add(item = recipe.primaryItemId, amount = 1)
            return
        }

        player.inventory.add(item = recipe.productItemId, amount = 1)
        if (recipe.primaryReplacementId != -1) {
            player.inventory.add(item = recipe.primaryReplacementId, amount = 1)
        }
        if (recipe.secondaryReplacementId != -1) {
            player.inventory.add(item = recipe.secondaryReplacementId, amount = 1)
        }
        player.message(recipe.message)
    }
}
