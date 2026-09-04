package org.alter.plugins.content.skills.agility

import org.alter.game.model.Direction
import org.alter.game.model.Tile
import kotlin.math.abs

/**
 * Immutable configuration for one Agility course, loaded from `data/cfg/agility/courses.json`.
 *
 * A course is an ordered list of obstacles. Clearing them in order awards [lapExperience] on top of
 * each obstacle's own experience; taking them out of order still trains the individual obstacles but
 * forfeits the lap bonus, which is how the real courses behave.
 */
data class CourseEntry(
    val name: String,
    val level: Int = 1,
    val lapExperience: Double = 0.0,
    val markOfGrace: MarkOfGrace? = null,
    val obstacles: List<ObstacleEntry>,
) {
    init {
        require(name.isNotBlank()) { "Course must have a name." }
        require(level >= 1) { "Course level requirement must be >= 1." }
        require(lapExperience >= 0.0) { "Course lap experience cannot be negative." }
        require(obstacles.isNotEmpty()) { "Course '$name' must define at least one obstacle." }
        markOfGrace?.let { cfg ->
            require(cfg.chance in 1..cfg.outOf) { "Course '$name' mark chance must be within 1..${cfg.outOf}." }
            require(cfg.cooldownTicks >= 0) { "Course '$name' mark cooldown cannot be negative." }
            require(cfg.despawnTicks >= 1) { "Course '$name' mark despawn must be at least one cycle." }
            require(cfg.tiles.isNotEmpty()) { "Course '$name' must define mark spawn tiles." }
            require(cfg.tiles.all { it.size == 3 }) { "Course '$name' mark tiles must each be [x, z, height]." }
        }
    }
}

/**
 * Mark of grace spawning for a rooftop course.
 *
 * The roll happens once per completed lap and only after [cooldownTicks] have elapsed since the last
 * mark, matching the live game's "one mark, then a cooldown" behaviour. [chance] out of [outOf] is
 * written as a fraction rather than a percentage because that is how the rates are published - most
 * rooftop courses are 2 in 6.
 */
data class MarkOfGrace(
    val item: String = "item.mark_of_grace",
    val chance: Int,
    val outOf: Int = 100,
    val cooldownTicks: Int = 300,
    val despawnTicks: Int = 1000,
    val tiles: List<List<Int>>,
)

/**
 * How an obstacle decides where the player ends up.
 */
enum class DestinationMode {
    /**
     * The player passes *through* the obstacle: travel [ObstacleEntry.distance] tiles in the
     * direction that points from the player to the object. Used for nets, pipes and walls, and
     * works from either side without extra configuration.
     */
    THROUGH,

    /**
     * The player crosses *along* the obstacle between two fixed endpoints. Whichever of
     * [ObstacleEntry.start] / [ObstacleEntry.end] is further from the player is the destination, so
     * log balances and ropes can be crossed in both directions.
     */
    SPAN,

    /**
     * The player always ends on [ObstacleEntry.end], regardless of where they started.
     */
    TILE,
}

/**
 * One obstacle. Every crossing is performed as a [org.alter.game.model.ForcedMovement] rather than a
 * queued walk, because agility obstacles deliberately span tiles that are flagged as blocked - the
 * movement queue's own collision check would cancel the walk on the first step.
 */
data class ObstacleEntry(
    val name: String,
    val objects: List<String>,
    val option: String,
    val level: Int = 1,
    val experience: Double = 0.0,
    val destination: DestinationMode = DestinationMode.THROUGH,
    /** Tiles travelled, [DestinationMode.THROUGH] only. */
    val distance: Int = 1,
    /** Height levels gained (negative to descend). */
    val heightChange: Int = 0,
    /** `[x, z, height]`, required by [DestinationMode.SPAN]. */
    val start: List<Int>? = null,
    /** `[x, z, height]`, required by [DestinationMode.SPAN] and [DestinationMode.TILE]. */
    val end: List<Int>? = null,
    val animation: Int = -1,
    /** Game cycles the crossing takes; also drives how long the client animates the slide. */
    val ticks: Int = 1,
    val message: String? = null,
    val fail: ObstacleFail? = null,
) {
    @Transient
    var objectIds: IntArray = intArrayOf()

    init {
        require(name.isNotBlank()) { "Obstacle must have a name." }
        require(objects.isNotEmpty()) { "Obstacle '$name' must define at least one object id." }
        require(option.isNotBlank()) { "Obstacle '$name' must define the object option to bind." }
        require(level >= 1) { "Obstacle '$name' level requirement must be >= 1." }
        require(experience >= 0.0) { "Obstacle '$name' experience cannot be negative." }
        require(ticks >= 1) { "Obstacle '$name' must take at least one game cycle." }
        when (destination) {
            DestinationMode.THROUGH -> require(distance >= 1) {
                "Obstacle '$name' must travel at least one tile."
            }
            DestinationMode.SPAN -> {
                require(start.isTile()) { "Obstacle '$name' needs a 3-element 'start' tile." }
                require(end.isTile()) { "Obstacle '$name' needs a 3-element 'end' tile." }
            }
            DestinationMode.TILE -> require(end.isTile()) {
                "Obstacle '$name' needs a 3-element 'end' tile."
            }
        }
        fail?.let { cfg ->
            require(cfg.chance in 1..100) { "Obstacle '$name' fail chance must be between 1 and 100." }
            require(cfg.safeLevel == 0 || cfg.safeLevel > level) {
                "Obstacle '$name' fail safeLevel must be above its own level requirement."
            }
            require(cfg.ticks >= 1) { "Obstacle '$name' fall must take at least one game cycle." }
            require(cfg.minDamage >= 0) { "Obstacle '$name' fail damage cannot be negative." }
            require(cfg.maxDamage >= cfg.minDamage) { "Obstacle '$name' fail damage max cannot be below min." }
            require(cfg.tile.isTile()) { "Obstacle '$name' needs a 3-element fail 'tile'." }
        }
    }
}

/**
 * Where a player lands, and how hard, when they slip off an obstacle.
 *
 * [chance] is a percentage that applies at the obstacle's own level requirement and falls off
 * linearly to nothing at [safeLevel]. The wiki records *which* obstacles can be failed and the
 * damage they deal, but publishes no rate, so the curve is an approximation - the shape (riskiest
 * at the minimum level, safe well before 99) is the part worth keeping. Leave [safeLevel] at its
 * default to keep [chance] flat at every level.
 */
data class ObstacleFail(
    val chance: Int,
    val safeLevel: Int = 0,
    val minDamage: Int = 0,
    val maxDamage: Int = minDamage,
    val tile: List<Int>,
    val animation: Int = -1,
    /** Cycles the fall takes; a drop is quicker than the crossing it interrupts. */
    val ticks: Int = 2,
    val message: String? = null,
) {
    /**
     * The chance, in percent, that a player of [level] slips off an obstacle whose own requirement
     * is [obstacleLevel].
     */
    fun chanceAt(
        level: Int,
        obstacleLevel: Int,
    ): Int {
        if (safeLevel <= obstacleLevel) {
            return chance
        }
        if (level >= safeLevel) {
            return 0
        }
        val remaining = safeLevel - level.coerceAtLeast(obstacleLevel)
        val span = safeLevel - obstacleLevel
        return (chance * remaining) / span
    }
}

private fun List<Int>?.isTile(): Boolean = this != null && size == 3

/**
 * The cardinal direction from [from] to [towards], collapsed onto whichever axis dominates.
 *
 * The route finder can leave the player on a tile that is diagonal to the obstacle - beside a pipe
 * mouth rather than in front of it - and a diagonal crossing would carry them off sideways, so the
 * smaller component is dropped. `null` when the player is standing on the object itself.
 */
internal fun axisTowards(
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

/**
 * The tile a [DestinationMode.THROUGH] obstacle would throw a player standing on [from] to when they
 * interact with a loc whose south-west tile is [objectTile]: straight through, [ObstacleEntry.distance]
 * tiles along the axis pointing at the object. `null` when the player is standing on the object itself.
 *
 * This is pure geometry and does not ask whether the result is somewhere a player can be - see
 * [throughLandingFrom] for the guarded version the plugin actually uses.
 */
internal fun ObstacleEntry.rawThroughLandingFrom(
    from: Tile,
    objectTile: Tile,
): Tile? {
    val direction = axisTowards(from, objectTile) ?: return null
    return Tile(
        x = from.x + direction.getDeltaX() * distance,
        z = from.z + direction.getDeltaZ() * distance,
        height = from.height + heightChange,
    )
}

/**
 * Where a [DestinationMode.THROUGH] obstacle drops a player standing on [from], or `null` when it
 * cannot be crossed from there.
 *
 * The landing depends on which side the player approached from, and the route finder is happy to
 * leave them at the *end* of a net or wall rather than in front of it. Projecting the crossing from
 * there aims it straight along the obstacle instead of through it, which lands the player inside the
 * scenery the obstacle is part of - stuck on a blocked tile with no obstacle to take them back out.
 * The Gnome Stronghold nets and pipes each have several such approaches.
 *
 * So the landing is only offered when [canStandOn] accepts it; otherwise the caller reports that the
 * obstacle cannot be used from where the player is standing. `AgilityVerify` checks this holds for
 * every tile the engine can leave a player on.
 */
internal fun ObstacleEntry.throughLandingFrom(
    from: Tile,
    objectTile: Tile,
    canStandOn: (Tile) -> Boolean,
): Tile? = rawThroughLandingFrom(from, objectTile)?.takeIf(canStandOn)

/** `[x, z, height]` as authored in the config. */
internal fun List<Int>.toTile(): Tile = Tile(this[0], this[1], this[2])

/**
 * Where [this] obstacle drops a player standing on [from] who interacts with a loc whose south-west
 * tile is [objectTile], or `null` when it cannot be crossed from there.
 *
 * Shared with `AgilityVerify` so the course-connectivity check follows the same branch the player
 * does for each [DestinationMode], rather than a copy of it that can drift.
 */
internal fun ObstacleEntry.landingFrom(
    from: Tile,
    objectTile: Tile,
    canStandOn: (Tile) -> Boolean,
): Tile? =
    when (destination) {
        DestinationMode.TILE -> end!!.toTile()

        DestinationMode.SPAN -> {
            val spanStart = start!!.toTile()
            val spanEnd = end!!.toTile()
            // Cross towards whichever end is further away, so the obstacle works both ways.
            if (from.getDistance(spanStart) <= from.getDistance(spanEnd)) spanEnd else spanStart
        }

        DestinationMode.THROUGH -> throughLandingFrom(from, objectTile, canStandOn)
    }
