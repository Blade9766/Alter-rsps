package org.alter.plugins.content.objects.gates

/**
 * @author Tom <rspsmods@gmail.com>
 */
data class GateSet(
    val closed: Gate,
    val opened: Gate,
    val requirement: GateRequirement? = null,
)

/**
 * A skill level a player must have before a gate will open for them. Optional - almost no gate has
 * one - and absent from the json for every gate that does not, in which case Gson leaves it null.
 *
 * [skill] is the skill's in-game name, matched case-insensitively against the cache's own skill
 * name enum, so the config never has to carry a raw skill index.
 */
data class GateRequirement(val skill: String, val level: Int)
