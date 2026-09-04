package org.alter.plugins.content.areas.templeofikov.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The monsters of the Temple of Ikov dungeon.
 *
 * All 3 published pins.
 *
 * The ice spiders, lesser demons and skeletons now stand at their own published pins. The
 * Fire Warrior of Lesarkus and the Guardian of Armadyl are still absent by design: the
 * former is damageable only with ice arrows, the latter is a quest NPC with no combat block.
 *
 * The normal aggressive bats, id 2834 - only Arceuus and the Dark Altar use the passive
 * version. Combat stats and drops live in `org.alter.plugins.content.npcs.critters`.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // 3 published pins.
        spawnNpc(npc = "npc.giant_bat", x = 2659, z = 9809, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2663, z = 9804, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2665, z = 9812, walkRadius = 8, direction = Direction.SOUTH)

        // Skeletons (15), levels 25 and 45 cycled evenly - the wiki lists them together.
        spawnNpc(npc = "npc.skeleton_77", x = 2637, z = 9891, walkRadius = 6, direction = Direction.NORTH)  // level 25
        spawnNpc(npc = "npc.skeleton_82", x = 2641, z = 9889, walkRadius = 6, direction = Direction.EAST)  // level 45
        spawnNpc(npc = "npc.skeleton_78", x = 2645, z = 9891, walkRadius = 6, direction = Direction.SOUTH)  // level 25
        spawnNpc(npc = "npc.skeleton_83", x = 2648, z = 9890, walkRadius = 6, direction = Direction.WEST)  // level 45
        spawnNpc(npc = "npc.skeleton_79", x = 2653, z = 9892, walkRadius = 6, direction = Direction.NORTH)  // level 25
        spawnNpc(npc = "npc.skeleton_82", x = 2657, z = 9894, walkRadius = 6, direction = Direction.EAST)  // level 45
        spawnNpc(npc = "npc.skeleton_80", x = 2658, z = 9820, walkRadius = 6, direction = Direction.SOUTH)  // level 25
        spawnNpc(npc = "npc.skeleton_83", x = 2658, z = 9888, walkRadius = 6, direction = Direction.WEST)  // level 45
        spawnNpc(npc = "npc.skeleton_81", x = 2660, z = 9885, walkRadius = 6, direction = Direction.NORTH)  // level 25
        spawnNpc(npc = "npc.skeleton_82", x = 2660, z = 9890, walkRadius = 6, direction = Direction.EAST)  // level 45
        spawnNpc(npc = "npc.skeleton_77", x = 2664, z = 9831, walkRadius = 6, direction = Direction.SOUTH)  // level 25
        spawnNpc(npc = "npc.skeleton_83", x = 2664, z = 9877, walkRadius = 6, direction = Direction.WEST)  // level 45
        spawnNpc(npc = "npc.skeleton_78", x = 2667, z = 9824, walkRadius = 6, direction = Direction.NORTH)  // level 25
        spawnNpc(npc = "npc.skeleton_82", x = 2670, z = 9828, walkRadius = 6, direction = Direction.EAST)  // level 45
        spawnNpc(npc = "npc.skeleton_79", x = 2671, z = 9822, walkRadius = 6, direction = Direction.SOUTH)  // level 25

        // Ice spiders (9), level 61.
        spawnNpc(npc = "npc.ice_spider", x = 2690, z = 9807, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.ice_spider_10722", x = 2693, z = 9816, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.ice_spider_13798", x = 2695, z = 9826, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.ice_spider", x = 2696, z = 9839, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.ice_spider_10722", x = 2708, z = 9842, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.ice_spider_13798", x = 2719, z = 9845, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.ice_spider", x = 2731, z = 9845, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.ice_spider_10722", x = 2744, z = 9840, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.ice_spider_13798", x = 2745, z = 9829, walkRadius = 6, direction = Direction.NORTH)

        // Lesser demons are spawned by `content/npcs/demon`, at the wiki's own three pins for this
        // dungeon. Respawning them here would double them.
    }
}
