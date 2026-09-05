package org.alter.tools

import dev.openrune.cache.CacheManager
import java.nio.file.Paths

/**
 * Prints an item's cache-declared options, which is what every `onItemOption` /
 * `onEquipmentOption` binding is resolved against.
 *
 * `KotlinPlugin.onItemOption(item, option)` finds the option's *index* in `interfaceOptions` and
 * binds `2 + index`, because inventory ops arrive as `2 + index` - so an option that is present
 * but sits at a different index than expected binds to an op the client never sends for it, and
 * an option that is absent entirely makes the binding `check` throw, which takes the whole plugin
 * down with it. Both failures look identical in game: the click does nothing.
 *
 * Usage: gradlew :game-server:itemOptDiag --args="1712,2552,11941"
 * Names work too, matched case-insensitively as a substring:
 *   gradlew :game-server:itemOptDiag --args="amulet of glory,ring of dueling,looting bag"
 */
object ItemOptDiag {
    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        // Gradle splits --args on whitespace, so rejoin before splitting on the real separator.
        val terms = args.joinToString(" ").split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (terms.isEmpty()) {
            println("Usage: --args=\"<id|name>[,<id|name>...]\"")
            return
        }

        terms.forEach { term ->
            val id = term.toIntOrNull()
            val matches =
                if (id != null) {
                    listOfNotNull(runCatching { CacheManager.getItem(id) }.getOrNull()?.let { id to it })
                } else {
                    (0 until CacheManager.itemSize())
                        .asSequence()
                        .mapNotNull { i -> runCatching { CacheManager.getItem(i) }.getOrNull()?.let { i to it } }
                        .filter { (_, d) -> d.name.equals(term, ignoreCase = true) }
                        .take(MAX_NAME_MATCHES)
                        .toList()
                }

            if (matches.isEmpty()) {
                println("'$term' -> no match")
                return@forEach
            }

            matches.forEach { (itemId, d) ->
                println("$itemId  ${d.name}")
                println("    interfaceOptions (inventory): ${render(d.interfaceOptions)}")
                println("    options (ground/equipped):    ${render(d.options)}")
                val params = d.params
                if (params != null && params.isNotEmpty()) {
                    println("    params: " + params.entries.sortedBy { it.key }.joinToString(" ") { "${it.key}=${it.value}" })
                } else {
                    println("    params: <none>")
                }
                d.interfaceOptions.forEachIndexed { index, opt ->
                    if (!opt.isNullOrBlank() && opt != "null") {
                        println("      '$opt' -> inventory op ${INVENTORY_OP_OFFSET + index} (index $index)")
                    }
                }
            }
            println()
        }
    }

    private fun render(options: List<String?>) =
        options.mapIndexed { i, o ->
            val shown = when {
                o == null -> "<null>"
                o.isEmpty() -> "<empty>"
                o == "null" -> "<\"null\" string>"
                else -> "'" + o + "'"
            }
            "$i=$shown"
        }.joinToString(" ")

    /** Mirrors the private `KotlinPlugin.INVENTORY_OP_OFFSET`. */
    private const val INVENTORY_OP_OFFSET = 2

    private const val MAX_NAME_MATCHES = 12
}
