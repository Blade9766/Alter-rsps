package org.alter.plugins.content.combat

import org.alter.api.ext.isMulti
import org.alter.game.model.attr.SINGLE_COMBAT_ATTACKER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import java.lang.ref.WeakReference

/**
 * Single-way combat: outside a multi-combat area, one monster fights a given target at a time.
 *
 * ## Why this is one object and not three
 *
 * This rule got implemented three separate times before it got implemented once.
 *
 * `content/npcs/darkwizard` wrote it first, and its doc explains why it had to:
 * *"this codebase has no engine-level single/multi-combat zone enforcement at all -
 * `setMultiCombatRegion`/`Tile.isMulti` only ever drive the client's multi-combat icon varbit, they
 * don't gate whether more than one NPC can simultaneously engage the same target... Without a claim
 * system every nearby wizard would independently attack the instant it's in range, which reads as
 * 'attacks way too fast, all at once'."* `content/npcs/slayer` then copied that method verbatim for
 * the aberrant spectres and infernal mages, and the second bestiary pass added a third at the
 * aggression layer after a pack of green dragons all breathed at once.
 *
 * The two copies did not even interoperate: each declared its **own private** `ENGAGED_BY`
 * attribute key, so a dark wizard and an aberrant spectre could hold a claim on the same player
 * simultaneously and neither would see the other's. Anything standing in both groups' range got
 * exactly the dogpile the mechanism was written to prevent.
 *
 * All three now go through here, against the one engine-level [SINGLE_COMBAT_ATTACKER_ATTR].
 *
 * ## Two entry points, because there are two questions
 *
 * - [mayEngage] is asked at the **aggression** layer, before a monster picks a target at all. It is
 *   read-only, and it additionally refuses a player who is already fighting something else - a
 *   monster should not jump into someone else's fight unprompted.
 * - [claim] is asked inside a monster's own **attack loop**, every swing. It takes the claim as well
 *   as testing it, and it deliberately does *not* consider whether the target is fighting someone
 *   else, because in that case the caller usually **is** that someone else. Requiring it would stop
 *   a monster fighting back.
 *
 * Both let a fight in a multi-combat area through untouched.
 *
 * ## What the merge changed, and what it did not
 *
 * **Changed:** the two copied implementations applied everywhere, including the Wilderness, which
 * `content/npcs/darkwizard`'s doc flagged as a known simplification - *"it applies everywhere,
 * including the Wilderness spawns, where real OSRS would let several wizards pile on at once -
 * traded off deliberately since 'wizards no longer dogpile' was the actual complaint"*. They now
 * respect multi-combat areas, because there is finally something for them to respect. In a multi
 * zone wizards and spectres will pile on again, which is correct.
 *
 * **Not changed:** the validation. A claim is honoured only while its holder is still spawned, still
 * alive and still has this target as its own combat target - the same three conditions both copies
 * checked - so a monster that dies, despawns or wanders off never locks a player out of combat.
 */
object SingleCombat {
    /**
     * Whether [npc] may *start* a fight with [target] - the aggression-layer question.
     *
     * Read-only: taking the claim is [claim]'s job, so that a monster which is refused here has left
     * nothing behind.
     */
    fun mayEngage(
        npc: Npc,
        target: Pawn,
    ): Boolean {
        if (target.tile.isMulti(npc.world)) {
            return true
        }
        /*
         * Already swinging at something else. Only the aggression layer asks this: inside an attack
         * loop the caller is usually the very thing the target is fighting.
         */
        val fighting = target.getCombatTarget()
        if (fighting != null && fighting != npc) {
            return false
        }
        return !heldByAnotherThan(npc, target)
    }

    /**
     * Takes, or keeps, the right to attack [target] this cycle - the attack-loop question.
     *
     * Returns false when somebody else holds a live claim, in which case the caller should do
     * something other than swing; `content/npcs/darkwizard` shuffles on the spot, which is what
     * stopped its wizards reading as a row of statues.
     */
    fun claim(
        npc: Npc,
        target: Pawn,
    ): Boolean {
        if (target.tile.isMulti(npc.world)) {
            return true
        }
        if (heldByAnotherThan(npc, target)) {
            return false
        }
        target.attr[SINGLE_COMBAT_ATTACKER_ATTR] = WeakReference(npc)
        return true
    }

    /** Whether [npc] is the one currently holding [target]'s claim. */
    fun holds(
        npc: Npc,
        target: Pawn,
    ): Boolean = target.attr[SINGLE_COMBAT_ATTACKER_ATTR]?.get() === npc

    /**
     * Gives up [npc]'s claim on [target], if it holds it.
     *
     * Not strictly required - [heldByAnotherThan] validates every claim on read, so an abandoned one
     * expires by itself the moment its holder drops the target - but releasing at the end of an
     * attack loop frees the player on the same cycle rather than the next one, and both copied
     * implementations did it.
     */
    fun release(
        npc: Npc,
        target: Pawn,
    ) {
        if (holds(npc, target)) {
            target.attr.remove(SINGLE_COMBAT_ATTACKER_ATTR)
        }
    }

    /**
     * Whether somebody other than [npc] holds a claim on [target] that is still live.
     *
     * The three conditions are the ones both copied implementations used. Note that none of them is
     * `Npc.isActive()`: that is a flag for whether an npc's AI is worth processing this cycle, false
     * whenever no player is near enough to see it, and treating it as liveness would drop a real
     * claim the moment its holder flickered inactive.
     */
    private fun heldByAnotherThan(
        npc: Npc,
        target: Pawn,
    ): Boolean {
        val holder = target.attr[SINGLE_COMBAT_ATTACKER_ATTR]?.get() ?: return false
        if (holder === npc) {
            return false
        }
        return holder is Npc && holder.isSpawned() && holder.isAlive() && holder.getCombatTarget() === target
    }
}
