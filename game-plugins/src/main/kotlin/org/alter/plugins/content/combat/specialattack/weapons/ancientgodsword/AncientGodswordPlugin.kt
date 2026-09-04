package org.alter.plugins.content.combat.specialattack.weapons.ancientgodsword

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.heal
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.dealExactHit
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Ancient godsword - **Blood Sacrifice**: double accuracy and 10% more damage, and the target is
 * marked. A short while later the mark detonates for a flat 25 unless they have moved away, and the
 * wielder is healed for what it dealt.
 *
 * "Unless they move away" is a cancel condition on the delayed hit rather than a re-check when it
 * lands: the tile the target stood on is captured now, and the hit withdraws itself if they are no
 * longer on it. That is also why the heal hangs off the hit's own action - a cancelled sacrifice
 * must not heal.
 *
 * The heal is capped at 15 against another player, per the cache's own note.
 */
class AncientGodswordPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Blood Sacrifice") {
            player.animate(Animation.ANCIENT_GODSWORD_SPECIAL)
            player.graphic(Graphic.ANCIENT_GODSWORD_SPECIAL)

            val victim = target
            val maxHit = MeleeCombatFormula.getMaxHit(player, victim, specialAttackMultiplier = DAMAGE_MULTIPLIER)
            val accuracy = MeleeCombatFormula.getAccuracy(player, victim, specialAttackMultiplier = ACCURACY_MULTIPLIER)
            val landHit = accuracy >= world.randomDouble()

            player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = landHit)

            if (landHit) {
                val markedTile = victim.tile
                val sacrifice =
                    player.dealExactHit(
                        target = victim,
                        damage = SACRIFICE_DAMAGE,
                        landHit = true,
                        delay = SACRIFICE_DELAY,
                    )
                sacrifice.hit.setCancelIf { victim.isDead() || victim.tile != markedTile }
                sacrifice.hit.addAction {
                    val cap = if (victim is Player) PVP_HEAL_CAP else SACRIFICE_DAMAGE
                    player.heal(minOf(SACRIFICE_DAMAGE, cap))
                }
            }
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 2.0
        const val DAMAGE_MULTIPLIER = 1.1

        const val SACRIFICE_DAMAGE = 25
        const val SACRIFICE_DELAY = 5
        const val PVP_HEAL_CAP = 15
    }
}
