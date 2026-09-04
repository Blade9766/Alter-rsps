package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.plugins.content.mechanics.water.WaterContainers
import org.alter.plugins.content.mechanics.water.WaterSources
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for the water-filling mechanic: every RSCM key resolves to a
 * real cache entry, the name scan that replaced `WaterPlugin`'s hardcoded object list
 * still finds everything the list named, and none of the bindings the plugin makes at
 * world-init can crash the server on the way up.
 *
 * That last one is why this test exists rather than being a formality.
 * `PluginRepository.bindItemOnObject` and `bindItemOnItem` both **throw** on a repeated
 * pair, and `onItemOption(item, option: String)` **throws** when the named option is not
 * on the item - all three at boot, taking the server down with them. The plugin generates
 * roughly 900 bindings out of a cache scan, so those properties are asserted here rather
 * than discovered the hard way.
 */
class WaterVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /**
         * The object ids `WaterPlugin` used to name one at a time, through the `Objs`
         * constants class that RSCM replaced (see git history for the commented-out
         * original). The un-suffixed `Objs` names - `FOUNTAIN`, `WELL`, `TAP`,
         * `WATER_TAP`, `WATER_BARREL`, `WATER_PUMP` - are the lowest id carrying that
         * name, which is the convention that generator used.
         *
         * This is the regression baseline: the name scan must still reach every one of
         * them. Deliberately a subset check rather than an equality check, because the
         * scan is *expected* to also find ids the cache has gained since.
         */
        val LEGACY_SOURCES =
            listOf(
                // fountains
                153, 879, 880, 2864, 3641, 5125, 6232, 7143, 10436, 10437, 10827, 11007,
                12941, 22973, 24102, 27536, 39162, 6749, 6750,
                // sinks
                873, 874, 1763, 3014, 4063, 6151, 7422, 8699, 9143, 9684, 10175, 12279,
                12609, 12974, 13563, 13564, 14868, 15678, 16704, 16705, 20358, 22715,
                25729, 25929, 27707, 27708, 28538, 34943, 39393, 39459, 39489, 40023, 22782,
                // barrels
                5598, 5599, 8702, 8703,
                // pumps
                15936, 15937, 15938, 35981, 36078,
                // taps
                34445, 4176, 4285, 4482, 8737, 20794,
                // wells
                878, 884, 3264, 3305, 3359, 3485, 3646, 4004, 4005, 6097, 6249, 6549,
                8747, 8927, 12201, 12897, 24150, 29100, 30930, 35881, 39720,
            )

        /**
         * Objects the scan finds that the old list did not name. Listed explicitly so a
         * cache update that adds a fill point shows up as a failing test to be looked at,
         * rather than silently becoming reachable in game.
         *
         * `Barrel of Water` and the three `Waterpump`s are new *categories* rather than
         * new ids - the old list had no name for either spelling - and are included on
         * purpose. The rest are ids the cache gained after that list was written.
         */
        val EXPECTED_NEW_SOURCES =
            listOf(
                // fountains added since
                42162, 43689, 47950, 52447, 54738, 54739, 55074, 55075,
                // sinks added since
                42205, 47393,
                // "Barrel of Water"
                33308,
                // "Waterpump", and water pumps added since
                3640, 20776, 24004, 41000, 41004, 47926, 48170, 52646,
                // wells added since
                43902, 43903, 46805, 54778,
            )

        /** Mirrors the containers `WaterPlugin` gives an "Empty" option to. */
        val EMPTYABLE =
            WaterContainers.values().filter { it != WaterContainers.CAN && it != WaterContainers.WATERSKIN }
    }

    private val sources: Map<Int, WaterSources> by lazy { WaterSources.scan() }

    @Test
    fun `every container key resolves to a real cache item`() {
        WaterContainers.values().forEach {
            listOf(it.container.unfilledKey, it.container.filledKey).forEach { key ->
                val id = getRSCM(key)
                assertTrue(id > 0, "$key did not resolve")
                assertNotNull(CacheManager.getItem(id), "$key resolved to $id, which is not in the cache")
            }
        }
    }

    @Test
    fun `the toy sink item and object both resolve`() {
        val item = getRSCM("item.sink")
        val obj = getRSCM("object.toy_sink")
        assertEquals("Sink", CacheManager.getItem(item)?.name, "item.sink is not the toy sink item")
        assertEquals("Toy sink", CacheManager.getObject(obj)?.name, "object.toy_sink is not the toy sink object")
        assertTrue(obj in sources, "the toy sink object is not reachable as a water source")
    }

    @Test
    fun `every container has a name the plugin can put in a message`() {
        WaterContainers.values().forEach {
            assertNotNull(
                CacheManager.getItem(getRSCM(it.container.filledKey))?.name,
                "${it.container.filledKey} has no name",
            )
            assertNotNull(
                CacheManager.getItem(getRSCM(it.container.unfilledKey))?.name,
                "${it.container.unfilledKey} has no name",
            )
        }
    }

    /**
     * `onItemOption(item, option = "Empty")` calls `check(option != -1)` at bind time, so
     * a container whose full form has no Empty option would stop the server booting.
     */
    @Test
    fun `every emptyable container really has an Empty option`() {
        EMPTYABLE.forEach {
            val options =
                CacheManager.getItem(getRSCM(it.container.filledKey))?.interfaceOptions?.filterNotNull().orEmpty()
            assertTrue(
                options.any { opt -> opt.equals("Empty", ignoreCase = true) },
                "${it.container.filledKey} has no Empty option [options=$options]",
            )
        }
    }

    /** The converse: the two the plugin skips genuinely have no Empty option to bind. */
    @Test
    fun `the watering can and waterskin are skipped for a reason`() {
        listOf(WaterContainers.CAN, WaterContainers.WATERSKIN).forEach {
            val options =
                CacheManager.getItem(getRSCM(it.container.filledKey))?.interfaceOptions?.filterNotNull().orEmpty()
            assertTrue(
                options.none { opt -> opt.equals("Empty", ignoreCase = true) },
                "${it.container.filledKey} has an Empty option now, so it should no longer be skipped",
            )
        }
    }

    @Test
    fun `the name scan still reaches every object the old hardcoded list named`() {
        val missing = LEGACY_SOURCES.filterNot { it in sources }
        assertTrue(
            missing.isEmpty(),
            "the scan lost water sources the old list had: " +
                missing.joinToString { "$it (${CacheManager.getObject(it)?.name})" },
        )
    }

    @Test
    fun `the scan finds the old list plus only the additions accounted for above`() {
        val unexpected = sources.keys.filterNot { it in LEGACY_SOURCES || it in EXPECTED_NEW_SOURCES }
        assertTrue(
            unexpected.isEmpty(),
            "the scan picked up objects nobody has looked at: " +
                unexpected.joinToString { "$it (${CacheManager.getObject(it)?.name})" },
        )
        assertEquals(LEGACY_SOURCES.size + EXPECTED_NEW_SOURCES.size, sources.size)
    }

    /**
     * The scan matches whole names, and this is what that buys: none of the near misses a
     * `contains` check would have swept in are reachable.
     */
    @Test
    fun `objects that merely sound like water sources are not bound`() {
        val notWater =
            listOf(
                18040 to "Tapestry",
                18597 to "Stairwell",
                4385 to "Catapult",
                1797 to "Waterfall rocks",
                16179 to "Waterlily",
                8683 to "Dead watermelons",
                31942 to "Fountain of Rune",
                37435 to "Swampy sink",
                35882 to "Broken Well",
                3546 to "Old well",
                43 to "Water",
            )
        notWater.forEach { (id, name) ->
            assertEquals(name, CacheManager.getObject(id)?.name, "cache moved: $id is no longer '$name'")
            assertTrue(id !in sources, "$name ($id) is being treated as a water source")
        }
    }

    /**
     * `bindItemOnObject` throws on a repeated (item, object) pair. The scan is keyed by
     * object id so it cannot repeat one, but this asserts the property the plugin actually
     * depends on rather than trusting the implementation to keep having it.
     */
    @Test
    fun `no item on object pair is generated twice`() {
        val pairs = sources.keys.flatMap { obj -> WaterContainers.values().map { obj to it.container.unfilled } }
        assertEquals(pairs.size, pairs.toSet().size, "an item-on-object pair is generated twice")
        assertEquals(sources.size * WaterContainers.values().size, pairs.size)
    }

    /**
     * `bindItemOnItem` hashes a pair as `(max shl 16) or min`, order-insensitively, and
     * throws on a repeat. Reproduced here over every pair `WaterPlugin` binds.
     */
    @Test
    fun `no item on item pair is generated twice`() {
        val toySink = getRSCM("item.sink")
        val pairs = mutableListOf<Pair<Int, Int>>()
        WaterContainers.values().forEach {
            pairs += it.container.filled to it.container.filled
            pairs += it.container.unfilled to it.container.filled
            pairs += toySink to it.container.unfilled
            pairs += toySink to it.container.filled
        }
        pairs += getRSCM("item.bowl_of_hot_water") to getRSCM("item.empty_cup")

        val duplicated =
            pairs
                .withIndex()
                .groupBy { (_, pair) -> (maxOf(pair.first, pair.second) shl 16) or minOf(pair.first, pair.second) }
                .filterValues { it.size > 1 }
                .values
                .map { group -> group.map { it.value } }
        assertTrue(duplicated.isEmpty(), "item-on-item pairs collide, which throws at boot: $duplicated")
    }

    /**
     * The left-click fill actions the plugin now binds.
     *
     * `onObjOption(obj, option)` calls `check(slot != -1)` at bind time, so an action name
     * that has drifted out of the cache would stop the server booting; and `bindObject`
     * throws on a repeated (object, option slot) pair. Both are pinned here, along with the
     * set of sources that carry such an action at all - a cache update that adds one to
     * another source should be looked at rather than becoming reachable silently.
     */
    @Test
    fun `only the known sources carry a left-click fill action`() {
        val withFillAction =
            sources.keys
                .associateWith { id ->
                    CacheManager.getObject(id).actions.filterNotNull().filter { action ->
                        action.equals("Fill-bucket", ignoreCase = true) ||
                            action.equals("Fill-from", ignoreCase = true)
                    }
                }.filterValues { it.isNotEmpty() }

        assertEquals(
            mapOf(
                9143 to listOf("Fill-bucket"),
                41004 to listOf("Fill-bucket"),
                35981 to listOf("Fill-from"),
                36078 to listOf("Fill-from"),
            ),
            withFillAction,
            "the set of water sources carrying a left-click fill action has changed",
        )
    }

    @Test
    fun `no fill action binds the same object option slot twice`() {
        val slots =
            sources.keys.flatMap { id ->
                val actions = CacheManager.getObject(id).actions
                actions
                    .withIndex()
                    .filter { (_, action) ->
                        action != null &&
                            (
                                action.equals("Fill-bucket", ignoreCase = true) ||
                                    action.equals("Fill-from", ignoreCase = true)
                            )
                    }.map { (index, _) -> id to index + 1 }
            }
        assertEquals(slots.size, slots.toSet().size, "a fill action binds the same object option twice")
    }

    /** The hot-water tea chain the plugin ends with, checked end to end. */
    @Test
    fun `the hot water tea keys resolve to the right items`() {
        mapOf(
            "item.bowl_of_hot_water" to "Bowl of hot water",
            "item.empty_cup" to "Empty cup",
            "item.cup_of_hot_water" to "Cup of hot water",
            "item.bowl" to "Bowl",
        ).forEach { (key, name) ->
            assertEquals(name, CacheManager.getItem(getRSCM(key))?.name, "$key is not '$name'")
        }
    }
}
