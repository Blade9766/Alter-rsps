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
 * Brother Jered, on the monastery's first floor at (3052, 3491), plane 1 - the level Abbot
 * Langley says is members-of-the-order only.
 *
 * Dialogue only. He describes two services this server cannot yet provide, and both are
 * left as description rather than wired to anything:
 * - **Blessing a silver symbol** into a holy symbol needs Crafting (making the unstrung
 *   symbol) to be worth anything, and Crafting is not built.
 * - **The Prayer skillcape** needs a skillcape system, which does not exist.
 *
 * Saying so in his own words is still worth having: it is what he actually says in game,
 * and it points the player at content rather than pretending it works.
 */
class BrotherJeredPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.brother_jered", x = 3052, z = 3491, height = 1, walkRadius = 3, direction = Direction.SOUTH)

        onNpcOption("npc.brother_jered", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatPlayer(player, "What can you do to help a bold adventurer like myself?")
        chatNpc(player, "I can tell you about holy symbols or the Skillcape of<br>Prayer.")

        when (options(
            player,
            "Tell me about holy symbols.",
            "Tell me about the Skillcape of Prayer.",
            "Praise be to Saradomin!",
        )) {
            1 -> {
                chatPlayer(player, "Tell me about holy symbols.")
                chatNpc(
                    player,
                    "If you have a silver star, which is the holy symbol of<br>Saradomin, then I can bless it.",
                )
            }

            2 -> {
                chatPlayer(player, "Tell me about the Skillcape of Prayer.")
                chatNpc(
                    player,
                    "The Skillcape of Prayer is the hardest of all the<br>skillcapes to get; it requires much devotion to acquire<br>but also imbues the wearer with the ability to briefly<br>fly!",
                )
                chatNpc(player, "The Cape increases Prayer points from potions when<br>equipped.")
            }

            3 -> {
                chatPlayer(player, "Praise be to Saradomin!")
                chatNpc(player, "Yes! Praise he who brings life to this world.")
            }
        }
    }
}
