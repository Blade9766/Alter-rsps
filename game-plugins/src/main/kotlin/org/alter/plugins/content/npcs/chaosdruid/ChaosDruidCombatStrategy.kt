package org.alter.plugins.content.npcs.chaosdruid

import org.alter.api.HitType
import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.FROZEN_TIMER
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.combat.strategy.CombatStrategy
import org.alter.plugins.content.combat.strategy.MagicCombatStrategy
import org.alter.plugins.content.combat.strategy.MeleeCombatStrategy
import org.alter.plugins.content.combat.strategy.playSpellSound
import org.alter.game.model.Graphic as GraphicEntity

/**
 * The chaos druid's attack: fists most swings, and now and then the spell its page describes -
 * "a spell that appears to be a combination of the [Confuse] and [Bind] spells, binding their
 * target very briefly if successful".
 *
 * ## Why a strategy and not an `onNpcCombat` loop
 *
 * Every other npc in this codebase that casts - the dark wizards, the infernal mage, the aberrant
 * spectre - does it through `onNpcCombat`, which hands the monster a bespoke attack loop. That is
 * not usable here. `onNpcCombat` replaces [org.alter.plugins.content.combat.CombatPlugin]'s loop
 * outright, and that loop is the only code in the game that *walks* an npc towards its target:
 * [Combat.moveToAttackRange], in spite of its name, only tests range - the walk call inside it is
 * commented out. A ten-tile caster never notices, because it is already in range of anything it
 * can see. A monster that fights with its fists would notice immediately: it would stand on its
 * spawn tile punching the air while the player walked away.
 *
 * So this is a [CombatStrategy] instead, registered against the chaos druid's npc id in
 * [org.alter.plugins.content.combat.CombatConfigs.setNpcCombatStrategy]. The engine keeps doing
 * the routing, leashing, line-of-sight and attack-speed work; only the swing itself is replaced.
 *
 * ## What the spell is made of
 *
 * The wiki publishes no ids for it, so each piece is sourced rather than picked:
 *
 * - **The cast animation is 710** ([Animation.DRUID_BIND]), which is not a guess: this project's
 *   own observed animation set for npc 520 is exactly `[425, 710, 422, 836]` - block, this, punch,
 *   death - and 710 has no other use in the game.
 * - **The graphics and sounds are the [Bind] spell's own** (177 cast, 178 projectile, 181 impact),
 *   because the binding is the half of the effect a player can see. The Confuse half is in the
 *   stat drain, not on screen.
 * - **The effect is both halves**: the target is frozen for [BIND_SECONDS], and its Attack level
 *   is drained by 5% of its current level - the same [Skills.ATTACK] / 5% Confuse carries as its
 *   `curseEffect` in [org.alter.plugins.content.combat.strategy.magic.CombatSpell].
 * - **It deals no damage.** The page's only max hit is `2 ([[Melee]])`, so the spell has none, and
 *   a landed cast renders as the 0 splat any non-damaging spell does.
 *
 * **Two numbers here are judgement calls rather than sources**, and are the only two: how long the
 * bind holds, and how often the spell is cast instead of a punch. "Binding their target very
 * briefly" is the whole of what the page says about the first - three seconds reads that as
 * shorter than the real Bind spell's five - and it says nothing at all about the second beyond
 * that the druid does both, so one swing in four is the spell. Neither affects how hard a druid
 * hits: the spell deals no damage, so casting more or less often only trades damage for the bind.
 *
 * Accuracy is rolled through [MagicCombatFormula], which has a real npc code path (the druid's
 * magic level of 10 against the target's magic defence) and, unlike `getMaxHit`, does not depend
 * on a selected spell an npc can never have. No `prepareAttack` call is made: it would leave the
 * druid's [org.alter.game.model.combat.CombatStyle] set to MAGIC, and the next punch would then
 * throw straight out of `MeleeCombatFormula.getEquipmentAttackBonus`, which accepts only stab,
 * slash and crush.
 */
internal object ChaosDruidCombatStrategy : CombatStrategy {
    /** Fists. Nothing travels, so range is tested as line of walk. */
    override val usesProjectile: Boolean = false

    override fun getAttackRange(pawn: Pawn): Int = Combat.npcAttackRange(pawn, MELEE_ATTACK_RANGE)

    override fun canAttack(
        pawn: Pawn,
        target: Pawn,
    ): Boolean = true

    override fun attack(
        pawn: Pawn,
        target: Pawn,
    ) {
        if (pawn is Npc && shouldCast(pawn, target)) {
            castBind(pawn, target)
        } else {
            MeleeCombatStrategy.attack(pawn, target)
        }
    }

    /**
     * The spell is a minority of swings, and never one that could not land: re-binding an already
     * frozen target would be a wasted attack, since [freeze] refuses while the timer is running.
     */
    private fun shouldCast(
        pawn: Npc,
        target: Pawn,
    ): Boolean = !target.timers.has(FROZEN_TIMER) && pawn.world.chance(1, CAST_ODDS)

    private fun castBind(
        pawn: Npc,
        target: Pawn,
    ) {
        val world = pawn.world
        val projectile =
            pawn.createProjectile(
                target,
                gfx = Graphic.BINDING_SPELL_PROJECTILE,
                startHeight = 43,
                endHeight = 31,
                delay = 41,
                angle = 15,
                steepness = 127,
            )
        pawn.animate(Animation.DRUID_BIND)
        pawn.graphic(id = Graphic.BINDING_SPELL_CAST, height = 92, delay = 0)
        world.spawn(projectile)
        // An npc has no client of its own, so the cast has to be heard through the player it is
        // aimed at - the same reason the dark wizards' casts were silent. See playSpellSound.
        playSpellSound(pawn, target, Sound.BIND_CAST)

        val hitDelay = MagicCombatStrategy.getHitDelay(pawn.getFrontFacingTile(target), target.getCentreTile())
        val landed = MagicCombatFormula.getAccuracy(pawn, target) >= world.randomDouble()
        val hit = target.hit(damage = 0, type = if (landed) HitType.HIT else HitType.BLOCK, delay = hitDelay)
        if (!landed) {
            return
        }

        target.graphic(GraphicEntity(Graphic.BIND_HIT, 124, projectile.impactDelay))

        /*
         * Both halves of the effect land *with* the hit, not when the spell leaves the druid's
         * hands - the same place [MagicCombatStrategy] applies a spell's `curseEffect`. Applying
         * them here directly would freeze and drain the target two cycles early (the hit delay at
         * melee range), and would still do it if the target died in between from something else.
         */
        hit.addAction {
            playSpellSound(pawn, target, Sound.BIND_IMPACT)
            target.freeze(BIND_SECONDS.secondsToTicks())
            if (target is Player) {
                val current = target.getSkills().getCurrentLevel(Skills.ATTACK)
                val reduction = (current * CONFUSE_DRAIN_PERCENT).toInt().coerceAtLeast(1)
                target.getSkills().alterCurrentLevel(Skills.ATTACK, -reduction)
            }
        }
    }

    /** Adjacent only - the druid punches. */
    private const val MELEE_ATTACK_RANGE = 1

    /** 1-in-N swings are the spell instead - judged, not published. See the note above. */
    private const val CAST_ODDS = 4

    /** "Binding their target very briefly" - judged, not published. See the note above. */
    private const val BIND_SECONDS = 3

    /** Confuse's own drain: 5% of the target's current Attack level, floored, minimum 1. */
    private const val CONFUSE_DRAIN_PERCENT = 0.05
}
