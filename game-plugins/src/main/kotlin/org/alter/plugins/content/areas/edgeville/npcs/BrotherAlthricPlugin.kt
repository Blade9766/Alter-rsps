package org.alter.plugins.content.areas.edgeville.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Brother Althric, tending the rose garden north of the Edgeville Monastery at (3052, 3505).
 *
 * His entire standard dialogue really is these two lines - a linear exchange with no
 * options, reproduced in full. His only other content is a Sins of the Father quest branch,
 * left out along with every other quest branch in this area.
 */
class BrotherAlthricPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.brother_althric", x = 3052, z = 3505, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        onNpcOption("npc.brother_althric", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatPlayer(player, "Very nice rosebushes you have here.")
        chatNpc(
            player,
            "Yes, it has taken me many long hours in this garden to<br>bring them to this state of near-perfection.",
        )
    }
}
