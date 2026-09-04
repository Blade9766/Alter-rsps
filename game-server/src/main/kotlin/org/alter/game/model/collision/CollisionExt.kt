package org.alter.game.model.collision

import org.alter.game.model.Tile
import org.alter.game.model.region.Chunk
import org.rsmod.routefinder.LineValidator
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.loc.LocShapeConstants

fun CollisionFlagMap.isClipped(tile: Tile): Boolean = get(tile) != 0
const val WALL_DIAGONAL = LocShapeConstants.WALL_DIAGONAL;
const val BLOCKED_TILE = 0x1
const val BRIDGE_TILE = 0x2
const val ROOF_TILE = 0x4

/**
 * Casts a line using Bresenham's Line Algorithm with point A [start] and
 * point B [target] being its two points and makes sure that there's no
 * collision flag that can block movement from and to both points. This function
 * was originally CollisionManager#raycast in rsmod1.
 *
 * @param projectile
 * Projectiles have a higher tolerance for certain objects when the object's
 * metadata explicitly allows them to.
 */
/*
 * @param srcSize
 * @param destWidth
 * @param destLength
 * The footprints of the two entities, in tiles. The validator walks the line
 * between the *nearest edges* of the two boxes rather than between their south-west
 * corners, so a large npc is visible from the side of it that faces you.
 *
 * These must be passed as at least `1`. The underlying [LineValidator] defaults
 * `destWidth`/`destLength` to `0`, and its edge-picking arithmetic
 * (`a + size - 1`) then resolves a destination that is *west/south of* the target
 * tile whenever the target sits east/north of the source - i.e. the line was being
 * cast at the wrong tile in half of all directions.
 */
fun LineValidator.rayCast(
    start: Tile,
    target: Tile,
    projectile: Boolean,
    srcSize: Int = 1,
    destWidth: Int = 1,
    destLength: Int = 1,
): Boolean {
    check(start.height == target.height) { "Tiles must be on the same height level." }
    return if (projectile) {
        hasLineOfSight(start.height, start.x, start.z, target.x, target.z, srcSize, destWidth, destLength)
    } else {
        hasLineOfWalk(start.height, start.x, start.z, target.x, target.z, srcSize, destWidth, destLength)
    }
}

/**
 * Extension stub function that will be used for setting a specific coordinate/height
 * to either impenetrable or not.
 */
fun CollisionFlagMap.block(newChunk: Chunk, chunkH: Int, lx: Int, lz: Int, impenetrable: Boolean) {
    // TODO impl this function.
}