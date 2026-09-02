package org.alter.plugins.content.areas.grandtreemine.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The giant bats of the Grand Tree Mine, beneath the Tree Gnome Stronghold.
 *
 * All 4 published pins, evenly spaced along the mine at z9860-9863.
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
        // 4 published pins.
        spawnNpc(npc = "npc.giant_bat", x = 2443, z = 9862, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2451, z = 9860, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2460, z = 9860, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 2469, z = 9863, walkRadius = 8, direction = Direction.WEST)
    }
}
