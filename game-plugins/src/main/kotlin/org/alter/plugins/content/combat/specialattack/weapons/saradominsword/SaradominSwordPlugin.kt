package org.alter.plugins.content.combat.specialattack.weapons.saradominsword

import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.getEquipment
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.dealExactHit
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackDefs
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit

/**
 * Saradomin sword and Saradomin's blessed sword - **Saradomin's Lightning**.
 *
 * One name over two different attacks, told apart by their own cache descriptions rather than by a
 * list of ids:
 *
 * - the **Saradomin sword** adds 10% melee damage and calls down 1-16 extra *magic* damage on top,
 *   as a second splat that ignores the target's melee defences entirely;
 * - the **blessed sword** simply hits 25% harder, and calls down nothing.
 *
 * The magic splat pays Magic experience rather than melee, so it is dealt directly instead of
 * through [dealMeleeSpecialHit].
 */
class SaradominSwordPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Saradomin's Lightning") {
            val description = player.getEquipment(EquipmentType.WEAPON)?.id?.let { SpecialAttackDefs.description(it) }.orEmpty()
            val blessed = "25% higher max hit" in description

            player.animate(if (blessed) Animation.SARADOMINS_BLESSED_SWORD_SPECIAL else Animation.SARADOMIN_SWORD_SPECIAL)
            player.graphic(Graphic.SARADOMIN_SWORD_SPECIAL_CAST)

            val multiplier = if (blessed) BLESSED_DAMAGE_MULTIPLIER else SWORD_DAMAGE_MULTIPLIER
            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = multiplier)
            val landHit = MeleeCombatFormula.getAccuracy(player, target) >= world.randomDouble()
            player.dealMeleeSpecialHit(target = target, maxHit = maxHit, landHit = landHit)

            if (!blessed && landHit) {
                target.graphic(Graphic.SARADOMIN_SWORD_SPECIAL_HIT, 96)
                val magic = world.random(MIN_MAGIC_DAMAGE..MAX_MAGIC_DAMAGE)
                player.dealExactHit(target = target, damage = magic, landHit = true, delay = 0)
                player.addXp(Skills.MAGIC, magic * MAGIC_XP_PER_DAMAGE)
            }
        }
    }

    private companion object {
        const val SWORD_DAMAGE_MULTIPLIER = 1.1
        const val BLESSED_DAMAGE_MULTIPLIER = 1.25
        const val MIN_MAGIC_DAMAGE = 1
        const val MAX_MAGIC_DAMAGE = 16
        const val MAGIC_XP_PER_DAMAGE = 2.0
    }
}
