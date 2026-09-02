package org.alter.plugins.content.skills.firemaking

import org.alter.api.Skills
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.DynamicObject
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.game.model.move.MovementQueue.StepType
import org.alter.game.model.move.setMapFlag
import org.alter.game.model.move.walkRoute
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM
import org.rsmod.routefinder.Route

/**
 * Firemaking: use a tinderbox on logs to light a fire for XP.
 *
 * As with [org.alter.plugins.content.skills.woodcutting.WoodcuttingPlugin]'s chop
 * chance, the exact per-attempt lighting success formula Jagex uses isn't public, so
 * [lightChance] is a labeled approximation, not a reproduction. Level requirements and
 * XP per log are sourced from the OSRS Wiki. The wiki also states a lit fire's burn
 * duration is "always unpredictable" (not documented precisely anywhere), so
 * [FIRE_DURATION_MIN]/[FIRE_DURATION_MAX] is a reasonable random range rather than an
 * exact figure; burnt-out fires leaving ashes behind is accurate.
 *
 * Not yet implemented: lighting multiple logs in a row automatically while walking
 * backwards (the classic "trail of fires"). Right now each click lights one fire.
 */
class FiremakingPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    private data class LogEntry(val log: String, val level: Int, val experience: Double) {
        val logId: Int by lazy { getRSCM(log) }
    }

    private val logEntries =
        listOf(
            LogEntry("item.logs", 1, 40.0),
            LogEntry("item.oak_logs", 15, 60.0),
            LogEntry("item.willow_logs", 30, 90.0),
            LogEntry("item.maple_logs", 45, 135.0),
            LogEntry("item.yew_logs", 60, 202.5),
            LogEntry("item.magic_logs", 75, 303.8),
        )

    /**
     * Tiles that currently have a fire burning on them, so a second fire can't be lit
     * on top of the first.
     */
    private val activeFires = HashSet<Tile>()

    init {
        logEntries.forEach { entry ->
            onItemOnItem("item.tinderbox", entry.log) {
                player.queue(TaskPriority.STANDARD) { lightFire(this, player, entry) }
            }
        }
    }

    private suspend fun lightFire(
        task: QueueTask,
        player: Player,
        entry: LogEntry,
    ) {
        val level = player.getSkills().getCurrentLevel(Skills.FIREMAKING)

        if (level < entry.level) {
            player.message("You need a Firemaking level of ${entry.level} to burn these logs.")
            return
        }

        if (!player.inventory.contains(entry.logId)) {
            return
        }

        if (player.tile in activeFires) {
            player.message("You can't light a fire here.")
            return
        }

        val tile = player.tile
        player.lock()
        try {
            player.animate(LIGHT_ANIMATION)
            player.playSound(Sound.FLINT)
            task.wait(3)

            if (player.world.randomDouble() > lightChance(level, entry)) {
                player.message("You attempt to light the logs, but fail.")
                return
            }

            player.inventory.remove(item = entry.logId, amount = 1)
            player.addXp(Skills.FIREMAKING, entry.experience)
            player.playSound(Sound.FIRE_LIT)
            player.message("The fire catches and the logs begin to burn.")

            // Worked out *before* the fire exists: spawning it puts collision on this
            // very tile, which would leave the route finder with a blocked source.
            val stepAside = findStepAsideRoute(player, tile)
            spawnFire(player.world, tile)
            stepAside?.let {
                player.walkRoute(it, StepType.NORMAL)
                // Real OSRS shows no destination marker for this automatic step.
                player.setMapFlag()
            }
        } finally {
            player.unlock()
        }
    }

    /**
     * The step the player takes off the tile they just lit.
     *
     * Direction order is the one the OSRS Wiki's Firemaking article gives: "they will
     * walk one step to the west if there is room there; otherwise, they will take one
     * step east. If both ways are blocked, the player will move south. If all three
     * ways are blocked, the player will travel north."
     *
     * Walkability is decided by the same route finder normal movement uses, rather
     * than a raw collision-flag test, so walls and objects are judged exactly the way
     * a real walk would judge them. Returns null when all four neighbours are blocked,
     * in which case the player just stays standing on their own fire - which is also
     * what happens in-game.
     */
    private fun findStepAsideRoute(
        player: Player,
        from: Tile,
    ): Route? {
        val candidates =
            listOf(
                Tile(from.x - 1, from.z, from.height), // west
                Tile(from.x + 1, from.z, from.height), // east
                Tile(from.x, from.z - 1, from.height), // south
                Tile(from.x, from.z + 1, from.height), // north
            )
        candidates.forEach { to ->
            val route =
                player.world.smartRouteFinder.findRoute(
                    level = from.height,
                    srcX = from.x,
                    srcZ = from.z,
                    destX = to.x,
                    destZ = to.z,
                )
            if (route.success && route.waypoints.isNotEmpty()) {
                return route
            }
        }
        return null
    }

    private fun lightChance(
        level: Int,
        entry: LogEntry,
    ): Double {
        val levelBonus = (level - entry.level).coerceAtLeast(0)
        return (0.6 + levelBonus * 0.02).coerceIn(0.6, 0.95)
    }

    private fun spawnFire(
        world: World,
        tile: Tile,
    ) {
        activeFires += tile
        val fire = DynamicObject(id = FIRE_OBJECT, type = 10, rot = 0, tile = tile)
        world.spawn(fire)

        world.queue {
            wait(world.random(FIRE_DURATION_MIN..FIRE_DURATION_MAX))
            if (world.isSpawned(fire)) {
                world.remove(fire)
            }
            activeFires -= tile
            world.spawn(GroundItem(item = getRSCM("item.ashes"), amount = 1, tile = tile))
        }
    }

    private companion object {
        const val LIGHT_ANIMATION = 733
        /**
         * The regular Firemaking fire: 1x1, animation 475, model 2260 in this cache,
         * and "id1 = Regular" on the wiki's Fire scenery infobox (26576/26575/20001/
         * 26186/20000 are the members-only blue/green/purple/red/white variants).
         *
         * Was 3769, which is also called "Fire" but is a **2x2** object built from a
         * different pair of models (3818/3811) - a campfire-sized asset, which is why
         * lit logs rendered as an oversized ring of stones with flat flame shapes. It
         * also occupied four tiles of collision instead of one.
         */
        const val FIRE_OBJECT = 26185
        const val FIRE_DURATION_MIN = 120
        const val FIRE_DURATION_MAX = 200
    }
}
