package org.alter.plugins.content.areas.legendsguild.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The giant bats of the Legends' Guild basement.
 *
 * All 5 published pins. One sits well west of the others, at 2703,9734.
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
        // The western passage (1).
        spawnNpc(npc = "npc.giant_bat", x = 2703, z = 9734, walkRadius = 8, direction = Direction.NORTH)

        // The main chamber (4).
        spawnNpc(npc = "npc.giant_bat", x = 2730, z = 9769, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2731, z = 9763, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 2731, z = 9774, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 2734, z = 9770, walkRadius = 8, direction = Direction.NORTH)
    }
}
