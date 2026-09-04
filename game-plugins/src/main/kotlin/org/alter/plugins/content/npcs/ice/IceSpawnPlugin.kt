package org.alter.plugins.content.npcs.ice

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the ice warriors and ice giants in the world. Tiles, ids and the reasoning behind both live
 * in [IceCreatures]; this file is the wiring and nothing else.
 *
 * Aggression, respawn, Slayer experience and drops are **not** here - they are in [IcePlugin].
 */
class IceSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        val giantKeys = IceCreatures.VARIANTS.filter { it.giant }.flatMap { it.npcKeys }.toSet()

        IceCreatures.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                val npcKey = dealer.next(camp.npcKeys)
                spawnNpc(
                    npc = npcKey,
                    x = x,
                    z = z,
                    height = camp.plane,
                    walkRadius =
                        if (npcKey in giantKeys) IceCreatures.GIANT_WALK_RADIUS else IceCreatures.WARRIOR_WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
