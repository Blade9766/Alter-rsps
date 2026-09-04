package org.alter.plugins.content.combat.specialattack.weapons.abyssalbludgeon

import org.alter.api.Skills
import org.alter.api.cfg.Graphic
import org.alter.api.ext.getTarget
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.AreaSound
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

class AbyssalBludgeonPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {

        SpecialAttacks.registerByName("Penance") {
            player.animate(id = 3299)
            player.graphic(id = 1284)

            world.spawn(AreaSound(tile = player.tile, id = 2715, radius = 10, volume = 1, delay = 10))
            world.spawn(AreaSound(tile = player.tile, id = 1930, radius = 10, volume = 1, delay = 30))

            /*
             * 0.5% more damage per prayer point spent. This used to pass the bonus alone as the
             * multiplier - 0.25 for fifty points drained - which scaled the max hit *down* to a
             * quarter and made Penance weaker the more it was earned. It is 1 + the bonus.
             */
            val spent = player.getSkills().getBaseLevel(Skills.PRAYER) - player.getSkills().getCurrentLevel(Skills.PRAYER)
            val multiplier = 1.0 + spent.coerceAtLeast(0) * DAMAGE_PER_PRAYER_POINT
            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = multiplier)
            val landHit = MeleeCombatFormula.getAccuracy(player, target) >= world.randomDouble()
            player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = landHit)
        }

        setItemCombatLogic("item.abyssal_bludgeon") {
            val target = player.getTarget()
            if (target != null) {
                target.graphic(Graphic.ABYSSAL_BLUDGEON_SPECIAL)
                player.dealHit(target = target, maxHit = 10, landHit = true, delay = 1)
            }
        }
    }

    private companion object {
        /** 0.5% per point of Prayer spent, per the wiki. */
        const val DAMAGE_PER_PRAYER_POINT = 0.005
    }
}
