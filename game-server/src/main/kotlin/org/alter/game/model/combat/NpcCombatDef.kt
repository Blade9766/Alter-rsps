package org.alter.game.model.combat

import org.alter.game.model.weightedTableBuilder.LootTable

/**
 * Represents the combat definition for an npc.
 *
 * @author Tom <rspsmods@gmail.com>
 */
data class NpcCombatDef(
    val attack: Int,
    val defence: Int,
    val strength: Int,
    val hitpoints: Int,
    val ranged: Int,
    val magic: Int,
    val attackSpeed: Int,
    val attackAnimation: Int,
    val blockAnimation: Int,
    val deathAnimation: List<Int>,
    val defaultAttackSound: Int,
    val defaultAttackSoundArea: Boolean,
    val defaultAttackSoundRadius: Int,
    val defaultAttackSoundVolume: Int,
    val defaultBlockSound: Int,
    val defaultBlockSoundArea: Boolean,
    val defaultBlockSoundRadius: Int,
    val defaultBlockSoundVolume: Int,
    val defaultDeathSound: Int,
    val defaultDeathSoundArea: Boolean,
    val defaultDeathSoundRadius: Int,
    val defaultDeathSoundVolume: Int,
    val respawnDelay: Int,
    val aggressiveRadius: Int,
    val aggroTargetDelay: Int,
    val aggressiveTimer: Int,
    /**
     * Percentage chance, 0 to 100, that one of this npc's attacks poisons its target.
     *
     * Meaningless without [poisonDamage], and [org.alter.api.NpcCombatBuilder] refuses to build a
     * def that sets one without the other - a chance with no damage is what this field was for
     * years, set on npcs and read by nothing.
     */
    val poisonChance: Double,
    val venomChance: Double,
    val slayerReq: Int,
    val slayerXp: Double,
    val bonuses: List<Int>,
    val species: Set<Any>,
    val LootTables: MutableSet<LootTable>?,
    val immunePoison: Boolean,
    val immuneVenom: Boolean,
    val immuneCannons: Boolean,
    val immuneThralls: Boolean,
    /**
     * Ordinal of [org.alter.api.Elements] this npc is weak to, or -1 for no weakness.
     * Stored as a plain ordinal rather than the enum itself since game-api (where
     * [org.alter.api.Elements] lives) already depends on this module, so this module
     * can't depend back on it.
     */
    val elementalWeaknessElement: Int = -1,
    val elementalWeaknessPercent: Int = 0,
    /**
     * How far away, in tiles, this npc can attack from, or -1 to take the default for
     * its [combatClass] (1 for melee, 7 for ranged, 10 for magic).
     *
     * Measured the way [org.alter.plugins.content.combat.Combat.edgeDistance] measures:
     * between the closest edges of the two footprints, so a large npc's size is already
     * accounted for and should not be added here.
     */
    val attackRange: Int = -1,
    /**
     * Which combat strategy this npc attacks with. Copied onto [org.alter.game.model.entity.Npc.combatClass]
     * at spawn; before this existed every npc was hard-wired to MELEE and the only way
     * to make one shoot or cast was a bespoke per-monster attack loop.
     */
    val combatClass: CombatClass = CombatClass.MELEE,
    /**
     * Which melee style this npc attacks *with*, which decides whose defence bonus its attacks
     * are rolled against - a crush monster against the player's crush defence, and so on.
     *
     * [org.alter.game.model.entity.Npc.combatStyle] defaults to [CombatStyle.STAB] and nothing
     * used to copy a style out of here, so every monster in the game attacked as though it were
     * stabbing and a plate-legged player took the wrong defence against all of them. Plugins that
     * set the style themselves in an `onNpcSpawn` hook still win, because those run after
     * `World.setNpcDefaults`.
     *
     * Only the three melee styles belong here: [org.alter.plugins.content.combat.formula.MeleeCombatFormula]
     * throws on anything else, and a ranged or magic monster's class is [combatClass]'s business.
     */
    val combatStyle: CombatStyle = CombatStyle.STAB,
    /**
     * Spotanim of the projectile a [CombatClass.RANGED] npc fires, or -1 for none.
     */
    val rangedProjectileGfx: Int = -1,
    /**
     * Ordinal of the [org.alter.api.ProjectileType] governing that projectile's flight
     * (start/end height, delay, angle). Stored as a plain ordinal for the same reason
     * as [elementalWeaknessElement]: the enum lives in game-api, which already depends
     * on this module, so this module can't depend back on it. -1 means "arrow".
     */
    val rangedProjectileType: Int = -1,
    /** Spotanim played on the npc as it fires, or -1 for none. */
    val rangedDrawbackGfx: Int = -1,
    val rangedDrawbackHeight: Int = 96,
    /** Spotanim played on the target as the projectile lands, or -1 for none. */
    val rangedImpactGfx: Int = -1,
    val rangedImpactHeight: Int = 0,
    /**
     * Initial poison damage this npc inflicts, or 0 if it cannot poison.
     *
     * This is the wiki infobox's `poisonous = Yes (N)` value: N is the damage the poison starts
     * at, not a chance. The wiki publishes N for every poisonous monster and a chance for none of
     * them, which is why the two are separate fields and why [DEFAULT_POISON_CHANCE] exists.
     */
    val poisonDamage: Int = 0,
) {
    companion object {
        /**
         * Chance applied to an npc that declares [poisonDamage] without a [poisonChance].
         *
         * **This figure is not published anywhere.** The wiki gives every poisonous monster an
         * initial damage and no rate at all, so rather than have each npc invent one, they share
         * this default: the 1/4 the wiki does publish, for a poisoned melee weapon. Set
         * `poisonChance` explicitly on any npc that should differ.
         */
        const val DEFAULT_POISON_CHANCE = 25.0

        private const val DEFAULT_HITPOINTS = 10
        private const val DEFAULT_ATTACK_SPEED = 4
        private const val DEFAULT_RESPAWN_DELAY = 25
        private const val DEFAULT_ATTACK_ANIMATION = 422
        private const val DEFAULT_BLOCK_ANIMATION = 424
        private const val DEFAULT_DEATH_ANIMATION = 836

        val DEFAULT =
            NpcCombatDef(
                hitpoints = DEFAULT_HITPOINTS,
                attack = 0,
                defence = 0,
                strength = 0,
                ranged = 0,
                magic = 0,
                attackSpeed = DEFAULT_ATTACK_SPEED,
                aggressiveRadius = 0,
                aggroTargetDelay = 0,
                aggressiveTimer = 0,
                attackAnimation = DEFAULT_ATTACK_ANIMATION,
                blockAnimation = DEFAULT_BLOCK_ANIMATION,
                deathAnimation = listOf(DEFAULT_DEATH_ANIMATION),
                defaultAttackSound = -1,
                defaultAttackSoundArea = false,
                defaultAttackSoundRadius = -1,
                defaultAttackSoundVolume = -1,
                defaultBlockSound = -1,
                defaultBlockSoundArea = false,
                defaultBlockSoundRadius = -1,
                defaultBlockSoundVolume = -1,
                defaultDeathSound = -1,
                defaultDeathSoundArea = false,
                defaultDeathSoundRadius = -1,
                defaultDeathSoundVolume = -1,
                respawnDelay = DEFAULT_RESPAWN_DELAY,
                poisonChance = 0.0,
                venomChance = 0.0,
                slayerReq = 1,
                slayerXp = 0.0,
                bonuses = emptyList(),
                species = emptySet(),
                LootTables = null,
                immunePoison = false,
                immuneVenom = false,
                immuneCannons = false,
                immuneThralls = false
            )
    }
}
