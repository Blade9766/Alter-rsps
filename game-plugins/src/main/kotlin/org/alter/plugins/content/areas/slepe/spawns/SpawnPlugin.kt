package org.alter.plugins.content.areas.slepe.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Slepe, in eastern Morytania: the giant bats on the moors north of the town, and the chaos
 * druids on top of its church.
 *
 * All published pins for both, 6 bats and 5 druids.
 *
 * The druids are on **plane 3** - the church roof, reached by the ladder at (3742,3316) - which
 * is what the `{{FloorNumber}}` in their location text resolves to, not a guess from the pin
 * coordinates. All five tiles were checked against the region's own terrain: the roof is a
 * cross-shaped floor spanning x3733-3744, z3311-3322, and every pin lands on it.
 *
 * The bats are the normal aggressive ones, id 2834 - only Arceuus and the Dark Altar use the
 * passive version. Combat stats and drops live in `org.alter.plugins.content.npcs.critters` for
 * the bats and `org.alter.plugins.content.npcs.chaosdruid` for the druids.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // Chaos druids (5), level 13, on the church roof.
        spawnNpc(npc = "npc.chaos_druid", x = 3736, z = 3318, height = 3, walkRadius = 4, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_druid", x = 3738, z = 3314, height = 3, walkRadius = 4, direction = Direction.EAST)
        spawnNpc(npc = "npc.chaos_druid", x = 3739, z = 3313, height = 3, walkRadius = 4, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.chaos_druid", x = 3739, z = 3321, height = 3, walkRadius = 4, direction = Direction.WEST)
        spawnNpc(npc = "npc.chaos_druid", x = 3742, z = 3317, height = 3, walkRadius = 4, direction = Direction.NORTH)

        // Giant bats (6), strung west to east across the moor above the town.
        spawnNpc(npc = "npc.giant_bat", x = 3731, z = 3393, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 3743, z = 3394, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 3744, z = 3399, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 3751, z = 3401, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 3754, z = 3398, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 3759, z = 3395, walkRadius = 8, direction = Direction.EAST)
    }
}
