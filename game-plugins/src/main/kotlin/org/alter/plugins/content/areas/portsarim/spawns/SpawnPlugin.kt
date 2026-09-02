package org.alter.plugins.content.areas.portsarim.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The NPC spawns of the Port Sarim area.
 *
 * Goblins only, for now - the OSRS Wiki Goblin page's "Outside Port Sarim" LocLine, all
 * twelve of it. Unlike the Lumbridge and Draynor lists this one needed no splitting: it
 * is the wiki's own Port Sarim entry and every pin really is in Port Sarim, on the strip
 * of scrub between the town's north gate and the Draynor road.
 *
 * All level 2, the unarmed free-to-play variant, on drop table 1 - the same monster as
 * the Lumbridge and Draynor goblins, and the first goblins on this server outside
 * Misthalin. Combat stats, animations and drops live in
 * `org.alter.plugins.content.npcs.goblin`.
 *
 * Which of the thirty level 2 ids stands on which tile is not published - the pins carry
 * coordinates only - so they are dealt round-robin over the tiles in the wiki's own
 * listing order, the same deliberate stable assignment the Lumbridge and Draynor spawn
 * files make, and for the same reason: what the ids encode is the mail colour.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // Outside Port Sarim, north of the town gate (12).
        spawnNpc(npc = "npc.goblin_3028", x = 2995, z = 3209, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3029", x = 2996, z = 3201, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3030", x = 2997, z = 3216, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3031", x = 2998, z = 3205, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3032", x = 2999, z = 3208, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3033", x = 2999, z = 3211, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3034", x = 3001, z = 3202, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3035", x = 3002, z = 3206, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3036", x = 3002, z = 3210, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3037", x = 3003, z = 3205, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3038", x = 3004, z = 3201, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3039", x = 3004, z = 3209, walkRadius = 8, direction = Direction.WEST)
    }
}
