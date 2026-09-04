package org.alter.plugins.content.mechanics.killcount

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Kill counts: the counting, the boss announcement, and `::kc` to read the table back.
 *
 * [KillCounts] holds the state and the rules; this file is the wiring.
 */
class KillCountPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        /*
         * Bound on death rather than pre-death so a kill is only credited once the npc is actually
         * gone, matching how Slayer credits its own task counter.
         *
         * KILLER_ATTR is whoever landed the killing blow, so a monster finished off by someone else
         * counts for them and not for whoever did most of the damage. That is the same rule the rest
         * of the codebase uses to decide who a drop belongs to, so the kill count and the loot agree.
         */
        onAnyNpcDeath {
            val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return@onAnyNpcDeath
            val name = KillCounts.nameOf(npc) ?: return@onAnyNpcDeath
            val count = KillCounts.record(killer, name)

            if (KillCounts.isAnnounced(name)) {
                killer.message("Your $name kill count is: <col=ff0000>$count</col>.")
            }
        }

        onCommand("kc", description = "Look up your kill counts") {
            val query = player.getCommandArgs().joinToString(" ").trim()
            if (query.isEmpty()) {
                sendSummary(player)
            } else {
                sendSearch(player, query)
            }
        }

        /*
         * Wiping a kill count is not something a player should be able to do to themselves by
         * mistyping `::kc`, but it is needed to re-test the announcements, so it lives behind the
         * developer privilege instead.
         */
        onCommand("resetkc", powerRequired = Privilege.DEV_POWER, description = "Wipe your kill counts") {
            KillCounts.clear(player)
            player.message("Your kill counts have been cleared.")
        }
    }

    /**
     * The default `::kc` view: the total, then the [SUMMARY_ROWS] most-killed monsters.
     *
     * Capped because the chatbox scrollback is the only place this output goes; a player with a few
     * hundred tracked monsters would otherwise push everything else out of it.
     */
    private fun sendSummary(player: Player) {
        val counts = KillCounts.all(player)
        if (counts.isEmpty()) {
            player.message("You haven't killed anything yet.")
            return
        }

        val top = counts.toList().sortedByDescending { it.second }
        player.message("You have killed ${format(KillCounts.total(player))} monsters, of ${counts.size} kinds:")
        top.take(SUMMARY_ROWS).forEach { (name, count) ->
            player.message("  $name: <col=ff0000>${format(count)}</col>")
        }
        if (top.size > SUMMARY_ROWS) {
            player.message("  ...and ${top.size - SUMMARY_ROWS} more - try ::kc <name> to search.")
        }
    }

    private fun sendSearch(
        player: Player,
        query: String,
    ) {
        val matches = KillCounts.search(player, query)
        if (matches.isEmpty()) {
            player.message("You haven't killed anything matching '$query'.")
            return
        }
        matches.take(SEARCH_ROWS).forEach { (name, count) ->
            player.message("$name: <col=ff0000>${format(count)}</col>")
        }
        if (matches.size > SEARCH_ROWS) {
            player.message("...and ${matches.size - SEARCH_ROWS} more matches - narrow your search.")
        }
    }

    private fun format(value: Int): String = String.format("%,d", value)

    private companion object {
        const val SUMMARY_ROWS = 15
        const val SEARCH_ROWS = 10
    }
}
