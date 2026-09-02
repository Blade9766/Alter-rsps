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
 * Herquin's Gems, the jeweller in south-west Falador. Herquin stands at (2945, 3334).
 *
 * `{{StoreTableHead|sellmultiplier=1000|buymultiplier=700|delta=30}}` - sell at 100%, buy
 * back at 70% of cache value (the most generous buy-back of the five Falador shops),
 * computed from this cache's own `ItemType.cost`.
 *
 * Six of the eight lines are `stock=0` on the wiki: Herquin only keeps one uncut sapphire
 * and one cut sapphire on the shelf, and everything above sapphire has to be sold to him
 * before he has any. Kept in the stock list so BUY_STOCK still accepts them.
 */
class HerquinPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val storeItems = listOf(
        ShopItem(getRSCM("item.uncut_sapphire"), 1, 25, 17),
        ShopItem(getRSCM("item.uncut_emerald"), 0, 50, 35),
        ShopItem(getRSCM("item.uncut_ruby"), 0, 100, 70),
        ShopItem(getRSCM("item.uncut_diamond"), 0, 200, 140),
        ShopItem(getRSCM("item.sapphire"), 1, 250, 175),
        ShopItem(getRSCM("item.emerald"), 0, 500, 350),
        ShopItem(getRSCM("item.ruby"), 0, 1000, 700),
        ShopItem(getRSCM("item.diamond"), 0, 2000, 1400),
    )

    init {
        spawnNpc(npc = "npc.herquin", x = 2945, z = 3334, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop(SHOP_NAME, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.herquin", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.herquin", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop(SHOP_NAME)

    suspend fun QueueTask.dialog(player: Player) {
        when (options(player, "Do you wish to trade?", "Sorry I don't want to talk to you actually.")) {
            1 -> {
                chatPlayer(player, "Do you wish to trade?")
                chatNpc(player, "Why yes, this a jewel shop after all.")
                player.shop()
            }
            2 -> {
                chatPlayer(player, "Sorry I don't want to talk to you actually.")
                chatNpc(player, "Huh! Charming!")
            }
        }
    }

    private companion object {
        const val SHOP_NAME = "Herquin's Gems"
    }
}
