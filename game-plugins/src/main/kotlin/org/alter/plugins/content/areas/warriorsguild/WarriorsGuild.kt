package org.alter.plugins.content.areas.warriorsguild

import org.alter.api.Skills
import org.alter.api.ext.hasEquipped
import org.alter.api.EquipmentType
import org.alter.api.ext.message
import org.alter.game.model.Area
import org.alter.game.model.Tile
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey

/**
 * The Warriors' Guild in western Burthorpe: the shared numbers every part of it keys off.
 *
 * ## Where the coordinates come from
 *
 * Object tiles are dumped straight out of this cache with `:game-server:agilityLocDump`, not
 * guessed - the guild sits in region 11319, and the basement in 11675. Npc tiles are the `{{Map}}`
 * template on each npc's own wiki page. Both are recorded next to what they belong to, and
 * `WarriorsGuildVerify` re-checks every object id and npc id against the cache by name.
 *
 * ## The one thing the cache does not have
 *
 * **The seven dummies are not in the map.** Object ids 23958-23964 exist as definitions - all
 * seven named "Dummy" with a single `Hit` action - but a scan of every region in the cache finds
 * no placed instance of any of them. What the dummy room does contain, on the tiles where the
 * dummies stand in game, is eight "Hole" objects (23965 and 24297-24302) with no actions at all.
 * So [DummyRoomPlugin] spawns the dummies itself onto seven of those hole tiles. The mechanic is
 * real; only the exact tile-to-dummy assignment is this project's choice rather than the cache's.
 */
object WarriorsGuild {
    // ------------------------------------------------------------------ entry

    /**
     * The combined Attack + Strength level the guild demands, or 99 in either on its own.
     *
     * Checked against **base** levels. The wiki is explicit that "temporary boosts cannot be
     * used", which is why this reads `getBaseLevel` rather than the current level - a super
     * attack potion must not open the door.
     */
    const val COMBINED_LEVEL = 130

    /** The alternative to [COMBINED_LEVEL]: 99 in Attack or in Strength alone. */
    const val MASTERY_LEVEL = 99

    fun meetsEntryRequirement(player: Player): Boolean {
        val attack = player.getSkills().getBaseLevel(Skills.ATTACK)
        val strength = player.getSkills().getBaseLevel(Skills.STRENGTH)
        return attack + strength >= COMBINED_LEVEL ||
            attack >= MASTERY_LEVEL ||
            strength >= MASTERY_LEVEL
    }

    // ------------------------------------------------------------------ tokens

    const val TOKEN = "item.warrior_guild_token"

    /** The number of tokens that must be carried before either cyclops room will open. */
    const val TOKENS_TO_ENTER = 100

    /** Taken immediately on entering a cyclops room, and again every minute inside. */
    const val TOKEN_DRAIN = 10

    /** A minute, in game cycles. The rate the wiki gives for the cyclops rooms' upkeep. */
    const val TOKEN_DRAIN_CYCLES = 100

    /** Counts down the next [TOKEN_DRAIN] while the player is in a cyclops room. */
    val TOKEN_DRAIN_TIMER = TimerKey()

    /**
     * An Attack cape lets its wearer into the cyclops rooms without tokens, and stops the drain.
     *
     * The guild's own reward for the skill it exists to train, and the reason [AjjatPlugin]'s shop
     * matters beyond being a place to spend 99,000 coins.
     */
    fun bypassesTokens(player: Player): Boolean =
        player.hasEquipped(EquipmentType.CAPE, "item.attack_cape", "item.attack_capet", "item.max_cape_13342")

    // ------------------------------------------------------------------ areas

    /**
     * The top-floor cyclops room: everything east of Kamfreena's doors at (2847, 3540) and
     * (2847, 3541), on plane 2.
     *
     * Bounded from the east side of the doors deliberately. Kamfreena herself stands at
     * (2844, 3540), *outside*, and a room that reached her would start charging a player for
     * standing next to the woman they are paying. Used to decide when the token drain runs and
     * when a player has left and re-entered - which is what advances the defender ladder.
     */
    val TOP_FLOOR_CYCLOPS = Area(2848, 3534, 2874, 3554)

    const val TOP_FLOOR_PLANE = 2

    /**
     * The basement cyclops room, east of Lorelai's door at (2911, 9968).
     *
     * **The wiki's published basement spawn coordinates are not usable against this cache.** They
     * put the level 106 cyclopes at x 2842-2874, y 9990-10004, which lands in region 11420 - and
     * a dump of that region returns *zero* objects, because the cache has no map data there at
     * all. Lorelai's own map pin (2909, 9972) and the cache agree with each other instead: region
     * 11675 holds a ladder up at (2906, 9968) and a door at (2911, 9968) with her standing beside
     * it, which is the shape of the basement entrance. That is what this models.
     */
    val BASEMENT_CYCLOPS = Area(2912, 9955, 2940, 9983)

    const val BASEMENT_PLANE = 0

    fun inCyclopsRoom(player: Player): Boolean = inTopFloorRoom(player) || inBasementRoom(player)

    fun inTopFloorRoom(player: Player): Boolean =
        player.tile.height == TOP_FLOOR_PLANE && TOP_FLOOR_CYCLOPS.contains(player.tile)

    fun inBasementRoom(player: Player): Boolean =
        player.tile.height == BASEMENT_PLANE && BASEMENT_CYCLOPS.contains(player.tile)

    // ------------------------------------------------------------------ objects

    /** The guild's front door, at (2877, 3546). Ghommal stands beside it. */
    const val ENTRANCE_DOOR = "object.door_24318"

    /** Kamfreena's pair of doors into the top-floor cyclops room, on plane 2. */
    val TOP_FLOOR_DOORS = listOf("object.door_24306" to Tile(2847, 3540, 2), "object.door_24309" to Tile(2847, 3541, 2))

    /** Lorelai's door into the basement cyclops room. */
    val BASEMENT_DOOR = "object.door_10043" to Tile(2911, 9968, 0)

    /** The two Magical Animators in the animation room, both on the ground floor. */
    val ANIMATORS = listOf(Tile(2851, 3536, 0), Tile(2857, 3536, 0))

    /**
     * The hole tiles in the dummy room that [DummyRoomPlugin] stands a dummy on.
     *
     * Seven of the eight holes the cache places here - see the class comment for why the dummies
     * are spawned rather than read from the map.
     */
    val DUMMY_TILES =
        listOf(
            Tile(2856, 3554, 0),
            Tile(2858, 3554, 0),
            Tile(2860, 3553, 0),
            Tile(2855, 3552, 0),
            Tile(2860, 3551, 0),
            Tile(2859, 3549, 0),
            Tile(2857, 3549, 0),
        )

    /** The two throwable shots in the shot put room, on the first floor. */
    val SHOT_PUT_SPOTS = listOf("object.shot_15664" to Tile(2861, 3554, 1), "object.shot_15665" to Tile(2861, 3548, 1))

    /** The Strength level the heavy doors into the shot put room demand. */
    const val SHOT_PUT_STRENGTH = 50

    /*
     * The first of the pair is `object.heavy_door` with no id suffix - the un-suffixed key really
     * is 15658 here, unlike the `npc.aubury` family of traps where it belongs to something else.
     */
    val SHOT_PUT_DOORS = listOf("object.heavy_door" to Tile(2852, 3552, 1), "object.heavy_door_15660" to Tile(2852, 3551, 1))

    // ------------------------------------------------------------------ shared state

    /**
     * Set while a player is inside a cyclops room, cleared when they leave.
     *
     * Not persisted: it exists only so the tick handler can tell "still inside" from "just walked
     * in" and from "just walked out", which is what starts the token drain and what advances the
     * defender ladder.
     */
    val IN_CYCLOPS_ROOM = AttributeKey<Boolean>()

    fun outOfTokens(player: Player) {
        player.message("You have run out of warrior guild tokens.")
    }
}
