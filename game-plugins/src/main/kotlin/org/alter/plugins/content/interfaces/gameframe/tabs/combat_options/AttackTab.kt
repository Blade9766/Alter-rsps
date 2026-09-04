package org.alter.plugins.content.interfaces.attack

import org.alter.api.ext.getVarp
import org.alter.api.ext.secondsToTicks
import org.alter.api.ext.setVarp
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey

/**
 * @author Tom <rspsmods@gmail.com>
 * @author Sequential - Special Attack Restore
 */
object AttackTab {
    const val ATTACK_TAB_INTERFACE_ID = 593
    const val ATTACK_STYLE_VARP = 43
    const val DISABLE_AUTO_RETALIATE_VARP = 172
    private const val SPECIAL_ATTACK_ENERGY_VARP = 300
    const val SPECIAL_ATTACK_VARP = 301

    /**
     * The special attack bar along the bottom of the Combat Options tab - the component
     * carrying the op "Use <col=00ff00>Special Attack</col>" in the cache. Not 36, which is
     * the "Toggle set effect" button next to Auto retaliate.
     */
    const val SPECIAL_ATTACK_BAR_COMPONENT = 38

    /** The special attack orb under the minimap, the tab bar's twin. */
    const val SPECIAL_ORB_INTERFACE_ID = 160
    const val SPECIAL_ORB_COMPONENT = 35

    val SPEC_RESTORE = TimerKey()

    fun setEnergy(
        p: Player,
        amount: Int,
    ) {
        check(amount in 0..100)
        p.setVarp(SPECIAL_ATTACK_ENERGY_VARP, amount * 10)
    }

    fun restoreEnergy(p: Player) {
        var newEnergy = p.getVarp(SPECIAL_ATTACK_ENERGY_VARP) + 100
        if (newEnergy > 1000) newEnergy = 1000
        p.setVarp(SPECIAL_ATTACK_ENERGY_VARP, newEnergy)
    }

    fun getEnergy(p: Player): Int = p.getVarp(SPECIAL_ATTACK_ENERGY_VARP) / 10

    fun disableSpecial(p: Player) {
        p.setVarp(SPECIAL_ATTACK_VARP, 0)
    }

    fun isSpecialEnabled(p: Player): Boolean = p.getVarp(SPECIAL_ATTACK_VARP) == 1

    fun resetRestorationTimer(player: Player) = player.timers.set(SPEC_RESTORE, 30.secondsToTicks())
}
