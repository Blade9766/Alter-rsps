package org.alter.plugins.content.areas.barbarianvillage.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
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
 * Atlas, the strongman training north-west of the Barbarian Village longhall at
 * (3075, 3439).
 *
 * Only his standard dialogue is implemented. His other branches (the paid 25,000gp
 * strength workouts) all sit behind Below Ice Mountain, and there's no quest system
 * yet, so wiring them would mean inventing a completion state that can never be set -
 * the same call already made for the Varrock palace NPCs.
 */
class AtlasPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.atlas", x = 3075, z = 3439, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        onNpcOption("npc.atlas", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        val address = if (player.appearance.gender == Gender.FEMALE) "woman" else "man"

        chatNpc(player, "What do you want, little $address?")
        chatPlayer(player, "Nothing. Just thought I'd try to strike up a conversation.")
        chatNpc(player, "Not now little one, I'm busy.")
        chatPlayer(player, "Okay then.")
    }
}
