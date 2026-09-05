package org.alter.plugins.content.objects.ladder

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.move.moveTo
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*

class LadderPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        /**Stairs*/

        val stairs =
            arrayOf(
                "object.staircase_16672",
                "object.staircase_16673",
                "object.staircase_16671",
            )

        stairs.forEach { stairs ->
            if (objHasOption(obj = stairs, option = "climb")) {
                onObjOption(obj = stairs, option = "climb") {
                    climbstairs(player)
                }
            }
            if (objHasOption(obj = stairs, option = "climb-up")) {
                onObjOption(obj = stairs, option = "climb-up") {
                    climbupstairs(player)
                }
            }
            if (objHasOption(obj = stairs, option = "climb-down")) {
                onObjOption(obj = stairs, option = "climb-down") {
                    climbdownstairs(player)
                }
            }
        }

        /**Ladders*/

        val ladders =
            arrayOf(
                "object.ladder_12964",
                "object.ladder_12965",
                "object.ladder_16683",
                "object.ladder_12966",
                "object.ladder_16679",
                "object.ladder_16684",
                // Lumber Yard, north-east of Varrock: 11794 climbs up from the yard floor
                // (3310,3509) and 11795 climbs back down from the floor above. 11802 is the
                // matching climb-down in the sawmill operator's hut at (3304,3494). All three are
                // plain vertical ladders with no quest logic of their own; they were simply never
                // in this list, which left the Lumber Yard's first floor - and Gertrude's cat -
                // unreachable. Ids from a location dump of region 13110.
                "object.ladder_11794",
                "object.ladder_11795",
                "object.ladder_11802",
            )

        ladders.forEach { ladder ->
            if (objHasOption(obj = ladder, option = "climb")) {
                onObjOption(obj = ladder, option = "climb") {
                    climbladder(player)
                }
            }
            if (objHasOption(obj = ladder, option = "climb-up")) {
                onObjOption(obj = ladder, option = "climb-up") {
                    climbupladder(player)
                }
            }
            if (objHasOption(obj = ladder, option = "climb-down")) {
                onObjOption(obj = ladder, option = "climb-down") {
                    climbdownladder(player)
                }
            }
        }

        /**Trapdoors.*/

        /*
         * NOT generically bound - and this is deliberate, after trying it and reverting.
         *
         * A cache scan finds **1,504 objects carrying a climb option** (457 named Ladder, 253
         * Staircase, 214 Stairs) against the twelve ids listed above, so almost every ladder in
         * the game answers "Nothing interesting happens". The climb helpers below are entirely
         * generic - one plane up or down on the same tile - so binding them to all 906 named
         * ladders looked like a one-line win.
         *
         * It is not, because **the destination is not derivable from the object**. The Taverley
         * dungeon ladder (16680, at 2884,3397) goes to 2884,9798 - a different region, not the
         * tile overhead. Binding it generically put the player one plane up, standing in mid-air
         * over Taverley with a black minimap.
         *
         * A `CollisionFlagMap.canOccupy` guard does **not** save it: collision flags mark
         * obstacles, not floors, so open sky on an allocated plane reads as perfectly walkable.
         * There is no "is there ground here" bit to test. That was tried too, and the player
         * floated again.
         *
         * What would actually work is deriving each ladder's real destination from the cache's
         * map data - a ladder with a counterpart directly above or below it is a genuine vertical
         * climb; one without needs a hand-written destination. That is a real piece of work and an
         * offline-generated table, not a runtime scan. Until then, ladders are added to the lists
         * above one at a time, which is slow but never puts anyone in the void.
         */

        onObjOption("object.trapdoor_14880", option = "climb-down") {
            climbTo(player, Tile(3210, 9616))
        }
        /*
         * Taverley Dungeon. The surface ladder is 16680 at (2884,3397); the ladder back up is
         * 17385 at (2884,9797). Both tiles, and the arrival tiles either side of them, were read
         * out of the cache's own map data and checked against its collision - see
         * `gradlew :game-server:locFind` and `:tileCheck` - rather than taken from memory.
         */
        onObjOption("object.ladder_16680", option = "climb-down") {
            climbTo(player, TAVERLEY_DUNGEON_ARRIVAL)
        }

        /*
         * **Object 17385 is placed in two different dungeons** - the Lumbridge cellar at
         * (3209,9616) and Taverley Dungeon at (2884,9797) - so a destination keyed on the id alone
         * is wrong for one of them. This used to send every climb of it to Lumbridge, which meant
         * that even once Taverley Dungeon was reachable, climbing out of it teleported you across
         * the map.
         *
         * The branch is on where the player actually is, not on the id. It is also the reason the
         * generic ladder binding above cannot work: ids are reused, so only a placement has a
         * destination.
         */
        onObjOption("object.ladder_17385", option = "climb-up") {
            if (player.tile.isWithinRadius(TAVERLEY_DUNGEON_LADDER, LADDER_MATCH_RADIUS)) {
                climbTo(player, TAVERLEY_SURFACE_ARRIVAL)
            } else {
                climbTo(player, Tile(3210, 3216))
            }
        }
    }

    private companion object {
        /** 17385's placement inside Taverley Dungeon, as opposed to the Lumbridge cellar one. */
        val TAVERLEY_DUNGEON_LADDER = Tile(2884, 9797)

        /** Arrival tiles, each verified standable with `gradlew :game-server:tileCheck`. */
        val TAVERLEY_DUNGEON_ARRIVAL = Tile(2884, 9798)
        val TAVERLEY_SURFACE_ARRIVAL = Tile(2884, 3398)

        /** Close enough to be *this* placement of a reused id rather than another one. */
        const val LADDER_MATCH_RADIUS = 4
    }

    /**
     * Climb to an explicit destination, with the animation.
     *
     * The ladders that go somewhere other than the tile overhead each called `moveTo` directly,
     * which teleports with no animation - the player simply blinked to the other end. The generic
     * [climbupladder]/[climbdownladder] helpers had always animated; only the explicit
     * destinations skipped it, so the more interesting ladders were the ones that looked worst.
     *
     * Same shape as those helpers: play the climb, hold the player still for the two ticks it
     * takes, then move. The lock matters - without it the player can walk out mid-animation and
     * still be moved when the wait expires.
     */
    private fun climbTo(
        player: Player,
        destination: Tile,
    ) {
        player.queue {
            player.animate(Animation.CLIMB_UP_LADDER)
            player.lock()
            wait(2)
            player.moveTo(destination.x, destination.z, destination.height)
            player.unlock()
        }
    }

    /**Function for ladders.*/

    fun climbupladder(player: Player) {
        player.queue {
            player.animate(828)
            player.lock()
            wait(2)
            player.moveTo(player.tile.x, player.tile.z, player.tile.height + 1)
            player.unlock()
        }
    }

    fun climbdownladder(player: Player) {
        player.queue {
            player.animate(828)
            player.lock()
            wait(2)
            player.moveTo(player.tile.x, player.tile.z, player.tile.height - 1)
            player.unlock()
        }
    }

    fun climbladder(player: Player) {
        player.queue {
            when (options(player, "Climb up the ladder.", "Climb down the ladder")) {
                1 -> climbupladder(player)
                2 -> climbdownladder(player)
            }
        }
    }

    /**Function for stairs.*/

    fun climbupstairs(player: Player) {
        player.moveTo(player.tile.x, player.tile.z, player.tile.height + 1)
    }

    fun climbdownstairs(player: Player) {
        player.moveTo(player.tile.x, player.tile.z, player.tile.height - 1)
    }

    fun climbstairs(player: Player) {
        player.queue {
            when (options(player, "Climb up the stairs.", "Climb down the stairs.")) {
                1 -> climbupstairs(player)
                2 -> climbdownstairs(player)
            }
        }
    }
}
