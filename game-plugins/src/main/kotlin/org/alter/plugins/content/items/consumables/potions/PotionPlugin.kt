package org.alter.plugins.content.items.consumables.potions

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

class PotionPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        Potion.values.forEach { potion ->
            potion.doses.forEachIndexed { index, item ->
                val dose = index + 1
                onItemOption(item = item, option = "drink") {
                    if (!Potions.canDrink(player)) {
                        return@onItemOption
                    }

                    /*
                     * A divine dose costs ten hitpoints and an overload fifty, and the game refuses
                     * one that would kill rather than letting it. Checked before the item is
                     * removed, so a refused dose is still in the inventory afterwards.
                     */
                    if (potion.minHitpoints > 0 &&
                        player.getSkills().getCurrentLevel(Skills.HITPOINTS) < potion.minHitpoints
                    ) {
                        player.message("You need at least ${potion.minHitpoints} hitpoints to drink this potion.")
                        return@onItemOption
                    }

                    val slot = player.getInteractingSlot()
                    if (player.inventory.remove(item = item, beginSlot = slot).hasSucceeded()) {
                        player.inventory.add(item = Potions.remainder(potion, dose), beginSlot = slot)
                        Potions.drink(player, potion, dose)
                    }
                }
            }
        }

        /*
         * Dragonfire protection is held on a timer, which does not survive a logout, but the charge
         * attribute it drives is persisted - so clear the pair on the way in rather than leave a
         * player permanently fireproof.
         */
        onLogin {
            player.attr.remove(ANTIFIRE_POTION_CHARGES_ATTR)
            player.attr.remove(DRAGONFIRE_IMMUNITY_ATTR)
            clearPrayerRegen(player)
        }

        onTimer(ANTIFIRE_TIMER) {
            player.attr.remove(ANTIFIRE_POTION_CHARGES_ATTR)
            player.message("Your protection against dragonfire has run out.")
        }

        onTimer(Potions.SUPER_ANTIFIRE_TIMER) {
            player.attr.remove(DRAGONFIRE_IMMUNITY_ATTR)
        }

        /*
         * The five minutes are up: the stats a divine potion, an overload or a Menaphite remedy was
         * holding flat go back to decaying a level a minute like any other boost.
         *
         * The message and the refund are read before [Divine.clear] wipes them, and an overload's
         * fifty hitpoints come back here rather than on the way down - the delay is the point.
         */
        onTimer(Divine.TIMER) {
            val message = player.attr[Divine.EXPIRY_MESSAGE] ?: "Your divine potion has expired."
            val refund = player.attr[Divine.EXPIRY_HEAL] ?: 0
            Divine.clear(player)
            if (refund > 0) {
                player.heal(refund)
            }
            player.message(message)
        }

        /*
         * A Prayer enhance hands its points back one at a time. The timer re-arms itself until the
         * dose is spent, and stops early if the player's prayer is already full - there is nothing
         * to give back and the potion is not a boost.
         */
        onTimer(Potions.PRAYER_REGEN_TIMER) {
            val left = player.attr[Potions.PRAYER_REGEN_LEFT] ?: 0
            if (left <= 0) {
                clearPrayerRegen(player)
                return@onTimer
            }

            val skills = player.getSkills()
            val base = skills.getBaseLevel(Skills.PRAYER)
            if (skills.getCurrentLevel(Skills.PRAYER) < base) {
                skills.setCurrentLevel(Skills.PRAYER, skills.getCurrentLevel(Skills.PRAYER) + 1)
            }

            val remaining = left - 1
            player.attr[Potions.PRAYER_REGEN_LEFT] = remaining
            if (remaining > 0) {
                player.timers[Potions.PRAYER_REGEN_TIMER] = player.attr[Potions.PRAYER_REGEN_INTERVAL] ?: 1
            } else {
                clearPrayerRegen(player)
            }
        }
    }

    private fun clearPrayerRegen(player: Player) {
        player.attr.remove(Potions.PRAYER_REGEN_LEFT)
        player.attr.remove(Potions.PRAYER_REGEN_INTERVAL)
    }
}
