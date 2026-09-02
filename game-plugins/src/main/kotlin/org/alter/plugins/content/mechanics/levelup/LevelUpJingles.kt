package org.alter.plugins.content.mechanics.levelup

import org.alter.api.Skills

/**
 * Cache ids of the per-skill "level up" jingles, played by [LevelUpPlugin].
 *
 * Every id here is the `cacheid` from that jingle's own OSRS Wiki infobox (the
 * Jingles article itself lists the tracks by name only, with no ids). They are *not*
 * guessable from the skill order: they mostly run alphabetically by skill name in
 * steps of two from 28, but Farming breaks that completely at 10 - it was added to
 * the game long after the others - which is exactly why each was looked up rather
 * than extrapolated.
 *
 * Three skills have **two** jingles rather than one, which is why they need
 * [jingleFor] rather than a flat map:
 * - Hitpoints and Strength each use one track below level 50 and a different one from
 *   50 up ("a jingle for levelling X before level 50" / "after level 49").
 * - Hunter alternates by the parity of the level reached ("to an even level" / "to an
 *   odd level").
 */
object LevelUpJingles {
    private val SINGLE =
        mapOf(
            Skills.AGILITY to 28,
            Skills.ATTACK to 29,
            Skills.CONSTRUCTION to 31,
            Skills.COOKING to 33,
            Skills.CRAFTING to 35,
            Skills.DEFENCE to 37,
            Skills.FARMING to 10,
            Skills.FIREMAKING to 39,
            Skills.FISHING to 41,
            Skills.FLETCHING to 43,
            Skills.HERBLORE to 45,
            Skills.MAGIC to 51,
            Skills.MINING to 53,
            Skills.PRAYER to 55,
            Skills.RANGED to 57,
            Skills.RUNECRAFTING to 59,
            Skills.SLAYER to 61,
            Skills.SMITHING to 63,
            Skills.THIEVING to 67,
            Skills.WOODCUTTING to 69,
        )

    private const val HITPOINTS_EARLY = 47
    private const val HITPOINTS_LATE = 48
    private const val HUNTER_EVEN = 49
    private const val HUNTER_ODD = 50
    private const val STRENGTH_EARLY = 65
    private const val STRENGTH_LATE = 66

    /** The level at which Hitpoints and Strength switch to their second jingle. */
    private const val LATE_JINGLE_LEVEL = 50

    /**
     * @param newLevel the level just reached, which decides the variant for the three
     *   skills that have one.
     * @return the jingle's cache id, or -1 if the skill has no known jingle.
     */
    fun jingleFor(
        skill: Int,
        newLevel: Int,
    ): Int =
        when (skill) {
            Skills.HITPOINTS -> if (newLevel >= LATE_JINGLE_LEVEL) HITPOINTS_LATE else HITPOINTS_EARLY
            Skills.STRENGTH -> if (newLevel >= LATE_JINGLE_LEVEL) STRENGTH_LATE else STRENGTH_EARLY
            Skills.HUNTER -> if (newLevel % 2 == 0) HUNTER_EVEN else HUNTER_ODD
            else -> SINGLE[skill] ?: -1
        }
}
