package org.alter.plugins.content.combat.strategy

import org.alter.api.ProjectileType
import org.alter.api.Skills
import org.alter.api.ext.getVarbit
import org.alter.api.ext.landed
import org.alter.api.ext.playSound
import org.alter.game.model.Graphic
import org.alter.game.model.Tile
import org.alter.game.model.combat.XpMode
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.combat.createProjectile
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.plugins.content.combat.strategy.magic.CurseEffect
import org.alter.plugins.content.magic.MagicSpells

/**
 * Plays a spell's cast or impact sound to the humans involved.
 *
 * [org.alter.game.model.entity.Npc]s have no client to send a sound to, so an NPC-cast
 * spell has to be heard through the player on the other end of it - which is why this
 * plays to the caster *and* the target rather than just the caster. Shared with
 * [org.alter.plugins.content.npcs.darkwizard.DarkWizardCombatPlugin], whose casts were
 * silent for the same reason.
 *
 * Ids of `-1` (or 0) mean "no sound defined" and are skipped.
 */
fun playSpellSound(
    caster: Pawn,
    target: Pawn,
    sound: Int,
) {
    if (sound <= 0) {
        return
    }
    (caster as? Player)?.playSound(sound)
    if (target !== caster) {
        (target as? Player)?.playSound(sound)
    }
}

/**
 * @author Tom <rspsmods@gmail.com>
 */
object MagicCombatStrategy : CombatStrategy {
    override fun getAttackRange(pawn: Pawn): Int = 10

    override fun canAttack(
        pawn: Pawn,
        target: Pawn,
    ): Boolean {
        if (pawn is Player) {
            val spell = pawn.attr[Combat.CASTING_SPELL]!!
            val requirements = MagicSpells.getMetadata(spell.id)
            if (requirements != null && !MagicSpells.canCast(pawn, requirements.lvl, requirements.items, requirements.spellbook)) {
                return false
            }
        }
        return true
    }

    override fun attack(
        pawn: Pawn,
        target: Pawn,
    ) {
        val world = pawn.world

        val spell = pawn.attr[Combat.CASTING_SPELL]!!
        val projectile =
            pawn.createProjectile(
                target,
                gfx = spell.projectile,
                type = ProjectileType.MAGIC,
                endHeight = spell.projectilEndHeight,
            )

        pawn.animate(spell.castAnimation)
        spell.castGfx?.let { gfx -> pawn.graphic(gfx) }
        spell.impactGfx?.let { gfx -> target.graphic(Graphic(gfx.id, gfx.height, projectile.lifespan)) }
        if (spell.projectile > 0) {
            world.spawn(projectile)
        }

        // The cast sound used to sit inside the `pawn is Player` block below, so a
        // spell cast by an NPC was completely silent. It is the spell making the
        // noise, not the caster, so it now plays either way.
        playSpellSound(pawn, target, spell.castSound)

        if (pawn is Player) {
            MagicSpells.getMetadata(spell.id)?.let { requirement -> MagicSpells.removeRunes(pawn, requirement.items) }
        }

        val formula = MagicCombatFormula
        val accuracy = formula.getAccuracy(pawn, target)
        val maxHit = formula.getMaxHit(pawn, target)
        val landHit = accuracy >= world.randomDouble()

        val hitDelay = getHitDelay(pawn.getCentreTile(), target.getCentreTile())
        val damage =
            pawn
                .dealHit(target = target, maxHit = maxHit, landHit = landHit, delay = hitDelay) { hit ->
                    // Runs when the hit actually applies, so the impact lands with
                    // the projectile rather than needing a guessed sound delay.
                    if (hit.landed()) {
                        playSpellSound(pawn, target, spell.impactSound)
                    }
                    val curseEffect = spell.curseEffect
                    if (hit.landed() && curseEffect != null) {
                        applyCurseEffect(target, curseEffect)
                    }
                }.hit.hitmarks
                .sumOf { it.damage }

        if (damage >= 0 && pawn.entityType.isPlayer) {
            addCombatXp(pawn as Player, target, damage, spell)
        }
    }

    /** Drains [effect.drainedSkill] by [effect.drainPercent] of its current level (floored, min 1). */
    private fun applyCurseEffect(
        target: Pawn,
        effect: CurseEffect,
    ) {
        when (target) {
            is Player -> {
                val current = target.getSkills().getCurrentLevel(effect.drainedSkill)
                val reduction = (current * effect.drainPercent).toInt().coerceAtLeast(1)
                target.getSkills().alterCurrentLevel(effect.drainedSkill, -reduction)
            }
            is Npc -> {
                val current = target.stats.getCurrentLevel(effect.drainedSkill)
                val reduction = (current * effect.drainPercent).toInt().coerceAtLeast(1)
                target.stats.alterCurrentLevel(effect.drainedSkill, -reduction)
            }
            else -> {}
        }
    }

    fun getHitDelay(
        start: Tile,
        target: Tile,
    ): Int {
        val distance = start.getDistance(target)
        return 2 + Math.floor((1.0 + distance) / 3.0).toInt()
    }

    private fun addCombatXp(
        player: Player,
        target: Pawn,
        damage: Int,
        spell: CombatSpell,
    ) {
        val modDamage = if (target.entityType.isNpc) Math.min(target.getCurrentHp(), damage) else damage
        val mode = CombatConfigs.getXpMode(player)
        val multiplier = if (target is Npc) Combat.getNpcXpMultiplier(target) else 1.0
        val baseXp = spell.baseXp

        if (mode == XpMode.MAGIC) {
            val defensive =
                player.getVarbit(
                    Combat.SELECTED_AUTOCAST_VARBIT,
                ) != 0 && player.getVarbit(Combat.DEFENSIVE_MAGIC_CAST_VARBIT) != 0
            if (!defensive) {
                player.addXp(Skills.MAGIC, (modDamage * 2.0 * multiplier) + baseXp)
                player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
            } else {
                player.addXp(Skills.MAGIC, (modDamage * 1.33 * multiplier) + baseXp)
                player.addXp(Skills.DEFENCE, modDamage * multiplier)
                player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
            }
        } else if (mode == XpMode.SHARED) {
            player.addXp(Skills.MAGIC, (modDamage * 1.33 * multiplier) + baseXp)
            player.addXp(Skills.DEFENCE, modDamage * multiplier)
            player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
        } else {
            player.addXp(Skills.MAGIC, (modDamage * 2.0 * multiplier) + baseXp)
            player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
        }
    }
}
