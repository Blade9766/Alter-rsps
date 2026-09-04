package org.alter.plugins.diag

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.loadLocations
import org.alter.plugins.content.skills.prayer.OfferingAction
import org.alter.plugins.content.skills.prayer.OfferingEntry
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
 * Verify-before-wire checks for Prayer: the offerings config parses, every RSCM key in it
 * resolves, every entry carries exactly one of the two cache options the plugin binds, the
 * altar scan cannot double-bind an option index, and the Chaos Temple altar really sits on
 * the tile [org.alter.plugins.content.skills.prayer.AltarPlugin] gates its offering on.
 *
 * The last two are the ones that would fail silently at runtime: a double bind throws in
 * the plugin constructor, which registers nothing at all, and a wrong tile would leave the
 * only bone-offering altar in the game answering "You fear the wrath of the gods!".
 */
class PrayerVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** Object id and tile of the Chaos Temple (church) altar, level 38 Wilderness. */
        const val CHAOS_TEMPLE_ALTAR_ID = 411
        val CHAOS_TEMPLE_ALTAR_TILE = Triple(2947, 3820, 0)

        /**
         * Items the cache gives a Bury/Scatter option that this skill deliberately leaves
         * out - quest and minigame bones whose experience the wiki does not publish, plus
         * the Hopespear's Will bones, which only pay when buried in Yu'biusk.
         */
        val EXCLUDED =
            setOf(
                2530, 3187, 24655, 25199, // duplicate "Bones" items belonging to quests/minigames
                3127, 3128, 3129, 3130, 3131, 3132, 3133, // the Zogre Flesh Eaters jogre-bone cooking chain
                610, 4488, // Zadimus corpse, Corpse of woman
                26589, 26590, 26591, 26592, 26593, // Hopespear's Will boss bones
            )
    }

    private val entries: List<OfferingEntry> by lazy {
        Files.newBufferedReader(Paths.get("../data/cfg/prayer/offerings.json")).use { reader ->
            Gson().fromJson(reader, object : TypeToken<List<OfferingEntry>>() {}.type)
        }
    }

    @Test
    fun `config parses with sane levels and experience`() {
        assertEquals(36, entries.size, "expected 36 offerings, got ${entries.size}")
        entries.forEach { entry ->
            assertTrue(entry.experience > 0.0, "${entry.name} pays no experience")
            assertTrue(entry.levelRequired in 1..99, "${entry.name} has level ${entry.levelRequired}")
        }
        // The one requirement above level 1 in the whole skill.
        assertEquals(
            listOf("Superior dragon bones" to 70),
            entries.filter { it.levelRequired > 1 }.map { it.name to it.levelRequired },
        )
    }

    @Test
    fun `every item key resolves and carries exactly one offering option`() {
        entries.forEach { entry ->
            val id = getRSCM(entry.item)
            val def = CacheManager.getItems()[id]
            assertNotNull(def, "${entry.item} -> $id has no cache item")

            val options = def.interfaceOptions.filterNotNull().filter { it.isNotBlank() }
            val actions = OfferingAction.values.filter { action -> options.any { it.equals(action.option, true) } }
            assertEquals(
                1,
                actions.size,
                "${entry.name} should carry one of ${OfferingAction.values.map { it.option }}, cache gives $options",
            )
        }
    }

    @Test
    fun `no two entries share an item`() {
        val duplicates = entries.groupBy { getRSCM(it.item) }.filterValues { it.size > 1 }
        assertTrue(duplicates.isEmpty(), "duplicate offering items: ${duplicates.values.map { g -> g.map { it.name } }}")
    }

    @Test
    fun `every buryable item in the cache is either configured or deliberately excluded`() {
        val configured = entries.map { getRSCM(it.item) }.toSet()
        val missed =
            CacheManager.getItems().filter { (id, def) ->
                if (id in configured || id in EXCLUDED) {
                    false
                } else {
                    def.interfaceOptions.filterNotNull().any { opt ->
                        OfferingAction.values.any { opt.equals(it.option, true) }
                    }
                }
            }.map { (id, def) -> "$id ${def.name}" }

        assertTrue(missed.isEmpty(), "buryable/scatterable items neither configured nor excluded: $missed")
    }

    /**
     * The altar scan takes the first of "Pray-at"/"Pray" it finds. If any object carried
     * both, the other would be silently unreachable - and if a future cache gave one object
     * the same option twice, `bindObject` would throw and unregister the plugin.
     */
    @Test
    fun `no altar object carries more than one pray option`() {
        CacheManager.getObjects().forEach { (id, def) ->
            val pray =
                def.actions.filterNotNull().filter { it.equals("Pray-at", true) || it.equals("Pray", true) }
            assertTrue(pray.size <= 1, "object $id (${def.name}) has multiple pray options: $pray")
        }
    }

    /**
     * Bones are bound onto every object named "altar" that the recharge scan also matches,
     * so those bindings must not collide with each other - one lambda per (item, object)
     * pair, and `bindItemOnObject` throws on a repeat.
     */
    @Test
    fun `altar objects that take bones are distinct ids`() {
        val altars =
            CacheManager.getObjects().filter { (_, def) ->
                def.name?.contains("altar", ignoreCase = true) == true
            }.keys
        assertEquals(altars.size, altars.toSet().size, "duplicate altar object ids in the scan")
        assertTrue(CHAOS_TEMPLE_ALTAR_ID in altars, "the Chaos Temple altar would not be bound for offerings")
    }

    /** The tile AltarPlugin gates the 350% offering on really holds the Chaos Temple altar. */
    @Test
    fun `the chaos temple altar sits where the plugin expects it`() {
        val (x, z, height) = CHAOS_TEMPLE_ALTAR_TILE
        val rx = x shr 6
        val rz = z shr 6
        val land = CacheManager.cache.data(MAPS, "l${rx}_$rz")
        assertNotNull(land, "region ${(rx shl 8) or rz} has no loc data")

        var found = false
        loadLocations(land) { loc ->
            if (loc.id == CHAOS_TEMPLE_ALTAR_ID &&
                (rx shl 6) + loc.localX == x &&
                (rz shl 6) + loc.localY == z &&
                loc.height == height
            ) {
                found = true
            }
        }
        assertTrue(found, "no object $CHAOS_TEMPLE_ALTAR_ID at ($x, $z, $height)")

        val def = CacheManager.getObject(CHAOS_TEMPLE_ALTAR_ID)
        assertTrue(
            def.actions.any { it?.equals("Pray-at", true) == true },
            "the Chaos Temple altar lost its Pray-at option [actions=${def.actions.toList()}]",
        )
    }
}
