package org.alter.plugins.content.combat.specialattack.weapons.bonedagger

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Bone dagger - **Backstab**: greatly increased accuracy against an unsuspecting target, and the
 * target's Defence dropped by the damage dealt.
 *
 * "Unsuspecting" means the target has not yet been drawn into the fight, which
 * [SpecialAttackEffects.isUnsuspecting] gets for free from the combat timer `Combat.postAttack`
 * only stamps on *after* the special resolves. Opening a fight with it is therefore a guaranteed
 * hit; using it mid-fight is an ordinary swing with the drain attached.
 */
class BoneDaggerPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Backstab") {
            player.animate(Animation.BONE_DAGGER_SPECIAL)
            player.graphic(Graphic.BONE_DAGGER_SPECIAL)

            val victim = target
            val maxHit = MeleeCombatFormula.getMaxHit(player, victim)
            val landHit =
                SpecialAttackEffects.isUnsuspecting(victim) ||
                    MeleeCombatFormula.getAccuracy(player, victim) >= world.randomDouble()

            val hit = player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = landHit)
            hit.hit.addAction {
                SpecialAttackEffects.drain(victim, Skills.DEFENCE, hit.hit.hitmarks.sumOf { it.damage })
            }
        }
    }
}
