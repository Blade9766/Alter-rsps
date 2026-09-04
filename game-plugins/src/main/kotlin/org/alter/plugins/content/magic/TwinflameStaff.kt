package org.alter.plugins.content.magic

import org.alter.api.EquipmentType
import org.alter.api.Spellbook
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.hasSpellbook
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.plugins.content.combat.strategy.magic.SpellGroup
import org.alter.plugins.content.combat.strategy.magic.groupOf

/**
 * The Twinflame staff: 60 Magic to wield, and more than a rune source.
 *
 * Its rune supply - unlimited fire *and* water - is declared with every other staff in
 * [ElementalStaves]; what lives here is the rest of what the staff does:
 *
 *  - **[standardSpellbookBonus]**: +10% accuracy and +10% damage on Standard spellbook spells.
 *    Applied by `MagicCombatFormula` on both the attack roll and the max hit, the same way the
 *    mystic smoke staff's +10% already is - multiplicative on the accuracy roll, and added to
 *    the damage multiplier alongside the magic damage bonus, which is how OSRS stacks the
 *    percentage damage boosts.
 *  - **[secondCastDamage]**: elemental Bolt, Blast and Wave spells are cast twice, the second
 *    landing a tick after the first for 40% of the first hit's damage. Strike and Surge spells
 *    are excluded, as are the non-elemental spells, which have no [SpellGroup] at all.
 *
 * ## Not implemented
 *
 * The staff's third passive - automatically substituting the elemental spell an NPC is weak to
 * for the one the player selected - is not here. That is not a modifier applied to a cast, it is
 * a change to *which spell is cast*, and `Combat.CASTING_SPELL` is the autocast selection itself:
 * rewriting it mid-attack fights `Combat`'s own check that the attribute still matches the
 * autocast varbit. It needs the strategy to carry an effective-spell separate from the selected
 * one, which is a change to the cast pipeline rather than to this item.
 *
 * ## Availability
 *
 * The staff is item 30634 in this cache, but the Royal Titans that drop the two element staff
 * crowns it is built from are not in the game, so nothing spawns it and there is no way to
 * assemble one. It is reachable through `::item` and the cheat menu only.
 */
object TwinflameStaff {
    const val ITEM = "item.twinflame_staff"

    /** +10% accuracy and damage, per the wiki. */
    private const val STANDARD_SPELLBOOK_BONUS = 0.1

    /** The second cast deals 40% of what the first one dealt. */
    private const val SECOND_CAST_DAMAGE_SHARE = 0.4

    /** Ticks between the two hits. */
    const val SECOND_CAST_DELAY = 1

    /**
     * The spell groups that fire twice. Strike and Surge are excluded, and so is everything with
     * no group - Ancient Magicks, God spells, the crumble/curse spells.
     */
    private val DOUBLE_CAST_GROUPS = setOf(SpellGroup.BOLT, SpellGroup.BLAST, SpellGroup.WAVE)

    fun isWielded(pawn: Pawn): Boolean = pawn is Player && pawn.hasEquipped(EquipmentType.WEAPON, ITEM)

    /**
     * The fractional accuracy and damage bonus [pawn] gets from the staff, or 0.0 when it does
     * not apply. Standard spellbook only, so an Ancient or Arceuus cast gets nothing.
     */
    fun standardSpellbookBonus(pawn: Pawn): Double {
        if (pawn !is Player || !isWielded(pawn) || !pawn.hasSpellbook(Spellbook.NORMAL)) {
            return 0.0
        }
        return STANDARD_SPELLBOOK_BONUS
    }

    /**
     * Damage for the second cast, given what the first one dealt, or 0 when the staff, the spell
     * or the first hit means there is no second cast to make.
     *
     * A first hit that dealt nothing produces no second hitsplat rather than a second 0: 40% of
     * nothing is nothing either way, and two block splats per cast reads as a bug.
     */
    fun secondCastDamage(
        pawn: Pawn,
        spell: CombatSpell,
        firstHitDamage: Int,
    ): Int {
        if (firstHitDamage <= 0 || !isWielded(pawn)) {
            return 0
        }
        if (groupOf(spell) !in DOUBLE_CAST_GROUPS) {
            return 0
        }
        return Math.floor(firstHitDamage * SECOND_CAST_DAMAGE_SHARE).toInt()
    }
}
