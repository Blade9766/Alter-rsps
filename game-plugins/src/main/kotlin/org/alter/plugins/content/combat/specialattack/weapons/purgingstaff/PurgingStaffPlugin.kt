package org.alter.plugins.content.combat.specialattack.weapons.purgingstaff

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.ext.getMagicDamageBonus
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMagicSpecialHit
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.interfaces.attack.AttackTab

/**
 * Purging staff - **Scatter ashes**: casts the strongest demonbane spell the wielder knows, and
 * hands the bar back if it lands the killing blow on a demon.
 *
 * **The demonbane spells are not in this codebase.** Inferno, Superior and Dark Demonbane are
 * Varlamore additions and nothing here implements them, so the special cannot cast one. Rather than
 * do nothing, it fires the equivalent damage directly - the demonbane band scaled off the Magic
 * level, doubled against a demon the way every demonbane spell is - and keeps the two halves the
 * cache actually promises: the energy refund and the faster follow-up on a demon kill. When the
 * demonbane spells arrive this should delegate to the highest one the player can cast instead.
 */
class PurgingStaffPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Scatter ashes") {
            player.animate(Animation.NIGHTMARE_STAFF_SPECIAL)

            val victim = target
            val demon = SpecialAttackEffects.isDemon(victim)
            val magic = player.getSkills().getCurrentLevel(Skills.MAGIC)
            val base = (magic / 3.0).coerceAtLeast(1.0) * (if (demon) DEMON_MULTIPLIER else 1.0)
            val maxHit = (base * (1.0 + player.getMagicDamageBonus() / 100.0)).toInt()

            val landHit = MagicCombatFormula.getAccuracy(player, victim) >= world.randomDouble()
            val damage = if (landHit) world.random(maxHit) else 0

            player.dealMagicSpecialHit(target = victim, damage = damage, landHit = landHit)

            /*
             * A demon killed by the cast refunds what it cost and lets the next attack come
             * straight away - the reason the staff is worth carrying through a demon task.
             */
            if (demon && damage >= victim.getCurrentHp()) {
                AttackTab.setEnergy(player, (AttackTab.getEnergy(player) + REFUND).coerceAtMost(FULL_BAR))
                player.attr[Combat.INSTANT_NEXT_ATTACK] = true
            }
        }
    }

    private companion object {
        /** Every demonbane spell hits demons twice as hard. */
        const val DEMON_MULTIPLIER = 2.0

        /** The special's own cost, handed straight back. */
        const val REFUND = 25
        const val FULL_BAR = 100
    }
}
