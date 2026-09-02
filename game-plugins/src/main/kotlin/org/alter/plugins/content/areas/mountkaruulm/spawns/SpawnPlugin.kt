package org.alter.plugins.content.areas.mountkaruulm.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The giant bats of Mount Karuulm, in the Kebos Lowlands north of the Karuulm Slayer
 * Dungeon.
 *
 * All 11 published pins from the OSRS Wiki Giant bat page's "Mount Karuulm" LocLine, the
 * single largest giant bat group anywhere. Mapsquares 20_59, 20_60 and 21_59 are all in
 * this project's decrypted cache. Combat stats and drops live in
 * `org.alter.plugins.content.npcs.critters`.
 *
 * **These are the normal, aggressive bats** (id 2834). That is worth stating because the
 * sibling `areas/darkaltar` file, also Kourend and also giant bats, uses the passive
 * Arceuus id instead - the Giant bat page names Arceuus and the Dark Altar as the
 * exceptions and says nothing of the kind about Karuulm.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // The slopes north of the mountain (7).
        spawnNpc(npc = "npc.giant_bat", x = 1280, z = 3837, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 1288, z = 3846, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 1290, z = 3840, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 1301, z = 3837, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 1316, z = 3839, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 1325, z = 3843, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 1336, z = 3842, walkRadius = 8, direction = Direction.SOUTH)

        // South-east, toward the Karuulm mine road (4).
        spawnNpc(npc = "npc.giant_bat", x = 1348, z = 3788, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 1349, z = 3783, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 1351, z = 3786, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 1353, z = 3788, walkRadius = 8, direction = Direction.SOUTH)
    }
}
