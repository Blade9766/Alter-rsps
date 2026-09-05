package org.alter.plugins.content.mechanics.follow

import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.FACING_PAWN_ATTR
import org.alter.game.model.attr.INTERACTING_PLAYER_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.model.move.MovementQueue.StepType
import org.alter.game.model.move.isRooted
import org.alter.game.model.move.stopMovement
import org.alter.game.model.move.walkRoute
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import java.util.LinkedList

/**
 * The player "Follow" option, sent to every client on login as option slot 3 by
 * `org.alter.plugins.content.OSRSPlugin`.
 *
 * The option was being sent and clicked correctly - it just had nothing bound to it. An unbound
 * player option falls through `PluginRepository.executePlayerOption`, which returns false, and
 * both `OpPlayerHandler`'s caller and `PawnPathAction` answer that with
 * [org.alter.game.model.entity.Entity.NOTHING_INTERESTING_HAPPENS]. "Follow" and "Attack" are the
 * two options the client shows on every player, so this was the more visible of the two holes.
 */
class FollowPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onPlayerOption(Follow.OPTION) {
            val target = player.attr[INTERACTING_PLAYER_ATTR]?.get() ?: return@onPlayerOption
            Follow.start(player, target)
        }
    }
}

object Follow {
    const val OPTION = "Follow"

    /**
     * How far the target may get before the follow gives up. A follow that outlives the target
     * leaving the follower's loaded map would keep pathfinding once a tick against a player who
     * is no longer really there, so it is cut off well inside that.
     */
    private const val MAX_FOLLOW_DISTANCE = 32

    /**
     * Starts [player] following [target] until something interrupts it.
     *
     * The follow does its own walking, once a tick, rather than being walked to the target by
     * [org.alter.game.model.move.PawnPathAction] the way "Trade with" and "Challenge" are - see
     * the routing note in `OpPlayerHandler`.
     */
    fun start(
        player: Player,
        target: Player,
    ) {
        if (player == target) {
            return
        }
        /*
         * Faced once, here, rather than every cycle: `facePawn` writes a face-entity update block
         * on each call, and the client holds the facing until it is told otherwise. Losing it is
         * also the loop's stop signal, so re-facing every tick would make the follow unstoppable.
         */
        player.facePawn(target)
        player.queue {
            while (true) {
                if (!cycle(player, target)) {
                    break
                }
                wait(1)
            }
        }
    }

    /**
     * One tick of the follow. Returns false to end it.
     */
    private fun cycle(
        player: Player,
        target: Player,
    ): Boolean {
        if (!target.isOnline || target.tile.height != player.tile.height) {
            return false
        }
        /*
         * The player stopped facing the target, which is what every "clicked somewhere else" path
         * leaves behind: `resetInteractions` calls `resetFacePawn`, and it is reached from
         * `walkTo` (a map click), from the next `OpPlayerHandler` click, and from combat starting.
         * The combat loop uses the same signal for the same reason - see `CombatPlugin.cycle`.
         *
         * A map click also runs `interruptQueues`, which terminates this task outright, so that
         * case never gets here. This catches the ones that only drop the facing.
         */
        if (player.attr[FACING_PAWN_ATTR]?.get() != target) {
            return false
        }
        if (player.tile.getDistance(target.tile) > MAX_FOLLOW_DISTANCE) {
            return false
        }

        val world = player.world
        val reached = world.reachStrategy.reached(
            flags = world.collision,
            level = player.tile.height,
            srcX = player.tile.x,
            srcZ = player.tile.z,
            destX = target.tile.x,
            destZ = target.tile.z,
            destWidth = target.getSize(),
            destLength = target.getSize(),
            srcSize = player.getSize(),
            locShape = -2,
        )
        if (reached && !player.tile.sameAs(target.tile)) {
            player.stopMovement()
            return true
        }
        /*
         * Frozen or stunned. `walkRoute` refuses a rooted pawn on its own and deliberately says
         * nothing about it, but this is the one caller that runs every tick, so the route is not
         * worth finding in the first place. The follow stays alive and resumes when the timer
         * runs out, which is what a frozen player watching their target walk away should see.
         */
        if (player.isRooted()) {
            return true
        }
        /*
         * Standing on the person being followed.
         *
         * Players do not block each other, so a follower who is caught up gets walked straight
         * through by a target doubling back, and both end up on one tile. Routing to the target
         * from there asks for a route to the tile already occupied, which is empty - so the
         * follower simply stayed on top of them, which is what "follows too closely" looks like.
         *
         * Stepping aside has to be done by pushing a tile onto the movement queue rather than
         * with `walkTo`: `walkTo` calls `interruptQueues()`, which would terminate the follow
         * task that is asking for the step.
         */
        if (player.tile.sameAs(target.tile)) {
            stepAside(player)?.let { player.walkRoute(LinkedList(listOf(it)), StepType.NORMAL) }
            return true
        }
        val route = world.smartRouteFinder.findRoute(
            level = player.tile.height,
            srcX = player.tile.x,
            srcZ = player.tile.z,
            destX = target.tile.x,
            destZ = target.tile.z,
            locShape = -2,
            destWidth = target.getSize(),
            destLength = target.getSize(),
        )
        player.walkRoute(route, StepType.NORMAL)
        return true
    }

    /**
     * The first orthogonally adjacent tile [player] can actually step onto, or null when it is
     * boxed in - in which case it stays put and tries again next tick, which is the same outcome
     * the combat chase settles for when every step towards its target is blocked.
     */
    private fun stepAside(player: Player): Tile? =
        Direction.NESW
            .firstOrNull { player.world.canTraverse(player.tile, it, player, player.getSize()) }
            ?.let { player.tile.step(it) }
}
