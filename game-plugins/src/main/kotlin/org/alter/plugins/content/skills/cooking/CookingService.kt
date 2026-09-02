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
            check(entry.experience > 0.0) { "Food '${entry.raw}' has no experience set." }
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

            check(entriesByRaw.put(entry.rawItemId, entry) == null) {
                "Two food entries share the raw item '${entry.raw}'."
            }
        }

        recipes.forEach { recipe ->
            check(recipe.message.isNotBlank()) { "Recipe '${recipe.product}' has no message." }
            recipe.primaryItemId = getRSCM(recipe.primary)
            recipe.secondaryItemId = getRSCM(recipe.secondary)
            recipe.productItemId = getRSCM(recipe.product)
            recipe.primaryReplacementId = recipe.primaryReplacement?.let { getRSCM(it) } ?: -1
            recipe.secondaryReplacementId = recipe.secondaryReplacement?.let { getRSCM(it) } ?: -1
            check(recipe.primaryItemId != recipe.secondaryItemId) {
                "Recipe '${recipe.product}' uses '${recipe.primary}' as both ingredients."
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
        recipes.groupBy { minOf(it.primaryItemId, it.secondaryItemId) to maxOf(it.primaryItemId, it.secondaryItemId) }
}
