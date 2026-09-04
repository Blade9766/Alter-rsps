package org.alter.plugins.content.areas.warriorsguild

import org.alter.api.Skills
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.Tile
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.plugin.Plugin
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * The guild's doors and the token upkeep behind two of them.
 *
 * Four gates live here because they are all the same shape - a door that checks something before
 * it opens - and splitting them across four files would spread one mechanic thin:
 *
 * - **The front door** (2877, 3546) wants Attack + Strength of [WarriorsGuild.COMBINED_LEVEL], or
 *   99 in either. Base levels, because the wiki is explicit that boosts do not count.
 * - **The heavy doors** into the shot put room want [WarriorsGuild.SHOT_PUT_STRENGTH] Strength.
 * - **Kamfreena's two doors** and **Lorelai's one** want [WarriorsGuild.TOKENS_TO_ENTER] tokens,
 *   take [WarriorsGuild.TOKEN_DRAIN] on the way in, and keep taking that much every minute the
 *   player stays inside. Lorelai additionally wants a rune defender already earned.
 *
 * An **Attack cape** skips the token requirement entirely, and the drain with it - the guild's own
 * reward for the skill it exists to train.
 *
 * Every door's opened state is `id + 1`, which is this cache's convention and holds for all five
 * (24318→24319, 24306→24307, 24309→24310, 10043→10044, 15658→15659, 15660→15661), so
 * [org.alter.api.ext.openDoor]'s default is correct here and none is passed explicitly - unlike
 * the Taverley dusty key door, whose `id + 1` lands on a different door entirely.
 *
 * The doors **close themselves again** after [OPEN_CYCLES]. Objects are world state rather than
 * per-player, so a door left open would be open for everyone and stop gating the moment one
 * qualifying player walked through.
 */
class WarriorsGuildPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onObjOption(obj = WarriorsGuild.ENTRANCE_DOOR, option = "open", lineOfSightDistance = 1) {
            if (WarriorsGuild.meetsEntryRequirement(player)) {
                open(player)
            } else {
                player.message("You need an Attack and Strength level totalling ${WarriorsGuild.COMBINED_LEVEL},")
                player.message("or level ${WarriorsGuild.MASTERY_LEVEL} in either, to enter the Warriors' Guild.")
            }
        }

        WarriorsGuild.SHOT_PUT_DOORS.forEach { (door, _) ->
            onObjOption(obj = door, option = "open", lineOfSightDistance = 1) {
                if (player.getSkills().getBaseLevel(Skills.STRENGTH) >= WarriorsGuild.SHOT_PUT_STRENGTH) {
                    open(player)
                } else {
                    player.message("The door is far too heavy for you to shift.")
                    player.message("You need ${WarriorsGuild.SHOT_PUT_STRENGTH} Strength to open it.")
                }
            }
        }

        WarriorsGuild.TOP_FLOOR_DOORS.forEach { (door, _) ->
            onObjOption(obj = door, option = "open", lineOfSightDistance = 1) {
                enterCyclopsRoom(player, requireRuneDefender = false)
            }
        }

        onObjOption(obj = WarriorsGuild.BASEMENT_DOOR.first, option = "open", lineOfSightDistance = 1) {
            enterCyclopsRoom(player, requireRuneDefender = true)
        }

        onLogin {
            /*
             * A player who logs in already standing in a cyclops room - the usual way back after a
             * disconnect - still owes the upkeep, so the watcher is armed for everyone and simply
             * stops itself on the first tick for anyone who is not in one.
             */
            player.timers[ROOM_WATCH_TIMER] = 1
        }

        onTimer(ROOM_WATCH_TIMER) {
            val player = pawn as? Player ?: return@onTimer
            if (player.isOnline) {
                watchRoom(player)
            }
        }
    }

    /**
     * Runs every tick for every player, and does nothing at all for the overwhelming majority:
     * one area check, then out.
     *
     * A tick-rate watcher rather than a minute-rate one because the leave-and-re-enter rule the
     * defender ladder turns on needs *leaving* noticed promptly. Checking once a minute would let
     * a player step out and back in without the ladder ever seeing it.
     */
    private fun watchRoom(player: Player) {
        val inside = WarriorsGuild.inCyclopsRoom(player)
        val wasInside = player.attr[WarriorsGuild.IN_CYCLOPS_ROOM] == true

        if (!inside) {
            if (wasInside) {
                player.attr.remove(WarriorsGuild.IN_CYCLOPS_ROOM)
                player.timers.remove(WarriorsGuild.TOKEN_DRAIN_TIMER)
                DefenderLadder.leftRoom(player)
            }
            player.timers[ROOM_WATCH_TIMER] = 1
            return
        }

        if (!wasInside) {
            player.attr[WarriorsGuild.IN_CYCLOPS_ROOM] = true
            player.timers[WarriorsGuild.TOKEN_DRAIN_TIMER] = WarriorsGuild.TOKEN_DRAIN_CYCLES
        } else if (!player.timers.has(WarriorsGuild.TOKEN_DRAIN_TIMER)) {
            if (!chargeUpkeep(player)) {
                return
            }
            player.timers[WarriorsGuild.TOKEN_DRAIN_TIMER] = WarriorsGuild.TOKEN_DRAIN_CYCLES
        }

        player.timers[ROOM_WATCH_TIMER] = 1
    }

    /**
     * Takes the minute's tokens, or throws the player out when they cannot pay.
     *
     * Returns false once they have been removed, so the caller stops rescheduling - the exit
     * teleport puts them outside the room, and the next tick's area check tidies up the rest.
     */
    private fun chargeUpkeep(player: Player): Boolean {
        if (WarriorsGuild.bypassesTokens(player)) {
            return true
        }
        if (player.inventory.getItemCount(getRSCM(WarriorsGuild.TOKEN)) < WarriorsGuild.TOKEN_DRAIN) {
            WarriorsGuild.outOfTokens(player)
            ejectFromRoom(player)
            return false
        }
        player.inventory.remove(WarriorsGuild.TOKEN, WarriorsGuild.TOKEN_DRAIN)
        player.message("Some of your tokens crumble away.")
        return true
    }

    /**
     * Puts a player who has run out of tokens back on the safe side of the door they came in by.
     *
     * Chosen by which room they are standing in rather than by remembering how they entered, so it
     * still does the right thing for someone who logged in inside the room.
     */
    private fun ejectFromRoom(player: Player) {
        val exit =
            if (WarriorsGuild.inBasementRoom(player)) {
                BASEMENT_EXIT
            } else {
                TOP_FLOOR_EXIT
            }
        player.attr.remove(WarriorsGuild.IN_CYCLOPS_ROOM)
        player.timers.remove(WarriorsGuild.TOKEN_DRAIN_TIMER)
        DefenderLadder.leftRoom(player)
        player.moveTo(exit)
        player.timers[ROOM_WATCH_TIMER] = 1
    }

    private fun Plugin.enterCyclopsRoom(
        player: Player,
        requireRuneDefender: Boolean,
    ) {
        if (requireRuneDefender && !DefenderLadder.hasRuneDefender(player)) {
            player.message("You need to have earned a rune defender before Lorelai will let you through.")
            return
        }

        if (WarriorsGuild.bypassesTokens(player)) {
            player.message("Your Attack cape marks you out; you are waved through without paying.")
            open(player)
            return
        }

        val tokens = player.inventory.getItemCount(getRSCM(WarriorsGuild.TOKEN))
        if (tokens < WarriorsGuild.TOKENS_TO_ENTER) {
            player.message("You need at least ${WarriorsGuild.TOKENS_TO_ENTER} warrior guild tokens to enter.")
            return
        }

        player.inventory.remove(WarriorsGuild.TOKEN, WarriorsGuild.TOKEN_DRAIN)
        player.message("You hand over ${WarriorsGuild.TOKEN_DRAIN} tokens.")
        open(player)
    }

    private fun Plugin.open(player: Player) {
        val door = player.getInteractingGameObj()
        world.queue {
            val opened = world.openDoor(door)
            player.playSound(Sound.OPEN_DOOR_SFX)
            wait(OPEN_CYCLES)
            world.closeDoor(opened)
        }
    }

    private companion object {
        /** Reschedules itself every tick; see [watchRoom] for why it runs that often. */
        val ROOM_WATCH_TIMER = TimerKey()

        /** Long enough to walk through, short enough to stay a gate. */
        const val OPEN_CYCLES = 10

        /** Immediately west of Kamfreena's doors, on the landing she stands on. */
        val TOP_FLOOR_EXIT = Tile(2846, 3540, 2)

        /** Immediately west of Lorelai's door. */
        val BASEMENT_EXIT = Tile(2910, 9968, 0)
    }
}
