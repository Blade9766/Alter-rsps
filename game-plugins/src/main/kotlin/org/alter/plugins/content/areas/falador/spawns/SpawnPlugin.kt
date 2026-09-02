package org.alter.plugins.content.areas.falador.spawns

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The permanent ground-item spawns of Falador.
 *
 * Coordinates come from each item's own wiki `{{ItemSpawnLine}}` row rather than from the
 * Falador article, which describes these spawns in prose but publishes no tiles. That
 * matters here: the article places the bronze axe and hammer on the furnace building's
 * ground floor, while both item pages give `plane=1` - upstairs. The item pages carry the
 * actual plane and tile, so they win.
 *
 * The bronze arrow row is `2944,3332,qty:2`, a stack of two on one tile, not two separate
 * spawn points - so it is a single 2x spawn rather than the one-per-tile treatment
 * Barbarian Village's beers needed.
 *
 * NPC spawns live with the NPCs: the shopkeepers under `areas/falador/npcs/stores` and the
 * rest under `areas/falador/npcs`.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // Estate agent's building, east of the park.
        spawnItem(item = "item.spade", amount = 1, x = 2981, z = 3370)

        // Upstairs of the furnace building.
        spawnItem(item = "item.bronze_axe", amount = 1, x = 2970, z = 3376, height = 1)
        spawnItem(item = "item.hammer", amount = 1, x = 2975, z = 3368, height = 1)

        // Upstairs of Cassie's Shield Shop.
        spawnItem(item = "item.cooked_chicken", amount = 1, x = 2971, z = 3382, height = 1)

        // South-west corner of Herquin's Gems.
        spawnItem(item = "item.bronze_arrow", amount = 2, x = 2944, z = 3332)
    }
}
