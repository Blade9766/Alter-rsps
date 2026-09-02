package org.alter.plugins.content.combat.strategy.magic

import org.alter.api.Elements

/**
 * Element and group membership for the Standard spellbook's 20 elemental combat spells
 * (Strike/Bolt/Blast/Wave/Surge x Wind/Water/Earth/Fire). Kept separate from
 * [CombatSpell] itself rather than adding fields to its constructor, since only these
 * 20 of its 36 entries (Ancient Magicks has none of this) need either piece of data.
 *
 * Two real OSRS mechanics depend on this, both applied in
 * [org.alter.plugins.content.combat.formula.MagicCombatFormula]:
 * - **Group max hit scaling**: within a group (e.g. all four Bolt spells), every
 *   element's effective max hit is the max hit of the *highest-level spell in that
 *   group the caster's current Magic level reaches* - not each spell's own individually
 *   lower max hit. See [SpellGroup.effectiveMaxHit].
 * - **Elemental weakness**: some monsters take extra accuracy/damage from a specific
 *   element, scaled 1:1 with their weakness percentage (100% weakness = double
 *   accuracy and damage). See [elementOf].
 */
internal enum class SpellGroup {
    STRIKE,
    BOLT,
    BLAST,
    WAVE,
    SURGE,
}

private val ELEMENT_BY_SPELL: Map<CombatSpell, Elements> =
    mapOf(
        CombatSpell.WIND_STRIKE to Elements.AIR,
        CombatSpell.WIND_BOLT to Elements.AIR,
        CombatSpell.WIND_BLAST to Elements.AIR,
        CombatSpell.WIND_WAVE to Elements.AIR,
        CombatSpell.WIND_SURGE to Elements.AIR,
        CombatSpell.WATER_STRIKE to Elements.WATER,
        CombatSpell.WATER_BOLT to Elements.WATER,
        CombatSpell.WATER_BLAST to Elements.WATER,
        CombatSpell.WATER_WAVE to Elements.WATER,
        CombatSpell.WATER_SURGE to Elements.WATER,
        CombatSpell.EARTH_STRIKE to Elements.EARTH,
        CombatSpell.EARTH_BOLT to Elements.EARTH,
        CombatSpell.EARTH_BLAST to Elements.EARTH,
        CombatSpell.EARTH_WAVE to Elements.EARTH,
        CombatSpell.EARTH_SURGE to Elements.EARTH,
        CombatSpell.FIRE_STRIKE to Elements.FIRE,
        CombatSpell.FIRE_BOLT to Elements.FIRE,
        CombatSpell.FIRE_BLAST to Elements.FIRE,
        CombatSpell.FIRE_WAVE to Elements.FIRE,
        CombatSpell.FIRE_SURGE to Elements.FIRE,
    )

private val GROUP_BY_SPELL: Map<CombatSpell, SpellGroup> =
    mapOf(
        CombatSpell.WIND_STRIKE to SpellGroup.STRIKE,
        CombatSpell.WATER_STRIKE to SpellGroup.STRIKE,
        CombatSpell.EARTH_STRIKE to SpellGroup.STRIKE,
        CombatSpell.FIRE_STRIKE to SpellGroup.STRIKE,
        CombatSpell.WIND_BOLT to SpellGroup.BOLT,
        CombatSpell.WATER_BOLT to SpellGroup.BOLT,
        CombatSpell.EARTH_BOLT to SpellGroup.BOLT,
        CombatSpell.FIRE_BOLT to SpellGroup.BOLT,
        CombatSpell.WIND_BLAST to SpellGroup.BLAST,
        CombatSpell.WATER_BLAST to SpellGroup.BLAST,
        CombatSpell.EARTH_BLAST to SpellGroup.BLAST,
        CombatSpell.FIRE_BLAST to SpellGroup.BLAST,
        CombatSpell.WIND_WAVE to SpellGroup.WAVE,
        CombatSpell.WATER_WAVE to SpellGroup.WAVE,
        CombatSpell.EARTH_WAVE to SpellGroup.WAVE,
        CombatSpell.FIRE_WAVE to SpellGroup.WAVE,
        CombatSpell.WIND_SURGE to SpellGroup.SURGE,
        CombatSpell.WATER_SURGE to SpellGroup.SURGE,
        CombatSpell.EARTH_SURGE to SpellGroup.SURGE,
        CombatSpell.FIRE_SURGE to SpellGroup.SURGE,
    )

private val SPELLS_BY_GROUP: Map<SpellGroup, List<CombatSpell>> = GROUP_BY_SPELL.entries.groupBy({ it.value }, { it.key })

internal fun elementOf(spell: CombatSpell): Elements? = ELEMENT_BY_SPELL[spell]

internal fun groupOf(spell: CombatSpell): SpellGroup? = GROUP_BY_SPELL[spell]

internal fun spellsInGroup(group: SpellGroup): List<CombatSpell> = SPELLS_BY_GROUP[group].orEmpty()
