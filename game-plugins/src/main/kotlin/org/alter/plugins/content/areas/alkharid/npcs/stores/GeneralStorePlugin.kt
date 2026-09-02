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
 * The Al Kharid general store, east of the palace, roughly (3315, 3182).
 *
 * Shop keeper 2817 / shop assistant 2818 are this store's real cache ids, per the wiki's
 * "Shop keeper (Al Kharid)" infobox. They previously read 2819/2820, which are Falador's
 * counter staff - harmless while Falador did not exist, but it meant one npc id backed two
 * different shops. Do not renumber these without re-checking the wiki infobox.
 */
class GeneralStorePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val shopkeepers = listOf("npc.shop_keeper_2817", "npc.shop_assistant_2818")

    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.pot"), 5, 1, 0),
        ShopItem(getRSCM("item.jug"), 2, 1, 0),
        ShopItem(getRSCM("item.empty_jug_pack"), 5, 182, 56),
        ShopItem(getRSCM("item.shears"), 2, 1, 0),
        ShopItem(getRSCM("item.bucket"), 3, 2, 0),
        ShopItem(getRSCM("item.empty_bucket_pack"), 15, 650, 200),
        ShopItem(getRSCM("item.bowl"), 2, 5, 1),
        ShopItem(getRSCM("item.cake_tin"), 2, 13, 4),
        ShopItem(getRSCM("item.tinderbox"), 2, 1, 0),
        ShopItem(getRSCM("item.chisel"), 2, 1, 0),
        ShopItem(getRSCM("item.hammer"), 5, 1, 0),
        ShopItem(getRSCM("item.newcomer_map"), 5, 1, 0),
        ShopItem(getRSCM("item.security_book"), 5, 2, 0),
    )

    init {
        spawnNpc(npc = "npc.shop_keeper_2817", x = 3315, z = 3182, height = 0, walkRadius = 3, direction = Direction.EAST)
        spawnNpc(npc = "npc.shop_assistant_2818", x = 3316, z = 3182, height = 0, walkRadius = 3, direction = Direction.EAST)

        createShop("Al Kharid General Store", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_TRADEABLES) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        shopkeepers.forEach {
            onNpcOption(it, option = "talk-to") { player.queue { dialog(player) } }
            onNpcOption(it, option = "trade") { player.shop() }
        }
    }

    fun Player.shop() = this.openShop("Al Kharid General Store")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Can I help you at all?")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
