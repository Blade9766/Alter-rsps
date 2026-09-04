package org.alter.plugins.content.areas.wilderness

import org.alter.api.InterfaceDestination
import org.alter.api.ext.getWildernessLevel
import org.alter.api.ext.closeInterface
import org.alter.api.ext.message
import org.alter.api.ext.openInterface
import org.alter.api.ext.setComponentHidden
import org.alter.api.ext.setComponentText
import org.alter.api.ext.pawn
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The Wilderness itself: the on-screen level overlay, the messages for crossing in and out and
 * past the level 30 fence, and the registration of its multi-combat zones.
 *
 * ## The overlay
 *
 * Interface **481** is the wilderness overlay, found by scanning every interface archive in the
 * cache for embedded text (`gradlew :game-server:interfaceTextDump`): it is the only one carrying
 * `Level: 0`, on component 42. Component 46 holds a `00:00` timer, which in the real game is the
 * Tele Block countdown - there is no Tele Block here, so it is hidden rather than left sitting at
 * zero.
 *
 * The overlay is put in [InterfaceDestination.OVERLAY], which nothing had ever used. That slot
 * being permanently empty is what made `Player.inWilderness()` - defined as "the overlay slot has
 * something in it" - permanently false, and with it PvP, the looting bag and wilderness-only
 * drops. That check reads the tile now, so the two can no longer drift apart; this plugin only
 * has to keep the *display* in step.
 *
 * ## Multi-combat
 *
 * The wilderness multi-combat areas are registered as chunks through the engine's existing
 * [org.alter.game.plugin.KotlinPlugin.setMultiCombatChunk], which means
 * [org.alter.api.ext.isMulti] answers for them and
 * [org.alter.plugins.content.mechanics.multi.MultiwayCombatPlugin] drives the client's icon with
 * no further help. Registering in the constructor rather than in `onWorldInit` is deliberate and
 * required: that plugin reads the chunk set *during* world init to bind its enter/exit hooks, and
 * every plugin's constructor has already run by then (`World.postLoad`).
 *
 * Chunks are 8x8, so the rectangles in [Wilderness.MULTI_AREAS] get rounded outwards to chunk
 * edges here. Given those rectangles are themselves approximate - see [Wilderness] - that
 * rounding costs nothing real, and it buys the whole thing for free off machinery that already
 * exists rather than a second, parallel multi-combat system.
 */
class WildernessPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Wilderness.MULTI_AREAS.forEach { area ->
            var x = area.bottomLeftX
            while (x <= area.topRightX) {
                var z = area.bottomLeftY
                while (z <= area.topRightY) {
                    setMultiCombatChunk(Tile(x, z).chunkCoords.hashCode())
                    z += CHUNK_SIZE
                }
                x += CHUNK_SIZE
            }
        }

        onLogin {
            /*
             * The attribute is deliberately not seeded here. It starts absent, so the first
             * refresh always counts as a change and re-sends the overlay - which is what a player
             * logging back in inside the Wilderness needs, since interfaces do not survive a
             * logout.
             */
            player.timers[WILDERNESS_TIMER] = 1
        }

        onTimer(WILDERNESS_TIMER) {
            val player = pawn as? Player ?: return@onTimer
            if (player.isOnline) {
                refresh(player)
            }
            player.timers[WILDERNESS_TIMER] = 1
        }
    }

    /**
     * Re-sends the overlay whenever the player's level actually changes. Runs every tick, but
     * does nothing at all on the overwhelming majority of them - the level is one shift and a
     * compare, and standing still or walking within a level is free.
     */
    private fun refresh(player: Player) {
        val level = player.tile.getWildernessLevel()
        val previous = player.attr[WILDERNESS_LEVEL_ATTR]
        if (previous == level) {
            return
        }
        player.attr[WILDERNESS_LEVEL_ATTR] = level

        if (level == 0) {
            // `previous == null` is a fresh login outside the Wilderness: nothing to close.
            if (previous != null) {
                leave(player)
            }
            return
        }

        if (previous == null || previous == 0) {
            enter(player)
        }

        player.setComponentText(OVERLAY_INTERFACE, LEVEL_COMPONENT, "Level: $level")

        if (previous != null && previous > 0) {
            announceFence(player, from = previous, to = level)
        }
    }

    private fun enter(player: Player) {
        player.openInterface(OVERLAY_INTERFACE, InterfaceDestination.OVERLAY)
        player.setComponentHidden(OVERLAY_INTERFACE, TIMER_COMPONENT, true)
        player.message("<col=4f006f>You have entered the Wilderness. Beware of other players.</col>")
    }

    private fun leave(player: Player) {
        player.closeInterface(InterfaceDestination.OVERLAY)
        player.message("<col=4f006f>You are no longer in the Wilderness.</col>")
    }

    /** The level 30 fence, in both directions. */
    private fun announceFence(
        player: Player,
        from: Int,
        to: Int,
    ) {
        val cap = Wilderness.DEEP_TELEPORT_CAP
        if (from <= cap && to > cap) {
            player.message("<col=4f006f>You are now in the deep Wilderness. Teleports no longer work here.</col>")
        } else if (from > cap && to <= cap) {
            player.message("<col=4f006f>You have left the deep Wilderness.</col>")
        }
    }

    private companion object {
        /** Found by scanning the cache's interface archives for the text `Level: 0`. */
        const val OVERLAY_INTERFACE = 481
        const val LEVEL_COMPONENT = 42

        /** The real game's Tele Block countdown; nothing here drives it, so it stays hidden. */
        const val TIMER_COMPONENT = 46

        const val CHUNK_SIZE = 8

        val WILDERNESS_TIMER = TimerKey()

        /**
         * The last level the overlay was told about. Absent (rather than 0) until the first
         * refresh, which is what makes a login inside the Wilderness re-open the overlay.
         */
        val WILDERNESS_LEVEL_ATTR = AttributeKey<Int>()
    }
}
