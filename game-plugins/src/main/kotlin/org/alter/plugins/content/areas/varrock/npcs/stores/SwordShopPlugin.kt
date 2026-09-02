package org.alter.plugins.content.areas.varrock.npcs.stores

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
 * The Varrock Swordshop, west of Varrock Square near Zaff's, roughly (3206, 3399).
 *
 * The shopkeeper here is cache id 2884, per the wiki's "Shop keeper (Varrock Swordshop)"
 * infobox. It previously read 2817, which is Al Kharid's general store keeper.
 */
class SwordShopPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.bronze_sword"), 5, 26, 16),
        ShopItem(getRSCM("item.iron_sword"), 4, 91, 55),
        ShopItem(getRSCM("item.steel_sword"), 4, 325, 195),
        ShopItem(getRSCM("item.black_sword"), 3, 624, 374),
        ShopItem(getRSCM("item.mithril_sword"), 3, 845, 507),
        ShopItem(getRSCM("item.adamant_sword"), 2, 2080, 1248),
        ShopItem(getRSCM("item.bronze_longsword"), 4, 40, 24),
        ShopItem(getRSCM("item.iron_longsword"), 3, 140, 84),
        ShopItem(getRSCM("item.steel_longsword"), 3, 500, 300),
        ShopItem(getRSCM("item.black_longsword"), 2, 960, 576),
        ShopItem(getRSCM("item.mithril_longsword"), 2, 1300, 780),
        ShopItem(getRSCM("item.adamant_longsword"), 1, 3200, 1920),
        ShopItem(getRSCM("item.bronze_dagger"), 10, 10, 6),
        ShopItem(getRSCM("item.iron_dagger"), 6, 35, 21),
        ShopItem(getRSCM("item.steel_dagger"), 5, 125, 75),
        ShopItem(getRSCM("item.black_dagger"), 4, 240, 144),
        ShopItem(getRSCM("item.mithril_dagger"), 3, 325, 195),
        ShopItem(getRSCM("item.adamant_dagger"), 2, 800, 480),
    )

    init {
        spawnNpc(npc = "npc.shop_keeper_2884", x = 3206, z = 3399, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop("Varrock Swordshop", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.shop_keeper_2884", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.shop_keeper_2884", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Varrock Swordshop")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to the Varrock Swordshop.")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
