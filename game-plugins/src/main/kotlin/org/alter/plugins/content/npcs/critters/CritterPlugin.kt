package org.alter.plugins.content.npcs.critters

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.DropRoll
import org.alter.rscm.RSCM.getRSCM

/**
 * Combat definitions and drops for the chickens, rats and spiders and for their overgrown
 * counterparts - giant spiders, giant rats and dungeon rats. Stats and tables come from
 * [Critters] and [GiantVermin]; this file is the wiring, and it follows the same shape as
 * `content/npcs/goblin` and `content/npcs/guard`.
 *
 * Spawns are not here - these three are scattered across most of the map and their
 * placement belongs with each area's own population, exactly as it already is for the
 * Lumbridge rats and the Falador Farm chickens. The Goblin Cave's boxes spawn them at
 * runtime instead (`areas/goblincave/objs/SearchBoxesPlugin`), which is what prompted
 * this file: they were being spawned aggressive, with 10 hitpoints and no stats.
 *
 * Notes on what is and is not declared:
 * - **`aggro { }` on the Stronghold of Security spider and on every giant spider and giant
 *   rat.** The small chickens, rats and spiders are `aggressive = No`, and so are the
 *   dungeon rats; both giant pages are `aggressive = Yes` unversioned, down to the level 2
 *   and level 3 ones. No `alwaysAggro()` anywhere here, unlike the Goblin Cave sentry: the
 *   wiki does not claim any of these ignore combat level, so
 *   [org.alter.plugins.content.mechanics.aggro.NpcAggroPlugin]'s default check applies and
 *   each stops bothering players above twice its own level, which is the normal rule.
 * - **Combat style is set on spawn**, not in the combat def, because the engine never
 *   copies a style out of the def. It matters here: the rats are crush while the chickens
 *   and spiders are stab, so leaving it at the STAB default would have been silently wrong
 *   for the rats alone.
 * - **`immunities { }` on the chicken.** The wiki gives it `poisonresistance = 100` and
 *   `venomresistance = 100`; the rats and spiders get neither.
 * - **`defence { magic { elementWeakness } }`** wherever the wiki publishes one: Fire at
 *   50% on every spider and giant spider, Air at 10% on the giant bats.
 * - **No `ranged { }` blocks** - every version is `range = 1`.
 */
class CritterPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        (Critters.VARIANTS + GiantVermin.VARIANTS).forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                setCombatDef(npcKey) {
                    configs {
                        attackSpeed = variant.attackSpeed
                        respawnDelay = variant.respawnCycles
                    }
                    if (variant.aggroRadius > 0) {
                        aggro {
                            radius = variant.aggroRadius
                            searchDelay = AGGRO_SEARCH_DELAY
                        }
                    }
                    stats {
                        hitpoints = variant.hitpoints
                        attack = variant.attack
                        strength = variant.strength
                        defence = variant.defence
                        magic = Critters.MAGIC_LEVEL
                        ranged = Critters.RANGED_LEVEL
                    }
                    bonuses {
                        attackBonus = variant.attackBonus
                        strengthBonus = variant.strengthBonus
                        defenceStab = variant.defenceStab
                        defenceSlash = variant.defenceSlash
                        defenceCrush = variant.defenceCrush
                        defenceMagic = variant.defenceMagic
                        defenceRanged = variant.defenceRanged
                    }
                    if (variant.poisonImmune || variant.venomImmune) {
                        immunities {
                            poison = variant.poisonImmune
                            venom = variant.venomImmune
                        }
                    }
                    variant.elementalWeakness?.let { weakness ->
                        defence {
                            magic {
                                elementWeakness = weakness
                            }
                        }
                    }
                    anims {
                        attack = variant.attackAnimation
                        block = variant.blockAnimation
                        death = variant.deathAnimation
                    }
                    slayerData {
                        xp = variant.slayerXp
                    }
                }

                onNpcSpawn(npc = npcKey) { npc.combatStyle = variant.combatStyle }

                onNpcDeath(npcKey) { onDeath(npc, variant.drops) }
            }
        }
    }

    /**
     * Rolled here rather than through the combat DSL's `drops { }` block, which builds a
     * loot table [org.alter.game.action.NpcDeathAction] never rolls.
     *
     * A variant with nothing to give drops nothing and spawns no ground items at all -
     * which for a regular rat outside the Wilderness is the correct, published outcome,
     * not a bug.
     */
    private fun onDeath(
        npc: Npc,
        drops: CritterDrops,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = drops.always.map { getRSCM(it) to 1 }.toMutableList()

        if (drops.table.isNotEmpty()) {
            DropRoll.pick(drops.table, world)?.let { picked ->
                picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
            }
        }

        if (drops.beginnerClueChance > 0.0 && world.randomDouble() <= drops.beginnerClueChance) {
            loot.add(getRSCM("item.clue_scroll_beginner") to 1)
        }

        if (drops.wildernessLootingBagChance > 0.0 &&
            killer.inWilderness() &&
            world.randomDouble() <= drops.wildernessLootingBagChance
        ) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }

        loot.forEach { (item, amount) ->
            world.spawn(GroundItem(item = item, amount = amount, tile = npc.tile, owner = killer))
        }
    }

    private companion object {
        /** Cycles between aggro sweeps for the one aggressive variant. */
        const val AGGRO_SEARCH_DELAY = 4
    }
}
