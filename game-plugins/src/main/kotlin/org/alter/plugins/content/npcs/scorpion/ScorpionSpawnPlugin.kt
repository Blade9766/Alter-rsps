package org.alter.plugins.content.npcs.scorpion

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the scorpions in the world. Tiles, ids and the reasoning behind both live in [Scorpions]; this
 * file is the wiring and nothing else.
 *
 * Aggression, respawn, Slayer experience, poison and drops are **not** here - they are in
 * [ScorpionPlugin].
 */
class ScorpionSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        Scorpions.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = dealer.next(camp.npcKeys),
                    x = x,
                    z = z,
                    height = camp.plane,
                    walkRadius = Scorpions.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
