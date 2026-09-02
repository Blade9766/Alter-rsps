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
 * The Gem Trader's stall, roughly (3287, 3212), east of the Lumbridge gate.
 */
class GemTraderPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.uncut_sapphire"), 1, 25, 17),
        ShopItem(getRSCM("item.uncut_emerald"), 1, 50, 35),
        ShopItem(getRSCM("item.uncut_ruby"), 0, 100, 70),
        ShopItem(getRSCM("item.uncut_diamond"), 0, 200, 140),
        ShopItem(getRSCM("item.sapphire"), 1, 250, 175),
        ShopItem(getRSCM("item.emerald"), 1, 500, 350),
        ShopItem(getRSCM("item.ruby"), 0, 1000, 700),
        ShopItem(getRSCM("item.diamond"), 0, 2000, 1400),
    )

    init {
        spawnNpc(npc = "npc.gem_trader", x = 3287, z = 3212, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop("Gem Trader", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.gem_trader", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.gem_trader", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Gem Trader")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Would you like to see my gems?")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
