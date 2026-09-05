package org.alter.plugins.content.areas.duelarena

import org.alter.api.ClientScript
import org.alter.game.model.Tile
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.timer.TimerKey

/**
 * Everything about the Duel Arena that is fixed by the cache or by the map, kept in one place.
 *
 * This cache is on revision 228, which is *after* Jagex replaced the Duel Arena with Emir's Arena,
 * so the interface ids every older server uses are gone - 482 is the Tombs of Amascut summary here
 * and 631 is a bank. The duel screens survived the move intact, only renumbered: [OPTIONS_INTERFACE]
 * and [CONFIRM_INTERFACE] below are the real thing, and the client scripts that drive them
 * ([SCRIPT_OPTIONS_INIT] and friends) are all still present and still resolve by name.
 *
 * What did *not* survive is a staking screen - no interface anywhere in the cache offers a
 * two-sided item wager any more. The stake is therefore run on the trade interfaces, which already
 * hold two offers side by side with values and a confirmation step; see [DuelSession].
 */
object DuelArena {
    /**
     * "Duel Options" - the rules screen. The 12 rule rows are built by the client into components
     * 30..41 and the worn-slot icons into 42/70, so almost nothing here is a static component.
     */
    const val OPTIONS_INTERFACE = 755

    /** "Confirm Duel Options" - the summary both players must accept. */
    const val CONFIRM_INTERFACE = 756

    /**
     * The rule bitmask. `[clientscript,6169]` ends with `if_setonvartransmit("script6173{var286}")`,
     * so the client redraws every rule row whenever this varp changes - the server owns it and the
     * screen follows.
     */
    const val RULES_VARP = 286

    /**
     * The locked-equipment-slot bitmask, indexed by worn slot. Read by `[proc,6175]` through
     * `testbit(%varbit642, $slot)`.
     */
    const val SLOTS_VARBIT = 642

    /**
     * Builds ONE rule row: `(int index, string title, component row, string description)`.
     *
     * The screen's own entry point is `[clientscript,6169]`, which is nothing but twelve calls to
     * this followed by a refresh. Invoking 6169 from the server does nothing observable - verified
     * on a live client, with the packet confirmed correct on the wire (`types=[i]`,
     * `values=[755<<16]`, matching its decoded 1-int signature) - while calling this directly
     * builds the row every time. So the server does 6169's job itself, which it is well placed to
     * do: every argument is a constant transcribed from 6169 and asserted against enum 4209 in
     * DuelArenaVerify.
     */
    val SCRIPT_BUILD_ROW = ClientScript(id = 6170)

    /** Redraws every radio button and worn-slot overlay from varp 286 / varbit 642. */
    val SCRIPT_OPTIONS_REFRESH = ClientScript(id = 6175)

    /**
     * Flashes one changed option. 0..13 is a rule, 14..27 a worn slot - the same numbering the
     * confirm screen packs into its flag word.
     */
    val SCRIPT_OPTION_CHANGED = ClientScript(id = 6176)

    /** Draws the worn-item icons on the options screen from the player's own equipment. */
    val SCRIPT_INIT_WORN = ClientScript("duel_initworn")

    /** Writes the "Opponent details / Before the duel starts / During the duel" summary. */
    val SCRIPT_CONFIRM_TEXT = ClientScript(id = 6193)

    /** Re-arms the Accept button once `clientclock` passes the value handed to it. */
    val SCRIPT_ACCEPT_BUTTON = ClientScript("duel_accept_button")

    /**
     * How long the Accept button stays dead after either player changes something.
     *
     * The client refuses the click on its own until `clientclock` catches up, but a client is not
     * where a rule like this can live, so the session keeps the same deadline server-side.
     */
    const val ACCEPT_DELAY_TICKS = 3

    /** Ticks between "3.. 2.. 1.. FIGHT!" and the players being unlocked. */
    const val COUNTDOWN_TICKS = 3

    /** Drives the pre-fight countdown; each player runs their own. */
    val COUNTDOWN_TIMER = TimerKey()

    /** How many counts a player has left before the duel starts. */
    val COUNTDOWN_LEFT = AttributeKey<Int>()

    /**
     * Watches a fighting player each tick to see whether they have left the arena.
     *
     * The arenas have an opening in the middle of each long wall, so walking out is the retreat the
     * trapdoors used to be. Whether that is allowed depends on "No Forfeit", which is why it has to
     * be noticed rather than prevented.
     */
    val WATCH_TIMER = TimerKey()

    /**
     * The four walled arenas, read off the region 13362 floor rather than off the wiki: each is a
     * ring of spear walls around an open floor, with the openings in the middle of the long walls.
     *
     * These are the interiors only - the tiles a duel is actually fought on.
     */
    val ARENAS =
        listOf(
            DuelPlot(name = "north-west", minX = 3334, maxX = 3351, minZ = 3246, maxZ = 3256),
            DuelPlot(name = "north-east", minX = 3370, maxX = 3387, minZ = 3246, maxZ = 3256),
            DuelPlot(name = "south-west", minX = 3334, maxX = 3351, minZ = 3208, maxZ = 3218),
            DuelPlot(name = "south-east", minX = 3370, maxX = 3387, minZ = 3208, maxZ = 3218),
        )

    /**
     * Where a duel ends: the staging area north of the arenas, beside the hospital. Both the winner
     * and the loser are put here, healed, the way the Duel Arena always returned players to the
     * building next to the arena rather than to a respawn point.
     */
    val LOBBY_TILE = Tile(3366, 3272, 0)

    /**
     * The area a player has to be standing in to challenge or be challenged. Deliberately generous -
     * it covers the whole staging area, the hospital and the ground between them - because the only
     * thing it really has to exclude is the rest of the world.
     */
    const val LOBBY_MIN_X = 3328
    const val LOBBY_MAX_X = 3391
    const val LOBBY_MIN_Z = 3264
    const val LOBBY_MAX_Z = 3295

    /** The region the lobby bounds sit inside, used to switch the Challenge option on and off. */
    const val LOBBY_REGION = 13363

    /**
     * The right-click option slot "Challenge" is sent on while a player is in the lobby.
     *
     * Sits after Follow (3), Trade with (4) and Report (5), which are the only other player options
     * this server sends. Slots are limited to 1-8 by `SetPlayerOp`.
     */
    const val CHALLENGE_OPTION_SLOT = 6

    /**
     * The slot "Attack" is sent on for the duration of a fight, as a left-click option.
     *
     * The client does NOT offer Attack by itself anywhere - this comment used to claim it did in
     * the Wilderness, and that assumption is why nothing sent the option there and PvP was
     * unreachable even with `inWilderness()` reading the tile correctly. `WildernessPlugin` now
     * sends its own on this same slot; the two never overlap, since a duel is fought inside the
     * arena and the arena is not Wilderness. Without an option in the slot the duel starts, both
     * players are unlocked, and neither has any way to hit the other.
     */
    const val ATTACK_OPTION_SLOT = 1

    fun inLobby(tile: Tile): Boolean =
        tile.height == 0 &&
            tile.x in LOBBY_MIN_X..LOBBY_MAX_X &&
            tile.z in LOBBY_MIN_Z..LOBBY_MAX_Z

    fun inAnyArena(tile: Tile): Boolean = ARENAS.any { it.contains(tile) }
}

/**
 * One of the four arenas.
 */
data class DuelPlot(
    val name: String,
    val minX: Int,
    val maxX: Int,
    val minZ: Int,
    val maxZ: Int,
) {
    fun contains(tile: Tile): Boolean =
        tile.height == 0 && tile.x in minX..maxX && tile.z in minZ..maxZ

    /**
     * The two tiles the duellists start on, a few squares apart along the arena's long axis so
     * neither begins in range of the other - except under "No Movement", where they are placed
     * adjacent instead because neither of them will be able to close the gap.
     */
    fun startTiles(adjacent: Boolean): Pair<Tile, Tile> {
        val z = (minZ + maxZ) / 2
        val centreX = (minX + maxX) / 2
        return if (adjacent) {
            Tile(centreX, z, 0) to Tile(centreX + 1, z, 0)
        } else {
            Tile(minX + 3, z, 0) to Tile(maxX - 3, z, 0)
        }
    }
}
