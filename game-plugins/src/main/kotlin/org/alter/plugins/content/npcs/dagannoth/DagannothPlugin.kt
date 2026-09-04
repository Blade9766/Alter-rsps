package org.alter.plugins.content.npcs.dagannoth

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.items.jewellery.RingOfWealth
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.rscm.RSCM.getRSCM

/**
 * Makes dagannoth aggressive, worth Slayer experience and worth killing, and makes the level 74
 * throw its spines instead of biting. [DagannothSpawnPlugin] puts them on the map; stats live in
 * `data/cfg/npcs/monsterStats.json`, the table and rates in [Dagannoths].
 *
 * `data/cfg/slayer/tasks.json` had a `Dagannoth` category with an **empty `monsters` list** - an
 * assignable task naming nothing, which `SlayerService.markAvailable` reads as not assignable. The
 * list names the monster now, and with `slayerXp` wired here the task pays out.
 *
 * ## The ranged patch
 *
 * See [Dagannoths] for why the level 74 needs it and why the ids it uses are sourced rather than
 * chosen. Mechanically it is four things at once, and all four are needed:
 *
 * - `combatClass` on the **def**, so a respawned dagannoth gets it again.
 * - `combatClass` on the **npc**, because `World.setNpcDefaults` copies that field once, before this
 *   hook runs.
 * - `combatStyle` RANGED, which is what `CombatConfigs.getCombatClass` and the defence roll read.
 * - `rangedProjectileGfx`, without which `RangedCombatStrategy.fireNpcProjectile` returns silently
 *   and the spines are invisible.
 */
class DagannothPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Dagannoths.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                onNpcSpawn(npcKey) {
                    val current = npc.combatDef
                    npc.combatDef =
                        if (variant.ranged) {
                            current.copy(
                                respawnDelay = Dagannoths.RESPAWN_CYCLES,
                                aggressiveRadius = Dagannoths.AGGRO_RADIUS,
                                aggroTargetDelay = Dagannoths.AGGRO_SEARCH_DELAY,
                                aggressiveTimer = Dagannoths.AGGRO_TIMER,
                                slayerXp = variant.slayerXp,
                                combatClass = CombatClass.RANGED,
                                combatStyle = CombatStyle.RANGED,
                                attackRange = Dagannoths.SPINE_RANGE,
                                attackAnimation = Animation.DAGANNOTH_SPINES_ATTACK,
                                rangedProjectileGfx = Graphic.DAGANNOTH_SPINES,
                            )
                        } else {
                            current.copy(
                                respawnDelay = Dagannoths.RESPAWN_CYCLES,
                                aggressiveRadius = Dagannoths.AGGRO_RADIUS,
                                aggroTargetDelay = Dagannoths.AGGRO_SEARCH_DELAY,
                                aggressiveTimer = Dagannoths.AGGRO_TIMER,
                                slayerXp = variant.slayerXp,
                            )
                        }

                    if (variant.ranged) {
                        // setNpcDefaults already copied the def's class and style onto the npc,
                        // before this hook ran, so the live fields have to be set as well.
                        npc.combatClass = CombatClass.RANGED
                        npc.combatStyle = CombatStyle.RANGED
                    }
                }

                onNpcDeath(npcKey) { onDeath(npc) }
            }
        }
    }

    /** Guaranteed bones, one roll on the shared table, then the two tertiaries that are modelled. */
    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)

        Dagannoths.TABLE.roll(world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }

        if (world.chance(1, Dagannoths.ENSOULED_HEAD_ONE_IN)) {
            loot.add(getRSCM("item.ensouled_dagannoth_head") to 1)
        }
        if (world.chance(1, Dagannoths.MEDIUM_CLUE_ONE_IN)) {
            loot.add(getRSCM("item.clue_scroll_medium") to 1)
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }
}
