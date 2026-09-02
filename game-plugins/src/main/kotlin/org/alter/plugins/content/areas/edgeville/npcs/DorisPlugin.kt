package org.alter.plugins.content.areas.edgeville.npcs

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
 * Doris, Evil Dave's mother, in the house south of Edgeville bank at (3079, 3492).
 *
 * Her Wilderness answer is the best writing in Edgeville and is reproduced in full, right
 * down to the joke about not having an Attack option any more.
 *
 * Two branches are left out: her hellcat cure (needs the Evil Dave / hell-rat content and a
 * cat system, neither of which exists here) and the "How did Dave come to be evil?" answer,
 * whose middle lines the wiki transcript only paraphrases rather than quoting, so there is
 * no verbatim text to reproduce and inventing it would be writing dialogue rather than
 * sourcing it.
 */
class DorisPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.doris", x = 3079, z = 3492, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        onNpcOption("npc.doris", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Hello again dearie. How are you doing?")

        when (options(
            player,
            "Pretty good!",
            "Not too good actually!",
            "What's it like living so close to the wilderness?",
        )) {
            1 -> {
                chatPlayer(player, "Pretty good!")
                chatNpc(player, "That's good to hear.")
            }

            2 -> {
                chatPlayer(player, "Not too good actually!")
                chatNpc(player, "Oh well.")
            }

            3 -> {
                chatPlayer(player, "What's it like living so close to the wilderness?")
                chatNpc(
                    player,
                    "Oh, it's not all that bad. It was a bit scary at first but<br>as long as I don't go past the warning signs I'm all right.",
                )
                chatNpc(
                    player,
                    "It's actually pretty quiet here. I used to live in<br>Lumbridge and, let me tell you, that's the dangerous<br>place for people like me.",
                )
                chatNpc(
                    player,
                    "Thieves were picking people's pockets with impunity,<br>and killing them on the streets in broad daylight!",
                )
                chatNpc(
                    player,
                    "And there weren't even any guards to protect us! Not<br>that the guards in the other cities do much good to<br>protect people from what I've heard.",
                )
                chatNpc(
                    player,
                    "I just thank goodness I haven't got an Attack option any<br>more! I wouldn't last five minutes!",
                )
            }
        }
    }
}
