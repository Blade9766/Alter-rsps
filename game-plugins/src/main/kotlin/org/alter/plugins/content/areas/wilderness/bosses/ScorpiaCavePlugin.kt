package org.alter.plugins.content.areas.wilderness.bosses

import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.move.moveTo
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The way in and out of the Scorpion Pit's cavern, where Scorpia lives.
 *
 * Both objects and both landing tiles came out of the cache. The `Cavern` (26762) sits at
 * (3231, 3951) behind a mouth of blocked tiles, with only (3230..3233, 3950) walkable in front of
 * it; the cave below is region 12961, whose floor is solid rock up to z 10331 and open from
 * z 10332 north. The `Crevice` (26763) at (3233, 10331) is the matching way back up.
 *
 * Two further crevices share that id deeper in the cave, at (3232, 10352) and (3243, 10352).
 * Binding by object id covers all three, and all three are sent to the same surface tile - they
 * are all ways out of the same pit, and being put at the pit mouth is the right answer from any
 * of them.
 */
class ScorpiaCavePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onObjOption("object.cavern", "enter") {
            player.moveTo(CAVE_ARRIVAL)
            player.message("You climb down into the darkness.")
        }

        onObjOption("object.crevice_26763", "use") {
            player.moveTo(SURFACE_ARRIVAL)
            player.message("You climb out of the cavern.")
        }
    }

    private companion object {
        /** First open row inside the cave, directly north of the crevice back up. */
        val CAVE_ARRIVAL = Tile(3233, 10332)

        /** The only walkable ground at the pit mouth, immediately south of the cavern. */
        val SURFACE_ARRIVAL = Tile(3232, 3950)
    }
}
