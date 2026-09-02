package org.alter.plugins.content.areas.barbarianvillage.npcs.stores

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
 * Peksa's Helmet Shop, the hut in the west of Barbarian Village at (3075, 3430).
 *
 * Like the Draynor Wine Shop, the wiki publishes this store as multipliers rather
 * than flat prices - `{{StoreTableHead|sellmultiplier=1000|buymultiplier=600}}`, i.e.
 * sell at 100% and buy back at 60% of each item's base value. The prices below were
 * computed from this server's own cache item values (verified via a throwaway cache
 * dump of each helmet's `ItemType.cost`) rather than from a wiki paraphrase of the
 * multiplier arithmetic, so they're guaranteed consistent with this cache. Stock
 * quantities are the wiki's real `stock=` values.
 *
 * `npc.peksa` (2872) really does carry both "Talk-to" and "Trade" in this cache -
 * checked before wiring, the same way the `npc.aubury` mismatch was caught.
 */
class PeksaPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "I could be, yes.",
        "No, I'll pass on that.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.bronze_med_helm"), 5, 24, 14),
        ShopItem(getRSCM("item.iron_med_helm"), 3, 84, 50),
        ShopItem(getRSCM("item.steel_med_helm"), 3, 300, 180),
        ShopItem(getRSCM("item.mithril_med_helm"), 1, 780, 468),
        ShopItem(getRSCM("item.adamant_med_helm"), 1, 1920, 1152),
        ShopItem(getRSCM("item.bronze_full_helm"), 4, 44, 26),
        ShopItem(getRSCM("item.iron_full_helm"), 3, 154, 92),
        ShopItem(getRSCM("item.steel_full_helm"), 2, 550, 330),
        ShopItem(getRSCM("item.mithril_full_helm"), 1, 1430, 858),
        ShopItem(getRSCM("item.adamant_full_helm"), 1, 3520, 2112),
    )

    init {
        spawnNpc(npc = "npc.peksa", x = 3075, z = 3430, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop(SHOP_NAME, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.peksa", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.peksa", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop(SHOP_NAME)

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Are you interested in buying or selling a helmet?")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> {
                chatPlayer(player, "I could be, yes.")
                player.shop()
            }
            2 -> {
                chatPlayer(player, "No, I'll pass on that.")
                chatNpc(player, "Well, come back if you change your mind.")
            }
        }
    }

    private companion object {
        const val SHOP_NAME = "Helmet Shop"
    }
}
