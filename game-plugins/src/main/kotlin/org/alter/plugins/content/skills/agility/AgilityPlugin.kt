package org.alter.plugins.content.skills.agility

import dev.openrune.cache.CacheManager.getObject
import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.ForcedMovement
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM
import kotlin.math.abs
import kotlin.math.max

/**
 * Agility courses.
 *
 * Every obstacle is a [ForcedMovement] rather than a queued walk: obstacles deliberately span tiles
 * flagged as blocked (a tightrope is a rope strung over a gap in a roof), and
 * [org.alter.game.model.move.MovementQueue.cycle] cancels a queued route the moment a step fails its
 * collision check. Forced movement moves the player server-side and tells the client to animate the
 * slide, which is what the real obstacles do.
 *
 * Lap tracking lives in two session attributes rather than the save file, matching the live game:
 * clearing obstacles in order earns the course's lap bonus, and skipping one forfeits it until the
 * player starts a fresh lap at the first obstacle.
 */
class AgilityPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {

    init {
        loadService(AgilityService())

        onWorldInit {
            val service = world.getService(AgilityService::class.java) ?: return@onWorldInit

            service.courses.forEach { course ->
                course.obstacles.forEachIndexed { index, obstacle ->
                    obstacle.objectIds.forEach { objId ->
                        val option = getObject(objId).actions
                            .filterNotNull()
                            .firstOrNull { it.equals(obstacle.option, ignoreCase = true) }

                        if (option == null) {
                            Server.logger.warn {
                                "Agility obstacle ${obstacle.name} binds option ${obstacle.option} " +
                                    "but object $objId does not have it - skipping."
                            }
                            return@forEach
                        }

                        onObjOption(obj = objId, option = option) {
                            val obj = player.getInteractingGameObj()
                            player.queue { cross(this, player, obj, course, obstacle, index) }
                        }
                    }
                }
            }
        }
    }

    private suspend fun cross(
        task: QueueTask,
        player: Player,
        obj: GameObject,
        course: CourseEntry,
        obstacle: ObstacleEntry,
        index: Int,
    ) {
        val level = player.getSkills().getCurrentLevel(Skills.AGILITY)
        val required = max(course.level, obstacle.level)
        if (level < required) {
            player.message("You need an Agility level of $required to use this obstacle.")
            return
        }

        val destination = destinationFor(player, obj, obstacle)
        if (destination == null) {
            player.message("You can't get onto that from here.")
            return
        }

        val fail = obstacle.fail
        val failed = fail != null &&
            fail.chanceAt(level, required).let { it > 0 && player.world.random(1..100) <= it }
        val landing = if (failed) fail!!.tile.toTile() else destination

        player.faceTile(obj.tile)
        if (!failed) {
            obstacle.message?.let { player.message(it) }
        }

        val animation = if (failed) fail!!.animation else obstacle.animation
        if (animation != -1) {
            player.animate(animation)
        }

        moveAcross(task, player, landing, if (failed) fail!!.ticks else obstacle.ticks)

        if (failed) {
            val damage =
                if (fail!!.maxDamage <= fail.minDamage) {
                    fail.minDamage
                } else {
                    player.world.random(fail.minDamage..fail.maxDamage)
                }
            if (damage > 0) {
                player.hit(damage)
            }
            player.message(fail.message ?: "You lose your footing and fall.")
            resetLap(player)
            return
        }

        if (obstacle.experience > 0.0) {
            player.addXp(Skills.AGILITY, obstacle.experience)
        }
        awardLapProgress(player, course, index)
    }

    /**
     * Slides the player to [landing] over [ticks] cycles. [Player.forceMove] owns the lock for the
     * duration and drops it again once the movement resolves.
     */
    private suspend fun moveAcross(
        task: QueueTask,
        player: Player,
        landing: Tile,
        ticks: Int,
    ) {
        val source = player.tile
        val direction = Direction.between(source, landing)
        val angle = if (direction == Direction.NONE) Direction.SOUTH.angle else direction.angle

        // Client delays are 30ms units, so one walked tile per game cycle is 20 units per tick.
        val clientDuration = ticks * 20
        val movement =
            ForcedMovement.of(
                src = source,
                dst = landing,
                clientDuration1 = clientDuration,
                clientDuration2 = clientDuration,
                directionAngle = angle,
            )
        player.forceMove(task, movement, cycleDuration = ticks)
    }

    /**
     * Resolves where an obstacle drops the player, or `null` when it cannot be used from where they
     * are standing.
     */
    private fun destinationFor(
        player: Player,
        obj: GameObject,
        obstacle: ObstacleEntry,
    ): Tile? {
        val from = player.tile
        return when (obstacle.destination) {
            DestinationMode.TILE -> obstacle.end!!.toTile()

            DestinationMode.SPAN -> {
                val start = obstacle.start!!.toTile()
                val end = obstacle.end!!.toTile()
                // Cross towards whichever end is further away, so the obstacle works both ways.
                if (from.getDistance(start) <= from.getDistance(end)) end else start
            }

            DestinationMode.THROUGH -> {
                // Head straight through the obstacle, along the axis pointing from player to object.
                val direction = axisTowards(from, obj.tile) ?: return null
                Tile(
                    x = from.x + direction.getDeltaX() * obstacle.distance,
                    z = from.z + direction.getDeltaZ() * obstacle.distance,
                    height = from.height + obstacle.heightChange,
                )
            }
        }
    }

    /**
     * The cardinal direction from [from] to [towards], collapsed onto whichever axis dominates.
     *
     * The route finder can leave the player on a tile that is diagonal to the obstacle - beside a
     * pipe mouth rather than in front of it - and a diagonal crossing would carry them off sideways,
     * so the smaller component is dropped. `null` when the player is standing on the object itself.
     */
    private fun axisTowards(
        from: Tile,
        towards: Tile,
    ): Direction? {
        val deltaX = towards.x - from.x
        val deltaZ = towards.z - from.z
        return when {
            deltaX == 0 && deltaZ == 0 -> null
            abs(deltaX) > abs(deltaZ) -> if (deltaX > 0) Direction.EAST else Direction.WEST
            else -> if (deltaZ > 0) Direction.NORTH else Direction.SOUTH
        }
    }

    private fun awardLapProgress(
        player: Player,
        course: CourseEntry,
        index: Int,
    ) {
        val onThisCourse = player.attr[COURSE_ATTR] == course.name
        val progress = if (onThisCourse) player.attr[PROGRESS_ATTR] ?: -1 else -1

        when {
            index == 0 -> {
                player.attr[COURSE_ATTR] = course.name
                player.attr[PROGRESS_ATTR] = 0
            }
            progress == index - 1 -> player.attr[PROGRESS_ATTR] = index
            // Obstacle taken out of order - the lap bonus is forfeit until they start over.
            else -> resetLap(player)
        }

        if (index == course.obstacles.size - 1 && player.attr[PROGRESS_ATTR] == index) {
            if (course.lapExperience > 0.0) {
                player.addXp(Skills.AGILITY, course.lapExperience)
            }
            player.message("You complete a lap of the ${course.name}.")
            course.markOfGrace?.let { rollMarkOfGrace(player, it) }
            resetLap(player)
        }
    }

    /**
     * Rolls for a mark of grace on lap completion. The mark is owned by the player who earned it and
     * is cleaned up again if they never come back for it.
     */
    private fun rollMarkOfGrace(
        player: Player,
        cfg: MarkOfGrace,
    ) {
        if (player.timers.has(MARK_COOLDOWN_TIMER)) {
            return
        }
        if (player.world.random(1..cfg.outOf) > cfg.chance) {
            return
        }

        player.timers[MARK_COOLDOWN_TIMER] = cfg.cooldownTicks

        val spawn = cfg.tiles[player.world.random(cfg.tiles.size - 1)]
        val mark =
            GroundItem(
                item = getRSCM(cfg.item),
                amount = 1,
                tile = Tile(spawn[0], spawn[1], spawn[2]),
                owner = player,
            )

        val world = player.world
        world.spawn(mark)
        world.queue {
            wait(cfg.despawnTicks)
            if (world.isSpawned(mark)) {
                world.remove(mark)
            }
        }
    }

    private fun resetLap(player: Player) {
        player.attr.remove(COURSE_ATTR)
        player.attr.remove(PROGRESS_ATTR)
    }

    private fun List<Int>.toTile(): Tile = Tile(this[0], this[1], this[2])

    companion object {
        /** Name of the course the player is part-way around, if any. */
        private val COURSE_ATTR = AttributeKey<String>()

        /** Index of the last obstacle cleared in lap order on [COURSE_ATTR]. */
        private val PROGRESS_ATTR = AttributeKey<Int>()

        /** Blocks further mark of grace rolls until it lapses. */
        private val MARK_COOLDOWN_TIMER = TimerKey()
    }
}
