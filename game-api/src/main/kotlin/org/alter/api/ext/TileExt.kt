package org.alter.api.ext

import org.alter.game.model.Tile
import org.alter.game.model.World

fun Tile.isMulti(world: World): Boolean {
    val region = regionId
    val chunk = chunkCoords.hashCode()
    return world.getMultiCombatChunks().contains(chunk) || world.getMultiCombatRegions().contains(region)
}

/**
 * The wilderness level of this tile, or 0 when it isn't in the Wilderness at all.
 *
 * Level 1 begins immediately north of the Wilderness ditch and each level is 8 tiles deep, so
 * the level is `((y - 3520) / 8) + 1`. **The base is 3520, not 3525**: that was verified against
 * the six wilderness obelisks, whose in-game levels the wiki publishes (13/19/27/35/44/50) and
 * whose centre tiles come straight out of this project's own cache (z = 3620, 3667, 3732, 3794,
 * 3866 and 3916 respectively). Base 3520 reproduces all six exactly; the 3525 this used to carry
 * read one level low at every one of them and put each boundary 5 tiles too far north.
 *
 * The `z > 6400` fold maps the underground wilderness onto the surface levels above it. It is
 * applied *before* the bounds check rather than after - the check used to run against the raw z,
 * so every underground wilderness tile failed it and returned 0 before the fold could ever run.
 */
fun Tile.getWildernessLevel(): Int {
    if (x !in WILDERNESS_X) {
        return 0
    }

    val y = if (z > UNDERGROUND_Z_OFFSET) z - UNDERGROUND_Z_OFFSET else z
    if (y !in WILDERNESS_Z) {
        return 0
    }

    return ((y - WILDERNESS_BASE_Z) shr 3) + 1
}

fun Tile.isInWilderness(): Boolean = getWildernessLevel() > 0

/**
 * The Wilderness fence at level 30 - north of it the members-only "deep" Wilderness begins,
 * where the level-30 teleport methods stop working too. See `content/areas/wilderness`.
 */
fun Tile.isInDeepWilderness(): Boolean = getWildernessLevel() > DEEP_WILDERNESS_LEVEL

const val DEEP_WILDERNESS_LEVEL = 30

/** z that level 1 is measured from - see [getWildernessLevel]. */
private const val WILDERNESS_BASE_Z = 3520

/** Underground wilderness regions sit exactly this far north of the surface they mirror. */
private const val UNDERGROUND_Z_OFFSET = 6400

/** West shoreline to the eastern coast; the Wilderness is one contiguous block between them. */
private val WILDERNESS_X = 2944..3392

/** Level 1 starts the tile north of the ditch; level 56 (the deepest) ends at 3967. */
private val WILDERNESS_Z = 3523..3967
