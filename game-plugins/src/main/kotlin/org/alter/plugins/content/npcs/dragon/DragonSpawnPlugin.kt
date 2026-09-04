package org.alter.plugins.content.npcs.dragon

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the dragons in the world. Tiles, ids and the reasoning behind both live in [Dragons]; this
 * file is the wiring and nothing else.
 *
 * The walk radius is picked per variant rather than fixed: an adult dragon is **size 4** and gets
 * the smaller [Dragons.ADULT_WALK_RADIUS], because a 4x4 monster given room to roam in a dungeon
 * spends its life wedged in doorways.
 *
 * Aggression, respawn, Slayer experience, dragonfire and drops are **not** here - they are in
 * [DragonPlugin].
 */
class DragonSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        val sizeOf = Dragons.VARIANTS.flatMap { v -> v.npcKeys.map { it to v.size } }.toMap()

        Dragons.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                val npcKey = dealer.next(camp.npcKeys)
                spawnNpc(
                    npc = npcKey,
                    x = x,
                    z = z,
                    height = camp.plane,
                    walkRadius = if (sizeOf[npcKey] == 4) Dragons.ADULT_WALK_RADIUS else Dragons.BABY_WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
