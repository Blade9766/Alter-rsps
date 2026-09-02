package org.alter.plugins.content.areas.treegnomevillage.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The Tree Gnome Village monsters: 10 goblins in the maze above, 3 giant bats in the
 * dungeon below.
 *
 * The goblins are all 10 published pins from the OSRS Wiki Goblin page's "Tree Gnome Village maze"
 * LocLine, levels 2 and 5. Mapsquare 39_49 is in this project's decrypted cache.
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
        // The maze and the village clearing (10).
        spawnNpc(npc = "npc.goblin_3028", x = 2505, z = 3176, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3045", x = 2519, z = 3145, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3029", x = 2523, z = 3155, walkRadius = 8, direction = Direction.SOUTH) // level 2
        spawnNpc(npc = "npc.goblin_3073", x = 2542, z = 3155, walkRadius = 8, direction = Direction.WEST) // level 5
        spawnNpc(npc = "npc.goblin_3030", x = 2550, z = 3166, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3074", x = 2550, z = 3197, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3031", x = 2552, z = 3195, walkRadius = 8, direction = Direction.SOUTH) // level 2
        spawnNpc(npc = "npc.goblin_3075", x = 2553, z = 3198, walkRadius = 8, direction = Direction.WEST) // level 5
        spawnNpc(npc = "npc.goblin_3032", x = 2555, z = 3194, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3076", x = 2557, z = 3198, walkRadius = 8, direction = Direction.EAST) // level 5

        // Giant bats (3), in the dungeon beneath - not the maze. Mapsquare 39_148, also
        // decrypted. Level 27 and aggressive, so nothing like the level 2 and 5 goblins
        // up top. Stats live in org.alter.plugins.content.npcs.critters.
        //
        // The Giant bat page has a *second* Tree Gnome Village dungeon LocLine, "during
        // Waterfall Quest", with three more bats at x2604-2607, z4437-4446. Those are not
        // here: that is the quest's instanced copy of the dungeon, reachable only while
        // Waterfall Quest is running, and this server has no quest to gate them behind -
        // spawning them would put three permanent bats in an area nobody can reach.
        spawnNpc(npc = "npc.giant_bat", x = 2540, z = 9566, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2542, z = 9557, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2543, z = 9564, walkRadius = 8, direction = Direction.SOUTH)
    }
}
