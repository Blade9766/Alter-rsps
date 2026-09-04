package org.alter.tools

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.alter.game.Server
import org.alter.game.plugin.PluginRepository
import org.alter.game.service.Service
import java.nio.file.Paths

/**
 * Boots a world and checks the Settings content actually registered, then prints the sub map the
 * "All Settings" panel will be read with.
 *
 * Two reasons this exists rather than trusting a clean compile. A plugin whose constructor throws is
 * reported by a single printed line and otherwise registers nothing, and a service that is never
 * loaded leaves its plugin reading a null - both look exactly like content that was never written.
 *
 * The second half is the part that cannot be settled from the cache alone. Interface 134 hands the
 * server a bare sub index, and the server recovers the setting by counting rows the way the client
 * counted them. That count depends on which rows this client hides, so the table below is the
 * server's prediction: open the panel in-game, click a row, and check `::settingsdebug` names the
 * same setting. Everything else here is verified, this is the one thing that is asserted.
 *
 * Usage: gradlew :game-server:settingsDiag
 */
object SettingsDiag {

    private const val ALL_SETTINGS = 134
    private const val SETTINGS_TAB = 116

    /** Components of the All Settings panel the server has to be listening to. */
    private val EXPECTED_ALL_SETTINGS_BUTTONS = listOf(4, 10, 19, 23)

    /**
     * Components of the side tab that carry a click mask in this cache. Anything bound outside this
     * set can never fire, which is how the previous bindings had gone stale without it showing.
     */
    private val EXPECTED_TAB_BUTTONS = listOf(5, 29, 30, 32, 44, 59, 67, 68, 85, 99, 113, 127, 128)

    @JvmStatic
    fun main(args: Array<String>) {
        // This boots a real world, so it wants the game port to itself. Without this check a running
        // server turns into a BindException raised inside a CompletableFuture: the world keeps
        // cycling, the checks below are never reached, and the task simply hangs with no clue why.
        val port = gamePort()
        if (port != null && !portIsFree(port)) {
            println("Port $port is already in use - stop the running game server before using this diagnostic.")
            Runtime.getRuntime().halt(1)
        }

        val server = Server()
        server.startServer(apiProps = Paths.get("../data/api.yml"))
        val world =
            server.startGame(
                filestore = Paths.get("../data", "cache"),
                gameProps = Paths.get("../game.yml"),
                devProps = Paths.get("../dev-settings.yml"),
            )

        var failures = 0
        fun check(
            label: String,
            ok: Boolean,
        ) {
            println((if (ok) "  ok   " else "  FAIL ") + label)
            if (!ok) failures++
        }

        println()
        println("=== Catalogue ===")

        val service = world.services.firstOrNull { it.javaClass.simpleName == "SettingsService" }
        check("SettingsService loaded", service != null)

        val categories = service?.categories()
        check("catalogue has categories, got ${categories?.size ?: 0}", !categories.isNullOrEmpty())

        println()
        println("=== Bindings ===")
        val buttons = world.plugins.privateField<Int2ObjectOpenHashMap<Any>>("buttonPlugins")
        fun bound(
            interfaceId: Int,
            component: Int,
        ) = buttons.containsKey((interfaceId shl 16) or component)

        val missingAll = EXPECTED_ALL_SETTINGS_BUTTONS.filterNot { bound(ALL_SETTINGS, it) }
        check("all settings panel components bound, missing=$missingAll", missingAll.isEmpty())

        val missingTab = EXPECTED_TAB_BUTTONS.filterNot { bound(SETTINGS_TAB, it) }
        check("settings tab components bound, missing=$missingTab", missingTab.isEmpty())

        val deadTab = (0..140).filter { bound(SETTINGS_TAB, it) && it !in EXPECTED_TAB_BUTTONS && it !in DROPDOWN_ROWS }
        check("no bindings on components that cannot be clicked, got $deadTab", deadTab.isEmpty())

        if (categories != null) {
            println()
            println("=== Predicted sub map for interface $ALL_SETTINGS:19 ===")
            println("  (open the panel, click a row, and confirm ::settingsdebug names the same setting)")
            // A category name (or a prefix of one) narrows the listing, because the whole map is
            // 351 rows and the useful case is checking one panel against the client.
            val filter = args.firstOrNull()
            for (category in categories) {
                val rows = category.visibleRows(service!!)
                val live = rows.count { it.live }
                println("  %-11s %3d rows drawn, %3d the server acts on".format(category.name, rows.size, live))
                if (filter != null && !category.name.startsWith(filter, ignoreCase = true)) {
                    continue
                }
                rows.forEachIndexed { sub, row ->
                    // "-" is a row the server deliberately leaves alone: a header has already been
                    // dropped, so these are the colour pickers, keybinds, sliders and info text.
                    val marker = if (row.live) "*" else "-"
                    println("      %s sub %-3d %s".format(marker, sub, row.title.take(72)))
                }
            }
        }

        println()
        println(if (failures == 0) "All checks passed." else "$failures check(s) failed.")
        Runtime.getRuntime().halt(if (failures == 0) 0 else 1)
    }

    /** `game-port` from game.yml, or null if it cannot be read - in which case the check is skipped. */
    private fun gamePort(): Int? =
        runCatching {
            java.io.File("../game.yml")
                .readLines()
                .firstNotNullOfOrNull { line ->
                    line.substringAfter("game-port:", "").trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                }
        }.getOrNull()

    private fun portIsFree(port: Int): Boolean = runCatching { java.net.ServerSocket(port).close() }.isSuccess

    /** The rows of the shared dropdown, which are legitimately bound even though the cache has no mask. */
    private val DROPDOWN_ROWS = listOf(36, 38, 39, 41)

    private data class Row(val title: String, val live: Boolean)

    /**
     * Reflects over the plugin classes rather than importing them, because `:game-server` does not
     * depend on `:game-plugins` at compile time.
     */
    private fun Any.categories(): List<Any> {
        @Suppress("UNCHECKED_CAST")
        return javaClass.getMethod("categories").invoke(this) as List<Any>
    }

    private val Any.name: String get() = javaClass.getMethod("getName").invoke(this) as String

    private fun Any.visibleRows(service: Any): List<Row> {
        // Free-to-play, which is what varbit 1777 reads as until something sets it.
        val layout = service.javaClass.getMethod("layout", javaClass, Int::class.javaPrimitiveType)
            .invoke(service, this, 0)

        @Suppress("UNCHECKED_CAST")
        val rows = layout.javaClass.getMethod("rows").invoke(layout) as List<Any>
        return rows.map { entry ->
            Row(
                title = entry.javaClass.getMethod("getTitle").invoke(entry) as String,
                live = entry.javaClass.getMethod("isLive").invoke(entry) as Boolean,
            )
        }
    }

    private fun List<Service>.firstOrNull(predicate: (Service) -> Boolean): Service? = this.find(predicate)

    @Suppress("UNCHECKED_CAST")
    private fun <T> PluginRepository.privateField(name: String): T =
        PluginRepository::class.java.getDeclaredField(name).also { it.isAccessible = true }.get(this) as T
}
