package org.alter.plugins.content.areas.warriorsguild.activities

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.areas.warriorsguild.WarriorsGuild

/**
 * Jimmy's challenge: stack kegs on your head, walk without dropping them, get paid for how many
 * you managed.
 *
 * Picking a keg up adds one to the stack, to a maximum of [MAX_KEGS]. Talking to Jimmy cashes the
 * stack in for tokens and Strength experience and clears it. Each keg picked up also costs run
 * energy, which is the wiki's stated limiter - "energy restoration extends duration".
 *
 * ## What is sourced and what is not
 *
 * The wiki gives the cap of five kegs, that "tokens [are] awarded based on kegs balanced", and
 * that the challenge "generates Strength experience passively". It publishes no table. So the
 * payout here is [TOKENS_PER_KEG] tokens and [XP_PER_KEG] Strength experience per keg, scaled by
 * the stack size squared so that a full stack of five is worth substantially more than five
 * separate single kegs - which is what makes balancing five the point of the activity rather than
 * a flourish.
 *
 * The **drop** is not modelled: in the real challenge a stack collapses if the player runs out of
 * energy while carrying it. Here the energy simply gates how many can be picked up. That removes
 * the risk but keeps the cost.
 */
class KegChallengePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        KEGS.forEach { keg ->
            onObjOption(obj = keg, option = "pick-up", lineOfSightDistance = 1) {
                pickUp(player)
            }
        }

        onNpcOption(npc = JIMMY, option = "talk-to") {
            player.queue { cashIn(player) }
        }
    }

    private fun pickUp(player: Player) {
        val balanced = player.attr[KEGS_BALANCED] ?: 0
        if (balanced >= MAX_KEGS) {
            player.message("You cannot balance any more kegs than that.")
            return
        }
        if (player.runEnergy < ENERGY_PER_KEG) {
            player.message("You are far too tired to lift another keg.")
            return
        }

        player.runEnergy -= ENERGY_PER_KEG
        player.attr[KEGS_BALANCED] = balanced + 1
        player.message("You balance a keg on your head. That makes ${balanced + 1}.")
    }

    private suspend fun org.alter.game.model.queue.QueueTask.cashIn(player: Player) {
        val balanced = player.attr[KEGS_BALANCED] ?: 0
        if (balanced == 0) {
            chatNpc(player, "Balance some kegs on your head and come back to me,<br>and I'll see you right for it.")
            return
        }

        /*
         * Squared, so five kegs beats five trips with one. The whole challenge is about stacking
         * them, and a linear payout would make the stack pointless.
         */
        val tokens = TOKENS_PER_KEG * balanced * balanced
        val xp = XP_PER_KEG * balanced * balanced

        player.attr.remove(KEGS_BALANCED)
        player.addXp(Skills.STRENGTH, xp)
        player.inventory.add(WarriorsGuild.TOKEN, tokens)

        chatNpc(player, "$balanced kegs! Here's $tokens tokens for your trouble.")
    }

    private companion object {
        const val JIMMY = "npc.jimmy"

        /** The kegs the cache places in Jimmy's room, on the first floor east of the guild hall. */
        val KEGS = listOf("object.keg_15668")

        const val MAX_KEGS = 5

        const val TOKENS_PER_KEG = 2
        const val XP_PER_KEG = 5.0

        const val ENERGY_PER_KEG = 8

        /**
         * How many kegs the player is currently balancing.
         *
         * Not persisted - a stack does not survive a logout any more than it survives a stumble.
         */
        val KEGS_BALANCED = AttributeKey<Int>()
    }
}
