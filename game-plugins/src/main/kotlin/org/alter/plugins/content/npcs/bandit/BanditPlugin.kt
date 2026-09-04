package org.alter.plugins.content.npcs.bandit

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
 * Makes bandits aggressive, worth Slayer experience and worth killing. [BanditSpawnPlugin] puts them
 * on the map; stats live in `data/cfg/npcs/monsterStats.json`, the tables in [BanditDrops], ids and
 * tiles in [Bandits].
 *
 * ## What this changes, per bandit
 *
 * Layered onto the def the engine already built, the pattern
 * `areas/wilderness/bosses/WildernessBossPlugin` documents:
 *
 * - **`respawnDelay`** 25 -> [Bandits.RESPAWN_CYCLES]. Read by `NpcDeathAction` at death, so
 *   patching it at spawn is enough.
 * - **`aggressiveRadius`**, **`aggroTargetDelay`** and **`aggressiveTimer`**. Both versions are
 *   `aggressive = Yes` - the page opens by warning about it - and all three fields are needed:
 *   without a radius `NpcAggroPlugin` never arms its timer, and without a non-zero timer it arms one
 *   that refuses every target.
 * - **`slayerXp`**, which `Slayer.onKill` reads off the dying npc and is the only place Slayer
 *   experience comes from.
 *
 * `slayerXp` is set even though there is **no assignable bandit task on this server**: the wiki's
 * `assignedby` is `krystilia` alone, and Krystilia does not exist here (see
 * `content/npcs/chaosdruid`, which met the same gap). The field costs nothing, is what the page
 * publishes, and means the day a Wilderness Slayer master is built the experience is already right.
 *
 * The patch is applied in an `onNpcSpawn` hook rather than once at load because a bandit that dies
 * is re-defaulted: `NpcDeathAction` calls `World.setNpcDefaults` and then `executeNpcSpawn` again,
 * which would otherwise hand the respawned bandit back its unpatched def. Per-npc spawn hooks run
 * before global ones, so both `NpcAggroPlugin` and
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] see the patched version.
 *
 * ## Drops
 *
 * Rolled here rather than through the combat DSL's `drops { }` block, for the reason every monster
 * package in this tree repeats: that block builds a `LootTable` which `NpcDeathAction` never rolls,
 * so a bandit killed before this file existed dropped nothing - not even the guaranteed bones.
 *
 * The looting bag is published **without** the "only in the Wilderness" note every other monster in
 * this tree carries on it, which makes sense: both places a bandit stands are already in the
 * Wilderness. It is rolled unconditionally rather than gated on the killer's position, because that
 * is what the page says.
 */
class BanditPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Bandits.VARIANTS.forEach { variant ->
            onNpcSpawn(variant.npcKey) {
                npc.combatDef =
                    npc.combatDef.copy(
                        respawnDelay = Bandits.RESPAWN_CYCLES,
                        aggressiveRadius = Bandits.AGGRO_RADIUS,
                        aggroTargetDelay = Bandits.AGGRO_SEARCH_DELAY,
                        aggressiveTimer = Bandits.AGGRO_TIMER,
                        slayerXp = variant.slayerXp,
                    )
            }

            onNpcDeath(variant.npcKey) { onDeath(npc, variant) }
        }
    }

    /** Guaranteed bones, then one roll on the version's table, then the tertiaries. */
    private fun onDeath(
        npc: Npc,
        variant: BanditVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)

        BanditDrops.tableFor(variant.dropTable).roll(world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }

        if (world.chance(1, variant.lootingBagOneIn)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }

        if (variant.dropTable == BanditTableId.LEVEL_130 && world.chance(1, BanditDrops.HARD_CLUE_ONE_IN)) {
            loot.add(getRSCM("item.clue_scroll_hard") to 1)
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }
}
