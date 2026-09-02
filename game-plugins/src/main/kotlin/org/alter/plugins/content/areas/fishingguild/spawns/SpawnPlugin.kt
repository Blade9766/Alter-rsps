package org.alter.plugins.content.areas.fishingguild.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The monsters around the Fishing Guild: 27 goblins and a rat.
 *
 * The goblins come from the OSRS Wiki Goblin page's two LocLines for the area: "West of Fishing Guild" (23 pins, the long strip of coast running north from
 * the guild) and "Outside Fishing Guild" (4 pins, right by the entrance). All level 2,
 * all members. Mapsquares 40_53 and 40_54 are in this project's decrypted cache.
 *
 * **The two lines differ in a way the wiki states outright, and it is the whole reason
 * they are wired differently here.** "West of Fishing Guild" carries
 * `dropversion = Drop table 1,Drop table 2`; "Outside Fishing Guild" carries
 * `Drop table 1` alone. Drop table 2's own blurb names the first of those explicitly -
 * "some level 2 goblins holding weapons and/or shields in the Goblin Cave and west of the
 * Fishing Guild" - so the western strip mixes armed and unarmed goblins and the four by
 * the entrance are all unarmed.
 *
 * That makes this the cleanest evidence on the page for how to read a doubled
 * `dropversion`: two adjacent locations, identical level, and the table list is the only
 * thing separating them. `areas/goblincave` reasons the same way from the same signal;
 * the dungeon files with a `2, 5, 13` level mix deliberately do *not*, because there the
 * second table is already explained by the level 5s.
 *
 * Which western tiles are armed is still unpublished, so they are dealt alternately in
 * the wiki's listing order - an even split, documented as a deliberate choice rather than
 * an invented ratio. Armed goblins have attack 3 and defence 4 against the unarmed ones'
 * 1 and 1, and roll the better table, so the split is a real mechanical difference and
 * not decoration. Each spawn line says which it is.
 *
 * `Goblins.CAVE_SENTRY_ID` is deliberately absent from the armed ids used here: it is
 * reserved for the one aggressive goblin in the Goblin Cave. Every goblin in this file is
 * passive, per `aggressive = No` on the wiki.
 *
 * Combat stats, animations and drops live in `org.alter.plugins.content.npcs.goblin`.
 *
 * **The rat is the only other monster the wiki places anywhere on this surface.** Its own
 * page gives the Fishing Guild a single level 1 pin inside the guild grounds, and that is
 * the whole of it - there are no bats, spiders, chickens or giant anything out here. The
 * giant bats and the rats people associate with this corner of the map are all underground
 * in the Goblin Cave east of the guild, which `areas/goblincave` covers.
 *
 * One near-miss worth recording so it is not chased again: the Chicken page does carry a
 * pin at 2622,3393, right beside the goblins outside the guild entrance, but its LocLine
 * is titled "Goblin Cave when searching boxes or crates" - the wiki is pinning the cave
 * entrance on the surface map for a monster that only ever spawns from searching the
 * scenery inside. That behaviour is already implemented in
 * `areas/goblincave/objs/SearchBoxesPlugin`, so there is no spawn to add for it here.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // West of the guild (23), alternating unarmed and armed.
        spawnNpc(npc = "npc.goblin_3028", x = 2563, z = 3398, walkRadius = 8, direction = Direction.NORTH) // unarmed
        spawnNpc(npc = "npc.goblin_5193", x = 2563, z = 3416, walkRadius = 8, direction = Direction.EAST) // armed
        spawnNpc(npc = "npc.goblin_3029", x = 2563, z = 3439, walkRadius = 8, direction = Direction.SOUTH) // unarmed
        spawnNpc(npc = "npc.goblin_5204", x = 2564, z = 3407, walkRadius = 8, direction = Direction.WEST) // armed
        spawnNpc(npc = "npc.goblin_3030", x = 2564, z = 3456, walkRadius = 8, direction = Direction.NORTH) // unarmed
        spawnNpc(npc = "npc.goblin_5205", x = 2566, z = 3453, walkRadius = 8, direction = Direction.EAST) // armed
        spawnNpc(npc = "npc.goblin_3031", x = 2567, z = 3428, walkRadius = 8, direction = Direction.SOUTH) // unarmed
        spawnNpc(npc = "npc.goblin_5206", x = 2568, z = 3403, walkRadius = 8, direction = Direction.WEST) // armed
        spawnNpc(npc = "npc.goblin_3032", x = 2568, z = 3433, walkRadius = 8, direction = Direction.NORTH) // unarmed
        spawnNpc(npc = "npc.goblin_5207", x = 2569, z = 3410, walkRadius = 8, direction = Direction.EAST) // armed
        spawnNpc(npc = "npc.goblin_3033", x = 2569, z = 3442, walkRadius = 8, direction = Direction.SOUTH) // unarmed
        spawnNpc(npc = "npc.goblin_5208", x = 2571, z = 3442, walkRadius = 8, direction = Direction.WEST) // armed
        spawnNpc(npc = "npc.goblin_3034", x = 2572, z = 3396, walkRadius = 8, direction = Direction.NORTH) // unarmed
        spawnNpc(npc = "npc.goblin_5193", x = 2572, z = 3420, walkRadius = 8, direction = Direction.EAST) // armed
        spawnNpc(npc = "npc.goblin_3035", x = 2574, z = 3409, walkRadius = 8, direction = Direction.SOUTH) // unarmed
        spawnNpc(npc = "npc.goblin_5204", x = 2576, z = 3436, walkRadius = 8, direction = Direction.WEST) // armed
        spawnNpc(npc = "npc.goblin_3036", x = 2578, z = 3400, walkRadius = 8, direction = Direction.NORTH) // unarmed
        spawnNpc(npc = "npc.goblin_5205", x = 2579, z = 3413, walkRadius = 8, direction = Direction.EAST) // armed
        spawnNpc(npc = "npc.goblin_3037", x = 2581, z = 3395, walkRadius = 8, direction = Direction.SOUTH) // unarmed
        spawnNpc(npc = "npc.goblin_5206", x = 2583, z = 3435, walkRadius = 8, direction = Direction.WEST) // armed
        spawnNpc(npc = "npc.goblin_3038", x = 2584, z = 3412, walkRadius = 8, direction = Direction.NORTH) // unarmed
        spawnNpc(npc = "npc.goblin_5207", x = 2584, z = 3428, walkRadius = 8, direction = Direction.EAST) // armed
        spawnNpc(npc = "npc.goblin_3039", x = 2586, z = 3425, walkRadius = 8, direction = Direction.SOUTH) // unarmed

        // Outside the guild entrance (4). Drop table 1 only, so all unarmed.
        spawnNpc(npc = "npc.goblin_3040", x = 2620, z = 3394, walkRadius = 8, direction = Direction.NORTH) // unarmed
        spawnNpc(npc = "npc.goblin_3041", x = 2621, z = 3393, walkRadius = 8, direction = Direction.EAST) // unarmed
        spawnNpc(npc = "npc.goblin_3042", x = 2623, z = 3396, walkRadius = 8, direction = Direction.SOUTH) // unarmed
        spawnNpc(npc = "npc.goblin_3043", x = 2623, z = 3397, walkRadius = 8, direction = Direction.WEST) // unarmed

        // The Fishing Guild rat (1), inside the guild grounds. Level 1, and like every
        // regular rat it drops nothing at all.
        spawnNpc(npc = "npc.rat_2854", x = 2592, z = 3416, walkRadius = 5, direction = Direction.NORTH)
    }
}
