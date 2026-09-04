package org.alter.plugins.content.npcs.rockcrab

import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.items.jewellery.RingOfWealth
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.rscm.RSCM.getRSCM

/**
 * Makes Rock Crabs aggressive, worth Slayer experience and worth killing. [RockCrabSpawnPlugin] puts
 * them on the map; everything else is in [RockCrabs].
 *
 * `data/cfg/slayer/tasks.json` already names `Rock Crab` in its `Crabs` category, so the task was
 * assignable and awarded **nothing** before this file existed - `Slayer.onKill` reads `slayerXp` off
 * the dying npc and there is nowhere else it comes from.
 */
class RockCrabPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        RockCrabs.NPC_KEYS.forEach { npcKey ->
            onNpcSpawn(npcKey) {
                npc.combatDef =
                    npc.combatDef.copy(
                        respawnDelay = RockCrabs.RESPAWN_CYCLES,
                        aggressiveRadius = RockCrabs.AGGRO_RADIUS,
                        aggroTargetDelay = RockCrabs.AGGRO_SEARCH_DELAY,
                        aggressiveTimer = RockCrabs.AGGRO_TIMER,
                        slayerXp = RockCrabs.SLAYER_XP,
                    )
            }

            onNpcDeath(npcKey) { onDeath(npc) }
        }
    }

    /** One roll on the table, then the clue. No bones - a rock crab has none. */
    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf<Pair<Int, Int>>()
        RockCrabs.TABLE.roll(world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }
        if (world.chance(1, RockCrabs.EASY_CLUE_ONE_IN)) {
            loot.add(getRSCM("item.clue_scroll_easy") to 1)
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }
}
