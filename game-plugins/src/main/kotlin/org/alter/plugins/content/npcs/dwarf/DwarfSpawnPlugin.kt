package org.alter.plugins.content.npcs.dwarf

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings

/**
 * Puts the dwarves in the world - 105 of them across ten published locations. Tiles, ids and the
 * reasoning behind both live in [Dwarves]; this file is the wiring and nothing else.
 *
 * **Why these live here rather than in `areas/<name>/spawns`.** The same call
 * `content/npcs/dungeon/HillGiantSpawnPlugin` and `content/npcs/zombie/ZombieSpawnPlugin` make: ten
 * locations across four regions, of which only Falador has an area package at all - and that
 * package does not own the Mining Guild, let alone the Dwarven Mine below it. Splitting one species
 * across nine `areas/` packages that mostly do not exist would have scattered the table rather than
 * organised it.
 *
 * Slayer experience, respawn and drops are **not** here - they are in [DwarfPlugin], and the combat
 * stats were already correct in `data/cfg/npcs/monsterStats.json` before any of this existed. What
 * dwarves were missing was anywhere to be.
 */
class DwarfSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val dealer = SpawnDealer()
        Dwarves.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = dealer.next(camp.npcKeys),
                    x = x,
                    z = z,
                    height = camp.plane,
                    walkRadius = Dwarves.WALK_RADIUS,
                    direction = SpawnFacings.at(index),
                )
            }
        }
    }
}
