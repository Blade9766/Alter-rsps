package org.alter.plugins.content.npcs.thief

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
 * Makes thieves worth killing. [ThiefSpawnPlugin] puts them on the map; stats live in
 * `data/cfg/npcs/monsterStats.json`, the table in [ThiefDrops], ids and tiles in [Thieves].
 *
 * ## What this changes, per thief
 *
 * One field, layered onto the def the engine already built: **`respawnDelay`**, which is the only
 * thing the wiki publishes that the stat table has no column for. Two of the nine versions want 30
 * rather than 25.
 *
 * There is deliberately **no aggression patch** - every version is `aggressive = No` - and **no
 * `slayerXp`**, because the page says `slayxp = Not assigned`: thieves belong to no Slayer category
 * and `data/cfg/slayer/tasks.json` has none for them.
 *
 * The patch is applied in an `onNpcSpawn` hook rather than once at load because a thief that dies is
 * re-defaulted: `NpcDeathAction` calls `World.setNpcDefaults` and then `executeNpcSpawn` again,
 * which would otherwise hand the respawned thief back its unpatched def.
 *
 * ## Drops
 *
 * Rolled here rather than through the combat DSL's `drops { }` block, for the reason every monster
 * package in this tree repeats: that block builds a `LootTable` which `NpcDeathAction` never rolls,
 * so a thief killed before this file existed dropped nothing - not even the guaranteed bones.
 *
 * The two clue scrolls are tertiaries: independent of the table and of each other, and only on the
 * five versions that roll a table at all. The four plain versions drop exactly one thing, which is
 * what `Drops (Plain)` publishes.
 */
class ThiefPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Thieves.VARIANTS.forEach { variant ->
            onNpcSpawn(variant.npcKey) {
                npc.combatDef = npc.combatDef.copy(respawnDelay = variant.respawnCycles)
            }

            onNpcDeath(variant.npcKey) { onDeath(npc, variant) }
        }
    }

    private fun onDeath(
        npc: Npc,
        variant: ThiefVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)

        if (variant.rollsTable) {
            ThiefDrops.TABLE.roll(world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }

            if (world.chance(1, ThiefDrops.BEGINNER_CLUE_ONE_IN)) {
                loot.add(getRSCM("item.clue_scroll_beginner") to 1)
            }
            if (world.chance(1, ThiefDrops.EASY_CLUE_ONE_IN)) {
                loot.add(getRSCM("item.clue_scroll_easy") to 1)
            }
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }
}
