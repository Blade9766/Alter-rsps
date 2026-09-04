package org.alter.plugins.content.npcs.dwarf

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
 * Makes dwarves worth Slayer experience and worth killing. [DwarfSpawnPlugin] puts them on the map;
 * stats live in `data/cfg/npcs/monsterStats.json`, the table in [DwarfDrops], ids and tiles in
 * [Dwarves].
 *
 * ## What this changes, per dwarf
 *
 * Two fields layered onto the def the engine already built, the pattern
 * `areas/wilderness/bosses/WildernessBossPlugin` documents:
 *
 * - **`slayerXp`**, which `Slayer.onKill` reads off the dying npc and is the only place Slayer
 *   experience comes from. Zeroed before this, so `data/cfg/slayer/tasks.json`'s `Dwarves` category
 *   - which Turael and Spria have always been able to assign - could be handed out and completed
 *   for nothing at all.
 * - **`respawnDelay`**, only where the wiki publishes one that differs from the engine default.
 *
 * There is deliberately **no aggression patch**. Every version of this monster is
 * `aggressive = No`, so unlike `content/npcs/zombie` there is nothing here to arm.
 *
 * The patch is applied in an `onNpcSpawn` hook rather than once at load because a dwarf that dies
 * is re-defaulted: `NpcDeathAction` calls `World.setNpcDefaults` and then `executeNpcSpawn` again,
 * which would otherwise hand the respawned dwarf back its unpatched def.
 *
 * ## Drops
 *
 * Rolled here rather than through the combat DSL's `drops { }` block, for the reason every monster
 * package in this tree repeats: that block builds a `LootTable` which `NpcDeathAction` never rolls,
 * so a dwarf killed before this file existed dropped nothing - not even the guaranteed bones.
 */
class DwarfPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Dwarves.VARIANTS.forEach { variant ->
            onNpcSpawn(variant.npcKey) {
                npc.combatDef =
                    npc.combatDef.copy(
                        respawnDelay = variant.respawnCycles,
                        slayerXp = variant.slayerXp,
                    )
            }

            onNpcDeath(variant.npcKey) { onDeath(npc, variant) }
        }
    }

    /** Guaranteed bones, then one roll on the shared table, then the beginner clue. */
    private fun onDeath(
        npc: Npc,
        variant: DwarfVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)

        DwarfDrops.TABLE.roll(world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }

        // "Not dropped by the level 20 variant" - the page's own exemption, and the only thing
        // that makes one dwarf's loot differ from another's.
        if (variant.npcKey != Dwarves.STANDARD_20 && world.chance(1, DwarfDrops.BEGINNER_CLUE_ONE_IN)) {
            loot.add(getRSCM("item.clue_scroll_beginner") to 1)
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }
}
