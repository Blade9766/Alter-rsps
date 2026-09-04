package org.alter.plugins.content.npcs.battlemage

import org.alter.api.HitType
import org.alter.api.ext.*
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.combat.strategy.CombatStrategy
import org.alter.plugins.content.combat.strategy.MagicCombatStrategy
import org.alter.plugins.content.combat.strategy.playSpellSound
import org.alter.game.model.Graphic as GraphicEntity

/**
 * The battle mages' attack: their own god spell, every swing.
 *
 * ## Why the god spells are not in [org.alter.plugins.content.combat.strategy.magic.CombatSpell]
 *
 * That enum holds the spells a *player* can autocast, and the three god spells are not among them -
 * they are unlocked by the Mage Arena miniquest, which is what these mages exist for. So the spell
 * has to be assembled here, and each piece is a named constant rather than a chosen id:
 * `Graphic.SARADOMIN_STRIKE` (76), `Graphic.CLAWS_OF_GUTHIX` (77), `Graphic.FLAMES_OF_ZAMORAK` (78),
 * `Animation.GOD_SPELL` (811) and, for the gnome, `Animation.GUTHIX_BATTLE_MAGE_ATTACK` (197).
 *
 * ## No projectile, and that is correct
 *
 * A god spell has no travelling missile: it appears on the target. So this spawns no `Projectile` at
 * all, and the impact graphic is played on the target at the hit delay instead. That is the one place
 * this differs from [org.alter.plugins.content.npcs.elderchaosdruid.ElderChaosDruidCombatStrategy],
 * which fires a real Wind Wave.
 *
 * ## Why a strategy, and why no `prepareAttack`
 *
 * The reasons `content/npcs/chaosdruid` sets out and the whole tree now follows: `onNpcCombat` would
 * replace the engine's combat loop, which is the only thing that walks, leashes and line-of-sights an
 * npc; and `prepareAttack` would leave the mage's `CombatStyle` set to MAGIC, which is a trap for any
 * monster that also swings. Accuracy is rolled through [MagicCombatFormula], which reads the mage's
 * magic level of 50 against the target's magic defence - and which is why the page can say these have
 * "high accuracy" and mean it.
 */
internal object BattleMageCombatStrategy : CombatStrategy {
    /** Nothing travels, but a god spell still needs sight of its target rather than a walkable path. */
    override val usesProjectile: Boolean = true

    override fun getAttackRange(pawn: Pawn): Int = Combat.npcAttackRange(pawn, BattleMages.SPELL_RANGE)

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
        val mage = BattleMages.ALL.firstOrNull { getRSCMId(it.npcKey) == pawn.id } ?: return
        val world = pawn.world

        pawn.animate(mage.castAnimation)
        // An npc has no client of its own, so the cast has to be heard through the player it is
        // aimed at - the same reason the dark wizards' casts were silent. See playSpellSound.
        playSpellSound(pawn, target, mage.castSound)

        val hitDelay = MagicCombatStrategy.getHitDelay(pawn.getFrontFacingTile(target), target.getCentreTile())
        if (MagicCombatFormula.getAccuracy(pawn, target) >= world.randomDouble()) {
            target.hit(damage = world.random(BattleMages.MAX_HIT), type = HitType.HIT, delay = hitDelay)
            target.graphic(GraphicEntity(mage.impactGfx, GRAPHIC_HEIGHT, hitDelay))
        } else {
            target.hit(damage = 0, type = HitType.BLOCK, delay = hitDelay)
        }
    }

    /** Resolved lazily and cached: `RSCM.init()` runs after this object's class is loaded. */
    private val ids: Map<String, Int> by lazy {
        BattleMages.ALL.associate { it.npcKey to org.alter.rscm.RSCM.getRSCM(it.npcKey) }
    }

    private fun getRSCMId(key: String): Int = ids.getValue(key)

    /** The height a spell impact renders at, as every impact graphic in this codebase uses. */
    private const val GRAPHIC_HEIGHT = 124
}
