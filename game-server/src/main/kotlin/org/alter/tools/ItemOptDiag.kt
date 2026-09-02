package org.alter.tools

import dev.openrune.cache.CacheManager
import java.nio.file.Paths

/** TEMPORARY diagnostic - dumps item options for the blowpipe family and scales. */
object ItemOptDiag {
    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)
        listOf(12924 to "toxic_blowpipe_empty", 12926 to "toxic_blowpipe", 28688 to "blazing_blowpipe", 12934 to "zulrahs_scales", 806 to "bronze_dart").forEach { (id, label) ->
            val d = runCatching { CacheManager.getItem(id) }.getOrNull()
            if (d == null) { println("$id ($label) -> MISSING"); return@forEach }
            println("$id ${d.name}  equipSlot=${d.equipSlot}")
            println("    interfaceOptions (inventory): ${d.interfaceOptions}")
            println("    options (ground/equipped):    ${d.options}")
        }
    }
}
