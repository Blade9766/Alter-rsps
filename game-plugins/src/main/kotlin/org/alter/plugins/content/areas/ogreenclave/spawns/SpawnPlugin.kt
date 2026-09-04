package org.alter.plugins.content.areas.ogreenclave.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The monsters of the Ogre Enclave, beneath Gu'Tanoth.
 *
 * All 4 published pins.
 *
 * The greater demons, ogre chieftains, skeletons and spiders now stand at their own
 * published pins. The blue dragons and ogre shamans are still absent by design - dragonfire
 * and NPC magic respectively, both undefined in `content/npcs/dungeon`.
 *
 * The normal aggressive bats, id 2834 - only Arceuus and the Dark Altar use the passive
 * version. Combat stats and drops live in `org.alter.plugins.content.npcs.critters`.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // 4 published pins.
        spawnNpc(npc = "npc.giant_bat", x = 2608, z = 9421, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2612, z = 9416, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2619, z = 9427, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 2620, z = 9419, walkRadius = 8, direction = Direction.WEST)

        // Spiders (10), level 1.
        spawnNpc(npc = "npc.spider_3019", x = 2565, z = 9443, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.spider_3019", x = 2576, z = 9454, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.spider_3019", x = 2586, z = 9423, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.spider_3019", x = 2588, z = 9431, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.spider_3019", x = 2600, z = 9432, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.spider_3019", x = 2604, z = 9441, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.spider_3019", x = 2607, z = 9460, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.spider_3019", x = 2608, z = 9436, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.spider_3019", x = 2610, z = 9446, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.spider_3019", x = 2617, z = 9451, walkRadius = 6, direction = Direction.EAST)

        // Ogre chieftains (6), level 81.
        spawnNpc(npc = "npc.ogre_chieftain", x = 2568, z = 9432, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.ogre_chieftain", x = 2573, z = 9447, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.ogre_chieftain", x = 2579, z = 9441, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.ogre_chieftain", x = 2580, z = 9431, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.ogre_chieftain", x = 2611, z = 9454, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.ogre_chieftain", x = 2616, z = 9447, walkRadius = 6, direction = Direction.EAST)

        // Greater demons are spawned by `content/npcs/demon`, at the wiki's own five pins for this
        // dungeon. Respawning them here would double them.

        // Skeletons (3), level 22.
        spawnNpc(npc = "npc.skeleton", x = 2576, z = 9436, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.skeleton_71", x = 2579, z = 9461, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.skeleton_72", x = 2590, z = 9437, walkRadius = 6, direction = Direction.SOUTH)
    }
}
