package org.alter.plugins.content.areas.ardougne.spawns

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The permanent ground-item spawns of East Ardougne.
 *
 * Both sit on the table in Wizard Cromperty's house in the north-east corner. Coordinates
 * come from each item's own wiki `{{ItemSpawnLine}}` row rather than the East Ardougne
 * article, which mentions them in prose without tiles - the same sourcing Falador's and
 * Edgeville's spawns needed. Neither row carries a `plane=`, so both are ground level.
 *
 * East Ardougne is spawn-poor for its size: two items across the whole city, against
 * Falador's five. Its market stalls are the draw instead, and those are scenery objects
 * already handled by `data/cfg/thieving/stalls.json`.
 *
 * The chisel's other wiki row is at (2543, 3287) in West Ardougne, which this project has
 * not built, so it is not placed.
 *
 * NPC spawns live with the NPCs: shopkeepers under `areas/ardougne/npcs/stores`, the knights
 * and paladins in `content/npcs/ardougne`, and the city guards in `content/npcs/guard`.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // Wizard Cromperty's house.
        spawnItem(item = "item.chisel", amount = 1, x = 2683, z = 3318)
        spawnItem(item = "item.hammer", amount = 1, x = 2684, z = 3318)
    }
}
