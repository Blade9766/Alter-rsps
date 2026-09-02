package org.alter.plugins.content.commands.commands.all

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.mechanics.music.MusicTracks
import org.alter.plugins.content.mechanics.music.MusicUnlocks

/**
 * ::music - search for and play any unlocked track by name.
 *
 * Unlike the native music tab (see MusicTabPlugin's doc comment for why that's only
 * best-effort), this always plays exactly the track picked, since it works by name
 * lookup instead of depending on matching the client's own row order.
 */
class MusicCommandPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onCommand("music", description = "Search for and play a music track by name") {
            player.queue(TaskPriority.STRONG) {
                searchAndPlay(player)
            }
        }
    }

    private suspend fun QueueTask.searchAndPlay(player: Player) {
        val query = inputString(player, "Enter a song name to search for").trim()
        if (query.isEmpty()) {
            return
        }

        val matches = MusicTracks.searchable.filter { it.name.contains(query, ignoreCase = true) }
        if (matches.isEmpty()) {
            player.message("No tracks found matching '$query'.")
            return
        }
        if (matches.size > 30) {
            player.message("${matches.size} tracks matched '$query' - please narrow your search.")
            return
        }

        val choice = pagedOptions(player, matches.map { it.name }, title = "Search results for '$query'")
        if (choice == -1) {
            return
        }
        val track = matches[choice - 1]

        if (!MusicUnlocks.isUnlocked(player, track.id)) {
            player.message("You haven't unlocked '${track.name}' yet - visit its area in-game to unlock it.")
            return
        }

        player.playSong(track.id)
        player.message("Now playing: ${track.name}.")
    }

    /**
     * Same paginated chatbox picker used by the Cheat Menu - the chatbox option list
     * only fits about 5 lines before entries get cut off with no way to scroll, so
     * anything longer needs "Next page"/"Previous page" entries instead.
     */
    private suspend fun QueueTask.pagedOptions(
        player: Player,
        items: List<String>,
        title: String,
        pageSize: Int = 3,
    ): Int {
        if (items.size <= 5) {
            return options(player, *items.toTypedArray(), title = title)
        }
        val totalPages = (items.size + pageSize - 1) / pageSize
        var page = 0
        while (true) {
            val start = page * pageSize
            val end = minOf(start + pageSize, items.size)
            val pageItems = items.subList(start, end).toMutableList()
            val itemCount = pageItems.size
            if (page > 0) pageItems.add("<< Previous page")
            if (page < totalPages - 1) pageItems.add("Next page >>")
            val choice = options(player, *pageItems.toTypedArray(), title = "$title (${page + 1}/$totalPages)")
            if (choice == -1) return -1
            val idx = choice - 1
            when {
                idx < itemCount -> return start + idx + 1
                page > 0 && idx == itemCount -> page--
                else -> page++
            }
        }
    }
}
