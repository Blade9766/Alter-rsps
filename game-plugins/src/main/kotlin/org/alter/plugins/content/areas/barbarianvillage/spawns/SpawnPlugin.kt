package org.alter.plugins.content.areas.barbarianvillage.spawns

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The permanent ground-item spawns of Barbarian Village.
 *
 * Every tile below is the wiki's own published spawn coordinate, one spawn per tile -
 * the wiki's "quantity" column counts spawn points, not stack size, so the three beers
 * are three separate 1x spawns on the longhall tables rather than one stack of three.
 *
 * NPC spawns live with the NPCs themselves: the named villagers each spawn from their
 * own plugin under `areas/barbarianvillage/npcs`, and the barbarians and Gunthor from
 * `content/npcs/barbarian`, which also carries their combat defs.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // Longhall tables.
        spawnItem(item = "item.beer", amount = 1, x = 3077, z = 3443)
        spawnItem(item = "item.beer", amount = 1, x = 3077, z = 3439)
        spawnItem(item = "item.beer", amount = 1, x = 3080, z = 3441)
        spawnItem(item = "item.cooked_meat", amount = 1, x = 3077, z = 3441)
        spawnItem(item = "item.cooked_meat", amount = 1, x = 3080, z = 3443)

        // Spinning hut, next to the mine.
        spawnItem(item = "item.bronze_pickaxe", amount = 1, x = 3081, z = 3429)

        // Peksa's helmet shop.
        spawnItem(item = "item.pot", amount = 1, x = 3074, z = 3431)
    }
}
