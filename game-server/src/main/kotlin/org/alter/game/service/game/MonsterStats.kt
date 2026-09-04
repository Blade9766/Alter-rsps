package org.alter.game.service.game

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.combat.NpcCombatDef

/**
 * One monster's combat stats, as `data/cfg/npcs/monsterStats.json` records them.
 *
 * Every field is optional and absent means "the default". Jackson binds these by field rather than
 * through the constructor, so an absent number arrives as `0` and *not* as the Kotlin default -
 * the same trap `ItemMetadataService.Metadata` documents - which is why [attackSpeed] is range
 * checked in [toCombatDef] rather than trusted.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MonsterStats(
    @field:JsonProperty("id") val id: Int = -1,
    /** The cache name at the time the file was generated. Read only by the verify test. */
    @field:JsonProperty("name") val name: String? = null,
    @field:JsonProperty("hitpoints") val hitpoints: Int = 0,
    @field:JsonProperty("attack") val attack: Int = 0,
    @field:JsonProperty("strength") val strength: Int = 0,
    @field:JsonProperty("defence") val defence: Int = 0,
    @field:JsonProperty("magic") val magic: Int = 0,
    @field:JsonProperty("ranged") val ranged: Int = 0,
    @field:JsonProperty("attackSpeed") val attackSpeed: Int = 0,
    @field:JsonProperty("combatStyle") val combatStyle: String? = null,
    @field:JsonProperty("bonuses") val bonuses: MonsterBonuses? = null,
    @field:JsonProperty("species") val species: List<String> = emptyList(),
    @field:JsonProperty("weakness") val weakness: MonsterWeakness? = null,
) {
    /**
     * This monster's stats as a combat definition, starting from [NpcCombatDef.DEFAULT] so that
     * everything the file does not describe - animations, sounds, respawn delay, aggression,
     * poison, slayer requirement, loot - keeps the value the rest of the server already assumes.
     *
     * Notably absent, and deliberately: [NpcCombatDef.combatClass]. The wiki knows which monsters
     * shoot and which cast, but `MagicCombatStrategy` reads a casting spell an npc never has, so
     * declaring a monster MAGIC here would throw the moment it attacked. Ranged and magic monsters
     * therefore keep their real levels and bonuses while still swinging as melee, which is what
     * they did before this file existed.
     */
    fun toCombatDef(): NpcCombatDef =
        NpcCombatDef.DEFAULT.copy(
            hitpoints = if (hitpoints > 0) hitpoints else NpcCombatDef.DEFAULT.hitpoints,
            attack = attack,
            strength = strength,
            defence = defence,
            magic = magic,
            ranged = ranged,
            attackSpeed = if (attackSpeed > 0) attackSpeed else NpcCombatDef.DEFAULT.attackSpeed,
            combatStyle = MELEE_STYLES[combatStyle] ?: CombatStyle.STAB,
            bonuses = (bonuses ?: MonsterBonuses()).toList(),
            species = resolveSpecies(species),
            elementalWeaknessElement = weakness?.let { ELEMENT_ORDINALS[it.element] } ?: -1,
            elementalWeaknessPercent = weakness?.percent ?: 0,
        )

    companion object {
        private val MELEE_STYLES =
            mapOf("STAB" to CombatStyle.STAB, "SLASH" to CombatStyle.SLASH, "CRUSH" to CombatStyle.CRUSH)

        /**
         * Ordinals of `org.alter.api.Elements`, which cannot be imported here: it lives in
         * game-api, and game-api already depends on this module. [NpcCombatDef] stores the
         * weakness as a bare ordinal for the same reason.
         */
        private val ELEMENT_ORDINALS = mapOf("EARTH" to 0, "AIR" to 1, "WATER" to 2, "FIRE" to 3)

        /**
         * `org.alter.api.NpcSpecies` constants by name, or empty if that class is not on the
         * classpath.
         *
         * Reflection for the same module-direction reason as [ELEMENT_ORDINALS], but here an
         * ordinal is not enough: `Npc.isSpecies` compares the set's contents against real
         * `NpcSpecies` values, so nothing but the enum constants themselves will match.
         */
        private val speciesConstants: Map<String, Any> by lazy {
            runCatching {
                Class.forName("org.alter.api.NpcSpecies").enumConstants
                    .filterIsInstance<Enum<*>>()
                    .associateBy { it.name }
            }.getOrDefault(emptyMap())
        }

        private fun resolveSpecies(names: List<String>): Set<Any> =
            names.mapNotNullTo(LinkedHashSet()) { speciesConstants[it] }
    }
}

/**
 * The bonuses, laid out the way an npc's are rather than an item's: a monster has one attack bonus
 * applied to whichever style it uses, not five.
 *
 * Positions follow `org.alter.api.BonusSlot` for the first ten and the `NPC_*_BONUS_INDEX`
 * constants in `org.alter.api.ext.NpcExt` for the last four. [attack] is written into all three
 * melee attack slots as well as slot 10, because `MeleeCombatFormula` picks the attacker's bonus
 * by combat style while `Combat.getNpcXpMultiplier` reads slot 10.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MonsterBonuses(
    @field:JsonProperty("attack") val attack: Int = 0,
    @field:JsonProperty("strength") val strength: Int = 0,
    @field:JsonProperty("magicAttack") val magicAttack: Int = 0,
    @field:JsonProperty("rangedAttack") val rangedAttack: Int = 0,
    @field:JsonProperty("rangedStrength") val rangedStrength: Int = 0,
    @field:JsonProperty("magicDamage") val magicDamage: Int = 0,
    @field:JsonProperty("defenceStab") val defenceStab: Int = 0,
    @field:JsonProperty("defenceSlash") val defenceSlash: Int = 0,
    @field:JsonProperty("defenceCrush") val defenceCrush: Int = 0,
    @field:JsonProperty("defenceMagic") val defenceMagic: Int = 0,
    @field:JsonProperty("defenceRanged") val defenceRanged: Int = 0,
) {
    fun toList(): List<Int> =
        listOf(
            attack, attack, attack, magicAttack, rangedAttack,
            defenceStab, defenceSlash, defenceCrush, defenceMagic, defenceRanged,
            attack, strength, rangedStrength, magicDamage,
        )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MonsterWeakness(
    @field:JsonProperty("element") val element: String? = null,
    @field:JsonProperty("percent") val percent: Int = 0,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MonsterStatsConfig(
    @field:JsonProperty("monsters") val monsters: List<MonsterStats> = emptyList(),
)
