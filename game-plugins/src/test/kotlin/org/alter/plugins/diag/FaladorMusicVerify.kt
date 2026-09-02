package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.game.model.Tile
import org.alter.plugins.content.mechanics.music.MusicTracks
import org.alter.plugins.content.mechanics.music.MusicZone
import org.alter.plugins.content.mechanics.music.MusicZones
import org.alter.plugins.content.mechanics.music.TileArea
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Checks the Falador music zones against the cache's own master track table (DBTable 44,
 * which [MusicTracks] decodes) rather than against the wiki alone: a zone is only correct
 * if the id it plays is really the track it claims in this cache.
 *
 * Also asserts the zone table as a whole has no overlapping areas - [MusicZones.lookup]
 * resolves with `firstOrNull`, so two zones covering one tile would make the winner depend
 * on declaration order, which is exactly the kind of thing that silently breaks when a
 * later area is added.
 */
class FaladorMusicVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    /** Track id -> the display name and unlock hint the wiki gives for it. */
    private val faladorTracks = mapOf(
        186 to ("Arrival" to "in Falador"),
        72 to ("Fanfare" to "in Falador"),
        15 to ("Workshop" to "in Falador"),
        12 to ("Long Way Home" to "south of Falador"),
        107 to ("Miles Away" to "south of Falador"),
        127 to ("Nightfall" to "south of Falador"),
        113 to ("Lightness" to "north of Falador"),
        54 to ("Scape Soft" to "north of Falador"),
        49 to ("Wander" to "in Draynor Village"),
    )

    @Test
    fun `every falador track id is really that track in this cache`() {
        faladorTracks.forEach { (id, expected) ->
            val (name, _) = expected
            val entry = MusicTracks.byId(id)
            assertNotNull(entry, "music id $id ($name) is not in this cache's track table")
            assertEquals(name, entry.name, "music id $id is '${entry.name}' in this cache, not '$name'")
        }
    }

    /**
     * The grouping the zones use is driven by the wiki's unlock hint - the three
     * inside-walls tracks share one hint, the three southern ones another, the two
     * northern ones a third, and Wander has a Draynor hint despite sitting in the Falador
     * grid. If the cache's own hint text disagrees, the grouping is wrong.
     */
    @Test
    fun `each falador track's cache hint matches the grouping the zones assume`() {
        faladorTracks.forEach { (id, expected) ->
            val (name, hintFragment) = expected
            val entry = MusicTracks.byId(id)!!
            assertTrue(
                entry.hint.contains(hintFragment, ignoreCase = true),
                "$name (id $id) cache hint is '${entry.hint}', expected it to mention '$hintFragment'",
            )
        }
    }

    @Test
    fun `real falador locations resolve to the expected track`() {
        // Every coordinate here is one this repo actually spawns something on, from the
        // areas/falador plugins - so this pins the music to the real content, not to
        // arbitrary points inside each rectangle.
        val expectations = listOf(
            Triple("Falador General Store", Tile(2958, 3387), 72), // Fanfare
            Triple("Cassie's Shield Shop", Tile(2976, 3384), 72),
            Triple("Flynn's Mace Market", Tile(2950, 3387), 72),
            Triple("Herquin's Gems", Tile(2945, 3334), 72),
            Triple("Rising Sun Inn (Emily)", Tile(2956, 3372), 72),
            Triple("White Knights' Castle (Sir Vyvin)", Tile(2984, 3339), 72),
            Triple("Sir Tiffy Cashien, Falador Park", Tile(2997, 3373), 72),
            Triple("Wyson, Falador Park east", Tile(3027, 3379), 15), // Workshop
            Triple("Party Pete, Party Room", Tile(3053, 3374), 15),
            // Wayne really does stand outside the south gate, so he is not on a
            // city track at all.
            Triple("Wayne's Chains (south gate)", Tile(2972, 3313), 127), // Nightfall
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

    @Test
    fun `entering falador unlocks the whole inside-walls genre`() {
        val zone = MusicZones.lookup(Tile(2958, 3387))!!
        val unlocked = (zone.trackIds + zone.alsoUnlock).toSet()
        assertTrue(
            unlocked.containsAll(listOf(186, 72, 15)),
            "'${zone.name}' unlocks $unlocked, expected all of Arrival/Fanfare/Workshop",
        )
    }

    /**
     * Wander's classic unlock is Falador Farm, but its hint belongs to Draynor - so it must
     * neither pull in the Falador genres nor be pulled in by them, and Draynor's own zones
     * must not unlock it either.
     */
    @Test
    fun `wander unlocks alone at falador farm`() {
        val farm = MusicZones.lookup(Tile(3040, 3300))
        assertNotNull(farm, "Falador Farm matches no music zone")
        assertEquals(listOf(49), farm.trackIds, "expected Wander at Falador Farm, got '${farm.name}'")
        assertTrue(farm.alsoUnlock.isEmpty(), "Falador Farm should unlock Wander alone, got ${farm.alsoUnlock}")

        MusicZones.zones.filter { it.name.startsWith("Falador") && it.name != "Falador Farm" }.forEach { zone ->
            assertTrue(49 !in zone.trackIds + zone.alsoUnlock, "'${zone.name}' should not unlock Wander")
        }
        MusicZones.zones.filter { it.name.startsWith("Draynor") }.forEach { zone ->
            assertTrue(49 !in zone.trackIds + zone.alsoUnlock, "'${zone.name}' should not unlock Wander")
        }
    }

    @Test
    fun `no two music zones claim the same tile`() {
        fun rectangles(zone: MusicZone): List<TileArea> =
            zone.areas + zone.regionIds.map { id ->
                val regionX = id shr 8
                val regionZ = id and 0xFF
                TileArea(regionX * 64, regionZ * 64, regionX * 64 + 63, regionZ * 64 + 63)
            }

        fun overlaps(a: TileArea, b: TileArea): Boolean =
            a.x1 <= b.x2 && b.x1 <= a.x2 && a.z1 <= b.z2 && b.z1 <= a.z2

        val zones = MusicZones.zones
        for (i in zones.indices) {
            for (j in i + 1 until zones.size) {
                rectangles(zones[i]).forEach { a ->
                    rectangles(zones[j]).forEach { b ->
                        assertTrue(
                            !overlaps(a, b),
                            "'${zones[i].name}' $a overlaps '${zones[j].name}' $b - " +
                                "lookup() would resolve by declaration order",
                        )
                    }
                }
            }
        }
    }
}
