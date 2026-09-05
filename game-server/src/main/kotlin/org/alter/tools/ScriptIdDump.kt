package org.alter.tools

import dev.openrune.cache.CacheManager
import java.nio.file.Paths

/**
 * Resolves clientscript names to ids the way [org.alter.api.ClientScript] does.
 *
 * `ClientScript(identifier)` calls `CacheManager.findScriptId` and **falls back to -1 when the
 * name is not found**, saying nothing. `runClientScript` then writes `RunClientScript(-1, ...)`,
 * which builds nothing - an interface whose construction depends on it opens as an empty frame,
 * with no error anywhere.
 *
 * Usage: gradlew :game-server:scriptIdDump --args="ge_offers_init,ge_offer_side_init,playermember"
 */
object ScriptIdDump {
    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        val names = args.joinToString(" ").split(Regex("[ ,]+")).map { it.trim() }.filter { it.isNotEmpty() }
        if (names.isEmpty()) {
            println("Usage: --args=\"<script name>[,<script name>...]\"")
            return
        }
        names.forEach { name ->
            val id = runCatching { CacheManager.findScriptId(name) }.getOrNull() ?: -1
            println(if (id == -1) "$name -> NOT FOUND (-1)" else "$name -> $id")
        }
    }
}
