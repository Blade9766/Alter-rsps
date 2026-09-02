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
 * Thessalia's Fine Clothes, on Varrock Square, roughly (3206, 3416).
 */
class ThessaliaPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.white_apron"), 3, 2, 1),
        ShopItem(getRSCM("item.leather_body"), 12, 21, 13),
        ShopItem(getRSCM("item.leather_gloves"), 10, 6, 4),
        ShopItem(getRSCM("item.leather_boots"), 10, 6, 4),
        ShopItem(getRSCM("item.brown_apron"), 1, 2, 1),
        ShopItem(getRSCM("item.pink_skirt"), 5, 2, 1),
        ShopItem(getRSCM("item.black_skirt"), 3, 2, 1),
        ShopItem(getRSCM("item.blue_skirt"), 2, 2, 1),
        ShopItem(getRSCM("item.red_cape"), 4, 2, 1),
        ShopItem(getRSCM("item.silk"), 5, 30, 18),
        ShopItem(getRSCM("item.priest_gown"), 3, 5, 3),
        ShopItem(getRSCM("item.priest_gown_428"), 3, 5, 3),
    )

    init {
        spawnNpc(npc = "npc.thessalia", x = 3206, z = 3416, height = 0, walkRadius = 2, direction = Direction.EAST)

        createShop("Thessalia's Fine Clothes", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.thessalia", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.thessalia", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Thessalia's Fine Clothes")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to my clothes shop.")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
