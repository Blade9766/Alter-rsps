package org.alter.plugins.content.mechanics.ambience

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.AreaSound
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Plays the background sound sources declared in [AmbientSounds], re-spawning each one
 * on its own cadence so it reads as continuous ambience (see that file for why looping
 * isn't available and has to be faked).
 *
 * Runs off a single **world** timer rather than a per-player one. That distinction
 * matters: [AreaSound] is broadcast to every player who has the sound's chunk in view,
 * so driving it per-player the way [org.alter.plugins.content.mechanics.music.MusicPlugin]
 * drives music would make two players standing together each trigger the same sound,
 * and everyone nearby would hear it twice. World timers reuse the same
 * `timerPlugins` registry as pawn timers ([PluginRepository.executeWorldTimer]), so
 * [AMBIENT_TIMER] must never also be set on a pawn.
 *
 * Sounds are only spawned when someone is actually close enough to hear them, which
 * keeps an empty world from doing any per-tick broadcast work at all.
 */
class AmbientSoundPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    /**
     * Ticks elapsed since start-up, used to phase each source by its own interval.
     * Deliberately not reset - it only ever feeds a modulo.
     */
    private var ticks = 0

    init {
        onWorldInit {
            world.timers[AMBIENT_TIMER] = 1
        }

        onTimer(AMBIENT_TIMER) {
            ticks++
            AmbientSounds.sources.forEach { source ->
                if (ticks % source.intervalTicks == 0 && anyoneInEarshot(source)) {
                    world.spawn(AreaSound(source.tile, source.soundId, source.radius, source.volume))
                }
            }
            world.timers[AMBIENT_TIMER] = 1
        }
    }

    /**
     * A player slightly outside the sound's radius still needs it spawned, since they
     * may walk into range before the next re-spawn comes round - hence the margin.
     */
    private fun anyoneInEarshot(source: AmbientSound): Boolean =
        world.players.any { it.isOnline && it.tile.isWithinRadius(source.tile, source.radius + EARSHOT_MARGIN) }

    private companion object {
        val AMBIENT_TIMER = TimerKey()
        const val EARSHOT_MARGIN = 8
    }
}
