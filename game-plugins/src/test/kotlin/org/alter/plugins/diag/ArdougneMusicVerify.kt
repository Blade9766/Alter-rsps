package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.game.model.Tile
import org.alter.plugins.content.mechanics.music.MusicTracks
import org.alter.plugins.content.mechanics.music.MusicZones
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Checks the East Ardougne music zones against the cache's own master track table, the same
 * way the Falador and Edgeville music tests do.
 *
 * The global "no two zones claim the same tile" invariant lives in `FaladorMusicVerify` and
 * covers these automatically - which matters here, because The Tower's polygon runs a
 * two-tile strip down Knightly's edge and its underground area has a notch cut out of it.
 */
class ArdougneMusicVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    /** Track id -> the display name and unlock hint the wiki gives for it. */
    private val tracks = mapOf(
        191 to "Knightly",
        99 to "Baroque",
        133 to "The Tower",
    )

    @Test
    fun `every ardougne track id is really that track in this cache`() {
        tracks.forEach { (id, name) ->
            val entry = MusicTracks.byId(id)
            assertNotNull(entry, "music id $id ($name) is not in this cache's track table")
            assertEquals(name, entry.name, "music id $id is '${entry.name}' in this cache, not '$name'")
        }
    }

    /**
     * **The three do not share a hint in this cache, and that is the point.** The wiki's
     * current infobox gives all three "This track unlocks in East Ardougne.", which would
     * imply one shared genre unlock. This cache says otherwise - three distinct hints, each
     * naming the area its own classic polygon covers. That is why these zones carry no
     * `alsoUnlock`: they unlock area by area. If a future cache dump collapses these to one
     * string, revisit that decision.
     */
    @Test
    fun `each track has its own distinct hint naming its own area`() {
        val expected = mapOf(
            191 to "East Ardougne Castle",
            99 to "in Ardougne",
            133 to "north-west of East Ardougne",
        )

        expected.forEach { (id, fragment) ->
            val entry = MusicTracks.byId(id)!!
            assertTrue(
                entry.hint.contains(fragment, ignoreCase = true),
                "${tracks[id]} (id $id) cache hint is '${entry.hint}', expected it to mention '$fragment'",
            )
        }

        val hints = expected.keys.map { MusicTracks.byId(it)!!.hint }
        assertEquals(hints.size, hints.distinct().size, "expected three distinct hints, got $hints")
    }

    /** Following from the above: entering one Ardougne area must not unlock the other two. */
    @Test
    fun `ardougne tracks unlock area by area, not as a genre`() {
        listOf(Tile(2614, 3293), Tile(2653, 3294), Tile(2590, 3360)).forEach { tile ->
            val zone = MusicZones.lookup(tile)!!
            assertTrue(
                zone.alsoUnlock.isEmpty(),
                "'${zone.name}' unlocks ${zone.alsoUnlock} alongside its own track",
            )
        }
    }

    @Test
    fun `real ardougne locations resolve to the expected track`() {
        // Coordinates this repo actually spawns content on, from areas/ardougne,
        // content/npcs/ardougne and the Ardougne city guards.
        val expectations = listOf(
            Triple("Aemad's Adventuring Supplies", Tile(2614, 3293), 191), // Knightly
            Triple("Paladin, castle ground floor", Tile(2571, 3307), 191),
            Triple("Paladin, castle first floor", Tile(2576, 3293, 1), 191),
            Triple("Knight of Ardougne (west)", Tile(2582, 3297), 191),
            Triple("Zenesha's Plate Mail Body Shop", Tile(2653, 3294), 99), // Baroque
            Triple("Ardougne fur trader", Tile(2664, 3296), 99),
            Triple("Chisel spawn, Cromperty's house", Tile(2683, 3318), 99),
            Triple("Paladin, market", Tile(2653, 3315), 99),
            Triple("Ardougne guard (market)", Tile(2663, 3301), 99),
        )

        expectations.forEach { (label, tile, expectedTrack) ->
            val zone = MusicZones.lookup(tile)
            assertNotNull(zone, "$label at (${tile.x}, ${tile.z}) matches no music zone")
            assertEquals(
                listOf(expectedTrack),
                zone.trackIds,
                "$label at (${tile.x}, ${tile.z}) resolved to zone '${zone.name}'",
            )
        }
    }

    /**
     * The Tower takes the band north of the castle, including the two-tile strip that runs
     * down Knightly's western edge (2558-2559, 3328-3335). Squaring that strip off would
     * have overlapped Knightly, so it is reproduced exactly - worth pinning.
     */
    @Test
    fun `the tower covers north of the castle including its western strip`() {
        listOf(Tile(2590, 3360), Tile(2560, 3330), Tile(2558, 3330), Tile(2559, 3335)).forEach { tile ->
            val zone = MusicZones.lookup(tile)
            assertNotNull(zone, "(${tile.x}, ${tile.z}) matches no music zone")
            assertEquals(listOf(133), zone.trackIds, "(${tile.x}, ${tile.z}) resolved to '${zone.name}'")
        }

        // Just below the strip is Knightly, not The Tower.
        assertEquals(listOf(191), MusicZones.lookup(Tile(2558, 3327))!!.trackIds)
    }

    /**
     * The Tower's underground polygon has a notch cut out of its south-west corner, which the
     * three rectangles reproduce. Inside the notch there should be no Ardougne zone at all.
     */
    @Test
    fun `the underground notch is genuinely excluded`() {
        // Inside the covered parts.
        assertEquals(listOf(133), MusicZones.lookup(Tile(2510, 9740))!!.trackIds, "west column")
        assertEquals(listOf(133), MusicZones.lookup(Tile(2600, 9780))!!.trackIds, "north strip")
        assertEquals(listOf(133), MusicZones.lookup(Tile(2600, 9740))!!.trackIds, "south-east block")
        // Knightly's own underground area, just south of it.
        assertEquals(listOf(191), MusicZones.lookup(Tile(2570, 9700))!!.trackIds, "Knightly underground")

        // The notch itself.
        val notch = MusicZones.lookup(Tile(2540, 9740))
        assertTrue(
            notch == null || notch.trackIds !in listOf(listOf(133), listOf(191)),
            "the underground notch resolved to an Ardougne zone ('${notch?.name}')",
        )
    }

    /**
     * The city's north-east corner - above z=3327 and east of x=2623, where three Ardougne
     * guards stand - used to be left deliberately unzoned, because the three East Ardougne
     * classic polygons stop short of it and the modern unified polygon that does cover it
     * never says which of the three plays there.
     *
     * A later region audit answered it: the corner belongs to none of them. It is
     * Wonderous's territory, and Wonderous's own classic polygon (2624,3328 -> 2688,3392)
     * is exactly this region, with the unlockdetail "Unlocked near the north of East
     * Ardougne." So the corner is now zoned - but to Wonderous, and it must not be
     * absorbed into an East Ardougne track.
     */
    @Test
    fun `the north-east corner belongs to Wonderous, not to an Ardougne track`() {
        val guardTile = Tile(2636, 3340)
        val zone = MusicZones.lookup(guardTile)
        assertNotNull(zone, "north-east Ardougne matches no music zone")
        assertEquals(listOf(81), zone.trackIds, "north-east Ardougne resolved to '${zone.name}'")
        listOf(191, 99, 133).forEach { ardougneTrack ->
            assertTrue(
                ardougneTrack !in zone.trackIds + zone.alsoUnlock,
                "'${zone.name}' should not play or unlock East Ardougne track $ardougneTrack",
            )
        }
    }
}
