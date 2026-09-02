package org.alter.plugins.content.npcs.dungeon

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Npc
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.DropRoll
import org.alter.plugins.content.npcs.GemDropTable
import org.alter.rscm.RSCM.getRSCM
import org.alter.game.model.entity.Player

/**
 * Combat definitions and guaranteed drops for the dungeon monsters. Stats come from
 * [DungeonMonsters]; this file is the wiring, and it follows the same shape as
 * `content/npcs/goblin`, `content/npcs/critters` and `content/npcs/guard`.
 *
 * Spawns are not here. None of these monsters is placed yet - the four dungeons currently
 * hold only their giant bats - so this file exists to make those areas *safe to populate*
 * rather than to populate them. Adding a hill giant to Taverley is now one `spawnNpc` line
 * that produces a real level 28 monster instead of a 10-hitpoint placeholder.
 *
 * Notes on what is and is not declared:
 * - **`aggro { }` wherever the wiki says `aggressive = Yes`**, which is most of them: only
 *   the dwarf, the jailer, the ogre chieftain and the suit of armour are passive. No
 *   `alwaysAggro()` anywhere - none of these pages claims to ignore combat level, so
 *   [org.alter.plugins.content.mechanics.aggro.NpcAggroPlugin]'s default check applies and
 *   each stops bothering players above twice its own level.
 * - **Combat style is set on spawn**, not in the combat def, because the engine never
 *   copies a style out of the def. It matters across this set: the skeletons alone split
 *   crush at level 22 and slash at 25 and 45.
 * - **No `ranged { }` blocks** - every monster here is `range = 1`.
 * - **No `immunities { }`** - none of these pages carries a poison or venom resistance.
 */
class DungeonMonsterPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        DungeonMonsters.ALL.forEach { monster ->
            monster.npcKeys.forEach { npcKey ->
                setCombatDef(npcKey) {
                    configs {
                        attackSpeed = monster.attackSpeed
                        respawnDelay = monster.respawnCycles
                    }
                    if (monster.aggroRadius > 0) {
                        aggro {
                            radius = monster.aggroRadius
                            searchDelay = AGGRO_SEARCH_DELAY
                        }
                    }
                    stats {
                        hitpoints = monster.hitpoints
                        attack = monster.attack
                        strength = monster.strength
                        defence = monster.defence
                        magic = DungeonMonsters.MAGIC_LEVEL
                        ranged = DungeonMonsters.RANGED_LEVEL
                    }
                    bonuses {
                        attackBonus = monster.attackBonus
                        strengthBonus = monster.strengthBonus
                        defenceStab = monster.defenceStab
                        defenceSlash = monster.defenceSlash
                        defenceCrush = monster.defenceCrush
                        defenceMagic = monster.defenceMagic
                        defenceRanged = monster.defenceRanged
                    }
                    monster.elementalWeakness?.let { weakness ->
                        defence {
                            magic {
                                elementWeakness = weakness
                            }
                        }
                    }
                    anims {
                        attack = monster.attackAnimation
                        block = monster.blockAnimation
                        death = monster.deathAnimation
                    }
                    slayerData {
                        xp = monster.slayerXp
                    }
                }

                onNpcSpawn(npc = npcKey) { npc.combatStyle = monster.combatStyle }

                onNpcDeath(npcKey) { onDeath(npc, monster) }
            }
        }
    }

    /**
     * The wiki's 100% rows, then one roll on the monster's own table, then - for the eleven
     * that reach it - an independent roll on the shared gem drop table at that monster's
     * published rate.
     *
     * The gem roll is a separate chance rather than a row inside the main table, because
     * that is how the wiki states it: the main table's rarities already sum against their
     * own denominator, and the gem access rate is quoted on top ("There is a 3/128 chance of
     * rolling the gem drop table").
     *
     * Rolled here rather than through the combat DSL's `drops { }` block, which builds a
     * loot table [org.alter.game.action.NpcDeathAction] never rolls.
     */
    private fun onDeath(
        npc: Npc,
        monster: DungeonMonster,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world
        val loot = monster.guaranteedDrops.map { getRSCM(it) to 1 }.toMutableList()

        if (monster.table.isNotEmpty()) {
            DropRoll.pick(monster.table, world)?.let { picked ->
                picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
            }
        }

        monster.gemTableChance?.let { chance ->
            if (world.randomDouble() <= chance) {
                DropRoll.pick(GemDropTable.TABLE, world)?.let { picked ->
                    picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
                }
            }
        }

        loot.forEach { (item, amount) ->
            world.spawn(GroundItem(item = item, amount = amount, tile = npc.tile, owner = killer))
        }
    }

    private companion object {
        /** Cycles between aggro sweeps, matching the other monster packages. */
        const val AGGRO_SEARCH_DELAY = 4
    }
}
