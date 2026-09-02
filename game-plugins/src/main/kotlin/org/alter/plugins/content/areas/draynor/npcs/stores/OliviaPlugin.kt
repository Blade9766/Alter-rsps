package org.alter.plugins.content.areas.draynor.npcs.stores

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
 * The Draynor Seed Market, roughly (3078, 3251) in the Draynor Village marketplace.
 */
class OliviaPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.potato_seed"), 20, 2, 1),
        ShopItem(getRSCM("item.onion_seed"), 10, 3, 1),
        ShopItem(getRSCM("item.cabbage_seed"), 5, 3, 1),
        ShopItem(getRSCM("item.tomato_seed"), 0, 4, 2),
        ShopItem(getRSCM("item.sweetcorn_seed"), 0, 9, 4),
        ShopItem(getRSCM("item.strawberry_seed"), 0, 21, 10),
        ShopItem(getRSCM("item.watermelon_seed"), 0, 67, 33),
        ShopItem(getRSCM("item.barley_seed"), 20, 2, 1),
        ShopItem(getRSCM("item.jute_seed"), 5, 6, 3),
        ShopItem(getRSCM("item.rosemary_seed"), 20, 4, 2),
        ShopItem(getRSCM("item.marigold_seed"), 20, 2, 1),
        ShopItem(getRSCM("item.hammerstone_seed"), 20, 2, 1),
        ShopItem(getRSCM("item.asgarnian_seed"), 10, 3, 1),
        ShopItem(getRSCM("item.yanillian_seed"), 5, 7, 3),
        ShopItem(getRSCM("item.krandorian_seed"), 0, 9, 4),
        ShopItem(getRSCM("item.wildblood_seed"), 0, 16, 8),
    )

    init {
        spawnNpc(npc = "npc.olivia", x = 3078, z = 3251, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop("Draynor Seed Market", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.olivia", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.olivia", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Draynor Seed Market")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to my seed market.")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
