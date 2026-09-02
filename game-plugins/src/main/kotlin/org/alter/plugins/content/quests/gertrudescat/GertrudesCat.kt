package org.alter.plugins.content.quests.gertrudescat

import org.alter.api.cfg.Varp
import org.alter.game.model.Tile
import org.alter.game.model.attr.AttributeKey
import org.alter.plugins.content.quests.Quest

/**
 * Gertrude's Cat - find Fluffs, who has gone missing from Gertrude's house west of Varrock and
 * turned up in the Lumber Yard with a kitten she will not leave behind.
 *
 * ## Sourcing
 *
 * Dialogue is the wiki's `Transcript:Gertrude's Cat`, line for line, including the branches that
 * only exist to be refused. Every id below was taken from the relevant wiki page's infobox and then
 * checked against this cache - name, and where the wiki publishes them, the option strings too. All
 * of them matched:
 *
 * | Thing | Wiki | Cache says |
 * |---|---|---|
 * | Gertrude (before quest) | 7284 | `Gertrude`, options `[Talk-to]` |
 * | Gertrude (after quest) | 7723 | `Gertrude`, options `[Talk-to, Kitten]` |
 * | Wilough / Shilop | 3503 / 3501 | `Wilough` / `Shilop`, `[Talk-to]` |
 * | Fluffs | 3497 | `Gertrude's cat`, `[Pick-up, Stroke, Talk-to]` |
 * | Quest crate | 3499 | `Crate`, `[Search]` |
 * | Fluffs' kitten / seasoned sardine / doogle leaves | 1554 / 1552 / 1573 | names match |
 * | Kitten reward colours | 1555-1560 | six `Pet kitten` items |
 *
 * The **quest crates are npcs, not scenery** - the wiki documents them with an `Infobox NPC` and
 * the cache agrees (npc 3499, examine "Can I hear kittens?"). The Lumber Yard is also full of
 * ordinary `Crate` *objects* (ids 2620 and 5106) which are not part of the quest; binding those by
 * mistake would have made every crate in the yard a kitten lottery.
 *
 * The **broken fence (object 2618 at 3308,3492) and the yard's ladder (11794 up / 11795 down at
 * 3310,3509)** were found by dumping region 13110's locations rather than guessed - see
 * [GertrudesCatPlugin]. Neither was wired to anything before this, so the first floor of the Lumber
 * Yard, and therefore Fluffs, was unreachable.
 *
 * ## Known deviations, all deliberate
 *
 * - **Fluffs does not disappear when she goes home.** She is a single world npc shared by every
 *   player, so despawning her on one player's completion would delete her for everyone mid-quest.
 *   The stage advances and the dialogue says she has gone; she stays standing there. The same
 *   applies to the crates, which is harmless - which crate holds the kitten is per player anyway
 *   ([CHOSEN_CRATE_ATTR]).
 * - **The kitten reward is an inventory item that does nothing.** This server has no pet or
 *   follower system, so the kitten cannot be dropped to walk around, grown, or fed. The item is
 *   real and the colour is rolled the way the game rolls it; there is just nothing yet to do with
 *   it.
 * - **Dropping Fluffs' kitten does not turn it back into an npc** that runs off and re-hides. The
 *   real game does that (npc 3498, which carries no options at all, consistent with being pure
 *   scenery). Here the item simply drops. The recovery path is the same either way: search the
 *   crates again, which still works because [CHOSEN_CRATE_ATTR] is persistent.
 * - **No Ring of Charos(a) colour choice**, and no easy Varrock Diary tree. Both need systems that
 *   do not exist.
 */
object GertrudesCat {
    /*
     * Stages. `0` is "not started" for every quest in this framework; the rest are this quest's own.
     *
     * These are also the values written to the quest's player-variable, which is why they are a
     * plain ascending run. The variable *id* is the real one (180, confirmed against RuneLite's
     * `VarPlayerID.FLUFFS`); the values are ours, because the live game's intermediate values are
     * not published and this cache has no quest struct to read them from. See
     * `org.alter.plugins.content.quests.syncVarps`.
     */

    /** Agreed to look for Fluffs. */
    const val STARTED = 1

    /** Paid Wilough and Shilop, and been told about the Lumber Yard. */
    const val KNOWS_LOCATION = 2

    /** Fluffs has had the bucket of milk. */
    const val CAT_HAD_MILK = 3

    /** Fluffs has had the seasoned sardine; the kittens can now be heard in the crates. */
    const val CAT_HAD_SARDINE = 4

    /** Fluffs' kitten has been found in a crate. */
    const val FOUND_KITTEN = 5

    /** The kitten has been given back to Fluffs and the pair have gone home. */
    const val REUNITED = 6

    /** Reported back to Gertrude. */
    const val COMPLETE = 7

    val QUEST =
        Quest(
            id = "gertrudes_cat",
            name = "Gertrude's Cat",
            questPoints = 1,
            varp = Varp.GERTRUDES_CAT,
            completedStage = COMPLETE,
        )

    /** Cooking experience on completion. */
    const val COOKING_XP = 1_525.0

    /** What Wilough and Shilop want for the location. */
    const val BRIBE = 100

    /**
     * The six crates the kitten can be hiding in, from the quest article's own pin map.
     *
     * Order is the map's; [CHOSEN_CRATE_ATTR] stores an index into this list.
     */
    val CRATE_TILES =
        listOf(
            Tile(3298, 3514, 0),
            Tile(3315, 3515, 0),
            Tile(3303, 3506, 0),
            Tile(3307, 3507, 0),
            Tile(3305, 3500, 0),
            Tile(3310, 3499, 0),
        )

    /**
     * Which crate this player's kitten is in, as an index into [CRATE_TILES], or absent until they
     * first search one.
     *
     * Persisted, and rolled once. The wiki is explicit that "the location of the kitten remains the
     * same" for a given player however much the mewing appears to move about, so re-rolling per
     * search - or per login - would be wrong as well as maddening.
     */
    val CHOSEN_CRATE_ATTR = AttributeKey<Int>(persistenceKey = "gertrudes_cat_crate")

    /**
     * The kitten colours Gertrude can hand over, in the wiki's own order: grey and black, white,
     * brown, black, grey and brown, grey and blue.
     */
    val KITTEN_COLOURS =
        listOf(
            "item.pet_kitten",
            "item.pet_kitten_1556",
            "item.pet_kitten_1557",
            "item.pet_kitten_1558",
            "item.pet_kitten_1559",
            "item.pet_kitten_1560",
        )

    /** Fluffs' tile on the Lumber Yard's first floor. */
    val FLUFFS_TILE = Tile(3310, 3506, 1)
}
