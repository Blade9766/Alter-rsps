package org.alter.plugins.content.combat.specialattack.weapons.thunderkhopesh

import org.alter.api.cfg.Animation
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.entity.Npc
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit
import org.alter.plugins.content.combat.strategy.ranged.RangedAoe

/**
 * Thunder khopesh - **Lingering Lightning**: two swings, and lightning left behind wherever the
 * target was standing when either of them landed.
 *
 * The lightning is the interesting half: it strikes the *tile*, not the target, so anything else
 * standing near where the target was takes it too - and a target that has since moved off does not.
 * The strike is delayed a few ticks, which is what makes the special worth using into a pack.
 */
class ThunderKhopeshPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Lingering Lightning") {
            player.animate(Animation.DRAGON_SCIMITAR_SPECIAL)

            val victim = target
            var struck = false

            repeat(2) { swing ->
                val maxHit = MeleeCombatFormula.getMaxHit(player, victim)
                val landHit = MeleeCombatFormula.getAccuracy(player, victim) >= world.randomDouble()
                player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = landHit, delay = swing)
                if (landHit) {
                    struck = true
                }
            }

            if (struck) {
                strike(victim.tile)
            }
        }
    }

    /** Everything within a tile of [where], a few ticks after the swings that called it down. */
    private fun org.alter.plugins.content.combat.specialattack.CombatContext.strike(where: Tile) {
        val struck = mutableListOf<Npc>()
        world.npcs.forEach { npc ->
            if (struck.size < MAX_STRUCK && npc.isAlive() && npc.tile.isWithinRadius(where, LIGHTNING_RADIUS)) {
                struck.add(npc)
            }
        }

        struck.forEach { npc ->
            val maxHit = MeleeCombatFormula.getMaxHit(player, npc, specialAttackMultiplier = LIGHTNING_DAMAGE)
            player.dealMeleeSpecialHit(target = npc, maxHit = maxHit, landHit = true, delay = LIGHTNING_DELAY)
        }
    }

    private companion object {
        const val LIGHTNING_RADIUS = 1
        const val LIGHTNING_DAMAGE = 0.5
        const val LIGHTNING_DELAY = 3
        const val MAX_STRUCK = 9
    }
}
