package org.alter.plugins.content.areas.yanilleagilitydungeon.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The monsters of the Yanille Agility Dungeon.
 *
 * All 8 published pins from the OSRS Wiki Giant bat page's "Yanille Agility Dungeon"
 * LocLine, every one level 27 - the normal aggressive variant, id 2834. Mapsquare 40_148
 * is in this project's decrypted cache, so these tiles have real collision data. Combat
 * stats and drops live in `org.alter.plugins.content.npcs.critters`, shared with the
 * Goblin Cave and Taverley bats.
 *
 * Unlike Taverley's single bat room these fall into two separate groups a long way apart -
 * three at x2569-2575, z9525-9530 and five at x2599-2607, z9478-9485 - so they are split
 * with a comment each rather than listed as one block.
 *
 * Every monster here that has a combat definition is now placed; see below for the one
 * that does not.
 *
 * The chaos druids, druid warriors and poison spiders now stand at their own published pins.
 * Salarin the twisted is still absent - he is a magic attacker, deliberately undefined in
 * `content/npcs/dungeon`, and a stat-less Salarin would be worse than none.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // The north-west passage (3).
        spawnNpc(npc = "npc.giant_bat", x = 2569, z = 9530, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2571, z = 9525, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2575, z = 9525, walkRadius = 8, direction = Direction.SOUTH)

        // The south-east chamber, past the obstacle pipe (5).
        spawnNpc(npc = "npc.giant_bat", x = 2599, z = 9485, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 2600, z = 9480, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2601, z = 9478, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2602, z = 9484, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 2607, z = 9485, walkRadius = 8, direction = Direction.WEST)

        // Poison spiders (18), level 64.
        spawnNpc(npc = "npc.poison_spider", x = 2530, z = 9447, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2535, z = 9458, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.poison_spider", x = 2537, z = 9460, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2538, z = 9463, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.poison_spider", x = 2539, z = 9457, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2539, z = 9463, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.poison_spider", x = 2541, z = 9463, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2542, z = 9457, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.poison_spider", x = 2544, z = 9455, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2550, z = 9454, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.poison_spider", x = 2554, z = 9449, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2556, z = 9436, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.poison_spider", x = 2558, z = 9450, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2559, z = 9435, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.poison_spider", x = 2559, z = 9445, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2560, z = 9432, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.poison_spider", x = 2561, z = 9439, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2563, z = 9438, walkRadius = 6, direction = Direction.EAST)

        // Chaos druids (11), level 13.
        spawnNpc(npc = "npc.chaos_druid", x = 2611, z = 9483, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_druid", x = 2611, z = 9487, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.chaos_druid", x = 2612, z = 9488, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.chaos_druid", x = 2613, z = 9482, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.chaos_druid", x = 2613, z = 9521, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_druid", x = 2614, z = 9483, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.chaos_druid", x = 2614, z = 9521, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.chaos_druid", x = 2614, z = 9525, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.chaos_druid", x = 2615, z = 9487, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_druid", x = 2616, z = 9484, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.chaos_druid", x = 2616, z = 9522, walkRadius = 6, direction = Direction.SOUTH)

        // Chaos druid warriors (9), level 37.
        spawnNpc(npc = "npc.chaos_druid_warrior", x = 2576, z = 9501, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_druid_warrior", x = 2578, z = 9500, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.chaos_druid_warrior", x = 2579, z = 9508, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.chaos_druid_warrior", x = 2580, z = 9497, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.chaos_druid_warrior", x = 2580, z = 9502, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_druid_warrior", x = 2583, z = 9499, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.chaos_druid_warrior", x = 2588, z = 9498, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.chaos_druid_warrior", x = 2594, z = 9498, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.chaos_druid_warrior", x = 2598, z = 9497, walkRadius = 6, direction = Direction.NORTH)
    }
}
