package org.alter.plugins.content.mechanics.water

import dev.openrune.cache.CacheManager
import org.alter.api.ext.getItemName
import org.alter.api.ext.replaceItemName
import org.alter.game.fs.DefinitionSet

/**
 * The kinds of scenery a water container can be filled from, and the message each one
 * prints.
 *
 * Sources are found by **scanning the cache for objects whose name is one of [names]**
 * rather than by listing ids, the same way [org.alter.plugins.content.skills.smithing
 * .SmeltingPlugin] finds furnaces and Cooking finds ranges. Water sources are the awkward
 * case for that pattern: a furnace carries a "Smelt" action and a range carries "Cook",
 * but a fountain carries no action at all - in OSRS you fill a container by using it on
 * the fountain, so there is nothing to key off but the name.
 *
 * Matching is therefore on the **whole name, case-insensitively**, never a substring. A
 * substring match is what makes this approach dangerous here: "well" is inside
 * `Stairwell`, `Dwellberry bush` and `Energy well`, "tap" is inside `Tapestry` and
 * `Catapult`, and "water" is inside `Waterfall rocks` and `Dead watermelons`. All of
 * those would have become fill points.
 *
 * The names below were chosen by dumping every object whose name mentions water and
 * comparing the result against the hardcoded id list this file used to carry (see git
 * history - it named ~100 ids through an `Objs` constants class that RSCM replaced). The
 * scan reproduces that list **exactly** and adds the ids that have entered the cache
 * since it was written; `WaterVerify` asserts that, so a cache update that renames or
 * removes a source is caught by the test rather than in-game.
 *
 * Deliberately excluded, though their names mention water: `Fountain of Rune`,
 * `Fountain of Heroes`, `Fountain of Uhld`, `Fairy fountain`, `Ornamental fountain`,
 * `Sinclair family fountain`, `Broken Fountain`, `Broken Sink`, `Swampy sink`,
 * `Sink space`, `Broken Well`, `Old well`, `Magic well`, `Energy well`, `Vyre well` and
 * the bare `Water` tiles. Some of those may well be fillable in OSRS - the ornamental and
 * Sinclair fountains especially - but none was in the list this replaces, and quietly
 * widening the set is not something a name scan should decide on its own.
 */
enum class WaterSources(val message: String, vararg val names: String) {
    FOUNTAINS("You fill the #ITEM from the fountain.", "fountain", "small fountain", "large fountain"),
    SINKS("You fill the #ITEM from the sink.", "sink", "toy sink"),
    BARRELS("You fill the #ITEM.", "water barrel", "barrel of water"),
    PUMPS("You fill the #ITEM.", "water pump", "waterpump"),
    TAPS("You fill the #ITEM.", "tap", "water tap"),
    WELLS("You fill the #ITEM careful not to fall in.", "well"),
    ;

    /**
     * The line printed when [container] is filled at this source.
     *
     * A cup is the one container the generic `#ITEM` substitution reads badly for - "You
     * fill the empty cup from the sink" - so it gets its own line, the way it did when this
     * lived inline in `WaterPlugin`. Shared by both fill routes so the left-click actions
     * cannot drift from the use-item-on-source ones.
     */
    fun messageFor(
        container: WaterContainer,
        definitions: DefinitionSet,
    ): String =
        if (container.unfilled.getItemName().contains("Cup")) {
            "You fill the cup."
        } else {
            message.replaceItemName(container.unfilled, definitions)
        }

    companion object {
        /**
         * Every object name that fills a container, mapped to the source it belongs to.
         * Built once; a name appearing under two sources would be a bug, and [scan]
         * relies on this being a genuine 1:1 map to guarantee no object is bound twice.
         */
        private val byName: Map<String, WaterSources> =
            values()
                .flatMap { source -> source.names.map { it to source } }
                .also { pairs ->
                    val duplicated = pairs.groupBy { it.first }.filterValues { it.size > 1 }.keys
                    check(duplicated.isEmpty()) { "Water source name listed under two sources: $duplicated" }
                }
                .toMap()

        /**
         * Every water-source object id in the cache, mapped to the source it acts as.
         *
         * Keyed by id, so an object can only ever appear once no matter how the names are
         * arranged - which matters because `PluginRepository.bindItemOnObject` throws on a
         * repeated (item, object) pair, taking the whole server down at boot rather than
         * failing quietly.
         */
        fun scan(): Map<Int, WaterSources> =
            CacheManager
                .getObjects()
                .mapNotNull { (id, def) ->
                    val source = byName[def.name?.lowercase() ?: return@mapNotNull null] ?: return@mapNotNull null
                    id to source
                }.toMap()
    }
}
