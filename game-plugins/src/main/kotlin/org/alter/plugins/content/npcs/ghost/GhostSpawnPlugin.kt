package org.alter.plugins.content.npcs.ghost

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the ghosts in the world - 100 of them across thirteen published locations. Tiles, ids and
 * the reasoning behind both live in [Ghosts]; this file is the wiring and nothing else.
 *
 * Aggression, respawn, Slayer experience and the two tertiaries are **not** here - they are in
 * [GhostPlugin].
 */
class GhostSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        Ghosts.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = dealer.next(camp.npcKeys),
                    x = x,
                    z = z,
                    height = camp.plane,
                    walkRadius = Ghosts.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
