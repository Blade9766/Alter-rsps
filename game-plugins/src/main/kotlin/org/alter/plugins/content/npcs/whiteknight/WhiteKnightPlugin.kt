package org.alter.plugins.content.npcs.whiteknight

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.DropRoll
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The White Knights of the White Knights' Castle: spawns, combat definitions and drops.
 * Stats and tiles come from [WhiteKnightData]; this file is the wiring.
 *
 * Points worth knowing, all checked rather than assumed:
 *
 * - **They are not aggressive.** Every rank's infobox says `aggressive = No`, so none gets
 *   an `aggro { }` block - you can walk through the castle unmolested, as in the real game.
 * - **Melee style is set on spawn, not in the combat def.** `Npc.combatStyle` defaults to
 *   `STAB` and nothing in the engine copies a style out of `NpcCombatDef`, so without the
 *   `onNpcSpawn` assignment every knight would roll against the player's *stab* defence
 *   instead of slash. Same gotcha the barbarians hit.
 * - Both the male and female id of each rank share one combat def and one drop table,
 *   because the wiki gives them one set of stats and one set of drops.
 *
 * Drops are rolled here in [onDeath] rather than through the DSL's `drops { }` block: that
 * block builds a loot table `NpcDeathAction` never rolls, so configuring it yields no drops
 * at all.
 *
 * **Which parts of the published tables are modelled.** The wiki gives each rank seven
 * sections; the 100% (bones), weapons/armour, runes/ammunition, coins and "other" sections
 * are all reproduced below. Three are deliberately not:
 * - **Herbs** (`HerbDropLines`) and **seeds** (`GeneralSeedDropLines`) - template-expanded
 *   sub-tables whose contents aren't on the page, and this server has neither Herblore nor
 *   Farming to use them.
 * - **The gem drop table** (`GemDropTable`) - likewise a shared template, and one that
 *   carries its own nested rare-drop logic.
 * Because weights are relative (see [DropRoll]), omitting those simply rescales the rest
 * rather than distorting any single item's rarity relative to its neighbours.
 *
 * **Members-world reading.** Rows the wiki marks `{{(m)}}` (the blood runes) are included
 * and rows marked `{{(f)}}` ("only drops in free-to-play") are excluded, since this server
 * already runs members content such as Barrows and the KBD. Where a row has an `altrarity`
 * for members (the mid-tier coin drops, 11/128 dropping to 10/128), the members value is
 * used, for the same reason.
 */
class WhiteKnightPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        WhiteKnightData.SPAWNS.forEach { spawn ->
            spawnNpc(
                npc = spawn.npcKey,
                x = spawn.x,
                z = spawn.z,
                height = spawn.height,
                walkRadius = 3,
                direction = Direction.SOUTH,
            )
        }

        WhiteKnightData.RANKS.forEach { rank ->
            rank.npcKeys.forEach { npcKey ->
                setCombatDef(npcKey) {
                    configs {
                        attackSpeed = WhiteKnightData.ATTACK_SPEED
                        respawnDelay = WhiteKnightData.RESPAWN_CYCLES
                    }
                    stats {
                        hitpoints = rank.hitpoints
                        attack = rank.attack
                        strength = rank.strength
                        defence = rank.defence
                        magic = 1
                        ranged = 1
                    }
                    bonuses {
                        defenceStab = rank.defenceStab
                        defenceSlash = rank.defenceSlash
                        defenceCrush = rank.defenceCrush
                        defenceMagic = rank.defenceMagic
                        defenceRanged = rank.defenceRanged
                        attackBonus = rank.attackBonus
                        strengthBonus = rank.strengthBonus
                    }
                    anims {
                        attack = WhiteKnightData.ATTACK_ANIMATION
                        block = WhiteKnightData.BLOCK_ANIMATION
                        death = WhiteKnightData.DEATH_ANIMATION
                    }
                }

                onNpcSpawn(npc = npcKey) { npc.combatStyle = WhiteKnightData.COMBAT_STYLE }

                onNpcDeath(npcKey) { onDeath(npc, npcKey) }
            }
        }
    }

    private fun onDeath(
        npc: Npc,
        npcKey: String,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world
        val table = TABLES.getValue(WhiteKnightData.rankOf(npcKey).name)

        val drops = mutableListOf(getRSCM("item.bones") to 1)
        val picked = DropRoll.pick(table, world)
        val pickedItem = picked?.item
        if (picked != null && pickedItem != null) {
            drops.add(pickedItem to DropRoll.amount(picked, world))
        }

        drops.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }

    private companion object {
        /** Level 36 - the only rank whose table carries a "Nothing" row at 2/128. */
        val INITIATE_TABLE: List<WeightedDrop> =
            listOf(
                WeightedDrop(getRSCM("item.iron_longsword"), weight = 2),
                WeightedDrop(getRSCM("item.steel_sword"), weight = 1),
                WeightedDrop(getRSCM("item.steel_med_helm"), weight = 1),
                WeightedDrop(getRSCM("item.mind_rune"), 5, weight = 11),
                WeightedDrop(getRSCM("item.nature_rune"), 4, weight = 4),
                WeightedDrop(getRSCM("item.body_rune"), 9, 13, weight = 3),
                WeightedDrop(getRSCM("item.chaos_rune"), 2, weight = 3),
                WeightedDrop(getRSCM("item.water_rune"), 25, 30, weight = 3),
                WeightedDrop(getRSCM("item.mithril_arrow"), 5, weight = 2),
                WeightedDrop(getRSCM("item.adamant_arrow"), 2, weight = 1),
                WeightedDrop(getRSCM("item.blood_rune"), 2, weight = 1),
                WeightedDrop(getRSCM("item.law_rune"), 2, weight = 1),
                WeightedDrop(getRSCM("item.coins_995"), 48, weight = 15),
                WeightedDrop(getRSCM("item.coins_995"), 15, weight = 15),
                WeightedDrop(getRSCM("item.coins_995"), 8, weight = 10),
                WeightedDrop(getRSCM("item.coins_995"), 45, 53, weight = 10),
                WeightedDrop(getRSCM("item.coins_995"), 1, weight = 5),
                WeightedDrop(getRSCM("item.coins_995"), 2, weight = 5),
                WeightedDrop(getRSCM("item.coins_995"), 120, weight = 1),
                WeightedDrop(getRSCM("item.iron_bar"), 2, weight = 6),
                WeightedDrop(getRSCM("item.iron_bar"), 1, weight = 2),
                WeightedDrop(getRSCM("item.half_an_apple_pie"), weight = 1),
                WeightedDrop(getRSCM("item.iron_ore"), weight = 1),
                WeightedDrop(getRSCM("item.pot_of_flour"), weight = 1),
                WeightedDrop(item = null, weight = 2),
            )

        /** Level 38. */
        val PROSELYTE_TABLE: List<WeightedDrop> =
            listOf(
                WeightedDrop(getRSCM("item.iron_longsword"), weight = 2),
                WeightedDrop(getRSCM("item.steel_med_helm"), weight = 2),
                WeightedDrop(getRSCM("item.steel_sword"), weight = 1),
                WeightedDrop(getRSCM("item.mind_rune"), 7, weight = 8),
                WeightedDrop(getRSCM("item.mithril_arrow"), 5, weight = 5),
                WeightedDrop(getRSCM("item.nature_rune"), 5, weight = 4),
                WeightedDrop(getRSCM("item.body_rune"), 10, 14, weight = 3),
                WeightedDrop(getRSCM("item.chaos_rune"), 3, weight = 3),
                WeightedDrop(getRSCM("item.water_rune"), 30, 35, weight = 3),
                WeightedDrop(getRSCM("item.adamant_arrow"), 2, weight = 3),
                WeightedDrop(getRSCM("item.blood_rune"), 2, weight = 1),
                WeightedDrop(getRSCM("item.law_rune"), 3, weight = 1),
                WeightedDrop(getRSCM("item.coins_995"), 15, weight = 13),
                WeightedDrop(getRSCM("item.coins_995"), 10, weight = 10),
                WeightedDrop(getRSCM("item.coins_995"), 54, weight = 10),
                WeightedDrop(getRSCM("item.coins_995"), 50, 58, weight = 10),
                WeightedDrop(getRSCM("item.coins_995"), 3, weight = 6),
                WeightedDrop(getRSCM("item.coins_995"), 1, weight = 1),
                WeightedDrop(getRSCM("item.coins_995"), 120, weight = 1),
                WeightedDrop(getRSCM("item.iron_bar"), 2, weight = 8),
                WeightedDrop(getRSCM("item.iron_bar"), 1, weight = 2),
                WeightedDrop(getRSCM("item.half_an_apple_pie"), weight = 1),
                WeightedDrop(getRSCM("item.iron_ore"), weight = 1),
                WeightedDrop(getRSCM("item.pot_of_flour"), weight = 1),
                WeightedDrop(item = null, weight = 1),
            )

        /** Level 39. Its published "other" section has no "Nothing" row, so neither has this. */
        val ACOLYTE_TABLE: List<WeightedDrop> =
            listOf(
                WeightedDrop(getRSCM("item.iron_longsword"), weight = 2),
                WeightedDrop(getRSCM("item.steel_med_helm"), weight = 2),
                WeightedDrop(getRSCM("item.steel_sword"), weight = 2),
                WeightedDrop(getRSCM("item.mind_rune"), 10, weight = 8),
                WeightedDrop(getRSCM("item.mithril_arrow"), 7, weight = 5),
                WeightedDrop(getRSCM("item.nature_rune"), 6, weight = 4),
                WeightedDrop(getRSCM("item.body_rune"), 15, 19, weight = 3),
                WeightedDrop(getRSCM("item.chaos_rune"), 4, weight = 3),
                WeightedDrop(getRSCM("item.water_rune"), 35, 40, weight = 3),
                WeightedDrop(getRSCM("item.adamant_arrow"), 4, weight = 3),
                WeightedDrop(getRSCM("item.blood_rune"), 3, weight = 1),
                WeightedDrop(getRSCM("item.law_rune"), 3, weight = 1),
                WeightedDrop(getRSCM("item.coins_995"), 15, 19, weight = 13),
                WeightedDrop(getRSCM("item.coins_995"), 15, weight = 10),
                WeightedDrop(getRSCM("item.coins_995"), 55, 63, weight = 10),
                WeightedDrop(getRSCM("item.coins_995"), 50, 69, weight = 10),
                WeightedDrop(getRSCM("item.coins_995"), 5, weight = 6),
                WeightedDrop(getRSCM("item.coins_995"), 120, weight = 2),
                WeightedDrop(getRSCM("item.coins_995"), 1, weight = 1),
                WeightedDrop(getRSCM("item.iron_bar"), 2, weight = 6),
                WeightedDrop(getRSCM("item.iron_bar"), 1, weight = 2),
                WeightedDrop(getRSCM("item.half_an_apple_pie"), weight = 1),
                WeightedDrop(getRSCM("item.iron_ore"), weight = 1),
                WeightedDrop(getRSCM("item.pot_of_flour"), weight = 1),
            )

        /** Level 42. Also has no published "Nothing" row. */
        val PARTISAN_TABLE: List<WeightedDrop> =
            listOf(
                WeightedDrop(getRSCM("item.iron_longsword"), weight = 2),
                WeightedDrop(getRSCM("item.steel_med_helm"), weight = 2),
                WeightedDrop(getRSCM("item.steel_sword"), weight = 2),
                WeightedDrop(getRSCM("item.mind_rune"), 12, weight = 3),
                WeightedDrop(getRSCM("item.mithril_arrow"), 9, weight = 5),
                WeightedDrop(getRSCM("item.nature_rune"), 7, weight = 4),
                WeightedDrop(getRSCM("item.body_rune"), 15, 24, weight = 3),
                WeightedDrop(getRSCM("item.chaos_rune"), 5, weight = 3),
                WeightedDrop(getRSCM("item.water_rune"), 40, 47, weight = 3),
                WeightedDrop(getRSCM("item.adamant_arrow"), 5, weight = 3),
                WeightedDrop(getRSCM("item.blood_rune"), 3, weight = 1),
                WeightedDrop(getRSCM("item.law_rune"), 3, weight = 1),
                WeightedDrop(getRSCM("item.coins_995"), 51, 55, weight = 13),
                WeightedDrop(getRSCM("item.coins_995"), 15, weight = 10),
                WeightedDrop(getRSCM("item.coins_995"), 65, 73, weight = 10),
                WeightedDrop(getRSCM("item.coins_995"), 8, weight = 6),
                WeightedDrop(getRSCM("item.coins_995"), 50, 69, weight = 5),
                WeightedDrop(getRSCM("item.coins_995"), 120, weight = 3),
                WeightedDrop(getRSCM("item.coins_995"), 1, weight = 1),
                WeightedDrop(getRSCM("item.iron_bar"), 2, weight = 5),
                WeightedDrop(getRSCM("item.iron_bar"), 1, weight = 2),
                WeightedDrop(getRSCM("item.half_an_apple_pie"), weight = 1),
                WeightedDrop(getRSCM("item.iron_ore"), weight = 1),
                WeightedDrop(getRSCM("item.pot_of_flour"), weight = 1),
            )

        val TABLES: Map<String, List<WeightedDrop>> =
            mapOf(
                "Initiate" to INITIATE_TABLE,
                "Proselyte" to PROSELYTE_TABLE,
                "Acolyte" to ACOLYTE_TABLE,
                "Partisan" to PARTISAN_TABLE,
            )
    }
}
