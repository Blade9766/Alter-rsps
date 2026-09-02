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
 * Sir Amik Varze, regent of Asgarnia and leader of the White Knights, on the top floor of
 * the White Knights' Castle - (2960, 3337) on plane 2, per his wiki map pin.
 *
 * Standard dialogue only. His quest branches (Black Knights' Fortress, Recruitment Drive,
 * Recipe for Disaster, Dragon Slayer II) are all left out rather than stubbed, since this
 * server has no quest framework yet.
 *
 * Note that the honour system he explains is described but not implemented - see
 * [SirVyvinPlugin] for why the rank-gated armoury it feeds is also absent.
 */
class SirAmikVarzePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.sir_amik_varze", x = 2960, z = 3337, height = 2, walkRadius = 2, direction = Direction.SOUTH)

        onNpcOption("npc.sir_amik_varze", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatPlayer(player, "Hello Sir Amik.")
        chatNpc(player, "Hello, friend!")

        when (options(player, "Can you explain the White Knight honour system again?", "Okay, bye!")) {
            1 -> {
                chatPlayer(player, "Can you explain the White Knight honour system again?")
                chatNpc(
                    player,
                    "Sadly we are not as rich as we once were, and there are<br>many White Knights who foolishly lose their combat<br>equipment.",
                )
                chatNpc(
                    player,
                    "We do not think it fair to make a profit from our<br>brethren, so we will sell you equipment at cost, and<br>rebuy it at the same cost.",
                )
                chatNpc(
                    player,
                    "By killing Black Knights, you will increase your<br>reputation with us, by killing White Knights we will<br>obviously think less of you.",
                )
                chatNpc(
                    player,
                    "You can check your White Knight reputation level by<br>looking at your quest journal for the Wanted! Quest, or<br>Sir Vyvin will let you know what level you are at.",
                )
                chatNpc(
                    player,
                    "Sir Vyvin can be found in Falador Castle, and he will<br>sell you any equipment appropriate to your reputation<br>level.",
                )
                chatNpc(player, "Have fun, and go kill some Black Knights for me!")
                chatPlayer(player, "Okay Amik, thanks for explaining!")
            }

            2 -> chatPlayer(player, "Okay, 'bye then Amik!")
        }
    }
}
