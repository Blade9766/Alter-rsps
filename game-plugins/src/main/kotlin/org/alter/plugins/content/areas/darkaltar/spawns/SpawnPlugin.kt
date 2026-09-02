package org.alter.plugins.content.areas.darkaltar.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The giant bats around the Dark Altar, north of Arceuus.
 *
 * All 9 published pins from the OSRS Wiki Giant bat page's "Dark Altar" LocLine.
 * Mapsquares 25_60, 26_60 and 27_60 are in this project's decrypted cache. Combat stats
 * and drops live in `org.alter.plugins.content.npcs.critters`.
 *
 * **These are the passive Arceuus bats (id 6824), not the normal aggressive ones**, and
 * the page says so in as many words rather than leaving it to be inferred from the map:
 * "the bats in Arceuus and around the Dark Altar are not aggressive to players, making it
 * safe to traverse the area and craft runes at the nearby altars even at a low combat
 * level."
 *
 * That sentence is the whole reason this is its own file rather than nine more lines in a
 * Kourend one. Nothing in the LocLine itself distinguishes the two versions - no version
 * field, no marker on the pins - so going by geography alone would have put nine
 * aggressive level 27 bats on the path low-level players walk to reach the blood and soul
 * altars, which is precisely what the wiki is warning it should not be.
 *
 * The `Arceuus` LocLine's own 4 pins are the same version and are not here; they belong to
 * Arceuus town, which has no area package yet.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // Around the Dark Altar and the northern Arceuus rune altars (9).
        spawnNpc(npc = "npc.giant_bat_6824", x = 1612, z = 3849, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat_6824", x = 1633, z = 3842, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat_6824", x = 1633, z = 3879, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat_6824", x = 1662, z = 3870, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat_6824", x = 1673, z = 3861, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat_6824", x = 1700, z = 3857, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat_6824", x = 1709, z = 3860, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat_6824", x = 1732, z = 3855, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat_6824", x = 1743, z = 3892, walkRadius = 8, direction = Direction.NORTH)
    }
}
