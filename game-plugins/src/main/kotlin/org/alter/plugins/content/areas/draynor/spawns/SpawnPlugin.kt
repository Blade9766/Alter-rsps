package org.alter.plugins.content.areas.draynor.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The NPC spawns of the Draynor area.
 *
 * Goblins only, for now - every published goblin pin in and around Draynor Village and
 * Draynor Manor, taken from the OSRS Wiki Goblin page. All of them are level 2, the
 * unarmed free-to-play variant; there is no level 5 or 13 goblin anywhere in this part
 * of the map.
 *
 * **Most of these are filed under "Lumbridge" on the wiki**, not Draynor - its Lumbridge
 * LocLine is one flat list of 121 pins that runs the whole way west past Draynor Manor.
 * They are split off here by where they actually stand rather than by which LocLine
 * happens to carry them, so that `areas/lumbridge` holds Lumbridge and this file holds
 * Draynor. The two exceptions, "South-east of Draynor Village" and "South of Draynor
 * Manor", are the wiki's own separate Draynor-named LocLines.
 *
 * Combat stats, animations and drops for every id below live in
 * `org.alter.plugins.content.npcs.goblin` - shared with the Lumbridge spawns, so a goblin
 * here is mechanically the same monster as one east of Lumbridge castle.
 *
 * Which of the thirty level 2 ids stands on which tile is not published - the pins carry
 * coordinates only - so they are dealt round-robin over the tiles in the wiki's own
 * listing order, the same deliberate stable assignment
 * `areas/lumbridge/spawns/SpawnPlugin` makes, and for the same reason: what the ids
 * actually encode is the mail colour, and a field of one repeated id reads wrong.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // East of Draynor Manor, on the rising ground between the manor and the village (25).
        spawnNpc(npc = "npc.goblin_3028", x = 3137, z = 3287, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3029", x = 3137, z = 3297, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3030", x = 3141, z = 3304, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3031", x = 3142, z = 3294, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3032", x = 3142, z = 3297, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3033", x = 3142, z = 3302, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3034", x = 3144, z = 3299, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3035", x = 3144, z = 3300, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3036", x = 3144, z = 3308, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3037", x = 3145, z = 3294, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3038", x = 3145, z = 3303, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3039", x = 3145, z = 3305, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3040", x = 3145, z = 3310, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3041", x = 3146, z = 3296, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3042", x = 3146, z = 3298, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3043", x = 3146, z = 3302, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3044", x = 3147, z = 3302, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3051", x = 3147, z = 3308, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3052", x = 3148, z = 3296, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3053", x = 3148, z = 3300, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3054", x = 3149, z = 3310, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5195", x = 3150, z = 3302, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_5196", x = 3151, z = 3305, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5197", x = 3154, z = 3309, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_5198", x = 3156, z = 3293, walkRadius = 8, direction = Direction.NORTH)

        // The open grassland running east from there toward the Lumbridge cow field (26).
        spawnNpc(npc = "npc.goblin_5199", x = 3160, z = 3289, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_5200", x = 3161, z = 3307, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5201", x = 3162, z = 3277, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_5202", x = 3162, z = 3287, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5203", x = 3163, z = 3286, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3028", x = 3165, z = 3288, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3029", x = 3165, z = 3292, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3030", x = 3165, z = 3299, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3031", x = 3167, z = 3287, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3032", x = 3167, z = 3289, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3033", x = 3167, z = 3295, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3034", x = 3169, z = 3283, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3035", x = 3170, z = 3286, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3036", x = 3172, z = 3311, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3037", x = 3175, z = 3286, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3038", x = 3180, z = 3286, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3039", x = 3183, z = 3284, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3040", x = 3185, z = 3282, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3041", x = 3185, z = 3285, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3042", x = 3185, z = 3307, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3043", x = 3188, z = 3290, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3044", x = 3189, z = 3281, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3051", x = 3189, z = 3300, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3052", x = 3190, z = 3283, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3053", x = 3190, z = 3296, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3054", x = 3197, z = 3280, walkRadius = 8, direction = Direction.SOUTH)

        // East of Draynor Village, either side of the Lumbridge road (5).
        spawnNpc(npc = "npc.goblin_5195", x = 3141, z = 3258, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_5196", x = 3141, z = 3261, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5197", x = 3142, z = 3260, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_5198", x = 3144, z = 3259, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5199", x = 3146, z = 3260, walkRadius = 8, direction = Direction.WEST)

        // South-east of Draynor Village (7). Its own LocLine on the wiki, and the
        // only one of these groups that is not filed under "Lumbridge" there. The 3110,3281
        // pin is the wiki's own, despite sitting north of the village rather than south-east
        // of it - copied as published rather than silently relocated.
        spawnNpc(npc = "npc.goblin_5200", x = 3110, z = 3281, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5201", x = 3142, z = 3230, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_5202", x = 3143, z = 3227, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5203", x = 3144, z = 3231, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3028", x = 3144, z = 3233, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3029", x = 3145, z = 3229, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3030", x = 3146, z = 3234, walkRadius = 8, direction = Direction.SOUTH)

        // South of Draynor Manor (1), by the gate.
        spawnNpc(npc = "npc.goblin_3031", x = 3114, z = 3310, walkRadius = 8, direction = Direction.WEST)
    }
}
