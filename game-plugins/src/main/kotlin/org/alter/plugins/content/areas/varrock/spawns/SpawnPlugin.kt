package org.alter.plugins.content.areas.varrock.spawns

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Ambient spawns for Varrock Square, to accompany the named shopkeeper plugins under
 * areas/varrock/npcs/stores, plus the level 5 goblins on the roads outside the city and every
 * permanent ground item in and around the city.
 *
 * The item spawns all come from each item's own `{{ItemSpawnLine}}` row rather than from the Varrock
 * article, which describes them in prose and publishes no coordinates - the same sourcing the
 * Edgeville and Falador spawns needed. Two places where the article and the rows disagree, with the
 * rows winning both times:
 *
 * - the article says "225 coins" in the west bank basement; the seven rows there sum to 232;
 * - the article says six logs, four of them in the sawmill; the rows give nine sawmill tiles and two
 *   at Old Man Yarlo's, so eleven.
 *
 * Several of these tiles are blocked - the pot sits on a table, and the bank basement spawns are
 * behind a wall and meant to need telekinetic grab. That is correct and deliberate: a ground item
 * does not need a standable tile, and putting them somewhere reachable would quietly delete the
 * puzzle.
 *
 * Note that `data/cfg/spawns/item_spawns.yml` also lists most of these tiles. **Nothing reads that
 * file** - it is gitignored, has no loader anywhere in the tree, and is an upstream leftover; the
 * only live path is [spawnItem], which is why the Edgeville, Falador and Ardougne spawn plugins call
 * it too. These lines are not duplicates of a working config.
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

        // --- Ground items -----------------------------------------------------------------------

        // Varrock west bank basement. Walled off from the stairs, which is the point: the gold bar,
        // gold ore, brass necklace and ruby ring are telekinetic-grab spawns.
        spawnItem(item = "item.gold_bar", amount = 1, x = 3192, z = 9822)
        spawnItem(item = "item.gold_ore", amount = 1, x = 3195, z = 9821)
        spawnItem(item = "item.ruby_ring", amount = 1, x = 3196, z = 9822)
        spawnItem(item = "item.brass_necklace", amount = 1, x = 3191, z = 9820)
        spawnItem(item = "item.coins_995", amount = 42, x = 3188, z = 9820)
        spawnItem(item = "item.coins_995", amount = 26, x = 3188, z = 9819)
        spawnItem(item = "item.coins_995", amount = 35, x = 3189, z = 9819)
        spawnItem(item = "item.coins_995", amount = 56, x = 3190, z = 9819)
        spawnItem(item = "item.coins_995", amount = 66, x = 3191, z = 9821)
        spawnItem(item = "item.coins_995", amount = 4, x = 3195, z = 9820)
        spawnItem(item = "item.coins_995", amount = 3, x = 3195, z = 9834)

        // Varrock Palace: two buckets upstairs by the kitchen stairs, a pie dish below, and the
        // loose change in the bear cage.
        spawnItem(item = "item.bucket", amount = 1, x = 3221, z = 3497, height = 1)
        spawnItem(item = "item.bucket", amount = 1, x = 3222, z = 3491, height = 1)
        spawnItem(item = "item.pie_dish", amount = 1, x = 3222, z = 3494)
        spawnItem(item = "item.coins_995", amount = 3, x = 3228, z = 3504)
        spawnItem(item = "item.coins_995", amount = 4, x = 3232, z = 3500)

        // South-east Varrock: the building next to Aubury's, and Ye olde Tea Shoppe.
        spawnItem(item = "item.leather_body", amount = 1, x = 3247, z = 3407)
        spawnItem(item = "item.jug", amount = 1, x = 3272, z = 3409)

        // The pubs: a pot on the Blue Moon Inn's kitchen table, thread in the Jolly Boar's kitchen.
        spawnItem(item = "item.pot", amount = 1, x = 3232, z = 3399)
        spawnItem(item = "item.thread", amount = 1, x = 3286, z = 3491)

        // East of the Grand Exchange. The only Varrock spawn with a published respawn time of its
        // own; every other row here is silent, so they keep the 50-cycle default.
        spawnItem(item = "item.body_rune", amount = 2, x = 3194, z = 3495, respawnCycles = 45)

        // Old Man Yarlo's house, east of the Blue Moon Inn.
        spawnItem(item = "item.logs", amount = 1, x = 3244, z = 3398)
        spawnItem(item = "item.logs", amount = 1, x = 3245, z = 3398)

        // The sawmill (Lumberyard) north-east of the city.
        listOf(
            3302 to 3502, 3303 to 3503, 3310 to 3504, 3298 to 3512, 3300 to 3508,
            3301 to 3507, 3302 to 3507, 3302 to 3510, 3310 to 3503,
        ).forEach { (x, z) -> spawnItem(item = "item.logs", amount = 1, x = x, z = z) }
    }
}
