package org.alter.plugins.content.areas.barbarianvillage.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.message
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
 * Litara, the explorer standing by the Barbarian Village mine at (3081, 3420).
 *
 * Standard dialogue only. Her two other branches both trigger off having spoken to
 * Solztun at the bottom of the Stronghold of Security - the Stronghold isn't built
 * on this server, so that state can never be reached and those branches would be
 * unreachable code.
 */
class LitaraPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.litara", x = 3081, z = 3420, height = 0, walkRadius = 3, direction = Direction.SOUTH)

        onNpcOption("npc.litara", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Hello there. You look lost, are you okay?")

        when (options(player, "I'm looking for a stronghold or something...", "I'm fine, just passing through.")) {
            1 -> {
                chatPlayer(player, "I'm looking for a stronghold or something...")
                chatNpc(player, "Ahh... the Stronghold of Security. It's down there.")
                player.message("Litara points at the hole in the ground that looks like you could squeeze through.")
                chatPlayer(player, "Looks kind of... deep and dark.")
                chatNpc(player, "Yeah... tell that to my brother, he still hasn't come<br>back.")
                chatPlayer(player, "Your brother?")
                chatNpc(
                    player,
                    "He's an explorer too. When the miner fell down that<br>hole he'd made and came back babbling about doors,<br>questions and treasure, my brother went to explore.<br>No-one has seen him since.",
                )
                chatPlayer(player, "Oh... that's not good.")
                chatNpc(
                    player,
                    "Lots of people have been down there, but none of them<br>have seen him. Let me know if you do, will you?",
                )
                chatPlayer(player, "I'll certainly keep my eyes open.")
            }

            2 -> chatPlayer(player, "I'm fine, just passing through.")
        }
    }
}
