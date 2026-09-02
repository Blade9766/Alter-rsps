package org.alter.plugins.content.areas.goblincave.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.goblin.Goblins

/**
 * The monsters of the Goblin Cave - the Plain of Mud - east of the Fishing Guild: 24
 * goblins, 19 rats, 5 dungeon rats and 6 giant bats, every published pin from all four
 * wiki pages' "Goblin Cave" LocLines. The map itself is real: mapsquare 40_153 was in this
 * project's own `xteas.json.backup` before `RemoveXteas` decrypted the maps into the cache,
 * so these tiles have genuine collision data rather than being spawns into a void.
 *
 * The cave reads as three bands. Rats and dungeon rats fill the south (z9801-9812), the
 * goblins hold the north (z9819-9853), and the giant bats cut across the east. That last
 * group matters most: at combat level 27 and aggressive they are by a wide margin the most
 * dangerous thing down here, and a low-level player who came for the level 2 goblins can
 * walk into one.
 *
 * **Armed and unarmed goblins are mixed here, and the wiki does not say which tiles are
 * which.** Its LocLine carries `dropversion = Drop table 1,Drop table 2` for this one
 * location - the only goblin location on the page that lists both - and drop table 2's
 * own blurb names the cave explicitly ("some level 2 goblins holding weapons and/or
 * shields in the Goblin Cave"). So both variants belong here, but the per-tile split is
 * unpublished. They are dealt alternately across the tiles in the wiki's listing order:
 * an even split, documented as a deliberate choice, in preference to inventing a ratio or
 * to dropping one of the two variants the wiki plainly says is present. This is the same
 * treatment `content/npcs/darkwizard` already applies where a LocLine lists several valid
 * variants for one tile set.
 *
 * The two are not cosmetic variations on each other: the armed goblins have attack 3 and
 * defence 4 against the unarmed ones' 1 and 1, and roll the distinctly better table 2.
 *
 * **[Goblins.CAVE_SENTRY_ID] is the exception to everything above** - the one aggressive
 * goblin in the game outside the God Wars Dungeons. Its combat def carries the aggro and
 * its "Halt intruder!" yell, both in `content/npcs/goblin`; what belongs here is the
 * wander range, which the wiki describes as spanning almost the entire cave. Its tile is
 * a central pin picked for that reason - the wiki does not publish where it starts, and
 * with a wander range this size the start tile barely matters.
 *
 * The cave's other way of angering a goblin - searching its stacked boxes and crates -
 * lives in `areas/goblincave/objs/SearchBoxesPlugin`, since it is behaviour on that
 * scenery rather than on the goblins.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // The sentry, roughly central, with a wander range covering most of the cave.
        spawnNpc(npc = Goblins.CAVE_SENTRY_ID, x = 2579, z = 9841, walkRadius = 18, direction = Direction.SOUTH)

        // The other 23, alternating unarmed and armed.
        spawnNpc(npc = "npc.goblin_3028", x = 2564, z = 9836, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5193", x = 2565, z = 9835, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3029", x = 2566, z = 9849, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5204", x = 2568, z = 9844, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3030", x = 2569, z = 9847, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5205", x = 2570, z = 9845, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3031", x = 2570, z = 9850, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5206", x = 2580, z = 9825, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3032", x = 2580, z = 9847, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5207", x = 2580, z = 9850, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3033", x = 2584, z = 9826, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5208", x = 2584, z = 9829, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3034", x = 2584, z = 9835, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5193", x = 2586, z = 9832, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3035", x = 2587, z = 9837, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5204", x = 2589, z = 9834, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3036", x = 2590, z = 9821, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5205", x = 2591, z = 9830, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3037", x = 2591, z = 9836, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_5206", x = 2592, z = 9819, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3038", x = 2594, z = 9826, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_5207", x = 2595, z = 9821, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3039", x = 2597, z = 9821, walkRadius = 8, direction = Direction.SOUTH)

        // Rats (19), through the southern half of the cave below the goblins. Level 1,
        // and the only monsters down here that drop nothing at all - not even bones.
        spawnNpc(npc = "npc.rat_2854", x = 2573, z = 9805, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.rat_2855", x = 2575, z = 9803, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.rat_2854", x = 2578, z = 9806, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.rat_2855", x = 2579, z = 9804, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.rat_2854", x = 2583, z = 9803, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.rat_2855", x = 2585, z = 9804, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.rat_2854", x = 2585, z = 9830, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.rat_2855", x = 2587, z = 9803, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.rat_2854", x = 2588, z = 9825, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.rat_2855", x = 2591, z = 9801, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.rat_2854", x = 2592, z = 9806, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.rat_2855", x = 2594, z = 9801, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.rat_2854", x = 2599, z = 9803, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.rat_2855", x = 2599, z = 9812, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.rat_2854", x = 2601, z = 9810, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.rat_2855", x = 2610, z = 9806, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.rat_2854", x = 2612, z = 9812, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.rat_2855", x = 2613, z = 9805, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.rat_2854", x = 2613, z = 9806, walkRadius = 6, direction = Direction.SOUTH)

        // Dungeon rats (5), mixed in among the rats. The wiki marks one of the five with
        // its own red pin and gives this LocLine `dropversion=Normal,Full tail`: that pin
        // is the second of the game's only two full-tail dungeon rats, the other being the
        // one in the Clock Tower Dungeon. It is the only rat in this cave that drops meat.
        //
        // As in the Clock Tower, which of the Normal four are size 1 and which are size 2
        // short-tails is not published, so they alternate.
        spawnNpc(npc = "npc.dungeon_rat_3607", x = 2580, z = 9804, walkRadius = 6, direction = Direction.NORTH) // small
        spawnNpc(npc = "npc.dungeon_rat_2866", x = 2585, z = 9801, walkRadius = 6, direction = Direction.EAST) // short tail
        spawnNpc(npc = "npc.dungeon_rat_3608", x = 2594, z = 9803, walkRadius = 6, direction = Direction.SOUTH) // small
        spawnNpc(npc = "npc.dungeon_rat_2867", x = 2601, z = 9801, walkRadius = 6, direction = Direction.WEST) // short tail
        spawnNpc(npc = "npc.dungeon_rat", x = 2588, z = 9806, walkRadius = 6, direction = Direction.SOUTH) // full tail

        // Giant bats (6), east and north-east. Combat level 27 and aggressive - by a long
        // way the most dangerous thing in the cave, and worth knowing before walking a
        // low-level character in for the goblins.
        spawnNpc(npc = "npc.giant_bat", x = 2596, z = 9836, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2599, z = 9837, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2606, z = 9825, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 2609, z = 9817, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 2609, z = 9823, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2612, z = 9815, walkRadius = 8, direction = Direction.EAST)
    }
}
