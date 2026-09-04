package org.alter.plugins.content.npcs.barbarian

import org.alter.api.ProjectileType
import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.plugins.content.npcs.barbarian.BarbarianData.DropTier
import org.alter.rscm.RSCM.getRSCM

/**
 * The barbarians of Barbarian Village, and their chieftain Gunthor the Brave: spawns,
 * combat definitions and drops. Stats and tiles come from [BarbarianData]; this file is
 * the wiring.
 *
 * Notable points, all checked rather than assumed:
 *
 * - **Barbarians are not aggressive** (the wiki's `aggressive` parameter is a single
 *   unversioned `No` covering every version), so none of them get an `aggro { }` block.
 *   Gunthor is `aggressive = Yes` and does.
 * - **The archer really shoots.** Wiki version 13 (npc 3068) attacks with Ranged, and
 *   this codebase now has a generic ranged-npc path - `ranged { }` in the combat DSL
 *   sets `combatClass = RANGED`, and `RangedCombatStrategy` fires the configured
 *   projectile for npcs. That is newer than the note in this project's own memory
 *   saying ranged monsters need a bespoke attack loop; the DSL block was re-checked
 *   against `NpcParams.setRangedProjectile` and `RangedCombatStrategy.fireNpcProjectile`
 *   before relying on it, so no custom combat plugin is needed here. It fires a bronze
 *   arrow (gfx 10, drawback 19), matching the bronze arrows on its own drop table -
 *   the wiki doesn't name its ammo, so that's the consistent choice, not a documented one.
 * - **Melee style is set on spawn, not in the combat def.** `Npc.combatStyle` defaults
 *   to `STAB` and nothing in the engine ever copies a style out of `NpcCombatDef`, so
 *   without this every barbarian would be rolling against the player's *stab* defence
 *   instead of crush. There is no DSL field for it, hence the `onNpcSpawn` assignment.
 *
 * Drops are rolled here in [onDeath] rather than through the DSL's `drops { }` block:
 * that block builds a loot table that `NpcDeathAction` never rolls, so configuring it
 * produces no drops at all. Weights below are the wiki's rarity numerators used as
 * relative weights, the same manual pattern
 * [org.alter.plugins.content.npcs.darkwizard.DarkWizardConfigsPlugin] uses. The wiki
 * splits barbarian drops into two tables by level band, not per variant - levels 9/10
 * share [LOW_TABLE], levels 15/17 share [HIGH_TABLE], and Gunthor's own published table
 * is identical to [HIGH_TABLE], so he reuses it.
 */
class BarbarianPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        (BarbarianData.VILLAGE_VARIANTS + BarbarianData.GUNTHOR).forEach { variant ->
            spawnNpc(
                npc = variant.npcKey,
                x = variant.spawnX,
                z = variant.spawnZ,
                height = 0,
                walkRadius = 3,
                direction = Direction.SOUTH,
            )

            setCombatDef(variant.npcKey) {
                configs {
                    attackSpeed = variant.attackSpeed
                    respawnDelay = BarbarianData.RESPAWN_CYCLES
                }
                stats {
                    hitpoints = variant.hitpoints
                    attack = variant.attack
                    strength = variant.strength
                    defence = variant.defence
                    magic = 1
                    ranged = variant.ranged
                }
                bonuses {
                    defenceStab = variant.defenceStab
                    defenceSlash = variant.defenceSlash
                    defenceCrush = variant.defenceCrush
                    defenceMagic = variant.defenceMagic
                    defenceRanged = variant.defenceRanged
                    attackBonus = variant.attackBonus
                    strengthBonus = variant.strengthBonus
                }
                anims {
                    attack = variant.attackAnimation
                    block = variant.blockAnimation
                    death = BarbarianData.DEATH_ANIMATION
                }
                if (variant.combatStyle == CombatStyle.RANGED) {
                    ranged {
                        projectile = BRONZE_ARROW_PROJECTILE
                        drawback = BRONZE_ARROW_DRAWBACK
                        type = ProjectileType.ARROW
                    }
                }
                if (variant.npcKey == BarbarianData.GUNTHOR.npcKey) {
                    aggro {
                        radius = 5
                        searchDelay = 1
                    }
                }
            }

            onNpcSpawn(npc = variant.npcKey) { npc.combatStyle = variant.combatStyle }

            onNpcDeath(variant.npcKey) { onDeath(npc, variant.dropTier) }
        }
    }

    private fun onDeath(
        npc: Npc,
        tier: DropTier,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world
        val table = if (tier == DropTier.LOW) LOW_TABLE else HIGH_TABLE

        val drops = mutableListOf(getRSCM("item.bones") to 1)
        val picked = weightedPick(table, world)
        val pickedItem = picked?.item
        if (picked != null && pickedItem != null) {
            drops.add(pickedItem to picked.amount)
        }

        drops.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }

    /** A weighted drop-table entry. A null [item] is the wiki's "Nothing" row. */
    private data class WeightedDrop(val item: Int?, val amount: Int, val weight: Int)

    /** Picks one entry from [table] by relative weight. */
    private fun weightedPick(
        table: List<WeightedDrop>,
        world: World,
    ): WeightedDrop? {
        val total = table.sumOf { it.weight }
        var roll = world.randomDouble() * total
        for (drop in table) {
            if (roll < drop.weight) {
                return drop
            }
            roll -= drop.weight
        }
        return null
    }

    private companion object {
        const val BRONZE_ARROW_PROJECTILE = 10
        const val BRONZE_ARROW_DRAWBACK = 19

        /** Levels 9 and 10. "Nothing" is 36/128 and is modelled as a weighted null. */
        val LOW_TABLE: List<WeightedDrop> =
            listOf(
                WeightedDrop(getRSCM("item.bronze_axe"), 1, 6),
                WeightedDrop(getRSCM("item.staff"), 1, 4),
                WeightedDrop(getRSCM("item.iron_mace"), 1, 1),
                WeightedDrop(getRSCM("item.chaos_rune"), 2, 4),
                WeightedDrop(getRSCM("item.bronze_arrow"), 15, 3),
                WeightedDrop(getRSCM("item.earth_rune"), 2, 3),
                WeightedDrop(getRSCM("item.fire_rune"), 5, 2),
                WeightedDrop(getRSCM("item.mind_rune"), 5, 2),
                WeightedDrop(getRSCM("item.law_rune"), 2, 1),
                WeightedDrop(getRSCM("item.coins_995"), 5, 42),
                WeightedDrop(getRSCM("item.coins_995"), 8, 9),
                WeightedDrop(getRSCM("item.coins_995"), 17, 5),
                WeightedDrop(getRSCM("item.coins_995"), 27, 3),
                WeightedDrop(getRSCM("item.tin_ore"), 1, 1),
                WeightedDrop(getRSCM("item.bear_fur"), 1, 1),
                WeightedDrop(getRSCM("item.beer"), 1, 1),
                WeightedDrop(getRSCM("item.cooked_meat"), 1, 1),
                WeightedDrop(getRSCM("item.flyer"), 1, 1),
                WeightedDrop(getRSCM("item.ring_mould"), 1, 1),
                WeightedDrop(item = null, amount = 0, weight = 36),
            )

        /** Levels 15 and 17, and Gunthor. "Nothing" is 32/128. */
        val HIGH_TABLE: List<WeightedDrop> =
            listOf(
                WeightedDrop(getRSCM("item.iron_axe"), 1, 6),
                WeightedDrop(getRSCM("item.bronze_battleaxe"), 1, 4),
                WeightedDrop(getRSCM("item.iron_mace"), 1, 1),
                WeightedDrop(getRSCM("item.bronze_arrow"), 10, 4),
                WeightedDrop(getRSCM("item.chaos_rune"), 3, 4),
                WeightedDrop(getRSCM("item.iron_arrow"), 8, 3),
                WeightedDrop(getRSCM("item.earth_rune"), 5, 3),
                WeightedDrop(getRSCM("item.mind_rune"), 10, 2),
                WeightedDrop(getRSCM("item.fire_rune"), 8, 2),
                WeightedDrop(getRSCM("item.law_rune"), 2, 1),
                WeightedDrop(getRSCM("item.coins_995"), 8, 42),
                WeightedDrop(getRSCM("item.coins_995"), 12, 9),
                WeightedDrop(getRSCM("item.coins_995"), 25, 5),
                WeightedDrop(getRSCM("item.coins_995"), 32, 3),
                WeightedDrop(getRSCM("item.cooked_meat"), 1, 1),
                WeightedDrop(getRSCM("item.tin_ore"), 1, 1),
                WeightedDrop(getRSCM("item.amulet_mould"), 1, 1),
                WeightedDrop(getRSCM("item.beer"), 1, 1),
                WeightedDrop(getRSCM("item.bear_fur"), 1, 1),
                WeightedDrop(getRSCM("item.flyer"), 1, 1),
                WeightedDrop(item = null, amount = 0, weight = 32),
            )
    }
}
