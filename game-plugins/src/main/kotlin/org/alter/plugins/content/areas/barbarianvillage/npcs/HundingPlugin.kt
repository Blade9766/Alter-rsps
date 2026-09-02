package org.alter.plugins.content.areas.barbarianvillage.npcs

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
 * Hunding, the tribe's historian, at the top of the Barbarian Village lookout tower -
 * (3097, 3429) on plane 2, per his wiki map pin.
 *
 * Full standard dialogue, including the long tribal-history speech, which the wiki
 * splits across seven separate chat lines; those breaks are reproduced here rather
 * than merged, since each one is its own click-to-continue box in game.
 *
 * Not implemented: his beginner clue-scroll branch, which needs a clue/casket system
 * this server doesn't have.
 */
class HundingPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.hunding", x = 3097, z = 3429, height = 2, walkRadius = 2, direction = Direction.SOUTH)

        onNpcOption("npc.hunding", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatPlayer(player, "Hello.")
        chatNpc(player, "What are you doing in our village, outlander?")

        when (options(player, "Nothing much.", "I'm exploring.", "I came to kill you all!")) {
            1 -> {
                chatPlayer(player, "Nothing much.")
                longhall(player)
            }

            2 -> {
                chatPlayer(player, "I'm exploring.")
                chatNpc(
                    player,
                    "Bah! You cannot hope to learn anything of us just by<br>strolling through our village! We are an ancient tribe,<br>our ways date back to the time before Avarrocka was<br>founded!",
                )

                when (options(
                    player,
                    "Would you care to tell me more?",
                    "You look like a load of primitive savages.",
                    "I'm bored.",
                )) {
                    1 -> tribalHistory(player)

                    2 -> {
                        chatPlayer(player, "You look like a load of primitive savages.")
                        chatNpc(player, "And you look like an arrogant fool. And you smell like<br>a raccoon's bottom.")
                    }

                    3 -> {
                        chatPlayer(player, "I'm bored.")
                        longhall(player)
                    }
                }
            }

            3 -> {
                chatPlayer(player, "I came to kill you all!")
                chatNpc(player, "Ho ho! Brave words indeed from an outerlander! Go<br>down to the longhall and try it!")
            }
        }
    }

    /** Hunding's stock answer to both "Nothing much." and "I'm bored." */
    private suspend fun QueueTask.longhall(player: Player) {
        chatNpc(
            player,
            "Bah! Go down to the longhall, and there you will find<br>excitement aplenty! Our finest warrior, Gunthor the<br>Brave, will give you a rousing welcome!",
        )
        chatPlayer(player, "I'll bear it in mind.")
    }

    private suspend fun QueueTask.tribalHistory(player: Player) {
        chatPlayer(player, "Would you care to tell me more?")
        chatNpc(
            player,
            "Our elders remember that about a century ago we were<br>living in the lands far to the west. We were a large<br>nomadic mountain tribe, settling wherever there was<br>food, moving on when it had run out. As the tribe",
        )
        chatNpc(
            player,
            "grew larger, it was hard to find enough food for<br>everyone, and we were forced to shift our camp more<br>and more often. In time, a warrior called Gunnar took<br>his friends and their families and left the larger tribe,",
        )
        chatNpc(
            player,
            "moving south in search of new places. They eventually<br>settled here and built this village, finding that the old<br>nomadic traditions were no longer needed.",
        )
        chatNpc(player, "Our current chieftain, Gunthor the Brave, is a direct-<br>line descendent of Gunnar.")
        chatNpc(
            player,
            "However, our ways have changed little in the last<br>century. Although more and more people use magical<br>powers, we do not believe it is wise to take upon<br>oneself the power of the gods in this way. To this day, we fight",
        )
        chatNpc(
            player,
            "with the mighty sword, the vicious axe and the swift<br>arrow on the wind. Some of our young, wishing to try<br>so-called 'civilisation' and the softness of city life,<br>abandon the tribe and move to the cities. It is sad to",
        )
        chatNpc(
            player,
            "see them go, but we do not prevent them; we live here<br>in our village because we love this life - we do not<br>force it on those whom it does not suit.",
        )
        chatNpc(player, "There, I have said enough.")
        chatPlayer(player, "Thank you.")
    }
}
