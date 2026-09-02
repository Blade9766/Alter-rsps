package org.alter.plugins.content.areas.ardougne.npcs.stores

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.shop.PurchasePolicy
import org.alter.game.model.shop.ShopItem
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.mechanics.shops.CoinCurrency
import org.alter.rscm.RSCM.getRSCM

/**
 * Aemad's Adventuring Supplies, East Ardougne's general store, at (2614, 3293) - run by
 * Aemad and his assistant Kortan, who share the same map pin.
 *
 * `{{StoreTableHead|sellmultiplier=1300|buymultiplier=400|delta=20}}`, the usual general
 * store schedule, so prices are `floor(cost * multiplier / 1000)` from this cache's own
 * `ItemType.cost`.
 *
 * Its stock is nothing like the Varrock/Falador/Edgeville general stores - no pots, jugs or
 * buckets at all. It is an *adventuring* supplier: vials, a pickaxe and axe, rope, papyrus,
 * arrows and wool. That is real, not an oversight.
 *
 * Kortan is offset one tile east so the two do not spawn stacked, the same treatment
 * Edgeville's shop staff needed.
 */
class AemadPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val shopkeepers = listOf("npc.aemad", "npc.kortan")

    private val storeItems = listOf(
        ShopItem(getRSCM("item.vial_of_water"), 500, 2, 0),
        ShopItem(getRSCM("item.waterfilled_vial_pack"), 250, 261, 80),
        ShopItem(getRSCM("item.bronze_pickaxe"), 2, 1, 0),
        ShopItem(getRSCM("item.iron_axe"), 2, 72, 22),
        ShopItem(getRSCM("item.cooked_meat"), 2, 5, 1),
        ShopItem(getRSCM("item.tinderbox"), 2, 1, 0),
        ShopItem(getRSCM("item.ball_of_wool"), 30, 2, 0),
        ShopItem(getRSCM("item.bronze_arrow"), 500, 1, 0),
        ShopItem(getRSCM("item.rope"), 20, 23, 7),
        ShopItem(getRSCM("item.papyrus"), 50, 13, 4),
        ShopItem(getRSCM("item.knife"), 2, 7, 2),
    )

    init {
        spawnNpc(npc = "npc.aemad", x = 2614, z = 3293, height = 0, walkRadius = 2, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.kortan", x = 2615, z = 3293, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop(SHOP_NAME, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_TRADEABLES) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        shopkeepers.forEach {
            onNpcOption(it, option = "talk-to") { player.queue { dialog(player) } }
            onNpcOption(it, option = "trade") { player.shop() }
        }
    }

    fun Player.shop() = this.openShop(SHOP_NAME)

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Hello there. You've come to the right place if you're<br>looking for adventurer's equipment.")

        when (options(player, "Oh that sounds interesting.", "No, sorry, I've come to the wrong place.")) {
            1 -> {
                chatPlayer(player, "Oh that sounds interesting.")
                player.shop()
            }
            2 -> {
                chatPlayer(player, "No, sorry, I've come to the wrong place.")
                chatNpc(player, "Hmph. Well, perhaps next time you'll need something<br>from me?")
            }
        }
    }

    private companion object {
        const val SHOP_NAME = "Aemad's Adventuring Supplies"
    }
}
