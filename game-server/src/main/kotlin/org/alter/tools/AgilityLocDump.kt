package org.alter.tools

import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.loadLocations
import dev.openrune.cache.filestore.loadTerrain
import java.nio.file.Paths

/**
 * TEMPORARY diagnostic - scans every map region in the cache and prints the world tile of every
 * static object whose definition name matches one of the supplied '|'-separated filters.
 *
 * Usage: gradlew :game-server:agilityLocDump --args="log balance|obstacle net"
 * Optional second arg: comma-separated region ids to restrict the scan to.
 */
object AgilityLocDump {
    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        // Arg convention: underscores stand in for spaces so the whole filter list survives
        // Gradle's whitespace splitting of --args.
        val filters = (args.firstOrNull() ?: "")
            .replace('_', ' ')
            .split('|')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        if (filters.isEmpty()) {
            println("No name filters supplied.")
            return
        }

        val regionFilter = args.getOrNull(1)
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toHashSet()
            ?.takeIf { it.isNotEmpty() }

        val nameCache = HashMap<Int, String>()
        fun nameOf(id: Int): String =
            nameCache.getOrPut(id) { runCatching { CacheManager.getObject(id).name ?: "null" }.getOrDefault("null") }
        fun sizeOf(id: Int): String =
            runCatching { CacheManager.getObject(id).let { "${it.sizeX}x${it.sizeY}" } }.getOrDefault("?")
        fun actionsOf(id: Int): String =
            runCatching { CacheManager.getObject(id).actions.filterNotNull().joinToString("/") }.getOrDefault("")
        fun clipOf(id: Int): String =
            runCatching {
                CacheManager.getObject(id).let {
                    "solid=${it.solid} clipMask=${it.clipMask} impen=${it.impenetrable} obstr=${it.obstructive} clipType=${it.clipType}"
                }
            }.getOrDefault("?")

        var regionsRead = 0
        var hits = 0
        for (rx in 0 until 256) {
            for (ry in 0 until 256) {
                val region = (rx shl 8) or ry
                if (regionFilter != null && region !in regionFilter) continue
                val land = runCatching { CacheManager.cache.data(MAPS, "l${rx}_$ry") }.getOrNull() ?: continue
                // DefinitionSet drops an object a level when its tile carries the bridge flag at
                // height 1, so the raw cache height is not always the in-game height.
                val terrain = runCatching {
                    CacheManager.cache.data(MAPS, "m${rx}_$ry")?.let { loadTerrain(it) }
                }.getOrNull()
                regionsRead++
                val baseX = rx shl 6
                val baseZ = ry shl 6
                runCatching {
                    loadLocations(land) { loc ->
                        val name = nameOf(loc.id)
                        val lower = name.lowercase()
                        val acts = actionsOf(loc.id)
                        val matched = if (filters.singleOrNull() == "everything") {
                            true
                        } else if (filters.singleOrNull() == "all") {
                            name != "null" && acts.isNotEmpty()
                        } else {
                            filters.any { lower.contains(it) }
                        }
                        if (matched) {
                            val bridge = terrain?.let {
                                (it[1][loc.localX][loc.localY].settings.toInt() and 0x2) != 0
                            } ?: false
                            val effective = if (bridge) loc.height - 1 else loc.height
                            println(
                                "region=$region id=${loc.id} name='$name' " +
                                    "tile=(${baseX + loc.localX},${baseZ + loc.localY},$effective)" +
                                    (if (bridge) "[BRIDGE raw h=${loc.height}]" else "") + " " +
                                    "type=${loc.type} rot=${loc.orientation} size=${sizeOf(loc.id)} ${clipOf(loc.id)} actions=[$acts]",
                            )
                            hits++
                        }
                    }
                }
            }
        }
        println("-- regionsRead=$regionsRead hits=$hits --")
    }
}
