package org.alter.tools

import dev.openrune.cache.CacheManager
import java.nio.file.Paths

/**
 * TEMPORARY diagnostic - prints the cache definition of every object id in the supplied ranges.
 *
 * Written to pair closed gates with their opened states: the two share a model list and differ only
 * in their actions (`Open` vs `Close` vs none), which is the only reliable way to tell, say, that
 * the wilderness gates 1727/1728 open into 1571/1572 rather than into 1729/1730.
 *
 * Usage: gradlew :game-server:objDefDump --args="1550-1600,1727,1728"
 */
object ObjDefDump {
    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        val ids = ArrayList<Int>()
        (args.firstOrNull() ?: "").split(',').forEach { part ->
            val range = part.trim().split('-')
            when {
                range.size == 2 -> (range[0].toInt()..range[1].toInt()).forEach { ids.add(it) }
                part.isNotBlank() -> ids.add(part.trim().toInt())
            }
        }
        if (ids.isEmpty()) {
            println("No object ids supplied.")
            return
        }

        ids.forEach { id ->
            runCatching {
                val obj = CacheManager.getObject(id)
                println(
                    "id=$id name='${obj.name}' actions=[${obj.actions.filterNotNull().joinToString("/")}] " +
                        "size=${obj.sizeX}x${obj.sizeY} solid=${obj.solid} " +
                        "models=${obj.objectModels?.joinToString(",")} types=${obj.objectTypes?.joinToString(",")}",
                )
            }
        }
    }
}
