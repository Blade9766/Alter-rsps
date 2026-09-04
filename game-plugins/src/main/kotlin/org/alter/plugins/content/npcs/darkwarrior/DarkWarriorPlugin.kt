package org.alter.plugins.content.npcs.darkwarrior

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Dark warriors: spawns, combat definitions and drops. Stats and tiles come from [DarkWarriors],
 * the drop tables from [DarkWarriorDrops]; this file is the wiring.
 *
 * They need no custom attack loop. Every version is an ordinary melee monster - `attack style =
 * Slash`, `range = 1`, no special mechanic on the page - so the engine's default combat cycle in
 * [org.alter.plugins.content.combat.CombatPlugin] drives them once the combat def exists.
 *
 * Points worth knowing, all checked rather than assumed:
 *
 * - **Combat style is declared in `configs { }`, not in an `onNpcSpawn` hook.** Older packages
 *   here set `npc.combatStyle` on spawn because nothing copied a style out of the def;
 *   `World.setNpcDefaults` now does, so `combatStyle = SLASH` in the def is enough and a player in
 *   platelegs takes their slash defence against these rather than their stab.
 * - **`aggroTimer` is still named explicitly** even though [DarkWarriors.AGGRO_CYCLES] is now the
 *   builder's own default, because the Wilderness split below reads better as a single either/or
 *   than as one branch and an implicit fallthrough. All five versions are `aggressive = Yes`.
 * - **The two Wilderness versions use `alwaysAggro()`.** Wilderness monsters ignore the
 *   "gives up above twice its own combat level" rule that
 *   [org.alter.plugins.content.mechanics.aggro.NpcAggroPlugin] otherwise applies. Without it the
 *   level 8s would ignore anyone above combat 16 and the fortress would be a quiet place to walk
 *   through, which is the opposite of what it is. The three Kourend versions get the default
 *   check, since nothing on the page claims they ignore combat level.
 * - **Slayer xp is declared** from the page's own `slayxp` fields (17 / 50 / 70 / 80 / 181.5).
 *   There is no `levelRequirement`; the page publishes no Slayer level to kill them.
 * - **No `immunities { }`** - `poisonresistance`, `venomresistance`, `immunecannon` and
 *   `immunethrall` are all 0/No on every version.
 *
 * The Dark Warriors' Fortress is already known to
 * [org.alter.plugins.content.areas.wilderness.Wilderness] as both a named location and a
 * multi-combat area; it simply had nothing standing in it until now.
 */
class DarkWarriorPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        DarkWarriors.SPAWNS.forEach { spawn ->
            spawnNpc(
                npc = spawn.npcKey,
                x = spawn.x,
                z = spawn.z,
                height = spawn.height,
                walkRadius = WALK_RADIUS,
                direction = Direction.SOUTH,
            )
        }

        DarkWarriors.VARIANTS.forEach { variant ->
            setCombatDef(variant.npcKey) {
                configs {
                    attackSpeed = DarkWarriors.ATTACK_SPEED
                    combatStyle = DarkWarriors.COMBAT_STYLE
                    respawnDelay = DarkWarriors.RESPAWN_CYCLES
                }
                aggro {
                    radius = DarkWarriors.AGGRO_RADIUS
                    searchDelay = DarkWarriors.AGGRO_SEARCH_DELAY
                    if (variant.wilderness) {
                        alwaysAggro()
                    } else {
                        aggroTimer = DarkWarriors.AGGRO_CYCLES
                    }
                }
                stats {
                    hitpoints = variant.hitpoints
                    attack = variant.attack
                    strength = variant.strength
                    defence = variant.defence
                    magic = variant.magic
                    ranged = 1
                }
                bonuses {
                    attackBonus = variant.attackBonus
                    strengthBonus = variant.strengthBonus
                    defenceStab = variant.defenceStab
                    defenceSlash = variant.defenceSlash
                    defenceCrush = variant.defenceCrush
                }
                anims {
                    attack = DarkWarriors.ATTACK_ANIMATION
                    block = DarkWarriors.BLOCK_ANIMATION
                    death = DarkWarriors.DEATH_ANIMATION
                }
                slayerData {
                    xp = variant.slayerXp
                }
            }

            onNpcDeath(variant.npcKey) { DarkWarriorDrops.rollOnDeath(npc, variant) }
        }
    }

    private companion object {
        /** How far a dark warrior wanders from its post, matching the other monster packages. */
        const val WALK_RADIUS = 3
    }
}
