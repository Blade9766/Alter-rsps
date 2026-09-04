package org.alter.plugins.content.combat.specialattack.weapons.abyssaldagger

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.AreaSound
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import kotlin.math.roundToInt

class AbyssalDaggerPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {

        // Every abyssal dagger, poisoned grades and Bounty Hunter variants included.
        run {
            SpecialAttacks.registerByName("Abyssal Puncture") {
                player.animate(id = 3300)
                player.graphic(id = 1283)
                world.spawn(AreaSound(tile = player.tile, id = 2537, radius = 10, volume = 1))

                val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = 1.25)
                val landHit = accuracy >= world.randomDouble()

                for (i in 0 until 2) {
                    val maxHit = MeleeCombatFormula.getMaxHit(player, target) * .85
                    player.dealHit(target = target, maxHit = maxHit.roundToInt(), landHit = landHit, delay = 1)
                }
            }
        }
    }
}
