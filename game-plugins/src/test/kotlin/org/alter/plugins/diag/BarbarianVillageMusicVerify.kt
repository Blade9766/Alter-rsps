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
 * Barbarian Village was playing the wrong track: `music_by_region.yaml` lists region 12341
 * eleven separate times and `MusicService` parses it with a plain `put`, so the last entry
 * won. Barbarianism (141) is the *first* of the eleven, and the region resolved to 145 -
 * Yesteryear, the Lumbridge theme - instead.
 *
 * These tests pin the curated zone that now overrides that file, checking the track id
 * against the cache's own master table (DBTable 44, decoded by [MusicTracks]) rather than
 * against the wiki alone.
 */
class BarbarianVillageMusicVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        const val BARBARIANISM = 141

        /** The track the broken region mapping was resolving to instead. */
        const val YESTERYEAR = 145
    }

    @Test
    fun `track 141 really is Barbarianism in this cache`() {
        val entry = MusicTracks.byId(BARBARIANISM)
        assertNotNull(entry, "music id $BARBARIANISM is not in this cache's track table")
        assertEquals("Barbarianism", entry.name, "music id $BARBARIANISM is '${entry.name}' in this cache")
    }

    /**
     * The cache's own hint is what decides grouping, not the wiki's - established when
     * Ardougne's three tracks turned out to have distinct hints in cache but one shared
     * hint on the wiki. Here it confirms Barbarianism unlocks on its own.
     */
    @Test
    fun `the cache hint ties Barbarianism to Barbarian Village`() {
        val entry = MusicTracks.byId(BARBARIANISM)!!
        assertTrue(
            entry.hint.contains("Barbarian Village", ignoreCase = true),
            "Barbarianism's cache hint is '${entry.hint}'",
        )
    }

    @Test
    fun `real Barbarian Village locations play Barbarianism`() {
        // Every coordinate is one this repo actually spawns something on, from the
        // areas/barbarianvillage and npcs/barbarian plugins - so this pins the music to
        // the real content rather than to arbitrary points inside the rectangle.
        val places = listOf(
            "Peksa's Helmet Shop" to Tile(3075, 3430),
            "Atlas" to Tile(3075, 3439),
            "Litara, by the mine" to Tile(3081, 3420),
            "Gunthor the Brave, longhall" to Tile(3081, 3444),
            "Tassie Slipcast's studio" to Tile(3085, 3409),
            "Checkal" to Tile(3087, 3415),
            "Hunding, lookout tower" to Tile(3097, 3429),
            "Sigurd, canoe station" to Tile(3112, 3409),
        )

        places.forEach { (label, tile) ->
            val zone = MusicZones.lookup(tile)
            assertNotNull(zone, "$label at (${tile.x}, ${tile.z}) matches no music zone")
            assertEquals(
                listOf(BARBARIANISM),
                zone.trackIds,
                "$label at (${tile.x}, ${tile.z}) resolved to zone '${zone.name}'",
            )
        }
    }

    /** The regression this whole zone exists to prevent. */
    @Test
    fun `Barbarian Village no longer resolves to the Lumbridge theme`() {
        val zone = MusicZones.lookup(Tile(3081, 3420))!!
        assertTrue(
            YESTERYEAR !in zone.trackIds + zone.alsoUnlock,
            "'${zone.name}' still plays or unlocks Yesteryear",
        )
    }

    @Test
    fun `Barbarianism unlocks on its own, with no genre`() {
        val zone = MusicZones.lookup(Tile(3081, 3420))!!
        assertTrue(
            zone.alsoUnlock.isEmpty(),
            "'${zone.name}' should unlock nothing else - Barbarianism has its own hint",
        )
    }

    /**
     * The new zone sits directly south of Edgeville's block and directly north of
     * Draynor's, so these pin that it did not eat either neighbour.
     */
    @Test
    fun `neighbouring zones are unaffected`() {
        val edgeville = MusicZones.lookup(Tile(3093, 3490))
        assertNotNull(edgeville, "Edgeville matches no zone")
        assertEquals(listOf(98), edgeville.trackIds, "Edgeville resolved to '${edgeville.name}'")

        val draynorNorth = MusicZones.lookup(Tile(3100, 3300))
        assertNotNull(draynorNorth, "north Draynor matches no zone")
        assertEquals(listOf(151), draynorNorth.trackIds, "north Draynor resolved to '${draynorNorth.name}'")

        val faladorFarm = MusicZones.lookup(Tile(3040, 3290))
        assertNotNull(faladorFarm, "Falador Farm matches no zone")
        assertEquals(listOf(49), faladorFarm.trackIds, "Falador Farm resolved to '${faladorFarm.name}'")
    }

    /**
     * The track's other wiki polygon (x 3072-3135, z 9792-9855, captioned "Location before
     * 2021") is the superseded underground one and is deliberately not used - it would sit
     * on top of the Edgeville Dungeon zone. This pins that it stayed out.
     */
    @Test
    fun `the superseded underground polygon was not added`() {
        val underground = MusicZones.lookup(Tile(3100, 9820))
        if (underground != null) {
            assertTrue(
                BARBARIANISM !in underground.trackIds,
                "'${underground.name}' plays Barbarianism underground - that polygon is superseded",
            )
        }
    }
}
