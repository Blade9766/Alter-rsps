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
 * The Wine Shop, roughly (3085, 3253) in the Draynor Village marketplace.
 *
 * The wiki's shop table shows this store using a 1000%/600% sell/buy multiplier on
 * each item's base value rather than flat prices (its raw `{{StoreLine}}` markup has
 * no per-item price at all) - the prices below were computed directly from this
 * server's own cache item values rather than trusting a wiki summary of the
 * multiplier math, so they're guaranteed consistent with what's actually in this
 * cache. Not implemented: the "jug of vinegar" being gated behind the Rag and Bone
 * Man I quest, since there's no quest system yet - it's just sold outright here.
 */
class FortunatoPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.jug_of_wine"), 5, 10, 6),
        ShopItem(getRSCM("item.jug"), 3, 10, 6),
        ShopItem(getRSCM("item.empty_jug_pack"), 5, 1400, 840),
        ShopItem(getRSCM("item.bottle_of_wine"), 2, 5000, 3000),
        ShopItem(getRSCM("item.jug_of_vinegar"), 500, 10, 6),
    )

    init {
        spawnNpc(npc = "npc.fortunato", x = 3085, z = 3253, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop("Wine Shop", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.fortunato", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.fortunato", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Wine Shop")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to my wine shop.")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
