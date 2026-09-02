package org.alter.plugins.content.areas.hosidius.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The Hosidius monsters: 5 goblins on the farmland south of the town, 3 giant bats
 * just north of the mine.
 *
 * All 5 published pins from the OSRS Wiki Goblin page's "South of Hosidius" LocLine.
 * Mapsquare 27_54 is in this project's decrypted cache.
 *
 * Level 2 throughout, on drop table 1 alone. That single-table listing is what says these
 * are the unarmed variant: where the wiki means armed goblins it lists both tables, as it
 * does for the Goblin Cave and the strip west of the Fishing Guild.
 *
 * Combat stats, animations and drops live in `org.alter.plugins.content.npcs.goblin`.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // South of Hosidius (5).
        spawnNpc(npc = "npc.goblin_3028", x = 1772, z = 3508, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3029", x = 1773, z = 3503, walkRadius = 8, direction = Direction.EAST) // level 2
        spawnNpc(npc = "npc.goblin_3030", x = 1776, z = 3505, walkRadius = 8, direction = Direction.SOUTH) // level 2
        spawnNpc(npc = "npc.goblin_3031", x = 1778, z = 3502, walkRadius = 8, direction = Direction.WEST) // level 2
        spawnNpc(npc = "npc.goblin_3032", x = 1779, z = 3508, walkRadius = 8, direction = Direction.NORTH) // level 2

        // Giant bats (3), north of the Hosidius mine. Level 27 and aggressive, against
        // goblins here that are level 2 and passive - a very different proposition a few
        // tiles up the road. Stats live in org.alter.plugins.content.npcs.critters.
        spawnNpc(npc = "npc.giant_bat", x = 1766, z = 3509, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 1767, z = 3504, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 1770, z = 3514, walkRadius = 8, direction = Direction.SOUTH)
    }
}
