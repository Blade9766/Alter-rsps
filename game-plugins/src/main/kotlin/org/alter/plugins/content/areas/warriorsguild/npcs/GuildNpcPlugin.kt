package org.alter.plugins.content.areas.warriorsguild.npcs

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.shop.PurchasePolicy
import org.alter.game.model.shop.ShopItem
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.areas.warriorsguild.DefenderLadder
import org.alter.plugins.content.areas.warriorsguild.WarriorsGuild
import org.alter.plugins.content.mechanics.shops.CoinCurrency
import org.alter.rscm.RSCM.getRSCM

/**
 * The guild's remaining staff: the two door guards, the guildmaster, the two room hosts who have
 * nothing to hand out, the two cyclops wardens, and Lilly's potion shop.
 *
 * One file rather than eight, because seven of the nine are a single `talk-to` that explains what
 * the room next to them does. Ajjat has his own file because he runs a shop with a level check,
 * and Gamfred's shield handout lives with the catapult it is for.
 *
 * Kamfreena and Lorelai matter beyond their dialogue: they are the players' only in-game
 * explanation of the token cost and the defender ladder, both of which are otherwise invisible
 * rules. Lorelai's line changes depending on whether the rune defender is already earned, since
 * that is what her door checks.
 */
class GuildNpcPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        createShop(POTION_SHOP, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            POTIONS.forEachIndexed { index, item -> items[index] = item }
        }

        onNpcOption(npc = LILLY, option = "talk-to") { player.queue { lilly(player) } }
        onNpcOption(npc = LILLY, option = "trade") { player.openShop(POTION_SHOP) }

        listOf(GHOMMAL, LAIDEE).forEach { guard ->
            onNpcOption(npc = guard, option = "talk-to") { player.queue { doorGuard(player) } }
        }

        onNpcOption(npc = HARRALLAK, option = "talk-to") { player.queue { guildmaster(player) } }
        onNpcOption(npc = SHANOMI, option = "talk-to") { player.queue { shanomi(player) } }
        onNpcOption(npc = GAMFRED, option = "talk-to") { player.queue { gamfred(player) } }
        onNpcOption(npc = KAMFREENA, option = "talk-to") { player.queue { warden(player, basement = false) } }
        onNpcOption(npc = LORELAI, option = "talk-to") { player.queue { warden(player, basement = true) } }

        /*
         * Claim-Tokens is the Combat Achievement reward claim in OSRS, and combat achievements do
         * not exist here. Bound anyway so the option says something rather than logging an
         * unhandled action.
         */
        listOf(KAMFREENA, SHANOMI, GAMFRED, LORELAI).forEach { npc ->
            onNpcOption(npc = npc, option = "claim-tokens") {
                player.message("You have no tokens waiting to be claimed.")
            }
        }
    }

    private suspend fun QueueTask.doorGuard(player: Player) {
        if (WarriorsGuild.meetsEntryRequirement(player)) {
            chatNpc(player, "Go on in. You have the arm for it.")
            return
        }
        chatNpc(
            player,
            "Not so fast. The guild is for warriors - come back when<br>" +
                "your Attack and Strength together reach ${WarriorsGuild.COMBINED_LEVEL},<br>" +
                "or when either alone reaches ${WarriorsGuild.MASTERY_LEVEL}.",
        )
    }

    private suspend fun QueueTask.guildmaster(player: Player) {
        chatNpc(player, "Welcome to the Warriors' Guild. I am Harrallak<br>Menarous, and everything you see here is mine.")
        chatNpc(
            player,
            "Earn tokens in the rooms around you, then spend them<br>" +
                "upstairs on the cyclopes. They carry defenders, and<br>" +
                "there is no other way to get one.",
        )
    }

    private suspend fun QueueTask.shanomi(player: Player) {
        chatNpc(
            player,
            "Bring a full helm, platebody and platelegs of one metal<br>" +
                "and feed them to an animator. Whatever stands up will<br>" +
                "want a word with you.",
        )
        chatNpc(player, "The better the metal, the more tokens it is worth -<br>and the less of it you lose in the fight.")
    }

    private suspend fun QueueTask.gamfred(player: Player) {
        chatNpc(player, "Take a defensive shield and stand in front of the<br>catapult. Block what it sends you.")
        chatNpc(player, "Defence experience and a token for every one you stop.<br>Turn up without the shield and you will feel it.")
    }

    private suspend fun QueueTask.warden(
        player: Player,
        basement: Boolean,
    ) {
        if (basement && !DefenderLadder.hasRuneDefender(player)) {
            chatNpc(
                player,
                "The cyclopes down here are a different proposition.<br>" +
                    "Come back with a rune defender and I will let you<br>through.",
            )
            return
        }

        val prize = if (basement) "dragon defender" else "defenders"
        chatNpc(
            player,
            "${WarriorsGuild.TOKENS_TO_ENTER} tokens to go in, ${WarriorsGuild.TOKEN_DRAIN} of them<br>" +
                "gone the moment you step through, and ${WarriorsGuild.TOKEN_DRAIN} more<br>every minute you stay.",
        )
        chatNpc(player, "The cyclopes in there carry $prize.<br>An Attack cape gets you in for nothing.")

        if (!basement) {
            val next = DefenderLadder.nextRung(player)
            if (next == null) {
                chatNpc(player, "You have the rune defender already. Talk to my<br>apprentice Lorelai, in the basement.")
            } else {
                chatNpc(player, "One defender per visit, mind. Get one and you must<br>step out and come back for the next.")
            }
        }
    }

    private suspend fun QueueTask.lilly(player: Player) {
        chatNpc(player, "Potions for the fighting sort. Attack, Strength,<br>Defence - want a look?")
        when (options(player, "Yes please.", "No thanks.")) {
            1 -> {
                chatPlayer(player, "Yes please.")
                player.openShop(POTION_SHOP)
            }
            2 -> chatPlayer(player, "No thanks.")
        }
    }

    private companion object {
        const val GHOMMAL = "npc.ghommal_13613"
        const val LAIDEE = "npc.laidee_gnonock"
        const val HARRALLAK = "npc.harrallak_menarous_13615"
        const val SHANOMI = "npc.shanomi"
        const val GAMFRED = "npc.gamfred"
        const val KAMFREENA = "npc.kamfreena"
        const val LORELAI = "npc.lorelai"
        const val LILLY = "npc.lilly"

        const val POTION_SHOP = "Warrior Guild Potion Shop"

        /**
         * The wiki's stock table: three-dose Attack, Strength and Defence potions, 10 in stock,
         * 240 coins each.
         */
        val POTIONS =
            listOf(
                ShopItem(getRSCM("item.attack_potion3"), 10, 240, 144),
                ShopItem(getRSCM("item.strength_potion3"), 10, 240, 144),
                ShopItem(getRSCM("item.defence_potion3"), 10, 240, 144),
            )
    }
}
