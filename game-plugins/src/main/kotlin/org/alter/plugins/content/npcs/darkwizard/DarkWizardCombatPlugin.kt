package org.alter.plugins.content.npcs.darkwizard

import org.alter.api.HitType
import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Graphic
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.move.walkTo
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.*
import org.alter.plugins.content.combat.SingleCombat
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.combat.strategy.MagicCombatStrategy
import org.alter.plugins.content.combat.strategy.playSpellSound
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.rscm.RSCM.getRSCM

/**
 * Dark wizards' magic attacks.
 *
 * There's no generic "NPC casts a magic attack" system in this codebase to declare
 * combat stats onto and get a working magic attacker for free: the engine's default
 * combat loop ([org.alter.plugins.content.combat.CombatPlugin.cycle]) reads
 * `Npc.combatClass`, which always defaults to MELEE and is only ever changed by
 * per-monster custom attack code (confirmed - it's set nowhere else in this codebase),
 * and [MagicCombatStrategy] (the generic magic strategy players use) unconditionally
 * reads a player's selected spellbook spell, which an NPC never has. So, like
 * [org.alter.plugins.content.npcs.kbd.KbdCombatPlugin], this is a small custom attack
 * loop. [MagicCombatFormula.getAccuracy] is reused as-is (it already has a real Npc
 * code path: magic level + bonuses vs the target's magic defence).
 *
 * All variants cast a real, wiki-verified spell pair - a damaging strike reused wholesale
 * from [CombatSpell] (no need to invent animation/graphics/max hit) and a curse spell
 * that drains one of the target's combat stats by 5% (floored, minimum 1) on a
 * successful hit:
 * - **Level 7/11**: Water Strike ([CombatSpell.WATER_STRIKE]) or Confuse (drains Attack).
 *   Water Strike's cast animation (711) happens to be one of the real candidates this
 *   project's own animation resolver saw for these exact npc ids (see
 *   [DarkWizardConfigsPlugin]'s doc comment), so it's a good fit.
 * - **Level 20/22/23**: Earth Strike ([CombatSpell.EARTH_STRIKE]) or Weaken (drains
 *   Strength).
 *
 * Per the wiki, both curse spells "can only be cast if [the] opponent's/target's stats
 * haven't already been lowered" - checked broadly across all five combat stats via
 * [hasNoActiveStatDrain] (matching the real curse-spell family's shared restriction,
 * since the wiki's wording is plural, not just the one stat each spell itself drains).
 * When a target already has an active drain, the wizard just casts its strike spell
 * instead that turn - see [castSpellPair].
 *
 * **Only one wizard attacks a given target at a time.** Several spawn locations place
 * many wizards within aggro range of each other (the Varrock stone circle and Dark
 * Wizards' Tower rooms especially), and without a claim system every nearby wizard
 * attacks the instant it is in range, which reads as "attacks way too fast, all at
 * once" even though each individual wizard's own attack speed is correct. A wizard that
 * is not holding the claim faces the target and shuffles about near its spawn instead
 * of attacking (see [idleShuffle]; it used to stand perfectly still, which read as a
 * row of statues).
 *
 * The rule is [org.alter.plugins.content.combat.SingleCombat] now, not a local one. This
 * plugin wrote the first implementation of it, `content/npcs/slayer` copied that method
 * verbatim for its casters, and the second bestiary pass added a third at the aggression
 * layer; the two copies each had their **own private** `ENGAGED_BY` key, so a wizard and
 * an aberrant spectre could hold the same player at the same time and neither saw the
 * other. All three ask one object against one attribute now.
 *
 * That merge also retired the caveat this note used to carry - that the claim "applies
 * everywhere, including the Wilderness spawns, where real OSRS *would* let several
 * wizards pile on at once". [org.alter.plugins.content.combat.SingleCombat] respects
 * multi-combat areas, so in one they pile on again, correctly.
 */
class DarkWizardCombatPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    private val lowTierIds: Set<Int> = (DarkWizardData.LEVEL_7 + DarkWizardData.LEVEL_11).map { getRSCM(it) }.toSet()

    init {
        DarkWizardData.VARIANTS.forEach { variant ->
            onNpcCombat(variant.npcKey) {
                npc.queue { npc.combat(this) }
            }
        }
    }

    private suspend fun Npc.combat(task: QueueTask) {
        var target = getCombatTarget() ?: return

        while (canEngageCombat(target)) {
            facePawn(target)
            val attacking =
                moveToAttackRange(task, target, distance = Combat.npcAttackRange(this, FALLBACK_ATTACK_RANGE), projectile = true) &&
                    isAttackDelayReady() &&
                    SingleCombat.claim(this, target)
            if (attacking) {
                if (id in lowTierIds) {
                    castSpellPair(target, strikeSpell = CombatSpell.WATER_STRIKE, curse = CONFUSE)
                } else {
                    castSpellPair(target, strikeSpell = CombatSpell.EARTH_STRIKE, curse = WEAKEN)
                }
                postAttackLogic(target)
            } else if (!SingleCombat.holds(this, target)) {
                idleShuffle()
            }
            task.wait(1)
            target = getCombatTarget() ?: break
        }

        SingleCombat.release(this, target)
        resetFacePawn()
        removeCombatTarget()
    }

    /**
     * A little idle movement for a wizard that is engaged but isn't the one actually
     * attacking.
     *
     * Without this such a wizard is a statue: the single-way claim means only one wizard
     * attacks a given target, but every *other* engaged wizard still runs this loop,
     * re-faces the target every tick and does nothing else. It doesn't wander either,
     * because [org.alter.plugins.content.mechanics.npcwalk.NpcRandomWalkPlugin] - which
     * these already qualify for, they spawn with `walkRadius = 3` - skips any NPC whose
     * `FACING_PAWN_ATTR` is set, and this loop sets it on every iteration. At the
     * Varrock stone circle that left ten wizards frozen mid-stare.
     *
     * Deliberately kept tight: steps are picked within [IDLE_RADIUS] of the wizard's
     * own **spawn tile**, not its current position, so repeated shuffles can't drift it
     * away from its post. Only bystanders shuffle - the wizard holding the attack slot
     * stands and casts, so this never pulls an attacker out of range mid-fight.
     */
    private fun Npc.idleShuffle() {
        if (hasMoveDestination() || !lock.canMove()) {
            return
        }
        if (!world.chance(1, IDLE_STEP_ODDS)) {
            return
        }
        val dest =
            spawnTile.transform(
                world.random(-IDLE_RADIUS..IDLE_RADIUS),
                world.random(-IDLE_RADIUS..IDLE_RADIUS),
            )
        // Same guard NpcRandomWalkPlugin uses - don't path into an unloaded chunk.
        if (world.chunks.get(dest, createIfNeeded = false) == null) {
            return
        }
        walkTo(dest)
    }


    private fun Npc.castSpellPair(
        target: Pawn,
        strikeSpell: CombatSpell,
        curse: CurseSpell,
    ) {
        if (target is Player && hasNoActiveStatDrain(target) && world.chance(1, 2)) {
            castCurse(target, curse)
        } else {
            castStrike(target, strikeSpell)
        }
    }

    /**
     * Whether a curse may be cast at [target] at all - the wiki's "can only be cast if the
     * opponent's stats haven't already been lowered", read across all five combat stats because its
     * wording is plural.
     *
     * A curse still travelling towards the target counts as a drain: its effect has not been
     * applied yet, so the stats alone would still read undrained. See [castCurse].
     */
    private fun hasNoActiveStatDrain(target: Player): Boolean =
        !target.timers.has(CURSE_IN_FLIGHT) &&
            COMBAT_STATS.none { target.getSkills().getCurrentLevel(it) < target.getSkills().getBaseLevel(it) }

    private fun Npc.castStrike(
        target: Pawn,
        spell: CombatSpell,
    ) {
        val projectile =
            createProjectile(
                target,
                gfx = spell.projectile,
                startHeight = 43,
                endHeight = spell.projectilEndHeight.takeIf { it >= 0 } ?: 31,
                delay = 41,
                angle = 15,
                steepness = 127,
            )
        prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
        animate(spell.castAnimation)
        spell.castGfx?.let { graphic(it) }
        world.spawn(projectile)
        // These casts were silent: the wizard has no client of its own, so the sound
        // has to reach the player it is aimed at. See playSpellSound.
        playSpellSound(this, target, spell.castSound)

        val hitDelay = MagicCombatStrategy.getHitDelay(getFrontFacingTile(target), target.getCentreTile())
        if (MagicCombatFormula.getAccuracy(this, target) >= world.randomDouble()) {
            target
                .hit(damage = world.random(spell.maxHit), type = HitType.HIT, delay = hitDelay)
                .addAction { playSpellSound(this@castStrike, target, spell.impactSound) }
            spell.impactGfx?.let { target.graphic(Graphic(it.id, it.height, projectile.impactDelay)) }
        } else {
            target.hit(damage = 0, type = HitType.BLOCK, delay = hitDelay)
        }
    }

    private fun Npc.castCurse(
        target: Player,
        curse: CurseSpell,
    ) {
        val projectile =
            createProjectile(target, gfx = curse.projectileGfx, startHeight = 43, endHeight = 31, delay = 41, angle = 15, steepness = 127)
        prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
        animate(curse.castAnimation)
        graphic(id = curse.castGfx, height = 92, delay = 0)
        world.spawn(projectile)
        playSpellSound(this, target, curse.castSound)

        val hitDelay = MagicCombatStrategy.getHitDelay(getFrontFacingTile(target), target.getCentreTile())
        val landed = MagicCombatFormula.getAccuracy(this, target) >= world.randomDouble()
        val curseHit = target.hit(damage = 0, type = if (landed) HitType.HIT else HitType.BLOCK, delay = hitDelay)

        if (landed) {
            /*
             * A drain is now on its way, and does not exist yet. [hasNoActiveStatDrain] counts that
             * as a drain already being active, because otherwise moving the drain onto the landing
             * would open a window it closes: a wizard attacks every 4 cycles and a curse takes up
             * to 5 to arrive at eight tiles, so the *next* cast would be chosen before this one
             * landed, read the target's stats as untouched, and curse a second time - stacking two
             * drains on a target the wiki says can carry only one.
             *
             * Only a curse that actually landed is marked. A miss drains nothing, so there is
             * nothing in the air to wait for and the next cast is free to curse.
             *
             * A plain [TimerKey] with no persistence key is transient and identity-keyed: it is
             * never written to a save file, and it counts itself down and is dropped, so nothing
             * strands the target un-cursable if the hit never lands - the target died, or the
             * wizard did.
             */
            target.timers[CURSE_IN_FLIGHT] = hitDelay
            target.graphic(id = curse.impactGfx, height = 124, delay = projectile.impactDelay)
            /*
             * The drain lands *with* the spell, not when it leaves the wizard's hands. It used to
             * be applied here directly, which dropped the target's stat up to five cycles - three
             * seconds - before the projectile reached them, and dropped it even if they died or
             * broke off in between. Same place the impact sound already fired, and the same place
             * [org.alter.plugins.content.combat.strategy.MagicCombatStrategy] applies a spell's
             * own `curseEffect`.
             */
            curseHit.addAction {
                playSpellSound(this@castCurse, target, curse.impactSound)
                val current = target.getSkills().getCurrentLevel(curse.drainedSkill)
                val reduction = (current * DRAIN_PERCENT).toInt().coerceAtLeast(1)
                target.getSkills().alterCurrentLevel(curse.drainedSkill, -reduction)
            }
        }
    }

    private data class CurseSpell(
        val drainedSkill: Int,
        val castAnimation: Int,
        val castGfx: Int,
        val projectileGfx: Int,
        val impactGfx: Int,
        /** Sound.CONFUSE_CAST_AND_FIRE / Sound.WEAKEN_CAST_AND_FIRE. */
        val castSound: Int,
        /** Sound.CONFUSE_HIT / Sound.WEAKEN_HIT. */
        val impactSound: Int,
    )

    private companion object {
        /** Only a fallback - the live value is `attackRange` in DarkWizardConfigsPlugin. */
        const val FALLBACK_ATTACK_RANGE = 8

        /** Idle steps stay within this many tiles of the wizard's spawn tile. */
        const val IDLE_RADIUS = 2

        /** 1-in-N chance per tick of a bystander taking an idle step (~7s average). */
        const val IDLE_STEP_ODDS = 12


        /**
         * Set on a target for as long as a curse is travelling towards it. Transient by
         * construction - a [TimerKey] with no persistence key is never saved - and self-expiring,
         * so a curse whose hit never lands cannot leave a target permanently un-cursable.
         */
        val CURSE_IN_FLIGHT = TimerKey()
        val COMBAT_STATS = listOf(Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE, Skills.MAGIC, Skills.RANGED)
        const val DRAIN_PERCENT = 0.05

        val CONFUSE =
            CurseSpell(
                drainedSkill = Skills.ATTACK,
                castAnimation = 716, // AnimationID.CAST_CONFUSE_WIZARD
                castGfx = 102, // Graphic.CONFUSE_CAST
                projectileGfx = 103, // Graphic.CONFUSE_PROJECTILE
                impactGfx = 104, // Graphic.CONFUSE_HIT
                castSound = 119,
                impactSound = 121,
            )
        val WEAKEN =
            CurseSpell(
                drainedSkill = Skills.STRENGTH,
                castAnimation = 717, // AnimationID.CAST_WEAKEN_WIZARD
                castGfx = 105, // Graphic.WEAKEN_CAST
                projectileGfx = 106, // Graphic.WEAKEN_PROJECTILE
                impactGfx = 107, // Graphic.WEAKEN_HIT
                castSound = 3011,
                impactSound = 3010,
            )
    }
}
