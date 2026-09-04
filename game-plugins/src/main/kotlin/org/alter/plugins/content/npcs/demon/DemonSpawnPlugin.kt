package org.alter.plugins.content.npcs.demon

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the demons in the world. Tiles, ids and the reasoning behind both live in [Demons]; this file
 * is the wiring and nothing else.
 *
 * The walk radius is picked per size: a greater demon is **size 3** and gets the smaller
 * [Demons.GREATER_WALK_RADIUS], because a 3x3 monster given room to roam in Entrana Dungeon spends
 * its life wedged in the doorway.
 *
 * Aggression, respawn, Slayer experience and drops are **not** here - they are in [DemonPlugin].
 */
class DemonSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        val sizeOf = Demons.VARIANTS.flatMap { v -> v.npcKeys.map { it to v.size } }.toMap()

        Demons.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                val npcKey = dealer.next(camp.npcKeys)
                spawnNpc(
                    npc = npcKey,
                    x = x,
                    z = z,
                    height = camp.plane,
                    walkRadius = if (sizeOf[npcKey] == 3) Demons.GREATER_WALK_RADIUS else Demons.LESSER_WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
