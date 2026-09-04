package org.alter.plugins.content.combat.specialattack.weapons.stackweapons

import org.alter.api.cfg.Animation
import org.alter.api.ext.getPrayerBonus
import org.alter.api.ext.heal
import org.alter.api.ext.message
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.SOUL_STACKS_ATTR
import org.alter.game.model.attr.SUNLIGHT_STACKS_ATTR
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.WEAPON_STACK_DECAY_TIMER
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.WeaponPassives
import org.alter.plugins.content.combat.specialattack.dealExactMeleeSpecialHit
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit
import org.alter.plugins.content.combat.strategy.ranged.RangedAoe

/**
 * The two weapons whose special is paid for by attacking rather than by the special attack bar: the
 * soulreaper axe's **Behead** and the sunlight spear's **Sol Slam**.
 *
 * Both build a stack per swing - landed or not - through [WeaponPassives], both lose one stack at a
 * time after fifty ticks without attacking, and both specials spend the lot. The bar cost the cache
 * gives each of them is a rounding artefact (0.1% and 0.7%, which this codebase's whole-percent bar
 * rounds up to 1%); the stacks are the real price.
 *
 * - **Behead**: 12% accuracy and 6% damage per stack consumed, with a damage floor that rises with
 *   them, and 8 hitpoints healed per stack. At five stacks that is +60% accuracy, +30% damage and
 *   40 healed.
 * - **Sol Slam**: needs all seven sunlight stacks, hits everything within three tiles, and scales
 *   3% per point of prayer bonus. Typeless in the real game, which here means it goes through
 *   protection prayers - [MeleeCombatFormula.getMaxHitPiercingPrayer].
 *
 * Stacks are not dropped when the weapon comes off. They cannot be spent by anything else - each
 * special reads only its own counter - and the decay timer clears them within thirty seconds.
 */
class StackWeaponsPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        WeaponPassives.registerByName("Behead") { player, _ -> gainStack(player, SOUL_STACKS_ATTR, MAX_SOUL_STACKS) }
        WeaponPassives.registerByName("Sol Slam") { player, _ -> gainStack(player, SUNLIGHT_STACKS_ATTR, MAX_SUNLIGHT_STACKS) }

        onTimer(WEAPON_STACK_DECAY_TIMER) {
            val pawn = ctx as? Pawn ?: return@onTimer
            val soul = pawn.attr[SOUL_STACKS_ATTR] ?: 0
            val sunlight = pawn.attr[SUNLIGHT_STACKS_ATTR] ?: 0
            if (soul > 0) {
                pawn.attr[SOUL_STACKS_ATTR] = soul - 1
            }
            if (sunlight > 0) {
                pawn.attr[SUNLIGHT_STACKS_ATTR] = sunlight - 1
            }
            if (soul > 1 || sunlight > 1) {
                pawn.timers[WEAPON_STACK_DECAY_TIMER] = DECAY_TICKS
            }
        }

        SpecialAttacks.registerByName("Behead") {
            val stacks = player.attr[SOUL_STACKS_ATTR] ?: 0
            if (stacks <= 0) {
                player.message("You have no soul stacks to consume.")
                return@registerByName
            }
            player.attr[SOUL_STACKS_ATTR] = 0

            player.animate(Animation.DRAGON_BATTLEAXE_SPECIAL)

            val accuracyMultiplier = 1.0 + stacks * ACCURACY_PER_SOUL
            val damageMultiplier = 1.0 + stacks * DAMAGE_PER_SOUL

            val maxHit = MeleeCombatFormula.getMaxHit(player, target, specialAttackMultiplier = damageMultiplier)
            val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = accuracyMultiplier)
            val landHit = accuracy >= world.randomDouble()

            // The floor rises with the stacks too - 6% of the max hit each, so a full spec cannot
            // roll below 30%.
            val floor = (maxHit * stacks * DAMAGE_PER_SOUL).toInt()
            val damage = if (landHit) world.random(floor..maxHit.coerceAtLeast(floor)) else 0

            player.dealExactMeleeSpecialHit(target = target, damage = damage, landHit = landHit)
            player.heal(stacks * HEAL_PER_SOUL)
        }

        SpecialAttacks.registerByName("Sol Slam") {
            val stacks = player.attr[SUNLIGHT_STACKS_ATTR] ?: 0
            if (stacks < MAX_SUNLIGHT_STACKS) {
                player.message("You need $MAX_SUNLIGHT_STACKS sunlight stacks to do that.")
                return@registerByName
            }
            player.attr[SUNLIGHT_STACKS_ATTR] = 0

            player.animate(Animation.HUMAN_SPECIAL_SPEAR_SWING)

            val prayerScaling = 1.0 + player.getPrayerBonus() * DAMAGE_PER_PRAYER_BONUS
            RangedAoe.targetsAround(player, target, radius = SLAM_RADIUS, max = MAX_TARGETS).forEach { victim ->
                val maxHit = MeleeCombatFormula.getMaxHitPiercingPrayer(player, victim, specialAttackMultiplier = prayerScaling)
                player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = true)
            }
        }
    }

    private fun gainStack(
        player: Player,
        attribute: org.alter.game.model.attr.AttributeKey<Int>,
        max: Int,
    ) {
        val current = player.attr[attribute] ?: 0
        if (current < max) {
            player.attr[attribute] = current + 1
        }
        player.timers[WEAPON_STACK_DECAY_TIMER] = DECAY_TICKS
    }

    private companion object {
        /** Fifty ticks, thirty seconds, without attacking before the first stack falls off. */
        const val DECAY_TICKS = 50

        const val MAX_SOUL_STACKS = 5
        const val ACCURACY_PER_SOUL = 0.12
        const val DAMAGE_PER_SOUL = 0.06
        const val HEAL_PER_SOUL = 8

        const val MAX_SUNLIGHT_STACKS = 7
        const val SLAM_RADIUS = 3
        const val MAX_TARGETS = 12
        const val DAMAGE_PER_PRAYER_BONUS = 0.03
    }
}
