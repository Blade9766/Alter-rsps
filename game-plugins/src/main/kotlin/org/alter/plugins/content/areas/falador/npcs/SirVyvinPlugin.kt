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

/**
 * Sir Vyvin, in the White Knights' Castle at (2984, 3339) on plane 2.
 *
 * The trade branch answers "No, I'm sorry." - which is not a stub, it is the real
 * transcript line for a player with no White Knight rank. His armoury is gated behind the
 * Wanted! quest's White Knight reputation ranks (Novice / Peon / upwards), and this server
 * has neither the quest framework nor a kill-count reputation system, so every player here
 * is genuinely unranked and this is the correct in-game response for all of them. The
 * rank-gated stock is deliberately not wired: adding a shop no rank system can ever open
 * would be dead code.
 *
 * His Wanted! quest branches are likewise left out.
 */
class SirVyvinPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.sir_vyvin", x = 2984, z = 3339, height = 2, walkRadius = 3, direction = Direction.SOUTH)

        onNpcOption("npc.sir_vyvin", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatPlayer(player, "Hello.")
        chatNpc(player, "Greetings traveller.")

        when (options(
            player,
            "Do you have anything to trade?",
            "Why are there so many knights in this city?",
            "Can I just distract you for a minute?",
        )) {
            1 -> {
                chatPlayer(player, "Do you have anything to trade?")
                chatNpc(player, "No, I'm sorry.")
            }

            2 -> {
                chatPlayer(player, "Why are there so many knights in this city?")
                chatNpc(
                    player,
                    "We are the White Knights of Falador. We are the most<br>powerful order of knights in the land. We are helping<br>King Vallance rule the kingdom as he is getting old and<br>tired.",
                )
            }

            3 -> {
                chatPlayer(player, "Can I just distract you for a minute?")
                chatNpc(player, "... ...what? I'm... not sure what you're asking me... you<br>want to join the White Knights?")
                chatPlayer(player, "Nope. I'm just trying to distract you.")
                chatNpc(player, "... ...you are very odd.")
            }
        }
    }
}
