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
 * The two barmaids of the Rising Sun Inn, north-west Falador: Emily at (2956, 3372) and
 * Kaylee at (2958, 3371).
 *
 * They share one plugin because they share one transcript - the wiki's Emily and Kaylee
 * standard dialogues are word-for-word identical, down to the greeting and the refusal
 * line, so duplicating it into two files would be copying the same text twice.
 *
 * Unlike Lumbridge's bartender, this checks the player actually has three coins rather than
 * merely holding a coin stack, so a player with one coin gets Kaylee's real refusal instead
 * of a free drink.
 *
 * Emily has two extra branches not reproduced here: buying empty beer glasses back at 2
 * coins each, and gossip about valuable items sitting in the party room drop chest. Both
 * depend on the Party Room, which is not built - the chest branch has nothing to read, and
 * the glass buy-back is left out rather than half-wired.
 */
class RisingSunInnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    private val barmaids = listOf("npc.emily", "npc.kaylee")

    init {
        spawnNpc(npc = "npc.emily", x = 2956, z = 3372, height = 0, walkRadius = 2, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.kaylee", x = 2958, z = 3371, height = 0, walkRadius = 2, direction = Direction.WEST)

        barmaids.forEach { barmaid ->
            onNpcOption(barmaid, option = "talk-to", lineOfSightDistance = 4) {
                player.queue { dialog(player) }
            }
        }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Heya! What can I get you?")

        when (options(
            player,
            "What ales are you serving?",
            "One Asgarnian Ale, please.",
            "I'll try the Mind Bomb.",
            "Can I have a Dwarven Stout?",
            "I don't feel like any of those.",
        )) {
            1 -> {
                chatPlayer(player, "What ales are you serving?")
                chatNpc(player, "Well, we've got Asgarnian Ale, Wizard's Mind Bomb and<br>Dwarven Stout, all for only 3 coins.")
                order(player)
            }

            2 -> buy(player, "One Asgarnian Ale, please.", "item.asgarnian_ale")
            3 -> buy(player, "I'll try the Mind Bomb.", "item.wizards_mind_bomb")
            4 -> buy(player, "Can I have a Dwarven Stout?", "item.dwarven_stout")
            5 -> chatPlayer(player, "I don't feel like any of those.")
        }
    }

    /** The same four purchase options again, after she has listed what is on tap. */
    private suspend fun QueueTask.order(player: Player) {
        when (options(
            player,
            "One Asgarnian Ale, please.",
            "I'll try the Mind Bomb.",
            "Can I have a Dwarven Stout?",
            "I don't feel like any of those.",
        )) {
            1 -> buy(player, "One Asgarnian Ale, please.", "item.asgarnian_ale")
            2 -> buy(player, "I'll try the Mind Bomb.", "item.wizards_mind_bomb")
            3 -> buy(player, "Can I have a Dwarven Stout?", "item.dwarven_stout")
            4 -> chatPlayer(player, "I don't feel like any of those.")
        }
    }

    private suspend fun QueueTask.buy(player: Player, request: String, drink: String) {
        chatPlayer(player, request)

        if (player.inventory.getItemCount(getRSCM("item.coins_995")) < ALE_PRICE) {
            chatNpc(player, "I said 3 coins! You haven't got 3 coins!")
            return
        }

        player.inventory.remove("item.coins_995", ALE_PRICE)
        player.inventory.add(drink, 1)
        chatPlayer(player, "Thanks.")
    }

    private companion object {
        const val ALE_PRICE = 3
    }
}
