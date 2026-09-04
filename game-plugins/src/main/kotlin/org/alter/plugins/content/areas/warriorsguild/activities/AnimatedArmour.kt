package org.alter.plugins.content.areas.warriorsguild.activities

/**
 * The seven suits of armour a Magical Animator will bring to life, and what each is worth.
 *
 * A tier is defined by three items - full helm, platebody, platelegs of one metal - which is
 * exactly what the animator consumes, and by the npc that stands up in their place.
 *
 * ## The numbers
 *
 * Combat levels, hitpoints and token payouts are the wiki's Animated armour table. They line up
 * with the npc definitions in this cache: 2450-2456 read back as combat 11, 23, 46, 69, 92, 113
 * and 138, matching the table row for row.
 *
 * Attack, Strength and Defence levels are **not published per suit** - the infobox for each gives
 * only the levels shown above - so each suit is given levels equal to its own hitpoints, the
 * relationship the bronze suit's published block (10 hitpoints, 10/10/10) actually shows. That is
 * an extrapolation from one data point and the most likely thing here to be wrong, but it produces
 * a monster that scales sensibly across the seven tiers rather than seven identical ones.
 *
 * ## Losing pieces
 *
 * "For bronze, iron, and steel variants, some armour pieces may break during combat"; from black
 * upward the player gets all three back. [pieceReturnChance] is 90% for the first three and
 * certain for the rest, rolled per piece.
 */
internal enum class AnimatedArmour(
    val npc: String,
    val combatLevel: Int,
    val hitpoints: Int,
    val tokens: Int,
    val helm: String,
    val body: String,
    val legs: String,
) {
    BRONZE("npc.animated_bronze_armour", 11, 10, 5, "item.bronze_full_helm", "item.bronze_platebody", "item.bronze_platelegs"),
    IRON("npc.animated_iron_armour", 23, 20, 10, "item.iron_full_helm", "item.iron_platebody", "item.iron_platelegs"),
    STEEL("npc.animated_steel_armour", 46, 40, 15, "item.steel_full_helm", "item.steel_platebody", "item.steel_platelegs"),
    BLACK("npc.animated_black_armour", 69, 60, 20, "item.black_full_helm", "item.black_platebody", "item.black_platelegs"),
    MITHRIL("npc.animated_mithril_armour", 92, 80, 25, "item.mithril_full_helm", "item.mithril_platebody", "item.mithril_platelegs"),
    ADAMANT("npc.animated_adamant_armour", 113, 99, 30, "item.adamant_full_helm", "item.adamant_platebody", "item.adamant_platelegs"),
    RUNE("npc.animated_rune_armour", 138, 120, 40, "item.rune_full_helm", "item.rune_platebody", "item.rune_platelegs"),
    ;

    val pieces: List<String> get() = listOf(helm, body, legs)

    /** See the class comment - an extrapolation from the bronze suit, the only one published. */
    val combatLevels: Int get() = hitpoints

    /** Per piece, on the suit's death. Certain from black upward. */
    val pieceReturnChance: Double
        get() = if (ordinal <= STEEL.ordinal) 0.9 else 1.0

    companion object {
        val values = enumValues<AnimatedArmour>()

        fun byNpc(key: String): AnimatedArmour? = values.firstOrNull { it.npc == key }

        /**
         * Four ticks, like every other melee monster here.
         *
         * The suits' own attack speed is not published separately; the bronze infobox gives 4 and
         * nothing suggests the others differ.
         */
        const val ATTACK_SPEED = 4

        /**
         * The suits share one attack animation across every tier.
         *
         * All six ids with observed data (2451-2456) include 388 - a human melee swing, which is
         * what a walking suit of armour uses - and no other animation is common to all of them.
         * 4166 and 4167 are the pair every suit also observes; 4167 is taken as the death, on the
         * convention that death is the highest of a set (91 of the 130 named groups follow it).
         *
         * **No block animation is set.** The observed sets contain nothing that could be one, and
         * 4166 is far more likely the "I'm ALIVE!" rise than a flinch - using it would make the
         * armour re-assemble itself every time it was hit.
         */
        const val ATTACK_ANIM = 388

        const val DEATH_ANIM = 4167

        const val NO_BLOCK_ANIM = -1

        /** The suits do not respawn: each one exists because a player fed an animator. */
        const val RESPAWN_CYCLES = 50
    }
}
