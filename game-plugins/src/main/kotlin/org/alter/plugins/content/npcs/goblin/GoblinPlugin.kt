package org.alter.plugins.content.npcs.goblin

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Combat definitions and drops for every goblin variant. Stats come from [Goblins], the
 * two drop tables from [GoblinDrops]; this file is the wiring.
 *
 * Spawns are deliberately *not* here. Unlike the guards or dark wizards, goblins are an
 * area-defining monster - their placement belongs with the rest of the area's population
 * in `areas/lumbridge/spawns/SpawnPlugin`, which is where they already lived.
 *
 * Notes on what is and is not declared:
 * - **`aggro { }` on exactly one id.** The wiki is `aggressive = No` on all four versions;
 *   the sole exception anywhere in the game is the armed Goblin Cave sentry, which is why
 *   [Goblins.CAVE_SENTRY_ID] is split into its own single-id variant rather than sharing
 *   the armed set. `alwaysAggro()` matches the wiki's "aggressive no matter what level you
 *   are" - without it [org.alter.plugins.content.mechanics.aggro.NpcAggroPlugin]'s default
 *   check would stop it attacking anyone above combat level 4.
 * - **Combat style is set on spawn**, not in the combat def:
 *   [org.alter.game.model.entity.Npc.combatStyle] defaults to STAB and the engine never
 *   copies a style out of [org.alter.game.model.combat.NpcCombatDef]. Without this the
 *   level 2 and level 13 goblins would roll against the player's *stab* defence instead
 *   of crush. It matters more here than usual: the level 5s really are stab, so the bug
 *   would have been invisible on one variant and wrong on the other three.
 * - **No `ranged { }` block** - every version is `range = 1`.
 * - **Animations are stated explicitly.** Setting a combat def at all takes an npc off
 *   [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin]'s resolver path,
 *   so the goblin animations that plugin was already supplying have to be repeated in the
 *   def or the goblins would silently revert to the human 422/424/836 fallbacks. Combat
 *   sounds still come from that plugin's own `GOBLIN` entry.
 */
class GoblinPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Goblins.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                setCombatDef(npcKey) {
                    configs {
                        attackSpeed = variant.attackSpeed
                        respawnDelay = Goblins.RESPAWN_CYCLES
                    }
                    if (variant.aggroRadius > 0) {
                        aggro {
                            radius = variant.aggroRadius
                            searchDelay = AGGRO_SEARCH_DELAY
                            alwaysAggro()
                        }
                    }
                    stats {
                        hitpoints = variant.hitpoints
                        attack = variant.attack
                        strength = variant.strength
                        defence = variant.defence
                        magic = Goblins.MAGIC_LEVEL
                        ranged = Goblins.RANGED_LEVEL
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

                onNpcDeath(npcKey) { GoblinDrops.rollOnDeath(npc, variant.dropTable) }
            }
        }

        // The sentry's challenge. On the wiki this is an overhead message every player
        // inside the Goblin Cave sees, wherever they are standing; forceChat renders
        // through the npc update block, so here it reaches whoever currently has the
        // goblin in view. Same npc, same words, heard when you are near enough to see it
        // shout - a deliberate simplification rather than a region-wide broadcast.
        onNpcSpawn(npc = Goblins.CAVE_SENTRY_ID) {
            npc.timers[SENTRY_YELL_DELAY] = world.random(YELL_DELAY_CYCLES)
        }

        onTimer(SENTRY_YELL_DELAY) {
            npc.forceChat("Halt intruder!")
            npc.timers[SENTRY_YELL_DELAY] = world.random(YELL_DELAY_CYCLES)
        }
    }

    private companion object {
        /** Cycles between aggro sweeps for the cave sentry. */
        const val AGGRO_SEARCH_DELAY = 4

        /** Roughly 30-60 seconds at 0.6s per cycle: the wiki's "regular time intervals". */
        val YELL_DELAY_CYCLES = 50..100

        val SENTRY_YELL_DELAY = TimerKey()
    }
}
