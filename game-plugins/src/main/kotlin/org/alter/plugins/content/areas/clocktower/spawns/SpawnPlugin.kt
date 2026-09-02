package org.alter.plugins.content.areas.clocktower.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The monsters of the Clock Tower Dungeon, beneath the clock tower south of Ardougne:
 * 14 goblins, 15 giant spiders and 12 dungeon rats, every published pin from all three
 * wiki pages' "Clock Tower Dungeon" LocLines. Mapsquare 40_150 is in this project's
 * decrypted cache, so these tiles have real collision data.
 *
 * The three populations barely overlap, which is what makes the dungeon read as a route
 * rather than a soup. The goblins hold the far west chamber and the south-east corner
 * (x2562-2571 and 2599-2610); the rats run through the middle-north (x2573-2591); the
 * spiders fill the east (x2602-2609). Walking through it you meet each in turn.
 *
 * **Every spider here is level 2** (`levels = 2` on its LocLine, id 3017) - so unlike the
 * goblins there was no per-tile level to guess. They are aggressive at that level like
 * every other giant spider, which the passive goblins next door are not.
 *
 * **The wiki lists several levels for this one tile set and does not say which tile is
 * which.** Its LocLine carries `levels = 2, 5, 13` for every pin together. Rather than
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
        // The western chamber (9).
        spawnNpc(npc = "npc.goblin_3028", x = 2562, z = 9657, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3045", x = 2562, z = 9659, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3046", x = 2563, z = 9661, walkRadius = 8, direction = Direction.SOUTH) // level 13
        spawnNpc(npc = "npc.goblin_3029", x = 2564, z = 9653, walkRadius = 8, direction = Direction.WEST) // level 2
        spawnNpc(npc = "npc.goblin_3073", x = 2565, z = 9655, walkRadius = 8, direction = Direction.NORTH) // level 5
        spawnNpc(npc = "npc.goblin_3046", x = 2566, z = 9626, walkRadius = 8, direction = Direction.EAST) // level 13
        spawnNpc(npc = "npc.goblin_3030", x = 2567, z = 9653, walkRadius = 8, direction = Direction.SOUTH) // level 2
        spawnNpc(npc = "npc.goblin_3074", x = 2569, z = 9633, walkRadius = 8, direction = Direction.WEST) // level 5
        spawnNpc(npc = "npc.goblin_3046", x = 2571, z = 9653, walkRadius = 8, direction = Direction.NORTH) // level 13

        // The eastern chamber (5).
        spawnNpc(npc = "npc.goblin_3028", x = 2599, z = 9626, walkRadius = 8, direction = Direction.EAST) // level 2
        spawnNpc(npc = "npc.goblin_3045", x = 2601, z = 9627, walkRadius = 8, direction = Direction.SOUTH) // level 5
        spawnNpc(npc = "npc.goblin_3046", x = 2605, z = 9621, walkRadius = 8, direction = Direction.WEST) // level 13
        spawnNpc(npc = "npc.goblin_3029", x = 2607, z = 9621, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3073", x = 2610, z = 9620, walkRadius = 8, direction = Direction.EAST) // level 5

        // Giant spiders, filling the middle of the dungeon between the two goblin
        // groups (15). All level 2.
        spawnNpc(npc = "npc.giant_spider_3017", x = 2602, z = 9640, walkRadius = 7, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2603, z = 9635, walkRadius = 7, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2603, z = 9638, walkRadius = 7, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2605, z = 9637, walkRadius = 7, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2605, z = 9639, walkRadius = 7, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2606, z = 9635, walkRadius = 7, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2606, z = 9646, walkRadius = 7, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2607, z = 9637, walkRadius = 7, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2607, z = 9641, walkRadius = 7, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2607, z = 9644, walkRadius = 7, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2607, z = 9648, walkRadius = 7, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2608, z = 9635, walkRadius = 7, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2608, z = 9642, walkRadius = 7, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2609, z = 9639, walkRadius = 7, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_spider_3017", x = 2609, z = 9643, walkRadius = 7, direction = Direction.SOUTH)

        // Dungeon rats (12). Two LocLines on the wiki, and the split between them is
        // published: eleven marked `dropversion=Normal` through the eastern half, and a
        // single `Full tail` one - the wiki gives it its own red pin - alone in the west.
        // That last rat is the only dungeon rat in the dungeon with its own drop table,
        // the only one that drops raw rat meat, and one of just two in the game (the other
        // stands in the Goblin Cave).
        //
        // Which of the Normal eleven are the size 1 small rats and which are the size 2
        // short-tailed ones is **not** published - the pins carry no model - so the two are
        // dealt alternately, the same even-split treatment used everywhere else here. The
        // wiki does confirm small ones are present: its Rag and Bone Man I note says giant
        // rat bones come "only from the small dungeon rats in the Clock Tower Dungeon".
        // The two are identical in combat and drops and differ only in respawn, 25s
        // against 50s.
        spawnNpc(npc = "npc.dungeon_rat_3607", x = 2579, z = 9655, walkRadius = 6, direction = Direction.NORTH) // small
        spawnNpc(npc = "npc.dungeon_rat_2866", x = 2581, z = 9656, walkRadius = 6, direction = Direction.EAST) // short tail
        spawnNpc(npc = "npc.dungeon_rat_3608", x = 2581, z = 9659, walkRadius = 6, direction = Direction.SOUTH) // small
        spawnNpc(npc = "npc.dungeon_rat_2867", x = 2583, z = 9659, walkRadius = 6, direction = Direction.WEST) // short tail
        spawnNpc(npc = "npc.dungeon_rat_3609", x = 2584, z = 9656, walkRadius = 6, direction = Direction.NORTH) // small
        spawnNpc(npc = "npc.dungeon_rat_2866", x = 2586, z = 9659, walkRadius = 6, direction = Direction.EAST) // short tail
        spawnNpc(npc = "npc.dungeon_rat_3607", x = 2587, z = 9656, walkRadius = 6, direction = Direction.SOUTH) // small
        spawnNpc(npc = "npc.dungeon_rat_2867", x = 2589, z = 9654, walkRadius = 6, direction = Direction.WEST) // short tail
        spawnNpc(npc = "npc.dungeon_rat_3608", x = 2589, z = 9658, walkRadius = 6, direction = Direction.NORTH) // small
        spawnNpc(npc = "npc.dungeon_rat_2866", x = 2591, z = 9656, walkRadius = 6, direction = Direction.EAST) // short tail
        spawnNpc(npc = "npc.dungeon_rat_3609", x = 2591, z = 9659, walkRadius = 6, direction = Direction.SOUTH) // small

        // The full-tail rat, in the western part of the dungeon.
        spawnNpc(npc = "npc.dungeon_rat", x = 2573, z = 9630, walkRadius = 6, direction = Direction.SOUTH) // full tail
    }
}
