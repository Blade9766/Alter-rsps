package org.alter.plugins.content.npcs.frog

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the frogs in the world. Tiles, ids and the reasoning behind both live in [Frogs]; this file
 * is the wiring and nothing else.
 *
 * There is no [org.alter.plugins.content.npcs.SpawnDealer] here, unlike most of this bestiary pass:
 * every frog version is a single id, so a camp has nothing to deal between.
 *
 * Respawn and drops are **not** here - they are in [FrogPlugin].
 */
class FrogSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Frogs.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = camp.npcKey,
                    x = x,
                    z = z,
                    height = camp.plane,
                    walkRadius = Frogs.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
