package org.alter.plugins.content.areas.edgeville.spawns

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The permanent ground-item spawns of Edgeville.
 *
 * Both tiles come from each item's own wiki `{{ItemSpawnLine}}` row rather than from the
 * Edgeville article, which mentions the spawns in prose but publishes no coordinates -
 * the same sourcing the Falador spawns needed. Neither row carries a `plane=`, so both
 * are ground level.
 *
 * Edgeville is a small town by spawn count: two items, against Falador's five and
 * Barbarian Village's seven.
 *
 * NPC spawns live with the NPCs: the shopkeepers under `areas/edgeville/npcs/stores`, the
 * rest under `areas/edgeville/npcs`, and the city guards in
 * `content/npcs/guard` alongside Varrock's and Ardougne's.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // The jailhouse, east of the general store.
        spawnItem(item = "item.iron_mace", amount = 1, x = 3111, z = 3517)

        // South of the bank.
        spawnItem(item = "item.leather_gloves", amount = 1, x = 3097, z = 3486)
    }
}
