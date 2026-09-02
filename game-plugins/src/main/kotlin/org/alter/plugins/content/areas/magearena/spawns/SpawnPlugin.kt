package org.alter.plugins.content.areas.magearena.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The giant bats of the deep Wilderness west of the Mage Arena.
 *
 * All 7 published pins, in a tight cluster at x3073-3080, z3949-3961.
 *
 * **This is the one giant bat location in the Wilderness**, which makes it the only
 * place their 1/5 looting bag drop can actually fire - three times the rate most
 * monsters get, and dead code everywhere else on this list.
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
        // 7 published pins.
        spawnNpc(npc = "npc.giant_bat", x = 3073, z = 3961, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 3074, z = 3952, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 3075, z = 3955, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 3076, z = 3961, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 3077, z = 3949, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 3079, z = 3957, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 3080, z = 3961, walkRadius = 8, direction = Direction.SOUTH)
    }
}
