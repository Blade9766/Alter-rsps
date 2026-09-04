package org.alter.plugins.content.npcs.citizen

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Combat definitions and drops for every Man and Woman in the game. Stats come from
 * [Citizens], the drop table from [CitizenDrops]; this file is the wiring.
 *
 * Spawns are deliberately *not* here. Citizens are area population, so their placement
 * belongs with the rest of a town's - `areas/lumbridge/spawns`, `areas/varrock/spawns` and
 * the others - which is where the ones that exist already live.
 *
 * Notes on what is and is not declared:
 * - **No `aggro { }` block.** `aggressive = No` on every page, for every version.
 * - **No `ranged { }` block.** Every citizen punches at range 1.
 * - **No `slayerData { }` block.** Men and women are on no Slayer master's list and the wiki
 *   gives them no `slayxp`, so the default 0 is right; declaring `xp = 0.0` would read as a
 *   deliberate zero rather than an absence.
 * - **Animations are stated explicitly**, because setting a combat def at all takes an npc
 *   off [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin]'s resolver path.
 *   That plugin still supplies the citizen's combat *sounds* from its own `MAN`/`WOMAN`
 *   entries; see [Citizens] for the attack/block swap those entries used to carry.
 */
class CitizenPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Citizens.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                setCombatDef(npcKey) {
                    configs {
                        attackSpeed = Citizens.ATTACK_SPEED
                        combatStyle = Citizens.COMBAT_STYLE
                        respawnDelay = variant.respawnCycles
                    }
                    stats {
                        hitpoints = variant.hitpoints
                        attack = variant.attack
                        strength = variant.strength
                        defence = variant.defence
                        magic = Citizens.MAGIC_LEVEL
                        ranged = Citizens.RANGED_LEVEL
                    }
                    bonuses {
                        attackBonus = Citizens.ATTACK_BONUS
                        strengthBonus = Citizens.STRENGTH_BONUS
                        defenceStab = variant.defenceStab
                        defenceSlash = variant.defenceSlash
                        defenceCrush = variant.defenceCrush
                        defenceMagic = variant.defenceMagic
                        defenceRanged = variant.defenceRanged
                    }
                    anims {
                        attack = Citizens.ATTACK_ANIMATION
                        block = Citizens.BLOCK_ANIMATION
                        death = Citizens.DEATH_ANIMATION
                    }
                }

                onNpcDeath(npcKey) { CitizenDrops.rollOnDeath(npc, variant) }
            }
        }
    }
}
