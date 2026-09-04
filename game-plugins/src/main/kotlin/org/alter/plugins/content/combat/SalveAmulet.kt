package org.alter.plugins.content.combat

import org.alter.api.EquipmentType
import org.alter.api.NpcSpecies
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.isSpecies
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player

/**
 * The salve amulet's accuracy and damage bonus, and the condition all three combat formulas were
 * missing: that the target is undead.
 *
 * Every formula wrote its own copy of this as the first arm of a `when`, with no target check at
 * all, so a salve amulet was a permanent boost against literally everything - cows included. Worse,
 * because the arms fell through to `SlayerHeadgear` only when no salve was worn, wearing one
 * *disabled the black mask bonus entirely*. Non-stacking is real (the wiki: wearing both applies
 * only the salve's bonus), but it is meant to be conditional on the salve actually applying.
 *
 * The three copies had also drifted apart. Ranged granted the plain amulet 7/6 on the ranged roll,
 * which it has never given - the plain and enchanted amulets are melee-only. Magic granted the
 * plain amulet 7/6 on the accuracy roll while its own max-hit path, a hundred lines away, correctly
 * required an *imbued* amulet and an undead target. So the same attack could be rolled with one
 * rule and damaged with another.
 *
 * ## The bonuses
 *
 * Against undead only, and only from the versions that cover the style being used:
 *
 * | | Melee | Ranged | Magic |
 * |---|---|---|---|
 * | Salve amulet | 16.67% | - | - |
 * | Salve amulet (e) | 20% | - | - |
 * | Salve amulet (i) | 16.67% | 16.67% | 15% |
 * | Salve amulet (ei) | 20% | 20% | 20% |
 *
 * The bonus never applies against another player - "undead" is a monster species, and a player is
 * never one.
 */
object SalveAmulet {
    private val PLAIN = arrayOf("item.salve_amulet")

    private val ENCHANTED = arrayOf("item.salve_amulet_e")

    /*
     * The imbued amulets carry spare ids alongside the ones the shop and drop tables use - the
     * Nightmare Zone and Soul Wars copies. They are the same item and have to grant the same bonus,
     * which is exactly the sort of thing a hand-written `hasEquipped` check misses.
     */
    private val IMBUED =
        arrayOf(
            "item.salve_amuleti",
            "item.salve_amuleti_25250",
            "item.salve_amuleti_26763",
        )

    private val ENCHANTED_IMBUED =
        arrayOf(
            "item.salve_amuletei",
            "item.salve_amuletei_25278",
            "item.salve_amuletei_26782",
        )

    /**
     * Every amulet key this recognises, for the verify test.
     *
     * An rscm key that does not resolve is the quiet failure mode here: `hasEquipped` simply never
     * matches, and the amulet silently does nothing rather than raising anything.
     */
    internal val ALL_KEYS: List<String> get() = (PLAIN + ENCHANTED + IMBUED + ENCHANTED_IMBUED).toList()

    /** 20% from an enchanted amulet, 16.67% from a plain or imbued one. */
    fun meleeMultiplier(
        player: Player,
        target: Pawn?,
    ): Double =
        multiplier(
            player,
            target,
            enchanted = 1.2,
            plain = 7.0 / 6.0,
            imbued = 7.0 / 6.0,
            enchantedImbued = 1.2,
        )

    /** Imbued amulets only - the plain and enchanted ones do nothing for ranged. */
    fun rangedMultiplier(
        player: Player,
        target: Pawn?,
    ): Double =
        multiplier(
            player,
            target,
            enchanted = 1.0,
            plain = 1.0,
            imbued = 7.0 / 6.0,
            enchantedImbued = 1.2,
        )

    /** Imbued amulets only, and the plain imbued one gives 15% here rather than 16.67%. */
    fun magicMultiplier(
        player: Player,
        target: Pawn?,
    ): Double =
        multiplier(
            player,
            target,
            enchanted = 1.0,
            plain = 1.0,
            imbued = 1.15,
            enchantedImbued = 1.2,
        )

    /**
     * The more specific amulets are tested first: an id belonging to `(ei)` must not be matched by
     * the `(i)` arm, and both are checked ahead of the plain and enchanted ones.
     */
    private fun multiplier(
        player: Player,
        target: Pawn?,
        enchanted: Double,
        plain: Double,
        imbued: Double,
        enchantedImbued: Double,
    ): Double {
        if (target !is Npc || !target.isSpecies(NpcSpecies.UNDEAD)) {
            return 1.0
        }
        return when {
            player.hasEquipped(EquipmentType.AMULET, *ENCHANTED_IMBUED) -> enchantedImbued
            player.hasEquipped(EquipmentType.AMULET, *IMBUED) -> imbued
            player.hasEquipped(EquipmentType.AMULET, *ENCHANTED) -> enchanted
            player.hasEquipped(EquipmentType.AMULET, *PLAIN) -> plain
            else -> 1.0
        }
    }
}
