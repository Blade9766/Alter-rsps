package org.alter.plugins.content.npcs.mossgiant

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the moss giants in the world - 93 of them across twelve published locations. Tiles, ids and
 * the reasoning behind both live in [MossGiants]; this file is the wiring and nothing else.
 *
 * Aggression, respawn, Slayer experience and drops are **not** here - they are in [MossGiantPlugin].
 */
class MossGiantSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        MossGiants.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = dealer.next(camp.npcKeys),
                    x = x,
                    z = z,
                    walkRadius = MossGiants.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
