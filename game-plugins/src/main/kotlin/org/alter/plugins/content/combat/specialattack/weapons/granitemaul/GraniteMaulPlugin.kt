package org.alter.plugins.content.combat.specialattack.weapons.granitemaul

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Granite maul - **Quick Smash**: one extra attack, immediately.
 *
 * No accuracy or damage bonus at all - the whole special is that it happens *now*, off the attack
 * timer, which is what makes the maul the classic PvP finisher.
 *
 * **Where this falls short of the real thing.** In OSRS the special is fired outside the attack
 * cycle entirely, so a maul spec lands on the same tick as a normal swing. Here specials run in
 * place of the swing that triggered them, so this is one ordinary hit rather than a second one on
 * top. Getting the real behaviour needs the special to be able to run without consuming the attack
 * - which is a change to `CombatPlugin`'s cycle, not to this weapon.
 */
class GraniteMaulPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Quick Smash") {
            player.animate(Animation.GRANITE_MAUL_SPECIAL)
            player.graphic(Graphic.GRANITE_MAUL_SPECIAL)

            val maxHit = MeleeCombatFormula.getMaxHit(player, target)
            val landHit = MeleeCombatFormula.getAccuracy(player, target) >= world.randomDouble()
            player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = landHit)
        }
    }
}
