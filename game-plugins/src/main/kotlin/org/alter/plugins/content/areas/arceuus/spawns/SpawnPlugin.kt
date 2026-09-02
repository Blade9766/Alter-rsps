package org.alter.plugins.content.areas.arceuus.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The giant bats of Arceuus town.
 *
 * All 4 published pins from the OSRS Wiki Giant bat page's "Arceuus" LocLine. Mapsquares
 * 25_57, 26_57 and 26_58 are in this project's decrypted cache. Combat stats and drops
 * live in `org.alter.plugins.content.npcs.critters`.
 *
 * **The passive Arceuus bats (id 6824)**, the version this town gives its name to. The
 * Giant bat page's own prose is the source: "the bats in Arceuus and around the Dark Altar
 * are not aggressive to players, making it safe to traverse the area and craft runes at
 * the nearby altars even at a low combat level." Nothing in the LocLine itself says which
 * version it is - see `areas/darkaltar`, which had the same problem and the same answer.
 *
 * These four and the Dark Altar's nine are the whole of the passive variant's published
 * range. Every other giant bat in the game, in this codebase and out of it, is the
 * aggressive id 2834.
 *
 * Bats only - Arceuus has no other spawns here yet.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // Across Arceuus, from the west edge to the library and church (4).
        spawnNpc(npc = "npc.giant_bat_6824", x = 1619, z = 3758, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat_6824", x = 1678, z = 3757, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat_6824", x = 1705, z = 3701, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat_6824", x = 1708, z = 3715, walkRadius = 8, direction = Direction.WEST)
    }
}
