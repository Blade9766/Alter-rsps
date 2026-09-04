package org.alter.plugins.content.items.consumables.potions

import dev.openrune.cache.CacheManager.getItem
import org.alter.api.EquipmentType
import org.alter.api.ext.*
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.plugins.content.areas.duelarena.DuelRules
import org.alter.game.model.timer.ATTACK_DELAY
import org.alter.game.model.timer.FOOD_DELAY
import org.alter.game.model.timer.POTION_DELAY
import org.alter.game.model.timer.TimerKey
import org.alter.rscm.RSCM.getRSCM

/**
 * Drinking a dose of a potion: the shared timing, the animation, and the walk down the dose chain
 * to the next-lowest dose or the empty vial.
 */
object Potions {
    /**
     * Runs alongside [ANTIFIRE_TIMER][org.alter.game.model.timer.ANTIFIRE_TIMER] so that full
     * dragonfire immunity ends with the super antifire that granted it, not with any ordinary
     * antifire drunk afterwards.
     */
    val SUPER_ANTIFIRE_TIMER = TimerKey()

    /**
     * Ticks down to the next single prayer point a [PrayerRegen][org.alter.plugins.content.items.consumables.PrayerRegen]
     * dose owes the player, and is re-armed by `PotionPlugin` until [PRAYER_REGEN_LEFT] runs out.
     */
    val PRAYER_REGEN_TIMER = TimerKey()

    /** Prayer points a Prayer enhance still has to hand back, one at a time. */
    val PRAYER_REGEN_LEFT = AttributeKey<Int>()

    /** Ticks between those points, chosen so the whole dose lands inside its stated duration. */
    val PRAYER_REGEN_INTERVAL = AttributeKey<Int>()

    private const val DRINK_ANIM = 829
    private const val DRINK_SOUND = 2401

    /**
     * Drinking blocks the next potion, and any food, for this many ticks, and costs the player an
     * attack.
     */
    private const val DRINK_DELAY = 3

    const val MAX_RUN_ENERGY = 10000.0

    /**
     * An hour of stamina, the point at which further doses stop adding time.
     */
    const val MAX_STAMINA_TICKS = 6000

    /**
     * Twenty minutes of dragonfire protection, the cap on stacking antifire doses.
     */
    const val MAX_ANTIFIRE_TICKS = 2000

    private val PRAYER_RESTORE_ITEMS = arrayOf("item.holy_wrench", "item.ring_of_the_gods_i")
    private val PRAYER_RESTORE_CAPES = arrayOf("item.prayer_cape", "item.prayer_capet", "item.max_cape")

    fun canDrink(p: Player): Boolean = !p.timers.has(POTION_DELAY) && DuelRules.canDrink(p)

    /**
     * Whether the player is carrying something that improves how much a prayer potion restores.
     */
    fun hasPrayerRestoreBonus(p: Player): Boolean =
        p.inventory.contains("item.holy_wrench") ||
            p.hasEquipped(EquipmentType.RING, *PRAYER_RESTORE_ITEMS) ||
            p.hasEquipped(EquipmentType.CAPE, *PRAYER_RESTORE_CAPES)

    /**
     * The item a potion leaves behind once [dose] has been drunk: the next dose down, or the empty
     * vial when the last one goes.
     */
    fun remainder(
        potion: Potion,
        dose: Int,
    ): String = if (dose > 1) potion.doses[dose - 2] else potion.emptied

    /**
     * The potion's name without the "(1)" dose suffix the item names carry.
     */
    private fun displayName(potion: Potion): String =
        getItem(getRSCM(potion.doses[0])).name.substringBefore('(').trim().lowercase()

    fun drink(
        p: Player,
        potion: Potion,
        dose: Int,
    ) {
        p.animate(DRINK_ANIM)
        p.playSound(DRINK_SOUND)

        potion.effects.forEach { it.apply(p) }

        /*
         * Set after the effects, because the effects are what start the timer this message belongs
         * to - and only while it is actually running, so a potion with no held effect never leaves
         * a stale message behind for the next one.
         */
        if (p.timers.has(Divine.TIMER)) {
            p.attr[Divine.EXPIRY_MESSAGE] = potion.expiryMessage
        }

        p.resetFacePawn()

        p.timers[POTION_DELAY] = DRINK_DELAY
        p.timers[FOOD_DELAY] = DRINK_DELAY
        /*
         * Never shorten a longer swing that is already pending - otherwise a potion would reset a
         * slow weapon's cooldown and buy a free attack.
         */
        val pendingAttack = if (p.timers.exists(ATTACK_DELAY)) p.timers[ATTACK_DELAY] else 0
        p.timers[ATTACK_DELAY] = Math.max(pendingAttack, DRINK_DELAY)

        p.message("You drink some of your ${displayName(potion)}.")

        val remaining = dose - 1
        if (remaining > 0) {
            p.message("You have $remaining ${if (remaining == 1) "dose" else "doses"} of potion left.")
        } else {
            p.message("You have finished your potion.")
        }
    }
}
