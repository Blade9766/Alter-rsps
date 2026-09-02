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
 * Cassie's Shield Shop, north-east Falador - Cassie stands at (2976, 3384).
 *
 * `{{StoreTableHead|sellmultiplier=1000|buymultiplier=600|delta=20}}`, so sell at 100%
 * and buy back at 60% of each item's cache value; prices computed from this cache's own
 * `ItemType.cost` rather than a wiki paraphrase.
 *
 * The four zero-stock lines are real: the wiki lists iron kiteshield, steel sq shield,
 * steel kiteshield and mithril sq shield with `stock=0`, meaning Cassie only ever has them
 * if a player sells her one. They are kept in the stock list (rather than dropped) so the
 * BUY_STOCK policy still lets players sell those four to her, which is how the real shop
 * behaves.
 */
class CassiePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val storeItems = listOf(
        ShopItem(getRSCM("item.wooden_shield"), 5, 20, 12),
        ShopItem(getRSCM("item.bronze_sq_shield"), 3, 48, 28),
        ShopItem(getRSCM("item.bronze_kiteshield"), 3, 68, 40),
        ShopItem(getRSCM("item.iron_sq_shield"), 2, 168, 100),
        ShopItem(getRSCM("item.iron_kiteshield"), 0, 238, 142),
        ShopItem(getRSCM("item.steel_sq_shield"), 0, 600, 360),
        ShopItem(getRSCM("item.steel_kiteshield"), 0, 850, 510),
        ShopItem(getRSCM("item.mithril_sq_shield"), 0, 1560, 936),
    )

    init {
        spawnNpc(npc = "npc.cassie", x = 2976, z = 3384, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop(SHOP_NAME, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.cassie", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.cassie", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop(SHOP_NAME)

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "I buy and sell shields, do you want to trade?")

        when (options(player, "Yes please.", "No thank you.")) {
            1 -> {
                chatPlayer(player, "Yes please.")
                player.shop()
            }
            2 -> chatPlayer(player, "No thank you.")
        }
    }

    private companion object {
        const val SHOP_NAME = "Cassie's Shield Shop"
    }
}
