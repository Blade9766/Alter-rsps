package org.alter.tools

import dev.openrune.cache.CacheManager
import dev.openrune.cache.INTERFACES
import java.nio.file.Paths

/**
 * TEMPORARY diagnostic - scans every interface archive in the cache and prints any printable
 * ASCII string embedded in a component's raw data whose text matches one of the supplied
 * '|'-separated (case-insensitive) filters.
 *
 * Interface component data is not decoded here: IF3 strings are null-terminated, so pulling
 * runs of printable bytes out of the raw file is enough to identify which interface owns a
 * given piece of on-screen text, which is all this is for.
 *
 * Usage: gradlew :game-server:interfaceTextDump --args="wilderness|level:"
 */
object InterfaceTextDump {
    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        val filters = (args.firstOrNull() ?: "")
            .replace('_', ' ')
            .split('|')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        if (filters.isEmpty()) {
            println("No text filters supplied.")
            return
        }

        val archiveFilter = args.getOrNull(1)
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toHashSet()
            ?.takeIf { it.isNotEmpty() }

        var archives = 0
        var hits = 0
        for (archive in 0 until 2000) {
            if (archiveFilter != null && archive !in archiveFilter) continue
            val files = runCatching { CacheManager.cache.files(INTERFACES, archive) }.getOrNull() ?: continue
            if (files.isEmpty()) continue
            archives++
            if (archiveFilter != null) {
                println("interface=$archive componentCount=${files.size} componentIds=${files.sorted()}")
            }
            for (file in files) {
                val data = runCatching { CacheManager.cache.data(INTERFACES, archive, file) }.getOrNull() ?: continue
                for (text in strings(data)) {
                    val lower = text.lowercase()
                    if (filters.singleOrNull() == "all" || filters.any { lower.contains(it) }) {
                        println("interface=$archive component=$file text='$text'")
                        hits++
                    }
                }
            }
        }
        println("-- archives=$archives hits=$hits --")
    }

    /** Runs of printable ASCII of length >= 3, as delimited by the null terminator. */
    private fun strings(data: ByteArray): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        for (b in data) {
            val c = b.toInt() and 0xFF
            if (c in 0x20..0x7E) {
                sb.append(c.toChar())
            } else {
                if (sb.length >= 3) out.add(sb.toString())
                sb.setLength(0)
            }
        }
        if (sb.length >= 3) out.add(sb.toString())
        return out
    }
}
