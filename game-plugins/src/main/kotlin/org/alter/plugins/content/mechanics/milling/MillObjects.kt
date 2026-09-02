package org.alter.plugins.content.mechanics.milling

import dev.openrune.cache.CacheManager

/**
 * The four pieces of scenery a windmill is made of, found by scanning the cache.
 *
 * Each is matched on its **whole name plus the action it must carry**, the same discipline
 * [org.alter.plugins.content.mechanics.water.WaterSources] uses and for the same reason: a
 * substring match on "hopper" would swallow the `Egg hopper` and `Gingerbread hopper`, and
 * matching the name alone would swallow the two dozen decorative `Hopper` objects that
 * carry no action at all, plus the Blast Furnace's `Hopper` (which has `Deposit`, not
 * `Fill`) and the Motherlode Mine's (`Use`). Requiring the action is what separates a real
 * mill from set dressing.
 *
 * This reproduces the five working windmills in the cache - Lumbridge's Mill Lane Mill, the
 * Cooks' Guild, Ardougne, Hosidius and Aldarin - without naming a single id, so a mill
 * added by a future cache update needs no code change. `MillingVerify` pins the count so a
 * rename is caught by a test rather than in-game.
 */
enum class MillObjects(val objectName: String, val action: String) {
    /** Growing wheat. Picking it yields grain and, as in OSRS, never depletes the plant. */
    WHEAT("wheat", "Pick"),

    /** Top floor: where grain goes in. */
    HOPPER("hopper", "Fill"),

    /** Top floor: the lever that grinds whatever is in the hopper. */
    CONTROLS("hopper controls", "Operate"),

    /** Ground floor: where the flour comes out, and where a pot gets filled. */
    BIN("flour bin", "Empty"),
    ;

    /**
     * Every cache object that is this piece of a mill, mapped to the exact action string it
     * carries - actions are matched case-insensitively but bound with the casing the cache
     * uses, because `onObjOption` looks the option up by exact position in the action list.
     */
    fun scan(): Map<Int, String> =
        CacheManager
            .getObjects()
            .mapNotNull { (id, def) ->
                if (!def.name.equals(objectName, ignoreCase = true)) return@mapNotNull null
                val option =
                    def.actions.filterNotNull().firstOrNull { it.equals(action, ignoreCase = true) }
                        ?: return@mapNotNull null
                id to option
            }.toMap()
}
