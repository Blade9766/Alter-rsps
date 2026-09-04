package org.alter.plugins.content.skills.cooking

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import gg.rsmod.util.ServerProperties
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.alter.api.ext.appendToString
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.service.Service
import org.alter.rscm.RSCM.getRSCM
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Loads the cookable food table and the item-on-item recipe chain that feeds it, so both
 * can be looked up by item id at runtime.
 */
class CookingService : Service {
    private val gson = Gson()

    val entries: ObjectArrayList<FoodEntry> = ObjectArrayList()

    val recipes: ObjectArrayList<RecipeEntry> = ObjectArrayList()

    private val entriesByRaw: Int2ObjectOpenHashMap<FoodEntry> = Int2ObjectOpenHashMap()

    override fun init(
        server: Server,
        world: World,
        serviceProperties: ServerProperties,
    ) {
        val file = Paths.get(serviceProperties.get("cooking_food") ?: "../data/cfg/cooking/food.json")
        val recipeFile = Paths.get(serviceProperties.get("cooking_recipes") ?: "../data/cfg/cooking/recipes.json")

        Files.newBufferedReader(file).use { reader ->
            val listType = object : TypeToken<List<FoodEntry>>() {}.type
            entries.addAll(gson.fromJson<List<FoodEntry>>(reader, listType))
        }

        Files.newBufferedReader(recipeFile).use { reader ->
            val listType = object : TypeToken<List<RecipeEntry>>() {}.type
            recipes.addAll(gson.fromJson<List<RecipeEntry>>(reader, listType))
        }

        entries.forEach { entry ->
            // Validated here rather than in an `init` block: Gson allocates without
            // running the constructor, so a field missing from the JSON arrives as 0 and
            // would silently make a food cookable at level 0 for no experience, or - far
            // worse for a stop-burning level - make it burn forever.
            check(entry.level in 1..99) { "Food '${entry.raw}' has an invalid level: ${entry.level}." }
            // Zero is legal, but only for the first gnome cook: turning a raw crunchy
            // tray into a half baked one pays nothing, the experience arriving at the
            // prepare and finish steps instead. Everything else must pay something.
            check(entry.experience >= 0.0) { "Food '${entry.raw}' has negative experience." }
            listOf(
                "fireLevel" to entry.fireLevel,
                "rangeLevel" to entry.rangeLevel,
                "castleLevel" to entry.castleLevel,
                "gauntletLevel" to entry.gauntletLevel,
            ).forEach { (field, value) ->
                check(value == -1 || value in entry.level..99) {
                    "Food '${entry.raw}' has $field = $value; expected -1 (never stops burning) " +
                        "or a level between ${entry.level} and 99."
                }
            }

            entry.rawItemId = getRSCM(entry.raw)
            entry.cookedItemId = getRSCM(entry.cooked)
            entry.burntItemId = getRSCM(entry.burnt)
            entry.returnItemId = entry.returns?.let { getRSCM(it) } ?: -1
            check(!(entry.rangeOnly && entry.fireOnly)) {
                "Food '${entry.raw}' is both range-only and fire-only, so nothing could cook it."
            }

            check(entriesByRaw.put(entry.rawItemId, entry) == null) {
                "Two food entries share the raw item '${entry.raw}'."
            }
        }

        recipes.forEach { recipe ->
            check(recipe.message.isNotBlank()) { "Recipe '${recipe.product}' has no message." }
            check(recipe.ingredients.size >= 2 || recipe.bind?.size == 2) {
                "Recipe '${recipe.product}' needs two ingredients or an explicit bind pair."
            }
            check(recipe.ingredients.distinct().size == recipe.ingredients.size) {
                "Recipe '${recipe.product}' lists the same ingredient twice."
            }
            check(recipe.level in 0..99) { "Recipe '${recipe.product}' has an invalid level: ${recipe.level}." }
            check(recipe.experience >= 0.0) { "Recipe '${recipe.product}' has negative experience." }

            recipe.ingredientIds = recipe.ingredients.map { getRSCM(it) }.toIntArray()
            recipe.ingredientAmounts =
                recipe.amounts?.toIntArray() ?: IntArray(recipe.ingredients.size) { 1 }
            check(recipe.ingredientAmounts.size == recipe.ingredientIds.size) {
                "Recipe '${recipe.product}' has ${recipe.ingredientAmounts.size} amounts for " +
                    "${recipe.ingredientIds.size} ingredients."
            }
            check(recipe.ingredientAmounts.all { it >= 1 }) {
                "Recipe '${recipe.product}' has an ingredient amount below one."
            }
            recipe.productItemId = getRSCM(recipe.product)
            recipe.returnIds = recipe.returns?.map { getRSCM(it) }?.toIntArray() ?: intArrayOf()
            recipe.toolIds = recipe.tools?.map { getRSCM(it) }?.toIntArray() ?: intArrayOf()
            recipe.failProductId = recipe.failProduct?.let { getRSCM(it) } ?: -1
            recipe.bindIds = recipe.bind?.map { getRSCM(it) }?.toIntArray() ?: intArrayOf()
            check(recipe.bindIds.isEmpty() || recipe.bindIds.size == 2) {
                "Recipe '${recipe.product}' has ${recipe.bindIds.size} bind items; item-on-item needs exactly two."
            }
            check((recipe.failProductId == -1) == (recipe.neverFailsLevel == 0)) {
                "Recipe '${recipe.product}' must set both failProduct and neverFailsLevel, or neither."
            }
            check(recipe.toolIds.none { it in recipe.ingredientIds }) {
                "Recipe '${recipe.product}' lists an item as both a tool and an ingredient."
            }
        }

        // A repeated binding pair is legal - it becomes a choice in the chatbox, which is
        // how the four flour-and-water doughs work, and how a half baked batta with equa
        // leaves offers either a fruit or a toad batta. What is not legal is two recipes
        // behind one pair making the same thing, since one would silently shadow the other.
        recipesByPair().forEach { (pair, group) ->
            val products = group.map { it.product }
            check(products.distinct().size == products.size) {
                "Recipes sharing the pair $pair make the same product twice: $products"
            }
        }

        Server.logger.info {
            "Loaded ${entries.size.appendToString("cookable food")} and " +
                "${recipes.size.appendToString("cooking recipe")}."
        }
    }

    fun lookup(rawItemId: Int): FoodEntry? = entriesByRaw[rawItemId]

    /**
     * Recipes grouped by their unordered ingredient pair. A pair with more than one entry
     * - flour and water, which can become any of three doughs - is offered as a choice.
     */
    fun recipesByPair(): Map<Pair<Int, Int>, List<RecipeEntry>> =
        recipes.groupBy { minOf(it.boundPair.first, it.boundPair.second) to maxOf(it.boundPair.first, it.boundPair.second) }
}
