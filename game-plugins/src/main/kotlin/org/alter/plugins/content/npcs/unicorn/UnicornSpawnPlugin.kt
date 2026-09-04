package org.alter.plugins.content.npcs.unicorn

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the unicorns and their foals in the world - 36 of them across seventeen
 * published locations. Tiles, ids
 * and the reasoning behind both live in [Unicorns]; this file is the wiring and nothing else.
 *
 * Respawn and drops are **not** here - they are in [UnicornPlugin].
 */
class UnicornSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        Unicorns.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = dealer.next(camp.npcKeys),
                    x = x,
                    z = z,
                    walkRadius = Unicorns.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
