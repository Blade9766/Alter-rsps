package org.alter.plugins.content.commands.commands.developer

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR
import org.alter.game.model.attr.FACING_PAWN_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * `::npctrace [ticks]` - samples a nearby npc once per tick and reports what it actually did,
 * rather than what it looked like it did.
 *
 * `::npcinfo` gives a snapshot, and a snapshot cannot tell a genuinely walking npc from one
 * holding a movement queue it can never drain: both report `moving YES`. The difference is
 * whether the tile changes, and that only shows over time.
 *
 * Reads three things per tick - the tile, whether a step is still queued, and the facing
 * target that suppresses random walking - so the summary separates:
 *
 * - tile changes on most ticks: the npc really is walking, and the question is why so often.
 * - queue always set but the tile never changes: steps are being issued and rejected, so the
 *   client is being told to walk on the spot.
 * - neither: the npc is idle and whatever was reported is not movement at all.
 */
class NpctracePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onCommand("npctrace", Privilege.DEV_POWER, description = "Sample a nearby npc's movement for N ticks") {
            val args = player.getCommandArgs()
            val ticks = args.firstOrNull()?.toIntOrNull()?.coerceIn(2, MAX_TICKS) ?: DEFAULT_TICKS
            val target = targetOf(player)

            if (target == null) {
                player.message("No npc in combat with you, facing you, or within $SEARCH_RADIUS tiles.")
                return@onCommand
            }

            player.message("Tracing ${target.def.name} ${target.id} (index ${target.index}) for $ticks ticks...")

            player.queue {
                val samples = ArrayList<Sample>(ticks)
                repeat(ticks) {
                    samples.add(
                        Sample(
                            x = target.tile.x,
                            z = target.tile.z,
                            height = target.tile.height,
                            queued = target.hasMoveDestination(),
                            facing = target.attr[FACING_PAWN_ATTR]?.get() != null,
                        ),
                    )
                    wait(1)
                }

                val moves = samples.zipWithNext().count { (a, b) -> a.x != b.x || a.z != b.z }
                val queuedTicks = samples.count { it.queued }
                val facingTicks = samples.count { it.facing }
                val distinctTiles = samples.map { it.x to it.z }.distinct()

                player.message(
                    "moved on $moves/${samples.size - 1} ticks | queue set $queuedTicks/${samples.size} | " +
                        "facing a target $facingTicks/${samples.size} | ${distinctTiles.size} distinct tiles",
                )
                player.message(
                    "path: " + distinctTiles.joinToString(separator = " -> ", limit = PATH_LIMIT) { "${it.first},${it.second}" },
                )
                player.message(
                    when {
                        moves > 0 && distinctTiles.size > 1 -> "verdict: genuinely walking"
                        queuedTicks > 0 && distinctTiles.size == 1 -> "verdict: <col=ff0000>queue set but never leaves its tile - walking on the spot</col>"
                        else -> "verdict: idle, whatever you are seeing is not movement"
                    },
                )
            }
        }
    }

    private fun targetOf(player: Player): Npc? {
        (player.attr[COMBAT_TARGET_FOCUS_ATTR]?.get() as? Npc)?.let { return it }
        (player.attr[FACING_PAWN_ATTR]?.get() as? Npc)?.let { return it }

        var closest: Npc? = null
        var closestDistance = Int.MAX_VALUE
        world.npcs.forEach { npc ->
            if (npc.tile.height != player.tile.height) return@forEach
            val distance = npc.tile.getDistance(player.tile)
            if (distance <= SEARCH_RADIUS && distance < closestDistance) {
                closest = npc
                closestDistance = distance
            }
        }
        return closest
    }

    private data class Sample(
        val x: Int,
        val z: Int,
        val height: Int,
        val queued: Boolean,
        val facing: Boolean,
    )

    private companion object {
        const val SEARCH_RADIUS = 10
        const val DEFAULT_TICKS = 20
        const val MAX_TICKS = 60
        const val PATH_LIMIT = 12
    }
}
