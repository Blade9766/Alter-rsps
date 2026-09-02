package org.alter.plugins.content.areas.observatorydungeon.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The goblins of the Observatory Dungeon, beneath the observatory in north-west
 * Kandarin.
 *
 * All 31 published pins from the OSRS Wiki Goblin page's "Observatory Dungeon" LocLine,
 * levels 2 and 5 - the largest single goblin population on the server after Lumbridge and
 * Draynor. Mapsquare 36_146 is in this project's decrypted cache.
 *
 * The wiki lists both levels for the whole tile set without saying which tile is which,
 * so the two are cycled evenly over the pins in its listing order, as
 * `areas/clocktower` and the other mixed-level files do. Each line says which level it is.
 *
 * Its `dropversion` names both drop tables, but that is already explained by the level
 * mix - level 2 rolls table 1, level 5 rolls table 2 - so unlike `areas/goblincave` and
 * `areas/fishingguild` this is not evidence of armed goblins. The level 2s here are the
 * unarmed variant.
 *
 * Combat stats, animations and drops live in `org.alter.plugins.content.npcs.goblin`.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // The dungeon's tunnels (31).
        spawnNpc(npc = "npc.goblin_3028", x = 2305, z = 9385, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3045", x = 2307, z = 9359, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3029", x = 2307, z = 9403, walkRadius = 8, direction = Direction.SOUTH) // level 2
        spawnNpc(npc = "npc.goblin_3073", x = 2310, z = 9396, walkRadius = 8, direction = Direction.WEST) // level 5
        spawnNpc(npc = "npc.goblin_3030", x = 2316, z = 9392, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3074", x = 2317, z = 9371, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3031", x = 2317, z = 9383, walkRadius = 8, direction = Direction.SOUTH) // level 2
        spawnNpc(npc = "npc.goblin_3075", x = 2319, z = 9367, walkRadius = 8, direction = Direction.WEST) // level 5
        spawnNpc(npc = "npc.goblin_3032", x = 2321, z = 9402, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3076", x = 2322, z = 9387, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3033", x = 2324, z = 9404, walkRadius = 8, direction = Direction.SOUTH) // level 2
        spawnNpc(npc = "npc.goblin_3045", x = 2325, z = 9360, walkRadius = 8, direction = Direction.WEST) // level 5
        spawnNpc(npc = "npc.goblin_3034", x = 2333, z = 9346, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3073", x = 2333, z = 9366, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3035", x = 2335, z = 9382, walkRadius = 8, direction = Direction.SOUTH) // level 2
        spawnNpc(npc = "npc.goblin_3074", x = 2335, z = 9393, walkRadius = 8, direction = Direction.WEST) // level 5
        spawnNpc(npc = "npc.goblin_3036", x = 2337, z = 9387, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3075", x = 2339, z = 9403, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3037", x = 2342, z = 9347, walkRadius = 8, direction = Direction.SOUTH) // level 2
        spawnNpc(npc = "npc.goblin_3076", x = 2343, z = 9360, walkRadius = 8, direction = Direction.WEST) // level 5
        spawnNpc(npc = "npc.goblin_3038", x = 2344, z = 9369, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3045", x = 2348, z = 9390, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3039", x = 2349, z = 9380, walkRadius = 8, direction = Direction.SOUTH) // level 2
        spawnNpc(npc = "npc.goblin_3073", x = 2349, z = 9402, walkRadius = 8, direction = Direction.WEST) // level 5
        spawnNpc(npc = "npc.goblin_3040", x = 2351, z = 9359, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3074", x = 2359, z = 9345, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3041", x = 2359, z = 9359, walkRadius = 8, direction = Direction.SOUTH) // level 2
        spawnNpc(npc = "npc.goblin_3075", x = 2359, z = 9374, walkRadius = 8, direction = Direction.WEST) // level 5
        spawnNpc(npc = "npc.goblin_3042", x = 2359, z = 9382, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3076", x = 2359, z = 9392, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3043", x = 2363, z = 9403, walkRadius = 8, direction = Direction.SOUTH) // level 2
    }
}
