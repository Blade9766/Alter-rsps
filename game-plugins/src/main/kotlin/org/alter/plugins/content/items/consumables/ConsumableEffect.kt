package org.alter.plugins.content.items.consumables

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.model.attr.ANTIFIRE_POTION_CHARGES_ATTR
import org.alter.game.model.attr.DRAGONFIRE_IMMUNITY_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.ANTIFIRE_TIMER
import org.alter.game.model.timer.TimerKey
import org.alter.plugins.content.mechanics.poison.Poison
import org.alter.plugins.content.items.consumables.food.Foods
import org.alter.plugins.content.items.consumables.potions.Divine
import org.alter.plugins.content.items.consumables.potions.Potions
import org.alter.plugins.content.mechanics.run.RunEnergy

/**
 * One thing a consumable does to the player. A potion or a piece of food carries a list of these,
 * applied in order when it is used.
 */
sealed interface ConsumableEffect {
    fun apply(p: Player)
}

/**
 * Raises [skill] by `flat + percent% of the base level`, up to that much above the base level.
 *
 * This is the shape every combat boost takes, including the Saradomin brew boosts that are allowed
 * to carry hitpoints and defence over their maximum - the cap is always "base level plus the boost",
 * so a second dose tops the stat back up without stacking past it.
 */
data class Boost(
    val skill: Int,
    val flat: Int,
    val percent: Int,
) : ConsumableEffect {
    override fun apply(p: Player) {
        val skills = p.getSkills()
        val base = skills.getBaseLevel(skill)
        val boost = flat + (base * percent) / 100
        val cap = base + boost
        val current = skills.getCurrentLevel(skill)
        if (current < cap) {
            skills.setCurrentLevel(skill, Math.min(cap, current + boost))
        }
    }
}

/**
 * Raises [skill] the way [Boost] does, and then holds it there for
 * [Divine.DURATION_TICKS] instead of letting it decay a level a minute.
 *
 * The boost itself is delegated rather than duplicated so the two can never drift apart; all this
 * adds is the floor the restore cycle has to respect.
 */
data class DivineBoost(
    val skill: Int,
    val flat: Int,
    val percent: Int,
) : ConsumableEffect {
    override fun apply(p: Player) {
        Boost(skill, flat, percent).apply(p)

        val base = p.getSkills().getBaseLevel(skill)
        Divine.protect(p, skill, base + flat + (base * percent) / 100)
    }
}

/**
 * Costs the player [amount] hitpoints.
 *
 * Only the divine potions do this, and only after `PotionPlugin` has refused the dose to anyone
 * who would not survive it - so this deliberately does not guard against killing the drinker
 * itself, and would if anything else ever used it.
 */
data class Damage(val amount: Int) : ConsumableEffect {
    override fun apply(p: Player) {
        p.hit(amount)
    }
}

/**
 * Lowers [skill] by `flat + percent% of the current level`, never below [floor].
 *
 * The brews drain from the current level rather than the base, so each successive dose takes less.
 */
data class Drain(
    val skill: Int,
    val flat: Int,
    val percent: Int,
    val floor: Int = 0,
) : ConsumableEffect {
    override fun apply(p: Player) {
        val skills = p.getSkills()
        val current = skills.getCurrentLevel(skill)
        val drain = flat + (current * percent) / 100
        skills.setCurrentLevel(skill, Math.max(floor, current - drain))
    }
}

/**
 * Recovers `flat + percent% of the base level` in every skill [skills] selects, never past the base
 * level. Boosted stats are left alone.
 */
data class Restore(
    val flat: Int,
    val percent: Int,
    val skills: (Int) -> Boolean,
) : ConsumableEffect {
    override fun apply(p: Player) {
        val stats = p.getSkills()
        for (skill in 0 until stats.maxSkills) {
            if (!skills(skill)) {
                continue
            }
            val base = stats.getBaseLevel(skill)
            val current = stats.getCurrentLevel(skill)
            if (current >= base) {
                continue
            }
            val amount = flat + (base * percent) / 100
            stats.setCurrentLevel(skill, Math.min(base, current + amount))
        }
    }
}

/**
 * Recovers `flat + percent% of the base Prayer level` prayer points, or [boostedPercent] instead
 * while the player is wearing something that improves prayer restoration.
 */
data class RestorePrayer(
    val flat: Int,
    val percent: Int,
    val boostedPercent: Int = percent,
) : ConsumableEffect {
    override fun apply(p: Player) {
        val skills = p.getSkills()
        val base = skills.getBaseLevel(Skills.PRAYER)
        val current = skills.getCurrentLevel(Skills.PRAYER)
        if (current >= base) {
            return
        }
        val effectivePercent = if (Potions.hasPrayerRestoreBonus(p)) boostedPercent else percent
        val amount = flat + (base * effectivePercent) / 100
        skills.setCurrentLevel(Skills.PRAYER, Math.min(base, current + amount))
    }
}

/**
 * Cures poison and grants immunity for [seconds].
 */
data class CurePoison(val seconds: Int) : ConsumableEffect {
    override fun apply(p: Player) {
        Poison.cure(p, Poison.cyclesForSeconds(seconds))
    }
}

/**
 * Restores [percent] of the player's run energy.
 */
data class RestoreEnergy(val percent: Int) : ConsumableEffect {
    override fun apply(p: Player) {
        p.runEnergy = Math.min(Potions.MAX_RUN_ENERGY, p.runEnergy + Potions.MAX_RUN_ENERGY * percent / 100.0)
        p.sendRunEnergy(p.runEnergy.toInt())
    }
}

/**
 * Slows run energy depletion for [ticks], stacking with any stamina effect already running.
 */
data class Stamina(val ticks: Int) : ConsumableEffect {
    override fun apply(p: Player) {
        val remaining = if (p.timers.exists(RunEnergy.STAMINA_BOOST)) p.timers[RunEnergy.STAMINA_BOOST] else 0
        p.timers[RunEnergy.STAMINA_BOOST] = Math.min(Potions.MAX_STAMINA_TICKS, Math.max(0, remaining) + ticks)
        p.message("You feel less fatigued.")
    }
}

/**
 * Protects against dragonfire for [ticks]. A [superAntifire] dose blocks dragonfire outright rather
 * than merely reducing it, and runs on its own clock so that topping up with an ordinary antifire
 * afterwards does not extend the immunity.
 */
data class Antifire(
    val ticks: Int,
    val superAntifire: Boolean,
) : ConsumableEffect {
    override fun apply(p: Player) {
        extend(p, ANTIFIRE_TIMER, ticks)
        p.attr[ANTIFIRE_POTION_CHARGES_ATTR] = remaining(p, ANTIFIRE_TIMER)

        if (superAntifire) {
            extend(p, Potions.SUPER_ANTIFIRE_TIMER, ticks)
            p.attr[DRAGONFIRE_IMMUNITY_ATTR] = true
        }

        p.message("You are now immune to dragonfire.")
    }

    private fun extend(
        p: Player,
        timer: TimerKey,
        ticks: Int,
    ) {
        p.timers[timer] = Math.min(Potions.MAX_ANTIFIRE_TICKS, remaining(p, timer) + ticks)
    }

    private fun remaining(
        p: Player,
        timer: TimerKey,
    ): Int = if (p.timers.exists(timer)) Math.max(0, p.timers[timer]) else 0
}

/**
 * Heals [amount] hitpoints, allowing the total to run [overheal] above the base level.
 *
 * Potions that heal are the barbarian mixes, Guthix rest and the raid supplies; the ones that may
 * carry hitpoints over the maximum say so with [overheal], the same way [Food][org.alter.plugins.content.items.consumables.food.Food]
 * does. Where the amount scales with the player's level - a raid Nectar, a Saradomin brew - use
 * [Boost] on [Skills.HITPOINTS] instead: its "base plus the boost" cap is exactly the overheal rule.
 */
data class Heal(
    val amount: Int,
    val overheal: Int = 0,
) : ConsumableEffect {
    override fun apply(p: Player) {
        p.heal(amount, overheal)
    }
}

/**
 * Cures disease.
 *
 * Disease is not modelled - nothing in the game infects the player - so this is a stub that reports
 * the cure and does nothing else. It exists so that Relicym's balm and Relicym's mix are drinkable
 * and carry the right effect the day disease arrives, rather than being silently inert items.
 */
object CureDisease : ConsumableEffect {
    override fun apply(p: Player) {
        p.message("You feel much better.")
    }
}

/**
 * Holds every skill [skills] selects at its base level for [Divine.DURATION_TICKS].
 *
 * A Menaphite remedy does not restore once - it tops the player's combat stats back up every
 * fifteen seconds for five minutes. Reusing the divine floor mechanic gives exactly that outcome:
 * the restore cycle can no longer take a drained stat below its base while the floor is in place.
 * Pair it with a [Restore] for the immediate half of the effect.
 */
data class HoldStats(val skills: (Int) -> Boolean) : ConsumableEffect {
    override fun apply(p: Player) {
        val stats = p.getSkills()
        for (skill in 0 until stats.maxSkills) {
            if (skills(skill)) {
                Divine.protect(p, skill, stats.getBaseLevel(skill))
            }
        }
    }
}

/**
 * Heals [amount] hitpoints when the five-minute divine timer runs out.
 *
 * Only the overloads use it: they cost 50 hitpoints up front and hand them back when the boost
 * ends. Held on the timer rather than applied here, because the point is the delay.
 */
data class HealOnExpiry(val amount: Int) : ConsumableEffect {
    override fun apply(p: Player) {
        p.attr[Divine.EXPIRY_HEAL] = amount
    }
}

/**
 * Restores `flat + percent% of the base Prayer level` prayer points a point at a time over
 * [durationTicks], rather than all at once.
 *
 * This is what a Prayer enhance does - one point every few ticks for most of five minutes - so the
 * interval is derived from the total rather than hard-coded, and a longer or weaker dose spaces its
 * points out accordingly.
 */
data class PrayerRegen(
    val durationTicks: Int,
    val flat: Int,
    val percent: Int,
) : ConsumableEffect {
    override fun apply(p: Player) {
        val base = p.getSkills().getBaseLevel(Skills.PRAYER)
        val total = flat + (base * percent) / 100
        if (total <= 0) {
            return
        }
        p.attr[Potions.PRAYER_REGEN_LEFT] = total
        val interval = Math.max(1, durationTicks / total)
        p.attr[Potions.PRAYER_REGEN_INTERVAL] = interval
        p.timers[Potions.PRAYER_REGEN_TIMER] = interval
    }
}

/**
 * Heals a percentage of the player's *maximum* hitpoints rather than a flat amount.
 *
 * Only the poison chalice works this way - every other drink heals a fixed number - so it takes the
 * base hitpoints level as the whole, the way the wiki states its outcomes.
 */
data class HealPercent(val percent: Int) : ConsumableEffect {
    override fun apply(p: Player) {
        p.heal((p.getSkills().getBaseLevel(Skills.HITPOINTS) * percent) / 100)
    }
}

/**
 * Heals a uniformly random number of hitpoints between [min] and [max] inclusive.
 *
 * A handful of foods heal a spread rather than a figure - the snail meats, the spiders on a stick,
 * frog spawn, a cooked slimy eel, purple sweets - and the wiki publishes them as a range with no
 * distribution behind it, so this treats every value in the range as equally likely.
 */
data class HealRange(
    val min: Int,
    val max: Int,
) : ConsumableEffect {
    override fun apply(p: Player) {
        p.heal(min + p.world.random(max - min))
    }
}

/**
 * Heals [amount] hitpoints [delayTicks] ticks after the food is eaten.
 *
 * This is what the Varlamore hunter meats do: a cooked dashing kebbit heals 13 at once and another
 * 10 three seconds later, for 23 in total. The second half has to be genuinely late - eating one at
 * full health and then taking a hit still gets the rest - so it is held on a timer rather than
 * folded into the immediate heal. `EatingPlugin` services the timer.
 */
data class DelayedHeal(
    val amount: Int,
    val delayTicks: Int,
) : ConsumableEffect {
    override fun apply(p: Player) {
        /*
         * A second meat eaten before the first has paid out adds to what is owed rather than
         * replacing it, and restarts the wait - which is what stops a player banking several
         * delayed heals and collecting them at once.
         */
        p.attr[Foods.DELAYED_HEAL_OWED] = (p.attr[Foods.DELAYED_HEAL_OWED] ?: 0) + amount
        p.timers[Foods.DELAYED_HEAL_TIMER] = delayTicks
    }
}

/**
 * Costs the player a percentage of their maximum hitpoints. See [HealPercent].
 *
 * Unlike [Damage] this is not gated by a minimum-hitpoints check, because the potion it belongs to
 * is meant to be able to kill the drinker.
 */
data class DamagePercent(val percent: Int) : ConsumableEffect {
    override fun apply(p: Player) {
        p.hit(Math.max(1, (p.getSkills().getBaseLevel(Skills.HITPOINTS) * percent) / 100))
    }
}

/**
 * Picks one of [outcomes] at random, reports it, and applies it.
 *
 * The poison chalice is the only drink in the game that does this - seven equally likely results
 * ranging from a thirty per cent heal to half the drinker's hitpoints - so the outcomes carry their
 * own chat message rather than the caller trying to describe what happened.
 */
data class OneOf(val outcomes: List<Outcome>) : ConsumableEffect {
    override fun apply(p: Player) {
        val outcome = outcomes[p.world.random(outcomes.size - 1)]
        p.message(outcome.message)
        outcome.effects.forEach { it.apply(p) }
    }
}

/** One result of a [OneOf], and what the player is told when it comes up. */
data class Outcome(
    val message: String,
    val effects: List<ConsumableEffect> = emptyList(),
)
