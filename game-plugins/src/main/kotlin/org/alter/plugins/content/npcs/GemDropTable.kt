package org.alter.plugins.content.npcs

import org.alter.rscm.RSCM.getRSCM

/**
 * The gem drop table - the shared sub-table a large fraction of the game's monsters roll
 * into, published in full on the wiki's Rare drop table page.
 *
 * Monsters do not list its contents themselves; each one just says how often it rolls the
 * table, with `{{GemDropTable|3/128}}` or similar. So this lives here, beside
 * [WeightedDrop] and [DropRoll], rather than in any one monster's package: eleven of the
 * dungeon monsters alone reach it, at rates from 1/128 to 5/128, and the guards and White
 * Knights can be pointed at it later without copying anything.
 *
 * Weights are the wiki's numerators out of 128, used directly - they already sum to 128
 * here, including the `Nothing` row, so this is one of the few tables in this codebase
 * where the relative-weight approximation is exact rather than a rescaling.
 *
 * **Two rows are deliberately absent:**
 * - **Mega-rare drop table** (1/128) - another shared sub-table, and one the wiki notes is
 *   "replaced by a talisman if Legends' Quest has not been completed". Modelling it would
 *   mean modelling that table *and* a quest gate. Its weight is folded into [NOTHING_WEIGHT]
 *   so the table still sums correctly rather than silently inflating every other row.
 * - The **ring of wealth** `altrarity` column, which removes the Nothing row and rescales
 *   everything to /65. There is no ring of wealth behaviour in this codebase to hang it on.
 *
 * The chaos and nature talismans are both included as published. In real OSRS which one
 * you get depends on whether you are above or below ground - "typically, a nature talisman
 * will drop above ground level and a chaos talisman will drop underground" - but that is a
 * per-tile rule with no hook here, and every monster wired to this table so far declares
 * both anyway.
 */
internal object GemDropTable {
    /** The published `Nothing` weight (63) plus the unmodelled mega-rare row (1). */
    private const val NOTHING_WEIGHT = 64

    val TABLE: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.uncut_sapphire"), 1, weight = 32),
            WeightedDrop(getRSCM("item.uncut_emerald"), 1, weight = 16),
            WeightedDrop(getRSCM("item.uncut_ruby"), 1, weight = 8),
            WeightedDrop(getRSCM("item.chaos_talisman"), 1, weight = 3),
            WeightedDrop(getRSCM("item.nature_talisman"), 1, weight = 3),
            WeightedDrop(getRSCM("item.uncut_diamond"), 1, weight = 2),
            WeightedDrop(getRSCM("item.rune_javelin"), 5, weight = 1),
            WeightedDrop(getRSCM("item.loop_half_of_key"), 1, weight = 1),
            WeightedDrop(getRSCM("item.tooth_half_of_key"), 1, weight = 1),
            WeightedDrop(item = null, weight = NOTHING_WEIGHT),
        )
}
