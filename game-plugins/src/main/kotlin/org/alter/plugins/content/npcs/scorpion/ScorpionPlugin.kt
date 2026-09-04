package org.alter.plugins.content.npcs.scorpion

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
 * Makes scorpions aggressive, worth Slayer experience and worth killing. [ScorpionSpawnPlugin] puts
 * them on the map; stats live in `data/cfg/npcs/monsterStats.json`, everything else in [Scorpions].
 *
 * `data/cfg/slayer/tasks.json` already names `Scorpion` in its `Scorpions` category, so the task was
 * assignable and awarded **nothing** before this file existed - `Slayer.onKill` reads `slayerXp` off
 * the dying npc and there is nowhere else it comes from.
 *
 * ## The level 38 really does poison
 *
 * `poisonous = Yes (1)` on the Ape Atoll version alone. `poisonDamage` is read by
 * [org.alter.plugins.content.mechanics.poison.CombatPoison] and is the same field
 * `content/npcs/dungeon` uses for the poison scorpion and poison spider; the rate is not published
 * for any monster and is left to `NpcCombatDef.DEFAULT_POISON_CHANCE`.
 */
class ScorpionPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Scorpions.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                onNpcSpawn(npcKey) {
                    val current = npc.combatDef
                    npc.combatDef =
                        current.copy(
                            respawnDelay = variant.respawnCycles,
                            aggressiveRadius = Scorpions.AGGRO_RADIUS,
                            aggroTargetDelay = Scorpions.AGGRO_SEARCH_DELAY,
                            aggressiveTimer = Scorpions.AGGRO_TIMER,
                            slayerXp = variant.slayerXp,
                            poisonDamage = if (variant.poisonDamage > 0) variant.poisonDamage else current.poisonDamage,
                        )
                }

                onNpcDeath(npcKey) { onDeath(npc) }
            }
        }
    }

    /**
     * The three tertiaries, and nothing else: a scorpion has no guaranteed drop and no weighted
     * table. See [Scorpions] for why the page's one `100%` row is not one.
     */
    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world
        val loot = mutableListOf<Pair<Int, Int>>()

        // "Looting bags are only dropped by scorpions found in the Wilderness" - gated on where the
        // killer stands, matching every other position-gated drop in this tree. Two of the thirteen
        // camps are Wilderness, so the test really discriminates.
        if (killer.inWilderness() && world.chance(1, Scorpions.LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }
        if (world.chance(1, Scorpions.ENSOULED_HEAD_ONE_IN)) {
            loot.add(getRSCM("item.ensouled_scorpion_head") to 1)
        }
        if (world.chance(1, Scorpions.BEGINNER_CLUE_ONE_IN)) {
            loot.add(getRSCM("item.clue_scroll_beginner") to 1)
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }
}
