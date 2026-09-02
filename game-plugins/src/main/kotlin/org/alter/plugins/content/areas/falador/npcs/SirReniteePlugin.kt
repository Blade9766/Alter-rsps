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
 * Sir Renitee, the castle's heraldry expert, at (2982, 3341) on plane 1.
 *
 * Only two of his three real options are offered here. The third, "Can you see if I have a
 * family crest?", drives the whole heraldry system - looking up the player's assigned crest,
 * then charging 5,000 coins to swap it for one of sixteen symbols, which then has to persist
 * on the player and render on heraldic kiteshields and helms. None of that exists on this
 * server, and there is no crest to report, so the option is omitted rather than wired to a
 * fabricated answer. Restore it alongside a real crest system.
 */
class SirReniteePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.sir_renitee", x = 2982, z = 3341, height = 1, walkRadius = 2, direction = Direction.SOUTH)

        onNpcOption("npc.sir_renitee", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Hmm? What's that, young one? What can I do for you?")

        when (options(player, "I don't know, what can you do for me?", "Nothing, thanks")) {
            1 -> {
                chatPlayer(player, "I don't know, what can you do for me?")
                chatNpc(
                    player,
                    "Hmm, well, mmm, do you have a family crest? I keep<br>track of every Gielinor family, you know, so I might be<br>able to find yours. I'm also something of an, mmm, a<br>painter. If you've met any important persons or visited",
                )
                chatNpc(player, "any nice places I could paint them for you.")
            }

            2 -> {
                chatPlayer(player, "Nothing thanks.")
                chatNpc(player, "Mmm, well, see you some other time maybe.")
            }
        }
    }
}
