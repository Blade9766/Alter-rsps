package org.alter.plugins.content.areas.lovakengj.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The giant bats of Lovakengj, in south-west Kourend.
 *
 * All 5 published pins. One stands alone up at 1478,3833 near the Lovakengj mine; the
 * other four are together on the southern slopes.
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
        // The northern outlier (1).
        spawnNpc(npc = "npc.giant_bat", x = 1478, z = 3833, walkRadius = 8, direction = Direction.NORTH)

        // The southern slopes (4).
        spawnNpc(npc = "npc.giant_bat", x = 1501, z = 3740, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 1505, z = 3738, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 1529, z = 3729, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 1532, z = 3732, walkRadius = 8, direction = Direction.NORTH)
    }
}
