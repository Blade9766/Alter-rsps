package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.TileData
import dev.openrune.cache.filestore.loadTerrain
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test

/**
 * A throwaway probe: for a handful of published wiki pins that [Bestiary2Verify] rejected, print what
 * every plane of that mapsquare actually holds.
 *
 * `BestiaryVerify`'s own doc records why this is worth doing rather than assuming: a `LocLine` that
 * says `plane = 0` is sometimes wrong, and the God Wars Dungeon and The Warrens both turned out to be
 * built on plane 2. This exists to answer that question for the five camps this pass could not place.
 */
class SpawnTileProbe {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    private val terrain = HashMap<Int, Array<Array<Array<TileData>>>?>()

    private fun describe(
        x: Int,
        z: Int,
        plane: Int,
    ): String {
        val rx = x shr 6
        val rz = z shr 6
        val tiles =
            terrain.getOrPut((rx shl 8) or rz) {
                CacheManager.cache.data(MAPS, "m${rx}_$rz")?.let { loadTerrain(it) }
            } ?: return "no mapsquare"
        val d = tiles[plane][x - (rx shl 6)][z - (rz shl 6)]
        val blocked = (d.settings.toInt() and 0x1) != 0
        val bare = d.overlayId.toInt() == 0 && d.underlayId.toInt() == 0
        return when {
            bare -> "no floor"
            blocked -> "BLOCK_WALK"
            else -> "ok(o=${d.overlayId} u=${d.underlayId})"
        }
    }

    /** How many tiles of a mapsquare's plane carry any paint at all - "is this plane built?" */
    private fun painted(
        x: Int,
        z: Int,
        plane: Int,
    ): Int {
        val rx = x shr 6
        val rz = z shr 6
        val tiles =
            terrain.getOrPut((rx shl 8) or rz) {
                CacheManager.cache.data(MAPS, "m${rx}_$rz")?.let { loadTerrain(it) }
            } ?: return -1
        var n = 0
        for (tx in 0 until 64) {
            for (tz in 0 until 64) {
                val d = tiles[plane][tx][tz]
                if (d.overlayId.toInt() != 0 || d.underlayId.toInt() != 0) n++
            }
        }
        return n
    }

    @Test
    fun `probe the pins Bestiary2Verify rejected`() {
        val samples =
            listOf(
                Triple("hellhound / Karuulm Slayer Dungeon", 1200, 10263),
                Triple("hellhound / Karuulm Slayer Dungeon", 1196, 10262),
                Triple("dragon / Taverley Dungeon upper", 2774, 9620),
                Triple("dragon / Taverley Dungeon upper", 2798, 9617),
                Triple("dragon / Brimhaven upper (baby green)", 2657, 9570),
                Triple("dragon / Brimhaven upper (baby green)", 2669, 9577),
                Triple("demon / Brimhaven upper (greater)", 2630, 9482),
                Triple("demon / Brimhaven upper (greater)", 2638, 9501),
                Triple("battle mage / Mage Arena", 3110, 3934),
                Triple("battle mage / Mage Arena", 3098, 3925),
                Triple("battle mage / Mage Arena", 3102, 3929),
            )
        println("=".repeat(100))
        samples.forEach { (label, x, z) ->
            val planes = (0..3).joinToString("  ") { "p$it=${describe(x, z, it)}" }
            val counts = (0..3).joinToString(",") { "${painted(x, z, it)}" }
            println("%-42s (%d, %d)  %s   [painted per plane: %s]".format(label, x, z, planes, counts))
        }
        println("=".repeat(100))

        // For the Mage Arena, walk a small window at plane 0 to see where the floor actually is.
        println("Mage Arena plane 0, x 3094..3120 / z 3920..3946, '.' = ok, '#' = BLOCK_WALK, ' ' = no floor")
        for (z in 3946 downTo 3920) {
            val row =
                (3094..3120).joinToString("") {
                    when (describe(it, z, 0)) {
                        "no floor" -> " "
                        "BLOCK_WALK" -> "#"
                        else -> "."
                    }
                }
            println("%d %s".format(z, row))
        }
    }
}
