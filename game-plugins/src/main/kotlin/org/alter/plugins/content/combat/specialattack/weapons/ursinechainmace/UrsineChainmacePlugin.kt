package org.alter.plugins.content.combat.specialattack.weapons.ursinechainmace

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Ursine chainmace - **Bear Down**: double accuracy, then 20 damage bled out over six seconds, the
 * target's Agility taken down twenty levels and their run stopped.
 *
 * The bleed and the crippling only land on a successful hit. The run half is a run-energy wipe
 * rather than the real six-tick prohibition - see [SpecialAttackEffects.stopRunning] - and the
 * Agility drain does nothing to an npc, which has no Agility level, exactly as in the real game.
 *
 * The mace's revenant ether charge is not modelled; there is no ether system here to spend.
 */
class UrsineChainmacePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Bear Down") {
            player.animate(Animation.DRAGON_MACE_SPECIAL)

            val victim = target
            val maxHit = MeleeCombatFormula.getMaxHit(player, victim)
            val accuracy = MeleeCombatFormula.getAccuracy(player, victim, specialAttackMultiplier = ACCURACY_MULTIPLIER)
            val landHit = accuracy >= world.randomDouble()

            player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = landHit).hit.addAction {
                if (landHit) {
                    SpecialAttackEffects.damageOverTime(player, victim, total = BLEED_DAMAGE, ticks = BLEED_TICKS)
                    SpecialAttackEffects.drain(victim, Skills.AGILITY, AGILITY_DRAIN)
                    SpecialAttackEffects.stopRunning(victim)
                }
            }
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 2.0

        /** 20 damage over six seconds. */
        const val BLEED_DAMAGE = 20
        const val BLEED_TICKS = 10

        const val AGILITY_DRAIN = 20
    }
}
