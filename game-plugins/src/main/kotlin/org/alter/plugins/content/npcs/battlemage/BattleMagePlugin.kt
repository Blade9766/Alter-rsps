package org.alter.plugins.content.npcs.battlemage

import org.alter.api.EquipmentType
import org.alter.api.ext.getEquipment
import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.mechanics.aggro.defaultNpcAggressiveness
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.plugins.content.npcs.SpawnFacings
import org.alter.rscm.RSCM.getRSCM

/**
 * The three Mage Arena battle mages: spawns, aggression with its cape exemption, respawn, the god
 * spells in [BattleMageCombatStrategy] and their two drops.
 *
 * One plugin rather than the usual three files, because there are three ids, one location and two
 * drop rows. Everything else is in [BattleMages].
 *
 * ## The cape exemption
 *
 * `aggressive = Yes, unless wearing the same cape as the battle mage`. [aggroCheckFor] composes that
 * exemption *on top of* the engine's ordinary rules rather than replacing them: a player without the
 * matching cape is still subject to the aggression timer and the combat-level check, exactly as
 * before. Composing rather than replacing matters here, because at combat level 54 the default check
 * already stops these bothering anyone above 108 - which is most of who reaches the Mage Arena - and
 * dropping it would have made them aggressive to everybody.
 *
 * The mages give **no Slayer experience**: their page publishes no `slayxp` and no `cat`, which is
 * right - there is no battle mage Slayer category.
 */
class BattleMagePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        BattleMages.ALL.forEach { mage ->
            CombatConfigs.setNpcCombatStrategy(getRSCM(mage.npcKey), BattleMageCombatStrategy)

            onNpcSpawn(mage.npcKey) {
                npc.combatDef =
                    npc.combatDef.copy(
                        respawnDelay = BattleMages.RESPAWN_CYCLES,
                        aggressiveRadius = BattleMages.AGGRO_RADIUS,
                        aggroTargetDelay = BattleMages.AGGRO_SEARCH_DELAY,
                        aggressiveTimer = BattleMages.AGGRO_TIMER,
                    )
                npc.aggroCheck = aggroCheckFor(mage)
            }

            onNpcDeath(mage.npcKey) { onDeath(npc) }
        }

        BattleMages.TILES.forEachIndexed { index, (x, z) ->
            spawnNpc(
                npc = BattleMages.ALL[index % BattleMages.ALL.size].npcKey,
                x = x,
                z = z,
                height = BattleMages.PLANE,
                walkRadius = BattleMages.WALK_RADIUS,
                direction = SpawnFacings.at(index),
            )
        }
    }

    /**
     * The engine's ordinary aggressiveness, and then the cape.
     *
     * Installed in the per-npc spawn hook, which `NpcAggroPlugin`'s global hook now respects: it
     * fills in its default only when the npc has no check of its own. Before that change this
     * assignment would have been overwritten a moment later and the exemption would silently have
     * done nothing.
     */
    private fun aggroCheckFor(mage: BattleMage): (Npc, Player) -> Boolean {
        val capeId = getRSCM(mage.capeKey)
        return { npc, player ->
            player.getEquipment(EquipmentType.CAPE)?.id != capeId && defaultNpcAggressiveness(npc, player)
        }
    }

    /** Guaranteed bones, then the looting bag - the whole of both published sections. */
    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)
        if (world.chance(1, BattleMages.LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }
}
