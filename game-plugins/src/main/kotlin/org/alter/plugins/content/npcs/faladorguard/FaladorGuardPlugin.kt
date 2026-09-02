package org.alter.plugins.content.npcs.faladorguard

import org.alter.api.ProjectileType
import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Npc
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.guard.GuardDrops

/**
 * Falador's city guards: spawns, combat definitions and drops. Stats and tiles come from
 * [FaladorGuardData]; this file is the wiring.
 *
 * Notable points:
 *
 * - **Not aggressive.** The Falador block's `aggressive = No` covers every version, so no
 *   `aggro { }` block - guards ignore you until attacked, as in the real game.
 * - **The ranged groups really shoot.** The crossbow and longbow guards get a `ranged { }`
 *   block, which sets `combatClass = RANGED` so `RangedCombatStrategy` fires their
 *   projectile - the same generic path the barbarian archer already proved out. Crossbow
 *   guards fire a bolt (gfx 27, no drawback, matching this codebase's own
 *   `RangedProjectile.BOLTS`); longbow guards fire a bronze arrow.
 * - **Combat style is set on spawn**, not in the combat def - `Npc.combatStyle` defaults to
 *   `STAB` and nothing in the engine copies a style out of `NpcCombatDef`, so the crush and
 *   ranged groups would otherwise roll against the wrong defence entirely.
 * - **Pickpocketing them now works.** All eleven ids carry a real "Pickpocket" cache option,
 *   but the Thieving config only listed the generic `guard_397..400` ids, so spawning these
 *   would have given them a menu option that silently did nothing. The eleven Falador ids
 *   were added to the existing guard entry in `data/cfg/thieving/pickpockets.json`, which
 *   already carried the right level-40 requirement and loot - no new plugin code needed,
 *   since `PickpocketPlugin` binds off whatever ids that file lists.
 *
 * Drops come from [GuardDrops], which every city's guards share - the wiki publishes a
 * single guard drop table with no per-location versioning, so Falador's and Varrock's
 * guards roll the identical one. See that file for what of it is and isn't modelled.
 */
class FaladorGuardPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        FaladorGuardData.SPAWNS.forEach { spawn ->
            spawnNpc(
                npc = spawn.npcKey,
                x = spawn.x,
                z = spawn.z,
                height = spawn.height,
                walkRadius = if (spawn.height == 0) 4 else 2,
                direction = Direction.SOUTH,
            )
        }

        FaladorGuardData.GROUPS.forEach { group ->
            group.npcKeys.forEach { npcKey ->
                setCombatDef(npcKey) {
                    configs {
                        attackSpeed = group.attackSpeed
                        respawnDelay = FaladorGuardData.RESPAWN_CYCLES
                    }
                    stats {
                        hitpoints = group.hitpoints
                        attack = group.attack
                        strength = group.strength
                        defence = group.defence
                        magic = 1
                        ranged = group.ranged
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
                        attack = group.attackAnimation
                        block = group.blockAnimation
                        death = FaladorGuardData.DEATH_ANIMATION
                    }
                    if (group.combatStyle == CombatStyle.RANGED) {
                        ranged {
                            projectile = group.projectile
                            if (group.drawback != -1) {
                                drawback = group.drawback
                            }
                            type = if (group.drawback == -1) ProjectileType.BOLT else ProjectileType.ARROW
                        }
                    }
                }

                onNpcSpawn(npc = npcKey) { npc.combatStyle = group.combatStyle }

                onNpcDeath(npcKey) { onDeath(npc) }
            }
        }
    }

    private fun onDeath(npc: Npc) = GuardDrops.rollOnDeath(npc)
}
