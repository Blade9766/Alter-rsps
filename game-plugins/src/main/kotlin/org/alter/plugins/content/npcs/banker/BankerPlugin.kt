package org.alter.plugins.content.npcs.banker

import org.alter.api.InterfaceDestination
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.interfaces.bank.openBank

/**
 * The bankers of every town and city: where they stand, and what they do when talked to.
 *
 * Spawns and ids come from [Banks]; this file is the wiring.
 *
 * Bankers are spawned with `walkRadius = 0` on purpose. That is the flag
 * [org.alter.plugins.content.mechanics.npcwalk.NpcRandomWalkPlugin] gates random walking on
 * (`if (npc.walkRadius > 0)`), so a zero keeps every banker planted on its own tile behind its own
 * booth instead of wandering off through the counter. Each one is turned to face across the booth
 * at the players rather than at the wall behind it.
 *
 * The npc option handlers are registered per id rather than once, because "talk-to", "bank" and
 * "collect" are per-npc registrations - a banker id that is spawned but not registered here is a
 * banker you cannot click.
 */
class BankerPlugin(
    r: PluginRepository, world: World, server: Server
) : KotlinPlugin(r, world, server) {

    init {
        Banks.ALL.forEach { bank ->
            bank.spawns.forEach { spawn ->
                spawnNpc(
                    npc = spawn.npcKey,
                    x = spawn.x,
                    z = spawn.z,
                    height = spawn.height,
                    walkRadius = 0,
                    direction = spawn.facing,
                )
            }
        }

        Banks.NPC_KEYS.forEach { banker ->
            onNpcOption(npc = banker, option = "talk-to", lineOfSightDistance = 2) {
                player.queue {
                    dialog(player, this)
                }
            }
            onNpcOption(npc = banker, option = "bank", lineOfSightDistance = 2) {
                player.openBank()
            }
            onNpcOption(npc = banker, option = "collect", lineOfSightDistance = 2) {
                openCollect(player)
            }
        }
    }

    suspend fun dialog(player: Player, it: QueueTask) {
        it.chatNpc(player, "Good day, how may I help you?")
        when (options(player, it)) {
            1 -> player.openBank()
            2 -> openPin(player)
            3 -> openCollect(player)
            4 -> whatIsThisPlace(player, it)
        }
    }

    suspend fun options(player: Player, it: QueueTask): Int =
        it.options(
            player,
            "I'd like to access my bank account, please.",
            "I'd like to check my PIN settings.",
            "I'd like to collect items.",
            "What is this place?",
        )

    suspend fun whatIsThisPlace(player: Player, it: QueueTask) {
        it.chatNpc(
            player,
            "This is a branch of the Bank of Gielinor. We have<br>branches in many towns.",
            animation = 568
        )
        it.chatPlayer(player, "And what do you do?", animation = 554)
        it.chatNpc(
            player,
            "We will look after your items and money for you.<br>Leave your valuables with us if you want to keep them<br>safe.",
            animation = 569,
        )
    }

    private fun openCollect(p: Player) {
        p.setInterfaceUnderlay(color = -1, transparency = -1)
        p.openInterface(interfaceId = 402, dest = InterfaceDestination.MAIN_SCREEN)
    }

    private fun openPin(p: Player) {
        p.setInterfaceUnderlay(color = -1, transparency = -1)
        p.openInterface(interfaceId = 14, dest = InterfaceDestination.MAIN_SCREEN)
    }

}
