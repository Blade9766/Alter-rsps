package org.alter.plugins.content.npcs.thief

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the thieves in the world - 23 of them across nine published locations. Tiles, ids and the reasoning
 * behind both live in [Thieves]; this file is the wiring and nothing else.
 *
 * Respawn and drops are **not** here - they are in [ThiefPlugin].
 */
class ThiefSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        Thieves.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = dealer.next(camp.npcKeys),
                    x = x,
                    z = z,
                    height = camp.plane,
                    walkRadius = Thieves.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
