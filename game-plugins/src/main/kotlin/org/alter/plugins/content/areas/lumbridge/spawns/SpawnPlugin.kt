package org.alter.plugins.content.areas.lumbridge.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**Example
 *spawnNpc(npc = "npc.ID", x = xxxx, y = zzzz, height = 0, walk = 0, direction = Direction.NORTH)
 */


class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.man_3106", x = 3206, z = 3219, walkRadius = 20, height = 1, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.man_3106", x = 3216, z = 3219, walkRadius = 20, direction = Direction.EAST)
        spawnNpc(npc = "npc.man_3106", x = 3207, z = 3227, walkRadius = 20, direction = Direction.EAST)
        spawnNpc(npc = "npc.man_3108", x = 3209, z = 3215, walkRadius = 20, height = 1, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.man_3108", x = 3221, z = 3219, walkRadius = 20, direction = Direction.EAST)
        spawnNpc(npc = "npc.woman_3111", x = 3211, z = 3213, walkRadius = 20, height = 1, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.woman_3111", x = 3217, z = 3205, walkRadius = 20, direction = Direction.NORTH)
        spawnNpc(npc = "npc.rat_2854", x = 3207, z = 3202, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npc.rat_2854", x = 3205, z = 3204, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npc.rat_2854", x = 3206, z = 3202, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npc.rat_2854", x = 3207, z = 3203, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npc.rat_2854", x = 3205, z = 3209, walkRadius = 10, direction = Direction.EAST)
        spawnNpc(npc = "npc.rat_2854", x = 3207, z = 3209, walkRadius = 10, direction = Direction.EAST)
        // The Lumbridge Castle imp moved to org.alter.plugins.content.npcs.imp.ImpSpawns,
        // which places it on this same tile along with the other 119 the wiki publishes.
        // Spawning it from both places would give Lumbridge two imps.
        spawnNpc(npc = "npc.sheep_2789", x = 3196, z = 3263, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npc.sheep_2789", x = 3199, z = 3261, walkRadius = 10, direction = Direction.EAST)
        spawnNpc(npc = "npc.sheep_2789", x = 3201, z = 3272, walkRadius = 10, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.sheep_2789", x = 3202, z = 3268, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npc.sheep_2789", x = 3206, z = 3266, walkRadius = 10, direction = Direction.WEST)
        spawnNpc(npc = "npc.ram_1265", x = 3201, z = 3263, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npc.ram_1265", x = 3207, z = 3271, walkRadius = 10, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.ram_1265", x = 3195, z = 3271, walkRadius = 10, direction = Direction.EAST)
        // Giant spiders. All 16 tiles the OSRS Wiki places around Lumbridge, across its
        // four Lumbridge-area LocLines, and every one of them level 2 (id 3017). Combat
        // stats and drops live in org.alter.plugins.content.npcs.critters.
        //
        // This block used to be seven hand-placed tiles naming three different monsters,
        // all wrong, and all harmless only because spiders had no combat defs:
        //
        // - "npc.huge_spider_134" was the **Huge spider**, a combat level 81 Player-Owned
        //   House dungeon monster - 90 hitpoints, 59/70/69, max hit 8, aggressive. Its wiki
        //   page has no Locations section at all, because it does not exist in the world;
        //   it only ever appears inside someone's POH combat room.
        // - "npc.giant_spider" is id 2477, the level 50 Stronghold of Security spider.
        // - "npc.giant_spider_3018" is the level 27.
        //
        // Giant spiders are aggressive at every level, so with real stats those three would
        // have hunted down the players they were standing among.

        // West of Lumbridge, on the swamp road (3).
        spawnNpc(npc = "npc.giant_spider_3017", x = 3159, z = 3223, walkRadius = 7, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 3162, z = 3225, walkRadius = 7, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_spider_3017", x = 3163, z = 3222, walkRadius = 7, direction = Direction.SOUTH)

        // Around the H.A.M. Hideout trapdoor (5).
        spawnNpc(npc = "npc.giant_spider_3017", x = 3165, z = 3249, walkRadius = 7, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_spider_3017", x = 3166, z = 3242, walkRadius = 7, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 3167, z = 3247, walkRadius = 7, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_spider_3017", x = 3170, z = 3245, walkRadius = 7, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 3170, z = 3248, walkRadius = 7, direction = Direction.WEST)

        // Lumbridge proper, west of the castle (2).
        spawnNpc(npc = "npc.giant_spider_3017", x = 3200, z = 3238, walkRadius = 7, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 3203, z = 3239, walkRadius = 7, direction = Direction.EAST)

        // East side of Lumbridge, across the river (6).
        spawnNpc(npc = "npc.giant_spider_3017", x = 3239, z = 3256, walkRadius = 7, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 3241, z = 3241, walkRadius = 7, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_spider_3017", x = 3241, z = 3249, walkRadius = 7, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 3243, z = 3236, walkRadius = 7, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_spider_3017", x = 3245, z = 3232, walkRadius = 7, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 3247, z = 3236, walkRadius = 7, direction = Direction.WEST)

        // Goblins. Tiles, levels and npc ids are the OSRS Wiki Goblin page's own published
        // spawn pins for Lumbridge (its "Lumbridge" LocLine, all level 2) plus the single
        // level 5 in Lumbridge Swamp. Combat stats and drops for every id below live in
        // org.alter.plugins.content.npcs.goblin.
        //
        // That LocLine is one flat list of 121 pins that runs the whole way west past
        // Draynor Manor. Only the 65 that actually stand in Lumbridge are here; the other
        // 56 are in areas/draynor/spawns, split by where they stand rather than by which
        // LocLine happens to carry them.
        //
        // Which of the thirty level 2 ids stands on which tile is not published - the pins
        // carry coordinates only - so they are dealt round-robin over the tiles in the
        // wiki's own listing order. That is a stable assignment, not an observed fact, but
        // it does reproduce the thing that actually reads as right in game: a field of
        // goblins in a mix of mail colours rather than thirty clones of one id.

        // East of Lumbridge - both banks of the river, south to the Al Kharid border (50).
        spawnNpc(npc = "npc.goblin_3028", x = 3241, z = 3244, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3029", x = 3241, z = 3251, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3030", x = 3242, z = 3242, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3031", x = 3244, z = 3245, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3032", x = 3244, z = 3247, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3033", x = 3244, z = 3251, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3034", x = 3246, z = 3235, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3035", x = 3246, z = 3236, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3036", x = 3246, z = 3241, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3037", x = 3246, z = 3244, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3038", x = 3246, z = 3245, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3039", x = 3247, z = 3240, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3040", x = 3247, z = 3245, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3041", x = 3247, z = 3247, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3042", x = 3248, z = 3229, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3043", x = 3248, z = 3241, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3044", x = 3249, z = 3243, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3051", x = 3249, z = 3252, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3052", x = 3249, z = 3256, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3053", x = 3250, z = 3227, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3054", x = 3250, z = 3228, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5195", x = 3250, z = 3238, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_5196", x = 3250, z = 3256, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5197", x = 3251, z = 3243, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_5198", x = 3251, z = 3252, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5199", x = 3252, z = 3228, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_5200", x = 3252, z = 3246, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5201", x = 3253, z = 3234, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_5202", x = 3253, z = 3241, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5203", x = 3253, z = 3250, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3028", x = 3255, z = 3222, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3029", x = 3255, z = 3245, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3030", x = 3255, z = 3247, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3031", x = 3255, z = 3249, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3032", x = 3255, z = 3252, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3033", x = 3256, z = 3226, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3034", x = 3258, z = 3220, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3035", x = 3258, z = 3228, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3036", x = 3258, z = 3245, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3037", x = 3258, z = 3249, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3038", x = 3259, z = 3223, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3039", x = 3259, z = 3230, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3040", x = 3260, z = 3229, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3041", x = 3260, z = 3233, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3042", x = 3260, z = 3237, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3043", x = 3260, z = 3240, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3044", x = 3262, z = 3218, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3051", x = 3263, z = 3220, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3052", x = 3263, z = 3228, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3053", x = 3263, z = 3243, walkRadius = 8, direction = Direction.EAST)

        // West of Lumbridge castle, along the Draynor road (12).
        spawnNpc(npc = "npc.goblin_3054", x = 3183, z = 3244, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5195", x = 3183, z = 3246, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_5196", x = 3185, z = 3244, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5197", x = 3185, z = 3246, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_5198", x = 3187, z = 3244, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5199", x = 3187, z = 3246, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_5200", x = 3192, z = 3245, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5201", x = 3194, z = 3248, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_5202", x = 3197, z = 3250, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5203", x = 3198, z = 3255, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3028", x = 3202, z = 3253, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3029", x = 3206, z = 3252, walkRadius = 8, direction = Direction.EAST)

        // North of Lumbridge, by the cow and sheep fields (3).
        spawnNpc(npc = "npc.goblin_3030", x = 3215, z = 3276, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3031", x = 3217, z = 3278, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3032", x = 3224, z = 3262, walkRadius = 8, direction = Direction.NORTH)

        // Lumbridge Swamp (1). The only non-level-2 goblin anywhere near Lumbridge:
        // 12 hitpoints, stab, and the better of the two drop tables.
        spawnNpc(npc = "npc.goblin_3045", x = 3167, z = 3205, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.drunken_man", x = 3230, z = 3241, walkRadius = 3, direction = Direction.EAST)
        spawnNpc(npc = "npc.man_3109", x = 3228, z = 3239, walkRadius = 3, direction = Direction.WEST)
        spawnNpc(npc = "npc.woman_3112", x = 3229, z = 3238, walkRadius = 3, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.man_3014", x = 3231, z = 3236, walkRadius = 3, direction = Direction.WEST)
        spawnNpc(npc = "npc.zombie_rat_3970", x = 3246, z = 3198, walkRadius = 5, direction = Direction.WEST)
        spawnNpc(npc = "npc.zombie_rat", x = 3239, z = 3198, walkRadius = 5, direction = Direction.WEST)

        // Item spawns
        spawnItem(item = "item.logs", amount = 1, x = 3205, z = 3224, height = 2)
        spawnItem(item = "item.logs", amount = 1, x = 3205, z = 3226, height = 2)
        spawnItem(item = "item.logs", amount = 1, x = 3208, z = 3225, height = 2)
        spawnItem(item = "item.logs", amount = 1, x = 3209, z = 3224, height = 2)
        spawnItem(item = "item.mind_rune", amount = 1, x = 3206, z = 3208)
        spawnItem(item = "item.bronze_arrow", amount = 1, x = 3205, z = 3227)
        spawnItem(item = "item.bronze_dagger", amount = 1, x = 3213, z = 3216, height = 1)
        spawnItem(item = "item.knife", amount = 1, x = 3205, z = 3212)
        spawnItem(item = "item.knife", amount = 1, x = 3224, z = 3202)
        spawnItem(item = "item.pot", amount = 1, x = 3209, z = 3214)
        spawnItem(item = "item.bowl", amount = 1, x = 3208, z = 3214)
        spawnItem(item = "item.jug", amount = 1, x = 3211, z = 3212)

    }
}
