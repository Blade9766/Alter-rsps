package org.alter.plugins.content.npcs.ghost

import org.alter.api.ext.inWilderness
import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.rscm.RSCM.getRSCM

/**
 * Makes ghosts aggressive, worth Slayer experience and worth killing. [GhostSpawnPlugin] puts them
 * on the map; stats live in `data/cfg/npcs/monsterStats.json` and ids and tiles in [Ghosts].
 *
 * ## What this changes, per ghost
 *
 * Layered onto the def the engine already built, the pattern
 * `areas/wilderness/bosses/WildernessBossPlugin` documents:
 *
 * - **`respawnDelay`** 25 -> [Ghosts.RESPAWN_CYCLES]. Read by `NpcDeathAction` at death, so patching
 *   it at spawn is enough.
 * - **`aggressiveRadius`**, **`aggroTargetDelay`** and **`aggressiveTimer`**. Every version is
 *   `aggressive = Yes`, and all three fields are needed: without a radius `NpcAggroPlugin` never
 *   arms its timer, and without a non-zero timer it arms one that refuses every target.
 * - **`slayerXp`**, which `Slayer.onKill` reads off the dying npc and is the only place Slayer
 *   experience comes from. Zeroed before this, so `data/cfg/slayer/tasks.json`'s `Ghosts` category
 *   - which Turael, Spria and Mazchna have always been able to assign, and which already named the
 *   right monsters - could be handed out and completed for nothing at all.
 *
 * The patch is applied in an `onNpcSpawn` hook rather than once at load because a ghost that dies is
 * re-defaulted: `NpcDeathAction` calls `World.setNpcDefaults` and then `executeNpcSpawn` again,
 * which would otherwise hand the respawned ghost back its unpatched def. Per-npc spawn hooks run
 * before global ones, so both `NpcAggroPlugin` and
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] see the patched version.
 *
 * ## Drops
 *
 * A ghost drops **nothing at all** on the main table - it has none, not even bones, which is what
 * the page means by "Ghosts are rarely used for training, because they drop nothing in most
 * locations". The two published tertiaries are the whole of it, which is why this package has no
 * `*Drops.kt`.
 *
 * Rolled here rather than through the combat DSL's `drops { }` block, for the reason every monster
 * package in this tree repeats: that block builds a `LootTable` which `NpcDeathAction` never rolls.
 */
class GhostPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Ghosts.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                onNpcSpawn(npcKey) {
                    npc.combatDef =
                        npc.combatDef.copy(
                            respawnDelay = Ghosts.RESPAWN_CYCLES,
                            aggressiveRadius = Ghosts.AGGRO_RADIUS,
                            aggroTargetDelay = Ghosts.AGGRO_SEARCH_DELAY,
                            aggressiveTimer = Ghosts.AGGRO_TIMER,
                            slayerXp = variant.slayerXp,
                        )
                }

                onNpcDeath(npcKey) { onDeath(npc) }
            }
        }
    }

    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world
        val loot = mutableListOf<Pair<Int, Int>>()

        // "Only dropped by those found in the Wilderness", 1/7. Gated on where the *killer* is
        // standing rather than on which variant died, matching `content/npcs/zombie` and
        // `content/npcs/slayer`: that is the wiki's own wording, and a hand-spawned Wilderness ghost
        // should not print looting bags in Lumbridge.
        if (killer.inWilderness() && world.chance(1, LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }

        if (world.chance(1, BEGINNER_CLUE_ONE_IN)) {
            loot.add(getRSCM("item.clue_scroll_beginner") to 1)
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }

    private companion object {
        /** Wiki tertiary, 1/7, Wilderness only, published against the level 19 drop version. */
        const val LOOTING_BAG_ONE_IN = 7

        /** Wiki tertiary, 1/90, published with no `dropversion` and so on all three versions. */
        const val BEGINNER_CLUE_ONE_IN = 90
    }
}
