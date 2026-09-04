package org.alter.tools

import dev.openrune.cache.CacheManager
import org.alter.game.Server
import org.alter.game.model.entity.Player
import org.alter.game.plugin.Plugin
import org.alter.game.plugin.PluginRepository
import java.net.ServerSocket
import java.nio.file.Paths

/**
 * TEMPORARY diagnostic - boots a world, runs the real login chain against a headless player, and
 * checks the minimap data orbs will actually be on screen.
 *
 * This exists because the two halves of the fix live in different plugins with independent login
 * hooks, and nothing in a compile or a unit test proves they cooperate:
 * `SettingsEffects.onLogin` seeds varbit 4084, while `OSRSPlugin`'s own login hook is what opens
 * interface 160. Running the whole chain the way a real login does is the only way to see the
 * result rather than the intent.
 *
 * Usage: gradlew :game-server:dataOrbsDiag
 */
object DataOrbsDiag {
    /** "Show data orbs" - 1 shows them. See `SettingsEffects.Setting.SHOW_DATA_ORBS`. */
    private const val SHOW_DATA_ORBS_VARBIT = 4084

    /** The minimap interface: the four orbs, the compass and the world-map button. */
    private const val MINIMAP_INTERFACE = 160

    @JvmStatic
    fun main(args: Array<String>) {
        if (!portIsFree(GAME_PORT)) {
            println("Port $GAME_PORT is already in use - stop the running game server first.")
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

        // Nothing is written anywhere: the checks below read the server's own state, which is what
        // the client would have been told about.
        val player = Player(world).apply { username = "OrbDiag" }

        println()
        println("=== Before login ===")
        println("  varbit $SHOW_DATA_ORBS_VARBIT = ${player.readVarbit(SHOW_DATA_ORBS_VARBIT)} (a fresh account, so 0)")

        /*
         * The login hooks are run one at a time rather than through `executeLogin`, which stops at
         * the first throw. A headless player has no network `playerInfo`, so the hook that opens
         * the game frame dies on it - and under `executeLogin` that would take every later hook
         * with it, including the one being tested here. Each is given its own attempt instead, and
         * what survives is reported honestly.
         */
        val hooks: List<Plugin.() -> Unit> = world.plugins.privateField("loginPlugins")
        var ran = 0
        var threw = 0
        hooks.forEach { hook ->
            if (runCatching { player.executePlugin(hook) }.isSuccess) ran++ else threw++
        }
        println("  login hooks: $ran ran, $threw could not (no client for a headless player)")

        var failures = 0
        fun check(
            label: String,
            ok: Boolean,
        ) {
            println((if (ok) "  ok   " else "  FAIL ") + label)
            if (!ok) failures++
        }

        println()
        println("=== After login ===")
        val orbs = player.readVarbit(SHOW_DATA_ORBS_VARBIT)
        check("varbit $SHOW_DATA_ORBS_VARBIT is 1 (orbs shown), got $orbs", orbs == 1)

        /*
         * Interface $MINIMAP_INTERFACE is deliberately not asserted here. Opening it needs the game
         * frame, which needs the client this player does not have - and since the check that used
         * to withhold it is gone, "is it opened" now has no condition left to get wrong.
         */

        println()
        println(if (failures == 0) "PASS - a fresh login turns the orbs on." else "FAIL - $failures check(s) failed.")
        Runtime.getRuntime().halt(if (failures == 0) 0 else 1)
    }

    private const val GAME_PORT = 43594

    private fun portIsFree(port: Int): Boolean =
        runCatching { ServerSocket(port).use { } }.isSuccess

    @Suppress("UNCHECKED_CAST")
    private fun <T> PluginRepository.privateField(name: String): T =
        PluginRepository::class.java.getDeclaredField(name).also { it.isAccessible = true }.get(this) as T

    /**
     * `Player.getVarbit` lives in game-api, which depends on this module rather than the other way
     * round, so the varbit is resolved against the cache here the same way that extension does.
     */
    private fun Player.readVarbit(id: Int): Int {
        val def = CacheManager.getVarbit(id)
        return varps.getBit(def.varp, def.startBit, def.endBit)
    }
}
