package org.alter.plugins.content.areas.heroesguild.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The giant bats of the Heroes' Guild basement.
 *
 * All 5 published pins, in two chambers either side of the basement.
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
        // The western chamber (2).
        spawnNpc(npc = "npc.giant_bat", x = 2889, z = 9907, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2896, z = 9911, walkRadius = 8, direction = Direction.EAST)

        // The eastern chamber (3).
        spawnNpc(npc = "npc.giant_bat", x = 2934, z = 9887, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 2936, z = 9896, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 2939, z = 9892, walkRadius = 8, direction = Direction.NORTH)
    }
}
