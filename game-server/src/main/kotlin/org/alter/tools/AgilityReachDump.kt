package org.alter.tools

import dev.openrune.cache.CacheManager
import org.alter.game.model.Tile
import org.alter.tools.CacheCollision.Loc
import org.rsmod.routefinder.RouteFinding
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.collision.CollisionStrategy
import org.rsmod.routefinder.flag.CollisionFlag
import org.rsmod.routefinder.reach.ReachStrategy
import java.nio.file.Paths

/**
 * TEMPORARY diagnostic - rebuilds the server's collision map for a set of regions straight from the
 * cache (the same way [org.alter.game.fs.DefinitionSet.createRegion] does) and then reports, for
 * every listed object, which surrounding tiles the route finder considers a valid place to stand
 * and interact from.
 *
 * This is what turns "cannot reach that" into a concrete answer: either no tile can reach the
 * obstacle (its block-access mask points at a tile with no floor), or the tile the config expects
 * the player to stand on is not one of them.
 *
 * Usage: gradlew :game-server:agilityReachDump --args="<regionCsv> <objectIdCsv> [radius]"
 */
object AgilityReachDump {
    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        val regions = args[0].split(',').mapNotNull { it.trim().toIntOrNull() }
        val objectIds = args[1].split(',').mapNotNull { it.trim().toIntOrNull() }.toHashSet()
        val radius = args.getOrNull(2)?.toIntOrNull() ?: 3

        val scene = CacheCollision.load(regions)

        println("regions=$regions objects=$objectIds radius=$radius")
        scene.locs.filter { it.id in objectIds }.forEach { loc -> report(scene, loc, radius) }
    }

    /**
     * Replays [org.alter.game.model.move.ObjectPathAction] `walkTo`: find the route, walk it to its
     * end, then apply the same "nearest tile within `lineOfSightRange ?: 1`" gate that decides
     * between running the plugin and printing "I can't reach that!".
     */
    private fun walkToSucceeds(
        collision: CollisionFlagMap,
        finder: RouteFinding,
        loc: Loc,
        srcX: Int,
        srcZ: Int,
    ): Boolean {
        val level = loc.tile.height
        val wall = loc.shape == 0 || loc.shape == 9
        val wallDeco = loc.shape == 4 || loc.shape == 5
        var width = loc.width
        var length = loc.length
        if (wallDeco) {
            width = 0
            length = 0
        } else if (!wall && (loc.angle == 1 || loc.angle == 3)) {
            width = loc.length
            length = loc.width
        }

        val route = finder.findRoute(
            level = level,
            srcX = srcX,
            srcZ = srcZ,
            destX = loc.tile.x,
            destZ = loc.tile.z,
            destWidth = loc.width,
            destLength = loc.length,
            srcSize = 1,
            collision = CollisionStrategy.Normal,
            locAngle = loc.angle,
            locShape = loc.shape,
            blockAccessFlags = loc.clipMask,
        )
        if (!route.success) return false

        val end = route.waypoints.lastOrNull()
        val endX = end?.x ?: srcX
        val endZ = end?.z ?: srcZ

        val adjustedWidth = if (loc.angle == 1 || loc.angle == 3) length else width
        val adjustedLength = if (loc.angle == 1 || loc.angle == 3) width else length
        val nearestX = endX.coerceIn(loc.tile.x..loc.tile.x + adjustedWidth)
        val nearestZ = endZ.coerceIn(loc.tile.z..loc.tile.z + adjustedLength)
        return kotlin.math.abs(endX - nearestX) <= 1 && kotlin.math.abs(endZ - nearestZ) <= 1
    }

    private fun report(
        scene: CacheCollision.Scene,
        loc: Loc,
        radius: Int,
    ) {
        val collision = scene.collision
        println()
        println(
            "== ${loc.name} (${loc.id}) at (${loc.tile.x},${loc.tile.z},${loc.tile.height}) " +
                "shape=${loc.shape} angle=${loc.angle} size=${loc.width}x${loc.length} clipMask=${loc.clipMask}",
        )

        val finder = RouteFinding(collision)
        val level = loc.tile.height
        val reachable = ArrayList<String>()

        for (dz in radius downTo -radius) {
            val row = StringBuilder()
            for (dx in -radius..radius) {
                val x = loc.tile.x + dx
                val z = loc.tile.z + dz
                val flags = collision[x, z, level]
                val standable = (flags and CollisionFlag.BLOCK_WALK) == 0 &&
                    (flags and CollisionFlag.LOC) == 0
                val reached = ReachStrategy.reached(
                    flags = collision,
                    level = level,
                    srcX = x,
                    srcZ = z,
                    destX = loc.tile.x,
                    destZ = loc.tile.z,
                    destWidth = loc.width,
                    destLength = loc.length,
                    srcSize = 1,
                    locAngle = loc.angle,
                    locShape = loc.shape,
                    blockAccessFlags = loc.clipMask,
                )
                val canInteract = scene.canInteractFrom(Tile(x, z, level), loc)
                row.append(
                    when {
                        dx == 0 && dz == 0 -> 'O'
                        !standable -> '#'
                        canInteract -> if (reached) 'R' else 'r'
                        else -> '.'
                    },
                )
                if (standable && canInteract) {
                    val route = finder.findRoute(
                        level = level,
                        srcX = x,
                        srcZ = z,
                        destX = loc.tile.x,
                        destZ = loc.tile.z,
                        destWidth = loc.width,
                        destLength = loc.length,
                        srcSize = 1,
                        collision = CollisionStrategy.Normal,
                        locAngle = loc.angle,
                        locShape = loc.shape,
                        blockAccessFlags = loc.clipMask,
                    )
                    reachable.add("($x,$z,$level) route.success=${route.success}")
                }
            }
            println("   $row")
        }
        println(
            "   legend: O=object  #=cannot stand  .=stands but cannot interact  " +
                "R=can interact (and formally reaches)  r=can interact but ReachStrategy says no",
        )

        // Second grid: the whole of ObjectPathAction.walkTo, run from every standable tile in a
        // wider box - route, then the "nearest tile within radius" gate the route has to clear.
        val wide = radius + 4
        println("   walkTo() outcome from each standable tile (S=succeeds, X='I can't reach that!'):")
        for (dz in wide downTo -wide) {
            val row = StringBuilder()
            for (dx in -wide..wide) {
                val x = loc.tile.x + dx
                val z = loc.tile.z + dz
                val flags = collision[x, z, level]
                val standable = (flags and CollisionFlag.BLOCK_WALK) == 0 &&
                    (flags and CollisionFlag.LOC) == 0 &&
                    (flags and CollisionFlag.GROUND_DECOR) == 0
                if (dx == 0 && dz == 0) {
                    row.append('O')
                } else if (!standable) {
                    row.append('#')
                } else {
                    row.append(if (walkToSucceeds(collision, finder, loc, x, z)) 'S' else 'X')
                }
            }
            println("   $row")
        }
        if (reachable.isEmpty()) {
            println("   !! NO tile within $radius can both be stood on and interact with this object.")
        } else {
            reachable.forEach { println("   ok $it") }
        }
    }
}
