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
 * "Wayne's Chains! - Chainmail specialist", by Falador's south gate. Wayne stands at
 * (2972, 3313), which is well south of the main city walls - that looks wrong at a glance
 * but is genuinely where the shop sits, confirmed by both his own map pin and the shop
 * page's `{{Map|2972,3312|mtype=rectangle|rectX=7|rectY=5}}`.
 *
 * `{{StoreTableHead|sellmultiplier=1000|buymultiplier=650|delta=10}}` - sell at 100% and
 * buy back at 65% of cache value, the only 65% shop in this slice. Prices computed from
 * this cache's own `ItemType.cost`.
 */
class WaynePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val storeItems = listOf(
        ShopItem(getRSCM("item.bronze_chainbody"), 3, 60, 39),
        ShopItem(getRSCM("item.iron_chainbody"), 2, 210, 136),
        ShopItem(getRSCM("item.steel_chainbody"), 1, 750, 487),
        ShopItem(getRSCM("item.black_chainbody"), 1, 1440, 936),
        ShopItem(getRSCM("item.mithril_chainbody"), 1, 1950, 1267),
        ShopItem(getRSCM("item.adamant_chainbody"), 1, 4800, 3120),
    )

    init {
        spawnNpc(npc = "npc.wayne", x = 2972, z = 3313, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop(SHOP_NAME, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.wayne", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.wayne", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop(SHOP_NAME)

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to Wayne's Chains. Do you wanna buy or sell<br>some chain mail?")

        when (options(player, "Yes please.", "No thanks.")) {
            1 -> {
                chatPlayer(player, "Yes please.")
                player.shop()
            }
            2 -> chatPlayer(player, "No thanks.")
        }
    }

    private companion object {
        const val SHOP_NAME = "Wayne's Chains"
    }
}
