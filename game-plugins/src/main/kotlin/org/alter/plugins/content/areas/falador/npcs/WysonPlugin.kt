package org.alter.plugins.content.areas.falador.npcs

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
import org.alter.rscm.RSCM.getRSCM

/**
 * Wyson the gardener, in Falador Park at (3027, 3379).
 *
 * His woad leaf haggle is fully working: 5 and 10 coins are refused outright, 15 coins buys
 * one leaf and 20 coins buys two, and each branch checks the player's coins first, exactly
 * as the transcript specifies. Both the coin and woad leaf item ids are real in this cache.
 *
 * The mole conversation is reproduced as dialogue only. Wyson's actual mole-parts-for-bird-
 * nests trade, and the holy moleys crafting branch, both need Giant Mole content that does
 * not exist here, so they are left out rather than faked - talking to him about the mole
 * works, handing him mole skin does not.
 */
class WysonPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.wyson_the_gardener", x = 3027, z = 3379, height = 0, walkRadius = 3, direction = Direction.SOUTH)

        onNpcOption("npc.wyson_the_gardener", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(
            player,
            "I'm the head gardener around here. If you're looking for<br>woad leaves, or if you need help with owt, I'm yer man.",
        )

        when (options(
            player,
            "Yes please, I need woad leaves.",
            "How about ME helping YOU instead?",
            "Sorry, but I'm not interested.",
        )) {
            1 -> {
                chatPlayer(player, "Yes please, I need woad leaves.")
                haggle(player)
            }

            2 -> {
                chatPlayer(player, "How about ME helping YOU instead?")
                mole(player)
            }

            3 -> {
                chatPlayer(player, "Sorry, but I'm not interested.")
                chatNpc(player, "Fair enough.")
            }
        }
    }

    private suspend fun QueueTask.haggle(player: Player) {
        chatNpc(player, "How much are you willing to pay?")

        when (options(player, "How about 5 coins?", "How about 10 coins?", "How about 15 coins?", "How about 20 coins?")) {
            1 -> {
                chatPlayer(player, "How about 5 coins?")
                tooLittle(player)
            }

            2 -> {
                chatPlayer(player, "How about 10 coins?")
                tooLittle(player)
            }

            3 -> {
                chatPlayer(player, "How about 15 coins?")
                chatNpc(player, "Mmmm... okay, that sounds fair.")
                buy(player, price = 15, leaves = 1)
            }

            4 -> {
                chatPlayer(player, "How about 20 coins?")
                chatNpc(player, "Okay, that's more than fair.")
                buy(player, price = 20, leaves = 2)
            }
        }
    }

    /** Wyson gives the same refusal for both the 5 and 10 coin offers. */
    private suspend fun QueueTask.tooLittle(player: Player) {
        chatNpc(
            player,
            "No no, that's far too little. Woad leaves are hard to get.<br>I used to have plenty but someone kept stealing them<br>off me.",
        )
    }

    private suspend fun QueueTask.buy(player: Player, price: Int, leaves: Int) {
        if (player.inventory.getItemCount(getRSCM("item.coins_995")) < price) {
            chatPlayer(player, "I don't have enough coins to buy the leaves. I'll come<br>back later.")
            return
        }

        player.inventory.remove("item.coins_995", price)
        player.inventory.add("item.woad_leaf", leaves)

        if (leaves > 1) {
            chatNpc(player, "Here, have two, you're a generous person.")
            chatPlayer(player, "Thanks.")
        } else {
            chatPlayer(player, "Thanks.")
            chatNpc(player, "I'll be around if you have any more gardening needs.")
        }
    }

    private suspend fun QueueTask.mole(player: Player) {
        chatNpc(
            player,
            "That's a nice thing to say. I do need a hand, now you<br>mention it. You see, there's some stupid mole digging up<br>my lovely garden.",
        )
        chatPlayer(player, "A mole? Surely you've dealt with moles in the past?")
        chatNpc(
            player,
            "Ah, well this is no ordinary mole! He's a big'un for sure.<br>Ya see... I'm always relied upon to make the most of this<br>'ere garden - the faster and bigger I can grow plants the<br>better!",
        )
        chatNpc(
            player,
            "In my quest for perfection I looked into 'Malignius-<br>Mortifer's-Super-Ultra-Flora-Growth-Potion'. It worked<br>well on my plants, no doubt about it! But it had the same<br>effect on a nearby mole. Ya can imagine the",
        )
        chatNpc(
            player,
            "havoc he causes to my patches of sunflowers! Why, if any<br>of the other gardeners knew about this mole, I'd be<br>looking for a new job in no time!",
        )
        chatPlayer(player, "I see. What do you need me to do?")
        chatNpc(
            player,
            "If ya are willing maybe yer wouldn't mind killing it for<br>me? Take a spade and use it to shake up them mole hills.<br>Be careful though, he really is big!",
        )
        chatPlayer(player, "Is there anything in this for me?")
        chatNpc(
            player,
            "Well, if yer gets any mole skin or mole claws off 'un, I'd<br>trade 'em for bird nests if ye brings 'em here to me.",
        )
        chatPlayer(player, "Right, I'll bear it in mind.")
    }
}
