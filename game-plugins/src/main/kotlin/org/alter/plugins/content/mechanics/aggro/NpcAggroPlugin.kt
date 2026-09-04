package org.alter.plugins.content.mechanics.aggro

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*
import org.alter.plugins.content.combat.SingleCombat
import org.alter.plugins.content.combat.getCombatTarget
import org.alter.plugins.content.combat.isAttacking

class NpcAggroPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    val AGGRO_CHECK_TIMER = TimerKey()

    init {
        onGlobalNpcSpawn {
            if (npc.combatDef.aggressiveRadius > 0 && npc.combatDef.aggroTargetDelay > 0) {
                /*
                 * Only when the npc has not already been given one. This hook is *global*, and
                 * global spawn hooks run after per-npc ones - so an unconditional assignment here
                 * silently discarded any `aggroCheck` a monster plugin installed for itself, and
                 * there was no other place for one to live.
                 *
                 * That is what a page like the battle mage's needs: `aggressive = Yes, unless
                 * wearing the same cape as the battle mage` is a per-monster exemption on top of
                 * the ordinary rules, not a replacement for the aggression system. See
                 * `content/npcs/battlemage`.
                 *
                 * Nothing else changes: `Npc.aggroCheck` starts null and only a plugin can set it,
                 * so every monster that does not sets the default exactly as before. A respawn
                 * re-runs the per-npc hook first, so a monster that wants its own check keeps it.
                 */
                if (npc.aggroCheck == null) {
                    npc.aggroCheck = defaultAggressiveness
                }
                npc.timers[AGGRO_CHECK_TIMER] = npc.combatDef.aggroTargetDelay
            }
        }

        onTimer(AGGRO_CHECK_TIMER) {
            if ((!npc.isAttacking() || npc.tile.isMulti(world)) && npc.lock.canAttack() && npc.isActive()) {
                checkRadius(npc)
            }
            npc.timers[AGGRO_CHECK_TIMER] = npc.combatDef.aggroTargetDelay
        }
    }
    


/**
 * The engine's ordinary aggressiveness check, as a class member so the plugin can install it.
 *
 * The rule itself lives in the top-level [defaultNpcAggressiveness] so that a monster plugin
 * wanting to *compose* an exemption on top of it - the battle mages' matching-cape rule, say - can
 * reach it. A class member cannot be imported; a top-level function can.
 */
val defaultAggressiveness: (Npc, Player) -> Boolean = ::defaultNpcAggressiveness

fun checkRadius(npc: Npc) {
    val radius = npc.combatDef.aggressiveRadius

    mainLoop@
    for (x in -radius..radius) {
        for (z in -radius..radius) {
            val tile = npc.tile.transform(x, z)
            val chunk = world.chunks.get(tile, createIfNeeded = false) ?: continue

            val players = chunk.getEntities<Player>(tile, EntityType.PLAYER, EntityType.CLIENT)
            if (players.isEmpty()) {
                continue
            }

            val targets = players.filter { canAttack(npc, it) }
            if (targets.isEmpty()) {
                continue
            }

            val target = targets.random()
            if (npc.getCombatTarget() != target) {
                npc.attack(target)
                /*
                 * Claim the target for single-way combat. `Pawn.attack` sets this npc's own combat
                 * focus synchronously, so the claim is already verifiable by the time the next
                 * npc's aggro timer fires later in this same cycle - which is exactly the case that
                 * matters, because a row of monsters spawned together share an aggro delay and
                 * would otherwise all engage on the same tick.
                 */
                SingleCombat.claim(npc, target)
            }
            break@mainLoop
        }
    }
}

fun canAttack(
    npc: Npc,
    target: Player,
): Boolean {
    if (!target.isOnline || target.invisible) {
        return false
    }
    if (!singleCombatAllows(npc, target)) {
        return false
    }
    return npc.aggroCheck == null || npc.aggroCheck?.invoke(npc, target) == true
}

/**
 * Single-way combat, at the aggression layer: outside a multi-combat area a monster will not pick a
 * player who is already fighting, or who another monster has already claimed.
 *
 * The rule itself lives in [SingleCombat], which is also what the dark wizards' and slayer casters'
 * own attack loops ask - it was implemented three separate times before it was implemented once, and
 * two of those copies could not even see each other's claims. This is the aggression-layer half of
 * it; see that object for the whole story.
 */
fun singleCombatAllows(
    npc: Npc,
    target: Player,
): Boolean = SingleCombat.mayEngage(npc, target)

}

/**
 * Whether [n] is aggressive towards [p] under the engine's ordinary rules: the two sentinel
 * `aggressiveTimer` values, then the timer since the player entered the area, then the standard
 * "npcs stop caring about anyone above twice their combat level".
 *
 * Top level rather than a member of [NpcAggroPlugin] so that a monster with a *published* exemption
 * can put its own check in front of this one instead of replacing the whole thing. `world` comes
 * from the npc rather than from the plugin, which is what makes that possible.
 */
fun defaultNpcAggressiveness(
    n: Npc,
    p: Player,
): Boolean {
    if (n.combatDef.aggressiveTimer == Int.MAX_VALUE) {
        return true
    } else if (n.combatDef.aggressiveTimer == Int.MIN_VALUE) {
        return false
    }

    if (Math.abs(n.world.currentCycle - p.lastMapBuildTime) > n.combatDef.aggressiveTimer) {
        return false
    }

    val npcLvl = n.def.combatLevel
    return p.combatLevel <= npcLvl * 2
}
