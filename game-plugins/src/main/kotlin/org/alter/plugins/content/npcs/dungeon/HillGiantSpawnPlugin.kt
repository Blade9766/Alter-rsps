package org.alter.plugins.content.npcs.dungeon

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Puts the hill giants in the world. Tiles, ids and the reasoning behind both live in
 * [HillGiantSpawns]; this file is the wiring and nothing else.
 *
 * Combat stats, animations, aggression and drops are **not** here - they are in
 * [DungeonMonsters] and [DungeonDrops], which have carried a correct hill giant since they were
 * written. The only thing missing was hill giants to apply them to: five in Taverley Dungeon
 * were the entire species.
 */
class HillGiantSpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        HillGiantSpawns.CAMPS.forEach { camp ->
            camp.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = camp.npcKeys[index % camp.npcKeys.size],
                    x = x,
                    z = z,
                    walkRadius = camp.walkRadius,
                    direction = FACINGS[index % FACINGS.size],
                )
            }
        }
    }

    private companion object {
        /**
         * Dealt round the camp so a dungeon is not twelve giants all facing south. Which way one
         * faces is not published and does not matter - it is overwritten the moment the giant
         * walks or picks a target, which for an aggressive monster is almost immediately.
         */
        val FACINGS = listOf(Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST)
    }
}
