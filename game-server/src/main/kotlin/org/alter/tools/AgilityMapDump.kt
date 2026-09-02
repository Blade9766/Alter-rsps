package org.alter.tools

import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.loadLocations
import dev.openrune.cache.filestore.loadTerrain
import java.nio.file.Paths

/**
 * TEMPORARY diagnostic - renders one map region at one height as ASCII so obstacle landing tiles
 * can be read off the actual floor layout instead of being guessed.
 *
 *   '#' tile flagged BLOCK_WALK      '.' tile has a floor (overlay/underlay)
 *   ' ' no floor at this height      digit/letter = an interactable object, keyed underneath
 *
 * Usage: gradlew :game-server:agilityMapDump --args="<region> <height> [minX maxX minZ maxZ]"
 */
object AgilityMapDump {
    private const val BLOCKED = 0x1

    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        val region = args[0].toInt()
        val height = args[1].toInt()
        val rx = region shr 8
        val ry = region and 0xFF
        val baseX = rx shl 6
        val baseZ = ry shl 6

        val hideObjects = args.any { it == "noobj" }

        // Clamped to the region: the grid indexes straight into this region's 64x64 tile array.
        val x0 = (args.getOrNull(2)?.toInt() ?: baseX).coerceIn(baseX, baseX + 63)
        val x1 = (args.getOrNull(3)?.toInt() ?: (baseX + 63)).coerceIn(baseX, baseX + 63)
        val z0 = (args.getOrNull(4)?.toInt() ?: baseZ).coerceIn(baseZ, baseZ + 63)
        val z1 = (args.getOrNull(5)?.toInt() ?: (baseZ + 63)).coerceIn(baseZ, baseZ + 63)

        val mapData = CacheManager.cache.data(MAPS, "m${rx}_$ry") ?: error("no terrain for region $region")
        val tiles = loadTerrain(mapData)

        // Collect interactable objects on this height so they can be overlaid on the grid.
        val marks = HashMap<Pair<Int, Int>, Char>()
        val legend = LinkedHashMap<Char, String>()
        var next = 'a'
        CacheManager.cache.data(MAPS, "l${rx}_$ry")?.let { land ->
            loadLocations(land) { loc ->
                if (loc.height != height) return@loadLocations
                val def = runCatching { CacheManager.getObject(loc.id) }.getOrNull() ?: return@loadLocations
                val acts = def.actions.filterNotNull()
                if (def.name == null || def.name == "null" || acts.isEmpty()) return@loadLocations
                val wx = baseX + loc.localX
                val wz = baseZ + loc.localY
                if (wx !in x0..x1 || wz !in z0..z1) return@loadLocations
                val key = marks.getOrPut(wx to wz) { next.also { next++ } }
                legend[key] = "${def.name} (${loc.id}) at ($wx,$wz,$height) type=${loc.type} rot=${loc.orientation} [${acts.joinToString("/")}]"
            }
        }

        println("region=$region height=$height  x $x0..$x1   z $z0..$z1   (north at top)")
        print("      ")
        for (x in x0..x1) print(if ((x % 10) == 0) "|" else " ")
        println()
        for (z in z1 downTo z0) {
            print(String.format("%5d ", z))
            for (x in x0..x1) {
                val t = tiles[height][x - baseX][z - baseZ]
                val mark = marks[x to z]
                val ch = when {
                    mark != null && !hideObjects -> mark
                    (t.settings.toInt() and BLOCKED) != 0 -> '#'
                    t.overlayId.toInt() != 0 || t.underlayId.toInt() != 0 -> '.'
                    else -> ' '
                }
                print(ch)
            }
            println()
        }
        println()
        legend.forEach { (k, v) -> println("  $k = $v") }

        // Ranges are the unambiguous form of the grid above: counting ASCII columns by eye is how
        // landing tiles get authored one tile off.
        println()
        println("walkable runs (x ranges per z):")
        for (z in z1 downTo z0) {
            val runs = ArrayList<String>()
            var runStart = -1
            for (x in x0..x1 + 1) {
                val walkable = x <= x1 && run {
                    val t = tiles[height][x - baseX][z - baseZ]
                    (t.settings.toInt() and BLOCKED) == 0 &&
                        (t.overlayId.toInt() != 0 || t.underlayId.toInt() != 0)
                }
                if (walkable && runStart == -1) {
                    runStart = x
                } else if (!walkable && runStart != -1) {
                    runs.add(if (runStart == x - 1) "$runStart" else "$runStart-${x - 1}")
                    runStart = -1
                }
            }
            if (runs.isNotEmpty()) println("  z=$z : ${runs.joinToString(", ")}")
        }
    }
}
