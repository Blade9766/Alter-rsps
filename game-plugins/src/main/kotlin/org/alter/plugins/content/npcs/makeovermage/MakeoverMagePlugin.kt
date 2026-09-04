package org.alter.plugins.content.npcs.makeovermage

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.itemMessageBox
import org.alter.api.ext.options
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.mechanics.appearance.Pronoun
import org.alter.plugins.content.mechanics.appearance.openMakeover
import org.alter.plugins.content.mechanics.appearance.pronoun
import org.alter.rscm.RSCM.getRSCM

/**
 * The Makeover Mage, in the house south-west of Falador at (2918, 3322) - the one with the
 * pharmakos bushes outside it.
 *
 * They give free access to the makeover window, which is where body type, skin colour and
 * pronouns actually get changed; this file is the NPC around it. Both npc options in the cache are
 * wired: "Makeover" opens the window straight away, and "Talk-to" runs the transcript, which
 * reaches the same window through "Sure, I'll have a makeover."
 *
 * The yin yang amulet is sold through dialogue rather than a shop, the way the transcript has it:
 * 100 coins, no bonuses, and untradeable.
 *
 * Two things from the wiki are deliberately not here. The mage flips between Pete (1306) and Peta
 * (1307) every ten seconds in the live game; [org.alter.game.model.entity.Npc.id] is fixed at
 * construction and the avatar is not reachable from a plugin, so there is no way to do that
 * without an engine change, and Pete is spawned. The Demonic skin contract needs an item and a
 * skin colour this cache's [org.alter.game.model.appearance.Colours.SKIN_COLOURS] does not carry.
 */
class MakeoverMagePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {

    init {
        // walkRadius 0, as with the bankers: this is a service npc, and a planted one is always
        // where the player who walked in to find them expects them to be. Faced east, towards the
        // door of the house.
        spawnNpc(npc = MAGE, x = 2918, z = 3322, height = 0, walkRadius = 0, direction = Direction.EAST)

        onNpcOption(npc = MAGE, option = "talk-to") {
            player.queue { dialog(player) }
        }

        onNpcOption(npc = MAGE, option = "makeover") {
            player.beginMakeover()
        }
    }

    private suspend fun QueueTask.dialog(player: Player) {
        chatNpc(
            player,
            "Hello there! I am known as the Makeover Mage! I have<br>spent many years researching magics that can change<br>your physical appearance!",
            npc = MAGE_ID,
        )
        chatNpc(
            player,
            "I can alter your physical form with my magic, there's no<br>fee. Would you like me to perform my magics upon you?",
            npc = MAGE_ID,
        )
        offer(player)
    }

    /**
     * The main menu. Every branch that does not end the conversation comes back here, which is
     * what lets the mage ask "Anyway, would you like me to alter your physical form?" and then put
     * the same options back up.
     */
    private suspend fun QueueTask.offer(player: Player) {
        while (true) {
            when (
                options(
                    player,
                    "Tell me more about this 'make-over'.",
                    "Sure, I'll have a makeover.",
                    "Could you change my pronouns?",
                    "Cool amulet! Can I have one?",
                    "No thanks.",
                )
            ) {
                1 -> explain(player)

                2 -> {
                    chatPlayer(player, "Sure, I'll have a makeover.")
                    chatNpc(
                        player,
                        "Good choice, good choice. You wouldn't want to carry on<br>looking like that, I'm sure!",
                        npc = MAGE_ID,
                    )
                    player.beginMakeover()
                    return
                }

                3 -> pronouns(player)

                4 -> {
                    amulet(player)
                    chatNpc(
                        player,
                        "Anyway, would you like me to alter your physical form?<br>For you, I'll do it for free!",
                        npc = MAGE_ID,
                    )
                }

                else -> {
                    chatPlayer(player, "No thanks.")
                    chatNpc(player, "Ehhh... suit yourself.", npc = MAGE_ID)
                    return
                }
            }
        }
    }

    private suspend fun QueueTask.explain(player: Player) {
        chatPlayer(player, "Tell me more about this 'make-over'.")
        chatNpc(
            player,
            "Why, of course! Basically, and I will try and explain this<br>so that you will understand it correctly,",
            npc = MAGE_ID,
        )
        chatNpc(
            player,
            "I use my secret magical technique to melt your body<br>down into a puddle of its elements.",
            npc = MAGE_ID,
        )
        chatNpc(
            player,
            "When I have broken down all trace of your body, I then<br>rebuild it into the form I am thinking of!",
            npc = MAGE_ID,
        )
        chatNpc(player, "Or, you know, somewhere vaguely close enough anyway.", npc = MAGE_ID)
        chatPlayer(player, "Uh... that doesn't sound particularly safe to me...")
        chatNpc(
            player,
            "It's as safe as houses! Why, I have only had thirty-six<br>major accidents this month!",
            npc = MAGE_ID,
        )
        chatNpc(player, "So what do you say? Feel like a change? There's no fee.", npc = MAGE_ID)
    }

    /**
     * Pronouns, out of the makeover window as well as inside it.
     *
     * This branch is not in the wiki's transcript - the transcript predates pronouns being a
     * makeover option at all - but the mage's own page describes them as the one who changes
     * "body type, skin colour or pronouns", and unlike the other two this one has a route that
     * does not depend on the window's dropdown reaching the server.
     */
    private suspend fun QueueTask.pronouns(player: Player) {
        chatPlayer(player, "Could you change my pronouns?")
        chatNpc(
            player,
            "Of course! No melting required for that one. What should<br>I call you?",
            npc = MAGE_ID,
        )

        val choice =
            options(
                player,
                *Pronoun.values.map { it.label }.toTypedArray(),
                title = "Select your pronouns",
            )
        val pronoun = Pronoun.values.getOrNull(choice - 1) ?: return

        player.pronoun = pronoun
        chatNpc(
            player,
            "${pronoun.label} it is. I shall spread the word!",
            npc = MAGE_ID,
        )
    }

    private suspend fun QueueTask.amulet(player: Player) {
        chatPlayer(player, "Cool amulet! Can I have one?")
        chatNpc(
            player,
            "No problem, but please remember that the amulet I will<br>sell you is only a copy of my own. It contains no magical<br>powers, and as such will only cost you 100 coins.",
            npc = MAGE_ID,
        )

        if (player.inventory.getItemCount(COINS_ID) < AMULET_PRICE) {
            chatPlayer(player, "Oh, I don't have enough money for that.")
            return
        }

        if (options(player, "Sure, here you go.", "No way! That's far too expensive.") != 1) {
            chatPlayer(player, "No way! That's far too expensive.")
            chatNpc(player, "That's fair enough, my jewellery is not to everyone's taste.", npc = MAGE_ID)
            return
        }

        chatPlayer(player, "Sure, here you go.")

        // Coins first: paying with a whole stack frees the slot the amulet then lands in, so
        // checking for a free slot up front would refuse a sale that fits.
        player.inventory.remove(COINS, AMULET_PRICE)
        if (player.inventory.add(AMULET).hasFailed()) {
            player.inventory.add(COINS, AMULET_PRICE)
            chatPlayer(player, "I don't have room to hold it. Maybe another time.")
            return
        }

        itemMessageBox(player, "You receive an amulet in exchange for 100 coins.", item = AMULET)
    }

    /**
     * Opens the window, and lines up one of the four post-transformation exchanges to run when the
     * player presses Confirm. The dialogue cannot simply follow the call: the window is a modal
     * driven by button packets, not something a queued task can wait on.
     */
    private fun Player.beginMakeover() {
        openMakeover { player -> player.queue { afterMakeover(player) } }
    }

    private suspend fun QueueTask.afterMakeover(player: Player) {
        when (world.random(3)) {
            0 -> {
                chatNpc(player, "Woah!", npc = MAGE_ID)
                chatPlayer(player, "What?")
                chatNpc(player, "You still look human!", npc = MAGE_ID)
            }

            1 -> {
                chatNpc(
                    player,
                    "Hmm... you didn't feel any unexpected growths anywhere<br>around your head just then did you?",
                    npc = MAGE_ID,
                )
                chatPlayer(player, "Uh... no...?")
                chatNpc(player, "Good, good! I was worried for a second there!", npc = MAGE_ID)
            }

            2 -> {
                chatNpc(player, "Whew! That was lucky!", npc = MAGE_ID)
                chatPlayer(player, "What was?")
                chatNpc(player, "Nothing! It's all fine! You seem alive anyway!", npc = MAGE_ID)
            }

            else ->
                chatNpc(
                    player,
                    "Two arms... two legs... one head... it seems that spell<br>finally worked okay!",
                    npc = MAGE_ID,
                )
        }
        chatPlayer(player, "Uh... Thanks, I guess.")
    }

    private companion object {
        const val MAGE = "npc.makeover_mage"
        const val COINS = "item.coins_995"
        const val AMULET = "item.yin_yang_amulet"
        const val AMULET_PRICE = 100

        /**
         * Passed to every chat box explicitly rather than left to the interacting npc: the
         * post-makeover lines run from a button press, long after that interaction ended.
         */
        val MAGE_ID = getRSCM(MAGE)
        val COINS_ID = getRSCM(COINS)
    }
}
