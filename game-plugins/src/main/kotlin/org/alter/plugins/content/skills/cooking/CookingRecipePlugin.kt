package org.alter.plugins.content.skills.cooking

import dev.openrune.cache.CacheManager.getItem
import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * The half of Cooking that happens away from the heat: flour and water into dough, dough
 * into pie shells and pizza bases, fillings into shells, a bowl of water into a stew,
 * three ingredients into a cake tin, and grapes into wine.
 *
 * Every combination is loaded from `data/cfg/cooking/recipes.json` through
 * [CookingService] - this plugin only reads it, since [CookingPlugin] is the one that
 * registers the service. Most steps neither grant Cooking experience nor check a level: in
 * OSRS the gate is on baking the result, so a level 1 player can assemble a raw summer pie
 * and then be told they need 95 to cook it. The exceptions are the assembly steps that are
 * real Cooking actions in their own right - kneading a pizza base needs 35, topping a
 * **cooked** plain pizza with meat or chicken needs 45 and pays 26, anchovies 55 and 39,
 * pineapple 65 and 45, and covering a cake in chocolate needs 50 and pays 30 - which is
 * why [RecipeEntry] carries a level and an experience value at all.
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
 * A cake is the one recipe that takes more than two ingredients - a tin, an egg, a bucket
 * of milk and a pot of flour - and item-on-item can only bind a pair, so it is expressed
 * as three recipes that each pair the tin with a different one of the three. Clicking any
 * of them onto the tin then works, as it does in OSRS, and [combine] consumes the whole
 * ingredient list rather than just the pair that was clicked.
 *
 * **Wine ferments on a timer rather than over heat.** Grapes in a jug of water make an
 * unfermented wine; twelve seconds later every unfermented wine in the inventory turns at
 * once, and the timer restarts whenever another jug is made. See [ferment].
 *
 * **The baked-potato toppings are here in full**, all six of them, and they are the
 * deepest chain in the skill: a knife chops an onion, mushroom, tuna, tomato, garlic or
 * cooked meat into a bowl; some of those bowls are then cooked (see [CookingPlugin]);
 * and the results combine into spicy sauce, chilli con carne, egg and tomato, mushroom &
 * onion or tuna and corn before going on a buttered potato. The butter and cheese come
 * from [DairyChurnPlugin].
 *
 * A note on their experience figures, because the wiki contradicts itself: the main
 * Cooking article's Vegetables table lists a **cumulative-from-scratch** number for some
 * rows and a per-step number for others - it gives chilli con carne 25, which is really
 * its spicy sauce's 25 plus its own 0, and mushroom & onion 120, which is its two fried
 * bowls at 60 each. The per-step figures from each item's own recipe block are what is
 * used here, so making one from scratch totals what the table says.
 *
 * **Not implemented:** curry from three curry leaves; spice, the usual route, works.
 */
class CookingRecipePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    private var unfermentedWine = -1
    private var wine = -1
    private var badWine = -1

    init {
        onWorldInit {
            val service = world.getService(CookingService::class.java) ?: return@onWorldInit

            unfermentedWine = getRSCM("item.unfermented_wine")
            wine = getRSCM("item.jug_of_wine")
            badWine = getRSCM("item.jug_of_bad_wine")

            service.recipesByPair().forEach { (pair, recipes) ->
                val (first, second) = pair
                onItemOnItem(first, second) {
                    player.queue(TaskPriority.STANDARD) { make(this, player, recipes) }
                }
            }
        }

        onTimer(WINE_FERMENT) { ferment(player) }
    }

    private suspend fun make(
        task: QueueTask,
        player: Player,
        recipes: List<RecipeEntry>,
    ) {
        var chosen: RecipeEntry? = null
        var requested = 0

        // The chatbox asks how many even when there is only one product, which is what
        // turns an inventory of dough or wine into one action rather than fourteen clicks.
        // Flour on water is the only pair with a real choice behind it: four doughs, one
        // of them level-gated.
        task.produceItemBox(
            player,
            *recipes.map { it.productItemId }.toIntArray(),
            title = "What would you like to make?",
            maxProducable = player.inventory.capacity,
        ) { item, qty ->
            chosen = recipes.firstOrNull { it.productItemId == item }
            requested = qty
        }

        val recipe = chosen ?: return
        if (requested <= 0) {
            return
        }
        if (player.getSkills().getCurrentLevel(Skills.COOKING) < recipe.level) {
            player.message("You need a Cooking level of ${recipe.level} to do that.")
            return
        }

        // No per-item wait: OSRS makes a whole inventory of dough in a single action, and
        // a tick apiece would make this slower here than in the real game.
        repeat(requested) {
            if (!combine(player, recipe)) {
                return
            }
        }
    }

    /** Returns false when the recipe couldn't be made, so a repeated run stops early. */
    private fun combine(
        player: Player,
        recipe: RecipeEntry,
    ): Boolean {
        // Every ingredient, not only the bound pair: a cake wants a tin, an egg, milk and
        // flour, and clicking any one of the three onto the tin has to find the other two.
        if (recipe.ingredientIds.withIndex().any { (i, id) ->
                player.inventory.getItemCount(id) < recipe.ingredientAmounts[i]
            }
        ) {
            return false
        }

        // Tools are held, not eaten - a knife chops an onion into a bowl and stays a knife.
        val missingTool = recipe.toolIds.firstOrNull { !player.inventory.contains(it) && !player.equipment.contains(it) }
        if (missingTool != null) {
            player.message("You need a ${getItem(missingTool).name.lowercase()} to do that.")
            return false
        }

        // A dough hands back three items - the dough plus the emptied pot and bucket - for
        // the two that went in, so it needs a spare slot. Checked before anything is
        // removed, since removing first and failing to add would quietly destroy the
        // ingredients.
        val net = 1 + recipe.returnIds.size - recipe.ingredientAmounts.sum()
        if (net > player.inventory.freeSlotCount) {
            player.message("You don't have enough inventory space to do that.")
            return false
        }

        // The removals aren't atomic, so anything already taken goes back if a later one
        // fails; a half-applied recipe would silently destroy an ingredient.
        val removed = mutableListOf<Pair<Int, Int>>()
        recipe.ingredientIds.forEachIndexed { i, id ->
            val amount = recipe.ingredientAmounts[i]
            if (player.inventory.remove(item = id, amount = amount, assureFullRemoval = true).hasFailed()) {
                removed.forEach { (back, n) -> player.inventory.add(item = back, amount = n) }
                return false
            }
            removed += id to amount
        }

        // Only the ugthanki kebab can fail. The wiki puts success at roughly 46% at level
        // 1, rising to certain at 37; the curve between is an approximation, as elsewhere.
        val level = player.getSkills().getCurrentLevel(Skills.COOKING)
        val failed =
            recipe.failProductId != -1 &&
                level < recipe.neverFailsLevel &&
                player.world.randomDouble() > successChance(level, recipe)

        player.inventory.add(item = if (failed) recipe.failProductId else recipe.productItemId, amount = 1)
        recipe.returnIds.forEach { player.inventory.add(item = it, amount = 1) }
        if (failed) {
            player.message("You make a mess of it.")
            return true
        }
        if (recipe.experience > 0.0) {
            player.addXp(Skills.COOKING, recipe.experience)
        }
        player.message(recipe.message)

        if (recipe.productItemId == unfermentedWine) {
            // Set again rather than only when idle: the wiki says making another jug
            // restarts the twelve seconds, so a whole batch turns together.
            player.timers[WINE_FERMENT] = FERMENT_TICKS
        }
        return true
    }

    /**
     * Turns every unfermented wine in the inventory once the timer runs out.
     *
     * The wiki gives the wait as 12 seconds - 20 ticks - and says a failure produces a jug
     * of bad wine, which "will stop failing entirely at Cooking level 68". The chance
     * below that level is an approximation, like every other chance in this skill.
     */
    private fun ferment(player: Player) {
        val level = player.getSkills().getCurrentLevel(Skills.COOKING)
        var turned = 0

        while (player.inventory.contains(unfermentedWine)) {
            if (player.inventory.remove(item = unfermentedWine, amount = 1).hasFailed()) {
                break
            }
            if (player.world.randomDouble() <= failChance(level)) {
                player.inventory.add(item = badWine, amount = 1)
            } else {
                player.inventory.add(item = wine, amount = 1)
                player.addXp(Skills.COOKING, WINE_EXPERIENCE)
            }
            turned++
        }

        if (turned > 0) {
            player.message("Your wine has finished fermenting.")
        }
    }

    /**
     * Chance the ugthanki kebab comes out right, interpolated from [KEBAB_BASE_SUCCESS] at
     * level 1 up to certain at the recipe's own [RecipeEntry.neverFailsLevel].
     */
    private fun successChance(
        level: Int,
        recipe: RecipeEntry,
    ): Double {
        val span = (recipe.neverFailsLevel - 1).coerceAtLeast(1)
        val progress = ((level - 1).toDouble() / span).coerceIn(0.0, 1.0)
        return KEBAB_BASE_SUCCESS + (1.0 - KEBAB_BASE_SUCCESS) * progress
    }

    private fun failChance(level: Int): Double {
        if (level >= WINE_NEVER_FAILS) {
            return 0.0
        }
        val span = (WINE_NEVER_FAILS - WINE_LEVEL).coerceAtLeast(1)
        val progress = ((level - WINE_LEVEL).toDouble() / span).coerceIn(0.0, 1.0)
        return BASE_FAIL_CHANCE * (1.0 - progress)
    }

    private companion object {
        /** Twelve seconds, from the wiki; restarted whenever another jug is made. */
        const val FERMENT_TICKS = 20

        const val WINE_LEVEL = 35
        const val WINE_EXPERIENCE = 200.0

        /** "Will stop failing entirely at Cooking level 68." */
        const val WINE_NEVER_FAILS = 68

        /** Chance of a jug of bad wine at exactly level 35. Approximated, as elsewhere. */
        const val BASE_FAIL_CHANCE = 0.4

        /** "Approximately 46% success at level 1 Cooking", per the wiki. */
        const val KEBAB_BASE_SUCCESS = 0.46

        val WINE_FERMENT = TimerKey(persistenceKey = "wine_ferment")
    }
}
