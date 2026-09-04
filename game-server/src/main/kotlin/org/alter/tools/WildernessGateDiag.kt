package org.alter.tools

import org.alter.game.model.attr.INTERACTING_OBJ_ATTR
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.Tile
import org.alter.game.model.entity.DynamicObject
import org.alter.game.model.entity.GameObject
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import net.rsprot.protocol.message.OutgoingGameMessage
import org.alter.game.model.entity.Player
import java.lang.ref.WeakReference
import java.nio.file.Paths

/**
 * TEMPORARY diagnostic - opens and closes every gate in the Wilderness against a booted world.
 *
 * The wilderness gates were unbound: `data/cfg/gates/gates.json` knew only the 1558/1560 pair and
 * the Emir's Arena gates, so clicking `Open` on any gate north of the ditch did nothing at all.
 *
 * Usage: gradlew :game-server:wildernessGateDiag
 */
object WildernessGateDiag {
    /** hinge tile, hinge id, extension tile, extension id, and the gate's name. */
    private val GATES =
        listOf(
            Triple(Tile(3008, 3849), 1727 to 1728, "Chaos Temple wall (west)"),
            Triple(Tile(3071, 3857), 1727 to 1728, "Level 44 fence (west)"),
            Triple(Tile(2948, 3904), 1727 to 1728, "Level 52 fence (far west)"),
            Triple(Tile(3202, 3856), 1727 to 1728, "Lava Maze fence"),
            Triple(Tile(3225, 3904), 1727 to 1728, "Level 52 fence (east)"),
            Triple(Tile(3337, 3896), 1727 to 1728, "Frozen Waste fence"),
            Triple(Tile(3075, 3867), 1568 to 1569, "Chaos Temple gate"),
            Triple(Tile(2998, 3931), 23552 to 23554, "Wilderness Agility Course (52 Agility)"),
        )

    @JvmStatic
    fun main(args: Array<String>) {
        val server = Server()
        server.startServer(apiProps = Paths.get("../data/api.yml"))
        val world =
            server.startGame(
                filestore = Paths.get("../data", "cache"),
                // args[0] lets the diagnostic boot on a spare port while a real server is running.
                gameProps = Paths.get(args.firstOrNull() ?: "../game.yml"),
                devProps = Paths.get("../dev-settings.yml"),
            )

        var failures = 0
        fun check(
            label: String,
            ok: Boolean,
        ) {
            println((if (ok) "  ok   " else "  FAIL ") + label)
            if (!ok) failures++
        }

        val messages = ArrayList<String>()

        fun client(agility: Int): Player =
            object : Player(world) {
                override fun write(vararg messages_: OutgoingGameMessage) {
                    messages_.forEach { if (it is MessageGame) messages.add(it.message) }
                }
            }.apply {
                username = "GateDiag"
                getSkills().setBaseLevel(16, agility)
            }

        // The agility gate is the only gate with a requirement; give it to the main tester.
        val player = client(99)

        fun click(
            obj: GameObject,
            opt: Int,
        ): Boolean {
            player.attr[INTERACTING_OBJ_ATTR] = WeakReference(obj)
            return world.plugins.executeObject(player, obj.id, opt)
        }

        /* Which way a wall faces, and so which way its gate swings: rot 0 west, 1 north, 2 east, 3 south. */
        fun outward(rot: Int): Pair<Int, Int> =
            when (rot) {
                0 -> -1 to 0
                1 -> 0 to 1
                2 -> 1 to 0
                else -> 0 to -1
            }

        GATES.forEach { (tile, ids, name) ->
            val (closedHinge, closedExtension) = ids
            println()
            println("=== $name - $closedHinge/$closedExtension at ${tile.x},${tile.z} ===")

            val hinge = world.getObject(tile, type = 0)
            if (hinge == null || hinge.id != closedHinge) {
                check("the closed gate stands at ${tile.x},${tile.z} (found ${hinge?.id})", false)
                return@forEach
            }
            val extension =
                (-1..1).flatMap { x -> (-1..1).map { z -> world.getObject(tile.transform(x, z), type = 0) } }
                    .firstOrNull { it?.id == closedExtension }
            if (extension == null) {
                check("its other leaf is next to it", false)
                return@forEach
            }

            val rot = hinge.rot
            val (dx, dz) = outward(rot)
            val hingeTile = tile.transform(dx, dz)
            val extensionTile = extension.tile.transform(dx, dz)

            check("Open is bound", click(hinge, 1))

            /*
             * The two leaves swing apart, each onto the tile directly outside its own closed one,
             * at opposite rotations so that each keeps its hinge end against its own post. Both
             * leaves stacked in a line - the rigid swing this replaced - fails these two checks.
             */
            val openHinge = world.getObject(hingeTile, type = 0)
            val openExtension = world.getObject(extensionTile, type = 0)
            check(
                "the hinge leaf swung to ${hingeTile.x},${hingeTile.z} rot ${(rot + 3) and 3} " +
                    "(found ${openHinge?.id} rot ${openHinge?.rot})",
                openHinge?.id == 1571 && openHinge.rot == ((rot + 3) and 3),
            )
            check(
                "the other leaf swung to ${extensionTile.x},${extensionTile.z} rot ${(rot + 1) and 3} " +
                    "(found ${openExtension?.id} rot ${openExtension?.rot})",
                openExtension?.id == 1572 && openExtension.rot == ((rot + 1) and 3),
            )
            check("and the gateway itself is clear", world.getObject(tile, type = 0) == null)

            if (openHinge != null) {
                check("Close is bound", click(openHinge, 1))
                check(
                    "the gate came back to $closedHinge at ${tile.x},${tile.z} rot $rot",
                    world.getObject(tile, type = 0).let { it?.id == closedHinge && it.rot == rot },
                )
                check(
                    "and to $closedExtension at ${extension.tile.x},${extension.tile.z}",
                    world.getObject(extension.tile, type = 0)?.id == closedExtension,
                )
            }
        }

        println()
        println("=== An open gate is walkable ===")
        run {
            val tile = Tile(3008, 3849)
            val gate = world.getObject(tile, type = 0)!!
            check("the shut gate blocks the way west", !world.canTraverse(tile, Direction.WEST, player))
            click(gate, 1)
            check("the open gate does not", world.canTraverse(tile, Direction.WEST, player))
            click(world.getObject(tile.transform(-1, 0), type = 0)!!, 1)
            check("and blocks again once shut", !world.canTraverse(tile, Direction.WEST, player))
        }

        println()
        println("=== Wilderness Agility Course gate refuses below 52 Agility ===")
        run {
            val tile = Tile(2998, 3931)
            val gate = world.getObject(tile, type = 0)!!
            val novice = client(51)
            messages.clear()
            novice.attr[INTERACTING_OBJ_ATTR] = WeakReference(gate)
            world.plugins.executeObject(novice, gate.id, 1)
            check("the gate stayed shut", world.getObject(tile, type = 0)?.id == 23552)
            check("and said why: ${messages.firstOrNull()}", messages.any { it.contains("Agility level 52") })
        }

        println()
        println("=== Resource Area gate 26760 at 3184,3944 ===")
        val resourceGate = world.getObject(Tile(3184, 3944), type = 0)
        check("the gate stands there (found ${resourceGate?.id})", resourceGate?.id == 26760)
        if (resourceGate != null) {
            messages.clear()
            check("Peek is bound", click(resourceGate, 2))
            check("and counts the area: ${messages.firstOrNull()}", messages.any { it.contains("resource area") })

            messages.clear()
            check("Open is bound", click(resourceGate, 1))
            check("a broke player is turned away", world.getObject(Tile(3184, 3944), type = 0)?.id == 26760)
            check("and told the price: ${messages.firstOrNull()}", messages.any { it.contains("7,500 coins") })

            player.inventory.add("item.coins_995", 10_000)
            messages.clear()
            click(resourceGate, 1)
            check(
                "paying leaves 2,500 coins, got ${player.inventory.getItemCount(995)}",
                player.inventory.getItemCount(995) == 2_500,
            )
            check("and says so: ${messages.firstOrNull()}", messages.any { it.contains("You pay") })

            /*
             * The plugin's own swing is deferred to a world queue and walks the player through, and
             * a synthetic Player has no playerInfo to walk with - so the swing is driven here the
             * way the plugin drives it, which is what proves the inferred opened id 1553 exists and
             * that closing puts 26760 back on its own tile at its own rotation.
             */
            // What org.alter.api.ext.openDoor does to a rot-1 wall; game-api is not on this
            // module's compile classpath, so the two lines are spelled out rather than called.
            val opened = DynamicObject(id = 1553, type = 0, rot = 2, tile = Tile(3184, 3945))
            world.remove(resourceGate)
            world.spawn(opened)
            check("null_1553 exists to swing onto", world.getObject(Tile(3184, 3945), type = 0)?.id == 1553)
            check("which clears the way south", world.canTraverse(Tile(3184, 3945), Direction.SOUTH, player))

            world.remove(opened)
            world.spawn(DynamicObject(id = 26760, type = 0, rot = 1, tile = Tile(3184, 3944)))
            val shut = world.getObject(Tile(3184, 3944), type = 0)
            check("and shuts back to 26760", shut?.id == 26760)
            check("blocking the way south again", !world.canTraverse(Tile(3184, 3945), Direction.SOUTH, player))
        }

        println()
        println(if (failures == 0) "All checks passed." else "$failures check(s) failed.")
        Runtime.getRuntime().halt(if (failures == 0) 0 else 1)
    }
}
