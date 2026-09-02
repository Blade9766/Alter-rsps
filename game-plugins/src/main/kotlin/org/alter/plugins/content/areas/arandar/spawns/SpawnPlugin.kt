package org.alter.plugins.content.areas.arandar.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The goblins at the entrance to Arandar, the mountain pass into Tirannwn.
 *
 * All 3 published pins from the OSRS Wiki Goblin page's "Entrance to Arandar" LocLine.
 * Mapsquare 37_52 is in this project's decrypted cache.
 *
 * Level 5 throughout, on drop table 2 - the stab-fighting variant with 12 hitpoints and
 * a slower 6-cycle attack, not the level 2s that fill the free-to-play world.
 *
 * Combat stats, animations and drops live in `org.alter.plugins.content.npcs.goblin`.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // The Arandar pass entrance (3).
        spawnNpc(npc = "npc.goblin_3045", x = 2373, z = 3333, walkRadius = 8, direction = Direction.NORTH) // level 5
        spawnNpc(npc = "npc.goblin_3073", x = 2385, z = 3339, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3074", x = 2394, z = 3334, walkRadius = 8, direction = Direction.SOUTH) // level 5
    }
}
