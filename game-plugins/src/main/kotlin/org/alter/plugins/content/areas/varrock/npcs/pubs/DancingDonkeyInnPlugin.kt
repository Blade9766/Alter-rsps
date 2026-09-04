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
import org.alter.plugins.content.areas.varrock.npcs.pubs.Bar.pay

/**
 * The Dancing Donkey Inn, tucked into the south-eastern corner of Varrock - the pub most players
 * never find. Its three regulars are the Biohazard drinking partners: Hops, Da Vinci and Chancy.
 *
 * | NPC       | id   | wiki tile  | spawned at |
 * |-----------|------|------------|------------|
 * | Bartender | 1311 | 3268, 3391 | as published |
 * | Hops      | 1108 | 3268, 3389 | as published |
 * | Da Vinci  | 1104 | 3272, 3389 | 3273, 3389 |
 * | Chancy    | 1106 | 3271, 3388 | 3270, 3388 |
 *
 * **Da Vinci and Chancy move one tile.** Both of their wiki rows are `mtype=pin` - a hand-placed
 * marker rather than a surveyed tile - and both land on the bar counter, the blocked column running
 * x 3271-3272 through z 3387-3390 in this cache. Each is put on the nearest free tile on the side
 * of the counter the pin was already leaning towards. Hops and the bartender need no adjustment;
 * their published tiles are clear.
 *
 * All three regulars have one-line brush-offs and nothing more. Their real dialogue belongs to
 * Biohazard, which is not built; the standard-dialogue lines here are what they say outside it.
 * The transcripts also carry a members-world variant of each line - this server has no members
 * flag, so the members-world line (the one without the "come back later" tail) is the one used.
 */
class DancingDonkeyInnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        spawnNpc(npc = BARTENDER, x = 3268, z = 3391, walkRadius = 1, direction = Direction.SOUTH)
        spawnNpc(npc = HOPS, x = 3268, z = 3389, walkRadius = 1, direction = Direction.EAST)
        spawnNpc(npc = DA_VINCI, x = 3273, z = 3389, walkRadius = 1, direction = Direction.WEST)
        spawnNpc(npc = CHANCY, x = 3270, z = 3388, walkRadius = 1, direction = Direction.EAST)

        onNpcOption(BARTENDER, option = "talk-to", lineOfSightDistance = 4) { player.queue { bartender(player) } }
        onNpcOption(HOPS, option = "talk-to", lineOfSightDistance = 4) { player.queue { hops(player) } }
        onNpcOption(DA_VINCI, option = "talk-to", lineOfSightDistance = 4) { player.queue { daVinci(player) } }
        onNpcOption(CHANCY, option = "talk-to", lineOfSightDistance = 4) { player.queue { chancy(player) } }
    }

    private suspend fun QueueTask.bartender(player: Player) {
        chatPlayer(player, "Hello.")
        chatNpc(player, "Good day to you, brave adventurer. Can I get you a<br>refreshing beer?")
        order(player)
    }

    /**
     * "How much?" puts the same three options back up - the transcript's `{{tact|other}}` - so the
     * offer is a loop rather than a recursive call; a player can ask the price as often as they like
     * without stacking up dialogue frames.
     */
    private suspend fun QueueTask.order(player: Player) {
        while (true) {
            when (options(player, "Yes please!", "No thanks.", "How much?")) {
                1 -> {
                    chatPlayer(player, "Yes please!")
                    chatNpc(player, "Ok then, that's two gold coins please.")

                    // The transcript gives him no line at all when the player cannot pay - he
                    // simply stops talking - so an unaffordable round ends in silence.
                    if (pay(player, "item.beer", BEER_PRICE)) {
                        chatNpc(player, "Cheers!")
                        chatPlayer(player, "Cheers!")
                    }
                    return
                }

                2 -> {
                    chatPlayer(player, "No thanks.")
                    chatNpc(player, "Let me know if you change your mind.")
                    return
                }

                3 -> {
                    chatPlayer(player, "How much?")
                    chatNpc(player, "Two gold pieces a pint. So, what do you say?")
                }
            }
        }
    }

    private suspend fun QueueTask.hops(player: Player) {
        chatNpc(player, "Hops don't wanna talk now.")
    }

    private suspend fun QueueTask.daVinci(player: Player) {
        chatNpc(
            player,
            "Bah! A great artist such as myself should not have to<br>suffer the HUMILIATION of spending time where the likes<br>of you wander everywhere!",
        )
    }

    private suspend fun QueueTask.chancy(player: Player) {
        chatPlayer(player, "Good morning.")
        chatNpc(player, "Leave me alone. I'm trying to find my gambling buddies!")
    }

    private companion object {
        const val BARTENDER = "npc.bartender_1311"
        const val HOPS = "npc.hops_1108"
        const val DA_VINCI = "npc.da_vinci_1104"
        const val CHANCY = "npc.chancy_1106"
    }
}
