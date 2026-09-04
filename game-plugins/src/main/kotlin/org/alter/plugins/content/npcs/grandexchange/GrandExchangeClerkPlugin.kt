package org.alter.plugins.content.npcs.grandexchange

import org.alter.api.ChatMessageType
import org.alter.api.ext.chatNpc
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.interfaces.bank.openBank
import org.alter.plugins.content.interfaces.itemsets.ItemSets.openSets
import org.alter.plugins.content.mechanics.grandexchange.openGrandExchange
import org.alter.plugins.content.mechanics.grandexchange.openGrandExchangeCollectionBox

/**
 * The Varrock Grand Exchange: its four clerks, the exchange booths and the bank booths that ring
 * them.
 *
 * The booths are static map objects and already sit in the cache, so only the clerks need spawning.
 * They stand on the raised centre - blocked to players, exactly as in OSRS - each one behind an
 * exchange booth and facing out over it.
 *
 * **Where "behind" is** was derived from the cache rather than guessed. The centre platform is the
 * blocked 4x4 block from (3163,3488) to (3166,3491), and the seven booths ring it: exchange booths
 * (10061) at (3164,3487) and (3165,3487) rotation 1, (3164,3492) and (3165,3492) rotation 3; bank
 * booths (10060) at (3162,3489) rotation 2 and (3167,3489)/(3167,3490) rotation 0. Reading each
 * rotation as facing the platform - 1 north, 3 south, 2 east, 0 west - puts every single booth's
 * back tile on the platform and nowhere else, which is what makes the reading safe.
 *
 * Note that this is **not** the mapping [org.alter.plugins.content.npcs.banker.Teller] uses for bank
 * booths, where rotation 0 means south and 2 means north. The Grand Exchange booth is a different
 * model with its default orientation turned a quarter turn, the same kind of per-model exception
 * that Lletya's bank counter is over there.
 */
class GrandExchangeClerkPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {

    init {
        // Four clerks behind the four exchange booths at (3164,3487), (3165,3487), (3164,3492) and
        // (3165,3492). They never move, so a walk radius of zero keeps them on the centre platform.
        spawnNpc(npc = "npc.grand_exchange_clerk", x = 3164, z = 3488, height = 0, walkRadius = 0, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.grand_exchange_clerk_2149", x = 3165, z = 3488, height = 0, walkRadius = 0, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.grand_exchange_clerk_2150", x = 3164, z = 3491, height = 0, walkRadius = 0, direction = Direction.NORTH)
        spawnNpc(npc = "npc.grand_exchange_clerk_2151", x = 3165, z = 3491, height = 0, walkRadius = 0, direction = Direction.NORTH)

        val clerks =
            arrayOf(
                "npc.grand_exchange_clerk",
                "npc.grand_exchange_clerk_2149",
                "npc.grand_exchange_clerk_2150",
                "npc.grand_exchange_clerk_2151",
            )

        clerks.forEach { clerk ->
            onNpcOption(npc = clerk, option = "exchange") {
                player.openGrandExchange()
            }
            onNpcOption(npc = clerk, option = "history") {
                player.message("Your offer history is not available yet.")
            }
            onNpcOption(npc = clerk, option = "sets") {
                player.openSets()
            }
            onNpcOption(npc = clerk, option = "talk-to") {
                player.queue { greet(player) }
            }
        }

        // The exchange booths. "Collect" leads to the collection box rather than the offer screen.
        val exchangeBooths = arrayOf("object.grand_exchange_booth_10061")
        exchangeBooths.forEach { booth ->
            onObjOption(obj = booth, option = "exchange") {
                player.openGrandExchange()
            }
            onObjOption(obj = booth, option = "collect") {
                player.openGrandExchangeCollectionBox()
            }
        }

        // The bank booths in the same ring. They carry "Bank" and "Collect" rather than "Exchange",
        // and are not in the generic bank booth list because their object name is a Grand Exchange
        // booth rather than a bank booth.
        val bankBooths = arrayOf("object.grand_exchange_booth")
        bankBooths.forEach { booth ->
            onObjOption(obj = booth, option = "bank") {
                player.openBank()
            }
            onObjOption(obj = booth, option = "collect") {
                player.openGrandExchangeCollectionBox()
            }
        }
    }

    private suspend fun QueueTask.greet(player: Player) {
        chatNpc(player, "Welcome to the Grand Exchange. How can I help you?")
        player.message(
            "Right-click a clerk or a booth to make an offer, or to collect one that has finished.",
            ChatMessageType.GAME_MESSAGE,
        )
    }
}
