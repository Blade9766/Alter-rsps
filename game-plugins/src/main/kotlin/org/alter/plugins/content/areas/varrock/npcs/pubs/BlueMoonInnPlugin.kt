package org.alter.plugins.content.areas.varrock.npcs.pubs

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.getInteractingNpc
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
import org.alter.plugins.content.areas.varrock.npcs.pubs.Bar.BEER_PRICE
import org.alter.plugins.content.areas.varrock.npcs.pubs.Bar.canAfford
import org.alter.plugins.content.areas.varrock.npcs.pubs.Bar.pay
import org.alter.plugins.content.mechanics.appearance.Pronoun
import org.alter.plugins.content.mechanics.appearance.pronoun
import org.alter.rscm.RSCM.getRSCM

/**
 * The Blue Moon Inn, south-central Varrock - the busiest of the city's four pubs, and the only one
 * whose regulars all have their own wiki pages.
 *
 * Everyone here comes from the inn's own `Personalities` and `Monsters` lists, placed on the tile
 * each one's own `{{Map}}` or `{{LocLine}}` publishes:
 *
 * | NPC                 | id    | tile                |
 * |---------------------|-------|---------------------|
 * | Bartender           | 1312  | 3226, 3398          |
 * | Dr Harlow           | 3480  | 3222, 3397          |
 * | Jonny the Beard     | 14138 | 3223, 3395          |
 * | Cook                | 2895  | 3230, 3400          |
 * | Barbarian (level 8) | 3262  | 3225, 3402          |
 * | Woman (Varrock)     | 3015  | 3218, 3395          |
 * | Man (Blue Moon Inn) | 3014  | 3231, 3395, plane 1 |
 *
 * Two deliberate calls:
 *
 * - **Jonny is the level-0 id, not the level-2 one.** The wiki lists three ids for him (5213,
 *   14138, 14139); the cache says 14139 and 5213 carry `Attack` and are combat level 2, while 14138
 *   is `Talk-to` only. He is only attackable once Straven tasks the player with killing him during
 *   Shield of Arrav, and that quest is not built, so the non-attackable id is the honest default.
 * - **The inn's article says "2 Men/1 Woman"; only one of each has a published tile.** Rather than
 *   invent a second man's spawn point, only the two the `Man` and `Woman` pages actually pin are
 *   here. The `Thief` and `Imp` the article also lists are wanderers that happen to walk in, not
 *   residents, so they are not spawned in the inn either.
 *
 * Combat stats for the barbarian and the citizens are not declared here - the level 8 Blue Moon
 * barbarian is the one variant [org.alter.plugins.content.npcs.barbarian.BarbarianData] deliberately
 * leaves out, and it falls through to `data/cfg/npcs/monsterStats.json`; the men and women are
 * covered by [org.alter.plugins.content.npcs.citizen.CitizenPlugin].
 *
 * Left out on purpose: the bartender's Barcrawl drink and his clue-scroll branch, and the cook's
 * clue-scroll branch. Alfred Grimhand's Barcrawl and Treasure Trails are both unbuilt, and a
 * half-wired branch reads worse than an absent one.
 */
class BlueMoonInnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = BARTENDER, x = 3226, z = 3398, walkRadius = 1, direction = Direction.SOUTH)
        spawnNpc(npc = HARLOW, x = 3222, z = 3397, walkRadius = 1, direction = Direction.EAST)
        spawnNpc(npc = JONNY, x = 3223, z = 3395, walkRadius = 1, direction = Direction.NORTH)
        spawnNpc(npc = COOK, x = 3230, z = 3400, walkRadius = 1, direction = Direction.WEST)
        spawnNpc(npc = BARBARIAN, x = 3225, z = 3402, walkRadius = 3, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.woman_3015", x = 3218, z = 3395, walkRadius = 4, direction = Direction.EAST)
        spawnNpc(npc = "npc.man_3014", x = 3231, z = 3395, height = 1, walkRadius = 4, direction = Direction.WEST)

        onNpcOption(BARTENDER, option = "talk-to", lineOfSightDistance = 4) { player.queue { bartender(player) } }
        onNpcOption(HARLOW, option = "talk-to", lineOfSightDistance = 4) { player.queue { harlow(player) } }
        onNpcOption(JONNY, option = "talk-to", lineOfSightDistance = 4) { player.queue { jonny(player) } }
        onNpcOption(COOK, option = "talk-to", lineOfSightDistance = 4) { player.queue { cook(player) } }
        onNpcOption(BARBARIAN, option = "talk-to", lineOfSightDistance = 4) { player.queue { barbarian(player) } }
    }

    // --- Bartender ------------------------------------------------------------------------------

    private suspend fun QueueTask.bartender(player: Player) {
        chatNpc(player, "What can I do yer for?")

        when (options(
            player,
            "A glass of your finest ale please.",
            "Can you recommend where an adventurer might make his fortune?",
            "Do you know where I can get some good equipment?",
        )) {
            1 -> {
                chatPlayer(player, "A glass of your finest ale please.")
                chatNpc(player, "No problemo. That'll be 2 coins.")

                if (!player.canAfford(BEER_PRICE)) {
                    chatPlayer(player, "Oh dear. I don't seem to have enough money.")
                } else if (pay(player, "item.beer", BEER_PRICE)) {
                    player.message("You buy a pint of beer.")
                }
            }

            2 -> {
                chatPlayer(player, "Can you recommend where an adventurer might make his fortune?")
                chatNpc(player, "Ooh I don't know if I should be giving away information,<br>makes the game too easy.")
                fourthWall(player)
            }

            3 -> {
                chatPlayer(player, "Do you know where I can get some good equipment?")
                chatNpc(player, "Well, there's the sword shop across the road, or there's<br>also all sorts of shops up around the market.")
            }
        }
    }

    /**
     * The branch the inn is famous for: the bartender is one of the few NPCs who knows he is in a
     * computer game, and the player refuses to believe him.
     */
    private suspend fun QueueTask.fourthWall(player: Player) {
        when (options(
            player,
            "Oh ah well...",
            "Game? What are you talking about?",
            "Just a small clue?",
        )) {
            1 -> chatPlayer(player, "Oh ah well...")

            2 -> {
                chatPlayer(player, "Game? What are you talking about?")
                chatNpc(player, "This world around us... is an online game... called<br>Old School RuneScape.")
                chatPlayer(player, "Nope, still don't understand what you are talking about.<br>What does 'online' mean?")
                chatNpc(
                    player,
                    "It's a sort of connection between magic boxes across the<br>world, big boxes on people's desktops and little ones<br>people can carry. They can talk to each other to play games.",
                )
                chatPlayer(player, "I give up. You're obviously completely mad!")
            }

            3 -> {
                chatPlayer(player, "Just a small clue?")
                chatNpc(player, "Go and talk to the bartender at the Jolly Boar Inn, he<br>doesn't seem to mind giving away clues.")
            }
        }
    }

    // --- The regulars ---------------------------------------------------------------------------

    private suspend fun QueueTask.harlow(player: Player) {
        chatNpc(player, "Buy me a drink pleassh...")
        chatPlayer(player, "I think you've had enough.")
    }

    private suspend fun QueueTask.jonny(player: Player) {
        chatNpc(player, "Whatever you want, I'm not interested!")
    }

    private suspend fun QueueTask.cook(player: Player) {
        chatNpc(player, "What do you want? I'm a little busy here, so make it quick!")

        when (options(
            player,
            "Can you sell me any food?",
            "Can you give me any free food?",
            "I don't want anything from this horrible kitchen.",
            title = "What would you like to say?",
        )) {
            1 -> {
                chatPlayer(player, "Can you sell me any food?")
                chatNpc(player, "I suppose I could sell you some cabbage, if you're willing<br>to pay for it. Cabbage is good for you.")
                cabbage(player)
            }

            2 -> {
                chatPlayer(player, "Can you give me any free food?")
                chatNpc(player, "Can you give me any free money?")
                chatPlayer(player, "Why should I give you free money?")
                chatNpc(player, "Why should I give you free food?")
                chatPlayer(player, "Oh, forget it.")
            }

            3 -> {
                chatPlayer(player, "I don't want anything from this horrible kitchen.")
                chatNpc(
                    player,
                    "How dare you? I put a lot of effort into cleaning this<br>kitchen. My daily sweat and elbow-grease keep this<br>kitchen clean!",
                )
                chatPlayer(player, "Ewww!")
                chatNpc(player, "Oh, just leave me alone.")
            }
        }
    }

    /**
     * The cook checks the player's money and their inventory space *before* offering the sale, and
     * has a separate scolding for each - the only bar transaction in Varrock that does, which is why
     * the guards are here rather than left to [Bar.pay].
     */
    private suspend fun QueueTask.cabbage(player: Player) {
        if (player.inventory.getItemCount(getRSCM("item.coins_995")) < CABBAGE_PRICE) {
            chatPlayer(player, "Oh, I haven't got any money.")
            chatNpc(player, "Why are you asking me to sell you food if you haven't got<br>any money? Go away!")
            return
        }

        if (player.inventory.freeSlotCount == 0) {
            chatPlayer(player, "Oh, I haven't got enough space to carry it.")
            chatNpc(player, "Why are you asking me to sell you food if you can't carry<br>it? Go away!")
            return
        }

        when (options(
            player,
            "Alright, I'll buy a cabbage.",
            "No thanks, I don't like cabbage.",
            title = "What would you like to say?",
        )) {
            1 -> {
                chatPlayer(player, "Alright, I'll buy a cabbage.")
                pay(player, "item.cabbage", CABBAGE_PRICE)
                chatNpc(player, "It's a deal. Now, make sure you eat it all up. Cabbage is<br>good for you.")
            }

            2 -> {
                chatPlayer(player, "No thanks, I don't like cabbage.")
                chatNpc(player, "Bah! People these days only appreciate junk food.")
            }
        }
    }

    // --- Barbarian ------------------------------------------------------------------------------

    /**
     * The level 8 barbarian's own transcript section, which is not the generic barbarian one. She is
     * female - her own line says "I strong barbarian woman" - so only the player's half of the
     * "little city man/woman/person" address varies, off the player's chosen pronoun.
     */
    private suspend fun QueueTask.barbarian(player: Player) {
        val cityFolk = player.cityFolk()

        chatPlayer(player, "Hello.")
        chatNpc(player, "Hah! What you want with me, little city $cityFolk?")

        when (options(
            player,
            "Can I buy you a drink?",
            "Can I fight you?",
            "Um...",
        )) {
            1 -> drinkTest(player, cityFolk)

            2 -> {
                chatPlayer(player, "Can I fight you?")
                chatNpc(player, "Ha ha ha! We fight anytime you like.")
            }

            3 -> {
                chatPlayer(player, "Um...")
                chatNpc(player, "Ha ha ha! I not interested in talking to soft city $cityFolk<br>who not know what he want.")
            }
        }
    }

    /** The barcry exchange: she yells, the player yells back, twice, and she is unimpressed. */
    private suspend fun QueueTask.drinkTest(player: Player, cityFolk: String) {
        val barbarian = player.getInteractingNpc()
        val name = player.username

        chatNpc(player, "Ha ha ha! Little $name wants to buy me a drink, how sweet.")
        chatPlayer(player, "Well... how about it?")
        chatNpc(
            player,
            "You not talk like barbarian $cityFolk. You certainly not<br>smell like barbarian $cityFolk! But I think I give you test<br>anyway.",
        )
        chatPlayer(player, "What's the test?")
        chatNpc(
            player,
            "Back in village to the west, we barbarians scream our<br>defiance as we fight. We scream<br>'YYEEEEEAAARRRRGGHHHH!!!' Can you do this?",
        )

        repeat(2) {
            barbarian.forceChat(BARBARIAN_CRY)
            wait(CRY_DELAY)
            player.forceChat(PLAYER_CRY)
            wait(CRY_DELAY)
        }

        chatNpc(
            player,
            "Ha ha ha! Little $name cannot yell like barbarian. Also, I<br>strong barbarian woman; I get my own drink. Run away<br>now, little $name.",
        )
    }

    /** "man", "woman" or "person", to match how the barbarian addresses the player. */
    private fun Player.cityFolk(): String =
        when (pronoun) {
            Pronoun.HE -> "man"
            Pronoun.SHE -> "woman"
            Pronoun.THEY -> "person"
        }

    private companion object {
        const val BARTENDER = "npc.bartender_1312"
        const val HARLOW = "npc.dr_harlow"
        const val JONNY = "npc.jonny_the_beard"
        const val COOK = "npc.cook_2895"
        const val BARBARIAN = "npc.barbarian_3262"

        /** Wiki `Cook (Blue Moon Inn)` transcript: "1 GP is deducted". */
        const val CABBAGE_PRICE = 1

        const val BARBARIAN_CRY = "YYEEEEEAAARRRRGGHHHH!!!"
        const val PLAYER_CRY = "Yyeeeeeaaarrrrgghhhh!!!"

        /** Long enough for one force-chat bubble to be read before the next one lands. */
        const val CRY_DELAY = 3
    }
}
