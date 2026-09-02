package org.alter.plugins.content.areas.shadowdungeon.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The giant bats of the Shadow Dungeon under Mort'ton.
 *
 * The single published pin - the smallest giant bat group in the game.
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
        // 1 published pin.
        spawnNpc(npc = "npc.giant_bat", x = 2709, z = 5086, walkRadius = 8, direction = Direction.NORTH)
    }
}
