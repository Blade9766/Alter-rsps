package org.alter.plugins.diag

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager
import org.alter.plugins.content.mechanics.water.WaterContainers
import org.alter.plugins.content.mechanics.water.WaterSources
import org.alter.plugins.content.skills.cooking.FoodEntry
import org.alter.plugins.content.skills.cooking.RecipeEntry
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Cross-plugin binding checks between Water and Cooking.
 *
 * Both plugins register themselves by scanning the cache, and both land in registries that
 * **throw** rather than warn when the same key is bound twice:
 * `PluginRepository.bindItemOnObject` on a repeated (item, object) pair, and
 * `bindItemOnItem` on a repeated unordered item pair. Either one takes the server down at
 * boot, before a single player connects, and neither plugin can see the other's bindings
 * from inside its own file.
 *
 * The overlap is not hypothetical. Cooking binds every object carrying a "Cook" action,
 * and the cache has objects that are plausibly both - object 7422 is an `Oven` with a Cook
 * action and was a `SINK_7422` in the id list `Waters` used to carry. That collides only if
 * some item is both a raw food and a water container, which is what these tests pin down.
 */
class WaterCookingBindingVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** Mirrors `CookingPlugin.FIRE_OBJECTS`. */
        val FIRE_OBJECTS =
            listOf(
                "object.fire_26185",
                "object.fire_26186",
                "object.fire_26575",
                "object.fire_26576",
                "object.fire_20000",
                "object.fire_20001",
            )
    }

    private val foods: List<FoodEntry> by lazy {
        Files.newBufferedReader(Paths.get("../data/cfg/cooking/food.json")).use {
            Gson().fromJson(it, object : TypeToken<List<FoodEntry>>() {}.type)
        }
    }

    private val recipes: List<RecipeEntry> by lazy {
        Files.newBufferedReader(Paths.get("../data/cfg/cooking/recipes.json")).use {
            Gson().fromJson(it, object : TypeToken<List<RecipeEntry>>() {}.type)
        }
    }

    /** Every (item, object) pair `CookingPlugin` binds. */
    private fun cookingItemOnObject(): Set<Pair<Int, Int>> {
        val heatSources =
            CacheManager
                .getObjects()
                .filterValues { def -> def.actions.filterNotNull().any { it.equals("Cook", ignoreCase = true) } }
                .keys + FIRE_OBJECTS.map { getRSCM(it) }
        return heatSources.flatMap { obj -> foods.map { getRSCM(it.raw) to obj } }.toSet()
    }

    /** Every (item, object) pair `WaterPlugin` binds. */
    private fun waterItemOnObject(): Set<Pair<Int, Int>> =
        WaterSources.scan().keys.flatMap { obj -> WaterContainers.values().map { it.container.unfilled to obj } }.toSet()

    @Test
    fun `water and cooking never bind the same item on the same object`() {
        val clash = cookingItemOnObject() intersect waterItemOnObject()
        assertTrue(
            clash.isEmpty(),
            "bindItemOnObject throws on a repeated (item, object) pair, crashing the server at boot: $clash",
        )
    }

    @Test
    fun `no water source is also a cooking heat source with an overlapping item`() {
        // Even where the objects themselves overlap this is fine, as long as no item is
        // both a raw food and a water container. Spelled out separately so a future food
        // entry like "bowl of water" is caught with a message that says why.
        val containers = WaterContainers.values().flatMap { listOf(it.container.unfilled, it.container.filled) }.toSet()
        val raws = foods.map { getRSCM(it.raw) }.toSet()
        val both = containers intersect raws
        assertTrue(both.isEmpty(), "an item is both a water container and a cookable raw food: $both")
    }

    @Test
    fun `water and cooking never bind the same item-on-item pair`() {
        fun key(a: Int, b: Int) = minOf(a, b) to maxOf(a, b)

        val cookingPairs = recipes.map { key(getRSCM(it.primary), getRSCM(it.secondary)) }.toSet()

        val waterPairs = mutableListOf<Pair<Int, Int>>()
        val toySink = getRSCM("item.sink")
        WaterContainers.values().forEach {
            val unfilled = it.container.unfilled
            val filled = it.container.filled
            waterPairs += key(filled, filled)
            waterPairs += key(unfilled, filled)
            waterPairs += key(toySink, unfilled)
            waterPairs += key(toySink, filled)
        }
        waterPairs += key(getRSCM("item.bowl_of_hot_water"), getRSCM("item.empty_cup"))

        val selfClash = waterPairs.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(selfClash.isEmpty(), "WaterPlugin binds the same pair twice: $selfClash")

        val clash = cookingPairs intersect waterPairs.toSet()
        assertTrue(
            clash.isEmpty(),
            "bindItemOnItem throws on a repeated pair, crashing the server at boot: $clash",
        )
    }

    @Test
    fun `the cooking dough recipes start from containers water can actually fill`() {
        // The whole point of reviving WaterPlugin: flour on a filled container must be
        // reachable, and the emptied container the recipe hands back must be one the
        // player can refill.
        val fillable = WaterContainers.values().associate { it.container.filled to it.container.unfilled }
        val doughs = recipes.filter { it.primary == "item.pot_of_flour" }
        assertTrue(doughs.isNotEmpty(), "no dough recipes found")

        doughs.forEach { recipe ->
            val water = getRSCM(recipe.secondary)
            assertTrue(water in fillable, "${recipe.secondary} is not a container WaterPlugin can fill")
            assertTrue(
                getRSCM(recipe.secondaryReplacement!!) == fillable[water],
                "${recipe.product} hands back ${recipe.secondaryReplacement}, which is not the empty form of ${recipe.secondary}",
            )
        }
    }
}
