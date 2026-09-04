package org.alter.plugins.content.npcs.elderchaosdruid

import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.items.jewellery.RingOfWealth
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.plugins.content.npcs.SpawnFacings
import org.alter.rscm.RSCM.getRSCM

/**
 * The thirteen Elder Chaos druids of the Chaos Temple: spawns, aggression, respawn, Slayer
 * experience, the Wind Wave in [ElderChaosDruidCombatStrategy], and the whole drop table.
 *
 * One plugin rather than the usual three files, because there is one npc id and one location - the
 * split `content/npcs/mossgiant` uses exists to keep 93 spawns away from 128 drop rows, and there is
 * nothing here to keep apart. Ids, tiles, rates and the table are all in [ElderChaosDruids].
 */
class ElderChaosDruidPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        CombatConfigs.setNpcCombatStrategy(getRSCM(ElderChaosDruids.NPC_KEY), ElderChaosDruidCombatStrategy)

        onNpcSpawn(ElderChaosDruids.NPC_KEY) {
            npc.combatDef =
                npc.combatDef.copy(
                    respawnDelay = ElderChaosDruids.RESPAWN_CYCLES,
                    aggressiveRadius = ElderChaosDruids.AGGRO_RADIUS,
                    aggroTargetDelay = ElderChaosDruids.AGGRO_SEARCH_DELAY,
                    aggressiveTimer = ElderChaosDruids.AGGRO_TIMER,
                    slayerXp = ElderChaosDruids.SLAYER_XP,
                )
        }

        onNpcDeath(ElderChaosDruids.NPC_KEY) { onDeath(npc) }

        ElderChaosDruids.TILES.forEachIndexed { index, (x, z) ->
            spawnNpc(
                npc = ElderChaosDruids.NPC_KEY,
                x = x,
                z = z,
                walkRadius = ElderChaosDruids.WALK_RADIUS,
                direction = SpawnFacings.at(index),
            )
        }
    }

    /**
     * Guaranteed bones, one roll on the table - which may hand back **up to four herbs** - then the
     * tertiaries.
     *
     * `rollAll` rather than `roll`: this is the one table in the tree whose herb row is a
     * distribution rather than a single herb, and `roll` would silently keep only the first of them.
     */
    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)
        loot += ElderChaosDruids.TABLE.rollAll(world, RingOfWealth.enhancesDropTables(killer))

        if (world.chance(1, ElderChaosDruids.ENSOULED_HEAD_ONE_IN)) {
            loot.add(getRSCM("item.ensouled_chaos_druid_head") to 1)
        }
        /*
         * No `killer.inWilderness()` test, unlike every other looting bag in this tree: the Chaos
         * Temple is the only place these stand and it is in the Wilderness, so the condition the
         * other packages check would be true on every kill anyway.
         */
        if (world.chance(1, ElderChaosDruids.LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }
        val clueOneIn =
            if (RingOfWealth.isImbued(killer)) {
                ElderChaosDruids.HARD_CLUE_WEALTH_ONE_IN
            } else {
                ElderChaosDruids.HARD_CLUE_ONE_IN
            }
        if (world.chance(1, clueOneIn)) {
            loot.add(getRSCM("item.clue_scroll_hard") to 1)
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }
}
