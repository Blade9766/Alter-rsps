package org.alter.plugins.content.npcs.outlaw

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
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings
import org.alter.rscm.RSCM.getRSCM

/**
 * The ten outlaws west of the Grand Exchange: spawns, respawn and drops. Everything else is in
 * [Outlaws].
 *
 * One plugin rather than three files: one location, ten pins, one table.
 *
 * No aggression is set - `aggressive = No` on every one of the ten ids - and no Slayer experience,
 * because the page publishes neither `slayxp` nor `cat`. There is no outlaw Slayer category, so
 * leaving it at zero is the fact rather than a gap.
 */
class OutlawPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Outlaws.NPC_KEYS.forEach { npcKey ->
            onNpcSpawn(npcKey) {
                npc.combatDef = npc.combatDef.copy(respawnDelay = Outlaws.RESPAWN_CYCLES)
            }

            onNpcDeath(npcKey) { onDeath(npc) }
        }

        val dealer = SpawnDealer()
        Outlaws.TILES.forEachIndexed { index, (x, z) ->
            spawnNpc(
                npc = dealer.next(Outlaws.NPC_KEYS),
                x = x,
                z = z,
                walkRadius = Outlaws.WALK_RADIUS,
                direction = SpawnFacings.at(index),
            )
        }
    }

    /** Guaranteed bones, then one roll on the table. There are no tertiaries on this page. */
    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)
        Outlaws.TABLE.roll(world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }
}
