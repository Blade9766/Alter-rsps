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
 * Lowe's Archery Emporium, just east of Varrock Square, roughly (3235, 3424).
 */
class LowePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.bronze_arrow"), 2000, 1, 0),
        ShopItem(getRSCM("item.iron_arrow"), 1500, 3, 1),
        ShopItem(getRSCM("item.steel_arrow"), 1000, 12, 7),
        ShopItem(getRSCM("item.mithril_arrow"), 800, 32, 19),
        ShopItem(getRSCM("item.adamant_arrow"), 600, 80, 48),
        ShopItem(getRSCM("item.bronze_bolts"), 1500, 1, 0),
        ShopItem(getRSCM("item.shortbow"), 4, 50, 30),
        ShopItem(getRSCM("item.longbow"), 4, 80, 48),
        ShopItem(getRSCM("item.oak_shortbow"), 3, 100, 60),
        ShopItem(getRSCM("item.oak_longbow"), 3, 160, 96),
        ShopItem(getRSCM("item.willow_shortbow"), 2, 200, 120),
        ShopItem(getRSCM("item.willow_longbow"), 2, 320, 192),
        ShopItem(getRSCM("item.maple_shortbow"), 1, 400, 240),
        ShopItem(getRSCM("item.maple_longbow"), 1, 640, 384),
        ShopItem(getRSCM("item.crossbow"), 2, 70, 42),
    )

    init {
        spawnNpc(npc = "npc.lowe", x = 3235, z = 3424, height = 0, walkRadius = 2, direction = Direction.WEST)

        createShop("Lowe's Archery Emporium", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.lowe", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.lowe", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Lowe's Archery Emporium")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to my archery emporium.")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
