package org.alter.plugins.content.areas.edgeville.npcs

import org.alter.api.Skills
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
 * Hari, the canoe tutor on the river bank east of Edgeville at (3132, 3509).
 *
 * Edgeville's counterpart to [org.alter.plugins.content.areas.barbarianvillage.npcs.SigurdPlugin],
 * and built the same way: his "teach me about canoes" branch is genuinely gated on
 * Woodcutting in the same five bands (1-11 / 12-26 / 27-41 / 42-56 / 57-99), Woodcutting
 * exists on this server, so the bands read the real skill rather than collapsing to one
 * answer. Base level, not boosted - canoe building is a base-level requirement in OSRS.
 *
 * His lines are not Sigurd's. Hari is the Wilderness-facing tutor, so every band ends by
 * talking about what the canoe can and cannot do on the northern river, and only the Waka
 * band mentions reaching the Wilderness pond at all.
 *
 * Not implemented: actually building or riding a canoe - there is no canoe travel system
 * here, so his advice is informational, exactly as Sigurd's is. His cache option is spelled
 * "Talk-To" rather than the usual "Talk-to"; `onNpcOption` lowercases both sides, so the
 * normal key still binds.
 */
class HariPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.hari", x = 3132, z = 3509, height = 0, walkRadius = 2, direction = Direction.SOUTH)

        onNpcOption("npc.hari", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatPlayer(player, "Hello there.")
        chatNpc(player, "Hello.")

        when (options(player, "Who are you?", "Can you teach me about Canoeing?")) {
            1 -> whoAreYou(player)
            2 -> teachCanoeing(player)
        }
    }

    private suspend fun QueueTask.whoAreYou(player: Player) {
        chatPlayer(player, "Who are you?")
        chatNpc(player, "My name is Hari.")
        chatPlayer(player, "And what are you doing here Hari?")
        chatNpc(
            player,
            "Like most people who come to Edgeville, I am here to<br>seek adventure in the Wilderness.",
        )
        chatNpc(player, "I found a secret underground river that will take me<br>quite a long way north.")
        chatPlayer(player, "Underground river? Where does it come out?")
        chatNpc(player, "It comes out in a pond located deep in the Wilderness.")
        chatNpc(
            player,
            "I had to find a very special type of canoe to get me up<br>the river though, would you like to know more?",
        )

        when (options(player, "Yes", "No")) {
            1 -> teachCanoeing(player)
            2 -> chatPlayer(player, "No thanks, not right now.")
        }
    }

    private suspend fun QueueTask.teachCanoeing(player: Player) {
        chatPlayer(player, "Could you teach me about canoes?")

        val woodcutting = player.getSkills().getBaseLevel(Skills.WOODCUTTING)

        if (woodcutting < 12) {
            chatNpc(player, "Well, you don't look like you have the skill to make a<br>canoe.")
            chatNpc(player, "You need to have at least level 12 woodcutting.")
            chatNpc(player, "Once you are able to make a canoe it makes travel<br>along the river much quicker!")
            return
        }

        chatNpc(player, "It's really quite simple. Just walk down to that tree on<br>the bank and chop it down.")
        chatNpc(player, "When you have done that you can shape the log further<br>with your axe to make a canoe.")

        when {
            woodcutting < 27 -> {
                chatNpc(
                    player,
                    "I can sense you're still a novice woodcutter, you will<br>only be able to make a log canoe at present.",
                )
                chatPlayer(player, "Is that good?")
                chatNpc(
                    player,
                    "A log will take you one stop along the river. But you<br>won't be able to travel into the Wilderness on it.",
                )
            }

            woodcutting < 42 -> {
                chatNpc(
                    player,
                    "You are an average woodcutter. You should be able to<br>make a Dugout canoe quite easily. It will take you 2<br>stops along the river.",
                )
                chatPlayer(player, "Can I take a dugout canoe to reach the Wilderness?")
                chatNpc(player, "You would never make it there alive.")
                chatPlayer(player, "Best not to try then.")
            }

            woodcutting < 57 -> {
                chatNpc(
                    player,
                    "You seem to be an accomplished woodcutter. You will<br>easily be able to make a Stable Dugout",
                )
                chatNpc(
                    player,
                    "They are reliable enough to get you anywhere on this<br>river, except to the Wilderness of course.",
                )
                chatNpc(player, "Only a Waka can take you there.")
                chatPlayer(player, "A Waka? What's that?")
                chatNpc(player, "Come and ask me when you have improved your skills as<br>a woodcutter.")
            }

            else -> {
                chatNpc(player, "Your skills rival mine friend. You will certainly be able<br>to build a Waka.")
                chatPlayer(player, "A Waka? What's that?")
                chatNpc(
                    player,
                    "A Waka is an invention of my people, it's an incredible<br>strong and fast canoe and will carry you safely to any<br>destination on the river.",
                )
                chatPlayer(player, "Any destination?")
                chatNpc(
                    player,
                    "Yes, you can take a waka north through the underground<br>portion of this river.",
                )
                chatNpc(
                    player,
                    "It will bring you out at a pond in the heart of the<br>Wilderness. Be careful up there, many have lost more<br>than their lives in that dark and twisted place.",
                )
            }
        }
    }
}
