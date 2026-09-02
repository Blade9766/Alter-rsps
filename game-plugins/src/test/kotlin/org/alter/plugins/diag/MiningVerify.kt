package org.alter.plugins.diag

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager
import org.alter.plugins.content.skills.mining.RockEntry
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
 * Verify-before-wire checks for the Mining skill: the JSON config parses, every RSCM key
 * in it resolves, every configured rock really carries a "Mine" action in this cache, and
 * every rock's model has a depleted-rock counterpart (or is a known exception).
 */
class MiningVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** Rock model -> grey depleted "Rocks" object, mirroring MiningPlugin. */
        val DEPLETED_BY_MODEL = mapOf(1388 to 8830, 1390 to 8828, 1391 to 8829)

        /** The newer Prifddinas rock model, which has no depleted counterpart in cache. */
        const val PRIFDDINAS_ROCK_MODEL = 37841
    }

    private val entries: List<RockEntry> by lazy {
        Files.newBufferedReader(Paths.get("../data/cfg/mining/rocks.json")).use { reader ->
            Gson().fromJson(reader, object : TypeToken<List<RockEntry>>() {}.type)
        }
    }

    @Test
    fun `config parses and covers the ten core ores`() {
        assertEquals(10, entries.size, "expected 10 rock entries, got ${entries.map { it.name }}")
        assertEquals(
            listOf("clay", "copper", "tin", "iron", "silver", "coal", "gold", "mithril", "adamantite", "runite"),
            entries.map { it.name },
        )
    }

    @Test
    fun `every ore item key resolves`() {
        entries.forEach { entry ->
            val id = getRSCM(entry.ore)
            assertNotNull(CacheManager.getItem(id), "${entry.ore} -> $id has no cache item")
        }
    }

    @Test
    fun `every pickaxe key resolves`() {
        listOf(
            "item.bronze_pickaxe", "item.iron_pickaxe", "item.steel_pickaxe", "item.black_pickaxe",
            "item.mithril_pickaxe", "item.adamant_pickaxe", "item.rune_pickaxe", "item.dragon_pickaxe",
        ).forEach { key ->
            assertNotNull(CacheManager.getItem(getRSCM(key)), "$key has no cache item")
        }
    }

    @Test
    fun `every configured rock really has a Mine action`() {
        entries.forEach { entry ->
            entry.objects.forEach { key ->
                val id = getRSCM(key)
                val def = CacheManager.getObjects()[id]
                assertNotNull(def, "$key -> $id has no cache object")
                assertTrue(
                    def.actions.any { it?.equals("Mine", ignoreCase = true) == true },
                    "$key -> $id has no Mine action [actions=${def.actions.toList()}]",
                )
            }
        }
    }

    @Test
    fun `every configured rock is named for the ore it yields`() {
        entries.forEach { entry ->
            entry.objects.forEach { key ->
                val def = CacheManager.getObjects()[getRSCM(key)]!!
                val name = def.name?.lowercase() ?: ""
                val expected = if (entry.name == "clay") "clay" else entry.name
                assertTrue(
                    name.startsWith(expected),
                    "${entry.name} entry lists $key but the cache calls it '${def.name}'",
                )
            }
        }
    }

    @Test
    fun `every rock model has a depleted counterpart or is a known exception`() {
        entries.forEach { entry ->
            entry.objects.forEach { key ->
                val def = CacheManager.getObjects()[getRSCM(key)]!!
                val model = def.objectModels?.firstOrNull()
                assertNotNull(model, "$key has no model")
                assertTrue(
                    model in DEPLETED_BY_MODEL || model == PRIFDDINAS_ROCK_MODEL,
                    "$key uses unmapped rock model $model - MiningPlugin would leave it with no depleted rock",
                )
            }
        }
    }

    @Test
    fun `the depleted rock objects exist, share their rock's model and are not mineable`() {
        DEPLETED_BY_MODEL.forEach { (model, depletedId) ->
            val def = CacheManager.getObjects()[depletedId]
            assertNotNull(def, "depleted rock $depletedId missing from cache")
            assertEquals(listOf(model), def.objectModels?.toList(), "depleted rock $depletedId model mismatch")
            assertTrue(
                def.actions.none { it?.equals("Mine", ignoreCase = true) == true },
                "depleted rock $depletedId is mineable [actions=${def.actions.toList()}]",
            )
            assertEquals(depletedId, getRSCM("object.rocks_$depletedId"), "rscm key mismatch for $depletedId")
        }
    }

    /**
     * Gson allocates without running the constructor, so a `RockEntry`'s Kotlin default
     * values and its `init { require(...) }` validation never fire for JSON-loaded
     * entries - a field missing from `rocks.json` silently becomes 0.0/0 rather than
     * defaulting or throwing. That makes this the check that actually guards the config.
     */
    @Test
    fun `every parsed entry has sane values despite gson bypassing the constructor`() {
        entries.forEach { entry ->
            assertTrue(entry.name.isNotBlank(), "entry with blank name")
            assertTrue(entry.objects.isNotEmpty(), "${entry.name} has no objects")
            assertTrue(entry.respawnTicks >= 1, "${entry.name} respawnTicks=${entry.respawnTicks}")
            assertTrue(entry.level in 1..99, "${entry.name} level=${entry.level}")
            assertTrue(entry.experience > 0.0, "${entry.name} experience=${entry.experience}")
            assertTrue(entry.baseChance > 0.0 && entry.baseChance <= 1.0, "${entry.name} baseChance=${entry.baseChance}")
            assertTrue(
                entry.maxChance >= entry.baseChance && entry.maxChance <= 1.0,
                "${entry.name} maxChance=${entry.maxChance} vs baseChance=${entry.baseChance}",
            )
        }
    }

    @Test
    fun `no rock object is claimed by two ores`() {
        val all = entries.flatMap { entry -> entry.objects.map { it to entry.name } }
        val duplicated = all.groupBy { it.first }.filterValues { it.size > 1 }
        assertTrue(duplicated.isEmpty(), "rock objects claimed by more than one ore: $duplicated")
    }
}
