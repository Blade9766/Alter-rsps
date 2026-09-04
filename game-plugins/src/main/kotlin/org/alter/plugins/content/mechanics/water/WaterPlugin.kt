package org.alter.plugins.content.mechanics.water

import dev.openrune.cache.CacheManager
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
 * **The `Fill-bucket` and `Fill-from` actions are bound too.** Four sources carry one in the
 * cache - 9143 `Sink`, 41004 `Water pump`, and 35981/36078 `Water Pump` - and left unbound
 * they were a left-click that visibly did nothing. `Fill-bucket` names its container, so it
 * fills a bucket and nothing else; `Fill-from` does not, so it fills the first container the
 * player is carrying in [WaterContainers] declaration order, which puts the bowl and bucket
 * ahead of the vial and waterskin. Every other source has no action at all, and is still
 * reached the general OSRS way, by using a container on it.
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
                        it.container.fill(player, source.messageFor(it.container, world.definitions))
                    }
                }

                /*
                 * The handful of sources that also carry a left-click fill action. Bound by
                 * the action name found on the object rather than by id, so a cache update
                 * that adds one to another source picks it up.
                 */
                CacheManager.getObject(obj).actions.filterNotNull().forEach { action ->
                    when {
                        action.equals(OPT_FILL_BUCKET, ignoreCase = true) ->
                            onObjOption(obj = obj, option = action) {
                                val bucket = WaterContainers.BUCKET.container
                                if (!player.inventory.contains(bucket.unfilled)) {
                                    player.message(NEED_CONTAINER)
                                } else {
                                    bucket.fill(player, source.messageFor(bucket, world.definitions))
                                }
                            }

                        action.equals(OPT_FILL_FROM, ignoreCase = true) ->
                            onObjOption(obj = obj, option = action) {
                                val held =
                                    WaterContainers.values()
                                        .map { it.container }
                                        .firstOrNull { player.inventory.contains(it.unfilled) }
                                if (held == null) {
                                    player.message(NEED_CONTAINER)
                                } else {
                                    held.fill(player, source.messageFor(held, world.definitions))
                                }
                            }
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
        const val OPT_FILL_BUCKET = "Fill-bucket"
        const val OPT_FILL_FROM = "Fill-from"

        /** Nothing in the inventory to fill, on the left-click routes that pick for you. */
        const val NEED_CONTAINER = "You have nothing to fill with water."
    }
}
