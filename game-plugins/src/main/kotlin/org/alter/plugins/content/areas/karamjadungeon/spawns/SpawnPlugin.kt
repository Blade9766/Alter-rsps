package org.alter.plugins.content.areas.karamjadungeon.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The giant bats of the Crandor and Karamja Dungeon.
 *
 * All 3 published pins, in the Karamja half near the entrance rope.
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
        // 3 published pins.
        spawnNpc(npc = "npc.giant_bat", x = 2834, z = 9568, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2856, z = 9575, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2861, z = 9568, walkRadius = 8, direction = Direction.SOUTH)
    }
}
