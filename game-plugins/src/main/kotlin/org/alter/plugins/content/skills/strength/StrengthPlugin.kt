package org.alter.plugins.content.skills.strength

import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
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
 * Otto Godblessed, and the barehanded fishing he teaches.
 *
 * Barehanded fishing is the one place outside combat where a Strength level is spent to earn
 * Strength experience: a tuna wants 55 Fishing *and* 35 Strength and pays 8 Strength experience
 * on top of the usual 80 Fishing, a swordfish 70/50 for 10 on top of 100. The catch itself is
 * handled where the rest of harpooning is, in
 * [org.alter.plugins.content.skills.fishing.FishingPlugin] - all that lives here is the lesson
 * that switches it on, and the [Strength.BAREHAND_FISHING_ATTR] it sets.
 *
 * Otto is spawned here rather than in an `areas/` package because nothing else in Otto's Grotto
 * is built yet; when the grotto gets its own content this spawn should move there and leave the
 * dialogue behind.
 *
 * Only the player's option line - "Please teach me of your cunning with harpoons." - is quoted
 * from the wiki; the transcript page does not carry Otto's side of this branch, so his replies
 * are written to match the voice of the ones it does carry.
 */
class StrengthPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = OTTO, x = OTTO_X, z = OTTO_Z, height = 0, walkRadius = 3, direction = Direction.SOUTH)

        onNpcOption(OTTO, option = "talk-to", lineOfSightDistance = 4) {
            player.queue { dialogue(player) }
        }
    }

    private suspend fun QueueTask.dialogue(player: Player) {
        chatNpc(player, "Greetings, friend. I am Otto Godblessed. I train those<br>who wish to learn the ways of the barbarians.")
        when (options(player, "Please teach me of your cunning with harpoons.", "Nothing, thanks.")) {
            1 -> {
                chatPlayer(player, "Please teach me of your cunning with harpoons.")
                if (Strength.hasBarehandFishing(player)) {
                    chatNpc(player, "You have already learnt this from me. Your arm is<br>bait enough - go and use it.")
                    return
                }
                chatNpc(player, "A harpoon? Bah. A barbarian needs no harpoon. Your<br>own arm is bait enough, if you have the strength to<br>hold on to what bites it.")
                chatNpc(player, "Fish this way and the struggle will make you stronger,<br>though the fish will not come as easily as they would<br>to a spear.")
                Strength.unlockBarehandFishing(player)
                player.message("You have learnt how to fish barehanded.")
                chatPlayer(player, "Thank you, Otto.")
            }

            2 -> chatPlayer(player, "Nothing, thanks.")
        }
    }

    private companion object {
        const val OTTO = "npc.otto_godblessed"

        /** Otto's Grotto, west of Baxtorian Falls - the tile the wiki's map template pins him to. */
        const val OTTO_X = 2502
        const val OTTO_Z = 3489
    }
}
