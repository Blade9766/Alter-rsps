package org.alter.plugins.content.npcs.demon

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
 * Makes lesser and greater demons aggressive, worth Slayer experience and worth killing.
 * [DemonSpawnPlugin] puts them on the map; stats live in `data/cfg/npcs/monsterStats.json`, the
 * tables in [DemonDrops], ids and tiles in [Demons].
 *
 * The patch runs in an `onNpcSpawn` hook rather than once at load because a demon that dies is
 * re-defaulted: `NpcDeathAction` calls `World.setNpcDefaults` and then `executeNpcSpawn` again,
 * which would otherwise hand the respawned demon back its unpatched def.
 *
 * Drops are rolled here rather than through the combat DSL's `drops { }` block, for the reason every
 * monster package in this tree repeats: that block builds a `LootTable` which `NpcDeathAction` never
 * rolls.
 */
class DemonPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Demons.VARIANTS.forEach { variant ->
            val respawn = Demons.respawnFor(variant)
            variant.npcKeys.forEach { npcKey ->
                onNpcSpawn(npcKey) {
                    npc.combatDef =
                        npc.combatDef.copy(
                            respawnDelay = respawn,
                            aggressiveRadius = Demons.AGGRO_RADIUS,
                            aggroTargetDelay = Demons.AGGRO_SEARCH_DELAY,
                            aggressiveTimer = Demons.AGGRO_TIMER,
                            slayerXp = variant.slayerXp,
                        )
                }

                onNpcDeath(npcKey) { onDeath(npc, variant) }
            }
        }
    }

    /**
     * Guaranteed vile ashes, then one roll on the version's table, then the tertiaries.
     *
     * Demons drop **vile ashes rather than bones** - there is no bones row on any version of either
     * page, and the ashes row is the whole of the `100%` section.
     */
    private fun onDeath(
        npc: Npc,
        variant: DemonVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.vile_ashes") to 1)

        DemonDrops.BY_LABEL.getValue(variant.dropTable).roll(world, RingOfWealth.enhancesDropTables(killer))
            ?.let { loot.add(it) }

        /*
         * "Looting bags are only dropped by those found in the Wilderness" on the Regular versions,
         * and unconditionally on the Wilderness Slayer Cave ones - which are in the Wilderness, so
         * one position test covers both. Gated on where the killer stands, matching every other
         * position-gated drop in this tree.
         */
        if (killer.inWilderness() && world.chance(1, DemonDrops.LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }

        val lesser = variant.size == LESSER_SIZE
        val headOneIn =
            if (lesser) DemonDrops.LESSER_ENSOULED_HEAD_ONE_IN else DemonDrops.GREATER_ENSOULED_HEAD_ONE_IN
        if (world.chance(1, headOneIn)) {
            loot.add(getRSCM("item.ensouled_demon_head") to 1)
        }

        if (lesser) {
            if (world.chance(1, DemonDrops.LESSER_CHAMPION_SCROLL_ONE_IN)) {
                loot.add(getRSCM("item.lesser_demon_champion_scroll") to 1)
            }
        } else {
            /*
             * Only the greater demon publishes a clue row - the lesser demon's Tertiary section has
             * no `DropsLineClue` at all - and its footnote names no place, so unlike the hellhound's
             * this is gated on the imbued ring alone with no Wilderness test.
             */
            val clueOneIn =
                if (RingOfWealth.isImbued(killer)) {
                    DemonDrops.GREATER_HARD_CLUE_WEALTH_ONE_IN
                } else {
                    DemonDrops.GREATER_HARD_CLUE_ONE_IN
                }
            if (world.chance(1, clueOneIn)) {
                loot.add(getRSCM("item.clue_scroll_hard") to 1)
            }
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }

    private companion object {
        /**
         * Which of the two species a variant is. Keyed off the footprint rather than a boolean,
         * because the footprint is the difference: a lesser demon is size 2 and a greater demon is
         * size 3, and `BestiaryVerify` checks that against the cache.
         */
        const val LESSER_SIZE = 2
    }
}
