package org.alter.plugins.content.npcs.hellhound

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
 * Makes hellhounds aggressive, worth Slayer experience and worth killing. [HellhoundSpawnPlugin]
 * puts them on the map; stats live in `data/cfg/npcs/monsterStats.json`, the tables in
 * [HellhoundDrops], ids and tiles in [Hellhounds].
 *
 * ## What this changes, per hound
 *
 * Layered onto the def the engine already built, the pattern `content/npcs/mossgiant` documents:
 * `respawnDelay` (the wiki's own per-version 89 / 25 / 50), the three aggression fields, and
 * `slayerXp`, which `Slayer.onKill` reads off the dying npc and is the only place Slayer experience
 * comes from.
 *
 * The patch runs in an `onNpcSpawn` hook rather than once at load because a hound that dies is
 * re-defaulted: `NpcDeathAction` calls `World.setNpcDefaults` and then `executeNpcSpawn` again,
 * which would otherwise hand the respawned hound back its unpatched def.
 *
 * ## Drops
 *
 * Rolled here rather than through the combat DSL's `drops { }` block, for the reason every monster
 * package in this tree repeats: that block builds a `LootTable` which `NpcDeathAction` never rolls.
 *
 * The three versions do not share a table, so the roll is keyed off which id died rather than
 * applied uniformly - see [onDeath]. All three drop vile ashes and roll the ensouled head; only the
 * level 122 has the death rune table, and only it and the Wilderness Slayer Cave version have a
 * smouldering stone rate, at different odds.
 */
class HellhoundPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Hellhounds.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                onNpcSpawn(npcKey) {
                    npc.combatDef =
                        npc.combatDef.copy(
                            respawnDelay = variant.respawnCycles,
                            aggressiveRadius = Hellhounds.AGGRO_RADIUS,
                            aggroTargetDelay = Hellhounds.AGGRO_SEARCH_DELAY,
                            aggressiveTimer = Hellhounds.AGGRO_TIMER,
                            slayerXp = variant.slayerXp,
                        )
                }

                onNpcDeath(npcKey) { onDeath(npc, variant) }
            }
        }
    }

    /**
     * Guaranteed vile ashes, then the version's own table and tertiaries - each independent of the
     * table and of each other.
     *
     * Hellhounds drop **vile ashes in place of regular bones**, which the page's own change log
     * records ("Hellhounds now drop vile ashes in place of regular bones", 16 June 2021). There is
     * no bones row on any of the three versions.
     */
    private fun onDeath(
        npc: Npc,
        variant: HellhoundVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.vile_ashes") to 1)

        // Only the level 122 publishes a main table; see HellhoundDrops.
        if (variant.npcKeys === Hellhounds.LEVEL_122_IDS) {
            HellhoundDrops.DEATH_RUNES.roll(world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }
        }

        if (world.chance(1, HellhoundDrops.ENSOULED_HEAD_ONE_IN)) {
            loot.add(getRSCM("item.ensouled_hellhound_head") to 1)
        }

        /*
         * "Looting bags are only dropped by those found in the Wilderness" on the level 122, and
         * unconditionally on the Wilderness Slayer Cave version - which is in the Wilderness, so
         * the one position test covers both. Gated on where the *killer* stands, matching every
         * other position-gated drop in this tree: the level 122 ids stand in six places, only one
         * of which is Wilderness.
         */
        if (killer.inWilderness() && world.chance(1, HellhoundDrops.LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }

        if (rollHardClue(killer, world)) {
            loot.add(getRSCM("item.clue_scroll_hard") to 1)
        }

        val smoulderingOneIn =
            when (variant.npcKeys) {
                Hellhounds.LEVEL_122_IDS -> HellhoundDrops.SMOULDERING_STONE_ONE_IN
                Hellhounds.WILDERNESS_CAVE_IDS -> HellhoundDrops.WILDERNESS_SMOULDERING_STONE_ONE_IN
                // The God Wars version publishes no `Other` section at all.
                else -> 0
            }
        if (smoulderingOneIn > 0 && world.chance(1, smoulderingOneIn)) {
            loot.add(getRSCM("item.smouldering_stone") to 1)
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }

    /**
     * The hard clue's two published rates: 1/64 ordinarily, 1/32 when "a ring of wealth (i) is worn
     * **and** fought in the Wilderness".
     *
     * Both halves are required, which is why this checks [RingOfWealth.isImbued] rather than
     * `enhancesDropTables`: an ordinary ring of wealth still removes the shared tables' `Nothing`
     * rows, but it does not improve this clue rate, and the improvement does not apply outside the
     * Wilderness even with the imbued ring on.
     */
    private fun rollHardClue(
        killer: Player,
        world: World,
    ): Boolean {
        val oneIn =
            if (RingOfWealth.isImbued(killer) && killer.inWilderness()) {
                HellhoundDrops.HARD_CLUE_WEALTH_ONE_IN
            } else {
                HellhoundDrops.HARD_CLUE_ONE_IN
            }
        return world.chance(1, oneIn)
    }
}
