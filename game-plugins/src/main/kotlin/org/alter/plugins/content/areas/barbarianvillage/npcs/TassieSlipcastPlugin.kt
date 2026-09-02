package org.alter.plugins.content.areas.barbarianvillage.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Tassie Slipcast, the potter in her studio in the south of Barbarian Village at
 * (3085, 3409).
 *
 * Her dialogue points at the potter's wheel and pottery oven she stands beside.
 * Those objects are part of the base map data and are already there in-game, but
 * nothing is wired to them: Crafting doesn't exist on this server yet, so the
 * wheel/oven are currently scenery and her advice is flavour only.
 */
class TassieSlipcastPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.tassie_slipcast", x = 3085, z = 3409, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        onNpcOption("npc.tassie_slipcast", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Please feel free to use the pottery wheel, I won't be<br>using it all the time. Put your pots in the kiln when<br>you've made one.")
        chatNpc(player, "And make sure you tidy up after yourself!")
    }
}
