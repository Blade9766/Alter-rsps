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
 * Zaff's Superior Staffs, north-west of Varrock Square, roughly (3203, 3434).
 */
class ZaffPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val dialogOptions: List<String> = listOf(
        "Yes please. What are you selling?",
        "No thanks.",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("item.staff"), 5, 15, 9),
        ShopItem(getRSCM("item.magic_staff"), 5, 200, 120),
        ShopItem(getRSCM("item.battlestaff"), 5, 7000, 4200),
        ShopItem(getRSCM("item.staff_of_air"), 2, 1500, 900),
        ShopItem(getRSCM("item.staff_of_water"), 2, 1500, 900),
        ShopItem(getRSCM("item.staff_of_earth"), 2, 1500, 900),
        ShopItem(getRSCM("item.staff_of_fire"), 2, 1500, 900),
    )

    init {
        spawnNpc(npc = "npc.zaff", x = 3203, z = 3434, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop("Zaff's Superior Staffs!", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.zaff", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.zaff", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop("Zaff's Superior Staffs!")

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Welcome to my superior staff shop!")

        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> player.shop()
            2 -> chatPlayer(player, "No thanks.")
        }
    }
}
