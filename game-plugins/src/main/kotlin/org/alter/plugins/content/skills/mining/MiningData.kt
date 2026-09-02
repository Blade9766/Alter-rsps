package org.alter.plugins.content.skills.mining

/**
 * Immutable configuration representing a mineable rock type loaded from JSON.
 *
 * Deliberately has no equivalent of Woodcutting's `depleteChance`: every one of these
 * ore rocks depletes after a single ore in OSRS, fast-respawning clay and copper
 * included. It also has no stump/depleted-object field, because the depleted rock is
 * derived from the mined rock's own cache model at runtime rather than configured per
 * ore - see `MiningPlugin.depletedRockFor`.
 */
data class RockEntry(
    /** Ore name, used only for the "You manage to mine some <name>." message. */
    val name: String,
    val objects: List<String>,
    /** Ticks before the rock returns. Wiki-sourced per ore, at 0.6s per tick. */
    val respawnTicks: Int,
    val level: Int,
    val experience: Double,
    val ore: String,
    /**
     * Chance of getting an ore on a single swing at exactly [level] (the rock's own
     * requirement) holding the worst pickaxe usable there, before any pickaxe-tier bonus.
     */
    val baseChance: Double = 0.30,
    /**
     * The same chance at Mining 99. [baseChance] is interpolated up to this as the
     * player levels past [level] - see MiningPlugin.mineChance.
     */
    val maxChance: Double = 0.75,
) {
    @Transient
    var objectIds: IntArray = intArrayOf()

    @Transient
    var oreItemId: Int = -1

    init {
        require(name.isNotBlank()) { "Rock entry must define a name." }
        require(objects.isNotEmpty()) { "Rock entry must define at least one object id." }
        require(respawnTicks >= 1) { "Rock respawn ticks must be at least 1." }
        require(level >= 1) { "Rock level requirement must be >= 1." }
        require(experience >= 0.0) { "Rock experience cannot be negative." }
        require(ore.isNotBlank()) { "Rock entry must define an ore item." }
        require(baseChance > 0.0 && baseChance <= 1.0) { "Rock base chance must be in (0, 1]." }
        require(maxChance in baseChance..1.0) { "Rock max chance must be between baseChance and 1." }
    }
}
