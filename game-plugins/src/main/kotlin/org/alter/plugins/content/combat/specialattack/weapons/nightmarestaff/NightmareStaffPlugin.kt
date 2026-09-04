package org.alter.plugins.content.combat.specialattack.weapons.nightmarestaff

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.getMagicDamageBonus
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMagicSpecialHit

/**
 * The volatile and eldritch nightmare staves - **Immolate** and **Invocate**.
 *
 * Both fire a spell that costs no runes and is not in the spellbook, so neither goes near
 * `Combat.CASTING_SPELL`: the max hit is computed here from the wiki's own formulas and handed
 * straight to a hit. Both scale off the *Magic level* and both then take the staff's own 15% magic
 * damage bonus plus whatever else the player is wearing, exactly as the wiki describes the two
 * being calculated separately.
 *
 * - **Immolate** (volatile): `min(58 x Magic / 99 + 1, 58)`, 50% more accuracy.
 * - **Invocate** (eldritch): `min(44 x Magic / 99 + 1, 44)`, with half the damage dealt paid back
 *   as Prayer points - and this is the one restore in the game that may take Prayer above the
 *   player's level, to a ceiling of 120.
 */
class NightmareStaffPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Immolate") {
            player.animate(Animation.NIGHTMARE_STAFF_SPECIAL)
            player.graphic(Graphic.VOLATILE_NIGHTMARE_STAFF_SPECIAL_CAST)

            val maxHit = staffMaxHit(player.getSkills().getCurrentLevel(Skills.MAGIC), VOLATILE_CAP)
            val accuracy = MagicCombatFormula.getAccuracy(player, target, specialAttackMultiplier = ACCURACY_MULTIPLIER)
            val landHit = accuracy >= world.randomDouble()

            val victim = target
            if (landHit) {
                victim.graphic(Graphic.VOLATILE_NIGHTMARE_STAFF_SPECIAL_HIT, 96)
            }
            player.dealMagicSpecialHit(
                target = victim,
                damage = if (landHit) world.random(maxHit) else 0,
                landHit = landHit,
            )
        }

        SpecialAttacks.registerByName("Invocate") {
            player.animate(Animation.NIGHTMARE_STAFF_SPECIAL)
            player.graphic(Graphic.ELDRITCH_NIGHTMARE_STAFF_SPECIAL_CAST)

            val maxHit = staffMaxHit(player.getSkills().getCurrentLevel(Skills.MAGIC), ELDRITCH_CAP)
            val accuracy = MagicCombatFormula.getAccuracy(player, target)
            val landHit = accuracy >= world.randomDouble()
            val damage = if (landHit) world.random(maxHit) else 0

            val victim = target
            if (landHit) {
                victim.graphic(Graphic.ELDRITCH_NIGHTMARE_STAFF_SPECIAL_HIT, 96)
            }
            player.dealMagicSpecialHit(target = victim, damage = damage, landHit = landHit)

            if (damage > 0) {
                val restored = damage / 2
                val prayer = player.getSkills().getCurrentLevel(Skills.PRAYER)
                player.getSkills().setCurrentLevel(Skills.PRAYER, minOf(prayer + restored, PRAYER_CEILING))
            }
        }
    }

    /**
     * `min(cap x magic / 99 + 1, cap)`, then the wearer's magic damage bonus - which already
     * includes the staff's own 15%, since it is on the staff's equipment stats.
     */
    private fun org.alter.plugins.content.combat.specialattack.CombatContext.staffMaxHit(
        magic: Int,
        cap: Int,
    ): Int {
        val spellMax = minOf(cap * magic / MAX_MAGIC_LEVEL + 1, cap)
        return (spellMax * (1.0 + player.getMagicDamageBonus() / 100.0)).toInt()
    }

    private companion object {
        const val MAX_MAGIC_LEVEL = 99
        const val VOLATILE_CAP = 58
        const val ELDRITCH_CAP = 44
        const val ACCURACY_MULTIPLIER = 1.5

        /** Invocate is allowed to overheal Prayer, but not past 120. */
        const val PRAYER_CEILING = 120
    }
}
