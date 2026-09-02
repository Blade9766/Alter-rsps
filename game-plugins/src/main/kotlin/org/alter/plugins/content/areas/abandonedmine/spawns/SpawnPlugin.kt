package org.alter.plugins.content.areas.abandonedmine.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The giant bats of the Abandoned Mine beneath Mort'ton.
 *
 * Both published LocLines, 6 pins across two of the mine's floors - the only giant bat
 * location the wiki splits by depth. Both mapsquares are in this project's decrypted cache.
 *
 * The normal aggressive bats, id 2834. Combat stats and drops live in
 * `org.alter.plugins.content.npcs.critters`.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // Level 1 (4), mapsquare 53_150.
        spawnNpc(npc = "npc.giant_bat", x = 3410, z = 9632, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 3411, z = 9620, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 3416, z = 9620, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 3425, z = 9637, walkRadius = 8, direction = Direction.WEST)

        // Level 4 (2), mapsquare 43_70 - a different mapsquare entirely, which is
        // how the mine's lower floors are laid out rather than an error.
        spawnNpc(npc = "npc.giant_bat", x = 2787, z = 4503, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2789, z = 4499, walkRadius = 8, direction = Direction.EAST)
    }
}
