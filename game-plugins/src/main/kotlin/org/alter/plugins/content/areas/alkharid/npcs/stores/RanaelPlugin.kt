package org.alter.plugins.content.areas.alkharid.npcs.stores

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
 * Ranael's Super Skirt Store, roughly (3316, 3163) - identical merchandise to Louie's,
 * just plateskirts instead of platelegs.
 */
class RanaelPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.bronze_plateskirt"), 5, 80, 52),
        ShopItem(getRSCM("item.iron_plateskirt"), 3, 280, 182),
        ShopItem(getRSCM("item.steel_plateskirt"), 2, 1000, 650),
        ShopItem(getRSCM("item.black_plateskirt"), 1, 1920, 1248),
        ShopItem(getRSCM("item.mithril_plateskirt"), 1, 2600, 1690),
        ShopItem(getRSCM("item.adamant_plateskirt"), 1, 6400, 4160),
    )

    init {
        spawnNpc(npc = "npc.ranael", x = 3316, z = 3163, height = 0, walkRadius = 2, direction = Direction.NORTH)

        createShop("Ranael's Super Skirt Store", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.ranael", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.ranael", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Ranael's Super Skirt Store")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to my skirt store.")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
