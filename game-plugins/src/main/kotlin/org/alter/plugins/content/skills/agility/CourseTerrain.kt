package org.alter.plugins.content.skills.agility

import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.TileData
import dev.openrune.cache.filestore.loadTerrain
import org.alter.game.model.Tile

/**
 * Whether the cache actually draws a floor at a tile.
 *
 * `world.collision` cannot answer this. [org.alter.game.fs.DefinitionSet] copies only the terrain's
 * *blocked* flag into the collision map, so a height with nothing drawn at it carries no flag at all
 * and reads as free - `canOccupy` says yes to open sky. That is fine for walking, because nothing
 * routes a player off the floor they are on, but obstacles move players by [org.alter.game.model.ForcedMovement]
 * rather than by walking, and a landing tile is only checked against the collision map.
 *
 * The Gnome Stronghold obstacle net is where that bites: its walkway sits at height 1 on the south
 * side only, so climbing it from the ground *underneath* that walkway projects the landing to height
 * 1 on the north side, where there is no floor - leaving the player standing on nothing.
 *
 * Terrain is decoded once per map region and kept, so this costs one decode per region a course
 * touches.
 */
internal object CourseTerrain {
    private val regions = HashMap<Int, Array<Array<Array<TileData>>>?>()

    @Synchronized
    fun hasFloor(tile: Tile): Boolean {
        if (tile.height !in 0..3) {
            return false
        }
        val rx = tile.x shr 6
        val rz = tile.z shr 6
        val terrain =
            regions.getOrPut((rx shl 8) or rz) {
                runCatching {
                    CacheManager.cache.data(MAPS, "m${rx}_$rz")?.let { loadTerrain(it) }
                }.getOrNull()
            } ?: return false

        val data = terrain[tile.height][tile.x - (rx shl 6)][tile.z - (rz shl 6)]
        return data.overlayId.toInt() != 0 || data.underlayId.toInt() != 0
    }
}
