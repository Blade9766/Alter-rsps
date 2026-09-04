package org.alter.plugins.content.npcs.frog

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
 * Respawn and drops for the five frogs. [FrogSpawnPlugin] puts them on the map; everything else is
 * in [Frogs].
 *
 * No aggression is set anywhere here, because none of the five is aggressive - every version
 * publishes `aggressive = No`. The patch is therefore the smallest in this bestiary pass: a respawn
 * delay, and nothing else.
 */
class FrogPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Frogs.VARIANTS.forEach { variant ->
            onNpcSpawn(variant.npcKey) {
                npc.combatDef = npc.combatDef.copy(respawnDelay = Frogs.RESPAWN_CYCLES)
            }

            onNpcDeath(variant.npcKey) { onDeath(npc, variant) }
        }
    }

    private fun onDeath(
        npc: Npc,
        variant: FrogVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = variant.guaranteed.map { getRSCM(it) to 1 }.toMutableList()

        variant.dropTable?.let { label ->
            Frogs.BY_LABEL.getValue(label).roll(world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }
        }

        if (variant.beginnerClueOneIn > 0 && world.chance(1, variant.beginnerClueOneIn)) {
            loot.add(getRSCM("item.clue_scroll_beginner") to 1)
        }

        // Only the two giant frogs publish these two rows - they are big-bones monsters, and both
        // rates are the ones every big-bones monster in this tree shares.
        if (variant.bones) {
            if (world.chance(1, Frogs.LONG_BONE_ONE_IN)) {
                loot.add(getRSCM("item.long_bone") to 1)
            }
            // 1/5012.5, a non-integer rate, so this cannot go through World.chance.
            if (world.randomDouble() < 1.0 / Frogs.CURVED_BONE_ONE_IN) {
                loot.add(getRSCM("item.curved_bone") to 1)
            }
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }
}
