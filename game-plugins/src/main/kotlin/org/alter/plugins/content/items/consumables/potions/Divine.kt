package org.alter.plugins.content.items.consumables.potions

import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey

/**
 * The floor a divine potion holds a boosted stat at, and how long it holds it there.
 *
 * An ordinary boost decays a level a minute in
 * [org.alter.plugins.content.mechanics.restoration.Restoration.restoreStats]. A divine dose pays ten
 * hitpoints to stop that: for five minutes the stat is topped straight back up whenever the restore
 * cycle would have taken a level off it, so a divine super strength holds `base + 5 + 15%` flat for
 * the whole duration instead of sliding down from it.
 *
 * The floors are held per skill rather than per potion because the divine potions overlap - a divine
 * super combat protects three stats at once, and drinking a divine super strength on top of it must
 * raise the Strength floor without disturbing the other two. The whole map is session state and is
 * deliberately not persisted: the protection is five minutes long, and a logout should end it rather
 * than bank it.
 */
object Divine {
    /** Ends the protection on every skill at once - see [PotionPlugin]'s handler. */
    val TIMER = TimerKey()

    /** Skill id to the level that skill may not fall below while [TIMER] is running. */
    val FLOORS = AttributeKey<HashMap<Int, Int>>()

    /**
     * Hitpoints handed back when [TIMER] runs out, for the overloads - they charge 50 hitpoints for
     * the boost and refund them when it ends. Absent for everything else.
     */
    val EXPIRY_HEAL = AttributeKey<Int>()

    /**
     * What to tell the player when [TIMER] runs out. Set from
     * [Potion.expiryMessage][org.alter.plugins.content.items.consumables.potions.Potion.expiryMessage]
     * on every dose, so the last potion drunk names itself rather than every held effect claiming to
     * be a divine one.
     */
    val EXPIRY_MESSAGE = AttributeKey<String>()

    /** Five minutes, in 600ms game cycles. */
    const val DURATION_TICKS = 500

    /**
     * The hitpoints every divine dose costs, and so the level below which one cannot be drunk -
     * the potion damages for ten and the game will not let a dose kill, so eleven is the floor.
     */
    const val HITPOINT_COST = 10
    const val MIN_HITPOINTS = HITPOINT_COST + 1

    /**
     * Protects [skill] at [level] and restarts the five minutes.
     *
     * A second dose refreshes the clock for *every* protected skill, which is what the live game
     * does - the potions share one duration rather than running their own timers.
     */
    fun protect(
        p: Player,
        skill: Int,
        level: Int,
    ) {
        val floors = p.attr[FLOORS] ?: HashMap()
        floors[skill] = maxOf(floors[skill] ?: 0, level)
        p.attr[FLOORS] = floors
        p.timers[TIMER] = DURATION_TICKS
    }

    /** The level [skill] is held at, or 0 when it is not protected. */
    fun floorOf(
        p: Player,
        skill: Int,
    ): Int {
        if (!p.timers.has(TIMER)) {
            return 0
        }
        return p.attr[FLOORS]?.get(skill) ?: 0
    }

    fun clear(p: Player) {
        p.attr.remove(FLOORS)
        p.attr.remove(EXPIRY_HEAL)
        p.attr.remove(EXPIRY_MESSAGE)
    }
}
