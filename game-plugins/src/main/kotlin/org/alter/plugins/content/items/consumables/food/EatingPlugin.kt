package org.alter.plugins.content.items.consumables.food

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
import org.alter.plugins.content.items.consumables.food.Foods

class EatingPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        Food.values.forEach { food ->
            onItemOption(item = food.item, option = food.option) {
                /*
                 * A few items carry a Drink option the live game will not honour - the Kelda stout
                 * that has to reach the drunken dwarf, the ingredients, the joke items. Refused
                 * before the delay check, because the refusal is not a consumption and should not
                 * cost the player a tick.
                 */
                food.refusal?.let {
                    player.message(it)
                    return@onItemOption
                }

                if (!Foods.canEat(player, food)) {
                    return@onItemOption
                }

                val inventorySlot = player.getInteractingSlot()
                if (player.inventory.remove(item = food.item, beginSlot = inventorySlot).hasSucceeded()) {
                    /*
                     * Put whatever is left - the next portion, or the dish it came in - back in the
                     * slot the food was eaten from, before anything else can claim it.
                     */
                    food.replacement?.let { player.inventory.add(item = it, beginSlot = inventorySlot) }
                    Foods.eat(player, food)
                }
            }
        }

        /*
         * The second half of a Varlamore hunter meat, three seconds after the bite. Paid out even
         * if the player is now at full health, exactly as the immediate half would be - it simply
         * heals nothing in that case.
         */
        onTimer(Foods.DELAYED_HEAL_TIMER) {
            val owed = player.attr[Foods.DELAYED_HEAL_OWED] ?: 0
            player.attr.remove(Foods.DELAYED_HEAL_OWED)
            if (owed > 0) {
                player.heal(owed)
            }
        }
    }
}
