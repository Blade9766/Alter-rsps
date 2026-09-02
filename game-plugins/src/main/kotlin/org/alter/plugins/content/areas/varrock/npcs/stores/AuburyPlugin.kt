package org.alter.plugins.content.areas.varrock.npcs.stores

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
 * Aubury's Rune Shop, south-east Varrock, just south of the east bank, roughly (3253, 3402).
 */
class AuburyPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.air_rune"), 5000, 4, 2),
        ShopItem(getRSCM("item.fire_rune"), 5000, 4, 2),
        ShopItem(getRSCM("item.water_rune"), 5000, 4, 2),
        ShopItem(getRSCM("item.earth_rune"), 5000, 4, 2),
        ShopItem(getRSCM("item.mind_rune"), 5000, 3, 1),
        ShopItem(getRSCM("item.body_rune"), 5000, 3, 1),
        ShopItem(getRSCM("item.chaos_rune"), 250, 90, 54),
        ShopItem(getRSCM("item.death_rune"), 250, 180, 108),
        ShopItem(getRSCM("item.fire_rune_pack"), 80, 430, 258),
        ShopItem(getRSCM("item.water_rune_pack"), 80, 430, 258),
        ShopItem(getRSCM("item.air_rune_pack"), 80, 430, 258),
        ShopItem(getRSCM("item.earth_rune_pack"), 80, 430, 258),
        ShopItem(getRSCM("item.mind_rune_pack"), 40, 330, 198),
        ShopItem(getRSCM("item.chaos_rune_pack"), 35, 9950, 5970),
    )

    init {
        spawnNpc(npc = "npc.aubury_11434", x = 3253, z = 3402, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop("Aubury's Rune Shop", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.aubury_11434", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.aubury_11434", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Aubury's Rune Shop")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Hello, would you like to buy some runes?")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
