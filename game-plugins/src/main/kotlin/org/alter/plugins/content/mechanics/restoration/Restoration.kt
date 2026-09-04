package org.alter.plugins.content.mechanics.restoration

import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.ext.hasEquipped
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey
import org.alter.plugins.content.items.consumables.potions.Divine
import org.alter.plugins.content.mechanics.prayer.Prayer
import org.alter.plugins.content.mechanics.prayer.Prayers

/**
 * Natural restoration of hitpoints and of stats that have been boosted or drained.
 *
 * A boost decays a level a cycle, with one exception: [Divine] holds a stat at the level a divine
 * potion set it to for as long as that potion runs.
 *
 * Nothing recovered these on its own before: damage stayed on a player until they ate, and a boost
 * from a potion or a piece of food never wore off. Both run on their own cycle so that a hitpoint
 * regeneration item cannot also accelerate the decay of a boost.
 *
 * Prayer points are deliberately left out of both cycles - they only come back from altars and from
 * prayer restoring items.
 */
object Restoration {
    val HITPOINT_REGEN = TimerKey()
    val STAT_RESTORE = TimerKey()

    /**
     * One restoration cycle, in ticks. A minute of game time.
     */
    const val RESTORE_INTERVAL = 100

    /**
     * A regen bracelet halves the wait between hitpoint restores.
     */
    const val BRACELET_REGEN_INTERVAL = 50

    fun hitpointInterval(p: Player): Int =
        if (p.hasEquipped(EquipmentType.GLOVES, "item.regen_bracelet")) BRACELET_REGEN_INTERVAL else RESTORE_INTERVAL

    /**
     * Recovers hitpoints toward the player's real level. Boosted hitpoints are left to
     * [restoreStats] so that a regen bracelet does not also burn through an overheal faster.
     */
    fun regenHitpoints(p: Player) {
        if (p.isDead()) {
            return
        }

        val skills = p.getSkills()
        val current = skills.getCurrentLevel(Skills.HITPOINTS)
        val base = skills.getBaseLevel(Skills.HITPOINTS)
        if (current >= base) {
            return
        }

        // Rapid Heal recovers a second hitpoint each cycle.
        val amount = if (Prayers.isActive(p, Prayer.RAPID_HEAL)) 2 else 1
        skills.alterCurrentLevel(Skills.HITPOINTS, amount)
    }

    /**
     * Steps every stat one level back toward its real level: boosts decay, drains recover. Hitpoints
     * take part in the decay direction only.
     */
    fun restoreStats(p: Player) {
        if (p.isDead()) {
            return
        }

        val skills = p.getSkills()
        val rapidRestore = Prayers.isActive(p, Prayer.RAPID_RESTORE)

        for (skill in 0 until skills.maxSkills) {
            if (skill == Skills.PRAYER) {
                continue
            }

            val current = skills.getCurrentLevel(skill)
            val base = skills.getBaseLevel(skill)

            when {
                // A boost always decays a level per cycle, Rapid Restore or not - unless a divine
                // potion is holding this stat up, in which case it decays only down to that floor.
                current > base && current > Divine.floorOf(p, skill) ->
                    skills.setCurrentLevel(skill, current - 1)
                current < base && skill != Skills.HITPOINTS -> {
                    // Rapid Restore recovers a second level of a drained stat each cycle.
                    val amount = if (rapidRestore) 2 else 1
                    skills.setCurrentLevel(skill, Math.min(base, current + amount))
                }
            }
        }
    }
}
