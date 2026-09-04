package org.alter.plugins.content.npcs.guard

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.entity.Npc
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The city guards of Varrock, Edgeville and Ardougne: spawns, combat definitions and drops.
 * Stats and tiles come from [CityGuards]; this file is the wiring.
 *
 * One plugin covers all three because the wiki gives each of them a single unversioned stat
 * block, so the only thing that varies between them is data. Falador keeps its own plugin -
 * its eleven versions split into four stat groups and include ranged archers, which needs
 * genuinely different wiring.
 *
 * - **None of them are aggressive** (`aggressive = No` on all three blocks), so no
 *   `aggro { }` block anywhere here.
 * - **Combat style is set on spawn**, not in the combat def: `Npc.combatStyle` defaults to
 *   `STAB` and the engine never copies a style out of `NpcCombatDef`. Without this, the
 *   Varrock and Ardougne guards would roll against the player's stab defence instead of
 *   crush - and since Edgeville genuinely *is* stab, the bug would have been invisible in
 *   one city and wrong in the other two.
 * - **No `ranged { }` blocks** - every version in all three cities is `range = 1`. Only
 *   Falador has archers.
 * - **Combat sounds are set explicitly**, in a `sound { }` block. Nothing else supplies
 *   them: `MonsterAnimationsPlugin` only preserves what is already set here (its
 *   `takeIf { it > 0 }` fallbacks) and cannot derive any of its own for an npc named
 *   `Guard` on this cache - see [CityGuard.HUMAN_BLOCK_SOUND] for why. Without this block
 *   the guards fight in complete silence, which is what they did.
 * - Drops come from [GuardDrops], the single table the wiki publishes for every guard in
 *   the game.
 */
class CityGuardPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        CityGuards.ALL.forEach { city ->
            city.spawns.forEach { spawn ->
                spawnNpc(
                    npc = spawn.npcKey,
                    x = spawn.x,
                    z = spawn.z,
                    height = spawn.height,
                    walkRadius = if (spawn.height == 0) 4 else 2,
                    direction = Direction.SOUTH,
                )
            }

            city.npcKeys.forEach { npcKey ->
                setCombatDef(npcKey) {
                    configs {
                        attackSpeed = city.attackSpeed
                        respawnDelay = CityGuard.RESPAWN_CYCLES
                    }
                    stats {
                        hitpoints = CityGuard.HITPOINTS
                        attack = city.attack
                        strength = city.strength
                        defence = city.defence
                        magic = CityGuard.MAGIC_LEVEL
                        ranged = CityGuard.RANGED_LEVEL
                    }
                    bonuses {
                        defenceStab = city.defenceStab
                        defenceSlash = city.defenceSlash
                        defenceCrush = city.defenceCrush
                        defenceMagic = city.defenceMagic
                        defenceRanged = city.defenceRanged
                        attackBonus = city.attackBonus
                        strengthBonus = city.strengthBonus
                    }
                    anims {
                        attack = city.attackAnimation
                        block = city.blockAnimation
                        death = city.deathAnimation
                    }
                    sound {
                        // `attackArea` has to be assigned before `attackRadius`: the
                        // radius setters `check(area)` and throw otherwise. Same for the
                        // block and death pairs below.
                        attackArea = true
                        attackRadius = CityGuard.SOUND_RADIUS
                        attackVolume = CityGuard.SOUND_LOOPS
                        attackSound = city.attackSound

                        blockArea = true
                        blockRadius = CityGuard.SOUND_RADIUS
                        blockVolume = CityGuard.SOUND_LOOPS
                        blockSound = CityGuard.HUMAN_BLOCK_SOUND

                        deathArea = true
                        deathRadius = CityGuard.SOUND_RADIUS
                        deathVolume = CityGuard.SOUND_LOOPS
                        deathSound = CityGuard.HUMAN_DEATH_SOUND
                    }
                }

                onNpcSpawn(npc = npcKey) { npc.combatStyle = city.combatStyle }

                onNpcDeath(npcKey) { onDeath(npc) }
            }
        }
    }

    private fun onDeath(npc: Npc) = GuardDrops.rollOnDeath(npc)
}
