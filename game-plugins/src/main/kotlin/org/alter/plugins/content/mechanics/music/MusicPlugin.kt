package org.alter.plugins.content.mechanics.music

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Plays the area's music track automatically as a player walks into a new region,
 * mirroring how the OSRS music player works in the background, and marks that track
 * unlocked via [MusicUnlocks]. Hand-curated [MusicZones] (accurate, but only covering
 * areas someone's actually verified) take priority over the generic per-region
 * [MusicService] data (broad coverage, but not fully accurate - see [MusicZones]'s
 * doc comment). The clickable music tab itself is [MusicTabPlugin].
 */
class MusicPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        loadService(MusicService())

        onLogin {
            player.timers[MUSIC_CHECK_TIMER] = 1
        }

        onTimer(MUSIC_CHECK_TIMER) {
            val player = pawn as? Player ?: return@onTimer
            if (player.isOnline) {
                checkForAreaMusic(player)
            }
            player.timers[MUSIC_CHECK_TIMER] = MUSIC_CHECK_DELAY
        }
    }

    private fun checkForAreaMusic(player: Player) {
        val zone = MusicZones.lookup(player.tile)
        if (zone != null) {
            checkZoneMusic(player, zone)
            return
        }

        val service = world.getService(MusicService::class.java) ?: return
        val musicId = service.lookup(player.tile.regionId)
        if (musicId == -1) {
            return
        }
        if (player.attr[LAST_MUSIC_ID_ATTR] == musicId) {
            return
        }
        player.attr[LAST_MUSIC_ID_ATTR] = musicId

        MusicUnlocks.unlock(player, musicId)
        player.playSong(musicId)
    }

    /**
     * Real OSRS unlocks every track in a shuffle zone's pool as soon as you enter it
     * (not just whichever one happens to play), and doesn't restart the music just
     * because you're still standing in the same zone - only entering fresh, or
     * switching from a different zone/track, picks a new one.
     */
    private fun checkZoneMusic(
        player: Player,
        zone: MusicZone,
    ) {
        zone.trackIds.forEach { MusicUnlocks.unlock(player, it) }
        zone.alsoUnlock.forEach { MusicUnlocks.unlock(player, it) }

        if (player.attr[LAST_MUSIC_ID_ATTR] in zone.trackIds) {
            return
        }

        val musicId = zone.trackIds[world.random(zone.trackIds.size - 1)]
        player.attr[LAST_MUSIC_ID_ATTR] = musicId
        player.playSong(musicId)
    }

    private companion object {
        val MUSIC_CHECK_TIMER = TimerKey()
        val LAST_MUSIC_ID_ATTR = AttributeKey<Int>()
        const val MUSIC_CHECK_DELAY = 2
    }
}
