package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.game.model.Tile
import org.alter.plugins.content.areas.duelarena.DuelArena
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
 * The Emir's Arena (the Duel Arena) was playing **Vampyre Assault** - a Morytania track -
 * through the same `music_by_region.yaml` last-entry-wins defect that had put the Lumbridge
 * theme on Barbarian Village. Regions 13362 and 13363 are each listed several times in that
 * file, once correctly against track 47 and later against 678, and `MusicService` parses it
 * with a plain `put`.
 *
 * Two curated zones now override it. Unusually for this table they use the tracks' **modern**
 * wiki polygons rather than the classic ones, because this cache is itself modern here - the
 * hint test below is what pins that. Getting it backwards would not have thrown; it would have
 * quietly played Shine in the duel lobby, so the hints are asserted rather than assumed.
 *
 * Track ids are checked against the cache's own master table (DBTable 44, decoded by
 * [MusicTracks]) rather than against the wiki alone.
 */
class EmirsArenaMusicVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** "The Emir's Arena" - the arena, its entrance, and the buildings north of it. */
        const val EMIRS_ARENA = 47

        /** "Shine" - the Mage Training Arena, immediately north. */
        const val SHINE = 122

        /** The track the broken region mapping was resolving to instead. */
        const val VAMPYRE_ASSAULT = 678

        const val AL_KHARID = 50
        const val ARABIAN_2 = 123
    }

    @Test
    fun `both track ids are the tracks this cache says they are`() {
        val arena = MusicTracks.byId(EMIRS_ARENA)
        assertNotNull(arena, "music id $EMIRS_ARENA is not in this cache's track table")
        assertTrue(
            arena.name.contains("Emir", ignoreCase = true) ||
                arena.name.contains("Duel Arena", ignoreCase = true),
            "music id $EMIRS_ARENA is '${arena.name}' in this cache, not the arena theme",
        )

        val shine = MusicTracks.byId(SHINE)
        assertNotNull(shine, "music id $SHINE is not in this cache's track table")
        assertEquals("Shine", shine.name, "music id $SHINE is '${shine.name}' in this cache")
    }

    /**
     * **The assumption the zones are built on.** Everywhere else in [MusicZones] the areas
     * come from the tracks' classic-mode polygons; here they come from the modern ones,
     * because this cache's hints are the post-2021 pair - 47 was extended north over the
     * entrance, hospital and bank in April 2021, and 122 was re-pointed at the Mage Training
     * Arena. Before that the split ran the other way, and Shine played at the hospital.
     *
     * If a cache bump ever moves these back to the classic wording, the zones below are
     * wrong and this fails rather than quietly playing the wrong track in the lobby. The
     * cache is what the client shows the player, so it wins over the wiki - the same
     * tie-break the Ardougne zones settled.
     */
    @Test
    fun `the cache hints are the modern pair`() {
        val arenaHint = MusicTracks.byId(EMIRS_ARENA)!!.hint
        assertTrue(
            arenaHint.contains("Emir", ignoreCase = true),
            "track $EMIRS_ARENA's cache hint is '$arenaHint' - if it no longer names the " +
                "Emir's Arena this cache is pre-2021 and these zones need the classic polygons",
        )

        val shineHint = MusicTracks.byId(SHINE)!!.hint
        assertTrue(
            shineHint.contains("Mage Training Arena", ignoreCase = true),
            "track $SHINE's cache hint is '$shineHint' - the classic hint put Shine at the " +
                "Duel Arena hospital, which would move the lobby back onto it",
        )

        // Distinct hints mean distinct unlocks, so neither zone gets an `alsoUnlock`.
        assertTrue(
            !arenaHint.equals(shineHint, ignoreCase = true),
            "the two hints are identical ('$arenaHint'), which would make them one genre",
        )
    }

    /**
     * All four arenas, from [DuelArena.ARENAS] itself rather than from hand-typed
     * coordinates - so this pins the music to the tiles duels are actually fought on, and
     * follows the arena bounds if they are ever corrected.
     */
    @Test
    fun `every arena interior plays the Emir's Arena theme`() {
        DuelArena.ARENAS.forEach { plot ->
            listOf(
                "min corner" to Tile(plot.minX, plot.minZ),
                "max corner" to Tile(plot.maxX, plot.maxZ),
                "centre" to Tile((plot.minX + plot.maxX) / 2, (plot.minZ + plot.maxZ) / 2),
            ).forEach { (label, tile) ->
                val zone = MusicZones.lookup(tile)
                assertNotNull(zone, "${plot.name} $label (${tile.x}, ${tile.z}) matches no music zone")
                assertEquals(
                    listOf(EMIRS_ARENA),
                    zone.trackIds,
                    "${plot.name} $label (${tile.x}, ${tile.z}) resolved to zone '${zone.name}'",
                )
            }
        }
    }

    /**
     * The April 2021 extension: the western entrance and the three buildings north of the
     * arenas are the arena theme's too, not Shine's. The lobby tile is [DuelArena]'s own, so
     * where duels actually start and end is covered by construction.
     */
    @Test
    fun `the entrance, lobby, hospital and bank play the Emir's Arena theme`() {
        listOf(
            "LOBBY_TILE" to DuelArena.LOBBY_TILE,
            "staging area, north edge" to Tile(3366, 3279),
            "western entrance road" to Tile(3316, 3236),
            "hospital" to Tile(3377, 3283),
            "bank" to Tile(3385, 3270),
            "walkway from the arenas" to Tile(3345, 3268),
        ).forEach { (label, tile) ->
            val zone = MusicZones.lookup(tile)
            assertNotNull(zone, "$label (${tile.x}, ${tile.z}) matches no music zone")
            assertEquals(
                listOf(EMIRS_ARENA),
                zone.trackIds,
                "$label (${tile.x}, ${tile.z}) resolved to zone '${zone.name}'",
            )
        }
    }

    /**
     * The regression the whole entry exists to prevent - and the reason it was not caught by
     * eye: 678 is a Morytania track, nowhere near this part of the map.
     */
    @Test
    fun `the arena no longer resolves to Vampyre Assault`() {
        listOf(
            "south-west arena" to Tile(3340, 3212),
            "north-east arena" to Tile(3378, 3251),
            "duel lobby" to DuelArena.LOBBY_TILE,
            "Mage Training Arena" to Tile(3363, 3300),
        ).forEach { (label, tile) ->
            val zone = MusicZones.lookup(tile)
            assertNotNull(zone, "$label (${tile.x}, ${tile.z}) matches no music zone")
            assertTrue(
                VAMPYRE_ASSAULT !in zone.trackIds + zone.alsoUnlock,
                "$label still plays or unlocks Vampyre Assault via '${zone.name}'",
            )
        }
    }

    /** North of the arena's buildings is Shine's, per this cache's hint for 122. */
    @Test
    fun `north of the arena plays Shine`() {
        listOf(
            "Mage Training Arena" to Tile(3363, 3300),
            "west of it" to Tile(3345, 3300),
            "east of it" to Tile(3385, 3300),
            "north edge" to Tile(3363, 3327),
        ).forEach { (label, tile) ->
            val zone = MusicZones.lookup(tile)
            assertNotNull(zone, "$label (${tile.x}, ${tile.z}) matches no music zone")
            assertEquals(
                listOf(SHINE),
                zone.trackIds,
                "$label (${tile.x}, ${tile.z}) resolved to zone '${zone.name}'",
            )
        }
    }

    /** Distinct cache hints mean distinct unlocks - neither drags the other in. */
    @Test
    fun `the two tracks unlock separately`() {
        val arena = MusicZones.lookup(Tile(3340, 3212))!!
        assertTrue(arena.alsoUnlock.isEmpty(), "'${arena.name}' should unlock nothing else")
        assertTrue(SHINE !in arena.trackIds, "'${arena.name}' also plays Shine")

        val north = MusicZones.lookup(Tile(3363, 3300))!!
        assertTrue(north.alsoUnlock.isEmpty(), "'${north.name}' should unlock nothing else")
        assertTrue(EMIRS_ARENA !in north.trackIds, "'${north.name}' also plays the arena theme")
    }

    /**
     * The boundary fix these zones needed. Al Kharid's zone is declared before them and
     * `lookup()` takes the first match, so its northern market extension running to x=3392
     * would have claimed the southern arenas (z 3208-3218) and the western entrance road
     * before the arena zone was ever consulted.
     */
    @Test
    fun `the Al Kharid market extension no longer reaches the arena`() {
        listOf(
            "south-west arena" to Tile(3340, 3212),
            "western entrance road" to Tile(3316, 3236),
        ).forEach { (label, tile) ->
            val zone = MusicZones.lookup(tile)!!
            assertTrue(
                AL_KHARID !in zone.trackIds,
                "$label resolved to '${zone.name}' - the extension still reaches it",
            )
        }

        // The two traders the extension exists for are still covered by it.
        listOf(
            "gem trader" to Tile(3287, 3212),
            "silk trader" to Tile(3298, 3202),
        ).forEach { (label, tile) ->
            val zone = MusicZones.lookup(tile)
            assertNotNull(zone, "$label (${tile.x}, ${tile.z}) matches no music zone")
            assertEquals(
                listOf(AL_KHARID),
                zone.trackIds,
                "$label (${tile.x}, ${tile.z}) resolved to zone '${zone.name}'",
            )
        }
    }

    /** Al Kharid proper, south of the split, is untouched by that rectangle being halved. */
    @Test
    fun `the Al Kharid zones are otherwise unaffected`() {
        listOf(
            Tile(3300, 3180),
            Tile(3270, 3150),
            Tile(3390, 3160),
        ).forEach { tile ->
            val zone = MusicZones.lookup(tile)
            assertNotNull(zone, "(${tile.x}, ${tile.z}) matches no music zone")
            assertEquals(
                listOf(AL_KHARID),
                zone.trackIds,
                "(${tile.x}, ${tile.z}) resolved to zone '${zone.name}'",
            )
        }

        // Arabian 2's band north of the city, which still runs to its polygon's x=3328.
        listOf(Tile(3300, 3300), Tile(3328, 3280)).forEach { tile ->
            val zone = MusicZones.lookup(tile)
            assertNotNull(zone, "north Al Kharid (${tile.x}, ${tile.z}) matches no music zone")
            assertEquals(
                listOf(ARABIAN_2),
                zone.trackIds,
                "north Al Kharid (${tile.x}, ${tile.z}) resolved to '${zone.name}'",
            )
        }
    }

    /**
     * No tile may match two zones - `lookup()` is a first-match, so an overlap would resolve
     * by declaration order and quietly hide whichever zone lost. This sweeps the whole
     * Al Kharid / arena corner, which is where every seam these zones introduced lives.
     */
    @Test
    fun `the new zones overlap nothing`() {
        for (x in 3260..3430) {
            for (z in 3130..3330) {
                val matches = MusicZones.zones.filter { it.contains(Tile(x, z)) }
                assertTrue(
                    matches.size <= 1,
                    "($x, $z) matches ${matches.size} zones: ${matches.joinToString { it.name }}",
                )
            }
        }
    }
}
