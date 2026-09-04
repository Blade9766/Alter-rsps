package org.alter.plugins.content.areas.varrock.npcs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.getInteractingNpc
import org.alter.api.ext.options
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.mechanics.appearance.pronoun
import org.alter.rscm.RSCM.getRSCM

/**
 * The tramp of south-east Varrock (npc 3255), who begs a coin off passers-by and shouts about them
 * if they say no.
 *
 * **He wanders, and the wiki says so.** His `{{Map}}` row is `r=15` around 3241,3398 - a roaming
 * radius, not a spawn tile - so 3241,3398 itself is the middle of a table and blocked. He is placed
 * one tile west on 3240,3398 with a walk radius, which puts him in the same room the marker covers.
 *
 * Ids 381-383 are also called "Tramp" in this cache but carry no options at all; 3255 is the only
 * one with `Talk-to`, and it is the id the wiki's page gives. [org.alter.plugins.content.areas
 * .varrock.spawns.SpawnPlugin] makes the same distinction for Varrock's guards.
 *
 * The overhead insult is the one piece of him that needs the player's pronoun - the wiki writes it
 * as "[he's/she's/they're] stingy" - so it comes off [pronoun] like the Blue Moon barbarian's
 * address does.
 */
class TrampPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = TRAMP, x = 3240, z = 3398, walkRadius = 6, direction = Direction.SOUTH)

        onNpcOption(TRAMP, option = "talk-to", lineOfSightDistance = 4) { player.queue { dialog(player) } }
    }

    private suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Got any spare change, mate?")

        if (player.inventory.getItemCount(getRSCM("item.coins_995")) < ALMS) {
            chatPlayer(player, "I haven't got any.")
            chatNpc(player, "Yeah, right, they all say that.")
            player.grumbleAbout()
            return
        }

        when (options(
            player,
            "Yes, I can spare a little money.",
            "Sorry, you'll have to earn it yourself, just like I did.",
            title = "What would you like to say?",
        )) {
            1 -> {
                chatPlayer(player, "Yes, I can spare a little money.")
                player.inventory.remove("item.coins_995", ALMS)
                chatNpc(player, "Thanks, mate!")
            }

            2 -> {
                chatPlayer(player, "Sorry, you'll have to earn it yourself, just like I did.")
                chatNpc(player, "Please yourself.")
                player.grumbleAbout()
            }
        }
    }

    /** One of the three things he shouts after being refused, picked at random. */
    private fun Player.grumbleAbout() {
        val contraction = "${pronoun.subject}${if (pronoun.subject == "they") "'re" else "'s"}"
        val insults = listOf(
            "I hate $username!",
            "$username is mean!",
            "Don't ask $username for anything - $contraction stingy!",
        )
        getInteractingNpc().forceChat(insults[world.random(insults.size - 1)])
    }

    private companion object {
        const val TRAMP = "npc.tramp_3255"

        /** The wiki's transcript: "Player gives the tramp 1 coin." */
        const val ALMS = 1
    }
}
