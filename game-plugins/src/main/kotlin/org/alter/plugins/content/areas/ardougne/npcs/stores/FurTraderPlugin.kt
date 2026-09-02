package org.alter.plugins.content.areas.ardougne.npcs.stores

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
 * The fur trader who runs the Ardougne Fur Stall in the East Ardougne market, at
 * (2664, 3296).
 *
 * `{{StoreTableHead|sellmultiplier=1200|buymultiplier=950|delta=20}}` - and that 950 is the
 * most generous buy-back of any shop in this project by a wide margin (the next best is
 * Herquin's 700, and general stores pay 400). Selling furs here really is close to selling
 * them at value, which is the point of the shop.
 *
 * The wiki lists ten further lines - polar kebbit, common kebbit, feldip weasel, desert
 * devil, larupia, graahk and kyatt furs, tatty and otherwise - all at `stock=0`. Every one
 * is a **Hunter** drop, and Hunter is not built here, so none of them can exist in a
 * player's inventory to sell. They are omitted rather than listed as unreachable rows.
 * Restore them alongside Hunter.
 *
 * Note this npc is the fur *shop*; the Ardougne fur **stall** you steal from is a separate
 * scenery object, already covered by `data/cfg/thieving/stalls.json` at Thieving 35.
 */
class FurTraderPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val storeItems = listOf(
        ShopItem(getRSCM("item.bear_fur"), 3, 12, 9),
        ShopItem(getRSCM("item.grey_wolf_fur"), 3, 60, 47),
    )

    init {
        spawnNpc(npc = "npc.fur_trader", x = 2664, z = 3296, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop(SHOP_NAME, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.fur_trader", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.fur_trader", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop(SHOP_NAME)

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Would you like to buy some fine furs?")

        when (options(player, "Yes please.", "No thanks.")) {
            1 -> {
                chatPlayer(player, "Yes please.")
                player.shop()
            }
            2 -> chatPlayer(player, "No thanks.")
        }
    }

    private companion object {
        const val SHOP_NAME = "Ardougne Fur Stall"
    }
}
