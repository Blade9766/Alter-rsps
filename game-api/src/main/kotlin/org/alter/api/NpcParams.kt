package org.alter.api

import org.alter.api.ext.enumSetOf
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.combat.NpcCombatDef
import org.alter.game.model.weightedTableBuilder.LootTable

/**
 * @author Cl0udS3c
 */
data class MeleeDefence(val stab: Int, val slash: Int, val crush: Int)
data class RangeDefence(val darts: Int, val arrows: Int, val bolts: Int)

data class MagicDefence(val magic: Int, val elementsWeakness: ElementalWeakness?)

enum class Elements {
    EARTH,
    AIR,
    WATER,
    FIRE
}
data class ElementalWeakness(var element: Elements, var percent: Int)





/**
 * @author Tom <rspsmods@gmail.com>
 */
object NpcSkills {
    const val ATTACK = 0
    const val STRENGTH = 1
    const val DEFENCE = 2
    const val MAGIC = 3
    const val RANGED = 4
}

/**
 * @author Tom <rspsmods@gmail.com>
 * https://oldschool.runescape.wiki/w/Monster_attribute
 */
enum class NpcSpecies {
    DEMON,
    SHADE,
    KALPHITE,
    SCARAB,
    DRACONIC,
    BASIC_DRAGON,
    BRUTAL_DRAGON,
    FIERY,
    UNDEAD,
    XERICIAN,
    GOLEM,
    PENANCE,
    RAT,
    SPECTRAL,
    VAMPYRE
}

/**
 * @author Tom <rspsmods@gmail.com>
 */
class NpcCombatBuilder {
    private var maxHealth = -1

    private var attackSpeed = -1

    private var attack : Int = 0
    private var defence : Int = 0
    private var strength : Int = 0
    private var hitpoints : Int = 0
    private var ranged : Int = 0
    private var magic : Int = 0

    private var defaultAttackAnim = -1

    private var defaultBlockAnim = -1

    private val deathAnimList = mutableListOf<Int>()

    private var defaultAttackSound = -1
    private var defaultAttackSoundArea: Boolean = false
    private var defaultAttackSoundRadius: Int = -1
    private var defaultAttackSoundVolume: Int = -1

    private var defaultBlockSound = -1
    private var defaultBlockSoundArea: Boolean = false
    private var defaultBlockSoundRadius: Int = -1
    private var defaultBlockSoundVolume: Int = -1

    private var defaultDeathSound = -1
    private var defaultDeathSoundArea: Boolean = false
    private var defaultDeathSoundRadius: Int = -1
    private var defaultDeathSoundVolume: Int = -1

    private var respawnDelay = -1

    private var aggroRadius = -1

    private var aggroTargetDelay = -1

    private var aggroTimer = -1

    private var poisonChance = -1.0

    private var poisonDamage = -1

    private var venomChance = -1.0

    private var slayerReq = -1

    private var slayerXp = -1.0

    private val bonuses = Array(BONUS_COUNT) { 0 }

    private val speciesSet = enumSetOf<NpcSpecies>()

    private var immunePoison = false
    private var immuneVenom = false
    private var immuneCannons = false
    private var immuneThralls = false
    private var elementalWeaknessElement = -1
    private var elementalWeaknessPercent = 0

    private var attackRange = -1

    private var combatClass = CombatClass.MELEE
    private var combatStyle = CombatStyle.STAB
    private var rangedProjectileGfx = -1
    private var rangedProjectileType = -1
    private var rangedDrawbackGfx = -1
    private var rangedDrawbackHeight = 96
    private var rangedImpactGfx = -1
    private var rangedImpactHeight = 0

    var LootTable: MutableSet<LootTable> = mutableSetOf()

    fun build(): NpcCombatDef {
        /**
         * @TODO Add indentifier if check fails
         */
        check(maxHealth != -1) { "Max health must be set. ${Throwable().stackTrace[3].fileName}" }
        check(attackSpeed != -1) { "Attack speed must be set. ${Throwable().stackTrace[3].fileName}" }
        check(deathAnimList.isNotEmpty()) { "A death animation must be set. ${Throwable().stackTrace[3].fileName}" }
        check(respawnDelay != -1) { "Respawn delay must be set. ${Throwable().stackTrace[3].fileName}" }

        /*
         * Poison needs both halves or it is theatre. A chance with no damage was the state this
         * codebase sat in for years - npcs declared `poisonChance` and nothing anywhere read it -
         * so it fails loudly now rather than looking wired.
         *
         * Damage without a chance is the ordinary case, because the wiki publishes the damage for
         * every poisonous monster and a rate for none, so that one takes the shared default.
         */
        check(poisonChance <= 0.0 || poisonDamage > 0) {
            "Poison chance was set without a poison damage - the npc would never poison anything. " +
                "${Throwable().stackTrace[3].fileName}"
        }
        if (poisonDamage > 0 && poisonChance <= 0.0) {
            poisonChance = NpcCombatDef.DEFAULT_POISON_CHANCE
        }

        poisonChance = Math.max(0.0, poisonChance)
        poisonDamage = Math.max(0, poisonDamage)
        venomChance = Math.max(0.0, venomChance)
        slayerReq = Math.max(1, slayerReq)
        slayerXp = Math.max(0.0, slayerXp)

        /*
         * An `aggro { }` block that declares only a radius reads like it makes the npc
         * aggressive, and both of the other two fields are load-bearing for that:
         *
         *  - `aggroTargetDelay` gates `NpcAggroPlugin`'s `onGlobalNpcSpawn`, which installs
         *    `npc.aggroCheck` and schedules the sweep timer only when it is `> 0`. Left at -1
         *    the npc never sweeps for a target at all.
         *  - `aggressiveTimer` is what that plugin's default check compares against, and no
         *    absolute value is ever `<= -1`, so an unset timer makes the check reject every
         *    player and the npc is *silently passive* even once it does sweep.
         *
         * Neither has any sensible meaning as "unset" once a radius exists, so both take a
         * default here rather than making every caller restate the same two numbers.
         */
        if (aggroRadius > 0 && aggroTargetDelay == -1) {
            aggroTargetDelay = DEFAULT_AGGRO_SEARCH_DELAY
        }

        if (aggroTimer == -1) {
            aggroTimer = DEFAULT_AGGRO_TIMER
        }

        return NpcCombatDef(
            hitpoints = maxHealth,
            attack = attack,
            defence = defence,
            strength = strength,
            ranged = ranged,
            magic = magic,
            attackSpeed = attackSpeed,
            attackAnimation = defaultAttackAnim,
            blockAnimation = defaultBlockAnim,
            deathAnimation = deathAnimList,
            defaultAttackSound = defaultAttackSound,
            defaultAttackSoundArea = defaultAttackSoundArea,
            defaultAttackSoundRadius = defaultAttackSoundRadius,
            defaultAttackSoundVolume = defaultAttackSoundVolume,
            defaultBlockSound = defaultBlockSound,
            defaultBlockSoundArea = defaultBlockSoundArea,
            defaultBlockSoundRadius = defaultBlockSoundRadius,
            defaultBlockSoundVolume = defaultBlockSoundVolume,
            defaultDeathSound = defaultDeathSound,
            defaultDeathSoundArea = defaultDeathSoundArea,
            defaultDeathSoundRadius = defaultDeathSoundRadius,
            defaultDeathSoundVolume = defaultDeathSoundVolume,
            respawnDelay = respawnDelay,
            aggressiveRadius = aggroRadius,
            aggroTargetDelay = aggroTargetDelay,
            aggressiveTimer = aggroTimer,
            poisonChance = poisonChance,
            poisonDamage = poisonDamage,
            venomChance = venomChance,
            slayerReq = slayerReq,
            slayerXp = slayerXp,
            bonuses = bonuses.toList(),
            species = speciesSet,
            LootTables = LootTable,
            immunePoison = immunePoison,
            immuneVenom = immuneVenom,
            immuneCannons = immuneCannons,
            immuneThralls = immuneThralls,
            elementalWeaknessElement = elementalWeaknessElement,
            elementalWeaknessPercent = elementalWeaknessPercent,
            attackRange = attackRange,
            combatClass = combatClass,
            combatStyle = combatStyle,
            rangedProjectileGfx = rangedProjectileGfx,
            rangedProjectileType = rangedProjectileType,
            rangedDrawbackGfx = rangedDrawbackGfx,
            rangedDrawbackHeight = rangedDrawbackHeight,
            rangedImpactGfx = rangedImpactGfx,
            rangedImpactHeight = rangedImpactHeight,
        )
    }

    /**
     * Overrides how far this npc can attack from. -1 leaves it on its combat class's
     * default, which is what every npc used before this existed - ranged monsters were
     * all stuck on the same hardcoded 7 and casters on 10, regardless of what the wiki
     * gives them.
     */
    fun setAttackRange(range: Int): NpcCombatBuilder {
        attackRange = range
        return this
    }

    fun setCombatClass(combatClass: CombatClass): NpcCombatBuilder {
        this.combatClass = combatClass
        return this
    }

    /**
     * Which melee style this npc attacks with, and so whose defence bonus its hits are rolled
     * against. See [NpcCombatDef.combatStyle]; only STAB, SLASH and CRUSH are meaningful.
     */
    fun setCombatStyle(style: CombatStyle): NpcCombatBuilder {
        this.combatStyle = style
        return this
    }

    /**
     * Configures the projectile a ranged npc fires. Setting it also marks the npc as a
     * ranged attacker, since a projectile is only meaningful for one.
     */
    fun setRangedProjectile(
        gfx: Int,
        type: ProjectileType,
        drawbackGfx: Int,
        drawbackHeight: Int,
        impactGfx: Int,
        impactHeight: Int,
    ): NpcCombatBuilder {
        combatClass = CombatClass.RANGED
        rangedProjectileGfx = gfx
        rangedProjectileType = type.ordinal
        rangedDrawbackGfx = drawbackGfx
        rangedDrawbackHeight = drawbackHeight
        rangedImpactGfx = impactGfx
        rangedImpactHeight = impactHeight
        return this
    }

    fun setElementalWeakness(
        element: Elements,
        percent: Int,
    ): NpcCombatBuilder {
        elementalWeaknessElement = element.ordinal
        elementalWeaknessPercent = percent
        return this
    }

    fun setAttackSoundArea(attackSoundArea: Boolean) {
        defaultAttackSoundArea = attackSoundArea
    }

    fun setAttackSoundRadius(soundRadius: Int) {
        defaultAttackSoundRadius = soundRadius
    }

    fun setAttackSoundVolume(soundVolume: Int) {
        defaultAttackSoundVolume = soundVolume
    }

    fun setBlockSoundArea(blockSoundArea: Boolean) {
        defaultBlockSoundArea = blockSoundArea
    }

    fun setBlockSoundRadius(blockSoundRadius: Int) {
        defaultBlockSoundRadius = blockSoundRadius
    }

    fun setBlockSoundVolume(blockSoundVolume: Int) {
        defaultBlockSoundVolume = blockSoundVolume
    }

    fun setDeathSoundArea(deathSoundArea: Boolean) {
        defaultDeathSoundArea = deathSoundArea
    }

    fun setDeathSoundRadius(DeathSoundRadius: Int) {
        defaultDeathSoundRadius = DeathSoundRadius
    }

    fun setDeathSoundVolume(DeathSoundVolume: Int) {
        defaultDeathSoundVolume = DeathSoundVolume
    }

    fun setHitpoints(health: Int): NpcCombatBuilder {
        check(maxHealth == -1) { "Max health already set. ${Throwable().stackTrace[2].fileName}" }
        maxHealth = health
        return this
    }

    /**
     * @param speed the attack speed, in cycles.
     */
    fun setAttackSpeed(speed: Int): NpcCombatBuilder {
        check(attackSpeed == -1) { " Attack speed already set, ${Throwable().stackTrace[2].fileName}" }
        attackSpeed = speed
        return this
    }

    fun setAttackLevel(level: Int): NpcCombatBuilder {
        check( attack == 0) { "Attack level already set. ${Throwable().stackTrace[2].fileName}" }
        attack = level
        return this
    }

    fun setStrengthLevel(level: Int): NpcCombatBuilder {
        check(strength == 0) { "Strength level already set. ${Throwable().stackTrace[2].fileName}" }
        strength = level
        return this
    }

    fun setDefenceLevel(level: Int): NpcCombatBuilder {
        check(defence == 0) { "Defence level already set. ${Throwable().stackTrace[2].fileName}" }
        defence = level
        return this
    }

    fun setMagicLevel(level: Int): NpcCombatBuilder {
        check(magic == 0) { "Magic level already set. ${Throwable().stackTrace[2].fileName}" }
        magic = level
        return this
    }

    fun setRangedLevel(level: Int): NpcCombatBuilder {
        check(ranged == 0) { "Ranged level already set. ${Throwable().stackTrace[2].fileName}" }
        ranged = level
        return this
    }
    // @TODO Add this back for different Skills As for now it's not really needed.
   // fun setLevel(
   //     index: Int,
   //     level: Int,
   // ): NpcCombatBuilder {
   //     check(stats[index] == -1) { "Level [$index] already set. ${Throwable().stackTrace[2].fileName}" }
   //     stats[index] = level
   //     return this
   // }

    fun setLevels(
        attack: Int,
        strength: Int,
        defence: Int,
        magic: Int,
        ranged: Int,
    ): NpcCombatBuilder {
        setAttackLevel(attack)
        setDefenceLevel(defence)
        setStrengthLevel(strength)
        setMagicLevel(magic)
        setRangedLevel(ranged)
        return this
    }

    fun setDefaultAttackSound(sound: Int) {
        check(defaultAttackSound == -1) { "Default attack sound already set. ${Throwable().stackTrace[2].fileName}" }
        defaultAttackSound = sound
    }

    fun setDefaultBlockSound(sound: Int) {
        check(defaultBlockSound == -1) { "Default block sound already set. ${Throwable().stackTrace[2].fileName}" }
        defaultBlockSound = sound
    }

    fun setDefaultDeathSound(sound: Int) {
        check(defaultDeathSound == -1) { "Default death sound already set. ${Throwable().stackTrace[2].fileName}" }
        defaultDeathSound = sound
    }

    fun setDefaultAttackAnimation(animation: Int): NpcCombatBuilder {
        check(defaultAttackAnim == -1) { "Default attack animation already set. ${Throwable().stackTrace[2].fileName}" }
        defaultAttackAnim = animation
        return this
    }

    fun setDefaultBlockAnimation(animation: Int): NpcCombatBuilder {
        check(defaultBlockAnim == -1) { "Default block animation already set. ${Throwable().stackTrace[2].fileName}" }
        defaultBlockAnim = animation
        return this
    }

    fun setCombatAnimations(
        attackAnimation: Int,
        blockAnimation: Int,
    ): NpcCombatBuilder {
        setDefaultAttackAnimation(attackAnimation)
        setDefaultBlockAnimation(blockAnimation)
        return this
    }

    fun setCombatSounds(
        attackSound: Int,
        blockSound: Int,
    ) {
        setDefaultAttackSound(attackSound)
        setDefaultBlockSound(blockSound)
    }

    fun setDeathAnimation(vararg anims: Int): NpcCombatBuilder {
        check(anims.isNotEmpty()) { "Animations not assigned. Caused by: ${Throwable().stackTrace[2].fileName} " }
        check(deathAnimList.isEmpty()) { "Death animation(s) already set." }
        anims.forEach { deathAnimList.add(it) }
        return this
    }

    fun setRespawnDelay(cycles: Int): NpcCombatBuilder {
        check(respawnDelay == -1) { "Respawn delay already set. ${Throwable().stackTrace[2].fileName}" }
        respawnDelay = cycles
        return this
    }

    fun setAggroRadius(radius: Int): NpcCombatBuilder {
        check(aggroRadius == -1) { "Aggro radius already set. ${Throwable().stackTrace[2].fileName}" }
        aggroRadius = radius
        return this
    }

    fun setFindAggroTargetDelay(delay: Int): NpcCombatBuilder {
        check(aggroTargetDelay == -1) { "Aggro target delay already set. ${Throwable().stackTrace[2].fileName}" }
        aggroTargetDelay = delay
        return this
    }

    fun setAggroTimer(timer: Int): NpcCombatBuilder {
        check(aggroTimer == -1) { "Aggro timer already set. ${Throwable().stackTrace[2].fileName}" }
        aggroTimer = timer
        return this
    }

    /**
     * Initial poison damage - the wiki's `poisonous = Yes (N)`. Without this an npc cannot poison,
     * whatever [setPoisonChance] says.
     */
    fun setPoisonDamage(damage: Int): NpcCombatBuilder {
        check(poisonDamage == -1) { "Poison damage already set. ${Throwable().stackTrace[2].fileName}" }
        poisonDamage = damage
        return this
    }

    fun setPoisonChance(chance: Double): NpcCombatBuilder {
        check(poisonChance == -1.0) { "Poison chance already set. ${Throwable().stackTrace[2].fileName}" }
        poisonChance = chance
        return this
    }

    fun setVenomChance(chance: Double): NpcCombatBuilder {
        check(venomChance == -1.0) { "Venom chance already set. ${Throwable().stackTrace[2].fileName}" }
        venomChance = chance
        return this
    }

    fun setSlayerRequirement(levelReq: Int): NpcCombatBuilder {
        check(slayerReq == -1) { "Slayer requirement already set. ${Throwable().stackTrace[2].fileName}" }
        slayerReq = levelReq
        return this
    }

    fun setSlayerXp(xp: Double): NpcCombatBuilder {
        check(slayerXp == -1.0) { "Slayer xp already set. ${Throwable().stackTrace[2].fileName}" }
        slayerXp = xp
        return this
    }

    fun setSlayerParams(
        levelReq: Int,
        xp: Double,
    ): NpcCombatBuilder {
        setSlayerRequirement(levelReq)
        setSlayerXp(xp)
        return this
    }

    fun setBonus(
        index: Int,
        value: Int,
    ): NpcCombatBuilder {
        check(bonuses[index] == 0) { "Bonus [$index] already set. ${Throwable().stackTrace[2].fileName}" }
        bonuses[index] = value
        return this
    }

    fun setAttackStabBonus(value: Int): NpcCombatBuilder {
        val index = BonusSlot.ATTACK_STAB.id
        check(bonuses[index] == 0) { "Bonus [$index] already set. ${Throwable().stackTrace[2].fileName}" }
        bonuses[index] = value
        return this
    }

    fun setAttackSlashBonus(value: Int): NpcCombatBuilder {
        val index = BonusSlot.ATTACK_SLASH.id
        check(bonuses[index] == 0) { "Bonus [$index] already set. ${Throwable().stackTrace[2].fileName}" }
        bonuses[index] = value
        return this
    }

    fun setAttackCrushBonus(value: Int): NpcCombatBuilder {
        val index = BonusSlot.ATTACK_CRUSH.id
        check(bonuses[index] == 0) { "Bonus [$index] already set. ${Throwable().stackTrace[2].fileName}" }
        bonuses[index] = value
        return this
    }

    fun setAttackMagicBonus(value: Int): NpcCombatBuilder {
        val index = BonusSlot.ATTACK_MAGIC.id
        check(bonuses[index] == 0) { "Bonus [$index] already set. ${Throwable().stackTrace[2].fileName}" }
        bonuses[index] = value
        return this
    }

    fun setAttackRangedBonus(value: Int): NpcCombatBuilder {
        val index = BonusSlot.ATTACK_RANGED.id
        check(bonuses[index] == 0) { "Bonus [$index] already set. ${Throwable().stackTrace[2].fileName}" }
        bonuses[index] = value
        return this
    }

    fun setDefenceStabBonus(value: Int): NpcCombatBuilder {
        val index = BonusSlot.DEFENCE_STAB.id
        check(bonuses[index] == 0) { "Bonus [$index] already set. ${Throwable().stackTrace[2].fileName}" }
        bonuses[index] = value
        return this
    }

    fun setDefenceSlashBonus(value: Int): NpcCombatBuilder {
        val index = BonusSlot.DEFENCE_SLASH.id
        check(bonuses[index] == 0) { "Bonus [$index] already set. ${Throwable().stackTrace[2].fileName}" }
        bonuses[index] = value
        return this
    }

    fun setDefenceCrushBonus(value: Int): NpcCombatBuilder {
        val index = BonusSlot.DEFENCE_CRUSH.id
        check(bonuses[index] == 0) { "Bonus [$index] already set. ${Throwable().stackTrace[2].fileName}" }
        bonuses[index] = value
        return this
    }

    fun setDefenceMagicBonus(value: Int): NpcCombatBuilder {
        val index = BonusSlot.DEFENCE_MAGIC.id
        check(bonuses[index] == 0) { "Bonus [$index] already set. ${Throwable().stackTrace[2].fileName}" }
        bonuses[index] = value
        return this
    }

    fun setDefenceRangedBonus(value: Int): NpcCombatBuilder {
        val index = BonusSlot.DEFENCE_RANGED.id
        check(bonuses[index] == 0) { "Bonus [$index] already set. ${Throwable().stackTrace[2].fileName}" }
        bonuses[index] = value
        return this
    }

    fun addSpecies(species: NpcSpecies): NpcCombatBuilder {
        speciesSet.add(species)
        return this
    }

    fun setSpecies(vararg species: NpcSpecies): NpcCombatBuilder {
        check(speciesSet.isEmpty()) { "Species already set. ${Throwable().stackTrace[2].fileName}" }
        speciesSet.addAll(species)
        return this
    }

    fun setPoisonImmunity(state: Boolean): NpcCombatBuilder {
        check(!immunePoison) { "Poison immunity was already applied. ${Throwable().stackTrace[2].fileName}"}
        immunePoison = state
        return this
    }
    fun setVenomImmunity(state: Boolean): NpcCombatBuilder {
        check(!immuneVenom) { "Venom immunity was already applied. ${Throwable().stackTrace[2].fileName}" +
                "" +
                "" +
                ""}
        immuneVenom = state
        return this
    }
    fun setCannonImmunity(state: Boolean): NpcCombatBuilder {
        check(!immuneCannons) { "Cannon immunity was already applied. ${Throwable().stackTrace[2].fileName}" +
                "" +
                "" +
                ""}
        immuneCannons = state
        return this
    }
    fun setThrallsImmunity(state: Boolean): NpcCombatBuilder {
        check(!immuneThralls) { "Thralls immunity was already applied. ${Throwable().stackTrace[2].fileName}" +
                "" +
                "" +
                ""}
        immuneThralls = state
        return this
    }







    companion object {
        private const val BONUS_COUNT = 14

        /**
         * How long after entering a region an npc stays aggressive, in cycles - ten minutes,
         * the interval the real game uses before monsters lose interest in a player who has
         * stood in their region that long.
         */
        const val DEFAULT_AGGRO_TIMER = 1000

        /**
         * Cycles between aggro sweeps when an `aggro { }` block declares a radius but no
         * `searchDelay`. Four is what every monster package in the game had settled on
         * independently before this was a default.
         */
        const val DEFAULT_AGGRO_SEARCH_DELAY = 4
    }
}
