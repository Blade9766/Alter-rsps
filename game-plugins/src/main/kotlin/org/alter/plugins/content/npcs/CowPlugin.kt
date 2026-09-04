package org.alter.plugins.content.npcs

import org.alter.api.cfg.Animation
import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.isBeingAttacked
import org.alter.rscm.RSCM.getRSCM

/**
 * Cows and cow calves: where they stand, how they fight, and what they leave behind.
 *
 * Spawns, ids and stats come from [Cows]; this file is the wiring, and it follows the same
 * shape as `content/npcs/critters` and `content/npcs/goblin`. The two [Bovine] variants go
 * through identical code because the pages agree on everything except four numbers and three
 * sounds - see [Bovine].
 *
 * What changed against the plugin this replaces, and why each one was wrong:
 *
 * - **Sixteen invented Lumbridge tiles are gone**, replaced by the wiki's 128 cow pins across
 *   fourteen locations and 35 calf pins across nine. See [Cows].
 * - **Calves exist at all.** 2792, 2794 and 2801 had no combat def and no spawn; they are a
 *   separate wiki page and were simply missing.
 * - **All five cow ids now carry the combat def**, not just `npc.cow` (2790). The other four
 *   were inheriting [org.alter.game.model.combat.NpcCombatDef.DEFAULT] - 10 hitpoints, zeroed
 *   stats - and nothing said so, because nothing spawned them.
 * - **`combatStyle` is set on spawn.** Both pages are `attack style = Crush`, and
 *   [org.alter.game.model.entity.Npc.combatStyle] defaults to STAB with the engine never
 *   copying a style out of the def. Every cow on this server was rolling against the player's
 *   stab defence.
 * - **Attack and strength bonuses are declared.** The cow def listed only the five defence
 *   bonuses, so its -15/-15 sat at the builder's 0 default - a cow fractionally *more*
 *   accurate than a real one.
 * - **Poison and venom immunity are declared.** Both pages give `poisonresistance = 100` and
 *   `venomresistance = 100`. The old def set `poisonChance`/`venomChance` to 0, which is a
 *   different fact - that is the chance the animal *inflicts* poison, not its resistance to it
 *   - so a poisoned weapon still worked. Both are kept: the chances because `poisonous = No`,
 *   the immunities because 100% resistance is the published number.
 * - **The beginner clue scroll is dropped**, 1/128 on both pages. The three 100% drops were
 *   already right for cows and are the same for calves.
 *
 * The "Moo" chatter now covers every cow and calf id rather than 2790 alone. Both pages list
 * `Cow mooing.wav`, so the calves moo too.
 *
 * Not done here, and flagged rather than faked: the **dairy cow**, which on this server is
 * scenery with a Milk action handled by `mechanics/dairy` rather than an npc.
 */
class CowPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        val mooDelay = TimerKey()

        Cows.ALL.forEach { bovine ->
            bovine.herds.forEach { herd ->
                herd.tiles.forEachIndexed { index, (x, z) ->
                    spawnNpc(
                        npc = herd.npcKeys[index % herd.npcKeys.size],
                        x = x,
                        z = z,
                        walkRadius = herd.walkRadius,
                        direction = FACINGS[index % FACINGS.size],
                    )
                }
            }

            bovine.combatDefIds.forEach { npcKey ->
                setCombatDef(npcKey) {
                    configs {
                        attackSpeed = Cows.ATTACK_SPEED
                        respawnDelay = Cows.RESPAWN_CYCLES
                        // Wiki `poisonous = No`: the hit never applies poison or venom. Not the
                        // same field as the immunities block below.
                        poisonChance = 0.0
                        venomChance = 0.0
                    }
                    stats {
                        hitpoints = bovine.hitpoints
                        attack = Cows.ATTACK
                        strength = Cows.STRENGTH
                        defence = Cows.DEFENCE
                        magic = Cows.MAGIC
                        ranged = Cows.RANGED
                    }
                    bonuses {
                        attackBonus = bovine.attackBonus
                        strengthBonus = bovine.strengthBonus
                        defenceStab = bovine.defenceBonus
                        defenceSlash = bovine.defenceBonus
                        defenceCrush = bovine.defenceBonus
                        defenceMagic = bovine.defenceBonus
                        defenceRanged = bovine.defenceBonus
                    }
                    immunities {
                        poison = true
                        venom = true
                    }
                    anims {
                        attack = Animation.COW_ATTACK
                        block = Animation.COW_HIT
                        death = Animation.COW_DEATH
                    }
                    sound {
                        attackSound = bovine.attackSound
                        blockSound = bovine.blockSound
                        deathSound = bovine.deathSound
                    }
                    /*
                     * Slayer experience equals the monster's hitpoints on both pages - 8 for a
                     * cow, 6 for a calf, the same figures the stats block declares. Cows are a
                     * Turael task and calves count for it too, sharing the "Cows" category.
                     */
                    slayerData {
                        xp = bovine.slayerXp
                    }
                }

                onNpcSpawn(npc = npcKey) {
                    npc.combatStyle = CombatStyle.CRUSH
                    npc.timers[mooDelay] = world.random(MOO_DELAY)
                }

                onNpcDeath(npcKey) {
                    val npc = npc
                    val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return@onNpcDeath

                    val loot = ALWAYS_DROPS.toMutableList()
                    if (world.randomDouble() <= Cows.BEGINNER_CLUE_CHANCE) {
                        loot.add("item.clue_scroll_beginner")
                    }

                    loot.forEach { item ->
                        world.spawn(
                            GroundItem(
                                item = getRSCM(item),
                                amount = 1,
                                tile = npc.tile,
                                owner = killer,
                            ),
                        )
                    }
                }
            }
        }

        onTimer(mooDelay) {
            val npc = npc
            if (!npc.isBeingAttacked()) {
                npc.forceChat("Moo")
            }
            npc.timers[mooDelay] = world.random(MOO_DELAY)
        }
    }

    private companion object {
        /**
         * The three 100% drops, in the order the wiki's tables list them - identical on both
         * pages, calves included. The order is cosmetic here: OSRS sorts a drop pile
         * client-side (which is what the 2018 "cowhides will now appear at the top of the drop
         * pile" change adjusted) and this server just spawns three ground items on one tile.
         */
        val ALWAYS_DROPS = listOf("item.cowhide", "item.raw_beef", "item.bones")

        /** Cycles between one "Moo" and the next, unchanged from the original plugin. */
        val MOO_DELAY = 100..200

        /**
         * Dealt round the herd so a field is not fifty animals all facing south. Which way one
         * faces is not published and does not matter - it is overwritten the moment the animal
         * walks or is attacked.
         */
        val FACINGS = listOf(Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST)
    }
}
