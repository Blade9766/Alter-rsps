package org.alter.plugins.content.areas.wilderness.objs

import org.alter.api.cfg.Sound
import org.alter.api.ext.getInteractingGameObj
import org.alter.api.ext.message
import org.alter.api.ext.openDoor
import org.alter.api.ext.closeDoor
import org.alter.api.ext.playSound
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.move.walkTo
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * The gate into the Wilderness [Resource Area](https://oldschool.runescape.wiki/w/Resource_Area),
 * the walled enclosure of yews, magic trees, rocks and dark crab spots at (3175-3196, 3925-3944).
 *
 * The gate is object **26760** - the only object in the cache named `Gate` carrying an `Open/Peek`
 * pair, and the id the wiki's own infobox lists - and it is the one wilderness gate that is not a
 * two-leaf swing, so [org.alter.plugins.content.objects.gates.GatePlugin] cannot drive it: it is a
 * toll gate, and it is a *single* wall object.
 *
 * **The fee is a flat 7,500 coins.** The wiki's table drops it to 6,000 / 3,750 / free with the
 * medium / hard / elite Wilderness Diary; there is no diary framework in this project, so everyone
 * pays the undiscounted price, which is the safe direction to be wrong in. Leaving is free, as it
 * is in the real game - the toll is on the way in only.
 *
 * **Peek** reports how many *other* players are inside, which is exactly the scouting tool it is in
 * the real game: it is what stops the fee being a blind 7,500gp bet on an empty area. Peeking from
 * the inside gives the wiki's own line about the barren wasteland instead.
 *
 * **[OPENED_GATE] is an inference.** The cache has no opened counterpart next to 26760 (26759 is
 * `Artefacts` and 26761 is the Deserted Keep's lever), so the gate is swung onto `null_1553`, the
 * unnamed, actionless placeholder built from the same model (609) - the same shape of stand-in
 * `areas/lumbridge/objs/AlkharidGate` uses for its toll gates. If it ever looks wrong, [OPENED_GATE]
 * alone is the fix; nothing else depends on it.
 *
 * The gate shuts itself again after [OPEN_CYCLES]. Objects here are world state rather than
 * per-player, so a gate left open would be open for everyone and the toll would stop being a toll
 * the moment one player paid it.
 */
class ResourceAreaPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onObjOption(obj = GATE, option = "open", lineOfSightDistance = 1) {
            val gate = player.getInteractingGameObj()
            val inside = player.isInsideResourceArea()

            if (!inside && !player.payFee()) {
                return@onObjOption
            }

            world.queue {
                val opened = world.openDoor(gate, OPENED_GATE)
                player.playSound(Sound.OPEN_DOOR_SFX)
                player.walkTo(Tile(GATE_TILE.x, if (inside) GATE_TILE.z + 1 else GATE_TILE.z))
                wait(OPEN_CYCLES)
                world.closeDoor(opened, GATE)
            }
        }

        onObjOption(obj = GATE, option = "peek", lineOfSightDistance = 1) {
            if (player.isInsideResourceArea()) {
                player.message("All you see is the barren wasteland of the Wilderness.")
            } else {
                val inside = world.players.count { it.isOnline && it.isInsideResourceArea() }
                player.message(
                    when (inside) {
                        0 -> "You peek through the gate. There is nobody inside the resource area."
                        1 -> "You peek through the gate. There is 1 other adventurer inside the resource area."
                        else -> "You peek through the gate. There are $inside other adventurers inside the resource area."
                    },
                )
            }
        }
    }

    /**
     * Takes the toll, or explains why it cannot. Coins are checked and removed in one place so a
     * player can never be charged for a gate that then refuses to open.
     */
    private fun Player.payFee(): Boolean {
        if (inventory.getItemCount(getRSCM(COINS)) < FEE) {
            message("You need ${"%,d".format(FEE)} coins to enter the resource area.")
            return false
        }
        inventory.remove(COINS, FEE)
        message("You pay ${"%,d".format(FEE)} coins and enter the resource area.")
        return true
    }

    private fun Player.isInsideResourceArea(): Boolean =
        tile.height == 0 &&
            tile.x >= AREA_WEST && tile.x <= AREA_EAST &&
            tile.z >= AREA_SOUTH && tile.z <= AREA_NORTH

    private companion object {
        const val GATE = "object.gate_26760"

        /** Inferred - see this class's doc comment. */
        const val OPENED_GATE = "object.null_1553"

        const val COINS = "item.coins_995"

        /** The undiscounted entrance fee; every diary tier below elite pays it here. */
        const val FEE = 7_500

        /** Roughly 6 seconds - long enough to walk through, short enough to stay a toll gate. */
        const val OPEN_CYCLES = 10

        /** The gate itself, on the north wall of the enclosure. */
        val GATE_TILE = Tile(3184, 3944)

        /*
         * The enclosure's interior, read off the wiki's map polygon for the area
         * (3174,3944 3174,3925 3175,3924 3196,3924 3197,3925 3197,3944 3196,3945 3175,3945), which
         * traces the wall - the tiles inside it are one in from each corner.
         */
        const val AREA_WEST = 3175
        const val AREA_EAST = 3196
        const val AREA_SOUTH = 3925
        const val AREA_NORTH = 3944
    }
}
