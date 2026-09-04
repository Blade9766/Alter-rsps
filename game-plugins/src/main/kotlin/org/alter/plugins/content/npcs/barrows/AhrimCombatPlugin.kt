package org.alter.plugins.content.npcs.barrows

import org.alter.api.HitType
import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.*
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.combat.strategy.MagicCombatStrategy
import org.alter.plugins.content.combat.strategy.playSpellSound

/**
 * Ahrim the Blighted's magic attack.
 *
 * He needs a custom loop for the same reason the Dark Wizards do: the engine's default
 * combat reads `Npc.combatClass`, which always defaults to MELEE, and the generic
 * [MagicCombatStrategy] only works off a *player's* selected spell. Without this, Ahrim
 * fell through to melee and swung with a Strength level of 1 - the only mage of the six
 * brothers was comfortably the least dangerous, hitting for about 1.
 *
 * Accuracy comes from [MagicCombatFormula.getAccuracy], which already has a real NPC
 * path (magic level and magic attack bonus against the target's magic defence), so his
 * 100 Magic and +73 magic attack bonus do the work. Damage is a flat roll up to
 * [MAX_HIT], the figure the wiki gives, rather than a spell's max hit - he casts no
 * identifiable standard spell.
 *
 * **Blighted Aura**, his set effect: "a 20% chance to lower the player's Strength stat
 * by 5 for each successful hit", and it applies *through* protection prayers, which is
 * why the drain is attached to the hit landing rather than to damage being dealt.
 *
 * Two honest gaps:
 * - **No projectile.** Nothing in this project's graphic table names an Ahrim spell
 *   projectile, and inventing an id would be a guess. He plays his cast animation and
 *   the damage lands on a distance-based delay; the only graphic used is his real
 *   [Graphic.AHRIMS_BLIGHTED_AURA], shown on the target when the aura procs.
 * - **He does not chase.** [moveToAttackRange] only tests range, it does not move (see
 *   its doc), and a custom npc combat loop bypasses the engine's own pathing. Fine in
 *   the Barrows tomb, where the player comes to him.
 *
 * The wiki also credits him with Confuse/Weaken/Curse; those are not implemented here -
 * only the Blighted Aura, which is his defining and precisely-specified effect.
 */
class AhrimCombatPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onNpcCombat(AHRIM) {
            npc.queue { npc.combat(this) }
        }
    }

    private suspend fun Npc.combat(task: QueueTask) {
        var target = getCombatTarget() ?: return

        while (canEngageCombat(target)) {
            facePawn(target)
            if (moveToAttackRange(task, target, distance = Combat.npcAttackRange(this, FALLBACK_ATTACK_RANGE), projectile = true) && isAttackDelayReady()) {
                castAttack(target)
                postAttackLogic(target)
            }
            task.wait(1)
            target = getCombatTarget() ?: break
        }

        resetFacePawn()
        removeCombatTarget()
    }

    private fun Npc.castAttack(target: Pawn) {
        prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
        animate(ATTACK_ANIMATION)
        /*
         * `Sound.AHRIM_ATTACK` pairs with the animation directly: 2078 is
         * `Animation.HUMAN_AHRIMS_STAFF_ATTACK` and 1317 is the clip that staff makes. An
         * `onNpcCombat` loop never reads `defaultAttackSound`, so without this line the whole fight
         * is silent - and Ahrim has no entry in `named-combat-media.json` to fill that field anyway.
         *
         * Sounded here rather than through an entry in that file for the reason
         * `npc-animations/README.md` records about the Barrows brothers: `AHRIM_ATTACK` has no HIT or
         * DEATH sibling, so it cannot supply the three-role triple an entry needs. It is exactly
         * right for this one swing.
         */
        playSpellSound(this, target, Sound.AHRIM_ATTACK)

        val hitDelay = MagicCombatStrategy.getHitDelay(getFrontFacingTile(target), target.getCentreTile())
        val landed = MagicCombatFormula.getAccuracy(this, target) >= world.randomDouble()

        if (!landed) {
            target.hit(damage = 0, type = HitType.BLOCK, delay = hitDelay)
            return
        }

        target
            .hit(damage = world.random(MAX_HIT), type = HitType.HIT, delay = hitDelay)
            .addAction { applyBlightedAura(target) }
    }

    /**
     * 20% chance per landed hit to drain 5 Strength. Clamped so it can't take the stat
     * below zero, and applied to players only - the drain is a skill-level change and
     * npc-vs-npc Barrows isn't a thing.
     */
    private fun Npc.applyBlightedAura(target: Pawn) {
        if (target !is Player || !world.chance(AURA_CHANCE_NUMERATOR, AURA_CHANCE_DENOMINATOR)) {
            return
        }
        val current = target.getSkills().getCurrentLevel(Skills.STRENGTH)
        if (current <= 0) {
            return
        }
        target.getSkills().alterCurrentLevel(Skills.STRENGTH, -minOf(AURA_DRAIN, current))
        target.graphic(id = Graphic.AHRIMS_BLIGHTED_AURA, height = 124)
        // The proc has a clip named for it - `Sound.AHRIMS_AURA` sits next to `AHRIM_ATTACK` - and
        // the aura is the one moment in the fight worth hearing.
        playSpellSound(this, target, Sound.AHRIMS_AURA)
        target.message("Ahrim's blighted aura saps your strength.")
    }

    private companion object {
        const val AHRIM = "npc.ahrim_the_blighted"

        /** His real staff attack animation. His combat def's 729 is STUN_SPELL_CAST. */
        const val ATTACK_ANIMATION = Animation.HUMAN_AHRIMS_STAFF_ATTACK

        /** Wiki max hit. Rolled flat 0..20 - he casts no identifiable standard spell. */
        const val MAX_HIT = 20

        /** Only a fallback - the live value is `attackRange` in AhrimPlugin's combat def. */
        const val FALLBACK_ATTACK_RANGE = 8

        const val AURA_CHANCE_NUMERATOR = 1
        const val AURA_CHANCE_DENOMINATOR = 5 // 20%
        const val AURA_DRAIN = 5
    }
}
