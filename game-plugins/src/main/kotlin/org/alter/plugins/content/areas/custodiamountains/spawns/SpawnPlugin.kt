package org.alter.plugins.content.areas.custodiamountains.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The giant bats of the Custodia Mountains, in Varlamore.
 *
 * All 8 published pins, in two groups a long way apart on the range.
 *
 * The normal aggressive bats, id 2834 - only Arceuus and the Dark Altar use the passive
 * version. Combat stats and drops live in `org.alter.plugins.content.npcs.critters`.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // The southern group (4).
        spawnNpc(npc = "npc.giant_bat", x = 1281, z = 3331, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 1287, z = 3330, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 1290, z = 3335, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 1294, z = 3331, walkRadius = 8, direction = Direction.WEST)

        // The northern group (4).
        spawnNpc(npc = "npc.giant_bat", x = 1283, z = 3406, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 1289, z = 3415, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 1295, z = 3409, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 1302, z = 3404, walkRadius = 8, direction = Direction.WEST)
    }
}
