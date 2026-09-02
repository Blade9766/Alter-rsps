package org.alter.plugins.diag

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager
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
 * Verify-before-wire checks for Cooking: the config parses, every RSCM key resolves to a
 * real cache item, the wiki's burn-level figures are what actually landed in the JSON,
 * and the two cache assumptions `CookingPlugin` makes when it binds itself still hold.
 */
class CookingVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** The "Cook-o-matic 100" in Lumbridge Castle's kitchen. */
        const val LUMBRIDGE_CASTLE_RANGE = 114

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
    fun `config parses and every raw item appears exactly once`() {
        assertEquals(43, foods.size, "expected 43 cookable foods, got ${foods.map { it.raw }}")

        val duplicated = foods.groupBy { it.raw }.filterValues { it.size > 1 }.keys
        assertTrue(duplicated.isEmpty(), "raw items bound twice would crash bindItemOnObject: $duplicated")
    }

    @Test
    fun `every item key resolves to a real cache item`() {
        foods.forEach { food ->
            listOf(food.raw, food.cooked, food.burnt).forEach { key ->
                val id = getRSCM(key)
                assertTrue(id > 0, "$key did not resolve")
                assertNotNull(CacheManager.getItem(id), "$key resolved to $id, which is not in the cache")
            }
        }
    }

    @Test
    fun `cooked item names match the entry names used in chat messages`() {
        foods.forEach { food ->
            assertEquals(
                food.name,
                CacheManager.getItem(getRSCM(food.cooked))?.name,
                "${food.raw} is named '${food.name}' but its cooked item is called something else",
            )
        }
    }

    @Test
    fun `burnt items are the real per-family burnt fish`() {
        // Five distinct "Burnt fish" items share the low fish between them; getting these
        // wrong is invisible in play until someone checks their bank. Ids confirmed
        // against the wiki's Burnt fish infoboxes.
        val expected =
            mapOf(
                "item.raw_shrimps" to 7954,
                "item.raw_anchovies" to 323,
                "item.raw_sardine" to 369,
                "item.raw_herring" to 357,
                "item.raw_mackerel" to 357,
                "item.raw_trout" to 343,
                "item.raw_cod" to 343,
                "item.raw_pike" to 343,
                "item.raw_salmon" to 343,
                "item.raw_tuna" to 367,
                "item.raw_bass" to 367,
            )
        expected.forEach { (raw, burntId) ->
            val food = foods.first { it.raw == raw }
            assertEquals(burntId, getRSCM(food.burnt), "${food.raw} burns to the wrong item")
        }
    }

    @Test
    fun `stop-burning levels are either never or at least the food's own level`() {
        foods.forEach { food ->
            listOf(
                "fireLevel" to food.fireLevel,
                "rangeLevel" to food.rangeLevel,
                "castleLevel" to food.castleLevel,
                "gauntletLevel" to food.gauntletLevel,
            ).forEach { (field, value) ->
                assertTrue(
                    value == -1 || value in food.level..99,
                    "${food.raw} has $field = $value, which is neither -1 nor a level in ${food.level}..99",
                )
            }
        }
    }

    @Test
    fun `the wiki's burn-level figures are what landed in the config`() {
        fun food(raw: String) = foods.first { it.raw == raw }

        // Gauntlet-affected foods, from the first table on Cooking/Burn level.
        food("item.raw_lobster").let {
            assertEquals(listOf(40, 74, 74, 70, 64), listOf(it.level, it.fireLevel, it.rangeLevel, it.castleLevel, it.gauntletLevel))
            assertEquals(120.0, it.experience)
        }
        food("item.raw_swordfish").let {
            assertEquals(listOf(45, 86, 80, 76, 80), listOf(it.level, it.fireLevel, it.rangeLevel, it.castleLevel, it.gauntletLevel))
            assertEquals(140.0, it.experience)
        }
        food("item.raw_monkfish").let {
            assertEquals(listOf(62, 92, 90, 86, 86), listOf(it.level, it.fireLevel, it.rangeLevel, it.castleLevel, it.gauntletLevel))
        }
        food("item.raw_shark").let {
            assertEquals(listOf(80, -1, -1, -1, 94), listOf(it.level, it.fireLevel, it.rangeLevel, it.castleLevel, it.gauntletLevel))
            assertEquals(210.0, it.experience)
        }
        food("item.raw_anglerfish").let {
            assertEquals(listOf(84, -1, -1, -1, 97), listOf(it.level, it.fireLevel, it.rangeLevel, it.castleLevel, it.gauntletLevel))
        }

        // Cod, swordfish, monkfish and curry are the only foods that stop burning earlier
        // on a normal range than on a fire; for everything else the wiki leaves the range
        // column blank, meaning "same level as a fire". (The range-only baked goods have
        // no meaningful fire level at all, so theirs mirrors the range one.)
        val rangeBeatsFire = foods.filter { it.rangeLevel != it.fireLevel }.map { it.raw }
        assertEquals(
            listOf("item.raw_cod", "item.raw_swordfish", "item.raw_monkfish", "item.uncooked_curry").sorted(),
            rangeBeatsFire.sorted(),
        )

        // The foods no amount of levelling saves without gauntlets or a cape.
        val neverStops = foods.filter { it.fireLevel == -1 }.map { it.raw }.sorted()
        assertEquals(
            listOf(
                "item.raw_anglerfish",
                "item.raw_dark_crab",
                "item.raw_manta_ray",
                "item.raw_sea_turtle",
                "item.raw_shark",
                "item.raw_summer_pie",
                "item.raw_wild_pie",
                "item.uncooked_dragonfruit_pie",
            ),
            neverStops,
        )

        // Gauntlets help exactly five foods; for the rest the field mirrors fireLevel so
        // that taking the better of the two changes nothing.
        val gauntletsHelp = foods.filter { it.gauntletLevel != it.fireLevel }.map { it.raw }.sorted()
        assertEquals(
            listOf(
                "item.raw_anglerfish",
                "item.raw_lobster",
                "item.raw_monkfish",
                "item.raw_shark",
                "item.raw_swordfish",
            ),
            gauntletsHelp,
        )
    }

    @Test
    fun `range-only foods are exactly the baked goods`() {
        val rangeOnly = foods.filter { it.rangeOnly }.map { it.cooked }.sorted()
        assertEquals(
            listOf(
                "item.admiral_pie",
                "item.apple_pie",
                "item.botanical_pie",
                "item.bread",
                "item.dragonfruit_pie",
                "item.fish_pie",
                "item.garden_pie",
                "item.meat_pie",
                "item.mud_pie",
                "item.mushroom_pie",
                "item.pitta_bread",
                "item.redberry_pie",
                "item.summer_pie",
                "item.wild_pie",
            ),
            rangeOnly,
            "stew and curry cook on a fire; every pie and both breads need a range",
        )
    }

    @Test
    fun `pitta bread can never actually burn`() {
        // It needs level 58 to cook but the wiki's burn table stops it burning at 37 - a
        // level you can never hold while cooking it. Encoded as "stops at its own
        // requirement" so the effect is right even though the number isn't the wiki's.
        val pitta = foods.first { it.raw == "item.pitta_dough" }
        assertEquals(58, pitta.level)
        assertEquals(58, pitta.rangeLevel)
        assertTrue(pitta.rangeLevel <= pitta.level, "pitta bread should never burn in practice")
    }

    @Test
    fun `recipe config parses and every item key resolves`() {
        assertEquals(38, recipes.size, "expected 38 recipes, got ${recipes.map { it.product }}")

        recipes.forEach { recipe ->
            listOfNotNull(
                recipe.primary,
                recipe.secondary,
                recipe.product,
                recipe.primaryReplacement,
                recipe.secondaryReplacement,
            ).forEach { key ->
                val id = getRSCM(key)
                assertTrue(id > 0, "$key did not resolve")
                assertNotNull(CacheManager.getItem(id), "$key resolved to $id, which is not in the cache")
            }
            assertTrue(recipe.message.isNotBlank(), "${recipe.product} has no message")
        }
    }

    @Test
    fun `no two recipes share an ingredient pair unless they are the doughs`() {
        // bindItemOnItem throws on a duplicate pair, so a clash here is a startup crash.
        // Flour on water is the one pair that legitimately has three products behind it,
        // and it appears once per water container.
        val grouped =
            recipes.groupBy {
                val a = getRSCM(it.primary)
                val b = getRSCM(it.secondary)
                minOf(a, b) to maxOf(a, b)
            }
        val multi = grouped.filterValues { it.size > 1 }
        assertEquals(3, multi.size, "only the three flour+water pairs may have several products")
        multi.values.forEach { group ->
            assertEquals(
                listOf("item.bread_dough", "item.pastry_dough", "item.pitta_dough"),
                group.map { it.product },
            )
            assertTrue(group.all { it.primary == "item.pot_of_flour" })
        }
    }

    @Test
    fun `every recipe chain ends at something cookable`() {
        // Walk each chain forward from the pie shell / bowl of water and check the last
        // product is a raw item the food table knows how to cook - otherwise a player can
        // build an item with nowhere to take it.
        val products = recipes.map { it.product }.toSet()
        // Consumed by a later step, as either half of the pair - pastry dough is the
        // *secondary* of the pie shell recipe, not its primary.
        val consumed = recipes.flatMap { listOf(it.primary, it.secondary) }.toSet()
        val cookable = foods.map { it.raw }.toSet()

        val deadEnds = products.filter { it !in consumed && it !in cookable }
        assertTrue(deadEnds.isEmpty(), "these recipe products can be made but never used: $deadEnds")
    }

    @Test
    fun `the three-part pies use the ingredient order the wiki's instructions give`() {
        fun chain(shellIngredient: String): List<String> {
            val steps = mutableListOf<String>()
            var current = recipes.first { it.primary == "item.pie_shell" && it.secondary == shellIngredient }
            steps += current.secondary
            while (true) {
                val next = recipes.firstOrNull { it.primary == current.product } ?: break
                steps += next.secondary
                current = next
            }
            return steps
        }

        assertEquals(listOf("item.compost", "item.bucket_of_water", "item.clay"), chain("item.compost"))
        assertEquals(listOf("item.tomato", "item.onion", "item.cabbage"), chain("item.tomato"))
        assertEquals(listOf("item.trout", "item.cod", "item.potato"), chain("item.trout"))
        assertEquals(listOf("item.salmon", "item.tuna", "item.potato"), chain("item.salmon"))
        assertEquals(listOf("item.raw_bear_meat", "item.raw_chompy", "item.raw_rabbit"), chain("item.raw_bear_meat"))
        assertEquals(listOf("item.strawberry", "item.watermelon", "item.cooking_apple"), chain("item.strawberry"))
    }

    @Test
    fun `dough recipes hand back the emptied containers`() {
        val doughs = recipes.filter { it.primary == "item.pot_of_flour" }
        assertEquals(9, doughs.size, "three doughs times three water containers")
        doughs.forEach {
            assertEquals("item.pot", it.primaryReplacement, "${it.product} should leave an empty pot")
            assertNotNull(it.secondaryReplacement, "${it.product} should leave the emptied water container")
        }
        assertEquals(
            setOf("item.bucket", "item.jug", "item.bowl"),
            doughs.mapNotNull { it.secondaryReplacement }.toSet(),
        )
    }

    @Test
    fun `the Lumbridge Castle range is a real cookable object`() {
        val def = assertNotNull(CacheManager.getObject(LUMBRIDGE_CASTLE_RANGE), "object 114 is missing from the cache")
        assertEquals("Cooking range", def.name)
        assertTrue(
            def.actions.filterNotNull().any { it.equals("Cook", ignoreCase = true) },
            "object 114 has no Cook action, so CookingPlugin would never bind it",
        )
    }

    @Test
    fun `fires carry no Cook action, so they are only bound once`() {
        // CookingPlugin binds every Cook-action object and then, separately, the six
        // tinderbox fires. If a fire ever gained a Cook action the two passes would bind
        // the same object/item pair twice and bindItemOnObject would throw on startup.
        FIRE_OBJECTS.forEach { key ->
            val id = getRSCM(key)
            val def = assertNotNull(CacheManager.getObject(id), "$key is missing from the cache")
            assertTrue(
                def.actions.filterNotNull().none { it.equals("Cook", ignoreCase = true) },
                "$key now has a Cook action and would be bound twice",
            )
        }
    }
}
