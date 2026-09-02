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
 * Horvik's Armour Shop, south of Varrock Square, roughly (3230, 3437).
 */
class HorvikPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.bronze_chainbody"), 5, 60, 36),
        ShopItem(getRSCM("item.iron_chainbody"), 3, 210, 126),
        ShopItem(getRSCM("item.steel_chainbody"), 3, 750, 450),
        ShopItem(getRSCM("item.mithril_chainbody"), 1, 1950, 1170),
        ShopItem(getRSCM("item.bronze_platebody"), 3, 160, 96),
        ShopItem(getRSCM("item.iron_platebody"), 1, 560, 336),
        ShopItem(getRSCM("item.steel_platebody"), 1, 2000, 1200),
        ShopItem(getRSCM("item.black_platebody"), 1, 3840, 2304),
        ShopItem(getRSCM("item.mithril_platebody"), 1, 5200, 3120),
        ShopItem(getRSCM("item.iron_platelegs"), 1, 280, 168),
        ShopItem(getRSCM("item.studded_body"), 1, 850, 510),
        ShopItem(getRSCM("item.studded_chaps"), 1, 750, 450),
    )

    init {
        spawnNpc(npc = "npc.horvik", x = 3230, z = 3437, height = 0, walkRadius = 2, direction = Direction.NORTH)

        createShop("Horvik's Armour Shop", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.horvik", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.horvik", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Horvik's Armour Shop")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to my armour shop.")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
