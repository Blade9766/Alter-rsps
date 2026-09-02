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
 * Oziach, in his hut at the north-west corner of Edgeville - (3070, 3517) per his own map
 * pin, with the shop itself at (3069, 3517).
 *
 * `{{StoreTableHead|sellmultiplier=1300|buymultiplier=400|delta=30}}` - the same schedule
 * the general stores use, which is why a rune platebody costs 84,500 rather than its 65,000
 * base value. Prices computed from this cache's own `ItemType.cost`.
 *
 * **The Dragon Slayer gate is not implemented, deliberately.** In the real game Oziach
 * refuses to trade at all until you have completed Dragon Slayer I - "I ain't got nothing to
 * sell ye, adventurer. Leave me be!" - and only then sells the rune platebody. There is no
 * quest framework on this server, so there is nothing to check: gating him would make the
 * shop permanently unreachable, and stubbing the quest would be faking it. He therefore
 * trades freely, and his dialogue uses the post-quest lines. This is the one place in the
 * Edgeville build where behaviour knowingly differs from OSRS rather than being merely
 * incomplete - flagged here rather than buried.
 *
 * His "Can I have another key to Melzar's Maze?" branch is left out for the same reason: it
 * only exists mid-Dragon-Slayer and points at a Champions' Guild that isn't built.
 */
class OziachPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val storeItems = listOf(
        ShopItem(getRSCM("item.rune_platebody"), 2, 84500, 26000),
        ShopItem(getRSCM("item.green_dhide_body"), 2, 10140, 3120),
        ShopItem(getRSCM("item.antidragon_shield"), 35, 26, 8),
    )

    init {
        spawnNpc(npc = "npc.oziach", x = 3070, z = 3517, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        createShop(SHOP_NAME, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npc.oziach", option = "talk-to") { player.queue { dialog(player) } }
        onNpcOption("npc.oziach", option = "trade") { player.shop() }
    }

    fun Player.shop() = this.openShop(SHOP_NAME)

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Aye, 'tis a fair day, my mighty dragon-slaying friend.")

        when (options(
            player,
            "Can I buy a rune platebody now, please?",
            "Yes, it's a very nice day.",
            "I'm not your friend.",
        )) {
            1 -> {
                chatPlayer(player, "Can I buy a rune platebody now please?")
                player.shop()
            }

            2 -> {
                chatPlayer(player, "Yes, it's a very nice day.")
                chatNpc(player, "Aye, may the gods walk by yer side. Now leave me<br>alone.")
            }

            3 -> {
                chatPlayer(player, "I'm not your friend.")
                chatNpc(player, "I'm surprised if you're anyone's friend with those kind<br>of manners.")
            }
        }
    }

    private companion object {
        const val SHOP_NAME = "Oziach's Armour"
    }
}
