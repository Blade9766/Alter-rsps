package org.alter.plugins.content.areas.varrock.npcs.pubs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.options
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.areas.varrock.npcs.pubs.Bar.BEER_PRICE
import org.alter.plugins.content.areas.varrock.npcs.pubs.Bar.STEW_PRICE
import org.alter.plugins.content.areas.varrock.npcs.pubs.Bar.canAfford
import org.alter.plugins.content.areas.varrock.npcs.pubs.Bar.pay

/**
 * The Ratpit Bar under south-east Varrock: one barman (npc 1013) at 2909,5078, in the Rat Pits below
 * the manhole at 3267,3400.
 *
 * It is the only Varrock pub that serves food - stew, 20 coins - which is the whole reason it is
 * here rather than being folded into the other three. It is also the only one behind a lock: the
 * wiki gates the room on Ratcatchers, and the manhole down to it is not built either, so nothing
 * currently walks a player into this room. The barman is placed anyway, on his real tile in a region
 * (11599) that this cache does have, so that the bar works the moment a route to it exists. Nothing
 * here fakes that route.
 *
 * The transcript wraps both purchases in a Pay / Don't pay confirmation, which is faithfully
 * reproduced - it is the only Varrock bar that asks twice before taking money.
 */
class RatpitBarPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = BARMAN, x = 2909, z = 5078, walkRadius = 1, direction = Direction.SOUTH)

        onNpcOption(BARMAN, option = "talk-to", lineOfSightDistance = 4) { player.queue { barman(player) } }
    }

    private suspend fun QueueTask.barman(player: Player) {
        chatNpc(player, "Welcome traveller, can I get you a drink or some food?")

        when (options(
            player,
            "I'd like a beer please.",
            "I'd like some food please.",
            "I have to go.",
        )) {
            1 -> {
                chatPlayer(player, "I'd like a beer please.")
                chatNpc(player, "That'll be 2 gold coins.")
                confirm(player, "item.beer", BEER_PRICE)
            }

            2 -> {
                chatPlayer(player, "I'd like some food please.")
                chatNpc(player, "I can make you a stew for 20 gold coins.")
                confirm(player, "item.stew", STEW_PRICE)
            }

            3 -> chatPlayer(player, "I have to go.")
        }
    }

    /** The Pay / Don't pay step both of his offers end with. */
    private suspend fun QueueTask.confirm(player: Player, item: String, price: Int) {
        when (options(player, "Pay.", "Don't pay.")) {
            1 -> {
                if (!player.canAfford(price)) {
                    chatPlayer(player, "Sorry, I don't have $price coins on me.")
                } else if (pay(player, item, price)) {
                    chatNpc(player, "Thanks for your custom.")
                }
            }

            2 -> chatPlayer(player, "Sorry, I changed my mind.")
        }
    }

    private companion object {
        const val BARMAN = "npc.barman"
    }
}
