package org.alter.plugins.content.areas.falador.npcs.stores

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
 * Flynn's Mace Market, immediately west of the general store. Flynn stands at (2950, 3387).
 *
 * `{{StoreTableHead|sellmultiplier=1000|buymultiplier=600|delta=10}}` - sell at 100%, buy
 * back at 60% of cache value, computed from this cache's own `ItemType.cost`.
 */
class FlynnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val storeItems = listOf(
        ShopItem(getRSCM("item.bronze_mace"), 5, 18, 10),
        ShopItem(getRSCM("item.iron_mace"), 4, 63, 37),
        ShopItem(getRSCM("item.steel_mace"), 4, 225, 135),
        ShopItem(getRSCM("item.mithril_mace"), 3, 585, 351),
        ShopItem(getRSCM("item.adamant_mace"), 2, 1440, 864),
    )

    init {
        spawnNpc(npc = "npc.flynn", x = 2950, z = 3387, height = 0, walkRadius = 2, direction = Direction.EAST)

        createShop(SHOP_NAME, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.flynn", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.flynn", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop(SHOP_NAME)

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Hello. Do you want to buy or sell any maces?")

        when (options(player, "Well, I'll have a look, at least.", "No thanks.")) {
            1 -> {
                chatPlayer(player, "Well, I'll have a look, at least.")
                player.shop()
            }
            2 -> chatPlayer(player, "No thanks.")
        }
    }

    private companion object {
        const val SHOP_NAME = "Flynn's Mace Market"
    }
}
