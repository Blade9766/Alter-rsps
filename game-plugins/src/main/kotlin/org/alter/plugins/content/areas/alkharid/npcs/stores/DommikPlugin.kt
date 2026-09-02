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
 * Dommik's Crafting Store, north-eastern Al Kharid, roughly (3321, 3194).
 */
class DommikPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.chisel"), 2, 1, 0),
        ShopItem(getRSCM("item.ring_mould"), 4, 5, 3),
        ShopItem(getRSCM("item.necklace_mould"), 2, 5, 3),
        ShopItem(getRSCM("item.amulet_mould"), 2, 5, 3),
        ShopItem(getRSCM("item.needle"), 3, 1, 0),
        ShopItem(getRSCM("item.thread"), 100, 1, 0),
        ShopItem(getRSCM("item.holy_mould"), 3, 5, 3),
        ShopItem(getRSCM("item.sickle_mould"), 6, 10, 6),
        ShopItem(getRSCM("item.tiara_mould"), 10, 100, 60),
        ShopItem(getRSCM("item.bolt_mould"), 10, 25, 15),
        ShopItem(getRSCM("item.bracelet_mould"), 5, 5, 3),
    )

    init {
        spawnNpc(npc = "npc.dommik", x = 3321, z = 3194, height = 0, walkRadius = 2, direction = Direction.WEST)

        createShop("Dommik's Crafting Store", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.dommik", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.dommik", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Dommik's Crafting Store")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to my crafting store.")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
