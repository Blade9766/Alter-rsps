package org.alter.plugins.content.mechanics.dairy

/**
 * The published numbers behind Steal-cowbell, kept apart from [DairyCowPlugin] so they can be
 * asserted directly rather than restated in a test - the same data-and-wiring split
 * `content/npcs/Cows` uses.
 *
 * Sources are the OSRS Wiki's Dairy cow and Cowbells articles, which carry the same figures:
 * Thieving 15 and 16 experience in prose, and the success curve as a `Skilling success chart`
 * with `low1 = 128`, `high1 = 200`, `req1 = 15`. Both cite Mod Ash directly - "50% at level 1
 * (if that were permitted) and 78% at level 99. Interpolate linearly between those."
 */
internal object Cowbells {
    /** Wiki: "if a player has started the Cold War quest and has 15 Thieving". */
    const val LEVEL = 15

    /** Wiki: "Stealing a cowbell provides 16 Thieving experience." */
    const val XP = 16.0

    /**
     * The curve's endpoints, out of 255 - the `low1`/`high1` of the articles' own success
     * chart. 128/255 is Mod Ash's 50% at level 1; 200/255 is his 78% at level 99.
     */
    const val SUCCESS_LOW = 128.0
    const val SUCCESS_HIGH = 200.0

    /**
     * The chance of one attempt succeeding, as a fraction of 1.
     *
     * The standard OSRS skilling interpolation: a value out of 255 moving linearly from
     * [SUCCESS_LOW] at level 1 to [SUCCESS_HIGH] at level 99. At 15 - the lowest level that can
     * reach the option at all - it gives 0.542, which is the "around 54%" the wiki states.
     *
     * The level is **clamped, not extrapolated**: 99 is the end of the published line, so a
     * Thieving boost above 99 buys nothing. Extrapolating would invent a number past the last
     * point Jagex gave.
     */
    fun successChance(level: Int): Double {
        val clamped = level.coerceIn(1, 99)
        return (SUCCESS_LOW * (99 - clamped) + SUCCESS_HIGH * (clamped - 1)) / 98.0 / 255.0
    }
}
