package org.alter.game.action

import dev.openrune.cache.CacheManager.getAnimOrDefault
import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.game.action.NpcDeathAction.reset
import org.alter.game.info.NpcInfo
import org.alter.game.model.LockState
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.AreaSound
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.model.move.stopMovement
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.model.weightedTableBuilder.roll
import org.alter.game.plugin.Plugin
import org.alter.game.service.log.LoggerService
import java.lang.ref.WeakReference

/**
 * This class is responsible for handling npc death events.
 *
 * @author Tom <rspsmods@gmail.com>
 */
object NpcDeathAction {
    private val logger = KotlinLogging.logger {}

    /**
     * How long to hold a skeletal death animation for, since the cache decoder never reads a
     * length for one. Three seconds: long enough to see any of them through, short enough not to
     * noticeably delay a respawn if a given one is briefer than that.
     */
    private const val SKELETAL_DEATH_CYCLES = 5

    var deathPlugin: Plugin.() -> Unit = {
        val npc = ctx as Npc
        if (!npc.world.plugins.executeNpcFullDeath(npc)) {
            npc.interruptQueues()
            npc.stopMovement()
            npc.lock()
            npc.queue(TaskPriority.STRONG) {
                death(npc)
            }
        }
    }

    suspend fun QueueTask.death(npc: Npc) {
        val world = npc.world
        val deathAnimation = npc.combatDef.deathAnimation
        val deathSound = npc.combatDef.defaultDeathSound
        val respawnDelay = npc.combatDef.respawnDelay
        var killer: Pawn? = null
        npc.damageMap.getMostDamage()?.let {
            if (it is Player) {
                killer = it
                world.getService(LoggerService::class.java, searchSubclasses = true)?.logNpcKill(it, npc)
            }
            npc.attr[KILLER_ATTR] = WeakReference(it)
        }
        NpcInfo(npc).setAllOpsInvisible()
        world.plugins.executeNpcPreDeath(npc)
        npc.resetFacePawn()
        if (deathSound > 0) {
            if (npc.combatDef.defaultDeathSoundArea) {
                world.spawn(AreaSound(npc.tile, deathSound, npc.combatDef.defaultDeathSoundRadius, npc.combatDef.defaultDeathSoundVolume))
            } else {
                (killer as? Player)?.playSound(deathSound, npc.combatDef.defaultDeathSoundVolume)
            }
        }

        /**
         * @TODO add interruption for this block if we would want to execute a plugin during it's death animation
         *
         * This loop sits between [NpcInfo.setAllOpsInvisible] above and
         * [org.alter.game.plugin.PluginRepository.executeNpcDeath] below, so anything that
         * throws here strands the npc mid-death: its options stay hidden, its drop plugin
         * never runs, and it never respawns. It had two ways to do that, both fixed here:
         *
         * 1. `getAnim` **throws** when an id is not in the cache. It is now
         *    [getAnimOrDefault], which returns a [dev.openrune.cache.filestore.definition.data.SequenceType]
         *    with `id = -1` instead; a missing animation is skipped and logged rather than
         *    killing the monster.
         * 2. [QueueTask.wait] **throws** on a non-positive argument
         *    (`check(cycles > 0)`), and a sequence with no computed length reports
         *    `cycleLength = 0`. That applied to real, present animations too, not just
         *    missing ones - hence the explicit guard rather than only the null-safe lookup.
         */
        deathAnimation.forEach { anim ->
            val def = getAnimOrDefault(anim)
            if (def.id < 0) {
                logger.warn { "Npc ${npc.id} has an unknown death animation and will skip it: $anim" }
                return@forEach
            }
            /*
             * `interruptable`, because death outranks everything. This runs from `hitsCycle`
             * on a tick where the npc may already have claimed its animation by swinging, and
             * without this the killing blow would leave it standing in its attack pose.
             */
            npc.animate(def.id, def.cycleLength, interruptable = true)
            /*
             * A *skeletal* sequence always reports `cycleLength = 0`, and that zero is an
             * artefact rather than a real length: `SequenceDecoder` only computes
             * `lengthInCycles` from the classic frame-list opcode, and never from the skeleton
             * data a skeletal animation stores its timing in. Waiting zero there cuts the
             * animation off the instant it starts - the npc snaps back to its spawn tile and
             * vanishes mid-death - which is what the remodelled bosses (Vet'ion and Calvar'ion,
             * whose whole animation set is skeletal) would otherwise do.
             *
             * The fallback is deliberately keyed on `skeletalId` rather than on the zero alone:
             * a non-skeletal sequence reporting zero genuinely has no frames to wait for, and
             * should still be skipped exactly as before.
             */
            val length = def.cycleLength.takeIf { it > 0 } ?: SKELETAL_DEATH_CYCLES.takeIf { def.skeletalId != -1 }
            if (length != null) {
                wait(length)
            }
        }
        world.plugins.executeNpcDeath(npc)
        world.plugins.anyNpcDeath.forEach {
            npc.executePlugin(it)
        }
        if (npc.respawns) {
            NpcInfo(npc).setInaccessible(true)
            npc.reset()
            wait(respawnDelay)
            NpcInfo(npc).setAllOpsVisible()
            NpcInfo(npc).setInaccessible(false)
            world.plugins.executeNpcSpawn(npc)
        } else {
            world.remove(npc)
        }
    }
    private fun Npc.reset() {
        lock = LockState.NONE
        moveTo(spawnTile)
        attr.clear()
        timers.clear()
        world.setNpcDefaults(this)
    }
}
