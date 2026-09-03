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
import org.alter.game.model.timer.ATTACK_DELAY
import org.alter.game.model.timer.COMBO_FOOD_DELAY
import org.alter.game.model.timer.FOOD_DELAY
import org.alter.game.model.timer.POTION_DELAY

/**
 * @author Tom <rspsmods@gmail.com>
 */
object Foods {
    private const val EAT_FOOD_ANIM = 829
    private const val EAT_FOOD_ON_SLED_ANIM = 1469
    private const val EAT_FOOD_SOUND = 2393

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
                Food.ANGLERFISH -> {
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
        p.playSound(EAT_FOOD_SOUND)
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
