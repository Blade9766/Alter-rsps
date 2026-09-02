package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.game.model.Tile
import org.alter.plugins.content.mechanics.music.MusicTracks
import org.alter.plugins.content.mechanics.music.MusicZones
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Guards against the defect that made Barbarian Village play the Lumbridge theme.
 *
 * `music_by_region.yaml` is not a one-track-per-region mapping: it lists every track whose
 * unlock area *touches* a region. **476 of its 790 regions carry more than one entry**, and
 * `MusicService` parses it with a plain `put`, so the last line silently wins. Which track a
 * region ends up on is therefore an artefact of line order, not a decision - Varrock's main
 * region resolves to Xenophobe, Lumbridge Castle's to Yesteryear (from 43 candidates),
 * Edgeville's to Witching (from 40).
 *
 * The curated [MusicZones] table is what makes that safe, because it takes priority. So the
 * real invariant for any area this repo has built content for is simply: **it must be
 * covered by a zone, and never left to fall through to that file.** This test asserts that
 * over every town, so a future city added without zones fails here instead of quietly
 * playing a lottery result in game.
 */
class MusicRegionAudit {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    /** Parses the yaml as MusicService does, but keeping every candidate per region. */
    private val candidatesByRegion: Map<Int, List<Int>> by lazy {
        val out = linkedMapOf<Int, MutableList<Int>>()
        var pending: Int? = null
        Files.readAllLines(Paths.get("../data/cfg/music/music_by_region.yaml")).forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("- regionID:") -> pending = line.substringAfter(":").trim().toIntOrNull()
                line.startsWith("musicID:") -> {
                    val music = line.substringAfter(":").trim().toIntOrNull()
                    val region = pending
                    if (region != null && music != null) {
                        out.getOrPut(region) { mutableListOf() }.add(music)
                    }
                    pending = null
                }
            }
        }
        out
    }

    /** Real coordinates this repo actually spawns content on, one or more per built area. */
    private val builtAreas = listOf(
        "Lumbridge Castle" to Tile(3222, 3218),
        "Lumbridge (Bob's axes)" to Tile(3232, 3241),
        "Lumbridge Swamp (east)" to Tile(3243, 3151),
        "Varrock Square" to Tile(3213, 3428),
        "Varrock (Aubury)" to Tile(3253, 3402),
        "Varrock (Zaff)" to Tile(3203, 3434),
        "Al Kharid (Dommik)" to Tile(3321, 3194),
        "Al Kharid (gem trader)" to Tile(3287, 3212),
        "Draynor Village (Olivia)" to Tile(3085, 3253),
        "Draynor Village (Fortunato)" to Tile(3078, 3251),
        "Falador (Party Room)" to Tile(3053, 3374),
        "Falador (Rising Sun)" to Tile(2956, 3372),
        "Falador Farm" to Tile(3040, 3290),
        "Edgeville (bank area)" to Tile(3093, 3490),
        "Edgeville (Oziach)" to Tile(3070, 3517),
        "East Ardougne (market)" to Tile(2614, 3293),
        "North of East Ardougne (guards)" to Tile(2636, 3340),
        "Barbarian Village (Peksa)" to Tile(3075, 3430),
        "Barbarian Village (Checkal)" to Tile(3087, 3415),
        "Barbarian Village (Sigurd)" to Tile(3112, 3409),
    )

    @Test
    fun `every built area is covered by a curated zone`() {
        val uncovered = builtAreas.filter { (_, tile) -> MusicZones.lookup(tile) == null }
        assertTrue(
            uncovered.isEmpty(),
            "these built areas fall through to music_by_region.yaml, whose track is decided " +
                "by line order: " +
                uncovered.joinToString { (label, tile) ->
                    val candidates = candidatesByRegion[tile.regionId].orEmpty()
                    "$label (region ${tile.regionId}, ${candidates.size} yaml candidates, " +
                        "would play ${candidates.lastOrNull()?.let { MusicTracks.byId(it)?.name } ?: "nothing"})"
                },
        )
    }

    @Test
    fun `every zoned track really exists in this cache`() {
        MusicZones.zones.forEach { zone ->
            (zone.trackIds + zone.alsoUnlock).forEach { id ->
                assertNotNull(MusicTracks.byId(id), "zone '${zone.name}' references music id $id, absent from this cache")
            }
            assertTrue(zone.trackIds.isNotEmpty(), "zone '${zone.name}' plays nothing")
        }
    }

    /**
     * Pins the defect itself, so that if the backing file is ever cleaned up to one entry
     * per region the reason all these zones exist can be revisited rather than guessed at.
     */
    @Test
    fun `the region file still has the duplicate-entry defect the zones work around`() {
        val duplicated = candidatesByRegion.filterValues { it.size > 1 }
        assertTrue(
            duplicated.size > 100,
            "music_by_region.yaml now has only ${duplicated.size} multi-entry regions - " +
                "if it has been cleaned up, the curated zones may no longer be load-bearing",
        )
    }
}
