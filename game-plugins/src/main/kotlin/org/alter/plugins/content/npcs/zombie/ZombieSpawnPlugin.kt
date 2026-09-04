package org.alter.plugins.content.npcs.zombie

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Puts the zombies in the world. Tiles, ids and the reasoning behind both live in [Zombies]; this
 * file is the wiring and nothing else.
 *
 * **Why these live here rather than in `areas/<name>/spawns`.** The same call
 * `content/npcs/dungeon/HillGiantSpawnPlugin` and `content/npcs/critters/ChickenSpawns` make:
 * fourteen locations across four regions, of which only Varrock, Draynor, Edgeville and Ardougne
 * have an area package at all - and none of those packages owns the sewer or dungeon the zombies
 * are actually in. Splitting one species across four `areas/` packages and four that do not exist
 * would have scattered the table rather than organised it.
 *
 * Aggression, respawn, Slayer experience and drops are **not** here - they are in [ZombiePlugin],
 * and the combat stats were already correct in `data/cfg/npcs/monsterStats.json` before any of this
 * existed. The only thing zombies were missing was anywhere to be.
 */
class ZombieSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Zombies.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = camp.npcKeys[index % camp.npcKeys.size],
                    x = x,
                    z = z,
                    walkRadius = Zombies.WALK_RADIUS,
                    direction = FACINGS[index % FACINGS.size],
                )
            }
        }
    }

    private companion object {
        /**
         * Dealt round the camp so a graveyard is not twenty-one zombies all facing south. Which way
         * one faces is not published and does not matter - it is overwritten the moment the zombie
         * walks or picks a target, which for an aggressive monster is almost immediately.
         */
        val FACINGS = listOf(Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST)
    }
}
