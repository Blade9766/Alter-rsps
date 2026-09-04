package org.alter.plugins.content.items.consumables.food

import dev.openrune.cache.CacheManager.getItem
import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.heal
import org.alter.api.ext.message
import org.alter.api.ext.playSound
import org.alter.game.model.entity.Player
import org.alter.plugins.content.areas.duelarena.DuelRules
import org.alter.rscm.RSCM.getRSCM
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.timer.ATTACK_DELAY
import org.alter.game.model.timer.COMBO_FOOD_DELAY
import org.alter.game.model.timer.FOOD_DELAY
import org.alter.game.model.timer.POTION_DELAY
import org.alter.game.model.timer.TimerKey

/**
 * @author Tom <rspsmods@gmail.com>
 */
object Foods {
    /**
     * Ticks down to the second half of a
     * [DelayedHeal][org.alter.plugins.content.items.consumables.DelayedHeal] - the Varlamore hunter
     * meats, which heal again three seconds after they are eaten. Serviced by `EatingPlugin`.
     */
    val DELAYED_HEAL_TIMER = TimerKey()

    /** Hitpoints those meats still owe the player. */
    val DELAYED_HEAL_OWED = AttributeKey<Int>()

    private const val EAT_FOOD_ANIM = 829
    private const val EAT_FOOD_ON_SLED_ANIM = 1469
    private const val EAT_FOOD_SOUND = 2393

    /**
     * Half this list is drunk rather than eaten - the ales, the teas, the cocktails - and a swallow
     * does not sound like a bite. The animation is the same 829 either way.
     */
    private const val DRINK_SOUND = 2401

    /**
     * Eating blocks the next piece of food, and any potion, for this many ticks, and costs the
     * player an attack.
     */
    private const val EAT_DELAY = 3

    fun canEat(
        p: Player,
        food: Food,
    ): Boolean =
        !p.timers.has(if (food.comboFood) COMBO_FOOD_DELAY else FOOD_DELAY) &&
            DuelRules.canEat(p)

    fun eat(
        p: Player,
        food: Food,
    ) {
        val delay = if (food.comboFood) COMBO_FOOD_DELAY else FOOD_DELAY
        val anim = if (p.hasEquipped(EquipmentType.WEAPON, "item.sled")) EAT_FOOD_ON_SLED_ANIM else EAT_FOOD_ANIM

        val heal =
            when (food) {
                /*
                 * The blighted copy is the same fish with a PvP-world-only id, so it scales the
                 * same way rather than healing the flat nothing an unlisted food would.
                 */
                Food.ANGLERFISH, Food.BLIGHTED_ANGLERFISH -> {
                    val c =
                        when (p.getSkills().getBaseLevel(Skills.HITPOINTS)) {
                            in 25..49 -> 4
                            in 50..74 -> 6
                            in 75..92 -> 8
                            in 93..99 -> 13
                            else -> 2
                        }
                    Math.floor(p.getSkills().getBaseLevel(Skills.HITPOINTS) / 10.0).toInt() + c
                }
                else -> food.heal
            }

        val oldHp = p.getSkills().getCurrentLevel(Skills.HITPOINTS)
        val foodName = getItem(getRSCM(food.item)).name

        p.animate(anim)
        p.playSound(if (food.option == "drink") DRINK_SOUND else EAT_FOOD_SOUND)
        if (heal > 0) {
            p.heal(heal, if (food.overheal) heal else 0)
        }

        food.effects.forEach { it.apply(p) }

        p.resetFacePawn()

        p.timers[delay] = food.tickDelay
        /*
         * Eating blocks potions too. A Karambwan is the exception on the food side only - it has its
         * own delay so it can follow another piece of food - but it still blocks a potion.
         */
        p.timers[POTION_DELAY] = EAT_DELAY
        /*
         * Never shorten a longer swing that is already pending, or eating would reset a slow
         * weapon's cooldown and buy a free attack.
         */
        val pendingAttack = if (p.timers.exists(ATTACK_DELAY)) p.timers[ATTACK_DELAY] else 0
        p.timers[ATTACK_DELAY] = Math.max(pendingAttack, EAT_DELAY)

        p.message("You ${food.option} the ${foodName.lowercase()}.")
        if (p.getSkills().getCurrentLevel(Skills.HITPOINTS) > oldHp) {
            p.message("It heals some health.")
        }
    }
}
