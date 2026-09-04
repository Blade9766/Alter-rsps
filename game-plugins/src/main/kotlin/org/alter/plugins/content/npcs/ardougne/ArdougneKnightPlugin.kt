package org.alter.plugins.content.npcs.ardougne

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
 * East Ardougne's Knights of Ardougne and Paladins: spawns, combat definitions and drops.
 * Stats and tiles come from [ArdougneKnightData]; this file is the wiring.
 *
 * - **Neither is aggressive**, so no `aggro { }` block. The paladin infobox does qualify
 *   this - "No, unless caught stealing from a silver, spice or gem stall" - which is a real
 *   Thieving interaction: failing one of those Ardougne stalls makes nearby paladins turn on
 *   you. That is **not implemented**; it would need a hook from `StallThievingPlugin` into
 *   nearby-npc aggression, which is its own piece of work rather than part of placing them.
 * - **Combat style is set on spawn**, not in the combat def - `Npc.combatStyle` defaults to
 *   `STAB` and the engine never copies a style out of `NpcCombatDef`, so both would
 *   otherwise roll against the wrong defence.
 * - **Knights drop only bones.** That is not an omission: their whole published combat drop
 *   table is one 100% bones row. Everything else a knight is worth comes from pickpocketing
 *   them, which `data/cfg/thieving/pickpockets.json` already handles.
 *
 * **What of the paladin table is modelled**: its 100%, weapons/armour, runes, materials,
 * coins and "other" sections. Not modelled, and flagged rather than faked - the herb
 * sub-table (`HerbDropLines`, template-expanded and no Herblore here), the gem drop table
 * (likewise), and the medium clue scroll (no clue system). Weights are relative, so omitting
 * those rescales the rest rather than distorting any single item's rarity.
 */
class ArdougneKnightPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        ArdougneKnightData.SPAWNS.forEach { spawn ->
            spawnNpc(
                npc = spawn.npcKey,
                x = spawn.x,
                z = spawn.z,
                height = spawn.height,
                walkRadius = 3,
                direction = Direction.SOUTH,
            )
        }

        ArdougneKnightData.GROUPS.forEach { group ->
            group.npcKeys.forEach { npcKey ->
                setCombatDef(npcKey) {
                    configs {
                        attackSpeed = group.attackSpeed
                        respawnDelay = group.respawnCycles
                    }
                    stats {
                        hitpoints = group.hitpoints
                        attack = group.attack
                        strength = group.strength
                        defence = group.defence
                        magic = 1
                        ranged = 1
                    }
                    bonuses {
                        defenceStab = group.defenceStab
                        defenceSlash = group.defenceSlash
                        defenceCrush = group.defenceCrush
                        defenceMagic = group.defenceMagic
                        defenceRanged = group.defenceRanged
                        attackBonus = group.attackBonus
                        strengthBonus = group.strengthBonus
                    }
                    anims {
                        attack = ArdougneKnightData.ATTACK_ANIMATION
                        block = ArdougneKnightData.BLOCK_ANIMATION
                        death = ArdougneKnightData.DEATH_ANIMATION
                    }
                }

                onNpcSpawn(npc = npcKey) { npc.combatStyle = ArdougneKnightData.COMBAT_STYLE }

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

        val drops = mutableListOf(getRSCM("item.bones") to 1)

        // Knights have no combat drops beyond bones - their published table is that one row.
        if (npcKey in ArdougneKnightData.PALADIN.npcKeys) {
            val picked = DropRoll.pick(PALADIN_TABLE, world)
            val pickedItem = picked?.item
            if (picked != null && pickedItem != null) {
                drops.add(pickedItem to DropRoll.amount(picked, world))
            }
        }

        drops.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }

    private companion object {
        val PALADIN_TABLE: List<WeightedDrop> =
            listOf(
                // Weapons and armour.
                WeightedDrop(getRSCM("item.steel_sword"), weight = 2),
                WeightedDrop(getRSCM("item.steel_longsword"), weight = 1),
                WeightedDrop(getRSCM("item.steel_full_helm"), weight = 1),
                // Runes.
                WeightedDrop(getRSCM("item.water_rune"), 30, weight = 13),
                WeightedDrop(getRSCM("item.blood_rune"), 1, weight = 1),
                // Materials.
                WeightedDrop(getRSCM("item.iron_bar"), 1, weight = 9),
                WeightedDrop(getRSCM("item.mithril_bar"), 1, weight = 1),
                WeightedDrop(getRSCM("item.steel_bar"), 1, weight = 1),
                // Coins.
                WeightedDrop(getRSCM("item.coins_995"), 48, weight = 40),
                WeightedDrop(getRSCM("item.coins_995"), 15, weight = 19),
                WeightedDrop(getRSCM("item.coins_995"), 2, weight = 16),
                WeightedDrop(getRSCM("item.coins_995"), 8, weight = 10),
                WeightedDrop(getRSCM("item.coins_995"), 120, weight = 2),
                // Other.
                WeightedDrop(item = null, weight = 2),
            )
    }
}
