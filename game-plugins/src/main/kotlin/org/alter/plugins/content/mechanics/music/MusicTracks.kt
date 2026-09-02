package org.alter.plugins.content.mechanics.music

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager
import dev.openrune.cache.filestore.definition.data.DBRowType
import java.nio.file.Files
import java.nio.file.Paths

data class MusicTrackEntry(
    /** The name the client renders, e.g. "The Heist". */
    val name: String,
    /** The cache/MIDI id `playSong` takes. */
    val id: Int,
    /**
     * The name the client *sorts* by, which is not the displayed one: leading articles
     * are moved to the end ("Heist, The", "Farmer's Grind, A"). Sorting by this is
     * what makes [MusicTracks.all] line up with the tab's actual row order.
     */
    val sortName: String = name,
    /** Unlock hint suffix, e.g. "in Lumbridge." */
    val hint: String = "",
)

/**
 * The client's real, ordered music track list, read straight out of the cache.
 *
 * **This supersedes the previous best-effort approach and the limitation that came
 * with it.** The master track list had been assumed undecodable here - an earlier
 * search for the displayed track titles across every *enum* and *struct* in the cache
 * came back empty, so the tab was driven off `data/cfg/music/new_music.json` (694
 * entries) with no way to confirm it matched what the client renders, which is what
 * made clicked rows resolve to the wrong track.
 *
 * The list was simply in a kind of cache definition that search never covered:
 * **DBTable [MUSIC_TABLE], as [DBRowType]s**, which this filestore library does decode
 * ([CacheManager.getDBRow]). It holds 812 rows - every title that search failed to
 * find, including "H.A.M. Fisted" and "The Heist", with no duplicate MIDI ids.
 * Since it comes from the same cache the client loads, the ordering is derived rather
 * than guessed.
 *
 * Columns: 0 = sort name, 1 = display name, 2 = unlock hint, 4 = MIDI id.
 *
 * Two caveats worth keeping honest:
 * - One row (id 3415) has a blank name and hint and is dropped as a placeholder,
 *   leaving 811 real tracks.
 * - The client had been observed showing 795. Nothing in this table cleanly filters
 *   811 down to that, so the client may hide a further handful for reasons not
 *   represented here. If rows still land off by a fixed amount in-game, that
 *   remaining filter is the thing left to find - but the names and ids themselves are
 *   now known-correct rather than approximated.
 */
object MusicTracks {
    private const val MUSIC_TABLE = 44

    /** DBRows have no bulk accessor, so ids are probed. Measured at ~27ms for the lot. */
    private const val MAX_DBROW_ID = 30000

    /** In the client's own row order: sorted by [MusicTrackEntry.sortName]. */
    val all: List<MusicTrackEntry> by lazy { loadFromCache().ifEmpty { loadFromJsonFallback() } }

    /** Same tracks, ordered by displayed name - used for `::music` name search. */
    val searchable: List<MusicTrackEntry> by lazy { all.sortedBy { it.name.lowercase() } }

    fun byId(id: Int): MusicTrackEntry? = all.firstOrNull { it.id == id }

    private fun loadFromCache(): List<MusicTrackEntry> {
        val rows = mutableListOf<DBRowType>()
        for (id in 0 until MAX_DBROW_ID) {
            val row = runCatching { CacheManager.getDBRow(id) }.getOrNull() ?: continue
            if (row.tableId == MUSIC_TABLE) rows.add(row)
        }

        return rows
            .mapNotNull { row ->
                val sortName = row.string(SORT_NAME_COLUMN) ?: return@mapNotNull null
                val name = row.string(DISPLAY_NAME_COLUMN) ?: return@mapNotNull null
                val midi = row.int(MIDI_COLUMN) ?: return@mapNotNull null
                // The single unnamed placeholder row is not something the client shows.
                if (sortName.isBlank() || name.isBlank()) return@mapNotNull null
                MusicTrackEntry(name = name, id = midi, sortName = sortName, hint = row.string(HINT_COLUMN) ?: "")
            }.sortedBy { it.sortName.lowercase() }
    }

    private fun DBRowType.value(column: Int): Any? = columnValues?.getOrNull(column)?.getOrNull(0)

    private fun DBRowType.string(column: Int): String? = value(column) as? String

    private fun DBRowType.int(column: Int): Int? = value(column) as? Int

    /**
     * Only reached if the cache has no [MUSIC_TABLE] at all (a very different cache
     * than the bundled one) - keeps the tab and `::music` working rather than going
     * silently empty.
     */
    private fun loadFromJsonFallback(): List<MusicTrackEntry> {
        val file = Paths.get("../data/cfg/music/new_music.json")
        if (!Files.exists(file)) {
            return emptyList()
        }
        val type = object : TypeToken<List<MusicTrackEntry>>() {}.type
        val loaded: List<MusicTrackEntry> = Files.newBufferedReader(file).use { Gson().fromJson(it, type) }
        return loaded.sortedBy { it.name.lowercase() }
    }

    private const val SORT_NAME_COLUMN = 0
    private const val DISPLAY_NAME_COLUMN = 1
    private const val HINT_COLUMN = 2
    private const val MIDI_COLUMN = 4
}
