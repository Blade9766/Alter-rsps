package org.alter.plugins.content.areas.wilderness.lever

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.message
import org.alter.api.ext.options
import org.alter.api.ext.playSound
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.LockState
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.move.moveTo
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The teleport lever network: Edgeville and East Ardougne in, the Deserted Keep out.
 *
 * ## Finding the Keep
 *
 * The wiki gives no coordinates for any of the three, so all of them were located in this
 * project's own cache with `gradlew :game-server:agilityLocDump --args="lever"`. Edgeville
 * (26761) and Ardougne (1814) were unambiguous. The Keep took more work: a scan of the whole
 * Wilderness turned up exactly three levers north of level 50, two of which are the Mage Arena's
 * pair (9706 and 9707, beside the arena bank), leaving object **5959 at (3090, 3956)** - which
 * sits in a small walled room with cobwebs and a searchable sack, between the Mage Arena and the
 * Wilderness Agility Course. That matches the Keep's description on every point the wiki does
 * give, and nothing else in the Wilderness comes close, so that is what it is taken to be. The
 * one wrinkle is depth: the wiki calls the Keep level 50-54 and that tile computes to 55. A
 * single level at the edge of a room the wiki describes only in prose is not enough to outweigh
 * everything else lining up.
 *
 * ## Where you land
 *
 * Each destination is the far lever's *own* tile. All three are type-4 wall decorations, which do
 * not occupy the floor, so the tile is the open ground directly at the lever - which is where the
 * real game puts you, and, more usefully, is a tile guaranteed to exist rather than an offset
 * guessed against walls that are not in the loc dump.
 *
 * ## The return trip
 *
 * In the real game the Keep's lever goes to Ardougne, and only an easy Wilderness Diary unlocks a
 * right-click alternative back to Edgeville. This cache's copy of object 5959 carries a single
 * `Pull` action, so there is no second option to bind even if diaries existed here - and they do
 * not. Pulling it therefore asks which way you want to go. That is a deliberate deviation: the
 * alternative is a lever network where arriving from Edgeville strands you on the wrong side of
 * the map with no way back.
 */
class WildernessLeverPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onObjOption("object.lever_26761", "pull") {
            player.pullLever(DESERTED_KEEP, "You pull the lever and are teleported into the Wilderness.")
        }

        onObjOption("object.lever_1814", "pull") {
            player.pullLever(DESERTED_KEEP, "You pull the lever and are teleported into the Wilderness.")
        }

        onObjOption("object.lever_5959", "pull") {
            player.queue(TaskPriority.STRONG) {
                val picked = options(player, "East Ardougne", "Edgeville", title = "Where would you like to go?")
                val destination =
                    when (picked) {
                        1 -> ARDOUGNE
                        2 -> EDGEVILLE
                        else -> return@queue
                    }
                teleport(player, destination, "You pull the lever and are teleported out of the Wilderness.")
            }
        }
    }

    private fun org.alter.game.model.entity.Player.pullLever(
        destination: Tile,
        arrivalMessage: String,
    ) {
        queue(TaskPriority.STRONG) {
            teleport(this@pullLever, destination, arrivalMessage)
        }
    }

    /**
     * Pull, then go.
     *
     * The player is locked for the pull so they cannot walk out of the animation, and unlocked on
     * the far side. The two sounds are the pair `api/cfg/Sound` documents for this network
     * specifically: 2400 for the lever itself and 200 for the teleport.
     */
    private suspend fun org.alter.game.model.queue.QueueTask.teleport(
        player: org.alter.game.model.entity.Player,
        destination: Tile,
        arrivalMessage: String,
    ) {
        player.lock = LockState.FULL_WITH_DAMAGE_IMMUNITY
        player.animate(Animation.PULL_LEVER)
        player.playSound(LEVER_SOUND)
        wait(PULL_TICKS)

        player.graphic(Graphic.NORMAL_TELEPORT, height = 92)
        player.playSound(TELEPORT_SOUND)
        wait(TELEPORT_TICKS)

        player.moveTo(destination)
        player.animate(-1)
        player.unlock()
        player.message(arrivalMessage)
    }

    private companion object {
        /** Object 26761, in the ruin south of the Edgeville bank. */
        val EDGEVILLE = Tile(3090, 3475)

        /** Object 1814, north of East Ardougne's castle. */
        val ARDOUGNE = Tile(2561, 3311)

        /** Object 5959 - see this class' doc comment for how the Keep was identified. */
        val DESERTED_KEEP = Tile(3090, 3956)

        const val PULL_TICKS = 2
        const val TELEPORT_TICKS = 3

        /** Both documented in `api/cfg/Sound` under "Lever teleport". */
        const val LEVER_SOUND = 2400
        const val TELEPORT_SOUND = 200
    }
}
