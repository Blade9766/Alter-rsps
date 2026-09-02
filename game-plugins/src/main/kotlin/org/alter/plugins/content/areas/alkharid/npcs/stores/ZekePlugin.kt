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
 * Zeke's Superior Scimitars, north-east of the Al Kharid bank, roughly (3288, 3190).
 */
class ZekePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.bronze_scimitar"), 5, 32, 18),
        ShopItem(getRSCM("item.iron_scimitar"), 3, 112, 62),
        ShopItem(getRSCM("item.steel_scimitar"), 2, 400, 220),
        ShopItem(getRSCM("item.mithril_scimitar"), 1, 1040, 572),
    )

    init {
        spawnNpc(npc = "npc.zeke", x = 3288, z = 3190, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop("Zeke's Superior Scimitars!", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.zeke", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.zeke", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Zeke's Superior Scimitars!")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to my scimitar shop.")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
