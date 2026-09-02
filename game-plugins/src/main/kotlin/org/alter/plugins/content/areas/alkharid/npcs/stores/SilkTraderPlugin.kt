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
 * The Silk trader, north of the Al Kharid palace, roughly (3298, 3202).
 *
 * Unlike the other Al Kharid shopkeepers, this NPC only has a "Talk-to" option in the
 * cache (no "Trade") - the purchase is reached through dialogue, matching how the real
 * silk trader haggles over price rather than offering a normal shop right-click.
 */
class SilkTraderPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Can I buy some silk?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.silk"), 100, 3, 0, resupplyAmount = 100),
    )

    init {
        spawnNpc(npc = "npc.silk_trader", x = 3298, z = 3202, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop("Silk Trader", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.silk_trader", option = "talk-to") { player.queue { dialog(player) } }
    }

    fun Player.shop() = this.openShop("Silk Trader")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Fine silk! Only 3 gold coins!")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
