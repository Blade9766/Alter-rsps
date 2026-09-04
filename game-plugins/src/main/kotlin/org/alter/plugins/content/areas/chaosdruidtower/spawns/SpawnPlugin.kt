package org.alter.plugins.content.areas.chaosdruidtower.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The Chaos Druid Tower, north-west of East Ardougne: the chaos druids the tower is named for,
 * and the giant bats in the dungeon below it.
 *
 * All published pins for both, 4 druids and 5 bats.
 *
 * The druids stand in the small ground-floor room the tower's stairs come up into, between the
 * climb-down ladder at (2562,3356) and the pick-lock door at (2565,3356) - the 46 Thieving door
 * the wiki's footnote is about. That door is scenery this server does not gate yet, so the room
 * is currently reachable without the Thieving level; the druids are placed on the assumption it
 * will be, and are given a walk radius of 2 so they stay inside the room either way.
 *
 * The bats are the normal aggressive ones, id 2834 - only Arceuus and the Dark Altar use the
 * passive version. Combat stats and drops live in `org.alter.plugins.content.npcs.critters` for
 * the bats and `org.alter.plugins.content.npcs.chaosdruid` for the druids.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // Chaos druids (4), level 13. All four pins are clear ground - checked against the
        // region's own loc data, the same way the Slayer dungeon spawns were.
        spawnNpc(npc = "npc.chaos_druid", x = 2561, z = 3355, walkRadius = 2, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_druid", x = 2561, z = 3357, walkRadius = 2, direction = Direction.EAST)
        spawnNpc(npc = "npc.chaos_druid", x = 2563, z = 3355, walkRadius = 2, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.chaos_druid", x = 2563, z = 3358, walkRadius = 2, direction = Direction.WEST)

        // Giant bats (5), in a line along the single corridor at z9752-9754.
        spawnNpc(npc = "npc.giant_bat", x = 2563, z = 9753, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2565, z = 9754, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2567, z = 9752, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 2569, z = 9753, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 2572, z = 9752, walkRadius = 8, direction = Direction.NORTH)
    }
}
