package org.alter.plugins.diag

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.TileData
import dev.openrune.cache.filestore.loadTerrain
import org.alter.plugins.content.skills.agility.CourseEntry
import org.alter.plugins.content.skills.agility.DestinationMode
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for Agility: the config parses (the data classes' `init` blocks are the
 * real validator), every RSCM key resolves to a real cache object, and - the assumption that
 * silently unbinds an obstacle at runtime - every object actually carries the option the JSON binds.
 */
class AgilityVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    /** Terrain per map region, decoded once. */
    private val terrain = HashMap<Int, Array<Array<Array<TileData>>>?>()

    /**
     * True when `[x, z, height]` has a floor and is not flagged BLOCK_WALK. Rooftop course walkways
     * are exactly the unblocked tiles at their height, which is what makes this a real check.
     */
    private fun isWalkable(tile: List<Int>): Boolean {
        val (x, z, height) = tile
        val rx = x shr 6
        val rz = z shr 6
        val tiles = terrain.getOrPut((rx shl 8) or rz) {
            CacheManager.cache.data(MAPS, "m${rx}_$rz")?.let { loadTerrain(it) }
        } ?: return false

        val data = tiles[height][x - (rx shl 6)][z - (rz shl 6)]
        val blocked = (data.settings.toInt() and 0x1) != 0
        val hasFloor = data.overlayId.toInt() != 0 || data.underlayId.toInt() != 0
        return !blocked && hasFloor
    }

    private val courses: List<CourseEntry> by lazy {
        Files.newBufferedReader(Paths.get("../data/cfg/agility/courses.json")).use { reader ->
            Gson().fromJson(reader, object : TypeToken<List<CourseEntry>>() {}.type)
        }
    }

    @Test
    fun `config parses and every course has obstacles`() {
        assertTrue(courses.isNotEmpty(), "No agility courses loaded.")
        courses.forEach { course ->
            assertTrue(course.obstacles.isNotEmpty(), "${course.name} has no obstacles.")
        }
    }

    @Test
    fun `every object resolves and carries the bound option`() {
        courses.forEach { course ->
            course.obstacles.forEach { obstacle ->
                obstacle.objects.forEach { rscm ->
                    val id = getRSCM(rscm)
                    assertTrue(id > 0, "$rscm did not resolve to an object id.")

                    val def = CacheManager.getObject(id)
                    assertNotNull(def.name, "$rscm ($id) has no name in the cache.")

                    val options = def.actions.filterNotNull()
                    assertTrue(
                        options.any { it.equals(obstacle.option, ignoreCase = true) },
                        "${course.name} / ${obstacle.name}: $rscm ($id, '${def.name}') has options " +
                            "$options, which does not include '${obstacle.option}'.",
                    )
                }
            }
        }
    }

    @Test
    fun `span and tile obstacles carry the tiles they need`() {
        courses.forEach { course ->
            course.obstacles.forEach { obstacle ->
                when (obstacle.destination) {
                    DestinationMode.SPAN -> {
                        assertEquals(3, obstacle.start?.size, "${obstacle.name} start tile")
                        assertEquals(3, obstacle.end?.size, "${obstacle.name} end tile")
                    }
                    DestinationMode.TILE -> assertEquals(3, obstacle.end?.size, "${obstacle.name} end tile")
                    DestinationMode.THROUGH -> assertTrue(obstacle.distance >= 1, "${obstacle.name} distance")
                }
            }
        }
    }

    /**
     * The authored landing tiles are the part of the config the wiki cannot confirm, so check them
     * against the cache's own terrain: a tile the player is dropped onto must have a floor and must
     * not be flagged BLOCK_WALK. This is what catches a landing tile authored one column off.
     *
     * [DestinationMode.THROUGH] landings depend on where the player approached from, so only the
     * fixed tiles - SPAN endpoints, TILE destinations, fall tiles and mark spawns - are checked.
     */
    @Test
    fun `every fixed landing tile is walkable floor in the cache`() {
        // Negative control, so a green run below cannot be vacuous: the tile Draynor's Gap object
        // sits on is off the walkway, and the tile east of it is the void between two roofs.
        assertFalse(isWalkable(listOf(3095, 3255, 3)), "Gap object tile should read as blocked.")
        assertFalse(isWalkable(listOf(3096, 3255, 3)), "The void east of the gap should read as blocked.")

        courses.forEach { course ->
            course.obstacles.forEach { obstacle ->
                val fixed = buildList {
                    when (obstacle.destination) {
                        DestinationMode.SPAN -> {
                            add("${obstacle.name} start" to obstacle.start!!)
                            add("${obstacle.name} end" to obstacle.end!!)
                        }
                        DestinationMode.TILE -> add("${obstacle.name} end" to obstacle.end!!)
                        DestinationMode.THROUGH -> Unit
                    }
                    obstacle.fail?.let { add("${obstacle.name} fail tile" to it.tile) }
                }
                fixed.forEach { (label, tile) ->
                    assertTrue(isWalkable(tile), "${course.name}: $label ${tile} is not walkable floor.")
                }
            }

            course.markOfGrace?.tiles?.forEachIndexed { i, tile ->
                assertTrue(isWalkable(tile), "${course.name}: mark spawn #$i ${tile} is not walkable floor.")
            }
        }
    }

    @Test
    fun `draynor rooftop matches the wiki experience table`() {
        val course = courses.single { it.name == "Draynor Village Rooftop Course" }

        assertEquals(
            listOf("Rough wall", "Tightrope", "Tightrope", "Narrow wall", "Wall", "Gap", "Crate"),
            course.obstacles.map { it.name },
        )
        assertEquals(
            listOf(5.0, 8.0, 7.0, 7.0, 10.0, 4.0, 79.0),
            course.obstacles.map { it.experience },
        )

        val perLap = course.obstacles.sumOf { it.experience } + course.lapExperience
        assertEquals(120.0, perLap, "Draynor Village is 120 xp per lap.")

        val marks = assertNotNull(course.markOfGrace, "Draynor is a rooftop course and awards marks.")
        assertEquals(2, marks.chance)
        assertEquals(6, marks.outOf)
        assertEquals(11849, getRSCM(marks.item), "Mark of grace item id.")
    }

    @Test
    fun `al kharid rooftop matches the wiki experience table`() {
        val course = courses.single { it.name == "Al Kharid Rooftop Course" }

        assertEquals(20, course.level, "Al Kharid requires 20 Agility.")
        assertEquals(
            listOf("Rough wall", "Tightrope 1", "Cable", "Zip line", "Tropical tree", "Roof top beams", "Tightrope 2", "Gap"),
            course.obstacles.map { it.name },
        )
        assertEquals(
            listOf(12.0, 36.0, 48.0, 48.0, 12.0, 6.0, 18.0, 36.0),
            course.obstacles.map { it.experience },
        )

        val perLap = course.obstacles.sumOf { it.experience } + course.lapExperience
        assertEquals(216.0, perLap, "Al Kharid is 216 xp per lap.")

        // The wiki names exactly these two as failable, for 1-5 damage each.
        assertEquals(
            listOf("Tightrope 1", "Zip line"),
            course.obstacles.filter { it.fail != null }.map { it.name },
        )
        course.obstacles.mapNotNull { it.fail }.forEach { fail ->
            assertEquals(1, fail.minDamage)
            assertEquals(5, fail.maxDamage)
        }
    }

    @Test
    fun `fail chance falls off with level and never goes negative`() {
        val fail = courses.single { it.name == "Al Kharid Rooftop Course" }
            .obstacles.single { it.name == "Tightrope 1" }
            .fail!!

        assertEquals(25, fail.chanceAt(20, 20), "Riskiest at the obstacle's own level.")
        assertEquals(12, fail.chanceAt(40, 20), "Roughly half way to the safe level.")
        assertEquals(0, fail.chanceAt(60, 20), "Safe at the configured level.")
        assertEquals(0, fail.chanceAt(99, 20), "Still safe above it.")
        // A player below the requirement cannot reach the obstacle, but the maths must not blow up.
        assertEquals(25, fail.chanceAt(1, 20))
    }

    @Test
    fun `gnome stronghold matches the wiki experience table`() {
        val course = courses.single { it.name == "Gnome Stronghold Agility Course" }

        assertEquals(
            listOf("Log balance", "Obstacle net", "Tree branch", "Balancing rope", "Tree branch", "Obstacle net", "Obstacle pipe"),
            course.obstacles.map { it.name },
        )
        assertEquals(
            listOf(10.0, 10.0, 6.5, 10.0, 6.5, 10.0, 7.5),
            course.obstacles.map { it.experience },
        )
        assertEquals(50.0, course.lapExperience)

        val perLap = course.obstacles.sumOf { it.experience } + course.lapExperience
        assertEquals(110.5, perLap, "Gnome Stronghold is 110.5 xp per lap.")
    }
}
