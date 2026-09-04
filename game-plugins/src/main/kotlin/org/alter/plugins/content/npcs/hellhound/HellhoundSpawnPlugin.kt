package org.alter.plugins.content.npcs.hellhound

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the hellhounds in the world - 49 of them across the five published locations this cache can
 * hold. Tiles, ids and the reasoning behind both live in [Hellhounds]; this file is the wiring and
 * nothing else.
 *
 * Aggression, respawn, Slayer experience and drops are **not** here - they are in [HellhoundPlugin].
 */
class HellhoundSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        Hellhounds.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = dealer.next(camp.npcKeys),
                    x = x,
                    z = z,
                    height = camp.plane,
                    walkRadius = Hellhounds.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
