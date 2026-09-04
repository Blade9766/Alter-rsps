package org.alter.plugins.content.mechanics.xptracker

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * `::xp` - session experience, rates and time to level - and the one-minute timer that banks time
 * played.
 *
 * [XpTracker] holds the state and the arithmetic; this file is the wiring.
 */
class XpTrackerPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onLogin {
            XpTracker.begin(player)
            player.timers[XpTracker.PLAYTIME_TIMER] = XpTracker.PLAYTIME_INTERVAL
        }

        onTimer(XpTracker.PLAYTIME_TIMER) {
            player.timers[XpTracker.PLAYTIME_TIMER] = XpTracker.PLAYTIME_INTERVAL
            XpTracker.addPlayed(player, XpTracker.PLAYTIME_INTERVAL)
        }

        onCommand("xp", description = "Session experience rates - ::xp [skill|reset]") {
            val args = player.getCommandArgs()
            val query = args.joinToString(" ").trim()
            when {
                query.isEmpty() -> sendSummary(player)
                query.equals("reset", ignoreCase = true) -> {
                    XpTracker.begin(player)
                    player.message("Session experience tracking has been reset.")
                }
                else -> sendSkill(player, query)
            }
        }
    }

    private fun sendSummary(player: Player) {
        val millis = XpTracker.sessionMillis(player)
        val gained = XpTracker.totalGained(player)
        val session = XpTracker.formatDuration(millis)

        if (gained <= 0.0) {
            player.message("You haven't gained any experience this session (online $session).")
            sendTimePlayed(player)
            return
        }

        player.message("Session: $session. Experience gained: ${XpTracker.format(gained)}${rate(gained, millis)}.")
        val skills = XpTracker.gainedSkills(player)
        skills.take(SUMMARY_ROWS).forEach { (skill, amount) ->
            val name = Skills.getSkillName(world, skill)
            player.message("  $name: ${XpTracker.format(amount)}${rate(amount, millis)}")
        }
        if (skills.size > SUMMARY_ROWS) {
            player.message("  ...and ${skills.size - SUMMARY_ROWS} more skills.")
        }
        sendTimePlayed(player)
    }

    private fun sendSkill(
        player: Player,
        query: String,
    ) {
        val skill = XpTracker.skillForName(player, query)
        if (skill == -1) {
            player.message("No single skill matches '$query'.")
            return
        }

        val name = Skills.getSkillName(world, skill)
        val millis = XpTracker.sessionMillis(player)
        val gained = XpTracker.gained(player, skill)
        val perHour = XpTracker.perHour(gained, millis)

        player.message("$name: ${XpTracker.format(gained)} xp gained this session${rate(gained, millis)}.")

        val remaining = XpTracker.xpToNextLevel(player, skill)
        val level = player.getSkills().getBaseLevel(skill)
        if (remaining == null) {
            player.message("  Level $level - there is nothing left to train.")
            return
        }

        /*
         * The estimate is only offered when the player is actually training this skill right now.
         * Extrapolating from a rate of zero would print a time to level of forever, and
         * extrapolating from a rate earned an hour ago in a different activity would be worse - it
         * would look authoritative and be wrong.
         */
        val eta = XpTracker.millisAtRate(remaining, perHour)
        if (eta == null) {
            player.message("  Level $level, ${XpTracker.format(remaining)} xp to level ${level + 1}.")
        } else {
            player.message(
                "  Level $level, ${XpTracker.format(remaining)} xp to level ${level + 1} - " +
                    "about ${XpTracker.formatDuration(eta)} at this rate.",
            )
        }
    }

    private fun sendTimePlayed(player: Player) {
        val ticks = XpTracker.playedTicks(player)
        if (ticks > 0) {
            player.message("Time played: ${XpTracker.formatDuration(XpTracker.ticksToMillis(ticks))}.")
        }
    }

    /** ` (168,000/hr)`, or an empty string while the session is still too short to say. */
    private fun rate(
        gained: Double,
        millis: Long,
    ): String {
        val perHour = XpTracker.perHour(gained, millis) ?: return ""
        return " (${XpTracker.format(perHour)}/hr)"
    }

    private companion object {
        const val SUMMARY_ROWS = 10
    }
}
