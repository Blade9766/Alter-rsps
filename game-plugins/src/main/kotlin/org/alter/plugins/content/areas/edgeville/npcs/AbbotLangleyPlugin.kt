package org.alter.plugins.content.areas.edgeville.npcs

import org.alter.api.Skills
import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.heal
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
 * Abbot Langley, head of the Edgeville Monastery, on the ground floor at (3052, 3490).
 *
 * **His healing actually works.** "Can you heal me? I'm injured." restores the player to
 * full hitpoints, which is the real mechanic and the main reason anyone talks to him -
 * `heal()` caps at the base level by default, so a boosted player is not clipped and an
 * unhurt one simply gains nothing.
 *
 * **His Prayer gate is real too.** Joining the order needs Prayer 31, and Prayer exists on
 * this server, so the branch reads the player's actual base level rather than flattening to
 * one answer - the same call [org.alter.plugins.content.areas.barbarianvillage.npcs.SigurdPlugin]
 * makes for Woodcutting. Note that passing the check only changes what he says: the second
 * floor is reached through a door this plugin does not wire, so the monastery's upper level
 * is not actually gated by anything yet.
 *
 * Left out: his spirit shield sigil-removal branch, which needs Nightmare-tier items that
 * this server has no content for.
 */
class AbbotLangleyPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.abbot_langley", x = 3052, z = 3490, height = 0, walkRadius = 3, direction = Direction.SOUTH)

        onNpcOption("npc.abbot_langley", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Greetings traveller.")

        when (options(
            player,
            "Can you heal me? I'm injured.",
            "Isn't this place built a bit out of the way?",
            "How do I get further into the monastery?",
        )) {
            1 -> {
                chatPlayer(player, "Can you heal me? I'm injured.")
                chatNpc(player, "Ok.")
                heal(player)
            }

            2 -> {
                chatPlayer(player, "Isn't this place built a bit out of the way?")
                chatNpc(
                    player,
                    "We like it that way actually! We get disturbed less. We<br>still get rather a large amount of travellers looking for<br>sanctuary and healing here as it is!",
                )
            }

            3 -> {
                chatPlayer(player, "How do I get further into the monastery?")
                chatNpc(player, "I'm sorry but only members of our order are allowed in<br>the second level of the monastery.")

                when (options(player, "Well can I join your order?", "Oh, sorry.")) {
                    1 -> {
                        chatPlayer(player, "Well can I join your order?")
                        if (player.getSkills().getBaseLevel(Skills.PRAYER) >= PRAYER_REQUIREMENT) {
                            chatNpc(player, "Ok, I see you are someone suitable for our order. You<br>may join.")
                        } else {
                            chatNpc(player, "No. I am sorry, but I feel you are not devout enough.")
                        }
                    }

                    2 -> chatPlayer(player, "Oh, sorry.")
                }
            }
        }
    }

    private fun heal(player: Player) {
        val skills = player.getSkills()
        val missing = skills.getBaseLevel(Skills.HITPOINTS) - skills.getCurrentLevel(Skills.HITPOINTS)
        if (missing > 0) {
            player.heal(missing)
        }
        player.message("Abbot Langley places his hands on your head. You feel a little better.")
    }

    private companion object {
        const val PRAYER_REQUIREMENT = 31
    }
}
