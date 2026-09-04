package org.alter.plugins.content.combat.specialattack.weapons.kerispartisan

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.ext.secondsToTicks
import org.alter.api.ext.message
import org.alter.api.ext.sendRunEnergy
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.specialattack.SpecialAttackEffects
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMeleeSpecialHit
import org.alter.plugins.content.mechanics.poison.Poison

/**
 * The two keris partisans from the Tombs of Amascut.
 *
 * - **Wrath of Amascut** (of corruption): double accuracy and 25% more damage at half attack speed,
 *   and the target takes 25% more damage from everything for the next six seconds.
 * - **Tumeken's Light** (of the sun): fifty Prayer points spent to overheal to 20% above the
 *   wielder's maximum, cure poison, undo every drained stat and refill run energy. It hits nobody,
 *   so it is `executeInstantly` and fires from the bar without a target.
 *
 * The halved attack speed is [SpecialAttackEffects.slowNextAttack] with the weapon's own delay
 * added on, i.e. the next attack takes twice as long - the special cannot set the attack delay
 * itself because `Combat.postAttack` overwrites it immediately afterwards.
 *
 * Neither is gated to the Tombs, as the cache says they should be: there is no Tombs of Amascut in
 * this codebase to gate on.
 */
class KerisPartisanPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Wrath of Amascut") {
            player.animate(Animation.HUMAN_SPECIAL_SPEAR_STAB)

            val victim = target
            val maxHit = MeleeCombatFormula.getMaxHit(player, victim, specialAttackMultiplier = DAMAGE_MULTIPLIER)
            val accuracy = MeleeCombatFormula.getAccuracy(player, victim, specialAttackMultiplier = ACCURACY_MULTIPLIER)
            val landHit = accuracy >= world.randomDouble()

            SpecialAttackEffects.slowNextAttack(player, CombatConfigs.getAttackDelay(player))

            player.dealMeleeSpecialHit(target = victim, maxHit = maxHit, landHit = landHit).hit.addAction {
                if (landHit) {
                    SpecialAttackEffects.amplifyDamageTaken(
                        target = victim,
                        multiplier = VULNERABILITY,
                        ticks = VULNERABILITY_SECONDS.secondsToTicks(),
                    )
                }
            }
        }

        SpecialAttacks.registerByName("Tumeken's Light", executeInstantly = true) {
            val skills = player.getSkills()
            if (skills.getCurrentLevel(Skills.PRAYER) < PRAYER_COST) {
                player.message("You need $PRAYER_COST Prayer points to call on Tumeken's light.")
                return@registerByName
            }

            player.animate(Animation.HUMAN_SPECIAL_SPEAR_STAB)
            skills.alterCurrentLevel(skill = Skills.PRAYER, value = -PRAYER_COST)

            val overheal = skills.getBaseLevel(Skills.HITPOINTS) * OVERHEAL_PERCENT / 100
            skills.setCurrentLevel(Skills.HITPOINTS, skills.getBaseLevel(Skills.HITPOINTS) + overheal)

            // Every drained stat back to its base - Prayer excepted, which just paid for this.
            RESTORED.forEach { skill ->
                val base = skills.getBaseLevel(skill)
                if (skills.getCurrentLevel(skill) < base) {
                    skills.setCurrentLevel(skill, base)
                }
            }

            Poison.cure(player, immunityCycles = 0)
            player.runEnergy = FULL_RUN_ENERGY
            player.sendRunEnergy(FULL_RUN_ENERGY.toInt())
            player.message("Tumeken's light washes over you.")
        }
    }

    private companion object {
        const val ACCURACY_MULTIPLIER = 2.0
        const val DAMAGE_MULTIPLIER = 1.25
        const val VULNERABILITY = 1.25
        const val VULNERABILITY_SECONDS = 6

        const val PRAYER_COST = 50
        const val OVERHEAL_PERCENT = 20
        const val FULL_RUN_ENERGY = 10000.0

        val RESTORED =
            listOf(
                Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE, Skills.RANGED, Skills.MAGIC,
                Skills.AGILITY, Skills.MINING, Skills.WOODCUTTING, Skills.FISHING,
            )
    }
}
