package org.alter.plugins.content.npcs.hobgoblin

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
 * Makes hobgoblins aggressive, worth Slayer experience and worth killing. [HobgoblinSpawnPlugin]
 * puts them on the map; stats live in `data/cfg/npcs/monsterStats.json`, the tables in
 * [HobgoblinDrops], ids and tiles in [Hobgoblins].
 *
 * ## What this changes, per hobgoblin
 *
 * Layered onto the def the engine already built, the pattern
 * `areas/wilderness/bosses/WildernessBossPlugin` documents:
 *
 * - **`aggressiveRadius`**, **`aggroTargetDelay`** and **`aggressiveTimer`**. Every version is
 *   `aggressive = Yes`, and all three fields are needed: without a radius `NpcAggroPlugin` never
 *   arms its timer, and without a non-zero timer it arms one that refuses every target.
 * - **`slayerXp`**, which `Slayer.onKill` reads off the dying npc and is the only place Slayer
 *   experience comes from. Zeroed before this, so `data/cfg/slayer/tasks.json`'s `Hobgoblins`
 *   category - which shipped with an **empty** monster list, and which
 *   `SlayerService.markAvailable` therefore read as "not assignable" - was doubly dead. Both halves
 *   are fixed: the list now names the monsters and the experience is real.
 *
 * **`respawnDelay` is deliberately untouched.** The infobox has no `respawn` field at all, so there
 * is nothing published to apply and the engine's own default of 25 stands.
 *
 * The patch is applied in an `onNpcSpawn` hook rather than once at load because a hobgoblin that
 * dies is re-defaulted: `NpcDeathAction` calls `World.setNpcDefaults` and then `executeNpcSpawn`
 * again, which would otherwise hand the respawned hobgoblin back its unpatched def. Per-npc spawn
 * hooks run before global ones, so both `NpcAggroPlugin` and
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] see the patched version.
 *
 * ## What is not modelled
 *
 * The God Wars Dungeon version's aggression is published as "Yes, **unless wearing a
 * Bandos-affiliated item**". That is the dungeon-wide god-faction rule, which belongs to a God Wars
 * Dungeon package rather than to this one - there is nothing anywhere in this codebase that knows
 * what a Bandos-affiliated item is, and inventing a list here would put the rule in the wrong place.
 * It is aggressive to everyone until that exists.
 *
 * ## Drops
 *
 * Rolled here rather than through the combat DSL's `drops { }` block, for the reason every monster
 * package in this tree repeats: that block builds a `LootTable` which `NpcDeathAction` never rolls,
 * so a hobgoblin killed before this file existed dropped nothing - not even the guaranteed bones.
 */
class HobgoblinPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Hobgoblins.VARIANTS.forEach { variant ->
            onNpcSpawn(variant.npcKey) {
                npc.combatDef =
                    npc.combatDef.copy(
                        aggressiveRadius = Hobgoblins.AGGRO_RADIUS,
                        aggroTargetDelay = Hobgoblins.AGGRO_SEARCH_DELAY,
                        aggressiveTimer = Hobgoblins.AGGRO_TIMER,
                        slayerXp = variant.slayerXp,
                    )
            }

            onNpcDeath(variant.npcKey) { onDeath(npc, variant) }
        }
    }

    /**
     * Guaranteed bones, then one roll on the version's table, then the tertiaries - each independent
     * of the table and of each other.
     */
    private fun onDeath(
        npc: Npc,
        variant: HobgoblinVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)

        HobgoblinDrops.tableFor(variant.dropTable).roll(world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }

        if (world.chance(1, HobgoblinDrops.CHAMPION_SCROLL_ONE_IN)) {
            loot.add(getRSCM("item.hobgoblin_champion_scroll") to 1)
        }

        // Both of these are published against the unarmed table only - the armed section's whole
        // tertiary list is the champion scroll.
        if (variant.dropTable == HobgoblinTableId.UNARMED) {
            // "Only dropped by those found in the Wilderness". Gated on where the *killer* is
            // standing rather than on which variant died, matching `content/npcs/zombie` and
            // `content/npcs/slayer`: that is the wiki's own wording, and the Bandit Camp mine
            // hobgoblins share an id with seven other camps outside it.
            if (killer.inWilderness() && world.chance(1, HobgoblinDrops.LOOTING_BAG_ONE_IN)) {
                loot.add(getRSCM("item.looting_bag") to 1)
            }

            // The page's "Not dropped by those found in the God Wars Dungeon" note needs no check
            // of its own: that version is `dropversion = Armed` and so is already outside this
            // branch.
            if (world.chance(1, HobgoblinDrops.BEGINNER_CLUE_ONE_IN)) {
                loot.add(getRSCM("item.clue_scroll_beginner") to 1)
            }
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }
}
