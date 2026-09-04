package org.alter.plugins.content.npcs.chaosdruid

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.inWilderness
import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.items.jewellery.RingOfWealth
import org.alter.plugins.content.npcs.DropRoll
import org.alter.plugins.content.npcs.GemDropTable
import org.alter.plugins.content.npcs.HerbDropTable
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.rscm.RSCM.getRSCM

/**
 * The chaos druid's combat definition and loot. Its numbers live in [ChaosDruids]; its magic
 * attack lives in [ChaosDruidCombatPlugin]; its spawns live with the five areas it stands in
 * (`areas/chaosdruidtower`, `areas/edgevilledungeon`, `areas/slepe`, `areas/taverleydungeon`
 * and `areas/yanilleagilitydungeon`), the same way every other monster in this codebase is
 * placed.
 *
 * ## Loot
 *
 * One kill rolls three separate tables, which is how the wiki quotes them:
 *
 * 1. **Bones**, always.
 * 2. **One pick from [ChaosDruids.TABLE]** - the runes, coins and "Other" rows, whose numerators
 *    share a single denominator of 128.
 * 3. **The herb table at 46/128**, for one herb (35/128) or two (11/128).
 * 4. **The gem table at 1/128**, through [GemDropTable.roll] rather than
 *    `DropRoll.pick(GemDropTable.TABLE)`, so the 1/128 step on to the mega-rare table is not
 *    skipped.
 * 5. **Each tertiary independently**, the looting bag only in the Wilderness.
 *
 * Rolled here in `onNpcDeath` rather than through the combat DSL's `drops { }` block, which
 * builds a loot table [org.alter.game.action.NpcDeathAction] never rolls.
 */
class ChaosDruidPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        /*
         * The magic half of "Crush, Magic". Registered before any druid can spawn, and it is what
         * makes the fists work too - see [ChaosDruidCombatStrategy] for why this is a strategy
         * rather than the `onNpcCombat` loop every other casting monster here uses.
         */
        CombatConfigs.setNpcCombatStrategy(getRSCM(ChaosDruids.NPC_KEY), ChaosDruidCombatStrategy)

        setCombatDef(ChaosDruids.NPC_KEY) {
            configs {
                attackSpeed = ChaosDruids.ATTACK_SPEED
                respawnDelay = ChaosDruids.RESPAWN_CYCLES
            }
            aggro {
                radius = ChaosDruids.AGGRO_RADIUS
                searchDelay = AGGRO_SEARCH_DELAY
            }
            stats {
                hitpoints = ChaosDruids.HITPOINTS
                attack = ChaosDruids.ATTACK_LEVEL
                strength = ChaosDruids.STRENGTH_LEVEL
                defence = ChaosDruids.DEFENCE_LEVEL
                magic = ChaosDruids.MAGIC_LEVEL
                ranged = ChaosDruids.RANGED_LEVEL
            }
            bonuses {
                attackBonus = ChaosDruids.ATTACK_BONUS
                strengthBonus = ChaosDruids.STRENGTH_BONUS
            }
            anims {
                attack = ChaosDruids.ATTACK_ANIMATION
                block = ChaosDruids.BLOCK_ANIMATION
                death = ChaosDruids.DEATH_ANIMATION
            }
            slayerData {
                xp = ChaosDruids.SLAYER_XP
            }
        }

        /*
         * Crush, per the page's attack style. Set on spawn rather than in the combat def because
         * a plugin's `onNpcSpawn` runs after `World.setNpcDefaults`, so this is the value that
         * survives - the same ordering every other monster package relies on.
         */
        onNpcSpawn(npc = ChaosDruids.NPC_KEY) { npc.combatStyle = CombatStyle.CRUSH }

        onNpcDeath(ChaosDruids.NPC_KEY) { onDeath(npc) }
    }

    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world
        val loot = ChaosDruids.GUARANTEED_DROPS.map { getRSCM(it) to 1 }.toMutableList()

        DropRoll.pick(ChaosDruids.TABLE, world)?.let { picked ->
            picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
        }

        /*
         * 35/128 for one herb, a further 11/128 for two, and the remaining 82/128 for none. Rolled
         * as one number against 128 rather than as two chances so the outcomes stay mutually
         * exclusive and total the published 46/128.
         */
        val herbRoll = world.random(ChaosDruids.DENOMINATOR - 1)
        val herbs =
            when {
                herbRoll < ChaosDruids.ONE_HERB_THRESHOLD -> 1
                herbRoll < ChaosDruids.TWO_HERB_THRESHOLD -> 2
                else -> 0
            }
        repeat(herbs) {
            DropRoll.pick(HerbDropTable.TABLE, world)?.let { picked ->
                picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
            }
        }

        if (world.randomDouble() <= ChaosDruids.GEM_TABLE_CHANCE) {
            GemDropTable.roll(world, RingOfWealth.enhancesDropTables(killer))?.let { picked ->
                picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
            }
        }

        ChaosDruids.TERTIARY_DROPS.forEach { tertiary ->
            if (tertiary.wildernessOnly && !killer.inWilderness()) {
                return@forEach
            }
            if (world.randomDouble() <= tertiary.chance) {
                loot.add(getRSCM(tertiary.item) to 1)
            }
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }

    private companion object {
        /** Cycles between aggro sweeps, matching the other monster packages. */
        const val AGGRO_SEARCH_DELAY = 4
    }
}
