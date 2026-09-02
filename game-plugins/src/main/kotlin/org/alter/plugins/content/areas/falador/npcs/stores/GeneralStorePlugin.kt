package org.alter.plugins.content.areas.falador.npcs.stores

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
 * The Falador General Store, on the north side of the city just west of the park -
 * the wiki polygon covers (2953..2961, 3385..3391).
 *
 * Another multiplier shop: `{{StoreTableHead|sellmultiplier=1300|buymultiplier=400|delta=30}}`,
 * i.e. sell at 130% and buy back at 40% of each item's base value, with no per-item price
 * published anywhere in the wikitext. Prices below are `floor(cost * multiplier)` computed
 * from this server's own cache values (dumped from `ItemType.cost`), not from a wiki
 * paraphrase of the arithmetic. As a cross-check they come out identical to the numbers
 * already in Varrock's `GeneralStorePlugin`, which is the same 1300/400 store.
 *
 * Falador's list is not quite Varrock's: it carries no knife and no spade, which is why
 * those two are absent here.
 *
 * Both counter staff (`npc.shop_keeper_2819` at (2958, 3387) and `npc.shop_assistant_2820`
 * at (2958, 3388)) really carry "Talk-to" and "Trade" in this cache - checked before wiring.
 */
class GeneralStorePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val shopkeepers = listOf("npc.shop_keeper_2819", "npc.shop_assistant_2820")

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
        spawnNpc(npc = "npc.shop_keeper_2819", x = 2958, z = 3387, height = 0, walkRadius = 2, direction = Direction.WEST)
        spawnNpc(npc = "npc.shop_assistant_2820", x = 2958, z = 3388, height = 0, walkRadius = 2, direction = Direction.WEST)

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
        const val SHOP_NAME = "Falador General Store"
    }
}
