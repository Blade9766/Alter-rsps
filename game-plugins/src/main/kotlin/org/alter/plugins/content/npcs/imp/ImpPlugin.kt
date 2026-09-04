package org.alter.plugins.content.npcs.imp

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.collision.canOccupy
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.getCombatTarget
import org.alter.plugins.content.combat.isBeingAttacked
import org.alter.plugins.content.npcs.DropRoll
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.rscm.RSCM.getRSCM

/**
 * Imps: combat definitions, drops, spawns and the one behaviour that makes an imp an imp.
 *
 * Stats live in [Imps], the drop table in [ImpDrops] and the 120 spawn tiles in
 * [ImpSpawns]; this file is the wiring, and it follows the same shape as
 * `content/npcs/critters` and `content/npcs/goblin`. Spawning is done here rather than
 * from each area package for the reason [ImpSpawns] gives - imps are scattered over more
 * of the map than the area packages cover.
 *
 * Notes on what is and is not declared:
 * - **No `aggro { }`.** Both wiki versions are `aggressive = No`, the God Wars one
 *   included. An imp only ever fights back.
 * - **`species { +NpcSpecies.DEMON }`** on both, from `attributes = demon`. This is what
 *   Arclight, the demonbane swords and the scorching bow check
 *   ([org.alter.plugins.content.combat.specialattack.SpecialAttackEffects.isDemon]), and
 *   nothing derives it from the cache - an imp without this line is simply not a demon as
 *   far as the server is concerned.
 * - **Combat style is set on spawn**, not in the combat def, because the engine never
 *   copies a style out of the def. Both versions are `attack style = Stab`.
 * - **`defence { magic { elementWeakness } }`** for the published Water 10%.
 * - **No `ranged { }` and no `immunities { }`** - both versions are `range = 1`, and the
 *   page gives `poisonresistance = 0` and `venomresistance = 0`.
 *
 * **Imps do not flee.** The wiki files them under "Monsters that retreat when low
 * hitpoints", and they do not here, for exactly the reason
 * `content/npcs/critters/ChickenPlugin` gives about chickens: this engine has no notion of
 * an npc breaking off a fight, in [Npc] or anywhere in the combat plugins. Adding one is a
 * combat-engine change, not an imp change.
 */
class ImpPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Imps.VARIANTS.forEach { variant ->
            setCombatDef(variant.npcKey) {
                species {
                    Imps.SPECIES.forEach { +it }
                }
                configs {
                    attackSpeed = Imps.ATTACK_SPEED
                    respawnDelay = variant.respawnCycles
                }
                stats {
                    hitpoints = variant.hitpoints
                    attack = variant.attack
                    strength = variant.strength
                    defence = variant.defence
                    magic = Imps.MAGIC_LEVEL
                    ranged = Imps.RANGED_LEVEL
                }
                bonuses {
                    attackBonus = Imps.ATTACK_BONUS
                    strengthBonus = Imps.STRENGTH_BONUS
                    defenceStab = Imps.DEFENCE_BONUS
                    defenceSlash = Imps.DEFENCE_BONUS
                    defenceCrush = Imps.DEFENCE_BONUS
                    defenceMagic = Imps.DEFENCE_BONUS
                    defenceRanged = Imps.DEFENCE_BONUS
                }
                defence {
                    magic {
                        elementWeakness = Imps.ELEMENTAL_WEAKNESS
                    }
                }
                anims {
                    attack = Imps.ATTACK_ANIMATION
                    block = Imps.BLOCK_ANIMATION
                    death = Imps.DEATH_ANIMATION
                }
                /*
                 * Imps are not a Slayer assignment and the page publishes no `slayxp`, so
                 * this is only ever read if one is somehow made into a task - Slayer xp is
                 * awarded on task and nowhere else (see skills/slayer/Slayer.onKill). Set
                 * to hitpoints, the convention the rest of content/npcs follows, so that a
                 * future task gets a sane number rather than zero.
                 */
                slayerData {
                    xp = variant.hitpoints.toDouble()
                }
            }

            onNpcSpawn(npc = variant.npcKey) {
                npc.combatStyle = Imps.COMBAT_STYLE
                npc.timers[TELEPORT_TIMER] = world.random(TELEPORT_CHECK_CYCLES)
            }

            onNpcDeath(variant.npcKey) { onDeath(npc) }
        }

        ImpSpawns.HAUNTS.forEach { haunt ->
            haunt.tiles.forEach { (x, z) ->
                spawnNpc(
                    npc = haunt.npcKey,
                    x = x,
                    z = z,
                    height = haunt.height,
                    walkRadius = haunt.walkRadius,
                )
            }
        }

        onTimer(TELEPORT_TIMER) { tryTeleport(npc) }
    }

    /**
     * The imp's disappearing act. Mod Ash, quoted on the wiki: "They have a 1/4 chance of
     * trying it every 30-120 secs." That is what this is - a check every
     * [TELEPORT_CHECK_CYCLES] ticks (50 to 200 ticks is 30 to 120 seconds) that succeeds a
     * quarter of the time - and it is the single most recognisable thing an imp does.
     *
     * Three deliberate limits:
     *
     * - **No animation and no puff of smoke.** Npc 5007 is observed playing two sequences
     *   this file does not use, 4288 and 4289, which are its teleport pair - but nothing in
     *   this cache says which is the vanish and which is the arrival, and the smoke
     *   spotanim is not derivable from the cache at all. Rather than guess an id and show
     *   players the wrong effect, the imp simply relocates. Both are one line to add once
     *   somebody can source them.
     * - **Never mid-fight.** An imp being attacked, or attacking, stays put and re-arms the
     *   timer. Real imps are not this polite, but yanking an npc out from under an active
     *   fight is how this engine gets into the stale-combat states
     *   `areas/goblincave/objs/SearchBoxesPlugin` guards its despawn against, and the same
     *   guard is used here.
     * - **Never outside its own patch.** The destination is drawn within
     *   [TELEPORT_RANGE] of where the imp is standing and then rejected unless it is still
     *   inside the walk radius it was spawned with, so an imp cannot ratchet itself across
     *   the map one hop at a time.
     */
    private fun tryTeleport(imp: Npc) {
        imp.timers[TELEPORT_TIMER] = world.random(TELEPORT_CHECK_CYCLES)

        if (!imp.isActive() || !imp.lock.canMove()) {
            return
        }
        if (imp.isBeingAttacked() || imp.getCombatTarget() != null) {
            return
        }
        if (world.randomDouble() > TELEPORT_CHANCE) {
            return
        }
        destinationFor(imp)?.let(imp::moveTo)
    }

    /**
     * A walkable tile near [imp] and still within its walk radius, or null if the few
     * attempts made all landed somewhere it cannot stand.
     *
     * [org.alter.game.model.collision.canOccupy] reads an unallocated zone as blocked, so
     * an imp in a region no player has loaded yet finds nowhere to go and stays where it
     * is - which is the right answer, not a failure.
     */
    private fun destinationFor(imp: Npc): Tile? {
        val radius = imp.walkRadius.coerceAtLeast(1)
        repeat(TELEPORT_ATTEMPTS) {
            val dest =
                imp.tile.transform(
                    world.random(-TELEPORT_RANGE..TELEPORT_RANGE),
                    world.random(-TELEPORT_RANGE..TELEPORT_RANGE),
                )
            val withinPatch =
                Math.abs(dest.x - imp.spawnTile.x) <= radius && Math.abs(dest.z - imp.spawnTile.z) <= radius
            if (dest != imp.tile && withinPatch && world.collision.canOccupy(dest)) {
                return dest
            }
        }
        return null
    }

    /**
     * Rolled here rather than through the combat DSL's `drops { }` block, which builds a
     * loot table [org.alter.game.action.NpcDeathAction] never rolls - the same reason
     * `content/npcs/critters` rolls its own.
     *
     * The main table always yields something: the wiki's five sub-tables sum to 128 with no
     * "Nothing" row, so every imp leaves fiendish ashes plus one other item.
     */
    private fun onDeath(imp: Npc) {
        val killer = imp.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = imp.world

        val loot = ImpDrops.ALWAYS.map { getRSCM(it) to 1 }.toMutableList()

        DropRoll.pick(ImpDrops.TABLE, world)?.let { picked ->
            picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
        }

        if (world.randomDouble() <= ImpDrops.ENSOULED_HEAD_CHANCE) {
            loot.add(getRSCM("item.ensouled_imp_head") to 1)
        }
        if (world.randomDouble() <= ImpDrops.CHAMPION_SCROLL_CHANCE) {
            loot.add(getRSCM("item.imp_champion_scroll") to 1)
        }
        if (killer.inWilderness() && world.randomDouble() <= ImpDrops.LOOTING_BAG_CHANCE) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, imp.tile)
        }
    }

    private companion object {
        val TELEPORT_TIMER = TimerKey()

        /** 30 to 120 seconds, in game ticks. */
        val TELEPORT_CHECK_CYCLES = 50..200

        /** Mod Ash's "1/4 chance of trying it". */
        const val TELEPORT_CHANCE = 0.25

        /** "A short distance away" - the wiki does not put a number on it. */
        const val TELEPORT_RANGE = 5

        /** Tries before the imp gives up and stays put for another interval. */
        const val TELEPORT_ATTEMPTS = 8
    }
}
