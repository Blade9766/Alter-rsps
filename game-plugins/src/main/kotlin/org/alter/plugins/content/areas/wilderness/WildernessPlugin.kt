package org.alter.plugins.content.areas.wilderness

import org.alter.api.ext.getWildernessLevel
import org.alter.api.ext.message
import org.alter.api.ext.removeOption
import org.alter.api.ext.sendOption
import org.alter.api.ext.setVarbit
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
 * **The server opens no interface for it.** It sets [IN_WILDERNESS_VARBIT] and the client draws
 * and updates the skull and level itself.
 *
 * Two interfaces were opened here before, and both were wrong. **481** is `InterfaceID.TOA_HUD`,
 * the Tombs of Amascut raid HUD - picked by scanning every interface archive for the embedded text
 * `Level: 0` and taking the only hit, which was the *raid* level. It drew in the right place and
 * showed `Level: 0` forever, along with four counters nothing here writes to, because the client
 * repopulates the raid HUD from raid state. **90** is RuneLite's `PVP_ICONS`, but in this
 * revision's cache it holds Last Man Standing (`Pile-jumping immunity`, `Players Remaining`,
 * `Final Safe Area`) and drew nothing at all - interface ids get recycled between revisions, the
 * same trap the duel screens have.
 *
 * The lesson, paid for twice: the overlay carries **no embedded string** for the level, because
 * client scripts build it. A text scan can never find it, and an interface that merely *contains*
 * the words is not evidence.
 *
 * `Player.inWilderness()` used to be defined as "the overlay slot has something in it", and
 * nothing ever filled that slot, so it was permanently false - taking PvP, the looting bag and
 * wilderness-only drops down with it. It reads the tile now, and is deliberately independent of
 * anything on screen, so no display problem can disable game logic again.
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

        if (previous != level) {
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

            if (previous != null && previous > 0) {
                announceFence(player, from = previous, to = level)
            }
        }
    }

    private fun enter(player: Player) {
        player.setVarbit(IN_WILDERNESS_VARBIT, 1)
        player.sendOption(ATTACK_OPTION, ATTACK_OPTION_SLOT, leftClick = true)
        player.message("<col=4f006f>You have entered the Wilderness. Beware of other players.</col>")
    }

    private fun leave(player: Player) {
        player.setVarbit(IN_WILDERNESS_VARBIT, 0)
        player.removeOption(ATTACK_OPTION_SLOT)
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

        /**
         * The client's own "am I in the Wilderness" flag - `Varbits.IN_WILDERNESS` in RuneLite,
         * 0 outside and 1 inside. Packed into varp 1105 (`WILDERNESS_STATISTICS`) at bit 22.
         *
         * **The client owns the overlay entirely.** The server's only job is this flag; the client
         * is expected to open, draw and update the skull and level itself.
         *
         * **Unverified in this revision, and currently nothing draws.** Setting the flag alone
         * produces no overlay, so either the client here wants more than this or it does not drive
         * the overlay from it at all. The flag is kept because it is semantically correct and
         * costs nothing, but the on-screen level is a known gap - parked 2026-09-04. Everything
         * functional is unaffected: `inWilderness()` reads the tile, so PvP, the looting bag,
         * wilderness drops, risk and multi-combat all work with no overlay at all.
         *
         * Sourced from RuneLite's `Varbits.java`, the same external-source rule the combat sound
         * ids follow: this cache decodes varbits but nothing names them, and guessing at ids has
         * been expensive here.
         */
        const val IN_WILDERNESS_VARBIT = 5963

        /**
         * "Attack", sent as a left-click option for as long as the player is in the Wilderness.
         *
         * The server has to send this. `DuelArena` assumed the opposite - its comment said the
         * client "decides to offer Attack by itself" in the Wilderness, and so only the arena ever
         * sent one. It does not: with no option in the slot there is nothing to click, which is
         * why PvP was unreachable even after `inWilderness()` was fixed to read the tile. The
         * option is only half of it - `Combat.inPvpArea` still gates the attack itself, so this
         * cannot be used to hit someone outside.
         *
         * Slot 1 is the same slot the arena uses. The two are mutually exclusive - a duel is
         * fought inside the arena, which is not Wilderness - so they cannot fight over it.
         */
        const val ATTACK_OPTION = "Attack"
        const val ATTACK_OPTION_SLOT = 1

        const val CHUNK_SIZE = 8

        val WILDERNESS_TIMER = TimerKey()

        /**
         * The last level the overlay was told about. Absent (rather than 0) until the first
         * refresh, which is what makes a login inside the Wilderness re-open the overlay.
         */
        val WILDERNESS_LEVEL_ATTR = AttributeKey<Int>()
    }
}
