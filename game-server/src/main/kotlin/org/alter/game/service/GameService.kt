@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.alter.game.service

import gg.rsmod.util.ServerProperties
import gg.rsmod.util.concurrency.ThreadFactoryBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.alter.game.model.World
import org.alter.game.task.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import org.alter.game.saving.PlayerSaving

/**
 * The service used to schedule and execute logic needed for the game to run properly.
 *
 * @author Tom <rspsmods@gmail.com>
 */
class GameService : Service {
    /**
     * The associated world with our current game.
     */
    lateinit var world: World

    /**
     * The max amount of incoming [org.alter.game.message.Message]s that can be
     * handled per cycle.
     */
    var maxMessagesPerCycle = 0

    /**
     * The scheduler for our game cycle logic as well as coroutine dispatcher.
     */
    private val executor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            ThreadFactoryBuilder()
                .setNameFormat("game-context")
                .setUncaughtExceptionHandler { t, e -> logger.error(e) { "Error with thread $t" } }
                .build(),
        )

    /**
     * A list of jobs that will be executed on the next cycle after being
     * submitted.
     */
    private val gameThreadJobs = ConcurrentLinkedQueue<() -> Unit>()

    /**
     * Cycles until the next autosave.
     *
     * Saving only on logout meant any stop that was not a clean logout threw away everything since
     * the player logged in. This bounds that loss to [AUTOSAVE_INTERVAL_CYCLES] rather than to the
     * length of the session.
     */
    private var cyclesUntilAutosave = AUTOSAVE_INTERVAL_CYCLES

    /**
     * The amount of ticks that have gone by since the last debug log.
     */
    private var debugTick = 0

    /**
     * The total time, in milliseconds, that the past [TICKS_PER_DEBUG_LOG]
     * cycles have taken to complete.
     */
    private var cycleTime = 0

    /**
     * The Kotlin Coroutine dispatcher to submit suspendable plugins.
     */
    val dispatcher: CoroutineDispatcher = executor.asCoroutineDispatcher()

    /**
     * The amount of time, in milliseconds, that each [GameTask] has taken away
     * from the game cycle.
     */
    private val taskTimes = Object2LongOpenHashMap<Class<GameTask>>()

    /**
     * The amount of time, in milliseconds, that [PlayerCycleTask]
     * has taken for each [org.alter.game.model.entity.Player].
     */
    internal val playerTimes = Object2LongOpenHashMap<String>()

    /**
     * The amount of active [org.alter.game.model.queue.QueueTask]s throughout
     * the [org.alter.game.model.entity.Player]s.
     */
    internal var totalPlayerQueues = 0

    /**
     * The amount of active [org.alter.game.model.queue.QueueTask]s throughout
     * the [org.alter.game.model.entity.Npc]s.
     */
    internal var totalNpcQueues = 0

    /**
     * The amount of active [org.alter.game.model.queue.QueueTask]s throughout
     * the [org.alter.game.model.World].
     */
    internal var totalWorldQueues = 0

    /**
     * A list of tasks that will be executed per game cycle.
     */
    private val tasks = mutableListOf<GameTask>()

    /**
     * This flag indicates that the game cycles should pause.
     *
     * Should not be used without proper knowledge of how it works!
     */
    internal var pause = false

    override fun init(
        server: org.alter.game.Server,
        world: World,
        serviceProperties: ServerProperties,
    ) {
        this.world = world
        populateTasks()
        maxMessagesPerCycle = serviceProperties.getOrDefault("messages-per-cycle", 30)
    }

    override fun bindNet(
        server: org.alter.game.Server,
        world: World,
    ) {
        executor.scheduleAtFixedRate(this::cycle, 0, world.gameContext.cycleTime.toLong(), TimeUnit.MILLISECONDS)
    }

    private fun populateTasks() {
        tasks.addAll(
            arrayOf(
                MessageHandlerTask(),
                QueueHandlerTask(),
                NpcCycleTask(),
                PlayerCycleTask(),
                ChunkCreationTask(),
                WorldRemoveTask(),
                SequentialSynchronizationTask(),
            ),
        )
    }

    /**
     * Submits a job that must be performed on the game-thread.
     */
    fun submitGameThreadJob(job: Function0<Unit>) {
        gameThreadJobs.offer(job)
    }

    /**
     * The entry point handed to [executor]'s `scheduleAtFixedRate`, which silently
     * cancels every future execution as soon as the task throws - the throwable is
     * captured in the returned future, so the thread's uncaught-exception handler
     * never sees it either. One escaped [Throwable] would therefore kill the game
     * loop permanently without printing anything: the server keeps its port open and
     * keeps accepting logins, but goes mute afterwards, which from the client's side
     * is indistinguishable from "cannot log in". Nothing is allowed to escape here.
     */
    private fun cycle() {
        if (pause) {
            return
        }
        try {
            gameCycle()
        } catch (t: Throwable) {
            logger.error(t) { "Error during game cycle." }
        }
    }

    private fun gameCycle() {
        val start = System.currentTimeMillis()

        /*
         * Clear the time it has taken to complete [GameTask]s from last cycle.
         */
        taskTimes.clear()
        playerTimes.clear()

        /*
         * Execute any logic jobs that were submitted.
         *
         * Drained rather than iterated and then cleared: jobs are offered from other
         * threads - logins, most notably - and any job that landed in the queue while
         * this loop was running used to be thrown away unexecuted by the [clear] call,
         * which silently dropped that player's login.
         */
        while (true) {
            val job = gameThreadJobs.poll() ?: break
            try {
                job()
            } catch (t: Throwable) {
                logger.error(t) { "Error executing game-thread job." }
            }
        }

        /*
         * Go over the [tasks] and execute their logic. Log the time it took
         * each [GameTask] to complete. Some of the tasks may also calculate
         * their time for each player so that we can have the amount of time,
         * in milliseconds, that each player took to perform certain tasks.
         */
        tasks.forEach { task ->
            val taskStart = System.currentTimeMillis()
            try {
                task.execute(world, this)
            } catch (t: Throwable) {
                logger.error(t) { "Error with task ${task.javaClass.simpleName}." }
            }
            taskTimes[task.javaClass] = System.currentTimeMillis() - taskStart
        }
        try {
            world.cycle()
        } catch (t: Throwable) {
            logger.error(t) { "Error during world cycle." }
        }

        /*
         * Autosave. Nothing here is allowed to escape - a save that throws must not take the game
         * loop with it, since the loop dying leaves the server accepting logins but otherwise mute.
         */
        if (--cyclesUntilAutosave <= 0) {
            cyclesUntilAutosave = AUTOSAVE_INTERVAL_CYCLES
            try {
                val saved = PlayerSaving.saveAll(world)
                if (saved > 0) {
                    logger.info { "Autosaved $saved player(s)." }
                }
            } catch (t: Throwable) {
                logger.error(t) { "Error during autosave." }
            }
        }

        /*
         * Calculate the time, in milliseconds, it took for this cycle to complete
         * and add it to [cycleTime].
         */
        cycleTime += (System.currentTimeMillis() - start).toInt()

        if (debugTick++ >= TICKS_PER_DEBUG_LOG) {
            val freeMemory = Runtime.getRuntime().freeMemory()
            val totalMemory = Runtime.getRuntime().totalMemory()
            val maxMemory = Runtime.getRuntime().maxMemory()

            /*
             * Description:
             *
             * Cycle time:
             * the average time it took for a game cycle to
             * complete the last [TICKS_PER_DEBUG_LOG] game cycles.
             *
             * Entities:
             * The amount of entities in the world.
             * p: players
             * n: npcs
             *
             * Map:
             * The amount of map entities that are currently active.
             * c: chunks [org.alter.game.model.region.Chunk]
             * r: regions
             * i: instanced maps [org.alter.game.model.instance.InstancedMap]
             *
             * Queues:
             * The amount of plugins that are being executed on this exact
             * game cycle.
             * p: players
             * n: npcs
             * w: world
             *
             * Mem Usage:
             * Memory usage statistics.
             * U: used memory, in megabytes
             * R: reserved memory, in megabytes
             * M: max memory available, in megabytes
             */
            logger.info("[Cycle time: {}ms] [Entities: {}p / {}n] [Map: {}c / {}r / {}i] [Queues: {}p / {}n / {}w] [Mem usage: U={}MB / R={}MB / M={}MB].",
                   cycleTime / TICKS_PER_DEBUG_LOG, world.players.count(), world.npcs.count(),
                   world.chunks.getActiveChunkCount(), world.chunks.getActiveRegionCount(), world.instanceAllocator.activeMapCount,
                   totalPlayerQueues, totalNpcQueues, totalWorldQueues,
                   (totalMemory - freeMemory) / (1024 * 1024), totalMemory / (1024 * 1024), maxMemory / (1024 * 1024))
            debugTick = 0
            cycleTime = 0
        }

        val freeTime = world.gameContext.cycleTime - (System.currentTimeMillis() - start)
        if (freeTime < 0) {
            /**
             * @TODO
             * If the cycle took more than [GameContext.cycleTime]ms, we log the
             * occurrence as well as the time each [GameTask] took to complete,
             * as well as how long each [org.alter.game.model.entity.Player] took
             * to process this cycle.
             */
            logger.error { "Cycle took longer than expected: ${(-freeTime) + world.gameContext.cycleTime}ms / ${world.gameContext.cycleTime}ms!" }
            logger.error { taskTimes.toList().sortedByDescending { (_, value) -> value }.toMap() }
            logger.error { playerTimes.toList().sortedByDescending { (_, value) -> value }.toMap() }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * The amount of ticks that must go by for debug info to be logged.
         */
        private const val TICKS_PER_DEBUG_LOG = 10

        /**
         * How often every online player is written to disk, in game cycles. 600ms a cycle, so this
         * is every minute.
         *
         * This is the only thing standing between a player and losing their session, because the
         * shutdown hook cannot help on Windows: a JVM with no console can only be terminated
         * forcefully, which runs no hooks. A save is a handful of small documents per player, so
         * the frequency costs nothing worth measuring.
         */
        private const val AUTOSAVE_INTERVAL_CYCLES = 100
    }
}
