package org.alter.plugins.content.combat.strategy.ranged.ammo

import org.alter.rscm.RSCM.getRSCM

/**
 * Thrown axes, grouped the way [Knives] and [Darts] are.
 *
 * Unlike every other thrown weapon these have no poisoned forms - thrownaxes cannot be
 * poisoned in the real game - so each tier is a single id, apart from dragon which has
 * a second cache entry.
 */
object Thrownaxes {
    val THROWNAXES =
        arrayOf(
            getRSCM("item.bronze_thrownaxe"),
            getRSCM("item.iron_thrownaxe"),
            getRSCM("item.steel_thrownaxe"),
            getRSCM("item.mithril_thrownaxe"),
            getRSCM("item.adamant_thrownaxe"),
            getRSCM("item.rune_thrownaxe"),
            getRSCM("item.dragon_thrownaxe"),
            getRSCM("item.dragon_thrownaxe_21207"),
        )
}
