package org.alter.plugins.content.areas.varrock.npcs

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
import org.alter.rscm.RSCM.getRSCM

/**
 * Aris (npc 11868), the fortune-teller in the tent on the west side of Varrock Square.
 *
 * **Tile.** Her own infobox pin, 3204,3425, is the crystal ball - a 2x2 loc covering 3203-3204 by
 * 3425-3426, and blocked. She goes on 3203,3424 instead, which is the tile the Demon Slayer article
 * publishes as `startmap` and is the free tile directly in front of the ball, inside the tent.
 *
 * **What she says.** Demon Slayer is not built, so nothing here starts, advances or checks a quest.
 * What is reproduced is her scrying: she takes one coin, tells the player what she sees, and answers
 * the four questions about it that need no quest state. Her closing line still plays, but it starts
 * nothing.
 *
 * One branch is deliberately absent: **the incantation.** Its five words are shuffled per player and
 * have to be remembered and said back to Delrith at the stone circle. Without the quest there is
 * nowhere to store the shuffle and nothing to say it to, so handing out five words would be handing
 * out a prop. Her post-quest and Treasure Trails dialogue is likewise out of reach and left out.
 *
 * The rest is flavour that reads correctly on its own - which is what her tent is for.
 */
class ArisPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = ARIS, x = 3203, z = 3424, walkRadius = 0, direction = Direction.NORTH)

        onNpcOption(ARIS, option = "talk-to", lineOfSightDistance = 4) { player.queue { dialog(player) } }
    }

    private suspend fun QueueTask.dialog(player: Player) {
        chatNpc(player, "Hello young one. Cross my palm with silver and the future<br>will be revealed to you.")

        if (player.inventory.getItemCount(getRSCM("item.coins_995")) < FORTUNE_PRICE) {
            chatPlayer(player, "Oh dear. I don't have any money.")
            return
        }

        when (options(player, "Yes.", "No.")) {
            1 -> {
                chatPlayer(player, "Okay, here you go.")
                player.inventory.remove("item.coins_995", FORTUNE_PRICE)
                scry(player)
            }

            2 -> {
                chatPlayer(player, "No, I don't believe in that stuff.")
                chatNpc(player, "Ok suit yourself.")
            }
        }
    }

    private suspend fun QueueTask.scry(player: Player) {
        chatNpc(player, "Come closer, and listen carefully to what the future holds<br>for you, as I peer into the swirling mists of the crystal ball.")
        chatNpc(player, "I can see images forming. I can see you.")
        chatNpc(player, "You are holding a very impressive looking sword. I'm sure I<br>recognise that sword...")
        chatNpc(player, "There is a big dark shadow appearing now.")
        chatNpc(player, "Aaargh!")
        chatPlayer(player, "Are you all right?")
        chatNpc(player, "It's Delrith! Delrith is coming!")
        chatPlayer(player, "Who's Delrith?")
        chatNpc(player, "Delrith...")
        chatNpc(player, "Delrith is a powerful demon.")
        chatNpc(player, "Oh! I really hope he didn't see me looking at him through<br>my crystal ball!")
        chatNpc(player, "He tried to destroy this city 150 years ago. He was stopped<br>just in time by the great hero Wally.")
        chatNpc(player, "Using his magic sword Silverlight, Wally managed to trap<br>the demon in the stone circle just south of this city.")
        chatNpc(player, "Ye gods! Silverlight was the sword you were holding in my<br>vision! You are the one destined to stop the demon this<br>time.")

        questions(player)
    }

    /**
     * Her follow-up questions. Every answer ends in the transcript's `{{tact|other}}` - back to the
     * same menu - so this is a loop, and the only way out is the last option. In the real game that
     * option is what starts Demon Slayer; here it only ends the conversation, which is why she still
     * wishes the player luck but nothing is recorded.
     */
    private suspend fun QueueTask.questions(player: Player) {
        while (true) {
            when (options(
                player,
                "How am I meant to fight a demon who can destroy cities?",
                "Okay, where is he? I'll kill him for you!",
                "Wally doesn't sound like a very heroic name.",
                "Where can I find Silverlight?",
                "Okay, thanks. I'll do my best to stop the demon.",
                title = "What would you like to say?",
            )) {
                1 -> {
                    chatPlayer(player, "How am I meant to fight a demon who can destroy cities?!")
                    chatNpc(player, "If you face Delrith while he is still weak from being<br>summoned, and use the correct weapon, you will not find<br>the task too arduous.")
                    chatNpc(player, "Do not fear. If you follow the path of the great hero Wally,<br>then you are sure to defeat the demon.")
                }

                2 -> {
                    chatPlayer(player, "Okay, where is he? I'll kill him for you!")
                    chatNpc(player, "Ah, the overconfidence of the young!")
                    chatNpc(player, "Delrith can't be harmed by ordinary weapons. You must<br>face him using the same weapon that Wally used.")
                }

                3 -> {
                    chatPlayer(player, "Wally doesn't sound a very heroic name.")
                    chatNpc(player, "Yes I know. Maybe that is why history doesn't remember<br>him. However he was a very great hero.")
                    chatNpc(player, "Who knows how much pain and suffering Delrith would<br>have brought forth without Wally to stop him!")
                    chatNpc(player, "It looks like you are going to need to perform similar<br>heroics.")
                }

                4 -> {
                    chatPlayer(player, "Where can I find Silverlight?")
                    chatNpc(player, "Silverlight has been passed down through Wally's<br>descendants. I believe it is currently in the care of one of<br>the King's knights called Sir Prysin.")
                    chatNpc(player, "He shouldn't be too hard to find. He lives in the royal<br>palace in this city. Tell him Aris sent you.")
                }

                5 -> {
                    chatPlayer(player, "Ok thanks. I'll do my best to stop the demon.")
                    chatNpc(player, "Good luck, and may Guthix be with you!")
                    return
                }
            }
        }
    }

    private companion object {
        const val ARIS = "npc.aris"

        /** Demon Slayer's own requirements line: "1 gold coin". */
        const val FORTUNE_PRICE = 1
    }
}
