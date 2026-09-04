package org.alter.plugins.content.npcs.ogre

import org.alter.api.ext.inWilderness
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
 * Makes ogres aggressive, worth Slayer experience and worth killing. [OgreSpawnPlugin] puts them on
 * the map; stats live in `data/cfg/npcs/monsterStats.json`, the table and tertiary rates in [Ogres].
 *
 * The patch runs in an `onNpcSpawn` hook rather than once at load because an ogre that dies is
 * re-defaulted by `NpcDeathAction`, which would otherwise hand the respawned ogre back its unpatched
 * def.
 *
 * Wiring the Slayer experience here matters more than usual: `data/cfg/slayer/tasks.json` already
 * names `Ogre` in its `Ogres` category, so the task was assignable and awarded **nothing** before
 * this file existed - `Slayer.onKill` reads `slayerXp` off the dying npc and there is nowhere else
 * it comes from.
 */
class OgrePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Ogres.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                onNpcSpawn(npcKey) {
                    npc.combatDef =
                        npc.combatDef.copy(
                            respawnDelay = variant.respawnCycles,
                            aggressiveRadius = Ogres.AGGRO_RADIUS,
                            aggroTargetDelay = Ogres.AGGRO_SEARCH_DELAY,
                            aggressiveTimer = Ogres.AGGRO_TIMER,
                            slayerXp = variant.slayerXp,
                        )
                }

                onNpcDeath(npcKey) { onDeath(npc) }
            }
        }
    }

    /** Guaranteed big bones, one roll on the mostly-empty table, then the tertiaries. */
    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.big_bones") to 1)

        Ogres.TABLE.roll(world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }

        // "Looting bags are only dropped by those found in the Wilderness" - gated on where the
        // killer stands, matching every other position-gated drop in this tree.
        if (killer.inWilderness() && world.chance(1, Ogres.LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }
        if (world.chance(1, Ogres.ENSOULED_HEAD_ONE_IN)) {
            loot.add(getRSCM("item.ensouled_ogre_head") to 1)
        }
        if (world.chance(1, Ogres.LONG_BONE_ONE_IN)) {
            loot.add(getRSCM("item.long_bone") to 1)
        }
        // 1/5012.5, a non-integer rate, so this cannot go through World.chance.
        if (world.randomDouble() < 1.0 / Ogres.CURVED_BONE_ONE_IN) {
            loot.add(getRSCM("item.curved_bone") to 1)
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }
}
