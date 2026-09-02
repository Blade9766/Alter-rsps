package org.alter.plugins.content.areas.falador.npcs

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
 * Sir Tiffy Cashien, on his park bench in Falador Park at (2997, 3373).
 *
 * His standard dialogue is a short linear exchange with no options, which is exactly what
 * is reproduced here. The Recruitment Drive / Temple Knight recruitment branch that makes
 * him interesting needs the quest framework, so it is left out.
 *
 * His greeting is gendered in game ("What ho, sir." / "What ho, milady."); this uses the
 * neutral third variant the transcript also lists, since it is a real in-game line rather
 * than a guess at how this server exposes appearance gender.
 */
class SirTiffyCashienPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.sir_tiffy_cashien", x = 2997, z = 3373, height = 0, walkRadius = 0, direction = Direction.SOUTH)

        onNpcOption("npc.sir_tiffy_cashien", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatPlayer(player, "Hello.")
        chatNpc(player, "What ho. Spiffing day for a walk in the park, what?")
        chatPlayer(player, "...spiffing?")
        chatNpc(player, "Absolutely, top-hole! Well, can't stay and chat all day,<br>dontchaknow! Ta-ta for now!")
        chatPlayer(player, "Erm... goodbye.")
    }
}
