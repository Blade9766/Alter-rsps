package org.alter.plugins.content.mechanics.water

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.DynamicObject
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * Filling and emptying water containers: buckets, bowls, jugs, vials, cups, watering cans
 * and waterskins, at every fountain, sink, well, water barrel, water pump and tap in the
 * game.
 *
 * This existed once and was commented out wholesale when RSCM replaced the `Items` and
 * `Objs` constants classes it was written against; what follows is that logic restored,
 * with the ~100 hardcoded object ids replaced by a cache scan (see [WaterSources]) and the
 * item ids by RSCM keys (see [WaterContainers]).
 *
 * It matters more than a water bucket sounds like it should. Nothing else on this server
 * fills a container, so `skills/cooking/CookingRecipePlugin`'s whole dough-and-pie chain -
 * and Herblore's vial of water - had no reachable starting point: general stores sell the
 * empty containers and nothing filled them. That gap is documented on `CookingRecipePlugin`
 * as upstream of it; this is the upstream.
 *
 * **Bindings are made in [onWorldInit]** rather than in `init`, matching Cooking and
 * Smelting. Both the cache and RSCM are in fact ready by the time a plugin is constructed
 * (`Server` calls `RSCM.init()` before `plugins.init()`), so `init` would work too, but
 * keeping every cache-scanned binding on the same hook keeps them comparable.
 *
 * **Not implemented, and worth knowing about:** the `Fill-bucket` and `Fill-from` actions
 * that a handful of sinks and pumps carry in the cache (objects 9143, 35981, 36078, 41004)
 * are left unbound. Filling is reached by using a container on the source, which is the
 * general OSRS mechanic and the one every source supports; wiring those actions as well
 * would mean guessing which container in the inventory they should pick, and they are four
 * objects out of ~130.
 */
class WaterPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onWorldInit {
            /**
             * Handle the filling of each [WaterContainers]' item at each of the [WaterSources].
             */
            WaterSources.scan().forEach { (obj, source) ->
                WaterContainers.values().forEach {
                    onItemOnObj(obj = obj, item = it.container.unfilled) {
                        val message =
                            if (it.container.unfilled.getItemName().contains("Cup")) {
                                "You fill the cup."
                            } else {
                                source.message.replaceItemName(it.container.unfilled, world.definitions)
                            }
                        it.container.fill(player, message)
                    }
                }
            }

            /**
             * Handle the emptying of each [WaterContainers]' item except for the watering can
             * and waterskin, as they DO NOT have an "Empty" option, only "Drop" or "Use".
             */
            WaterContainers.values().filter { it != WaterContainers.CAN && it != WaterContainers.WATERSKIN }.forEach {
                onItemOption(item = it.container.filledKey, option = OPT_EMPTY) {
                    it.container.empty(player)
                }
            }

            val toySink = getRSCM("item.sink")
            WaterContainers.values().forEach {
                /**
                 * Using a [WaterContainer] on another one does nothing; you cannot transfer
                 * water around like pots, you must fill them.
                 */
                onItemOnItem(it.container.filled, it.container.filled) {
                    player.nothingMessage()
                }
                onItemOnItem(it.container.unfilled, it.container.filled) {
                    player.nothingMessage()
                }

                /**
                 * Toy sink item!
                 */
                onItemOnItem(toySink, it.container.unfilled) {
                    it.container.fill(
                        player,
                        "The cute sink fills the ${it.container.unfilled.getItemName(lowercase = true)} to the brim.",
                    )
                }
                onItemOnItem(toySink, it.container.filled) {
                    player.message("The ${it.container.unfilled.getItemName(lowercase = true)} cannot hold any more water.")
                }
            }

            /**
             * hot water is apparently only created in bowls lol, registering bowls to heat
             * here would require knowing all the fire sources so we'll ignore lack of ability
             * to make for now; this is mostly used for testing Guthix rest teas without heat plugins.
             */
            onItemOnItem("item.bowl_of_hot_water", "item.empty_cup") {
                if (player.comboItemReplace(
                        oldItem = getRSCM("item.empty_cup"),
                        newItem = getRSCM("item.cup_of_hot_water"),
                        otherOld = getRSCM("item.bowl_of_hot_water"),
                        otherNew = getRSCM("item.bowl"),
                        slotAware = true,
                    )
                ) {
                    player.message("You pour the hot water into the tea cup.")
                }
            }
        }

        /**
         * sexy little drop hack creates a toy sink object one tile north
         * of player and queues it for removal after 300 cycles (~3minutes)
         * also prevents from dropping item from inventory, but could be done here
         */
        canDropItem("item.sink") {
            val obj = DynamicObject(getRSCM("object.toy_sink"), 10, 3, player.tile.transform(0, 1))
            world.spawn(obj)
            player.world.queue {
                wait(300)
                world.remove(obj)
            }
            false
        }
    }

    private companion object {
        const val OPT_EMPTY = "Empty"
    }
}
