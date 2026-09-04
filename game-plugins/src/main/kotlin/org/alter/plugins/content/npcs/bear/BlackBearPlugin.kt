package org.alter.plugins.content.npcs.bear

import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.plugins.content.npcs.SpawnFacings
import org.alter.rscm.RSCM.getRSCM

/**
 * The black bears: spawns, respawn, Slayer experience and their five drops.
 *
 * One plugin rather than the usual three files, because there is one npc id, thirteen pins and no
 * weighted table at all - the split `content/npcs/mossgiant` uses exists to keep 93 spawns away from
 * 128 drop rows, and there is nothing here to keep apart. Everything is in [BlackBears].
 */
class BlackBearPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onNpcSpawn(BlackBears.NPC_KEY) {
            npc.combatDef =
                npc.combatDef.copy(
                    respawnDelay = BlackBears.RESPAWN_CYCLES,
                    slayerXp = BlackBears.SLAYER_XP,
                )
        }

        onNpcDeath(BlackBears.NPC_KEY) { onDeath(npc) }

        var index = 0
        BlackBears.CAMPS.forEach { camp ->
            camp.tiles.forEach { (x, z) ->
                spawnNpc(
                    npc = BlackBears.NPC_KEY,
                    x = x,
                    z = z,
                    walkRadius = BlackBears.WALK_RADIUS,
                    direction = SpawnFacings.at(index++),
                )
            }
        }
    }

    /**
     * The three guaranteed rows, then the two tertiaries. There is no weighted table on this page at
     * all - a black bear's whole value is its fur and its meat.
     */
    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = BlackBears.GUARANTEED.map { getRSCM(it) to 1 }.toMutableList()

        if (world.chance(1, BlackBears.ENSOULED_HEAD_ONE_IN)) {
            loot.add(getRSCM("item.ensouled_bear_head") to 1)
        }
        if (world.chance(1, BlackBears.BEGINNER_CLUE_ONE_IN)) {
            loot.add(getRSCM("item.clue_scroll_beginner") to 1)
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }
}
