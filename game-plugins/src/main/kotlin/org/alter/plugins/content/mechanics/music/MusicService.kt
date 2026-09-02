package org.alter.plugins.content.mechanics.music

import gg.rsmod.util.ServerProperties
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.service.Service
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Loads the region-id -> music-track-id mapping so [MusicPlugin] can play the right
 * track as a player walks into a new area.
 *
 * The backing file (`music_by_region.yaml`) is a flat list of `regionID`/`musicID`
 * pairs, so it's parsed by hand here rather than pulling in a YAML library dependency
 * just for this one simple, well-known shape.
 */
class MusicService : Service {
    private val regionToSong = Int2IntOpenHashMap().also { it.defaultReturnValue(-1) }

    override fun init(
        server: Server,
        world: World,
        serviceProperties: ServerProperties,
    ) {
        val file = Paths.get(serviceProperties.get("music-regions") ?: "../data/cfg/music/music_by_region.yaml")
        if (!Files.exists(file)) {
            Server.logger.warn { "Music region file not found at $file - area music will be disabled." }
            return
        }

        var pendingRegion: Int? = null
        Files.readAllLines(file).forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("- regionID:") -> pendingRegion = line.substringAfter(":").trim().toIntOrNull()
                line.startsWith("musicID:") -> {
                    val musicId = line.substringAfter(":").trim().toIntOrNull()
                    val regionId = pendingRegion
                    if (regionId != null && musicId != null) {
                        regionToSong.put(regionId, musicId)
                    }
                    pendingRegion = null
                }
            }
        }

        Server.logger.info { "Loaded ${regionToSong.size} region music mappings." }
    }

    /**
     * @return the music track id for [regionId], or -1 if that region has no music
     * mapping.
     */
    fun lookup(regionId: Int): Int = regionToSong.get(regionId)
}
