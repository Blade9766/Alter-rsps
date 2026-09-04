package org.alter.plugins.content.skills.strength

import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.WeaponType
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.ForcedMovement
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.collision.canOccupy
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.skills.agility.CourseTerrain
import org.alter.plugins.content.skills.agility.axisTowards

/**
 * The crossbow-and-grapple shortcuts - the closest thing Strength has to an obstacle course.
 *
 * These are the "forcing obstacles out of the way" the Strength skill page talks about: each one
 * wants a Strength level to haul yourself up the rope, an Agility level to make the crossing, and a
 * Ranged level to put the grapple where it needs to go. The requirement set is in
 * [GrappleShortcuts]; everything here is the crossing itself.
 *
 * Crossings are a [ForcedMovement], not a walk, for the same reason the Agility obstacles are: the
 * tiles between the two sides are walls and open water, and
 * [org.alter.game.model.move.MovementQueue.cycle] cancels a queued route the moment a step fails its
 * collision check.
 *
 * No grapple-swing animation is played. The player animation for it is not something the wiki
 * records and guessing an id shows the wrong action rather than none, so the crossing is the slide
 * alone until the real id is sourced - which is a one-line change here when it is.
 */
class GrapplePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        GrappleShortcuts.ALL.forEach { shortcut ->
            shortcut.objects.forEach { key ->
                onObjOption(obj = key, option = "grapple") {
                    val obj = player.getInteractingGameObj()
                    player.queue { cross(this, player, obj, shortcut) }
                }
            }
        }
    }

    private suspend fun cross(
        task: QueueTask,
        player: Player,
        obj: GameObject,
        shortcut: GrappleShortcut,
    ) {
        /*
         * Both halves of the kit are checked before any level is, so a player who has the levels
         * but left the grapple at home is told what is missing rather than what they lack.
         * "Any crossbow" is read off the weapon type rather than an id list - the cache's weapon
         * type is what decides whether something is a crossbow, and an id list would go stale the
         * next time one is added.
         */
        if (!player.hasWeaponType(WeaponType.CROSSBOW)) {
            player.message("You need to be wielding a crossbow to use this shortcut.")
            return
        }
        if (!player.hasEquipped(EquipmentType.AMMO, GrappleShortcuts.MITH_GRAPPLE)) {
            player.message("You need a mith grapple equipped to use this shortcut.")
            return
        }

        val skills = player.getSkills()
        val missing =
            when {
                skills.getCurrentLevel(Skills.AGILITY) < shortcut.agility -> "Agility" to shortcut.agility
                skills.getCurrentLevel(Skills.STRENGTH) < shortcut.strength -> "Strength" to shortcut.strength
                skills.getCurrentLevel(Skills.RANGED) < shortcut.ranged -> "Ranged" to shortcut.ranged
                else -> null
            }
        if (missing != null) {
            player.message("You need a ${missing.first} level of ${missing.second} to use this shortcut.")
            return
        }

        val landing = landingFor(player, obj, shortcut)
        if (landing == null) {
            player.message("You can't get across from here.")
            return
        }

        player.faceTile(obj.tile)
        player.message("You fire your grapple and swing across.")
        slide(task, player, landing, shortcut.ticks)
    }

    /**
     * The first tile past [obj] the player can stand on, along the axis they approached it from.
     *
     * The search starts on the far side of the object rather than at the player, so a wall's own
     * near side - which is perfectly walkable, and is where the player is standing - is never
     * mistaken for the far side. It stops at [GrappleShortcut.maxCrossing] tiles beyond the object,
     * which keeps a crossing that has been changed underneath us from flinging the player across
     * half a region.
     *
     * [CourseTerrain.hasFloor] is checked as well as `canOccupy` for the reason its own doc gives:
     * the collision map carries no flag at all for a height with nothing drawn at it, so open water
     * on a plane that was never built reads as free.
     */
    private fun landingFor(
        player: Player,
        obj: GameObject,
        shortcut: GrappleShortcut,
    ): Tile? {
        val direction = axisTowards(player.tile, obj.tile) ?: return null
        val objectSize = maxOf(objectSizeAlong(obj, direction), 1)

        /*
         * How far along the axis the object's far edge sits. Objects are placed by their south-west
         * tile, so approaching from the north or the east the near edge is the far one in world
         * coordinates and the size does not need adding.
         */
        val toObject =
            when (direction) {
                Direction.NORTH -> obj.tile.z - player.tile.z + objectSize - 1
                Direction.SOUTH -> player.tile.z - obj.tile.z
                Direction.EAST -> obj.tile.x - player.tile.x + objectSize - 1
                Direction.WEST -> player.tile.x - obj.tile.x
                else -> return null
            }
        if (toObject < 0) {
            return null
        }

        for (step in toObject + 1..toObject + shortcut.maxCrossing) {
            val tile =
                Tile(
                    x = player.tile.x + direction.getDeltaX() * step,
                    z = player.tile.z + direction.getDeltaZ() * step,
                    height = player.tile.height,
                )
            if (player.world.collision.canOccupy(tile) && CourseTerrain.hasFloor(tile)) {
                return tile
            }
        }
        return null
    }

    private fun objectSizeAlong(
        obj: GameObject,
        direction: Direction,
    ): Int {
        val def = obj.getDef()
        return if (direction == Direction.NORTH || direction == Direction.SOUTH) def.sizeY else def.sizeX
    }

    /** Slides the player to [landing] over [ticks] cycles, the same way an Agility obstacle does. */
    private suspend fun slide(
        task: QueueTask,
        player: Player,
        landing: Tile,
        ticks: Int,
    ) {
        val source = player.tile
        val direction = Direction.between(source, landing)
        val angle = if (direction == Direction.NONE) Direction.SOUTH.angle else direction.angle

        // The two delays are arrival times, not durations - see AgilityPlugin.moveAcross for why
        // passing the same value for both leaves no window to slide in.
        val movement =
            ForcedMovement.of(
                src = source,
                dst = landing,
                clientDuration1 = 0,
                clientDuration2 = ticks * CLIENT_CYCLES_PER_TICK,
                directionAngle = angle,
            )
        player.forceMove(task, movement, cycleDuration = ticks)
    }

    private companion object {
        /** Client cycles are 20ms, a game cycle 600ms. */
        private const val CLIENT_CYCLES_PER_TICK = 30
    }
}
