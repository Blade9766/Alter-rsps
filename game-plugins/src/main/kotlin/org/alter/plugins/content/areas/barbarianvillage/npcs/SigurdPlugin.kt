package org.alter.plugins.content.areas.barbarianvillage.npcs

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
 * Sigurd, the canoe tutor on the river bank east of Barbarian Village at (3112, 3409).
 *
 * His "Can you teach me about canoes?" branch is really gated on the player's
 * Woodcutting level in five bands, and Woodcutting *does* exist on this server (see
 * `content/skills/woodcutting`), so the bands are wired to the real skill rather than
 * flattened to one generic answer. Base level is used, not boosted - canoe building
 * in OSRS is a base-level requirement.
 *
 * Not implemented: actually building or riding a canoe. There is no canoe travel
 * system here, so his advice is currently informational. Cache option is spelled
 * "Talk-To" on this npc rather than the usual "Talk-to"; `onNpcOption` lowercases
 * both sides before matching, so the normal key still binds.
 */
class SigurdPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = "npc.sigurd", x = 3112, z = 3409, height = 0, walkRadius = 2, direction = Direction.WEST)

        onNpcOption("npc.sigurd", option = "talk-to") { player.queue { dialog(player) } }
    }

    suspend fun QueueTask.dialog(player: Player) {
        chatPlayer(player, "Hello there.")
        chatNpc(player, "Ha Ha! Hello!")

        when (options(player, "Who are you?", "Can you teach me about canoeing?")) {
            1 -> whoAreYou(player)
            2 -> teachCanoeing(player)
        }
    }

    private suspend fun QueueTask.whoAreYou(player: Player) {
        chatPlayer(player, "Who are you?")
        chatNpc(player, "I'm Sigurd the Great and Brainy.")
        chatPlayer(player, "Why do they call you the Great and Brainy?")
        chatNpc(player, "Because I invented the Log Canoe!")
        chatPlayer(player, "Log Canoe?")
        chatNpc(
            player,
            "Yeash! Me and my cousins were having a great party by<br>the river when we decided to have a game of 'Smack<br>The Tree'",
        )
        chatPlayer(player, "Smack the Tree?")
        chatNpc(
            player,
            "It's a game were you take it in turnsh shmacking a<br>tree. First one to uproot the tree winsh!",
        )
        chatNpc(
            player,
            "Anyway, I won the game with a flying tackle. The tree<br>came loose and down the river bank I went, still<br>holding the tree.",
        )
        chatNpc(
            player,
            "I woke up a few hours later and found myself several<br>miles down river. And thatsh how I invented the log<br>canoe!",
        )
        chatPlayer(player, "So you invented the 'Log Canoe' by falling into a river<br>hugging a tree?")
        chatNpc(player, "Well I refined the design from the original you know!")
        chatNpc(player, "I cut all the branches off to make it more comfortable.<br>I could tell you how to if you like?")

        when (options(player, "Yes", "No")) {
            1 -> teachCanoeing(player, askedDirectly = false)
            2 -> chatPlayer(player, "No thanks, not right now.")
        }
    }

    private suspend fun QueueTask.teachCanoeing(
        player: Player,
        askedDirectly: Boolean = true,
    ) {
        if (askedDirectly) {
            chatPlayer(player, "Can you teach me about canoes?")
        }

        val woodcutting = player.getSkills().getBaseLevel(Skills.WOODCUTTING)

        if (woodcutting < 12) {
            chatNpc(player, "Well, you don't look like you have the skill to make a<br>canoe.")
            chatNpc(player, "You need to have at least level 12 woodcutting.")
            chatNpc(player, "Once you are able to make a canoe it makes travel<br>along the river much quicker!")
            return
        }

        chatNpc(player, "It's really quite simple. Just walk down to that tree on<br>the bank and chop it down.")
        chatNpc(player, "Then take your axe to it and shape it how you like!")

        when {
            woodcutting < 27 -> {
                chatNpc(player, "You can make a log canoe like mine! It'll get you 1 stop<br>down the river.")
                chatNpc(
                    player,
                    "There's some snooty fella down near the Champions<br>Guild who reckons his canoes are better than mine.<br>He's never said it to my face though.",
                )
            }

            woodcutting < 42 -> {
                chatNpc(
                    player,
                    "You could make a Dugout canoe with your woodcutting<br>skill, but I don't see why you would want to.",
                )
            }

            woodcutting < 57 -> {
                chatNpc(player, "Well, you're pretty handy with an axe!")
                chatNpc(player, "You could make Stable Dugout canoes, like that snooty<br>fella Tarquin.")
                chatNpc(player, "He reckons his canoes are better than mine. He's never<br>said it to my face though.")
            }

            else -> {
                chatNpc(player, "You look like you know your way around a tree, you can<br>make a Waka canoe.")
                chatPlayer(player, "What's a Waka?")
                chatNpc(
                    player,
                    "I've only ever seen Hari using them. People say he's<br>found a way to canoe the river underground and into<br>the Wilderness.",
                )
                chatNpc(player, "Hari hangs around up near Edgeville.")
                chatNpc(player, "He's a nice bloke.")
            }
        }
    }
}
