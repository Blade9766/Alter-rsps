package org.alter.tools

import dev.openrune.cache.CacheManager
import dev.openrune.cache.INTERFACES
import java.nio.file.Paths

/**
 * Hex-dumps one interface component's raw cache data.
 *
 * The cache library in this project does not decode interfaces at all, so a component's
 * var-transmit triggers - the varps whose change makes the client re-run the component's scripts
 * and repopulate it - cannot be read through any existing API. This prints the bytes so they can
 * be read by hand.
 *
 * Written for the Wilderness overlay: interface 481 component 42 renders `Level: 0` and ignores
 * `setComponentText` entirely, while component 46 on the same interface accepts it - proving 42 is
 * driven by a var rather than by server text. The trigger list is what names that var.
 *
 * Usage: gradlew :game-server:interfaceRawDump --args="481:42 481:46"
 */
object InterfaceRawDump {
    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        args.joinToString(" ").split(Regex("[ ,]+")).filter { it.isNotBlank() }.forEach { term ->
            val parts = term.split(':')
            val archive = parts.getOrNull(0)?.toIntOrNull()
            val file = parts.getOrNull(1)?.toIntOrNull()
            if (archive == null || file == null) {
                println("skip '$term' - expected <interface>:<component>")
                return@forEach
            }
            val data = runCatching { CacheManager.cache.data(INTERFACES, archive, file) }.getOrNull()
            if (data == null) {
                println("$archive:$file -> no data")
                return@forEach
            }
            println("=== $archive:$file (${data.size} bytes) ===")
            data.toList().chunked(BYTES_PER_LINE).forEachIndexed { line, chunk ->
                val offset = (line * BYTES_PER_LINE).toString().padStart(4, '0')
                val hex = chunk.joinToString(" ") { "%02x".format(it) }.padEnd(BYTES_PER_LINE * 3)
                val ascii = chunk.map { b -> if (b in 32..126) b.toInt().toChar() else '.' }.joinToString("")
                println("$offset  $hex |$ascii|")
            }
            println()
        }
    }

    private const val BYTES_PER_LINE = 16
}
