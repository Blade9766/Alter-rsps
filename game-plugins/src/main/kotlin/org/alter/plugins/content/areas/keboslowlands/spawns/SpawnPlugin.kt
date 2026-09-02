package org.alter.plugins.content.areas.keboslowlands.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The goblins of the Kebos Lowlands, west of Mount Karuulm.
 *
 * All 6 published pins from the OSRS Wiki Goblin page's "Kebos Lowlands" LocLine.
 * Mapsquare 20_54 is in this project's decrypted cache.
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
        // The Kebos Lowlands (6).
        spawnNpc(npc = "npc.goblin_3028", x = 1321, z = 3489, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3029", x = 1323, z = 3492, walkRadius = 8, direction = Direction.EAST) // level 2
        spawnNpc(npc = "npc.goblin_3030", x = 1324, z = 3485, walkRadius = 8, direction = Direction.SOUTH) // level 2
        spawnNpc(npc = "npc.goblin_3031", x = 1326, z = 3491, walkRadius = 8, direction = Direction.WEST) // level 2
        spawnNpc(npc = "npc.goblin_3032", x = 1327, z = 3488, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3033", x = 1330, z = 3491, walkRadius = 8, direction = Direction.EAST) // level 2
    }
}
