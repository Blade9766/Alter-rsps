package org.alter.plugins.content.npcs.slayer

import org.alter.api.HitType
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.*
import org.alter.plugins.content.combat.SingleCombat
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.combat.playAttackSound
import org.alter.plugins.content.combat.strategy.MagicCombatStrategy
import org.alter.rscm.RSCM.getRSCM

/**
 * Magic attacks for the two Slayer monsters that actually cast: the infernal mage and the aberrant
 * spectre.
 *
 * There is still no generic "NPC attacks with magic" path in this codebase - `MagicCombatStrategy`
 * dereferences the attacker's selected spellbook spell, which an npc never has, and
 * `CombatConfigs` says so itself. So these two need a bespoke combat loop for the same reason
 * `content/npcs/darkwizard` does, and this file is deliberately modelled on
 * [org.alter.plugins.content.npcs.darkwizard.DarkWizardCombatPlugin] rather than inventing a second
 * approach. Without it, giving either monster `CombatStyle.MAGIC` would route it into that strategy
 * and read a null spell.
 *
 * ## What is different from the dark wizards
 *
 * The wizards cast **named spells** lifted wholesale from `CombatSpell`, which hands them an
 * animation, a projectile, impact graphics and a max hit for free. That is not available here: the
 * infernal mage and aberrant spectre do not cast a player spell, and their published data is a max
 * hit (8 each) and an attack animation, nothing more. So the attack is built from what *is*
 * published - own animation, own max hit, accuracy rolled through [MagicCombatFormula] against the
 * target's magic defence - and carries **no projectile or impact graphic**.
 *
 * For the aberrant spectre that is correct: its attack is a stench with no projectile in the real
 * game. For the infernal mage it is a visible shortfall - it does throw something in OSRS - but the
 * wiki publishes no graphic id for it, and picking one out of the cache that merely looks right
 * would be a guess dressed as data. It casts, the damage and timing are right, and the missile is
 * missing.
 *
 * ## Attack claiming
 *
 * Both monsters are spawned in clusters - fifteen infernal mages share one floor of the Slayer
 * Tower - so without a claim a player walking onto the first floor is hit by every mage in the room
 * at once.
 *
 * The claim is [org.alter.plugins.content.combat.SingleCombat]. This file used to carry its own copy
 * of `content/npcs/darkwizard`'s method, down to a private `ENGAGED_BY` attribute of the same name -
 * which meant the two could not see each other's claims and a player standing in range of both got
 * the dogpile anyway. One object, one attribute, and it respects multi-combat areas, which neither
 * copy did.
 */
class SlayerCasterPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    private val castersByNpcId: Map<Int, SlayerMonster> =
        SlayerMonsters.ALL
            .filter { it.magicMaxHit != null }
            .flatMap { monster -> monster.npcKeys.map { getRSCM(it) to monster } }
            .toMap()

    init {
        SlayerMonsters.ALL.filter { it.magicMaxHit != null }.forEach { monster ->
            monster.npcKeys.forEach { npcKey ->
                onNpcCombat(npcKey) {
                    npc.queue { npc.combat(this) }
                }
            }
        }
    }

    private suspend fun Npc.combat(task: QueueTask) {
        var target = getCombatTarget() ?: return
        val monster = castersByNpcId[id] ?: return

        while (canEngageCombat(target)) {
            facePawn(target)
            val attacking =
                moveToAttackRange(task, target, distance = Combat.npcAttackRange(this, ATTACK_RANGE), projectile = true) &&
                    isAttackDelayReady() &&
                    SingleCombat.claim(this, target)
            if (attacking) {
                cast(target, monster)
                postAttackLogic(target)
            }
            task.wait(1)
            target = getCombatTarget() ?: break
        }

        SingleCombat.release(this, target)
        resetFacePawn()
        removeCombatTarget()
    }

    /**
     * One cast: the monster's own animation, accuracy through the shared magic formula, and a
     * damage roll capped at the monster's published max hit.
     *
     * [MagicCombatStrategy.getHitDelay] is reused rather than a flat delay so the hit lands the same
     * number of ticks after the cast as a player's spell would at the same distance.
     */
    private fun Npc.cast(
        target: Pawn,
        monster: SlayerMonster,
    ) {
        prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
        animate(monster.attackAnimation)
        /*
         * An `onNpcCombat` loop never touches `defaultAttackSound`, so these casts were silent even
         * where the sound was already sourced: the aberrant spectre carries `attackSound = 272` in
         * its own combat def and had never played it once.
         */
        playAttackSound(target)

        val hitDelay = MagicCombatStrategy.getHitDelay(getFrontFacingTile(target), target.getCentreTile())
        if (MagicCombatFormula.getAccuracy(this, target) >= world.randomDouble()) {
            target.hit(damage = world.random(monster.magicMaxHit!!), type = HitType.HIT, delay = hitDelay)
        } else {
            target.hit(damage = 0, type = HitType.BLOCK, delay = hitDelay)
        }
    }


    private companion object {
        /**
         * Neither wiki page publishes an attack range. Ten is the standard magic attack range, and
         * it is what `Combat.npcAttackRange` falls back to when the cache carries none.
         */
        const val ATTACK_RANGE = 10

    }
}
