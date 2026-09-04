package org.alter.api.dsl

import org.alter.api.*
import org.alter.api.ext.NPC_ATTACK_BONUS_INDEX
import org.alter.api.ext.NPC_MAGIC_DAMAGE_BONUS_INDEX
import org.alter.api.ext.NPC_RANGED_STRENGTH_BONUS_INDEX
import org.alter.api.ext.NPC_STRENGTH_BONUS_INDEX
import org.alter.game.model.combat.CombatStyle
import org.alter.api.ext.enumSetOf
import org.alter.game.plugin.KotlinPlugin

fun KotlinPlugin.setCombatDef(
    npc: String,
    init: NpcCombatDsl.Builder.() -> Unit,
) {

    val builder = NpcCombatDsl.Builder()
    init(builder)

    setCombatDef(npc, builder.build())
}

fun KotlinPlugin.setCombatDef(
    vararg npc: String,
    init: NpcCombatDsl.Builder.() -> Unit,
) {
    npc.forEach { setCombatDef(it, init) }
}

object NpcCombatDsl {
    @DslMarker
    annotation class CombatDslMarker

    @CombatDslMarker
    class Builder {
        private val combatBuilder = NpcCombatBuilder()

        fun build() = combatBuilder.build()

        fun configs(init: ConfigBuilder.() -> Unit) {
            val builder = ConfigBuilder()
            init(builder)

            combatBuilder.setAttackSpeed(builder.attackSpeed)
            builder.combatStyle?.let { combatBuilder.setCombatStyle(it) }
            combatBuilder.setAttackRange(builder.attackRange)
            combatBuilder.setRespawnDelay(builder.respawnDelay)
            combatBuilder.setPoisonChance(builder.poisonChance)
            combatBuilder.setPoisonDamage(builder.poisonDamage)
            combatBuilder.setVenomChance(builder.venomChance)
        }

        /**
         * @TODO Add multiple element weakness support
         */
        @CombatDslMarker
        class defenceMagicBuilder {
            var magic: Int = 0
            var elementWeakness: ElementalWeakness? = null

            fun build(): MagicDefence {
                return MagicDefence(magic, elementWeakness)
            }
        }
        @CombatDslMarker
        class defenceMeleeBuilder {
            var stab: Int = 0
            var slash: Int = 0
            var crush: Int = 0

            fun build(): MeleeDefence {
                return MeleeDefence(stab, slash, crush)
            }
        }

        @CombatDslMarker
        class defenceRangeBuilder {
            var darts = 0
            var arrows = 0
            var bolts = 0
            fun build(): RangeDefence {
                return RangeDefence(darts, arrows, bolts)
            }
        }
        class DefenceBuilder {
            var meleeDefence: MeleeDefence? = null
            var rangeDefence: RangeDefence? = null
            var magicDefence: MagicDefence? = null
            fun melee(init: defenceMeleeBuilder.() -> Unit) {
                val builder = defenceMeleeBuilder()
                builder.init()
                meleeDefence = builder.build()
            }

            fun range(init: defenceRangeBuilder.() -> Unit) {
                val builder = defenceRangeBuilder()
                builder.init()
                rangeDefence = builder.build()
            }

            fun magic(init: defenceMagicBuilder.() -> Unit) {
                val builder = defenceMagicBuilder()
                builder.init()
                magicDefence = builder.build()
            }
        }
        /**
         * Only the magic sub-block's [ElementalWeakness] is actually wired to
         * [combatBuilder] - [DefenceBuilder.meleeDefence]/[DefenceBuilder.rangeDefence]
         * (and a flat magic defence level, which [bonuses] already covers via
         * [BonusBuilder.defenceMagic]) are built but never consumed by anything, same
         * as this whole block was before this fix (it built a [DefenceBuilder] and
         * discarded it without ever touching [combatBuilder]).
         */
        fun defence(init: DefenceBuilder.() -> Unit) {
            val builder = DefenceBuilder()
            builder.init()
            builder.magicDefence?.elementsWeakness?.let { weakness ->
                combatBuilder.setElementalWeakness(weakness.element, weakness.percent)
            }
        }

        @CombatDslMarker
        class ImmunitiesBuilder {
            var poison = false
            var venom = false
            var cannon = false
            var thralls = false
        }
        fun immunities(init: ImmunitiesBuilder.() -> Unit) {
            val builder = ImmunitiesBuilder()
            init(builder)
            combatBuilder.setPoisonImmunity(builder.poison)
            combatBuilder.setVenomImmunity(builder.venom)
            combatBuilder.setCannonImmunity(builder.cannon)
            combatBuilder.setThrallsImmunity(builder.thralls)
        }

        /**
         * Declares this npc as a ranged attacker and describes the projectile it fires.
         *
         * Without this block an npc is a melee attacker, which is what every npc in the
         * codebase used to be: [org.alter.game.model.entity.Npc.combatClass] defaulted
         * to MELEE and nothing ever set it, so the only way to build a monster that
         * shot at you was a bespoke per-monster attack loop.
         *
         * ```
         * ranged {
         *     projectile = 10          // bronze arrow
         *     drawback = 19
         *     type = ProjectileType.ARROW
         * }
         * ```
         */
        @CombatDslMarker
        class RangedBuilder {
            /** Spotanim of the projectile in flight. Required. */
            var projectile = -1

            /** Flight profile - heights, delay and angle. */
            var type = ProjectileType.ARROW

            /** Spotanim played on the npc as it fires. -1 for none. */
            var drawback = -1
            var drawbackHeight = 96

            /** Spotanim played on the target as the projectile lands. -1 for none. */
            var impact = -1
            var impactHeight = 0
        }

        fun ranged(init: RangedBuilder.() -> Unit) {
            val builder = RangedBuilder()
            init(builder)

            check(builder.projectile != -1) { "A ranged npc must set a projectile spotanim." }
            combatBuilder.setRangedProjectile(
                gfx = builder.projectile,
                type = builder.type,
                drawbackGfx = builder.drawback,
                drawbackHeight = builder.drawbackHeight,
                impactGfx = builder.impact,
                impactHeight = builder.impactHeight,
            )
        }

        fun aggro(init: AggressivenessBuilder.() -> Unit) {
            val builder = AggressivenessBuilder()
            init(builder)

            combatBuilder.setAggroRadius(builder.radius)
            combatBuilder.setFindAggroTargetDelay(builder.searchDelay)
            combatBuilder.setAggroTimer(builder.aggroTimer)
        }

        fun stats(init: StatsBuilder.() -> Unit) {
            val stats = mutableListOf<Pair<Int, Int>>()
            val builder = StatsBuilder(stats)
            init(builder)

            combatBuilder.setHitpoints(builder.hitpoints)
            combatBuilder.setAttackLevel(builder.attack)
            combatBuilder.setDefenceLevel(builder.defence)
            combatBuilder.setStrengthLevel(builder.strength)
            combatBuilder.setMagicLevel(builder.magic)
            combatBuilder.setRangedLevel(builder.ranged)

        }

        fun bonuses(init: BonusBuilder.() -> Unit) {
            val bonuses = mutableListOf<Pair<Int, Int>>()
            val builder = BonusBuilder(bonuses)
            init(builder)

            bonuses.forEach { bonus ->
                combatBuilder.setBonus(bonus.first, bonus.second)
            }
        }

        fun species(init: SpeciesBuilder.() -> Unit) {
            val species = enumSetOf<NpcSpecies>()
            val builder = SpeciesBuilder(species)
            init(builder)

            combatBuilder.setSpecies(*species.toTypedArray())
        }

        fun anims(init: AnimationBuilder.() -> Unit) {
            val builder = AnimationBuilder()
            init(builder)

            combatBuilder.setDefaultAttackAnimation(builder.attack)
            combatBuilder.setDefaultBlockAnimation(builder.block)
            combatBuilder.setDeathAnimation(*builder.getDeathList().toIntArray())
        }

        fun sound(init: SoundBuilder.() -> Unit) {
            val builder = SoundBuilder()
            init(builder)

            combatBuilder.setDefaultAttackSound(builder.attackSound)
            combatBuilder.setAttackSoundArea(builder.attackArea)
            combatBuilder.setAttackSoundRadius(builder.attackRadius)
            combatBuilder.setAttackSoundVolume(builder.attackVolume)

            combatBuilder.setDefaultBlockSound(builder.blockSound)
            combatBuilder.setBlockSoundArea(builder.blockArea)
            combatBuilder.setBlockSoundRadius(builder.blockRadius)
            combatBuilder.setBlockSoundVolume(builder.blockVolume)

            combatBuilder.setDefaultDeathSound(builder.deathSound)
            combatBuilder.setDeathSoundArea(builder.deathArea)
            combatBuilder.setDeathSoundRadius(builder.deathRadius)
            combatBuilder.setDeathSoundVolume(builder.deathVolume)
        }

        fun slayerData(init: SlayerBuilder.() -> Unit) {
            val builder = SlayerBuilder()
            init(builder)
            /**
             * @TODO Forgot if it's true or not but => Theres some monsters that can only be attacked on task.
             * Addition: Yh, so there are mobs that can only be attacked during Slayer task / If you have paid your way in,
             *          ^ Add a way to block out attacking if code block returns false.
             *          So that we could reuse it for other shit and slayer.
             */
            combatBuilder.setSlayerParams(builder.levelRequirement, builder.xp)
        }

        fun drops(init: WeightedTableBuilder.() -> Unit) {
            val builder = WeightedTableBuilder()
            builder.combatBuilder = combatBuilder
            init(builder)
        }
    }

    @CombatDslMarker
    class ConfigBuilder {
        /**
         * The speed at which the npc can attack, in cycles.
         */
        var attackSpeed = -1

        /**
         * Which melee style this npc attacks with - the one whose defence bonus the player's
         * armour is read from when it swings. Leave null for STAB, which is what every npc
         * used before this could be declared.
         */
        var combatStyle: CombatStyle? = null

        /**
         * How far away, in tiles, the npc can attack from. Leave at -1 to use the
         * default for its combat class - 1 for melee, 7 for ranged, 10 for magic.
         *
         * Distance is measured between the closest edges of the two footprints, so a
         * large npc's own size is already accounted for: a 3x3 dragon with
         * `attackRange = 1` reaches anything standing next to any of its nine tiles.
         */
        var attackRange = -1

        /**
         * The delay to wait to respawn the npc after death, in cycles.
         * If npc should not respawn, this value should be set to 0.
         */
        var respawnDelay: Int = -1

        /**
         * Initial poison damage this npc inflicts - the wiki infobox's `poisonous = Yes (N)`.
         *
         * This is the field that turns poison on. Setting only [poisonChance] is rejected at build
         * time, because a chance with no damage poisons nobody.
         */
        var poisonDamage = -1

        /**
         * The chance of inflicting poison, as a percentage from 0 to 100.
         *
         * Optional: an npc with [poisonDamage] and no chance takes
         * [org.alter.game.model.combat.NpcCombatDef.DEFAULT_POISON_CHANCE], since the wiki
         * publishes a rate for no monster at all. Set it only where a monster is known to differ.
         */
        var poisonChance = -1.0

        /**
         * The chance of inflicting venom on damage. Value should vary from
         * 0 to 100 where 0 means the npc will never inflict venom and 100
         * meaning the npc will always inflict venom on damage.
         */
        var venomChance = -1.0

        /**
         * @TODO
         * Not added yet, need more info.
         * Cannons => If it's immune to cannonBalls or the cannonball ignores these npcs.
         * Thralls => If it's immune to thralls damage or thralls don't attack.
         */
    }

    @CombatDslMarker
    class AggressivenessBuilder {
        /**
         * The radius, in tiles, in which the npc can target a player.
         */
        var radius = -1

        /**
         * The delay, in cycles, in which the npc can search for possible
         * targets.
         *
         * Left unset this takes [NpcCombatBuilder.DEFAULT_AGGRO_SEARCH_DELAY]. It must end up
         * `> 0`: [org.alter.plugins.content.mechanics.aggro.NpcAggroPlugin] only installs the
         * aggro check and schedules the sweep timer for an npc whose delay is positive.
         */
        var searchDelay = -1

        /**
         * The time, in cycles, in which the npc will be aggressive to
         * nearby targets.
         *
         * Left unset this takes [NpcCombatBuilder.DEFAULT_AGGRO_TIMER]. It must not be left
         * negative: the default aggro check gives up once
         * `abs(currentCycle - lastMapBuildTime) > aggressiveTimer`, and no absolute value is
         * ever `<= -1`, so a negative timer makes the npc silently passive. Use [neverAggro]
         * to say that deliberately.
         */
        var aggroTimer = -1

        /**
         * The time, in minutes, in which the npc will be aggressive to
         * nearby targets. This property is simply an alias for [aggroTimer]
         * and will set [aggroTimer] accordingly.
         *
         * The conversion used to be the expression `aggroTimer * 1000`, which computes a value
         * and discards it - so this set [aggroTimer] to nothing at all and any npc configured
         * in minutes was left on the unset timer. A cycle is 600ms, so a minute is 100 of them.
         */
        var aggroMinutes: Int = -1
            set(value) {
                field = value
                aggroTimer = value * CYCLES_PER_MINUTE
            }

        fun alwaysAggro() {
            aggroTimer = Int.MAX_VALUE
        }

        fun neverAggro() {
            aggroTimer = Int.MIN_VALUE
        }

        companion object {
            /** Game cycles in one minute, at the 600ms cycle. */
            private const val CYCLES_PER_MINUTE = 100
        }
    }

    @CombatDslMarker
    class StatsBuilder(private val stats: MutableList<Pair<Int, Int>>) {
        /*
         * Every setter here has to assign `field` as well as record the pair.
         *
         * [Builder.stats] reads these five back as *properties* - `combatBuilder.setAttackLevel(
         * builder.attack)` - and never looks at the [stats] list at all, so a setter that only
         * appended to the list left the backing field at its `= 1` initializer. The effect was
         * that every combat level in every `stats { }` block in the game was silently discarded:
         * a guard declaring `attack = 21` and a level 13 goblin declaring `attack = 12` both
         * built a def with attack 1, and `World.setNpcStats` then copied that 1 onto the live
         * npc for every combat formula to read. Only `hitpoints`, which has no custom setter,
         * ever worked.
         *
         * The list is still populated because its duplicate check - "Stat [n] already set" - is
         * what catches a block that declares the same stat twice.
         */
        var hitpoints = 1

        var attack: Int = 1
            set(value) {
                field = value
                set(Pair(NpcSkills.ATTACK, value))
            }

        var strength: Int = 1
            set(value) {
                field = value
                set(Pair(NpcSkills.STRENGTH, value))
            }

        var defence: Int = 1
            set(value) {
                field = value
                set(Pair(NpcSkills.DEFENCE, value))
            }

        var magic: Int = 1
            set(value) {
                field = value
                set(Pair(NpcSkills.MAGIC, value))
            }

        var ranged: Int = 1
            set(value) {
                field = value
                set(Pair(NpcSkills.RANGED, value))
            }

        infix fun set(stat: Pair<Int, Int>) {
            check(stats.none { it.first == stat.first }) { "Stat [${stat.first}] already set." }
            stats.add(stat)
        }

        operator fun Pair<Int, Int>.unaryPlus() = set(this)
    }

    @CombatDslMarker
    class BonusBuilder(private val bonuses: MutableList<Pair<Int, Int>>) {
        var attackStab: Int = 0
            set(value) {
                set(Pair(BonusSlot.ATTACK_STAB.id, value))
            }

        var attackSlash: Int = 0
            set(value) {
                set(Pair(BonusSlot.ATTACK_SLASH.id, value))
            }

        var attackCrush: Int = 0
            set(value) {
                set(Pair(BonusSlot.ATTACK_CRUSH.id, value))
            }

        var attackMagic: Int = 0
            set(value) {
                set(Pair(BonusSlot.ATTACK_MAGIC.id, value))
            }

        var attackRanged: Int = 0
            set(value) {
                set(Pair(BonusSlot.ATTACK_RANGED.id, value))
            }

        var defenceStab: Int = 0
            set(value) {
                set(Pair(BonusSlot.DEFENCE_STAB.id, value))
            }

        var defenceSlash: Int = 0
            set(value) {
                set(Pair(BonusSlot.DEFENCE_SLASH.id, value))
            }

        var defenceCrush: Int = 0
            set(value) {
                set(Pair(BonusSlot.DEFENCE_CRUSH.id, value))
            }

        var defenceMagic: Int = 0
            set(value) {
                set(Pair(BonusSlot.DEFENCE_MAGIC.id, value))
            }

        var defenceRanged: Int = 0
            set(value) {
                set(Pair(BonusSlot.DEFENCE_RANGED.id, value))
            }

        var attackBonus: Int = 0
            set(value) {
                set(Pair(NPC_ATTACK_BONUS_INDEX, value))
            }

        var strengthBonus: Int = 0
            set(value) {
                set(Pair(NPC_STRENGTH_BONUS_INDEX, value))
            }

        var rangedStrengthBonus: Int = 0
            set(value) {
                set(Pair(NPC_RANGED_STRENGTH_BONUS_INDEX, value))
            }

        var magicDamageBonus: Int = 0
            set(value) {
                set(Pair(NPC_MAGIC_DAMAGE_BONUS_INDEX, value))
            }

        infix fun set(bonus: Pair<Int, Int>) {
            check(bonuses.none { it.first == bonus.first }) { "Bonus [${bonus.first}] already set." }
            bonuses.add(bonus)
        }

        operator fun Pair<Int, Int>.unaryPlus() = set(this)
    }

    @CombatDslMarker
    class SpeciesBuilder(private val species: MutableSet<NpcSpecies>) {
        infix fun of(species: NpcSpecies) {
            this.species.add(species)
        }

        operator fun NpcSpecies.unaryPlus() = of(this)
    }

    @CombatDslMarker
    class SlayerBuilder {
        /**
         * The Slayer level requirement needed to kill the npc.
         */
        var levelRequirement = 1

        /**
         * The Slayer xp gained from killing the npc.
         */
        var xp = 0.0
    }

    @CombatDslMarker
    class AnimationBuilder {
        var attack = -1
        var block = -1
        private val deathList = mutableListOf<Int>()

        var death: Int = 0
            set(value) {
                check(deathList.isEmpty()) { "Death animation already set. Use `death { }` to set multiple animations instead." }
                deathList.add(value)
            }

        fun death(init: DeathBuilder.() -> Unit) {
            check(deathList.isEmpty()) { "Death animations already set." }

            val builder = DeathBuilder(deathList)
            init(builder)
        }

        fun getDeathList(): List<Int> = deathList

        @CombatDslMarker
        class DeathBuilder(private val anims: MutableList<Int>) {
            infix fun add(anim: Int) {
                anims.add(anim)
            }
        }
    }

    @CombatDslMarker
    class SoundBuilder {
        var attackSound = -1
        var attackArea: Boolean = false
        var attackVolume: Int = 50
        var attackRadius: Int = 0
            set(value) {
                check(attackArea) { "Can't assign attackRadius when attackArea is false." }
                field = value
            }

        var blockSound = -1
        var blockArea: Boolean = false
        var blockVolume: Int = 50
        var blockRadius: Int = 0
            set(value) {
                check(blockArea) { "Can't assign blockRadius when blockArea is false." }
                field = value
            }

        var deathSound = -1
        var deathArea: Boolean = false
        var deathVolume: Int = 50

        var deathRadius: Int = 0
            set(value) {
                check(deathArea) { "Can't assign deathRadius when deathArea is false." }
                field = value
            }
    }
}
