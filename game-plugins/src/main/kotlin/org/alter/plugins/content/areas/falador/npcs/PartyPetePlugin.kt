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
 * Party Pete, host of the Falador Party Room, at (3053, 3374).
 *
 * All five of his standard dialogue options, verbatim. What he describes - the balloon-drop
 * lever, the party drop chest, the Party Room Knights - is not implemented; the Party Room
 * itself needs its own chest container, lever object and balloon spawning, which is a slice
 * of its own. Talking to him works and explains the room; pulling the lever does nothing yet.
 *
 * His "I wanna party!" branch ends with him performing a dance emote in game. That is left
 * as dialogue here, since the specific emote animation is not published anywhere this could
 * be verified against, and guessing an animation id would be inventing content.
 */
class PartyPetePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.party_pete", x = 3053, z = 3374, height = 0, walkRadius = 3, direction = Direction.SOUTH)

        onNpcOption("npc.party_pete", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Hi! I'm Party Pete. Welcome to the Party Room!")

        when (options(
            player,
            "So what's this room for?",
            "What's the big lever over there for?",
            "What's the gold chest for?",
            "Have you always been here in Falador?",
            "I wanna party!",
        )) {
            1 -> {
                chatPlayer(player, "So what's this room for?")
                chatNpc(player, "This room is for partying the night away!")
                chatPlayer(player, "How do you have a party in Gielinor?")
                chatNpc(player, "Get a few mates round, get the beers in and have fun!")
                chatNpc(player, "Some players organise parties so keep an eye open!")
                chatPlayer(player, "Woop! Thanks Pete!")
            }

            2 -> {
                chatPlayer(player, "What's the big lever over there for?")
                chatNpc(player, "Simple. With the lever you can do some fun stuff.")
                chatPlayer(player, "What kind of stuff?")
                chatNpc(
                    player,
                    "A balloon drop costs 1000 gold. For this you get 200<br>balloons dropped across the whole of the party room.<br>You can then have fun popping the balloons! If there are<br>items in the Party Drop Chest they will be inside the",
                )
                chatNpc(
                    player,
                    "balloons! For 500 gold you can summon the Party Room<br>Knights who will dance for your delight.",
                )
                chatNpc(player, "Their singing isn't a delight though!")
            }

            3 -> {
                chatPlayer(player, "What's the gold chest for?")
                chatNpc(
                    player,
                    "Any items that are in the chest will be dropped inside the<br>balloons when you pull the lever!",
                )
                chatPlayer(player, "Cool! Sounds like a fun way to do a drop party!")
                chatNpc(player, "Exactly!")
                chatNpc(
                    player,
                    "A word of warning though. Any items that you put into<br>the chest can't be taken out again and it costs 1000 gold<br>pieces for each balloon drop.",
                )
            }

            4 -> {
                chatPlayer(player, "Have you always been here in Falador?")
                chatNpc(
                    player,
                    "We used to be in Seers' Village, far to the west, but we<br>had to move - the seers were complaining about the noise<br>level, and the knights of Camelot got it into their heads<br>that the Party Room knights were making fun of them.",
                )
                chatNpc(
                    player,
                    "We're doing well here, though. The people of Falador are<br>happy we're here, and we've hardly ever had the White<br>Knights telling us to keep the noise down.",
                )
                chatNpc(player, "We're going to turn Falador into the party capital of<br>Gielinor!")
            }

            5 -> {
                chatPlayer(player, "I wanna party!")
                chatNpc(player, "I've won the Dance Trophy at the Kandarin Ball three<br>years in a trot!")
                chatPlayer(player, "Show me your moves Pete!")
            }
        }
    }
}
