package org.alter.tools

import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.loadLocations
import java.nio.file.Paths

/**
 * Finds scenery placements by name within a box of world tiles.
 *
 * Used to verify where a ladder actually goes before wiring it, rather than trusting a remembered
 * OSRS coordinate. A vertical ladder has a counterpart on the destination plane; a dungeon
 * entrance has one at the far end of the descent.
 *
 * Note this cache's map files are **not** xtea-encrypted - they decode with empty keys, and
 * passing the stale keys in `data/xteas.json.backup` makes every read return null silently.
 *
 * Usage: gradlew :game-server:locFind --args="ladder 2870 9780 2900 9820"
 *        (name-substring, then minX minZ maxX maxZ)
 */
object LocFind {
    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        val parts = args.joinToString(" ").split(Regex("[ ,]+")).filter { it.isNotBlank() }
        if (parts.size < 5) {
            println("Usage: --args=\"<name> <minX> <minZ> <maxX> <maxZ>\"")
            return
        }
        val needle = parts[0].lowercase()
        val (minX, minZ, maxX, maxZ) = parts.drop(1).take(4).map { it.toInt() }

        var found = 0
        for (rx in (minX shr 6)..(maxX shr 6)) {
            for (rz in (minZ shr 6)..(maxZ shr 6)) {
                val data = runCatching { CacheManager.cache.data(MAPS, "l${rx}_${rz}", IntArray(4)) }.getOrNull() ?: continue
                runCatching {
                    loadLocations(data) { loc ->
                        val wx = (rx shl 6) + loc.localX
                        val wz = (rz shl 6) + loc.localY
                        if (wx in minX..maxX && wz in minZ..maxZ) {
                            val name = runCatching { CacheManager.getObject(loc.id).name }.getOrNull() ?: return@loadLocations
                            if (name.lowercase().contains(needle)) {
                                val opts = runCatching { CacheManager.getObject(loc.id).actions.filterNotNull() }.getOrNull()
                                println("  id=${loc.id} '$name' at ($wx, $wz, plane ${loc.height})  type=${loc.type} rot=${loc.orientation}  opts=$opts")
                                found++
                            }
                        }
                    }
                }
            }
        }
        println("\n$found match(es) for '$needle' in ($minX,$minZ)-($maxX,$maxZ)")
    }
}
