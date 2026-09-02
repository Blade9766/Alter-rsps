package org.alter.plugins.content.areas.varrock.spawns

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Ambient spawns for Varrock Square, to accompany the named shopkeeper plugins under
 * areas/varrock/npcs/stores, plus the level 5 goblins on the roads outside the city.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // Guards are NOT spawned here. They used to be - three of them, as
        // npc.guard_998/999/1000 on invented coordinates - but a cache check found all
        // three are combat level 0 with no options at all (`actions` is five nulls), so
        // they were unattackable, untalkable scenery. Varrock's real guards are ids
        // 11911-11917 and now live in content/npcs/guard, on the wiki's own 37
        // published tiles with real stats and drops.

        // Generic townsfolk to keep the square from feeling empty.
        spawnNpc(npc = "npc.man_3106", x = 3214, z = 3422, walkRadius = 10, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.woman_3111", x = 3220, z = 3412, walkRadius = 10, direction = Direction.EAST)

        // Level 5 goblins, from the OSRS Wiki Goblin page's "West of Varrock" and "Between
        // Lumbridge and Varrock" LocLines. Combat stats and drops live in
        // org.alter.plugins.content.npcs.goblin.
        //
        // These are a genuinely different monster from the level 2s filling Lumbridge,
        // Draynor and Port Sarim, not a re-skin: 12 hitpoints instead of 5, stab instead
        // of crush, a slower 6-cycle attack, +12/+12 attack and strength bonuses where the
        // level 2s carry -21/-15, and the better of the two drop tables. Only five ids
        // exist for the variant (3045, 3073-3076) and all five are used here; 3045 also
        // stands in Lumbridge Swamp, the only other level 5 on the server.

        // West of Varrock, on the road out toward Barbarian Village (5).
        spawnNpc(npc = "npc.goblin_3045", x = 3118, z = 3432, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.goblin_3073", x = 3119, z = 3444, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.goblin_3074", x = 3121, z = 3422, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.goblin_3075", x = 3126, z = 3431, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.goblin_3076", x = 3126, z = 3450, walkRadius = 8, direction = Direction.NORTH)

        // Between Lumbridge and Varrock, on the east road (1).
        spawnNpc(npc = "npc.goblin_3045", x = 3259, z = 3338, walkRadius = 8, direction = Direction.EAST)
    }
}
