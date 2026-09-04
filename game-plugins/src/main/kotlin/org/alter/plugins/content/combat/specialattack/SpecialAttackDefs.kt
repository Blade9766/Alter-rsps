package org.alter.plugins.content.combat.specialattack

import dev.openrune.cache.CacheManager

/**
 * The cache's own register of which weapons have a special attack, what it is called and what it
 * costs.
 *
 * Two enums carry it, and between them they name every one of the 265 items in this revision that
 * the client will draw a special attack bar for:
 *
 * - **enum 906** maps item id -> bar cost, in tenths of a percent (`250` is the dragon dagger's
 *   25%). The same weapon's variants do not always agree - a `(bh)` dragon mace costs 15% where the
 *   ordinary one costs 25%, and the ornamented granite maul 50% against the plain one's 60% - which
 *   is exactly why the cost belongs here and not in a constant beside the effect.
 * - **enum 1739** maps item id -> the special's wiki description, always
 *   `"<Name>: <what it does>"`. The name before the colon is shared by every variant of a weapon:
 *   all nine dragon daggers (plain, `(p)`, `(p+)`, `(p++)`, the `(cr)` set and the untradeable
 *   20407) read `Puncture`, all six dark bows read `Descent of Darkness`.
 *
 * That second point is what this exists for. Every special used to be bound by hand-listing item
 * ids, and the lists were short: the dragon dagger's special was registered for item 1215 alone, so
 * the poisoned daggers players actually carry had none, and the same held for every ornamented,
 * `(cr)`, `(bh)` and deadman variant in the game. Registering against the *name* picks all of them
 * up from the cache, and picks up any new variant a future cache adds for free.
 *
 * Read straight off the enums with no local copy: a cache bump can move ids and re-price specials,
 * and a hard-coded table would go quietly wrong. `SpecialAttackVerify` re-checks that both enums
 * still look like this.
 */
object SpecialAttackDefs {
    /** Item id -> bar cost, in tenths of a percent. */
    private const val COST_ENUM = 906

    /** Item id -> `"<Name>: <description>"`. */
    private const val DESCRIPTION_ENUM = 1739

    private val costs: Map<Int, Int> by lazy {
        val values = runCatching { CacheManager.getEnum(COST_ENUM)?.values }.getOrNull() ?: return@lazy emptyMap()
        values.keys.mapNotNull { id ->
            val tenths = values[id] as? Int ?: return@mapNotNull null
            if (tenths <= 0) return@mapNotNull null
            // Round up, so the two sub-1% oddities (Soulreaper axe at 0.1%, Sunlight spear at 0.7%)
            // still cost something against a bar the rest of this codebase counts in whole percent.
            id to (tenths + 9) / 10
        }.toMap()
    }

    private val descriptions: Map<Int, String> by lazy {
        val values = runCatching { CacheManager.getEnum(DESCRIPTION_ENUM)?.values }.getOrNull() ?: return@lazy emptyMap()
        values.keys.mapNotNull { id ->
            val text = values[id] as? String ?: return@mapNotNull null
            id to text
        }.toMap()
    }

    /** `"Puncture: Deal two quick slashes..."` -> `"Puncture"`. */
    private fun nameOf(description: String): String = description.substringBefore(':').trim()

    private val itemsByName: Map<String, List<Int>> by lazy {
        descriptions.entries
            .groupBy({ nameOf(it.value) }, { it.key })
            .mapValues { (_, ids) -> ids.sorted() }
    }

    /**
     * Every item id whose special attack is called [name], e.g. every dragon dagger for
     * `"Puncture"`. Empty if this cache has no such special - which is a bug in the caller, not a
     * state to paper over, so [SpecialAttacks.registerByName] refuses it.
     */
    fun itemsWith(name: String): List<Int> = itemsByName[name].orEmpty()

    /** What [itemId]'s special costs off the bar, in percent, or `null` if it has no special. */
    fun cost(itemId: Int): Int? = costs[itemId]

    /** The name of [itemId]'s special attack, e.g. `"Sever"`, or `null` if it has none. */
    fun name(itemId: Int): String? = descriptions[itemId]?.let(::nameOf)

    /** The full wiki description of [itemId]'s special attack. */
    fun description(itemId: Int): String? = descriptions[itemId]

    /**
     * Every item this cache gives a special attack to.
     *
     * Costed off enum 906 rather than the description enum, because a handful of ornamented
     * skilling tools (the `(or)` and uncharged infernal harpoons, pickaxes and axes) carry a cost
     * but no description. Those are the [SpecialAttacks.registerByName] `extraItems` cases.
     */
    val weapons: Set<Int> get() = costs.keys

    /** Every distinct special attack name in this cache. */
    val names: Set<String> get() = itemsByName.keys
}
