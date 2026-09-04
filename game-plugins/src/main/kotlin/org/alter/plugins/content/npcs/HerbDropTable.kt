package org.alter.plugins.content.npcs

import org.alter.rscm.RSCM.getRSCM

/**
 * The standard herb drop table - the second of the game's shared sub-tables, alongside
 * [GemDropTable].
 *
 * Monsters reference it the same way they reference the gem table: their own drop tables carry a
 * single "Herb drop table" row with a rate (`31/128` on a turoth, `78/128` on an aberrant spectre)
 * and never list the herbs themselves. So, like the gem table, it lives here beside [WeightedDrop]
 * and [DropRoll] rather than in any one monster's package - ten of the sixteen Slayer Tower and
 * Fremennik Slayer Dungeon monsters roll into it, and the existing dungeon and guard tables can be
 * pointed at it later without copying anything.
 *
 * Weights are the wiki's numerators out of 128 and they sum to exactly 128 with no `Nothing` row,
 * so - as with the gem table - the relative-weight approximation [DropRoll] makes is exact here
 * rather than a rescaling.
 *
 * **Grimy, not clean.** Every row is the unidentified herb, which is what monsters actually drop;
 * identifying them is Herblore's job.
 *
 * The **snapdragon, toadflax and torstol** rows some pages show belong to the *rare* seed and herb
 * tables, not this one, and are correctly absent: the published standard table stops at dwarf weed.
 */
internal object HerbDropTable {
    val TABLE: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.grimy_guam_leaf"), 1, weight = 32),
            WeightedDrop(getRSCM("item.grimy_marrentill"), 1, weight = 24),
            WeightedDrop(getRSCM("item.grimy_tarromin"), 1, weight = 18),
            WeightedDrop(getRSCM("item.grimy_harralander"), 1, weight = 14),
            WeightedDrop(getRSCM("item.grimy_ranarr_weed"), 1, weight = 11),
            WeightedDrop(getRSCM("item.grimy_irit_leaf"), 1, weight = 8),
            WeightedDrop(getRSCM("item.grimy_avantoe"), 1, weight = 6),
            WeightedDrop(getRSCM("item.grimy_kwuarm"), 1, weight = 5),
            WeightedDrop(getRSCM("item.grimy_cadantine"), 1, weight = 4),
            WeightedDrop(getRSCM("item.grimy_lantadyme"), 1, weight = 3),
            WeightedDrop(getRSCM("item.grimy_dwarf_weed"), 1, weight = 3),
        )
}
