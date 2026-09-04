package org.alter.plugins.content.areas.warriorsguild.activities

import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Area
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.areas.warriorsguild.WarriorsGuild

/**
 * Gamfred's catapult room on the first floor: stand in front of the catapult with a defensive
 * shield and block what it throws at you.
 *
 * Every [VOLLEY_CYCLES] ticks the catapult fires at each player in the room. A player holding the
 * shield blocks it, for [DEFENCE_XP] Defence experience and [TOKENS] warrior guild token; a player
 * without one takes [UNBLOCKED_DAMAGE] and gets nothing. The rate the wiki quotes - "750 tokens
 * and 7,500 defence experience per hour" - falls straight out of those numbers at an 8-tick
 * volley, which is what pins the interval.
 *
 * ## What is simplified, and why
 *
 * In OSRS the block is an **aim**: the catapult tells you which quadrant it is firing at and you
 * point the shield there, so a distracted player misses. That is an interface minigame - it needs
 * the shield's own targeting component and the client-side prompt - and none of that plumbing
 * exists here. Blocking is therefore modelled as "is the shield equipped", which keeps the reward
 * rate, the cost of turning up unequipped, and the shape of the activity, but not the attention it
 * demands. This is the single largest departure from the real guild in this package.
 */
class CatapultRoomPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onLogin {
            player.timers[VOLLEY_TIMER] = VOLLEY_CYCLES
        }

        onTimer(VOLLEY_TIMER) {
            val player = pawn as? Player ?: return@onTimer
            if (player.isOnline && inRoom(player)) {
                fire(player)
            }
            player.timers[VOLLEY_TIMER] = VOLLEY_CYCLES
        }

        /*
         * Gamfred hands out the shields, and does it for free and repeatedly - the shield is a
         * consumable in the sense that players drop it on the way out, not a reward to be earned.
         */
        onNpcOption(npc = GAMFRED, option = "claim-shield") {
            if (player.inventory.contains(DEFENSIVE_SHIELD) || player.hasEquipped(EquipmentType.SHIELD, DEFENSIVE_SHIELD)) {
                player.message("You already have a defensive shield.")
            } else if (player.inventory.add(DEFENSIVE_SHIELD, 1).hasSucceeded()) {
                player.message("Gamfred hands you a defensive shield.")
            } else {
                player.message("You do not have room for the shield.")
            }
        }
    }

    private fun inRoom(player: Player): Boolean = player.tile.height == PLANE && ROOM.contains(player.tile)

    private fun fire(player: Player) {
        if (player.hasEquipped(EquipmentType.SHIELD, DEFENSIVE_SHIELD)) {
            player.addXp(Skills.DEFENCE, DEFENCE_XP)
            player.inventory.add(WarriorsGuild.TOKEN, TOKENS)
            player.message("You block the incoming shot.")
            return
        }

        player.hit(UNBLOCKED_DAMAGE)
        player.message("The catapult's shot slams into you. You need a defensive shield to block it.")
    }

    private companion object {
        val VOLLEY_TIMER = TimerKey()

        /**
         * The room around the catapult at (2840, 3552, 1), reaching the door at (2842, 3542, 1).
         *
         * Object tiles from the cache dump; the bounds are drawn to enclose them.
         */
        val ROOM = Area(2836, 3544, 2848, 3556)

        const val PLANE = 1

        /** "The catapult fires every 8 ticks", which is what makes the wiki's hourly rates work. */
        const val VOLLEY_CYCLES = 8

        const val DEFENCE_XP = 10.0
        const val TOKENS = 1

        /** Enough to make standing there unequipped a bad idea, not enough to kill outright. */
        const val UNBLOCKED_DAMAGE = 5

        const val DEFENSIVE_SHIELD = "item.defensive_shield"
        const val GAMFRED = "npc.gamfred"
    }
}
