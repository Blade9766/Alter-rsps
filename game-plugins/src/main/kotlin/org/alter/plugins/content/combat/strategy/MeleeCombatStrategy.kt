package org.alter.plugins.content.combat.strategy

import org.alter.api.Skills
import org.alter.api.WeaponType
import org.alter.api.ext.hasWeaponType
import org.alter.api.ext.playSound
import org.alter.game.model.combat.XpMode
import org.alter.game.model.entity.AreaSound
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.combat.playAttackSound
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.WeaponPassives
import java.lang.IllegalStateException

/**
 * @author Tom <rspsmods@gmail.com>
 */
object MeleeCombatStrategy : CombatStrategy {
    /** Adjacent only, matching an ordinary one-handed weapon. */
    private const val DEFAULT_ATTACK_RANGE = 1

    /** Nothing travels: a swing needs line of *walk*, not line of sight. */
    override val usesProjectile: Boolean = false

    override fun getAttackRange(pawn: Pawn): Int {
        if (pawn is Player) {
            val halberd = pawn.hasWeaponType(WeaponType.HALBERD)
            return if (halberd) 2 else 1
        }
        return Combat.npcAttackRange(pawn, DEFAULT_ATTACK_RANGE)
    }

    override fun canAttack(
        pawn: Pawn,
        target: Pawn,
    ): Boolean {
        return true
    }

    override fun attack(
        pawn: Pawn,
        target: Pawn,
    ) {
        val world = pawn.world
        val animation = CombatConfigs.getAttackAnimation(pawn)
        pawn.animate(animation)
        if (pawn is Npc) {
            pawn.playAttackSound(target)
        } else if (pawn is Player) {
            world.spawn(AreaSound(pawn.tile, CombatConfigs.getWeaponAttackSound(pawn), 5, 1))
        }
        val formula = MeleeCombatFormula
        val accuracy = formula.getAccuracy(pawn, target)
        val maxHit = formula.getMaxHit(pawn, target)
        val landHit = accuracy >= world.randomDouble()

        // Melee has no travel time: the hit lands on the cycle of the swing itself.
        val damage = pawn.dealHit(target = target, maxHit = maxHit, landHit = landHit, delay = 0).hit.hitmarks.sumOf { it.damage }

        if (pawn is Player) {
            /*
             * Weapons that build a resource up over ordinary swings - the soulreaper axe's soul
             * stacks, the sunlight spear's sunlight stacks - earn it here, on a miss as well as on
             * a hit, which is what the real weapons do.
             */
            WeaponPassives.attacked(pawn, target)
        }

        if (damage > 0 && pawn.entityType.isPlayer) {
            addCombatXp(pawn as Player, target, damage)
        }
    }

    /**
     * Public so special attacks can grant the same experience an ordinary swing would.
     *
     * They deal their damage through [org.alter.plugins.content.combat.dealHit] directly rather
     * than through [attack], and so used to award nothing at all - a player specialling a monster
     * down got the kill and none of the Attack, Strength, Defence or Hitpoints experience for it.
     */
    fun addCombatXp(
        player: Player,
        target: Pawn,
        damage: Int,
    ) {
        val modDamage = if (target.entityType.isNpc) Math.min(target.getCurrentHp(), damage) else damage
        val mode = CombatConfigs.getXpMode(player)
        val multiplier = if (target is Npc) Combat.getNpcXpMultiplier(target) else 1.0

        when (mode) {
            XpMode.ATTACK -> {
                player.addXp(Skills.ATTACK, modDamage * 4.0 * multiplier)
                player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
            }
            XpMode.STRENGTH -> {
                player.addXp(Skills.STRENGTH, modDamage * 4.0 * multiplier)
                player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
            }
            XpMode.DEFENCE -> {
                player.addXp(Skills.DEFENCE, modDamage * 4.0 * multiplier)
                player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
            }
            XpMode.SHARED -> {
                player.addXp(Skills.ATTACK, modDamage * 1.33 * multiplier)
                player.addXp(Skills.STRENGTH, modDamage * 1.33 * multiplier)
                player.addXp(Skills.DEFENCE, modDamage * 1.33 * multiplier)
                player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
            }
            else -> throw IllegalStateException("Unknown $mode in MeleeCombatStrategy.")
        }
    }
}
