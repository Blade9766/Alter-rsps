package org.alter.plugins.content.areas.varrock.npcs.pubs

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
import org.alter.plugins.content.areas.varrock.npcs.pubs.Bar.BEER_PRICE
import org.alter.plugins.content.areas.varrock.npcs.pubs.Bar.canAfford
import org.alter.plugins.content.areas.varrock.npcs.pubs.Bar.pay
import org.alter.plugins.content.mechanics.appearance.pronoun

/**
 * The Jolly Boar Inn, north-east of Varrock on the Wilderness border - the city's only two-storey
 * pub, and the one whose bartender will actually tell you where to go adventuring.
 *
 * | NPC              | id   | wiki tile           | spawned at          |
 * |------------------|------|---------------------|---------------------|
 * | Bartender        | 1310 | 3277, 3489          | 3277, 3490          |
 * | Cook             | 2896 | 3285, 3489          | as published        |
 * | Johnathon        | 5443 | 3278, 3505, plane 1 | as published        |
 * | Black Knight     | 516  | 3277, 3505          | as published        |
 * | Man              | 3106 | 3277, 3495, plane 1 | as published        |
 * | Woman            | 3111 | 3278, 3502          | as published        |
 * | Woman            | 3112 | 3279, 3496          | as published        |
 *
 * **The bartender moves one tile north.** His published tile, 3277,3489, is the bar counter itself -
 * blocked in this cache along the whole run x 3272-3280 - so he goes behind it, into the single free
 * gap at 3277,3490 between the barrels at 3276 and 3278. That gap is exactly bartender-shaped and is
 * the only tile on the serving side he can occupy.
 *
 * **The man and the two women are the generic ids.** Their `{{LocLine}}` rows carry no `title:`, so
 * the wiki does not say which shirt colour stands where; unlike the Blue Moon Inn, this pub has no
 * named `Man (…)` version of its own. The ids picked are the plain ones, and within
 * [org.alter.plugins.content.npcs.citizen.Citizens] the choice is purely cosmetic anyway.
 *
 * The article describes the man and woman as acting drunk when spoken to - "there are two of them",
 * "giant hairy cabbages" - but neither line is in any transcript, so they are spawned without
 * dialogue rather than with invented dialogue. Johnathon likewise gets only his pre-quest line:
 * everything else he says needs Family Crest, and his gauntlet-enchanting service belongs to id 5445
 * (the one the cache gives a `Gauntlets` option), which is the post-quest version of him.
 *
 * The `Thief` the article mentions is a wanderer, not a resident, and is not spawned here.
 */
class JollyBoarInnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = BARTENDER, x = 3277, z = 3490, walkRadius = 0, direction = Direction.SOUTH)
        spawnNpc(npc = COOK, x = 3285, z = 3489, walkRadius = 1, direction = Direction.NORTH)
        spawnNpc(npc = JOHNATHON, x = 3278, z = 3505, height = 1, walkRadius = 1, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_knight", x = 3277, z = 3505, walkRadius = 4, direction = Direction.WEST)
        spawnNpc(npc = "npc.man_3106", x = 3277, z = 3495, height = 1, walkRadius = 4, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.woman_3111", x = 3278, z = 3502, walkRadius = 4, direction = Direction.WEST)
        spawnNpc(npc = "npc.woman_3112", x = 3279, z = 3496, walkRadius = 4, direction = Direction.EAST)

        onNpcOption(BARTENDER, option = "talk-to", lineOfSightDistance = 4) { player.queue { bartender(player) } }
        onNpcOption(COOK, option = "talk-to", lineOfSightDistance = 4) { player.queue { cook(player) } }
        onNpcOption(JOHNATHON, option = "talk-to", lineOfSightDistance = 4) { player.queue { johnathon(player) } }
    }

    private suspend fun QueueTask.bartender(player: Player) {
        chatNpc(player, "Can I help you?")

        when (options(
            player,
            "I'll have a beer please.",
            "Any hints where I can go adventuring?",
            "Heard any good gossip?",
        )) {
            1 -> {
                chatPlayer(player, "I'll have a pint of beer please.")
                chatNpc(player, "Ok, that'll be two coins please.")

                if (!player.canAfford(BEER_PRICE)) {
                    chatPlayer(player, "Oh dear, I don't seem to have enough money.")
                } else {
                    pay(player, "item.beer", BEER_PRICE)
                }
            }

            2 -> {
                chatPlayer(player, "Any hints on where I can go adventuring?")
                chatNpc(player, "Ooh, now. Let me see...")
                chatNpc(
                    player,
                    "Well there is the Varrock sewers. There are tales of<br>untold horrors coming out at night and stealing babies<br>from houses.",
                )
                chatPlayer(player, "Sounds perfect! Where's the entrance?")
                chatNpc(player, "It's just to the east of the palace.")
            }

            3 -> {
                chatPlayer(player, "Heard any gossip?")
                chatNpc(
                    player,
                    "I'm not that well up on the gossip out here. I've heard<br>that the bartender in the Blue Moon Inn has gone a little<br>crazy, he keeps claiming he is part of something called an<br>online game.",
                )
                chatNpc(player, "What that means, I don't know. That's probably old news<br>by now though.")
            }
        }
    }

    /**
     * The inn's cook, who has no interest in cooking anything for anyone. His "recently asked"
     * greeting variant is left out: it turns on state nothing else reads, and the difference is one
     * sentence of grumbling.
     */
    private suspend fun QueueTask.cook(player: Player) {
        chatNpc(player, "What do you want? I'm busy!")

        when (options(
            player,
            "Can you cook me something?",
            "Why do you work here?",
            "Can you tell me any good jokes?",
            title = "What would you like to say?",
        )) {
            1 -> {
                chatPlayer(player, "Can you cook me something?")
                chatNpc(player, "What? No! Go away.")
                chatPlayer(player, "Well, you're meant to be a cook. Why can't you cook<br>something for me?")
                chatNpc(player, "I don't want to, that's why! Now, leave me alone.")
                chatPlayer(player, "You're not a very nice man.")
                chatNpc(player, "Yet you keep talking to me!")
                chatPlayer(player, "Good point. I should stop that.")
                chatNpc(player, "Yes, do us both a favour.")
            }

            2 -> {
                chatPlayer(player, "Why do you work here?")
                chatNpc(
                    player,
                    "Why do you think I work in a disreputable inn filled with<br>rough scoundrels on the very edge of the Wilderness? I<br>work here because I need the job!",
                )
                chatPlayer(player, "Do you like it?")
                chatNpc(
                    player,
                    "Like it? It's sucked the warmth and humanity out of me,<br>and I've become a hollow shell holding nothing but anger,<br>misery and gourmet recipes.",
                )
                chatPlayer(player, "Oh.")
                chatNpc(
                    player,
                    "What did you expect to hear? I suppose you expected me<br>to tell you an inspiring story about how working here can<br>teach you to find hope in the darkest places, or how to<br>see the best in everything?",
                )
                chatPlayer(player, "Maybe you should think about moving to a different inn.")
                chatNpc(player, "Bah! I'm not letting it defeat me. Now, let me<br>concentrate on my cooking.")
                chatPlayer(player, "Okay.")
            }

            3 -> {
                chatPlayer(player, "Can you tell me any good jokes?")
                chatNpc(player, "Ohhh, if you insist...")
                chatNpc(player, "What did the half-wit say to the cook?")
                chatPlayer(player, "I don't know.")
                // The transcript writes this as "[He/She/They] said" - the punchline is the player,
                // so the subject pronoun is theirs.
                val they = player.pronoun.subject.replaceFirstChar { it.uppercase() }
                chatNpc(player, "$they said 'Can you tell me any good jokes?' Got ya!<br>Hahahaha!")
                chatPlayer(player, "Whatever.")
            }
        }
    }

    private suspend fun QueueTask.johnathon(player: Player) {
        chatNpc(player, "I am so very tired... Leave me be... to rest...")
    }

    private companion object {
        const val BARTENDER = "npc.bartender_1310"
        const val COOK = "npc.cook_2896"
        const val JOHNATHON = "npc.johnathon"
    }
}
