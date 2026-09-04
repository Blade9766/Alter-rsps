package org.alter.plugins.content.areas.wilderness.obelisk

import org.alter.api.cfg.Graphic
import org.alter.api.ext.getInteractingGameObj
import org.alter.api.ext.message
import org.alter.api.ext.options
import org.alter.api.ext.player
import org.alter.api.ext.playSound
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.DynamicObject
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.areas.wilderness.Obelisk
import org.alter.plugins.content.areas.wilderness.Wilderness
import org.alter.rscm.RSCM.getRSCM

/**
 * The six Wilderness obelisks and the network between them.
 *
 * Each obelisk is four pillars on the corners of a 5x5, all sharing one object id, with the
 * teleport pad being the 3x3 between them. Touching any pillar lights all four, and a moment later
 * everything standing on the pad goes - which is why this teleports a *box* rather than the one
 * player who clicked, and why an obelisk mid-activation ignores further clicks.
 *
 * The ids, the pillar tiles and therefore the centres all came out of a cache scan rather than the
 * wiki; see [Wilderness.OBELISKS].
 *
 * ## Destinations
 *
 * "Activate" sends you to a random *other* obelisk, which is the behaviour every account has. The
 * cache also carries "Set Destination" and "Teleport to Destination" on these objects - in the real
 * game those are gated behind the hard Wilderness diary. There is no diary framework in this
 * project, so they are bound and left ungated rather than dropped: an unbound option is a dead
 * right-click menu entry, which reads as broken content, whereas an ungated one reads as a server
 * that has not implemented diaries yet. The same reading `content/edgeville/npcs/stores` takes for
 * Oziach's quest gate.
 */
class WildernessObeliskPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Wilderness.OBELISKS.forEach { obelisk ->
            onObjOption(obelisk.obj, "activate") {
                activate(obelisk, destination = null)
            }

            onObjOption(obelisk.obj, "teleport to destination") {
                val chosen = player.attr[OBELISK_DESTINATION_ATTR]
                val destination = Wilderness.OBELISKS.firstOrNull { it.level == chosen }
                if (destination == null || destination.level == obelisk.level) {
                    player.message("You have not set a destination for this obelisk network.")
                    return@onObjOption
                }
                activate(obelisk, destination)
            }

            onObjOption(obelisk.obj, "set destination") {
                val choices = Wilderness.OBELISKS.filter { it.level != obelisk.level }
                player.queue(TaskPriority.STANDARD) {
                    val picked =
                        options(
                            player,
                            *choices.map { "Level ${it.level} Obelisk" }.toTypedArray(),
                            title = "Set this network's destination",
                        )
                    val destination = choices.getOrNull(picked - 1) ?: return@queue
                    player.attr[OBELISK_DESTINATION_ATTR] = destination.level
                    player.message("This obelisk network will now send you to the level ${destination.level} obelisk.")
                }
            }
        }
    }

    /**
     * Lights [obelisk]'s four pillars and, once they finish, moves everything on the pad.
     *
     * [destination] null means "somewhere random", which is the plain Activate behaviour. The
     * random pick is made when the pillars *finish*, not when they are lit, so it cannot be read
     * off the glow before committing to standing on the pad.
     */
    private fun org.alter.game.plugin.Plugin.activate(
        obelisk: Obelisk,
        destination: Obelisk?,
    ) {
        val clicked = player.getInteractingGameObj()
        if (world.attr[ACTIVE_OBELISKS_ATTR]?.contains(obelisk.level) == true) {
            return
        }
        markActive(obelisk, active = true)

        val pillars = pillarTiles(obelisk)
        val pillarType = clicked.type
        val pillarRot = clicked.rot

        /*
         * The sequence runs on the world's queue, not the clicking player's. An obelisk is shared
         * scenery: if this hung off the player, logging out or dying between the glow and the
         * teleport would strand all four pillars on the lit id and leave the obelisk marked busy
         * for the rest of the server's uptime.
         */
        world.queue {
            pillars.forEach { tile ->
                world.getObject(tile, pillarType)?.let { world.remove(it) }
                world.spawn(DynamicObject(id = GLOWING_PILLAR, type = pillarType, rot = pillarRot, tile = tile))
            }

            wait(ACTIVATION_TICKS)

            val target = destination ?: randomOther(obelisk)
            teleportPad(obelisk, target)

            pillars.forEach { tile ->
                world.getObject(tile, pillarType)?.let { world.remove(it) }
                world.spawn(DynamicObject(id = getRSCM(obelisk.obj), type = pillarType, rot = pillarRot, tile = tile))
            }
            markActive(obelisk, active = false)
        }
    }

    /** Everything standing on the 3x3 between the pillars, moved together. */
    private fun teleportPad(
        from: Obelisk,
        to: Obelisk,
    ) {
        val centre = from.centre
        world.players.forEach { player ->
            val onPad =
                player.tile.x in (centre.x - 1)..(centre.x + 1) &&
                    player.tile.z in (centre.z - 1)..(centre.z + 1) &&
                    player.tile.height == centre.height
            if (!onPad) {
                return@forEach
            }
            player.graphic(Graphic.WILDERNESS_OBELISK)
            player.playSound(TELEPORT_SOUND)
            player.moveTo(to.centre)
            player.message("Ancient magic teleports you somewhere in the Wilderness!")
        }
    }

    private fun randomOther(obelisk: Obelisk): Obelisk {
        val others = Wilderness.OBELISKS.filter { it.level != obelisk.level }
        return others[world.random(others.size - 1)]
    }

    /** The four corners of the 5x5 the obelisk occupies, from its centre. */
    private fun pillarTiles(obelisk: Obelisk): List<Tile> =
        listOf(
            obelisk.centre.transform(-2, -2),
            obelisk.centre.transform(-2, 2),
            obelisk.centre.transform(2, -2),
            obelisk.centre.transform(2, 2),
        )

    private fun markActive(
        obelisk: Obelisk,
        active: Boolean,
    ) {
        val levels = world.attr[ACTIVE_OBELISKS_ATTR] ?: HashSet<Int>().also { world.attr[ACTIVE_OBELISKS_ATTR] = it }
        if (active) levels.add(obelisk.level) else levels.remove(obelisk.level)
    }

    private companion object {
        /** Object 14825, the lit variant - the only obelisk id whose examine reads "It's glowing!". */
        val GLOWING_PILLAR = getRSCM("object.obelisk_14825")

        /** Ticks the pillars stay lit before the pad fires. */
        const val ACTIVATION_TICKS = 7

        /** The shared "teleport" sound the lever network uses too - see `api/cfg/Sound`. */
        const val TELEPORT_SOUND = 200

        /**
         * Which obelisks are mid-activation, so a second click cannot restart the sequence and
         * leave a pillar stuck on the lit id. Held on the world rather than the player because an
         * obelisk is shared - the player who lit it may well not be the one standing on the pad.
         */
        val ACTIVE_OBELISKS_ATTR = AttributeKey<HashSet<Int>>()

        /** The destination set through "Set Destination", as an obelisk level. */
        val OBELISK_DESTINATION_ATTR = AttributeKey<Int>(persistenceKey = "wilderness_obelisk_destination")
    }
}
