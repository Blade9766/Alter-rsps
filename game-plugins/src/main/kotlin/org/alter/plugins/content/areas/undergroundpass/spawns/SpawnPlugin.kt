package org.alter.plugins.content.areas.undergroundpass.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The goblins of the Underground Pass.
 *
 * The wiki splits the dungeon across **two** LocLines and both are here: the seven pins at
 * `levels = 5, 13` in mapsquare 37_150, and the three at `levels = 5` alone, further into
 * the pass in mapsquare 38_151. Both mapsquares are in this project's decrypted cache, so
 * these tiles have real collision data.
 *
 * The level-5-only group carries `dropversion = Drop table 2`, matching the level 5s in
 * the first group - there is no level 2 anywhere in this dungeon, and so no drop table 1.
 *
 * **The wiki lists several levels for the first tile set and does not say which tile is
 * which.** Its LocLine carries `levels = 5, 13` for every pin together. Rather than
 * invent a per-tile precision the source does not have, the levels are cycled evenly over
 * the tiles in the wiki's listing order - the same treatment
 * `content/npcs/darkwizard` documents for exactly this situation, and the same one
 * `areas/goblincave` uses for its armed/unarmed split. Each level then draws its own next
 * npc id from that variant's id list, so the cosmetic variety within a level is preserved
 * too. The level of each spawn is written on the line.
 *
 * These are not re-skins of each other. Level 2 has 5 hitpoints and rolls drop table 1;
 * level 5 has 12, fights with stab on a slower 6-cycle attack, and rolls table 2; level 13
 * has 16 hitpoints, attack 12, strength 13, a max hit of 2, and real positive defence
 * bonuses. Combat stats, animations and drops all live in
 * `org.alter.plugins.content.npcs.goblin`.
 *
 * The level 2s here are the **unarmed** variant. The LocLine's `dropversion` names both
 * drop tables, but that is already fully explained by the level mix - level 2 and 13 roll
 * table 1, level 5 rolls table 2 - so it is not evidence of armed goblins the way it was
 * in the Goblin Cave, where every pin was level 2 and both tables were still listed.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // Goblins, Part 2 (7), mapsquare 37_150. The wiki numbers the pass's two halves
        // on the Giant bat page below; this is the one it calls Part 2.
        spawnNpc(npc = "npc.goblin_3045", x = 2377, z = 9657, walkRadius = 8, direction = Direction.NORTH) // level 5
        spawnNpc(npc = "npc.goblin_3046", x = 2381, z = 9655, walkRadius = 8, direction = Direction.EAST) // level 13
        spawnNpc(npc = "npc.goblin_3073", x = 2384, z = 9655, walkRadius = 8, direction = Direction.SOUTH) // level 5
        spawnNpc(npc = "npc.goblin_3046", x = 2391, z = 9655, walkRadius = 8, direction = Direction.WEST) // level 13
        spawnNpc(npc = "npc.goblin_3074", x = 2392, z = 9656, walkRadius = 8, direction = Direction.NORTH) // level 5
        spawnNpc(npc = "npc.goblin_3046", x = 2396, z = 9658, walkRadius = 8, direction = Direction.EAST) // level 13
        spawnNpc(npc = "npc.goblin_3075", x = 2397, z = 9655, walkRadius = 8, direction = Direction.SOUTH) // level 5

        // Goblins, Part 1 (3), mapsquare 38_151. Level 5 throughout.
        spawnNpc(npc = "npc.goblin_3045", x = 2438, z = 9700, walkRadius = 8, direction = Direction.NORTH) // level 5
        spawnNpc(npc = "npc.goblin_3073", x = 2439, z = 9696, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3074", x = 2441, z = 9698, walkRadius = 8, direction = Direction.SOUTH) // level 5

        // Giant bats (11), from the Giant bat page's own two Underground Pass LocLines.
        // Combat level 27 and aggressive, against goblins here that are level 5 and 13 and
        // passive - by some distance the thing that will actually kill you down here.

        // Part 1 (6), mapsquare 38_151.
        spawnNpc(npc = "npc.giant_bat", x = 2468, z = 9696, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2471, z = 9693, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2485, z = 9704, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 2488, z = 9703, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 2490, z = 9698, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2491, z = 9705, walkRadius = 8, direction = Direction.EAST)

        // Part 2 (5), mapsquare 37_150.
        spawnNpc(npc = "npc.giant_bat", x = 2373, z = 9630, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 2374, z = 9635, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 2419, z = 9636, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2421, z = 9634, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2422, z = 9631, walkRadius = 8, direction = Direction.SOUTH)
    }
}
