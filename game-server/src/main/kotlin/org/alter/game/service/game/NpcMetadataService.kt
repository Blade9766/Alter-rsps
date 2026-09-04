package org.alter.game.service.game

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import dev.openrune.cache.CacheManager.getNpcs
import gg.rsmod.util.ServerProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.game.Server
import org.alter.game.fs.DefinitionSet
import org.alter.game.model.World
import org.alter.game.service.Service
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Tom <rspsmods@gmail.com>
 */
class NpcMetadataService : Service {
    private lateinit var path: Path

    override fun init(
        server: Server,
        world: World,
        serviceProperties: ServerProperties,
    ) {
        path = Paths.get(serviceProperties.getOrDefault("path", "../data/cfg/npcs.csv"))
        if (!Files.exists(path)) {
            throw FileNotFoundException("Path does not exist. $path")
        }
        load(world.definitions)
        loadMonsterStats(
            world,
            Paths.get(serviceProperties.getOrDefault("stats-path", "../data/cfg/npcs/monsterStats.json")).toFile(),
        )
    }

    /**
     * Fills [World.npcStats] with the combat stats of every monster the wiki publishes and no
     * plugin declares.
     *
     * Nothing gave an npc combat stats except a hand-written `setCombatDef` block, of which there
     * are a few dozen, so every other attackable npc in the game spawned as
     * [org.alter.game.model.combat.NpcCombatDef.DEFAULT]: ten hitpoints, zero in all five combat
     * levels, no bonuses. `World.setNpcDefaults` now falls back to this table instead, which
     * leaves every existing plugin definition exactly as authoritative as it was.
     *
     * A missing or unreadable file is not fatal - the server starts, and the monsters go back to
     * being punching bags - because a bad config should not take the game down.
     */
    fun loadMonsterStats(
        world: World,
        file: File,
    ) {
        if (!file.exists()) {
            logger.info { "No monster stat config at ${file.path}; npcs fall back to the default combat definition." }
            return
        }
        val config =
            try {
                ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(file, MonsterStatsConfig::class.java)
            } catch (e: Exception) {
                logger.error(e) { "Could not load monster stats from ${file.path}." }
                return
            }

        var loaded = 0
        config.monsters.forEach { monster ->
            if (monster.id < 0) {
                return@forEach
            }
            world.npcStats[monster.id] = monster.toCombatDef()
            loaded++
        }
        logger.info { "Loaded combat stats for $loaded monsters from ${file.name}." }
    }

    private fun load(definitions: DefinitionSet) {
        val npcs = getNpcs()

        Files.newBufferedReader(path).use { reader ->
            for (line in reader.lineSequence()) {
                val parts = line.split(",", limit = 2)
                val id = parts.getOrNull(0)?.toIntOrNull() ?: continue
                val examine = parts.getOrNull(1)?.trim()?.removeSurrounding("\"") ?: ""

                npcs[id]?.examine = examine
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
