package org.alter.plugins.diag

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager
import org.alter.plugins.content.mechanics.milling.MillObjects
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for grain milling, plus the binding-conflict checks against the
 * two plugins it now shares a registry with.
 *
 * Milling is the third cache-scanning plugin to bind into `bindItemOnObject`, after Cooking
 * and Water, and that registry throws rather than warns on a repeated (item, object) pair -
 * a boot crash, not a runtime bug. None of the three can see the others' bindings from
 * inside its own file, so the check lives here.
 */
class MillingVerify {
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

    @Test
    fun `every mill part finds real objects and only the working ones`() {
        MillObjects.values().forEach { part ->
            val found = part.scan()
            assertTrue(found.isNotEmpty(), "${part.objectName} matched nothing in the cache")
            found.forEach { (id, option) ->
                val def = assertNotNull(CacheManager.getObject(id), "object $id vanished from the cache")
                assertTrue(def.name.equals(part.objectName, ignoreCase = true), "object $id is named ${def.name}")
                assertTrue(option in def.actions.filterNotNull(), "object $id has no '$option' action")
            }
        }
    }

    @Test
    fun `the decoy hoppers are excluded`() {
        val hoppers = MillObjects.HOPPER.scan().keys
        // Egg hopper (Load/Look-in) and Gingerbread hopper (Fill) are the two the name
        // would catch on a substring match; the Blast Furnace (Deposit) and Motherlode Mine
        // (Use) hoppers are named exactly "Hopper" and are excluded only by the action.
        listOf(20264, 20265, 20266, 46446, 46448, 26674, 30973, 54903).forEach {
            assertTrue(it !in hoppers, "object $it should not be a mill hopper")
        }
        // ...while the real ones are all present.
        listOf(2586, 24960, 36591, 52591).forEach {
            assertTrue(it in hoppers, "object $it is a real mill hopper and should be bound")
        }
    }

    @Test
    fun `wheat without a Pick action is left alone`() {
        val wheat = MillObjects.WHEAT.scan().keys
        listOf(7463, 7464, 7465, 51839).forEach {
            assertTrue(it !in wheat, "object $it is decorative wheat with no Pick action")
        }
        assertTrue(313 in wheat, "the Lumbridge wheat field should be pickable")
    }

    @Test
    fun `the milling items resolve and are the ones cooking expects`() {
        val grain = getRSCM("item.grain")
        val pot = getRSCM("item.pot")
        val potOfFlour = getRSCM("item.pot_of_flour")
        listOf(grain, pot, potOfFlour).forEach {
            assertTrue(it > 0)
            assertNotNull(CacheManager.getItem(it))
        }
        assertEquals("Grain", CacheManager.getItem(grain)?.name)
        assertEquals("Pot", CacheManager.getItem(pot)?.name)
        assertEquals("Pot of flour", CacheManager.getItem(potOfFlour)?.name)

        // The whole point: what the mill produces is what the dough recipes consume, and
        // what dough hands back is what the mill takes.
        val doughs = recipes.filter { it.ingredients[0] == "item.pot_of_flour" }
        assertTrue(doughs.isNotEmpty(), "no dough recipes consume a pot of flour")
        assertTrue(doughs.all { it.returns?.contains("item.pot") == true }, "dough should hand back the pot the mill refills")
    }

    @Test
    fun `milling never binds an item on an object that cooking or water already claimed`() {
        val grain = getRSCM("item.grain")
        val pot = getRSCM("item.pot")

        val milling =
            MillObjects.HOPPER.scan().keys.map { grain to it }.toSet() +
                MillObjects.BIN.scan().keys.map { pot to it }.toSet()

        val cookingObjects =
            CacheManager
                .getObjects()
                .filterValues { def -> def.actions.filterNotNull().any { it.equals("Cook", ignoreCase = true) } }
                .keys + FIRE_OBJECTS.map { getRSCM(it) }
        val cooking = cookingObjects.flatMap { obj -> foods.map { getRSCM(it.raw) to obj } }.toSet()

        val water =
            WaterSources.scan().keys.flatMap { obj -> WaterContainers.values().map { it.container.unfilled to obj } }.toSet()

        // DairyChurnPlugin is the fourth cache-scanning plugin in this registry: it binds a
        // bucket of milk on every "Dairy churn" object.
        val milkId = getRSCM("item.bucket_of_milk")
        val churns =
            CacheManager
                .getObjects()
                .filterValues { def ->
                    def.name.equals("Dairy churn", ignoreCase = true) &&
                        def.actions.filterNotNull().any { it.equals("Churn", ignoreCase = true) }
                }.keys
        assertTrue(churns.isNotEmpty(), "no dairy churns found in the cache")
        val churning = churns.map { milkId to it }.toSet()

        val everyoneElse = cooking + water + churning
        val clash = milling intersect everyoneElse
        assertTrue(
            (churning intersect (cooking + water)).isEmpty(),
            "the dairy churn collides with cooking or water bindings",
        )
        assertTrue(
            clash.isEmpty(),
            "bindItemOnObject throws on a repeated (item, object) pair, crashing the server at boot: $clash",
        )
    }

    @Test
    fun `milling never binds an object option another mill part already claimed`() {
        // bindObject throws on a repeated (object, option-slot) pair too. The four parts
        // are matched on distinct names, so an object can only belong to one of them -
        // this asserts that rather than assuming it.
        val seen = mutableMapOf<Int, MillObjects>()
        MillObjects.values().forEach { part ->
            part.scan().keys.forEach { id ->
                val previous = seen.put(id, part)
                assertTrue(previous == null, "object $id is claimed by both $previous and $part")
            }
        }
    }
}
