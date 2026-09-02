package org.alter.plugins.content.areas.barbarianvillage.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.appearance.Gender
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Checkal, the bodybuilder in the south-east of Barbarian Village at (3087, 3415).
 *
 * Only the pre-quest branch is implemented; his post-Below Ice Mountain dialogue
 * needs a quest system that doesn't exist yet. Note his after-quest location is
 * Nardah, so this spawn is the correct one for a server with no quests.
 */
class CheckalPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.checkal", x = 3087, z = 3415, height = 0, walkRadius = 2, direction = Direction.WEST)

        onNpcOption("npc.checkal", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        val address = if (player.appearance.gender == Gender.FEMALE) "miss" else "sir"

        player.message("Checkal stares at you intensely.")
        chatNpc(player, "You best not be here to start any trouble, $address.")
        chatPlayer(player, "Wouldn't dream of it.")
    }
}
