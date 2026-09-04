package org.alter.plugins.content.combat.specialattack

import org.alter.api.NpcSpecies
import org.alter.api.ext.sendRunEnergy
import org.alter.api.ext.setVarp
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.attr.SPECIAL_ATTACK_BURN_ATTR
import org.alter.game.model.attr.SPECIAL_ATTACK_BURN_SOURCE_ATTR
import org.alter.game.model.timer.ACTIVE_COMBAT_TIMER
import org.alter.game.model.timer.DAMAGE_TAKEN_MODIFIER_TIMER
import org.alter.game.model.timer.MELEE_DAMAGE_TAKEN_MODIFIER_TIMER
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.dealExactHit
import org.alter.game.model.timer.SPECIAL_ATTACK_BURN_TIMER
import org.alter.plugins.content.mechanics.run.RunEnergy.RUN_ENABLED_VARP
import java.lang.ref.WeakReference

/**
 * The handful of things special attacks do to a target beyond dealing damage - draining a combat
 * stat, and asking what species it is.
 *
 * Stat drains are the reason this is shared. Players and npcs keep their levels in two different
 * places indexed two different ways: a player's are [org.alter.api.Skills] constants on
 * `getSkills()`, an npc's are [org.alter.api.NpcSkills] constants on `stats`, and the two
 * numberings disagree (Strength is 2 for a player and 1 for an npc). Every special that drains -
 * the dragon warhammer, the elder maul, the godswords, the anchor, Darklight - would otherwise
 * repeat that mapping, and getting it wrong drains a different stat than the one advertised.
 */
object SpecialAttackEffects {
    /** [target]'s current level in the player-numbered [skill], or 0 if it has no such stat. */
    fun currentLevel(
        target: Pawn,
        skill: Int,
    ): Int =
        when (target) {
            is Player -> target.getSkills().getCurrentLevel(skill)
            is Npc -> Combat.toNpcSkill(skill)?.let { target.stats.getCurrentLevel(it) } ?: 0
            else -> 0
        }

    /**
     * Drains [amount] levels of the player-numbered [skill] from [target], never below zero.
     *
     * Uncapped on purpose: a drain from a special attack is not restored by the level's own regen
     * cap the way a temporary boost is, so it takes the level down from wherever it currently sits.
     */
    fun drain(
        target: Pawn,
        skill: Int,
        amount: Int,
    ) {
        if (amount <= 0) {
            return
        }
        when (target) {
            is Player -> {
                val current = target.getSkills().getCurrentLevel(skill)
                target.getSkills().setCurrentLevel(skill, (current - amount).coerceAtLeast(0))
            }
            is Npc -> {
                val npcSkill = Combat.toNpcSkill(skill) ?: return
                val current = target.stats.getCurrentLevel(npcSkill)
                target.stats.setCurrentLevel(npcSkill, (current - amount).coerceAtLeast(0))
            }
            else -> {}
        }
    }

    /**
     * Drains [percent] (as a fraction, `0.3` for 30%) of [target]'s *current* level in [skill].
     *
     * Off the current level, not the base one - which is why a second dragon warhammer special
     * takes less than the first, exactly as it does in the real game.
     */
    fun drainPercent(
        target: Pawn,
        skill: Int,
        percent: Double,
    ) {
        val current = currentLevel(target, skill)
        drain(target, skill, (current * percent).toInt())
    }

    /** Whether [target] is a demon, for the demonbane specials. */
    fun isDemon(target: Pawn): Boolean = target is Npc && NpcSpecies.DEMON in target.species

    /**
     * Spreads [total] damage over [ticks] game ticks as a run of one-damage-or-more splats.
     *
     * The shape every "damage over time" special shares - the ursine chainmace's 20 over six
     * seconds, the burning claws' burn, Morrigan's javelin's delayed repeat. There is no engine
     * concept of a damage-over-time effect, and there does not need to be: a queued hit already
     * takes a delay, so this is a handful of them spaced out. They keep landing if the attacker
     * walks away, which is what the real effects do.
     */
    fun damageOverTime(
        source: Pawn,
        target: Pawn,
        total: Int,
        ticks: Int,
        splats: Int = 4,
    ) {
        if (total <= 0 || splats <= 0) {
            return
        }
        val each = total / splats
        val remainder = total % splats
        val spacing = (ticks / splats).coerceAtLeast(1)
        repeat(splats) { index ->
            val damage = each + if (index < remainder) 1 else 0
            if (damage > 0) {
                source.dealExactHit(target = target, damage = damage, landHit = true, delay = spacing * (index + 1))
            }
        }
    }

    /**
     * A burn: 10 damage over 40 ticks, the effect the Varlamore demonbane weapons apply, stacking
     * up to five at once.
     *
     * Unlike [damageOverTime] this is a counter rather than a set of queued hits, ticked down by
     * [SpecialAttackTemporariesPlugin]. It has to be: the eclipse atlatl consumes whatever burn is
     * left on its target and adds it to its own max hit, and a hit already queued cannot be called
     * back.
     */
    fun burn(
        source: Pawn,
        target: Pawn,
    ) {
        val remaining = target.attr[SPECIAL_ATTACK_BURN_ATTR] ?: 0
        target.attr[SPECIAL_ATTACK_BURN_ATTR] = (remaining + BURN_DAMAGE).coerceAtMost(BURN_DAMAGE * MAX_BURN_STACKS)
        target.attr[SPECIAL_ATTACK_BURN_SOURCE_ATTR] = WeakReference(source)
        if (!target.timers.has(SPECIAL_ATTACK_BURN_TIMER)) {
            target.timers[SPECIAL_ATTACK_BURN_TIMER] = BURN_TICKS_BETWEEN_SPLATS
        }
    }

    /** Takes every point of burn still owed off [target] and hands it back to the caller. */
    fun consumeBurn(target: Pawn): Int {
        val remaining = target.attr[SPECIAL_ATTACK_BURN_ATTR] ?: 0
        clearBurn(target)
        return remaining
    }

    fun clearBurn(target: Pawn) {
        target.attr.remove(SPECIAL_ATTACK_BURN_ATTR)
        target.attr.remove(SPECIAL_ATTACK_BURN_SOURCE_ATTR)
        target.timers.remove(SPECIAL_ATTACK_BURN_TIMER)
    }

    /** 10 damage over 40 ticks - two every eight. */
    const val BURN_DAMAGE = 10
    const val BURN_DAMAGE_PER_TICK = 2
    const val BURN_TICKS_BETWEEN_SPLATS = 8
    const val MAX_BURN_STACKS = 5

    /**
     * Temporarily multiplies the damage [target] takes, from every style bar magic.
     *
     * Cleared by [org.alter.plugins.content.combat.specialattack.SpecialAttackTemporariesPlugin]
     * when the timer lapses. Only one such modifier can be in force at a time - the last one wins -
     * which is fine for the one special that applies it to a target (Wrath of Amascut).
     */
    fun amplifyDamageTaken(
        target: Pawn,
        multiplier: Double,
        ticks: Int,
    ) {
        target.attr[Combat.DAMAGE_TAKE_MULTIPLIER] = multiplier
        target.timers[DAMAGE_TAKEN_MODIFIER_TIMER] = ticks
    }

    /** As [amplifyDamageTaken], but melee damage only - Power of Death and Spear Wall. */
    fun setMeleeDamageTaken(
        target: Pawn,
        multiplier: Double,
        ticks: Int,
    ) {
        target.attr[Combat.MELEE_DAMAGE_TAKE_MULTIPLIER] = multiplier
        target.timers[MELEE_DAMAGE_TAKEN_MODIFIER_TIMER] = ticks
    }

    /**
     * Adds [ticks] to the delay before the attacker's next attack.
     *
     * Left on an attribute for `Combat.postAttack` to pick up, because it runs after the special
     * and would overwrite anything set here directly.
     */
    fun slowNextAttack(
        player: Player,
        ticks: Int,
    ) {
        player.attr[Combat.EXTRA_ATTACK_DELAY] = (player.attr[Combat.EXTRA_ATTACK_DELAY] ?: 0) + ticks
    }

    /**
     * Stops [target] running, by the only route the engine has: drain the energy and drop the run
     * toggle, exactly as `RunEnergy` itself does when a player runs the bar dry.
     *
     * Not a timed lock - the target starts running again once energy regenerates - so the effect is
     * a run-energy wipe rather than the real six-tick prohibition. Npcs have no run energy and are
     * left alone.
     */
    fun stopRunning(target: Pawn) {
        if (target !is Player) {
            return
        }
        target.runEnergy = 0.0
        target.setVarp(RUN_ENABLED_VARP, 0)
        target.sendRunEnergy(0)
    }

    /**
     * Whether [target] has not yet been drawn into this fight - the "unsuspecting" condition the
     * bone dagger's Backstab and the Dorgeshuun crossbow's Snipe want.
     *
     * `Combat.postAttack` stamps [ACTIVE_COMBAT_TIMER] on whatever is attacked, and it runs *after*
     * the special, so the opening attack of a fight sees a clean target and everything after it
     * does not. That is precisely the real condition, for free.
     */
    fun isUnsuspecting(target: Pawn): Boolean = !target.timers.has(ACTIVE_COMBAT_TIMER)
}
