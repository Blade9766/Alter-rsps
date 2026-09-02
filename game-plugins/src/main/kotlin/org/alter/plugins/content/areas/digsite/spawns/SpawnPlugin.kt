package org.alter.plugins.content.areas.digsite.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The goblin at the Digsite, on the road above the excavation.
 *
 * The single published pin from the OSRS Wiki Goblin page's "The Digsite" LocLine.
 * Mapsquare 51_52 is in this project's decrypted cache.
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
        // The Digsite (1).
        spawnNpc(npc = "npc.goblin_3045", x = 3311, z = 3375, walkRadius = 8, direction = Direction.NORTH) // level 5
    }
}
