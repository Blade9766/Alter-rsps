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
 * Checks the Edgeville-area music zones against the cache's own master track table, the same
 * way `FaladorMusicVerify` does - a zone is only right if the id it plays really is that
 * track in this cache.
 *
 * The global "no two zones claim the same tile" invariant lives in `FaladorMusicVerify` and
 * covers these new zones automatically, which matters here: Alone's real polygon interlocks
 * with Lightness's along a jagged shared border, so the rectangles chosen for it had to stay
 * clear of the existing Falador zone.
 */
class EdgevilleMusicVerify {
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
        98 to ("Forever" to "in Edgeville"),
        102 to ("Alone" to "at Ice Mountain"),
        190 to ("Heart and Mind" to "at the Body Altar"),
    )

    @Test
    fun `every edgeville-area track id is really that track in this cache`() {
        tracks.forEach { (id, expected) ->
            val (name, _) = expected
            val entry = MusicTracks.byId(id)
            assertNotNull(entry, "music id $id ($name) is not in this cache's track table")
            assertEquals(name, entry.name, "music id $id is '${entry.name}' in this cache, not '$name'")
        }
    }

    @Test
    fun `each track's cache hint matches what the zones assume`() {
        tracks.forEach { (id, expected) ->
            val (name, hintFragment) = expected
            val entry = MusicTracks.byId(id)!!
            assertTrue(
                entry.hint.contains(hintFragment, ignoreCase = true),
                "$name (id $id) cache hint is '${entry.hint}', expected it to mention '$hintFragment'",
            )
        }
    }

    @Test
    fun `real edgeville locations resolve to Forever`() {
        // Coordinates this repo actually spawns content on, from areas/edgeville and the
        // Edgeville city guards - so the music is pinned to real content, not to arbitrary
        // points inside the rectangle.
        val edgevilleTiles =
            listOf(
                "Edgeville General Store" to Tile(3080, 3510),
                "Doris" to Tile(3079, 3492),
                "Hari (river bank)" to Tile(3132, 3509),
                "Edgeville guard (bridge)" to Tile(3085, 3518),
                "Edgeville guard (east)" to Tile(3114, 3512),
                "Iron mace spawn (jailhouse)" to Tile(3111, 3517),
                "Leather gloves spawn (south of bank)" to Tile(3097, 3486),
            )

        edgevilleTiles.forEach { (label, tile) ->
            val zone = MusicZones.lookup(tile)
            assertNotNull(zone, "$label at (${tile.x}, ${tile.z}) matches no music zone")
            assertEquals(listOf(98), zone.trackIds, "$label resolved to zone '${zone.name}'")
        }
    }

    /**
     * The wiki's hint for Alone names only Ice Mountain, but its classic polygon genuinely
     * covers the Edgeville Monastery too - Abbot Langley and Brother Althric both stand
     * inside it. Pinning that stops the zone being "corrected" to exclude them later.
     */
    @Test
    fun `the edgeville monastery plays Alone, not Forever`() {
        listOf(
            "Abbot Langley" to Tile(3052, 3490),
            "Brother Jered" to Tile(3052, 3491, 1),
            "Brother Althric" to Tile(3052, 3505),
        ).forEach { (label, tile) ->
            val zone = MusicZones.lookup(tile)
            assertNotNull(zone, "$label at (${tile.x}, ${tile.z}) matches no music zone")
            assertEquals(listOf(102), zone.trackIds, "$label resolved to zone '${zone.name}'")
        }
    }

    /**
     * Oziach's hut sits two tiles west of Forever's classic polygon, so it plays Alone. That
     * is real, not a boundary bug - his hut is outside the town proper and the polygon starts
     * at x=3072. Asserted so a future edit doesn't quietly "fix" it by inventing a boundary.
     */
    @Test
    fun `oziachs hut falls outside the Edgeville block`() {
        val zone = MusicZones.lookup(Tile(3070, 3517))
        assertNotNull(zone, "Oziach at (3070, 3517) matches no music zone")
        assertEquals(
            listOf(102),
            zone.trackIds,
            "Oziach's hut is west of Forever's classic block and should play Alone, got '${zone.name}'",
        )
    }

    @Test
    fun `the edgeville dungeon and body altar resolve to their own tracks`() {
        val dungeon = MusicZones.lookup(Tile(3100, 9900))
        assertNotNull(dungeon, "Edgeville Dungeon matches no music zone")
        assertEquals(listOf(98), dungeon.trackIds, "dungeon resolved to '${dungeon.name}'")

        val altar = MusicZones.lookup(Tile(2528, 4832))
        assertNotNull(altar, "Body Altar matches no music zone")
        assertEquals(listOf(190), altar.trackIds, "altar resolved to '${altar.name}'")
    }

    /**
     * Lightness stays where it was. It was reported as playing "north of Edgeville in the
     * Wilderness", but its own wiki infobox reads "This track unlocks north of Falador" and
     * neither of its map polygons reaches Edgeville - the classic one is the Falador/Ice
     * Mountain approach, and its pre-2021 modern one (3136-3200, 3520-3584) is north of
     * Varrock, not Edgeville. So nothing north of Edgeville should resolve to it.
     */
    @Test
    fun `lightness is not placed north of edgeville`() {
        val northOfEdgeville = MusicZones.lookup(Tile(3090, 3540))
        assertTrue(
            northOfEdgeville == null || northOfEdgeville.trackIds != listOf(113),
            "north of Edgeville resolved to Lightness via '${northOfEdgeville?.name}'",
        )

        // It is still correctly placed on its own classic polygon west of Ice Mountain.
        val falador = MusicZones.lookup(Tile(2970, 3480))
        assertNotNull(falador, "Lightness's own area matches no zone")
        assertEquals(listOf(113), falador.trackIds, "expected Lightness, got '${falador.name}'")
    }
}
