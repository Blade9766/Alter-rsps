package org.alter.tools

import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.loadLocations
import dev.openrune.cache.filestore.loadTerrain
import org.alter.game.model.Tile
import org.alter.game.model.collision.canOccupy
import org.alter.game.model.collision.toggleLoc
import org.rsmod.routefinder.RouteFinding
import org.rsmod.routefinder.collision.CollisionStrategy
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Builds the server's collision map for a set of map regions straight from the cache, mirroring
 * [org.alter.game.fs.DefinitionSet.createRegion]: blocked terrain first (with the bridge-tile level
 * shift), then every loc's own clipping through the same `toggleLoc` the live server uses.
 *
 * Verified against a booted server's `world.collision` - the two agree tile for tile - which is what
 * makes it usable both from the `agilityReachDump` diagnostic and from tests that need to know where
 * a player can actually stand.
 *
 * [CacheManager.init] must already have been called.
 */
object CacheCollision {
    private const val BLOCKED_TILE = 0x1
    private const val BRIDGE_TILE = 0x2

    /** One static object as the cache places it, after the bridge-tile height adjustment. */
    data class Loc(
        val id: Int,
        val name: String,
        val shape: Int,
        val angle: Int,
        val tile: Tile,
        val width: Int,
        val length: Int,
        val clipMask: Int,
    )

    /** The collision map for a set of regions, plus every loc that was applied to it. */
    class Scene(
        val collision: CollisionFlagMap,
        val locs: List<Loc>,
    ) {
        // Stateful and not reentrant, but a Scene is only ever driven from one thread.
        private val finder = RouteFinding(collision)

        fun locsOf(id: Int): List<Loc> = locs.filter { it.id == id }

        /**
         * The tile [org.alter.game.model.move.ObjectPathAction] measures the interaction distance
         * from - the point on [loc]'s footprint nearest to [from].
         *
         * Faithful to that code including its quirks: wall decorations collapse to a zero-size
         * footprint, and the rotation swap it performs cancels out the one inside its own
         * `findNearestTile`, so the footprint used is always the unrotated one.
         */
        private fun nearestInteractionTile(
            from: Tile,
            loc: Loc,
        ): Tile {
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
            val adjustedWidth = if (loc.angle == 1 || loc.angle == 3) length else width
            val adjustedLength = if (loc.angle == 1 || loc.angle == 3) width else length
            return Tile(
                x = from.x.coerceIn(loc.tile.x..loc.tile.x + adjustedWidth),
                z = from.z.coerceIn(loc.tile.z..loc.tile.z + adjustedLength),
                height = loc.tile.height,
            )
        }

        /**
         * Whether a player standing on [from] passes the gate that decides between running an
         * object's plugin and printing "I can't reach that!".
         *
         * That gate is **not** [ReachStrategy]: `Route.success` is hardcoded `true` for every route
         * the finder manages to build, with `alternative` carrying whether it actually got there, so
         * what `ObjectPathAction` ends up testing is the distance from the nearest footprint tile.
         * The Al Kharid tropical tree is the case that shows the difference - it is swung to over a
         * fence, so `ReachStrategy` rejects the tile the player stands on while the game allows it.
         */
        fun canInteractFrom(
            from: Tile,
            loc: Loc,
            radius: Int = 1,
        ): Boolean = from.isWithinRadius(nearestInteractionTile(from, loc), radius)

        /**
         * Whether a player standing on [from] can walk over and interact with [loc] - the whole of
         * [org.alter.game.model.move.ObjectPathAction]'s `walkTo`: route to the object, then apply
         * [canInteractFrom] where the route leaves them.
         *
         * The route finder works within a single level, so a [from] on a different height than the
         * loc is unreachable by definition: getting between planes needs an obstacle or a staircase,
         * not a walk.
         */
        fun canReach(
            from: Tile,
            loc: Loc,
        ): Boolean {
            if (from.height != loc.tile.height) {
                return false
            }
            val route =
                finder.findRoute(
                    level = loc.tile.height,
                    srcX = from.x,
                    srcZ = from.z,
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
            if (!route.success) {
                return false
            }
            val last = route.waypoints.lastOrNull()
            val end = if (last == null) from else Tile(last.x, last.z, last.level)
            return canInteractFrom(end, loc)
        }

        /** Whether a player-sized pawn can occupy [tile]; the same test the live server applies. */
        fun canStandOn(tile: Tile): Boolean = collision.canOccupy(tile)

        /**
         * Every tile a player can stand on and use [loc] from - see [canInteractFrom] for why that
         * is the engine's real test. Scanning one tile beyond the footprint is enough, since the
         * gate is a radius of one around the nearest footprint tile.
         */
        fun approachTiles(loc: Loc): List<Tile> {
            val level = loc.tile.height
            val tiles = ArrayList<Tile>()
            // The footprint turns with the loc, so the box has to as well - `ReachStrategy` rotates
            // the dimensions internally, and scanning the unrotated box misses the tiles off the
            // ends of a rotated obstacle.
            val width = if (loc.angle == 1 || loc.angle == 3) loc.length else loc.width
            val length = if (loc.angle == 1 || loc.angle == 3) loc.width else loc.length
            for (x in loc.tile.x - 1..loc.tile.x + width) {
                for (z in loc.tile.z - 1..loc.tile.z + length) {
                    val tile = Tile(x, z, level)
                    if (!canStandOn(tile)) {
                        continue
                    }
                    if (canInteractFrom(tile, loc)) {
                        tiles.add(tile)
                    }
                }
            }
            return tiles
        }
    }

    fun load(regions: Iterable<Int>): Scene {
        val collision = CollisionFlagMap()
        val locs = ArrayList<Loc>()
        regions.distinct().forEach { region -> loadRegion(region, collision, locs) }
        return Scene(collision, locs)
    }

    /** The map region id that contains [x], [z]. */
    fun regionOf(
        x: Int,
        z: Int,
    ): Int = ((x shr 6) shl 8) or (z shr 6)

    private fun loadRegion(
        region: Int,
        collision: CollisionFlagMap,
        out: MutableList<Loc>,
    ) {
        val rx = region shr 8
        val rz = region and 0xFF
        val baseX = rx shl 6
        val baseZ = rz shl 6

        // A region with no terrain simply does not exist in the cache; callers over-request
        // neighbours on purpose, so this is not an error.
        val mapData = CacheManager.cache.data(MAPS, "m${rx}_$rz") ?: return
        val tiles = loadTerrain(mapData)

        for (cx in 0 until 8) {
            for (cz in 0 until 8) {
                for (level in 0 until 4) {
                    collision.allocateIfAbsent(baseX + cx * 8, baseZ + cz * 8, level)
                }
            }
        }

        val bridges = HashSet<Tile>()
        for (height in 0 until 4) {
            for (lx in 0 until 64) {
                for (lz in 0 until 64) {
                    val bridge = (tiles[1][lx][lz].settings.toInt() and BRIDGE_TILE) != 0
                    if (bridge) {
                        bridges.add(Tile(baseX + lx, baseZ + lz, height))
                    }
                    if ((tiles[height][lx][lz].settings.toInt() and BLOCKED_TILE) != 0) {
                        val level = if (bridge) height - 1 else height
                        if (level < 0) continue
                        collision.add(baseX + lx, baseZ + lz, level, CollisionFlag.BLOCK_WALK)
                    }
                }
            }
        }

        val landData = CacheManager.cache.data(MAPS, "l${rx}_$rz") ?: return
        loadLocations(landData) { loc ->
            val tile = Tile(baseX + loc.localX, baseZ + loc.localY, loc.height)
            val hasBridge = bridges.contains(tile)
            if (hasBridge && loc.height == 0) return@loadLocations
            val adjusted = if (hasBridge) tile.transform(-1) else tile
            val def = runCatching { CacheManager.getObject(loc.id) }.getOrNull() ?: return@loadLocations
            collision.toggleLoc(
                coords = adjusted,
                width = def.sizeX,
                length = def.sizeY,
                shape = loc.type,
                angle = loc.orientation,
                blockWalk = def.solid,
                blockRange = def.impenetrable,
                breakRouteFinding = def.obstructive,
                add = true,
            )
            out.add(
                Loc(
                    id = loc.id,
                    name = def.name ?: "null",
                    shape = loc.type,
                    angle = loc.orientation,
                    tile = adjusted,
                    width = def.sizeX,
                    length = def.sizeY,
                    clipMask = def.clipMask,
                ),
            )
        }
    }
}
