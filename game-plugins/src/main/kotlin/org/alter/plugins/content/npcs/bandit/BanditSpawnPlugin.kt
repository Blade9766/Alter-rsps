package org.alter.plugins.content.npcs.bandit

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the bandits in the world - 23 in the Bandit Camp and one in the Wilderness Slayer Cave.
 * Tiles, ids and the reasoning behind both live in [Bandits]; this file is the wiring and nothing
 * else.
 *
 * Aggression, respawn, Slayer experience and drops are **not** here - they are in [BanditPlugin].
 */
class BanditSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        Bandits.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = dealer.next(camp.npcKeys),
                    x = x,
                    z = z,
                    walkRadius = Bandits.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
