package org.alter.tools

import dev.openrune.cache.CacheManager
import java.nio.file.Paths

/**
 * Counts every object in the cache carrying a climb option, and how many of those any plugin
 * actually binds.
 *
 * `LadderPlugin` gates on a hand-written list of ids while its climb logic is entirely generic -
 * it moves the player one plane up or down on the same tile and knows nothing about the object.
 * So the list is the only thing standing between a working ladder and "Nothing interesting
 * happens", and this says how much of the game that leaves unreachable.
 *
 * Usage: gradlew :game-server:climbObjScan
 */
object ClimbObjScan {
    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        val byOption = HashMap<String, MutableList<Pair<Int, String>>>()
        var scanned = 0
        for (id in 0 until CacheManager.objectSize()) {
            val def = runCatching { CacheManager.getObject(id) }.getOrNull() ?: continue
            scanned++
            val name = def.name ?: continue
            if (name == "null") continue
            def.actions.filterNotNull().forEach { opt ->
                if (opt.lowercase().startsWith("climb")) {
                    byOption.getOrPut(opt.lowercase()) { mutableListOf() } += id to name
                }
            }
        }
        println("scanned $scanned object defs\n")
        var total = 0
        byOption.entries.sortedByDescending { it.value.size }.forEach { (opt, list) ->
            total += list.size
            println("  '$opt' -> ${list.size} objects")
        }
        println("\ntotal objects with a climb option: $total")

        val names = byOption.values.flatten().groupingBy { it.second }.eachCount()
        println("\ntop names:")
        names.entries.sortedByDescending { it.value }.take(12).forEach { (n, c) -> println("  ${n.padEnd(24)} $c") }
    }
}
