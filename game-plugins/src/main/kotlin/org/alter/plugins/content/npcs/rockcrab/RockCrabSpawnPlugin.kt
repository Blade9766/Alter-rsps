package org.alter.plugins.content.npcs.rockcrab

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the Rock Crabs in the world - 103 of them across five published locations, all on plane 0.
 * Tiles, ids and the reasoning behind both live in [RockCrabs]; this file is the wiring.
 *
 * Aggression, respawn, Slayer experience and drops are **not** here - they are in [RockCrabPlugin].
 */
class RockCrabSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        RockCrabs.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = dealer.next(RockCrabs.NPC_KEYS),
                    x = x,
                    z = z,
                    walkRadius = RockCrabs.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
