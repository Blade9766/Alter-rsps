package org.alter.plugins.content.npcs.wolf

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the wolves in the world - 119 of them across eighteen published locations, spanning both the
 * `Wolf` and `White wolf` pages. Tiles, ids and the reasoning behind both live in [Wolves]; this
 * file is the wiring and nothing else.
 *
 * Aggression, respawn, Slayer experience and drops are **not** here - they are in [WolfPlugin].
 */
class WolfSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        Wolves.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = dealer.next(camp.npcKeys),
                    x = x,
                    z = z,
                    walkRadius = Wolves.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
