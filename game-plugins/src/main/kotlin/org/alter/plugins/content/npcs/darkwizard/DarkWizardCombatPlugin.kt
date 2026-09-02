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
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.*
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
 * Wizards' Tower rooms especially), and this codebase has no engine-level single/multi
 * -combat zone enforcement at all - `setMultiCombatRegion`/`Tile.isMulti` only ever
 * drive the client's multi-combat icon varbit ([org.alter.plugins.content.mechanics.multi.MultiwayCombatPlugin]),
 * they don't gate whether more than one NPC can simultaneously engage the same target
 * (confirmed by reading `Combat.canEngage` - it has no such check). Without a claim
 * system every nearby wizard would independently attack the instant it's in range,
 * which reads as "attacks way too fast, all at once" even though each individual
 * wizard's own attack speed is correct. [claimAttackSlot] has each wizard record itself
 * as the target's attacker via [ENGAGED_BY]; a wizard that isn't holding the claim (and
 * can't take over a stale one - the holder died, despawned, or moved on) faces the
 * target and shuffles about near its spawn instead of attacking (see [idleShuffle];
 * it used to stand perfectly still, which read as a row of statues). This is a
 * simplification, not a real
 * single-combat-zone implementation: it applies everywhere, including the Wilderness
 * spawns, where real OSRS *would* let several wizards pile on at once - traded off
 * deliberately since "wizards no longer dogpile" was the actual complaint being fixed.
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
                moveToAttackRange(task, target, distance = 8, projectile = true) &&
                    isAttackDelayReady() &&
                    claimAttackSlot(target)
            if (attacking) {
                if (id in lowTierIds) {
                    castSpellPair(target, strikeSpell = CombatSpell.WATER_STRIKE, curse = CONFUSE)
                } else {
                    castSpellPair(target, strikeSpell = CombatSpell.EARTH_STRIKE, curse = WEAKEN)
                }
                postAttackLogic(target)
            } else if (target.attr[ENGAGED_BY] != index) {
                idleShuffle()
            }
            task.wait(1)
            target = getCombatTarget() ?: break
        }

        if (target.attr[ENGAGED_BY] == index) {
            target.attr.remove(ENGAGED_BY)
        }
        resetFacePawn()
        removeCombatTarget()
    }

    /**
     * A little idle movement for a wizard that is engaged but isn't the one actually
     * attacking.
     *
     * Without this such a wizard is a statue: [claimAttackSlot] means only one wizard
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

    /**
     * Claims (or keeps) the right to attack [target] this cycle. A held claim is only
     * given up if its holder is no longer actually a threat - dead, despawned, or moved
     * on to a different target - so a wizard that dies mid-fight doesn't permanently
     * lock its target out of being attacked by anything else.
     */
    private fun Npc.claimAttackSlot(target: Pawn): Boolean {
        val holderIndex = target.attr[ENGAGED_BY]
        if (holderIndex != null && holderIndex != index) {
            val holder = world.npcs[holderIndex]
            val holderStillEngaging = holder != null && holder.isSpawned() && holder.isAlive() && holder.getCombatTarget() == target
            if (holderStillEngaging) {
                return false
            }
        }
        target.attr[ENGAGED_BY] = index
        return true
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

    private fun hasNoActiveStatDrain(target: Player): Boolean =
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
            spell.impactGfx?.let { target.graphic(Graphic(it.id, it.height, hitDelay)) }
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
            curseHit.addAction { playSpellSound(this@castCurse, target, curse.impactSound) }
            target.graphic(id = curse.impactGfx, height = 124, delay = hitDelay)
            val current = target.getSkills().getCurrentLevel(curse.drainedSkill)
            val reduction = (current * DRAIN_PERCENT).toInt().coerceAtLeast(1)
            target.getSkills().alterCurrentLevel(curse.drainedSkill, -reduction)
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
        /** Idle steps stay within this many tiles of the wizard's spawn tile. */
        const val IDLE_RADIUS = 2

        /** 1-in-N chance per tick of a bystander taking an idle step (~7s average). */
        const val IDLE_STEP_ODDS = 12

        val ENGAGED_BY = AttributeKey<Int>()
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
