package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.loadLocations
import org.alter.plugins.content.mechanics.dairy.Cowbells
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for `mechanics/dairy/DairyCowPlugin`.
 *
 * The plugin finds its cows by scanning the cache for objects named "dairy cow" carrying a
 * "Milk" action, and binds two things to each: the Milk option and a bucket used on it.
 * `onObjOption(obj, option)` calls `check(slot != -1)` at bind time and `bindObject` throws
 * on a repeated (object, option slot) pair - both at boot, taking the server down - so the
 * scan's shape is asserted here rather than discovered on the way up.
 */
class DairyCowVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** Mirrors `DairyCowPlugin.MILKABLE_NAMES`. */
        val MILKABLE_NAMES = setOf("dairy cow")
    }

    private val cows: Map<Int, String> by lazy {
        CacheManager
            .getObjects()
            .filter { (_, def) ->
                def.name?.lowercase() in MILKABLE_NAMES &&
                    def.actions.filterNotNull().any { it.equals("Milk", ignoreCase = true) }
            }.mapValues { (_, def) -> def.name!! }
    }

    @Test
    fun `the scan finds exactly the two dairy cows`() {
        assertEquals(mapOf(8689 to "Dairy cow", 12111 to "Dairy Cow"), cows)
    }

    /**
     * The one object the name filter deliberately keeps out. It carries a Milk action, but
     * nothing says a buffalo fills the same bucket of milk, so a name scan should not decide
     * that on its own - if this ever stops being a buffalo, the filter needs revisiting.
     */
    @Test
    fun `the dairy buffalo is left out on purpose`() {
        val def = CacheManager.getObject(52576)
        assertTrue(
            def.actions.filterNotNull().any { it.equals("Milk", ignoreCase = true) },
            "52576 no longer has a Milk action, so the exclusion no longer means anything",
        )
        assertTrue(52576 !in cows, "the dairy buffalo is being treated as a dairy cow")
    }

    @Test
    fun `the bucket keys resolve to the right items`() {
        assertEquals("Bucket", CacheManager.getItem(getRSCM("item.bucket"))?.name)
        assertEquals("Bucket of milk", CacheManager.getItem(getRSCM("item.bucket_of_milk"))?.name)
    }

    @Test
    fun `no cow binds the same option slot or item-on-object pair twice`() {
        val optionSlots =
            cows.keys.flatMap { id ->
                CacheManager
                    .getObject(id)
                    .actions
                    .withIndex()
                    .filter { (_, action) -> action != null && action.equals("Milk", ignoreCase = true) }
                    .map { (index, _) -> id to index + 1 }
            }
        assertEquals(optionSlots.size, optionSlots.toSet().size, "a cow binds the same Milk option twice")
        assertEquals(cows.size, optionSlots.size, "a cow carries more than one Milk action")

        val pairs = cows.keys.map { it to getRSCM("item.bucket") }
        assertEquals(pairs.size, pairs.toSet().size, "a cow binds the bucket item-on-object pair twice")
    }

    /**
     * Steal-cowbell, the option that did nothing before it was bound.
     *
     * Asserted against the cache rather than a literal because the binding is built from the
     * cache's own spelling: if the action is ever renamed or dropped, the plugin silently binds
     * nothing and this is what says so.
     */
    @Test
    fun `both dairy cows carry Steal-cowbell`() {
        cows.keys.forEach { id ->
            val actions = CacheManager.getObject(id).actions.filterNotNull()
            assertTrue(
                actions.any { it.equals("Steal-cowbell", ignoreCase = true) },
                "$id ('${CacheManager.getObject(id).name}') no longer has a Steal-cowbell action; it has $actions",
            )
        }
    }

    @Test
    fun `the cowbells key resolves to the right item`() {
        assertEquals("Cowbells", CacheManager.getItem(getRSCM("item.cowbells"))?.name)
    }

    /**
     * The published success curve, checked at the three points the sources actually state:
     * Mod Ash's 50% at level 1 and 78% at 99, and the wiki's "around 54%" at the level 15
     * requirement. Anything that drifts these has changed the odds.
     */
    @Test
    fun `the cowbell success curve matches the published figures`() {
        assertEquals(0.502, Cowbells.successChance(1), 0.001, "50% at level 1")
        assertEquals(0.542, Cowbells.successChance(Cowbells.LEVEL), 0.001, "around 54% at level 15")
        assertEquals(0.784, Cowbells.successChance(99), 0.001, "78% at level 99")
        // Clamped, not extrapolated - a boost past 99 must not push past the published end.
        assertEquals(Cowbells.successChance(99), Cowbells.successChance(120), 0.0)
    }

    /**
     * Every dairy cow the wiki publishes, checked against the objects this cache actually
     * places. Dairy cows are scenery baked into the map files, so nothing spawns them - which
     * means the only thing that can go wrong is one not being there, and nothing would say so.
     *
     * The Zanaris cow is the one pin that does not match exactly: the wiki says (2436, 4451)
     * and the cache places it at (2435, 4450). The object is 1x2, so a pin taken from the other
     * corner is the likely explanation; it is allowed within a tile rather than dropped, and
     * every other pin is exact.
     */
    @Test
    fun `every published dairy cow is in this cache's map`() {
        val published =
            listOf(
                "Lumbridge East farm" to (3254 to 3272),
                "Lumbridge East farm" to (3252 to 3275),
                "Lumbridge West farm" to (3172 to 3317),
                "South Falador Farm" to (3040 to 3301),
                "Crafting Guild" to (2923 to 3285),
                "Crafting Guild" to (2920 to 3291),
                "Sinclair Mansion" to (2739 to 3560),
                "Ardougne Farm" to (2674 to 3351),
                "West of Nightmare Zone" to (2585 to 3122),
                "Rellekka" to (2678 to 3670),
                "Gwenith" to (2197 to 3419),
                "North of Hosidius Town square" to (1765 to 3642),
                "South-east of the Farming Guild" to (1305 to 3719),
                "Gordon and Mary's farm" to (1265 to 3688),
                "Zanaris (East of Puro-Puro)" to (2436 to 4451),
            )

        val placed = HashSet<Pair<Int, Int>>()
        published.map { (_, t) -> (t.first shr 6) to (t.second shr 6) }.distinct().forEach { (rx, rz) ->
            val data = CacheManager.cache.data(MAPS, "l${rx}_$rz")
            assertNotNull(data, "map region l${rx}_$rz is missing from the cache")
            loadLocations(data!!) { loc ->
                if (loc.id in cows.keys && loc.height == 0) {
                    placed += ((rx shl 6) + loc.localX) to ((rz shl 6) + loc.localY)
                }
            }
        }

        val missing =
            published.filterNot { (_, pin) ->
                placed.any { kotlin.math.abs(it.first - pin.first) <= 1 && kotlin.math.abs(it.second - pin.second) <= 1 }
            }
        assertTrue(missing.isEmpty(), "Published dairy cows with no object in the cache: $missing")
        assertEquals(published.size, placed.size, "the cache places a different number of dairy cows than the wiki lists")
    }
}
