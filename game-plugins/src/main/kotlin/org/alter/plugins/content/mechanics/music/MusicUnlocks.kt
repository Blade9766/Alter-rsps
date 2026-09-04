package org.alter.plugins.content.mechanics.music

import org.alter.api.cfg.Song
import org.alter.api.ext.message
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.plugins.content.interfaces.gameframe.tabs.settings.Setting
import org.alter.plugins.content.interfaces.gameframe.tabs.settings.settingEnabled

/**
 * Tracks which music tracks a player has unlocked, keyed by the real music/track id
 * (the same id [org.alter.api.ext.playSong] takes) rather than any list position, so
 * this stays correct regardless of which track list ends up rendered in the tab.
 *
 * There's no existing bitset/varbit system in this codebase sized for hundreds of
 * individual flags (the [org.alter.game.model.bits.BitStorage] used elsewhere only
 * holds 32 bits per storage), so this is persisted as a compact comma-separated
 * string of unlocked ids instead of trying to force it into that system.
 */
object MusicUnlocks {
    private val UNLOCKED_MUSIC_ATTR = AttributeKey<String>(persistenceKey = "unlocked_music")

    /**
     * "Scape Main" (id 0) is unlocked automatically for everyone, matching real OSRS.
     */
    private const val DEFAULT_UNLOCKED = "0"

    fun isUnlocked(
        player: Player,
        musicId: Int,
    ): Boolean = musicId in read(player)

    fun unlock(
        player: Player,
        musicId: Int,
    ) {
        val current = read(player)
        if (musicId in current) {
            return
        }
        write(player, current + musicId)
        announce(player, musicId)
    }

    /**
     * The chatbox line the "Music unlock message" setting controls. The setting existed and was
     * togglable before this, but nothing ever read it, so it did nothing either way.
     */
    private fun announce(
        player: Player,
        musicId: Int,
    ) {
        if (!player.settingEnabled(Setting.MUSIC_UNLOCK_MESSAGE)) {
            return
        }
        val name = MusicTracks.all.firstOrNull { it.id == musicId }?.name ?: return
        player.message("<col=ff0000>You have unlocked a new music track: $name.</col>")
    }

    fun unlockAll(player: Player) {
        val allIds = MusicTracks.all.map { it.id }.toSet() + Song.values.map { it.id }.toSet()
        write(player, allIds)
    }

    /** For diagnosing unlock issues - how many tracks does this player currently have unlocked. */
    fun unlockedCount(player: Player): Int = read(player).size

    private fun read(player: Player): Set<Int> =
        (player.attr[UNLOCKED_MUSIC_ATTR] ?: DEFAULT_UNLOCKED)
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

    private fun write(
        player: Player,
        ids: Set<Int>,
    ) {
        player.attr[UNLOCKED_MUSIC_ATTR] = ids.sorted().joinToString(",")
    }
}
