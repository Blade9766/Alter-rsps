package org.alter.plugins.content.areas.edgeville.npcs.stores

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
 * The Edgeville General Store, on the main street south of the bank - the wiki polygon
 * covers roughly (3076..3085, 3507..3514).
 *
 * Another `{{StoreTableHead|sellmultiplier=1300|buymultiplier=400|delta=30}}` shop, the same
 * schedule every general store uses, so prices are `floor(cost * multiplier / 1000)` from
 * this cache's own `ItemType.cost`.
 *
 * Its stock is Varrock's list minus the spade, and Falador's plus the knife - the three
 * general stores really do differ slightly, so this is not a copy of either.
 *
 * The counter staff are `npc.shop_keeper_2821` and `npc.shop_assistant_2822` - checked
 * against the wiki's own "Shop keeper (Edgeville)" infobox rather than assumed from the
 * numbering, after Al Kharid and Varrock's swordshop both turned out to have been wired to
 * the wrong variant. Both carry a real "Trade" option in this cache.
 *
 * The wiki gives both staff the same map pin (3080, 3510); the assistant is offset one tile
 * east so the two do not spawn stacked on each other.
 */
class GeneralStorePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val shopkeepers = listOf("npc.shop_keeper_2821", "npc.shop_assistant_2822")

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
        ShopItem(getRSCM("item.knife"), 1, 7, 2),
        ShopItem(getRSCM("item.newcomer_map"), 5, 1, 0),
        ShopItem(getRSCM("item.security_book"), 5, 2, 0),
    )

    init {
        spawnNpc(npc = "npc.shop_keeper_2821", x = 3080, z = 3510, height = 0, walkRadius = 2, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.shop_assistant_2822", x = 3081, z = 3510, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop(SHOP_NAME, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_TRADEABLES) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        shopkeepers.forEach {
            onNpcOption(it, option = "talk-to") { player.queue { dialog(player) } }
            onNpcOption(it, option = "trade") { player.shop() }
        }
    }

    fun Player.shop() = this.openShop(SHOP_NAME)

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Can I help you at all?")

        when (options(player, "Yes please. What are you selling?", "No thanks.")) {
            1 -> {
                chatPlayer(player, "Yes please. What are you selling?")
                player.shop()
            }
            2 -> chatPlayer(player, "No thanks.")
        }
    }

    private companion object {
        const val SHOP_NAME = "Edgeville General Store"
    }
}
