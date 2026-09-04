package org.alter.plugins.content.areas.edgevilledungeon.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The chaos druids of the Edgeville Dungeon, in the north-western chambers past the gate at
 * (3105,9944).
 *
 * All 11 published pins. This dungeon sits under the Wilderness, which is what makes these the
 * only chaos druids in the game that drop a looting bag - see
 * `org.alter.plugins.content.npcs.chaosdruid.ChaosDruids`, where that tertiary is marked
 * Wilderness-only and resolved against where the *killer* is standing.
 *
 * Every tile was checked against the region's own loc and terrain data before wiring, and one
 * needed moving: the wiki's pin at **(3106,9940)** is inside the rock wall dividing the two
 * north-south corridors, so it is placed one tile north at (3106,9941), the nearest walkable
 * tile and the corner of the western corridor the pin sits against. The other ten are as
 * published.
 *
 * This is the first content in this dungeon; the rest of its residents - the giant spiders,
 * skeletons, zombies, hobgoblins and Vannaka's chamber - are not placed yet.
 *
 * Combat stats and drops live in `org.alter.plugins.content.npcs.chaosdruid`.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // Chaos druids (11), level 13.
        spawnNpc(npc = "npc.chaos_druid", x = 3104, z = 9942, walkRadius = 4, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_druid", x = 3105, z = 9936, walkRadius = 4, direction = Direction.EAST)
        // Published at (3106,9940), which is solid rock; nudged one tile north.
        spawnNpc(npc = "npc.chaos_druid", x = 3106, z = 9941, walkRadius = 4, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.chaos_druid", x = 3107, z = 9943, walkRadius = 4, direction = Direction.WEST)
        spawnNpc(npc = "npc.chaos_druid", x = 3109, z = 9931, walkRadius = 4, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_druid", x = 3110, z = 9941, walkRadius = 4, direction = Direction.EAST)
        spawnNpc(npc = "npc.chaos_druid", x = 3111, z = 9936, walkRadius = 4, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.chaos_druid", x = 3111, z = 9939, walkRadius = 4, direction = Direction.WEST)
        spawnNpc(npc = "npc.chaos_druid", x = 3114, z = 9929, walkRadius = 4, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_druid", x = 3115, z = 9925, walkRadius = 4, direction = Direction.EAST)
        spawnNpc(npc = "npc.chaos_druid", x = 3115, z = 9932, walkRadius = 4, direction = Direction.SOUTH)
    }
}
