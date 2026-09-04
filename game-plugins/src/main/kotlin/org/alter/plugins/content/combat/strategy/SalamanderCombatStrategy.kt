package org.alter.plugins.content.combat.strategy

import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.WeaponType
import org.alter.api.ext.*
import org.alter.game.model.entity.AreaSound
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.formula.RangedCombatFormula
import org.alter.rscm.RSCM.getRSCM

/**
 * The five salamanders, and the herb tar each one burns.
 *
 * [magicStrength] is the salamander's hidden magic strength bonus, which drives the
 * Blaze max hit and nothing else. It does not appear in the equipment interface and is
 * absent from the wiki's own bonuses infobox, so these values were read straight out of
 * the cache - item param 65, which [dev.openrune.cache.filestore.definition.data.ParamMapper.item]
 * already named `MAGIC_DAMAGE_BONUS_SALAMANDER` and then never read. They agree with the
 * per-salamander numbers the wiki quotes in prose (black 92).
 *
 * The tars are ordinary ammunition and carry a ranged strength bonus in the usual param
 * (guam 16, marrentill 22, tarromin 31, harralander 49, irit 60), so Flare needs nothing
 * special - equipping the tar feeds that bonus into the ranged formula by itself.
 */
private enum class SalamanderType(
    val itemKey: String,
    val tarKey: String,
    val magicStrength: Int,
) {
    SWAMP_LIZARD("item.swamp_lizard", "item.guam_tar", 56),
    ORANGE("item.orange_salamander", "item.marrentill_tar", 59),
    RED("item.red_salamander", "item.tarromin_tar", 77),
    BLACK("item.black_salamander", "item.harralander_tar", 92),
    TECU("item.tecu_salamander", "item.irit_tar", 104),
    ;

    val item: Int by lazy { getRSCM(itemKey) }
    val tar: Int by lazy { getRSCM(tarKey) }

    companion object {
        fun equipped(player: Player): SalamanderType? {
            val weapon = player.getEquipment(EquipmentType.WEAPON) ?: return null
            return entries.firstOrNull { it.item == weapon.id }
        }
    }
}

/**
 * Salamanders, which are the only weapon in the game whose *combat class* changes with
 * the attack style rather than just the style's bonuses:
 *
 * ```
 * style  option  combat class  speed  accuracy from        max hit from
 * 0      Scorch  Melee         5      slash attack bonus   melee strength bonus
 * 1      Flare   Ranged        4      ranged attack bonus  the tar's ranged strength
 * 2      Blaze   Magic         5      magic attack bonus   the salamander's magic strength
 * ```
 *
 * [CombatConfigs.getCombatStyle] and [CombatConfigs.getXpMode] already knew that, but
 * [CombatConfigs.getCombatClass] did not - it only returned RANGED for BOW, CHINCHOMPA,
 * CROSSBOW and THROWN, so a salamander fought as pure melee whatever style was selected.
 * Blaze and Flare rolled melee accuracy against the target's melee defence, took their
 * max hit from the strength bonus, and paid out Strength experience.
 *
 * The class is now resolved per style, which is what protection prayers, defence rolls
 * and experience all key off. Execution is gathered here rather than left to the three
 * ordinary strategies because two of the three would not survive the trip:
 * [MagicCombatStrategy] dereferences `CASTING_SPELL` unconditionally and a salamander
 * casts no spell, and [RangedCombatStrategy] would look up a projectile and ammo
 * recovery rules for tar, which has neither.
 *
 * **Deliberately not modelled:** salamanders cannot be poisoned, have no special attack,
 * and their tar is always consumed - there is no Ava's-style recovery roll, which is why
 * this does not go through [org.alter.plugins.content.combat.strategy.ranged.RangedAmmo].
 */
object SalamanderCombatStrategy : CombatStrategy {
    /**
     * Salamanders reach one tile, like most melee weapons - the wiki's attack range
     * table gives them a 1 with no Longrange column at all, and the cache agrees
     * (`attackrange: 1`). Their quirk is that, being Ranged weapons, they may also
     * attack diagonally.
     */
    private const val ATTACK_RANGE = 1

    /** Attack-style varp values for the three combat options. */
    private const val SCORCH = 0
    private const val FLARE = 1

    /**
     * The divisor and offset in the Blaze max hit, which is the same shape as every
     * other max hit in the game: `floor(0.5 + level * (bonus + 64) / 640)`.
     */
    private const val BONUS_OFFSET = 64.0
    private const val MAX_HIT_DIVISOR = 640.0

    override fun getAttackRange(pawn: Pawn): Int = ATTACK_RANGE

    override fun canAttack(
        pawn: Pawn,
        target: Pawn,
    ): Boolean {
        val player = pawn as? Player ?: return true
        val salamander = SalamanderType.equipped(player) ?: return true

        if (player.getEquipment(EquipmentType.AMMO)?.id != salamander.tar) {
            player.message("Your salamander needs to be fuelled to attack with it.")
            return false
        }
        return true
    }

    override fun attack(
        pawn: Pawn,
        target: Pawn,
    ) {
        val player = pawn as? Player ?: return
        val world = player.world
        val style = player.getAttackStyle()

        /*
         * Only the five known salamanders are in the table, and the cache says that is
         * all of them - the "claw" items that share the name are not equippable weapons.
         * If a sixth ever appears, fall back to a plain melee swing rather than standing
         * there doing nothing, which is what returning here would look like in-game.
         */
        val salamander =
            SalamanderType.equipped(player) ?: run {
                MeleeCombatStrategy.attack(pawn, target)
                return
            }

        player.animate(CombatConfigs.getAttackAnimation(player))
        world.spawn(AreaSound(player.tile, CombatConfigs.getWeaponAttackSound(player), 5, 1))

        /*
         * One tar per attack, always. Unlike arrows and bolts, tar is never recovered
         * and never drops to the floor.
         */
        player.equipment.remove(salamander.tar, 1)

        val accuracy: Double
        val maxHit: Int
        when (style) {
            SCORCH -> {
                accuracy = MeleeCombatFormula.getAccuracy(player, target)
                maxHit = MeleeCombatFormula.getMaxHit(player, target)
            }
            FLARE -> {
                accuracy = RangedCombatFormula.getAccuracy(player, target)
                maxHit = RangedCombatFormula.getMaxHit(player, target)
            }
            else -> {
                /*
                 * Accuracy is an ordinary magic roll, which for a salamander means it
                 * rolls against a magic attack bonus of zero - they genuinely have none,
                 * in the cache and in the wiki's stat table alike. That is why Blaze is
                 * the least accurate of the three options.
                 */
                accuracy = MagicCombatFormula.getAccuracy(player, target)
                maxHit = blazeMaxHit(player, salamander)
            }
        }

        val landHit = accuracy >= world.randomDouble()
        val damage = player.dealHit(target = target, maxHit = maxHit, landHit = landHit, delay = 1).hit.hitmarks.sumOf { it.damage }

        if (damage > 0) {
            addCombatXp(player, target, style, damage)
        }
    }

    /**
     * The Blaze max hit: `floor(0.5 + Magic * (Bonus + 64) / 640)`, where `Magic` is the
     * player's *visible* (boosted) Magic level and `Bonus` is the salamander's own magic
     * strength - see [SalamanderType.magicStrength].
     *
     * The wiki states this formula only as an image, so it was reconstructed and then
     * checked against the black salamander's published max hit table: all 56 levels from
     * 70 to 125 match exactly, including every one of the 14 band boundaries. Note there
     * is no `+8` here - unlike the other combat formulas this takes the visible level
     * directly, which is what makes the fit exact.
     */
    private fun blazeMaxHit(
        player: Player,
        salamander: SalamanderType,
    ): Int {
        val magic = player.getSkills().getCurrentLevel(Skills.MAGIC)
        return Math.floor(0.5 + magic * (salamander.magicStrength + BONUS_OFFSET) / MAX_HIT_DIVISOR).toInt()
    }

    /**
     * Experience for the style actually used. [CombatConfigs.getXpMode] already maps the
     * three options to Strength/Ranged/Magic, but the rates differ per skill so the
     * split is done here.
     *
     * Scorch and Flare pay the standard 4-per-damage of any Aggressive melee or Accurate
     * ranged attack. Blaze pays the standard 2-per-damage of a magic attack with no
     * spell base experience to add, there being no spell.
     */
    private fun addCombatXp(
        player: Player,
        target: Pawn,
        style: Int,
        damage: Int,
    ) {
        val modDamage = if (target.entityType.isNpc) Math.min(target.getCurrentHp(), damage) else damage
        val multiplier = if (target is Npc) Combat.getNpcXpMultiplier(target) else 1.0

        when (style) {
            SCORCH -> player.addXp(Skills.STRENGTH, modDamage * 4.0 * multiplier)
            FLARE -> player.addXp(Skills.RANGED, modDamage * 4.0 * multiplier)
            else -> player.addXp(Skills.MAGIC, modDamage * 2.0 * multiplier)
        }
        player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
    }

    /** Whether [pawn] is wielding a salamander, and so should use this strategy at all. */
    fun applies(pawn: Pawn): Boolean = pawn is Player && pawn.hasWeaponType(WeaponType.SALAMANDER)
}
