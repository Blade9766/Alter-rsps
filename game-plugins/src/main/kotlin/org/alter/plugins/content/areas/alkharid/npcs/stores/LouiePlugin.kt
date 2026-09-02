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
 * Louie's Armoured Legs Bazaar, roughly (3316, 3175).
 */
class LouiePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.bronze_platelegs"), 5, 80, 52),
        ShopItem(getRSCM("item.iron_platelegs"), 3, 280, 182),
        ShopItem(getRSCM("item.steel_platelegs"), 2, 1000, 650),
        ShopItem(getRSCM("item.black_platelegs"), 1, 1920, 1248),
        ShopItem(getRSCM("item.mithril_platelegs"), 1, 2600, 1690),
        ShopItem(getRSCM("item.adamant_platelegs"), 1, 6400, 4160),
    )

    init {
        spawnNpc(npc = "npc.louie_legs", x = 3316, z = 3175, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop("Louie's Armoured Legs Bazaar", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.louie_legs", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.louie_legs", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Louie's Armoured Legs Bazaar")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to my leg armour bazaar.")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
