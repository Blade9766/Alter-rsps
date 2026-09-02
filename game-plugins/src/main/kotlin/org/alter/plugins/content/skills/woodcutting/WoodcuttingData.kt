package org.alter.plugins.content.skills.woodcutting

/**
 * Immutable configuration representing a choppable tree species loaded from JSON.
 */
data class TreeEntry(
    val objects: List<String>,
    val stumpObject: String? = null,
    val respawnTicksMin: Int,
    val respawnTicksMax: Int = respawnTicksMin,
    val level: Int,
    val experience: Double,
    val log: String,
    /**
     * Chance, per successfully chopped log, that the tree depletes and needs to
     * respawn. Regular trees always deplete after a single log; Oak and rarer trees
     * have a much smaller chance per log (OSRS uses 1/8 for these).
     */
    val depleteChance: Double = 1.0,
    /**
     * Chance of getting a log on a single swing at exactly [level] (the tree's own
     * requirement) holding the worst axe usable there, before any axe-tier bonus.
     */
    val baseChance: Double = 0.35,
    /**
     * The same chance at Woodcutting 99. [baseChance] is interpolated up to this as
     * the player levels past [level] - see WoodcuttingPlugin.chopChance.
     */
    val maxChance: Double = 0.75,
) {
    @Transient
    var objectIds: IntArray = intArrayOf()

    @Transient
    var stumpObjectId: Int = -1

    @Transient
    var logItemId: Int = -1

    init {
        require(objects.isNotEmpty()) { "Tree entry must define at least one object id." }
        require(respawnTicksMin >= 1) { "Tree respawn ticks must be at least 1." }
        require(respawnTicksMax >= respawnTicksMin) { "Tree respawn max ticks cannot be less than min." }
        require(level >= 1) { "Tree level requirement must be >= 1." }
        require(experience >= 0.0) { "Tree experience cannot be negative." }
        require(depleteChance in 0.0..1.0) { "Tree deplete chance must be between 0 and 1." }
        require(log.isNotBlank()) { "Tree entry must define a log item." }
        require(baseChance > 0.0 && baseChance <= 1.0) { "Tree base chance must be in (0, 1]." }
        require(maxChance in baseChance..1.0) { "Tree max chance must be between baseChance and 1." }
    }
}
