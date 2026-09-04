package org.alter.plugins.content.npcs.elderchaosdruid

import org.alter.api.HitType
import org.alter.api.ext.*
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.combat.strategy.CombatStrategy
import org.alter.plugins.content.combat.strategy.MagicCombatStrategy
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.plugins.content.combat.strategy.playSpellSound
import org.alter.game.model.Graphic as GraphicEntity

/**
 * The Elder Chaos druid's attack: Wind Wave, every swing.
 *
 * ## Which spell, and why it is not a guess
 *
 * The page publishes only `attack style = [[Magic]]` and `max hit = 17`. Two independent facts pin
 * it down to one spell in [CombatSpell]:
 *
 * - The druid's own observed action animation is **727**, which is `Animation.MAGIC_WAVE_CAST` and is
 *   the `castAnimation` of all four wave spells and of nothing else.
 * - **`WIND_WAVE.maxHit` is 17** - exactly the published figure. Water Wave is 18, Earth Wave 19 and
 *   Fire Wave 20, so within the family the max hit picks the element on its own.
 *
 * So the projectile, the cast and impact graphics, the sounds and the damage all come from the real
 * spell rather than from a choice made here.
 *
 * ## Why a strategy and not an `onNpcCombat` loop
 *
 * The argument `content/npcs/chaosdruid` sets out: `onNpcCombat` replaces
 * [org.alter.plugins.content.combat.CombatPlugin]'s loop wholesale, and that loop is the only code in
 * the game that walks an npc toward its target. A caster standing off at ten tiles rarely notices, but
 * it also loses leashing, line of sight and retaliation, all of which this keeps.
 *
 * ## Why there is no `prepareAttack`
 *
 * Same reason again: `prepareAttack` leaves the npc's `CombatStyle` set to MAGIC, and
 * `MeleeCombatFormula` accepts only stab, slash and crush. This druid never punches, so it would not
 * bite today - but the pattern is the one the tree already uses, and accuracy is rolled through
 * [MagicCombatFormula] directly, which has a real npc code path reading the druid's magic level of
 * 110 against the target's magic defence.
 */
internal object ElderChaosDruidCombatStrategy : CombatStrategy {
    /** The wave travels, so range is tested as line of sight rather than line of walk. */
    override val usesProjectile: Boolean = true

    override fun getAttackRange(pawn: Pawn): Int = Combat.npcAttackRange(pawn, SPELL_RANGE)

    override fun canAttack(
        pawn: Pawn,
        target: Pawn,
    ): Boolean = true

    override fun attack(
        pawn: Pawn,
        target: Pawn,
    ) {
        if (pawn !is Npc) {
            return
        }
        val world = pawn.world
        val spell = CombatSpell.WIND_WAVE

        val projectile =
            pawn.createProjectile(
                target,
                gfx = spell.projectile,
                startHeight = 43,
                endHeight = spell.projectilEndHeight.takeIf { it >= 0 } ?: 31,
                delay = 41,
                angle = 15,
                steepness = 127,
            )
        pawn.animate(spell.castAnimation)
        spell.castGfx?.let { pawn.graphic(it.id, it.height) }
        world.spawn(projectile)
        // An npc has no client of its own, so the cast has to be heard through the player it is
        // aimed at - the same reason the dark wizards' casts were silent. See playSpellSound.
        playSpellSound(pawn, target, spell.castSound)

        val hitDelay = MagicCombatStrategy.getHitDelay(pawn.getFrontFacingTile(target), target.getCentreTile())
        if (MagicCombatFormula.getAccuracy(pawn, target) >= world.randomDouble()) {
            target
                .hit(damage = world.random(spell.maxHit), type = HitType.HIT, delay = hitDelay)
                .addAction { playSpellSound(pawn, target, spell.impactSound) }
            spell.impactGfx?.let { target.graphic(GraphicEntity(it.id, it.height, projectile.impactDelay)) }
        } else {
            target.hit(damage = 0, type = HitType.BLOCK, delay = hitDelay)
        }
    }

    /** The engine's own magic default. The page publishes no attack range for this monster. */
    private const val SPELL_RANGE = 10
}
