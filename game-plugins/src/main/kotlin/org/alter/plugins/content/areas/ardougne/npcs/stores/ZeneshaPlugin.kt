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
 * Zenesha's Plate Mail Body Shop, East Ardougne, with Zenesha at (2653, 3294).
 *
 * **The npc key is `npc.zenesha_8681`, not `npc.zenesha`.** This one is worth spelling out:
 * `npc.zenesha` resolves to id 4584, which a cache check showed has
 * `actions=[null,null,null,null,null]` - no Talk-to, no Trade, nothing. The wiki confirms
 * why: 4584 is her *Ratcatchers quest* variant, which stands in a mansion at (2844, 5092),
 * while 8681 is the shopkeeper who actually stands in Ardougne. Wiring the obvious-looking
 * key would have produced a silent, unusable shop - the same trap `npc.aubury` sprang.
 *
 * `{{StoreTableHead|sellmultiplier=1000|buymultiplier=600|delta=20}}`, so sell at cost and
 * buy back at 60%, computed from this cache's own `ItemType.cost`.
 *
 * Her Song of the Elves tax-collection branch is left out along with every other quest
 * branch in this area.
 */
class ZeneshaPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val storeItems = listOf(
        ShopItem(getRSCM("item.bronze_platebody"), 3, 160, 96),
        ShopItem(getRSCM("item.iron_platebody"), 1, 560, 336),
        ShopItem(getRSCM("item.steel_platebody"), 1, 2000, 1200),
        ShopItem(getRSCM("item.black_platebody"), 1, 3840, 2304),
        ShopItem(getRSCM("item.mithril_platebody"), 1, 5200, 3120),
    )

    init {
        spawnNpc(npc = "npc.zenesha_8681", x = 2653, z = 3294, height = 0, walkRadius = 2, direction = Direction.WEST)

        createShop(SHOP_NAME, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.zenesha_8681", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.zenesha_8681", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop(SHOP_NAME)

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Hello there! I sell plate mail bodies, are you interested?")

        when (options(player, "I'm interested.", "Sorry, I'm not interested.")) {
            1 -> {
                chatPlayer(player, "I'm interested.")
                player.shop()
            }
            2 -> chatPlayer(player, "Sorry, I'm not interested.")
        }
    }

    private companion object {
        const val SHOP_NAME = "Zenesha's Plate Mail Body Shop"
    }
}
