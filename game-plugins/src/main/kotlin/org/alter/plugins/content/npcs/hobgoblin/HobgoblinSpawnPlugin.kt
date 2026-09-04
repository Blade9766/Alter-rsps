package org.alter.plugins.content.npcs.hobgoblin

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the hobgoblins in the world - 118 of them across fifteen published locations. Tiles, ids and
 * the reasoning behind both live in [Hobgoblins]; this file is the wiring and nothing else.
 *
 * Aggression, Slayer experience and drops are **not** here - they are in [HobgoblinPlugin].
 */
class HobgoblinSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        Hobgoblins.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = dealer.next(camp.npcKeys),
                    x = x,
                    z = z,
                    height = camp.plane,
                    walkRadius = Hobgoblins.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
