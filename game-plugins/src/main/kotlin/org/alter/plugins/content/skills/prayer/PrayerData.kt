package org.alter.plugins.content.skills.prayer

/**
 * How an offering leaves the inventory when it is used on its own.
 *
 * The distinction isn't a config field - it is read back off the item's own cache options,
 * because that is where the client already draws the line. Every bone in the cache carries
 * a "Bury" option and every demonic ash carries "Scatter", so [PrayerService] resolves this
 * per entry rather than trusting the JSON to agree with the cache.
 */
enum class OfferingAction(
    /** The item option in the cache that performs it. */
    val option: String,
    /** The line OSRS prints before the item is gone, for the actions that have one. */
    val prelude: String?,
    /** The line OSRS prints once the item is gone. */
    val message: String,
) {
    BURY("Bury", "You dig a hole in the ground.", "You bury the bones."),
    SCATTER("Scatter", null, "You scatter the ashes."),
    ;

    companion object {
        val values = enumValues<OfferingAction>()
    }
}

/**
 * One thing that can be offered for Prayer experience, loaded from
 * `data/cfg/prayer/offerings.json`.
 *
 * [experience] is the *burying* figure - the base every other offering method scales from,
 * and the number the OSRS Wiki's `Prayer info` template calls `xp`. A gilded or chaos altar
 * multiplies it; see [AltarPlugin].
 *
 * As elsewhere in this codebase, Gson allocates these without running the constructor, so
 * neither Kotlin defaults nor an `init` block would ever apply - a field missing from the
 * JSON silently arrives as 0/0.0/null. [level] is therefore deliberately nullable: only
 * superior dragon bones have a requirement above 1, so the other thirty-five rows leave it
 * out and [levelRequired] fills it in. Everything else is validated explicitly by
 * [PrayerService].
 */
data class OfferingEntry(
    /** The item's name, used in validation failures rather than in-game text. */
    val name: String,
    val item: String,
    val experience: Double,
    /** Prayer level needed, or null for the level-1 default. */
    val level: Int? = null,
) {
    val levelRequired: Int
        get() = level ?: DEFAULT_LEVEL

    @Transient
    var itemId: Int = -1

    @Transient
    lateinit var action: OfferingAction

    companion object {
        const val DEFAULT_LEVEL = 1
    }
}
