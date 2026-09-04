package org.alter.plugins.content.npcs.dagannoth

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the dagannoth in the Lighthouse cave. Tiles, ids and the reasoning behind both live in
 * [Dagannoths]; this file is the wiring and nothing else.
 *
 * Aggression, respawn, Slayer experience, the spine attack and drops are **not** here - they are in
 * [DagannothPlugin].
 */
class DagannothSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        Dagannoths.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = dealer.next(camp.npcKeys),
                    x = x,
                    z = z,
                    height = camp.plane,
                    walkRadius = Dagannoths.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
