package org.alter.plugins.content.mechanics.welcome

import org.alter.api.ChatMessageType
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.LAST_LOGIN_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.mechanics.xptracker.XpTracker
import org.alter.plugins.content.skills.slayer.Slayer
import java.util.concurrent.TimeUnit

/**
 * The handful of lines a player sees when they log in: when they were last here, how long they have
 * played, and what they left unfinished.
 *
 * `LAST_LOGIN_ATTR` has been declared in the engine's attribute list since the beginning and nothing
 * had ever written to it, so "you last logged in..." could never have been shown. This plugin is
 * what finally fills it in - it reads the previous value before overwriting it with now.
 *
 * ## Ordering
 *
 * `OSRSPlugin` sends "Welcome to <world>." from its own login hook. Login hooks run in the order the
 * plugin scanner found them, which is not something a plugin can pin down, so these lines may land
 * either side of it. They read correctly in either order, which is why none of them repeat the
 * greeting.
 *
 * The chatbox is the whole delivery mechanism here rather than the real game's welcome screen
 * (interface 378): that interface wants a set of varps this server does not populate, and half a
 * welcome screen is worse than a clear line of text.
 */
class LoginSummaryPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onLogin {
            val previous = player.attr[LAST_LOGIN_ATTR]?.toLongOrNull()
            player.attr[LAST_LOGIN_ATTR] = System.currentTimeMillis().toString()

            sendLastLogin(player, previous)
            sendTimePlayed(player)
            sendSlayerTask(player)

            if (previous == null) {
                player.message(
                    "Type ::xp to see your experience rates and ::kc for your kill counts.",
                    ChatMessageType.GAME_MESSAGE,
                )
            }
        }
    }

    private fun sendLastLogin(
        player: Player,
        previous: Long?,
    ) {
        if (previous == null) {
            return
        }

        /*
         * A clock that moved backwards - a corrected server clock, a save copied between machines -
         * would otherwise produce "you last logged in -3 days ago". Saying nothing is the honest
         * answer when the stored time is in the future.
         */
        val elapsed = System.currentTimeMillis() - previous
        if (elapsed < 0) {
            return
        }

        val days = TimeUnit.MILLISECONDS.toDays(elapsed)
        val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)

        val ago =
            when {
                days > 0 -> "$days ${plural(days, "day")} ago"
                hours > 0 -> "$hours ${plural(hours, "hour")} ago"
                minutes > 0 -> "$minutes ${plural(minutes, "minute")} ago"
                else -> "moments ago"
            }
        player.message("You last logged in $ago.", ChatMessageType.GAME_MESSAGE)
    }

    private fun sendTimePlayed(player: Player) {
        val ticks = XpTracker.playedTicks(player)
        if (ticks > 0) {
            player.message(
                "Time played: ${XpTracker.formatDuration(XpTracker.ticksToMillis(ticks))}.",
                ChatMessageType.GAME_MESSAGE,
            )
        }
    }

    private fun sendSlayerTask(player: Player) {
        val task = Slayer.taskName(player) ?: return
        val amount = Slayer.amount(player)
        if (amount <= 0) {
            return
        }
        player.message(
            "Slayer task: $amount ${task.lowercase()} remaining.",
            ChatMessageType.GAME_MESSAGE,
        )
    }

    private fun plural(
        value: Long,
        word: String,
    ): String = if (value == 1L) word else "${word}s"
}
