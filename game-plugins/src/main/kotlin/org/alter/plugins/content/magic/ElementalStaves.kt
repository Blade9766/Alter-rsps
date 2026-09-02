package org.alter.plugins.content.magic

import org.alter.api.EquipmentType
import org.alter.api.ext.hasEquipped
import org.alter.game.model.entity.Player
import org.alter.rscm.RSCM.getRSCM

/**
 * Elemental staves that provide unlimited amounts of one specific rune while equipped -
 * e.g. Staff of Air supplies unlimited air runes, so a spell needing air runes can be
 * cast without holding or consuming any. Consulted by [MagicSpells.canCast]/
 * [MagicSpells.removeRunes]. Real OSRS has one of these per basic element plus mystic/
 * combination variants; only Staff of Air is wired up since that's all that's been
 * asked for so far - adding another is just one more map entry.
 */
internal object ElementalStaves {
    private val RUNE_BY_STAFF: Map<String, String> =
        mapOf(
            "item.staff_of_air" to "item.air_rune",
        )

    fun providesUnlimited(
        player: Player,
        runeId: Int,
    ): Boolean = RUNE_BY_STAFF.any { (staff, rune) -> getRSCM(rune) == runeId && player.hasEquipped(EquipmentType.WEAPON, staff) }
}
