package org.alter.tools

import dev.openrune.cache.CacheManager
import java.nio.file.Paths

/**
 * Prints a varbit's cache definition - the varp it packs into and the bit range it occupies.
 *
 * `Player.setVarbit` resolves the id through `CacheManager.getVarbit` and writes into
 * `def.varp` at `def.startBit..def.endBit`. An id that does not exist in this revision, or that
 * resolves to a different varp than expected, therefore writes nothing the client will read - and
 * says nothing about it either way.
 *
 * Usage: gradlew :game-server:varbitDump --args="5963 8121 4143"
 */
object VarbitDump {
    private const val VARBIT_SCAN_LIMIT = 20000

    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        if (args.joinToString(" ").contains("varp=")) {
            val varp = args.joinToString(" ").substringAfter("varp=").trim().toIntOrNull()
            if (varp != null) {
                println("--- every varbit packed into varp $varp ---")
                (0 until VARBIT_SCAN_LIMIT).forEach { id ->
                    val d = runCatching { CacheManager.getVarbit(id) }.getOrNull() ?: return@forEach
                    if (d.varp == varp) {
                        val width = d.endBit - d.startBit + 1
                        val max = (1L shl width) - 1
                        println("  varbit $id -> bits ${d.startBit}..${d.endBit} (width $width, max $max)")
                    }
                }
                return
            }
        }
        val ids = args.joinToString(" ").split(Regex("[ ,]+")).mapNotNull { it.trim().toIntOrNull() }
        if (ids.isEmpty()) {
            println("Usage: --args=\"<varbit id>[ <varbit id>...]\"")
            return
        }
        ids.forEach { id ->
            val def = runCatching { CacheManager.getVarbit(id) }.getOrNull()
            if (def == null) {
                println("varbit $id -> MISSING from this cache")
            } else {
                println("varbit $id -> varp=${def.varp} bits=${def.startBit}..${def.endBit}")
            }
        }
    }
}
