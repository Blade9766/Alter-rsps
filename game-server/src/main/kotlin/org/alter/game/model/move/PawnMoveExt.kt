package org.alter.game.model.move

import net.rsprot.protocol.game.outgoing.misc.player.SetMapFlag
import org.alter.game.info.NpcInfo
import org.alter.game.model.Tile
import org.alter.game.model.attr.CLIENT_KEY_COMBINATION
import org.alter.game.model.entity.*
import org.alter.game.model.move.MovementQueue.StepType
import org.alter.game.model.priv.Privilege
import org.alter.game.model.timer.FROZEN_TIMER
import org.alter.game.model.timer.STUN_TIMER
import org.rsmod.routefinder.Route
import org.rsmod.routefinder.RouteCoordinates
import java.util.*

/**
 * Used to teleport [Pawn] to [Tile],
 */
fun Pawn.moveTo(
    x: Int,
    y: Int,
    height: Int = 0,
) {
    tile = Tile(x, y, height)
    movementQueue.clear()

    if (entityType.isNpc) {
        NpcInfo(this as Npc).teleport(height, x, y, true)
    } else if (entityType.isPlayer) {
        (this as Player).avatar.extendedInfo.setTempMoveSpeed(127)
    }
}

fun Pawn.moveTo(tile: Tile) = moveTo(tile.x, tile.z, tile.height)

/**
 * @property x = xInBuildArea the x coordinate within the build area
 *  to render the map flag at.
 * @property y = zInBuildArea the z coordinate within the build area
 *  to render the map flag at.
 * If nothing gets passed, it will assign x to 255 and z to 255
 * which is used to remove the map flag.
 */
fun Pawn.setMapFlag(
    x: Int = 255,
    y: Int = 255,
) {
    if (this is Player) {
        write(SetMapFlag(x, y))
    }
}

/**
 * Whether this pawn is rooted - frozen or stunned - and so may not walk anywhere.
 *
 * The two timers are checked together everywhere they matter, and were previously checked in
 * different places from each other: [walkTo] knew about freezing only, and
 * [org.alter.game.model.move.ObjectPathAction] was the one caller that knew about both.
 */
fun Pawn.isRooted(): Boolean = timers.has(FROZEN_TIMER) || timers.has(STUN_TIMER)

/**
 * The message a player should be given for a walk [isRooted] refused, or `null` when they are
 * free to walk. Freezing is reported first, matching
 * [org.alter.game.model.move.ObjectPathAction].
 */
fun Pawn.rootedMessage(): String? =
    when {
        timers.has(FROZEN_TIMER) -> Entity.MAGIC_STOPS_YOU_FROM_MOVING
        timers.has(STUN_TIMER) -> Entity.YOURE_STUNNED
        else -> null
    }

/**
 * Walk to all the tiles specified in our [route] queue, using [stepType] as
 * the [MovementQueue.StepType].
 *
 *
 * Cancel out the walk logic when within @param [target]
 */
fun Pawn.walkRoute(
    route: Route,
    stepType: StepType,
) {
    walkRoute(route.toTileQueue(), stepType)
}

fun Pawn.walkRoute(
    path: Queue<Tile>,
    stepType: StepType,
) {
    /*
     * A rooted pawn - frozen or stunned - cannot walk, by any route.
     *
     * The check used to live only in [walkTo] (and there for freezing alone), which is just one
     * of the ways steps get queued - and not the one that mattered. `walkRoute` is what
     * [org.alter.game.model.move.PawnPathAction] and the combat loop
     * (`org.alter.plugins.content.combat.CombatPlugin.cycle`) call, so a frozen player still
     * chased their target around, and a duel fought under the "No Movement" rule - which is
     * held with the freeze timer - was not still at all. Jade bolts and the King Black Dragon's
     * freeze had the same hole: `Pawn.freeze` clears the movement queue once, and the next tick
     * of the victim's combat loop simply filled it again. `Pawn.stun` shares that shape exactly.
     *
     * Deliberately silent. Every caller that a *click* reaches reports [rootedMessage] itself
     * ([walkTo], [ObjectPathAction], [PawnPathAction], [GroundItemRouteAction]); the ones that
     * do not are automatic - the combat loop's chase and firemaking's step aside - and would
     * spam the chatbox once a tick.
     *
     * Applies to npcs as well as players, which is what makes a jade bolt actually root the
     * monster it procs on.
     */
    if (isRooted()) {
        stopMovement()
        setMapFlag()
        return
    }
    if (path.isEmpty()) {
        setMapFlag()
        return
    }
    movementQueue.clear()
    var tail: Tile? = null
    var next = path.poll()
    while (next != null) {
        movementQueue.addStep(next, stepType)
        interaction?.let {
            if (next.isWithinRadius(it.goal, it.range)) {
                if (this is Player) {
                    writeMessage("Stopped as goal was reached.")
                }
                stopMovement()
            }
        }
        val poll = path.poll()
        if (poll == null) {
            tail = next
        }
        next = poll
    }
    /*
     * If the tail is null (should never be unless we mess with code above), or
     * if the tail is the tile we're standing on, then we don't have to move at all!
     */
    if (tail == null || tail.sameAs(tile)) {
        setMapFlag()
        movementQueue.clear()
        return
    }
    if (this is Player && lastKnownRegionBase != null) {
        setMapFlag(tail.x - lastKnownRegionBase!!.x, tail.z - lastKnownRegionBase!!.z)
    }
}

fun Route.toTileQueue(): Queue<Tile> {
    return ArrayDeque(this.waypoints.map { Tile(it.x, it.z, it.level) })
}

fun Pawn.stopMovement() = movementQueue.clear()
fun Pawn.walkTo(tile: Tile, stepType: StepType = StepType.NORMAL) =
    walkTo(targetX = tile.x, targetY = tile.z, stepType = stepType)

fun Pawn.walkRoute(route: RouteCoordinates, stepType: StepType = StepType.NORMAL) {
    this.walkTo(Tile(route.x, route.z), stepType)
}

fun Pawn.walkTo(
    targetX: Int,
    targetY: Int,
    stepType: StepType = StepType.NORMAL,
) {
    if (this is Player) {
        if (!lock.canMove()) {
            /**
             * @TODO Add silent lock.
             */
            writeMessage("You are locked")
            return
        }
        /*
         * [walkRoute] below refuses a rooted pawn on its own; this branch is what turns that
         * refusal into the message and the cleared map flag a map click should produce. It only
         * knew about freezing, so a stunned player's map click walked them off regardless.
         */
        rootedMessage()?.let { message ->
            writeMessage(message)
            write(SetMapFlag(255, 255))
            return
        }
        this.closeInterfaceModal()
        this.interruptQueues()
        this.resetInteractions()
    }
    val route = world.smartRouteFinder.findRoute(
        level = tile.height,
        srcX = tile.x,
        srcZ = tile.z,
        destX = targetX,
        destZ = targetY,
    )
    if (attr[CLIENT_KEY_COMBINATION] == 2 && this is Player && world.privileges.isEligible(
            privilege,
            Privilege.ADMIN_POWER
        )
    ) {
        moveTo(Tile(targetX, targetY, tile.height))
        attr[CLIENT_KEY_COMBINATION] = 0
    } else {
        walkRoute(route.toTileQueue(), stepType)
    }
}

fun Pawn.hasMoveDestination(): Boolean = movementQueue.hasDestination()