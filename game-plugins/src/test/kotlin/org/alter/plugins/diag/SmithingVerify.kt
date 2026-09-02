package org.alter.plugins.diag

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager
import org.alter.plugins.content.skills.smithing.BarEntry
import org.alter.plugins.content.skills.smithing.MetalEntry
import org.alter.plugins.content.skills.smithing.SmithCategory
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
 * Verify-before-wire checks for Smithing: both JSON configs parse, every RSCM key resolves,
 * the wiki figures are what actually landed in the config, and the presentation invariant
 * that each category fits in one `produceItemBox` still holds.
 */
class SmithingVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** `produceItemBox` renders at most ten items. */
        const val PRODUCE_BOX_CAPACITY = 10
    }

    private val bars: List<BarEntry> by lazy {
        Files.newBufferedReader(Paths.get("../data/cfg/smithing/bars.json")).use {
            Gson().fromJson(it, object : TypeToken<List<BarEntry>>() {}.type)
        }
    }

    private val metals: List<MetalEntry> by lazy {
        Files.newBufferedReader(Paths.get("../data/cfg/smithing/products.json")).use {
            Gson().fromJson(it, object : TypeToken<List<MetalEntry>>() {}.type)
        }
    }

    @Test
    fun `configs parse with the expected shape`() {
        assertEquals(8, bars.size, "expected 8 smeltable bars, got ${bars.map { it.name }}")
        assertEquals(
            listOf("Bronze", "Iron", "Steel", "Mithril", "Adamant", "Rune"),
            metals.map { it.name },
        )
        metals.forEach { assertEquals(18, it.products.size, "${it.name} should have 18 products") }
    }

    @Test
    fun `every bar and ore key resolves to a real cache item`() {
        bars.forEach { entry ->
            assertNotNull(CacheManager.getItem(getRSCM(entry.bar)), "${entry.bar} has no cache item")
            entry.ingredients.forEach { ing ->
                assertNotNull(CacheManager.getItem(getRSCM(ing.item)), "${ing.item} has no cache item")
            }
        }
    }

    @Test
    fun `every smithable product key resolves to a real cache item`() {
        metals.forEach { metal ->
            assertNotNull(CacheManager.getItem(getRSCM(metal.bar)), "${metal.bar} has no cache item")
            metal.products.forEach { product ->
                assertNotNull(CacheManager.getItem(getRSCM(product.item)), "${product.item} has no cache item")
            }
        }
    }

    /**
     * Gson allocates without running the constructor, so Kotlin defaults and any `init`
     * validation never fire for these JSON-loaded classes - a missing field silently
     * becomes 0/0.0/null. This is the check that actually guards the config.
     */
    @Test
    fun `every parsed value is sane despite gson bypassing the constructor`() {
        bars.forEach { entry ->
            assertTrue(entry.name.isNotBlank(), "bar with blank name")
            assertTrue(entry.level in 1..99, "${entry.name} level=${entry.level}")
            assertTrue(entry.experience > 0.0, "${entry.name} experience=${entry.experience}")
            assertTrue(entry.successChance > 0.0 && entry.successChance <= 1.0, "${entry.name} chance=${entry.successChance}")
            assertTrue(entry.ingredients.isNotEmpty(), "${entry.name} has no ingredients")
            entry.ingredients.forEach { assertTrue(it.amount >= 1, "${entry.name} ingredient amount=${it.amount}") }
        }
        metals.forEach { metal ->
            assertTrue(metal.experiencePerBar > 0.0, "${metal.name} experiencePerBar=${metal.experiencePerBar}")
            metal.products.forEach { p ->
                assertTrue(p.level in 1..99, "${metal.name} ${p.item} level=${p.level}")
                assertTrue(p.bars >= 1, "${metal.name} ${p.item} bars=${p.bars}")
                assertNotNull(p.category, "${metal.name} ${p.item} has no category")
            }
        }
    }

    /**
     * The UI invariant: each category is shown in one `produceItemBox`, which holds ten
     * items. If a future product pushes a category past ten it would be silently
     * unreachable in game, so this fails the build instead.
     */
    @Test
    fun `each category fits in a single produce box`() {
        metals.forEach { metal ->
            SmithCategory.entries.forEach { category ->
                val count = metal.products.count { it.category == category }
                assertTrue(
                    count in 1..PRODUCE_BOX_CAPACITY,
                    "${metal.name} ${category.displayName} has $count products (max $PRODUCE_BOX_CAPACITY)",
                )
            }
        }
    }

    @Test
    fun `products are ordered by ascending level within each metal`() {
        metals.forEach { metal ->
            val levels = metal.products.map { it.level }
            assertEquals(levels.sorted(), levels, "${metal.name} products are not in level order")
        }
    }

    @Test
    fun `no product item appears twice`() {
        val all = metals.flatMap { m -> m.products.map { it.item } }
        val duplicated = all.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(duplicated.isEmpty(), "duplicate smithing products: $duplicated")
    }

    /** Spot-checks against figures quoted directly by the wiki. */
    @Test
    fun `known wiki values landed in the config`() {
        val iron = bars.single { it.name == "Iron bar" }
        assertEquals(0.5, iron.successChance, "iron should fail half the time")
        assertEquals(15, iron.level)

        val steel = bars.single { it.name == "Steel bar" }
        assertEquals(1, steel.ingredients.single { it.item == "item.iron_ore" }.amount)
        assertEquals(2, steel.ingredients.single { it.item == "item.coal" }.amount)

        val runite = bars.single { it.name == "Runite bar" }
        assertEquals(8, runite.ingredients.single { it.item == "item.coal" }.amount)
        assertEquals(85, runite.level)

        val bronze = metals.single { it.name == "Bronze" }
        val bronzePlatebody = bronze.products.single { it.item == "item.bronze_platebody" }
        assertEquals(18, bronzePlatebody.level)
        assertEquals(5, bronzePlatebody.bars)
        // The wiki states this exact example: 5 bars at 12.5 xp each.
        assertEquals(62.5, bronze.experiencePerBar * bronzePlatebody.bars)

        val rune = metals.single { it.name == "Rune" }
        // Rune compresses at the top rather than following the usual offsets.
        listOf("rune_2h_sword", "rune_platelegs", "rune_plateskirt", "rune_platebody").forEach { item ->
            assertEquals(99, rune.products.single { it.item == "item.$item" }.level, "$item should be level 99")
        }
        assertEquals(85, rune.products.single { it.item == "item.rune_dagger" }.level)
    }

    /**
     * Every metal's spear costs one bar and grants exactly double that metal's per-bar
     * rate on the wiki - an independent confirmation of the whole XP ladder.
     */
    @Test
    fun `experience per bar matches the wiki spear rates`() {
        val wikiSpearXp = mapOf(
            "Bronze" to 25.0, "Iron" to 50.0, "Steel" to 75.0,
            "Mithril" to 100.0, "Adamant" to 125.0, "Rune" to 150.0,
        )
        metals.forEach { metal ->
            val expected = wikiSpearXp.getValue(metal.name) / 2.0
            assertEquals(expected, metal.experiencePerBar, "${metal.name} per-bar xp")
        }
    }

    @Test
    fun `furnaces and anvils are findable by the action scan the plugins use`() {
        val objects = CacheManager.getObjects()

        val furnaces = objects.filter { (_, def) ->
            (def.name ?: "").contains("furnace", true) &&
                def.actions.any { it?.equals("Smelt", true) == true }
        }
        assertTrue(furnaces.size >= 10, "only ${furnaces.size} smeltable furnaces found")

        val anvils = objects.filter { (_, def) ->
            (def.name ?: "").contains("anvil", true) &&
                def.actions.any { it?.equals("Smith", true) == true }
        }
        assertTrue(anvils.size >= 5, "only ${anvils.size} smithable anvils found")

        // Barbarian Village's anvil should be reachable by the same scan.
        assertTrue(25349 in anvils.keys, "the Barbarian anvil (25349) was not picked up")
    }

    @Test
    fun `the hammer key resolves`() {
        assertNotNull(CacheManager.getItem(getRSCM("item.hammer")), "item.hammer has no cache item")
    }
}
