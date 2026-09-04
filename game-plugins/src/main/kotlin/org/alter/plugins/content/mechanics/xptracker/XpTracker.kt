package org.alter.plugins.content.mechanics.xptracker

import org.alter.api.Skills
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.model.skill.SkillSet
import org.alter.game.model.timer.TimerKey
import kotlin.math.roundToLong

/**
 * Session experience rates and lifetime time played.
 *
 * ## How the session is measured
 *
 * By difference, not by listening. There is no hook for "experience was gained" - `Player.addXp`
 * only calls out on a *level* increase (see
 * [org.alter.plugins.content.mechanics.levelup.LevelUpPlugin]) - so instead of intercepting every
 * gain, this takes a snapshot of all 23 skills at login and subtracts it whenever someone asks. The
 * snapshot is a plain non-persistent attribute; the numbers are only meaningful for the session that
 * produced them, and a rate carried over a logout would be a lie about a rate.
 *
 * That makes gains and rates exact rather than sampled, and costs nothing per tick.
 *
 * ## How time played is measured
 *
 * By a repeating one-minute timer that adds to a persistent counter, rather than by adding the
 * session length on logout. A logout hook does not run when the process dies or a connection drops,
 * and time played that silently loses whole sessions is worse than useless. The cost of the timer
 * approach is that the final partial minute of a session is never banked.
 */
object XpTracker {
    /** Experience in every skill as of login. Non-persistent - see the class comment. */
    private val SNAPSHOT = AttributeKey<DoubleArray>()

    /** `System.currentTimeMillis()` when [SNAPSHOT] was taken. */
    private val SESSION_START = AttributeKey<Long>()

    /** Lifetime time played, in game ticks, banked a minute at a time. */
    private val PLAYED_TICKS = AttributeKey<Int>(persistenceKey = "played_ticks")

    /** Drives the time-played counter. Non-persistent: it is re-armed at login. */
    val PLAYTIME_TIMER = TimerKey()

    /** One minute, in ticks. */
    const val PLAYTIME_INTERVAL = 100

    /** Attack through Construction. The skill array is sized 25, but only 23 are real skills. */
    const val TRACKED_SKILLS = Skills.CONSTRUCTION + 1

    /** Start (or restart) the session measurement from this player's current experience. */
    fun begin(player: Player) {
        player.attr[SNAPSHOT] = DoubleArray(TRACKED_SKILLS) { player.getSkills().getCurrentXp(it) }
        player.attr[SESSION_START] = System.currentTimeMillis()
    }

    /**
     * Experience gained in [skill] this session.
     *
     * Never negative: an admin dropping someone's experience should read as "no gain", not as a
     * negative rate.
     */
    fun gained(
        player: Player,
        skill: Int,
    ): Double {
        val snapshot = player.attr[SNAPSHOT] ?: return 0.0
        if (skill !in snapshot.indices) {
            return 0.0
        }
        return (player.getSkills().getCurrentXp(skill) - snapshot[skill]).coerceAtLeast(0.0)
    }

    /** Experience gained across every skill this session. */
    fun totalGained(player: Player): Double = (0 until TRACKED_SKILLS).sumOf { gained(player, it) }

    /** Every skill that gained experience this session, largest gain first. */
    fun gainedSkills(player: Player): List<Pair<Int, Double>> =
        (0 until TRACKED_SKILLS)
            .map { it to gained(player, it) }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }

    /** How long this session has been running, in milliseconds. */
    fun sessionMillis(player: Player): Long {
        val start = player.attr[SESSION_START] ?: return 0L
        return (System.currentTimeMillis() - start).coerceAtLeast(0L)
    }

    /** Lifetime time played, in ticks. */
    fun playedTicks(player: Player): Int = player.attr[PLAYED_TICKS] ?: 0

    /** Bank another [ticks] of time played. */
    fun addPlayed(
        player: Player,
        ticks: Int,
    ) {
        player.attr[PLAYED_TICKS] = playedTicks(player) + ticks
    }

    /**
     * The experience still needed to reach the next level in [skill], or null at 99.
     *
     * Measured from base experience, so a boosted or drained level does not move the target.
     */
    fun xpToNextLevel(
        player: Player,
        skill: Int,
    ): Double? {
        val level = player.getSkills().getBaseLevel(skill)
        if (level >= MAX_LEVEL) {
            return null
        }
        return SkillSet.getXpForLevel(level + 1) - player.getSkills().getCurrentXp(skill)
    }

    /**
     * [gained] experience over [millis], extrapolated to an hour.
     *
     * Returns null until [RATE_WARMUP_MS] has passed. A rate taken over the first few seconds of a
     * session is dominated by whatever happened to land in them and reads as nonsense - a single
     * shark cooked in the first two seconds is not 900k/hr.
     */
    fun perHour(
        gained: Double,
        millis: Long,
    ): Long? {
        if (millis < RATE_WARMUP_MS || gained <= 0.0) {
            return null
        }
        return (gained * MILLIS_PER_HOUR / millis).roundToLong()
    }

    /** How long [remaining] experience would take at [perHour], or null if it never arrives. */
    fun millisAtRate(
        remaining: Double,
        perHour: Long?,
    ): Long? {
        if (perHour == null || perHour <= 0L || remaining <= 0.0) {
            return null
        }
        return (remaining * MILLIS_PER_HOUR / perHour).roundToLong()
    }

    /** `4h 07m`, `12m 30s`, `45s`. Deliberately compact - these go in chatbox lines. */
    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
            minutes > 0 -> "${minutes}m ${seconds.toString().padStart(2, '0')}s"
            else -> "${seconds}s"
        }
    }

    /** Ticks are 600ms, so time played converts straight to a duration. */
    fun ticksToMillis(ticks: Int): Long = ticks.toLong() * TICK_MILLIS

    fun format(value: Double): String = String.format("%,d", value.roundToLong())

    fun format(value: Long): String = String.format("%,d", value)

    /**
     * The skill [name] refers to, or -1.
     *
     * [Skills.getSkillForName] only matches a full lowercase name, which makes `::xp wc` and
     * `::xp fish` fail for no good reason, so this falls back to a unique prefix and then a unique
     * substring. Ambiguous input resolves to nothing rather than to a guess.
     */
    fun skillForName(
        player: Player,
        name: String,
    ): Int {
        val query = name.trim().lowercase()
        if (query.isEmpty()) {
            return -1
        }
        val names = (0 until TRACKED_SKILLS).map { Skills.getSkillName(player.world, it).lowercase() }

        names.indexOf(query).let { if (it != -1) return it }

        val prefixed = names.withIndex().filter { it.value.startsWith(query) }
        if (prefixed.size == 1) {
            return prefixed.first().index
        }
        val contained = names.withIndex().filter { it.value.contains(query) }
        return if (contained.size == 1) contained.first().index else -1
    }

    private const val MAX_LEVEL = 99
    private const val TICK_MILLIS = 600L
    private const val MILLIS_PER_HOUR = 3_600_000.0

    /** Rates are suppressed until a session has run this long. */
    private const val RATE_WARMUP_MS = 30_000L
}
