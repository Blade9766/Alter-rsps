package org.alter.tools

import dev.openrune.cache.CacheManager
import org.alter.game.model.Tile
import java.nio.file.Paths

/**
 * Reports whether a player can stand on given tiles, using the same collision the live server
 * builds from the cache.
 *
 * Written to verify a ladder's arrival tile before wiring it. Guessing a destination is how a
 * player ends up in mid-air or inside a wall, and the map data answers it exactly.
 *
 * Usage: gradlew :game-server:tileCheck --args="2884,9798,0 2884,3398,0"
 */
object TileCheck {
    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        val tiles =
            args.joinToString(" ").split(Regex("[ ]+")).filter { it.isNotBlank() }.mapNotNull { spec ->
                val p = spec.split(',').mapNotNull { it.trim().toIntOrNull() }
                if (p.size >= 2) Tile(p[0], p[1], p.getOrElse(2) { 0 }) else null
            }
        if (tiles.isEmpty()) {
            println("Usage: --args=\"<x>,<z>[,<plane>] ...\"")
            return
        }
        val scene = CacheCollision.load(tiles.map { it.regionId }.distinct())
        tiles.forEach { tile ->
            val ok = scene.canStandOn(tile)
            println("  (${tile.x}, ${tile.z}, plane ${tile.height})  ->  ${if (ok) "STANDABLE" else "blocked"}")
        }
    }
}
