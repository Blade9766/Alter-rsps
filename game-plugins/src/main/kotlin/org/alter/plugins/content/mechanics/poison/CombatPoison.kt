package org.alter.plugins.content.mechanics.poison

import dev.openrune.cache.CacheManager
import org.alter.api.EquipmentType
import org.alter.api.ext.getEquipment
import org.alter.api.ext.message
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo
import org.alter.rscm.RSCM.getRSCM

/**
 * Poison inflicted by whoever is attacking: a player's `(p)` / `(p+)` / `(p++)` weapons and ammo,
 * the abyssal tentacle, the four Ancient smoke spells, and an npc's own
 * [org.alter.game.model.combat.NpcCombatDef.poisonDamage].
 *
 * Every one of those existed as an item, a spell or a def field but poisoned nothing - the only
 * two things in the whole codebase that ever called
 * [org.alter.plugins.content.mechanics.poison.poison] were emerald bolts (e) and the King Black
 * Dragon's poison breath, so a rune dagger(p++) was a plain rune dagger and a cave crawler was a
 * harmless one.
 *
 * Hooked from [org.alter.plugins.content.combat.dealExactHit], which every melee, ranged and magic
 * attack funnels through - ordinary attacks, special attacks and the multi-projectile specials
 * alike. That is also why the roll is per *hit* rather than per attack: a dragon dagger special is
 * two chances to poison, which is what OSRS does.
 *
 * Figures are the wiki's (https://oldschool.runescape.wiki/w/Poison). The wiki states poison as a
 * "severity" that ticks down by one per poison cycle; this codebase's [Poison] works in initial
 * damage instead and derives the same decay from it, so the severities are converted here
 * (severity 20 -> 4 damage, and so on).
 */
object CombatPoison {
    /**
     * How strong a weapon's poison coating is, and the initial damage it inflicts.
     *
     * A ranged application is 14 severity weaker than the same coating used in melee - the wiki's
     * 4/2, 5/3, 6/4 column - which is why each strength carries both.
     */
    enum class Strength(
        /** What the cache item name ends with. */
        val suffix: String,
        val meleeDamage: Int,
        val rangedDamage: Int,
    ) {
        REGULAR(suffix = "(p)", meleeDamage = 4, rangedDamage = 2),
        SUPER(suffix = "(p+)", meleeDamage = 5, rangedDamage = 3),
        EXTRA(suffix = "(p++)", meleeDamage = 6, rangedDamage = 4),
        ;

        companion object {
            val values = enumValues<Strength>()
        }
    }

    /**
     * A resolved poison application: [damage] to start at, on a [chance] percent roll.
     *
     * [appliesOnMiss] is the one place player and npc poison genuinely differ. The wiki: *"Monsters
     * that can inflict poison may do so regardless of whether or not they inflict any damage, even
     * if protection prayers are used"*, while *"player inflicted poison does need to deal damage to
     * apply"*. So an npc rolls on every attack it throws and a player only on one that lands.
     */
    data class Source(
        val damage: Int,
        val chance: Double,
        val appliesOnMiss: Boolean,
    )

    private const val MELEE_CHANCE = 25.0
    private const val RANGED_CHANCE = 12.5
    private const val SMOKE_SPELL_CHANCE = 12.5

    /** The tentacle's own poison: a quarter chance starting at 4, whatever it is wielded with. */
    private const val TENTACLE_DAMAGE = 4

    private val TENTACLES: Set<Int> by lazy {
        setOf(getRSCM("item.abyssal_tentacle"), getRSCM("item.abyssal_tentacle_or"))
    }

    /**
     * Every poisoned weapon and ammo id in the cache, read from the item names rather than listed
     * by hand.
     *
     * There are 214 of them across ten weapon families and three coating strengths, and new ones
     * arrive with each cache - a hand-written table would be wrong the moment it was written. The
     * equipment-slot filter is what keeps the two non-weapons whose names also end that way out of
     * it: `Camel mould (p)` and `Dynamite(p)`.
     *
     * Built lazily rather than at class-init so it is read after the cache is loaded.
     */
    private val COATED: Map<Int, Strength> by lazy {
        val coated = HashMap<Int, Strength>()
        for ((id, def) in CacheManager.getItems()) {
            if (def.equipSlot != EquipmentType.WEAPON.id && def.equipSlot != EquipmentType.AMMO.id) {
                continue
            }
            // Notes and bank placeholders share the real item's name and cannot be equipped.
            if (def.noteTemplateId != -1 || def.placeholderTemplate != -1) {
                continue
            }
            val name = def.name?.trim() ?: continue
            val strength = Strength.values.firstOrNull { name.endsWith(it.suffix) } ?: continue
            coated[id] = strength
        }
        coated
    }

    /**
     * The coating on [itemId], or `null` if it carries none.
     *
     * Public so [org.alter.plugins.diag.PoisonVerify] can assert the derived table without having
     * to build a player.
     */
    fun strengthOf(itemId: Int): Strength? = COATED[itemId]

    /** Whether [itemId] is an abyssal tentacle, which poisons without being coated. */
    fun isTentacle(itemId: Int): Boolean = itemId in TENTACLES

    /**
     * The poison [pawn]'s current attack can inflict, or `null` if it cannot inflict any.
     *
     * Read when the hit is *scheduled* rather than when it lands, so it sees the weapon and spell
     * that actually threw the attack rather than whatever is equipped a few ticks later.
     */
    fun sourceFor(pawn: Pawn): Source? =
        when (pawn) {
            is Npc -> npcSource(pawn)
            is Player ->
                when (CombatConfigs.getCombatClass(pawn)) {
                    CombatClass.MELEE -> meleeSource(pawn)
                    CombatClass.RANGED -> rangedSource(pawn)
                    CombatClass.MAGIC -> magicSource(pawn)
                }
            else -> null
        }

    /**
     * An npc's innate poison, straight off its combat def.
     *
     * Both halves are required, and [org.alter.api.NpcCombatBuilder] enforces that at build time -
     * a `poisonChance` with no `poisonDamage` used to be the normal state of this field and
     * poisoned nobody.
     */
    private fun npcSource(npc: Npc): Source? {
        val def = npc.combatDef
        if (def.poisonDamage <= 0 || def.poisonChance <= 0.0) {
            return null
        }
        return Source(damage = def.poisonDamage, chance = def.poisonChance, appliesOnMiss = true)
    }

    private fun meleeSource(player: Player): Source? {
        val weapon = player.getEquipment(EquipmentType.WEAPON) ?: return null
        if (isTentacle(weapon.id)) {
            return Source(damage = TENTACLE_DAMAGE, chance = MELEE_CHANCE, appliesOnMiss = false)
        }
        val strength = strengthOf(weapon.id) ?: return null
        return Source(damage = strength.meleeDamage, chance = MELEE_CHANCE, appliesOnMiss = false)
    }

    /**
     * Ranged poison comes from whatever was fired - the quiver for a bow or crossbow, the weapon
     * slot for darts and knives, which [RangedAmmo.ammoSlot] already settles.
     *
     * Caveat shared with [org.alter.plugins.content.combat.strategy.ranged.ammo.EnchantedBolt]:
     * the ammo is consumed before the hit is scheduled, so firing the *last* piece of a stack reads
     * an empty slot and inflicts nothing. One shot in a stack.
     */
    private fun rangedSource(player: Player): Source? {
        val ammo = player.getEquipment(RangedAmmo.ammoSlot(player)) ?: return null
        val strength = strengthOf(ammo.id) ?: return null
        return Source(damage = strength.rangedDamage, chance = RANGED_CHANCE, appliesOnMiss = false)
    }

    /**
     * Only the smoke spells poison, and they carry their own damage - see
     * [org.alter.plugins.content.combat.strategy.magic.CombatSpell.poisonDamage].
     */
    private fun magicSource(player: Player): Source? {
        val spell = player.attr[Combat.CASTING_SPELL] ?: return null
        if (spell.poisonDamage <= 0) {
            return null
        }
        return Source(damage = spell.poisonDamage, chance = SMOKE_SPELL_CHANCE, appliesOnMiss = false)
    }

    /**
     * Rolls [source] against [target] as the hit resolves. [landed] is whether the attack actually
     * dealt its damage - see [Source.appliesOnMiss].
     *
     * Immunity - a serpentine helm, a poison-immune npc, or an antipoison still running - is
     * [Poison.isImmune]'s call, made inside [org.alter.plugins.content.mechanics.poison.poison].
     */
    fun apply(
        attacker: Pawn,
        target: Pawn,
        source: Source,
        landed: Boolean,
    ) {
        if (!landed && !source.appliesOnMiss) {
            return
        }
        if (target.isDead()) {
            return
        }
        if (!attacker.world.percentChance(source.chance)) {
            return
        }
        target.poison(source.damage) {
            if (target is Player) {
                target.message("You have been poisoned.")
            }
        }
    }
}
