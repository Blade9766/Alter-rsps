package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import dev.openrune.cache.SOUNDEFFECTS
import org.alter.plugins.content.mechanics.ambience.AmbientSound
import org.alter.plugins.content.mechanics.ambience.AmbientSounds
import org.junit.BeforeClass
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the ambience table, which had no test at all until the underground pass on 2026-09-04
 * roughly doubled it.
 *
 * Ambience is the least self-reporting audio in the game. A wrong id plays nothing, a wrong tile
 * plays somewhere nobody stands, and both are indistinguishable in-game from the silence that was
 * there before - which is how 38 of 48 content areas came to have none without anyone noticing.
 *
 * These ids are also **single-sourced**: RuneLite carries no ambient entries, so unlike the combat
 * clips there is no second naming to cross-check against and they rest on `Sound.kt` alone. What
 * can be checked mechanically is checked here; that a clip *sounds* like its name cannot be, and
 * is not claimed.
 */
class AmbientSoundVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(java.nio.file.Paths.get("../data", "cache"), 228)
        }
    }

    /** An id that is not an archive in index 4 plays nothing, silently and forever. */
    @Test
    fun `every ambient sound id is a real sound archive`() {
        val archives = CacheManager.cache.archives(SOUNDEFFECTS).toHashSet()
        assertTrue(archives.isNotEmpty(), "no sound archives loaded; the cache is not readable here")

        val bad = AmbientSounds.sources.filter { it.soundId !in archives }.map { "${it.name}=${it.soundId}" }
        assertTrue(bad.isEmpty(), "These ambient ids are not archives in the cache and play nothing: $bad")
    }

    /** `AreaSound`'s constructor throws above 15; catching it here beats a plugin failing to load. */
    @Test
    fun `no source exceeds the protocol radius`() {
        val bad = AmbientSounds.sources.filter { it.radius !in 1..AmbientSound.MAX_RADIUS }.map { it.name }
        assertTrue(bad.isEmpty(), "Radius must be 1..${AmbientSound.MAX_RADIUS}: $bad")
    }

    /**
     * Catches the same clip placed twice in effectively the same spot - an accidental duplicate,
     * which doubles the sound on itself.
     *
     * The threshold is deliberately "one centre inside the other's radius", not "the radii touch
     * anywhere". A first cut used the looser rule and failed on placements that are correct and
     * deliberate: the two Lumbridge Castle fountains are 16 tiles apart *because* the cache puts
     * two Fountain objects there, and the Draynor Manor wind pair covers the manor and its
     * graveyard separately. Both share a clip on purpose. A test that fails on correct data is
     * worse than no test, so this only fires when two sources are genuinely on top of each other.
     */
    @Test
    fun `the same clip is not placed twice in one spot`() {
        val clashes = mutableListOf<String>()
        val all = AmbientSounds.sources
        for (i in all.indices) {
            for (j in i + 1 until all.size) {
                val a = all[i]
                val b = all[j]
                if (a.soundId != b.soundId || a.tile.height != b.tile.height) continue
                val dx = kotlin.math.abs(a.tile.x - b.tile.x)
                val dz = kotlin.math.abs(a.tile.z - b.tile.z)
                val core = kotlin.math.max(a.radius, b.radius)
                if (dx <= core && dz <= core) {
                    clashes += "${a.name} / ${b.name} (clip ${a.soundId}, only ${dx}x${dz} apart)"
                }
            }
        }
        assertTrue(clashes.isEmpty(), "These are the same clip in effectively one place: $clashes")
    }

    /** The underground pass is the point of the exercise; keep it from silently regressing. */
    @Test
    fun `the underground areas have ambience`() {
        val underground = AmbientSounds.sources.count { it.tile.z >= 3900 }
        assertTrue(underground >= 25, "expected the underground sources to still be present, found $underground")
    }
}
