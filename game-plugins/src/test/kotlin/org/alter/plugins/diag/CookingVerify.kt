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

        /**
         * Chain ends that are deliberately not food.
         *
         * A mud pie is a Ranged weapon that saps run energy, not something you eat. Pitta
         * bread heals nothing in OSRS - it exists to become a kebab. An unfermented wine
         * is consumed by `CookingRecipePlugin`'s fermentation timer rather than by any
         * recipe, so nothing else references it.
         */
        val TERMINAL_EXEMPTIONS =
            setOf(
                "item.mud_pie",
                "item.pitta_bread",
                "item.unfermented_wine",
                // Gnome half-way stages. They are cooked items, but only ever on the way
                // to the finished dish; eating one is not a thing in OSRS either.
                "item.half_baked_crunchy",
                "item.half_baked_batta",
                "item.half_baked_bowl",
                "item.unfinished_crunchy_9582",
                "item.unfinished_crunchy_9580",
                "item.unfinished_crunchy_9584",
                "item.unfinished_crunchy_9578",
                "item.unfinished_batta_9479",
                "item.unfinished_batta_9481",
                "item.unfinished_batta_9486",
                "item.unfinished_batta_9484",
                "item.unfinished_bowl_9560",
                "item.unfinished_bowl_9562",
                "item.unfinished_bowl_9564",
                // Heating a poured cocktail. The drink is finished by CocktailPlugin's
                // Add-ingreds option afterwards, not by anything in recipes.json.
                "item.mixed_saturday_9573",
                // Cocktail garnishes. CocktailPlugin consumes these through the shaker's
                // Pour option, so recipes.json alone cannot see where they go.
                "item.lemon_slices",
                "item.lemon_chunks",
                "item.lime_slices",
                "item.lime_chunks",
                "item.orange_slices",
                "item.orange_chunks",
                "item.pineapple_chunks",
                "item.pineapple_ring",
                // The botched kebab. It is edible in OSRS but with odd effects nobody has
                // written down precisely, so it is produced but not wired into Food.
                "item.ugthanki_kebab",
            )

        /** The three burnt items that mark a cook step as gnome cooking. */
        val GNOME_BURNT = setOf("item.burnt_crunchies", "item.burnt_batta", "item.burnt_gnomebowl")

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

    /**
     * The pair the engine actually binds a recipe to - its explicit `bind` when it has one,
     * and otherwise its first two ingredients. Slicing a lemon has only one ingredient, so
     * reaching straight for `ingredients[1]` throws.
     */
    private fun boundPair(recipe: RecipeEntry): Pair<Int, Int> {
        val keys = recipe.bind ?: recipe.ingredients.take(2)
        val a = getRSCM(keys[0])
        val b = getRSCM(keys[1])
        return minOf(a, b) to maxOf(a, b)
    }

    private val recipes: List<RecipeEntry> by lazy {
        Files.newBufferedReader(Paths.get("../data/cfg/cooking/recipes.json")).use {
            Gson().fromJson(it, object : TypeToken<List<RecipeEntry>>() {}.type)
        }
    }

    @Test
    fun `config parses and every raw item appears exactly once`() {
        assertEquals(73, foods.size, "expected 73 cookable foods, got ${foods.map { it.raw }}")

        val duplicated = foods.groupBy { it.raw }.filterValues { it.size > 1 }.keys
        assertTrue(duplicated.isEmpty(), "raw items bound twice would crash bindItemOnObject: $duplicated")
    }

    @Test
    fun `every item key resolves to a real cache item`() {
        foods.forEach { food ->
            listOfNotNull(food.raw, food.cooked, food.burnt, food.returns).forEach { key ->
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
            listOf(
                "item.raw_cod",
                "item.raw_swordfish",
                "item.raw_monkfish",
                "item.uncooked_curry",
                "item.sliced_mushrooms",
            ).sorted(),
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
        // Gnome cooking is all range-only too, and all of it is reached through the three
        // half-baked trays, so it is checked by family rather than listed dish by dish.
        val gnome = foods.filter { it.rangeOnly && it.burnt in GNOME_BURNT }
        assertEquals(
            18,
            gnome.size,
            "three half-baked trays, one cook per dish, and the two heated cocktails",
        )

        val rangeOnly = foods.filter { it.rangeOnly && it.burnt !in GNOME_BURNT }.map { it.cooked }.sorted()
        assertEquals(
            listOf(
                "item.admiral_pie",
                "item.apple_pie",
                "item.botanical_pie",
                "item.bread",
                "item.cake",
                "item.dragonfruit_pie",
                "item.fish_pie",
                "item.garden_pie",
                "item.meat_pie",
                "item.mud_pie",
                "item.mushroom_pie",
                "item.pitta_bread",
                "item.plain_pizza",
                "item.redberry_pie",
                "item.summer_pie",
                "item.wild_pie",
            ),
            rangeOnly,
            "stew and curry cook on a fire; every pie and both breads need a range",
        )
    }

    @Test
    fun `fire-only foods are exactly the spit roasts`() {
        val fireOnly = foods.filter { it.fireOnly }.map { it.cooked }.sorted()
        assertEquals(
            listOf(
                "item.cooked_chompy",
                "item.roast_beast_meat",
                "item.roast_bird_meat",
                "item.roast_rabbit",
            ),
            fireOnly,
            "spit-roasting is fire-only; every wiki recipe block reads facilities = Fire",
        )
        // And every one of them hands the iron spit back.
        foods.filter { it.fireOnly }.forEach {
            assertEquals("item.iron_spit", it.returns, "${it.cooked} should return the spit")
        }
    }

    @Test
    fun `nothing is both range-only and fire-only`() {
        val impossible = foods.filter { it.rangeOnly && it.fireOnly }.map { it.cooked }
        assertTrue(impossible.isEmpty(), "these could never be cooked at all: $impossible")
    }

    @Test
    fun `both routes to an uncooked curry are gated the same`() {
        // The wiki gives level 60 and no experience for the spice route and the
        // curry-leaf route alike; the leaf route wants three leaves, the spice one.
        val curries = recipes.filter { it.product == "item.uncooked_curry" }
        assertEquals(2, curries.size, "spice and curry leaves")
        curries.forEach { assertEquals(60, it.level, "${it.ingredients} should need level 60") }

        val leaves = curries.first { "item.curry_leaf" in it.ingredients }
        assertEquals(listOf(1, 3), leaves.amounts, "three curry leaves, one stew")
        val spice = curries.first { "item.spice" in it.ingredients }
        assertEquals(null, spice.amounts, "one spice, one stew")
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
        assertEquals(122, recipes.size, "expected 122 recipes, got ${recipes.map { it.product }}")

        recipes.forEach { recipe ->
            (
                recipe.ingredients + recipe.product +
                    (recipe.returns ?: emptyList()) + (recipe.tools ?: emptyList())
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
        val grouped = recipes.groupBy { boundPair(it) }
        val multi = grouped.filterValues { it.size > 1 }

        // A shared pair is a choice in the chatbox, not a bug - but every product behind
        // one has to be distinct, or a recipe would be unreachable.
        multi.forEach { (pair, group) ->
            val products = group.map { it.product }
            assertEquals(products.distinct(), products, "pair $pair makes the same thing twice")
        }

        val doughs = multi.values.filter { it.first().ingredients.first() == "item.pot_of_flour" }
        assertEquals(3, doughs.size, "one flour+water pair per water container")
        doughs.forEach { group ->
            assertEquals(
                listOf("item.bread_dough", "item.pastry_dough", "item.pitta_dough", "item.pizza_base"),
                group.map { it.product },
            )
            // Only the pizza base is gated; the three doughs are free to anyone.
            assertEquals(listOf(0, 0, 0, 35), group.map { it.level })
        }

        // The other legitimate choice: equa leaves on a half baked batta could be heading
        // for either a fruit batta or a toad batta.
        val battas = multi.values.filter { it.first().ingredients.first() == "item.half_baked_batta" }
        assertEquals(1, battas.size)
        assertEquals(
            listOf("item.half_made_batta", "item.half_made_batta_9482"),
            battas.single().map { it.product }.sorted(),
        )
    }

    @Test
    fun `every chain ends at something edible`() {
        // The real invariant, and a stronger one than "is used somewhere": follow every
        // recipe and every cook forward, and whatever falls out the end must be food a
        // player can actually eat. This is what catches a cookable item nobody wired into
        // the Food enum - cooked rabbit and anchovies were both in that state.
        val edible = edibleItems()
        val consumed = recipes.flatMap { it.ingredients }.toSet()
        val cookable = foods.map { it.raw }.toSet()

        val terminal = (recipes.map { it.product } + foods.map { it.cooked }).toSet() - consumed - cookable
        val notEdible =
            terminal.filter { it !in edible && it !in TERMINAL_EXEMPTIONS }
        assertTrue(
            notEdible.isEmpty(),
            "these can be made but are neither an ingredient, cookable, nor edible: $notEdible",
        )
    }

    @Test
    fun `every cooked food is edible`() {
        val edible = edibleItems()
        val missing = foods.map { it.cooked }.distinct().filter { it !in edible && it !in TERMINAL_EXEMPTIONS }
        assertTrue(missing.isEmpty(), "cookable but nothing happens when eaten: $missing")
    }

    /**
     * Every item the `Food` enum can consume, read straight out of its source.
     *
     * Parsing the enum rather than depending on it keeps this test honest about the thing
     * that actually matters - what a player can put in their mouth - without the test
     * having to construct a `Player`.
     */
    private fun edibleItems(): Set<String> {
        val source =
            Files.readString(
                Paths.get("src/main/kotlin/org/alter/plugins/content/items/consumables/food/Food.kt"),
            )
        val found =
            Regex("""item\s*=\s*"(item\.[a-z0-9_]+)"""")
                .findAll(source)
                .map { it.groupValues[1] }
                .toSet()
        // Guards against the regex silently matching nothing and every check above
        // passing vacuously.
        assertTrue(found.size > 50, "only parsed ${found.size} edible items out of Food.kt; the regex has drifted")
        return found
    }

    @Test
    fun `the three-part pies use the ingredient order the wiki's instructions give`() {
        fun chain(shellIngredient: String): List<String> {
            val steps = mutableListOf<String>()
            var current = recipes.first { it.ingredients == listOf("item.pie_shell", shellIngredient) }
            steps += current.ingredients[1]
            while (true) {
                val next = recipes.firstOrNull { it.ingredients[0] == current.product } ?: break
                steps += next.ingredients[1]
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
        val doughs = recipes.filter { it.ingredients[0] == "item.pot_of_flour" }
        assertEquals(12, doughs.size, "four products times three water containers")
        doughs.forEach {
            assertEquals("item.pot", it.returns?.get(0), "${it.product} should leave an empty pot")
            assertEquals(2, it.returns?.size, "${it.product} should leave the pot and the water container")
        }
        assertEquals(
            setOf("item.bucket", "item.jug", "item.bowl"),
            doughs.mapNotNull { it.returns?.get(1) }.toSet(),
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
