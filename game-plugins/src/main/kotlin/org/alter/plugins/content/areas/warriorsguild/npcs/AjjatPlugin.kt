package org.alter.plugins.content.areas.warriorsguild.npcs

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.shop.PurchasePolicy
import org.alter.game.model.shop.ShopItem
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.items.skillcapes.SkillCape
import org.alter.plugins.content.mechanics.shops.CoinCurrency
import org.alter.rscm.RSCM.getRSCM

/**
 * Ajjat, "a former Kinshra knight and master swordsman, who sells the Attack cape to those who
 * have reached 99 Attack".
 *
 * He is the Attack skill's own reward vendor, and the reason
 * [org.alter.plugins.content.items.skillcapes.SkillCapePlugin] has anything to gate: before this
 * there was no way to obtain an Attack cape at all. He also hosts the dummy room, which is
 * [org.alter.plugins.content.areas.warriorsguild.activities.DummyRoomPlugin]'s business rather
 * than his.
 *
 * ## The stock
 *
 * The wiki lists exactly two lines - Attack cape and Attack cape(t), both 99,000 coins - and gives
 * no stock quantity, so each is stocked singly and restocks. The refusal to sell below 99 Attack
 * is Ajjat's own, checked here rather than left to the cape's equip requirement: being sold a
 * 99,000 coin cape you cannot wear would be a worse experience than being told no.
 *
 * **The hood is not sold.** In OSRS the hood comes with the cape rather than being a separate
 * purchase, and this shop system has no way to bundle a second item into one sale. A player who
 * buys a cape here therefore gets the cape alone. That is the one part of this shop that does not
 * match the real one.
 */
class AjjatPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        createShop(SHOP_NAME, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            items[0] = ShopItem(getRSCM(SkillCape.ATTACK.cape), 1, CAPE_PRICE, CAPE_PRICE)
            items[1] = ShopItem(getRSCM(SkillCape.ATTACK.trimmed), 1, CAPE_PRICE, CAPE_PRICE)
        }

        onNpcOption(npc = AJJAT, option = "talk-to") { player.queue { dialog(player) } }

        onNpcOption(npc = AJJAT, option = "claim-tokens") {
            player.message("Ajjat has no tokens waiting for you.")
        }
    }

    private suspend fun QueueTask.dialog(player: Player) {
        val attack = player.getSkills().getBaseLevel(Skills.ATTACK)

        if (attack < SkillCape.REQUIRED_LEVEL) {
            chatNpc(player, "You handle a blade well enough, but not well enough<br>for one of my capes. Come back at 99 Attack.")
            return
        }

        chatNpc(player, "A true master of the blade. I have a cape worthy of<br>you, if you can meet the price.")
        when (options(player, "Show me the cape.", "Not today.")) {
            1 -> {
                chatPlayer(player, "Show me the cape.")
                player.openShop(SHOP_NAME)
            }
            2 -> chatPlayer(player, "Not today.")
        }
    }

    private companion object {
        const val AJJAT = "npc.ajjat"
        const val SHOP_NAME = "Ajjat's Attack Capes"

        /** The wiki's figure for both the plain and the trimmed cape. */
        const val CAPE_PRICE = 99_000
    }
}
