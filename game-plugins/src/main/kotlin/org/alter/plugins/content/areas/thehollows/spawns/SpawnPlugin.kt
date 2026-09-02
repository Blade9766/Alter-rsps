package org.alter.plugins.content.areas.thehollows.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The goblins of The Hollows, the cave under Mort Myre Swamp.
 *
 * All 3 published pins from the OSRS Wiki Goblin page's "The Hollows cave" LocLine -
 * one of each level, since the wiki lists three levels for exactly three tiles.
 * Mapsquare 54_153 is in this project's decrypted cache, so these tiles have real
 * collision data.
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
        // The Hollows (3).
        spawnNpc(npc = "npc.goblin_3028", x = 3465, z = 9797, walkRadius = 8, direction = Direction.NORTH) // level 2
        spawnNpc(npc = "npc.goblin_3045", x = 3466, z = 9796, walkRadius = 8, direction = Direction.EAST) // level 5
        spawnNpc(npc = "npc.goblin_3046", x = 3466, z = 9798, walkRadius = 8, direction = Direction.SOUTH) // level 13
    }
}
