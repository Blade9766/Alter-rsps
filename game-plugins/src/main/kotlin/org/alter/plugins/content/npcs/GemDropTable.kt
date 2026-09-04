package org.alter.plugins.content.npcs

import org.alter.game.model.World
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
 * Weights are the wiki's numerators out of 128, used directly. **They do not actually sum to
 * 128**: the ten item rows plus the mega-rare row come to 68 and the `Nothing` row is
 * published as `rarity=63/128`, for 131. That is a discrepancy in the source - its own rows
 * disagree with its stated total - and since [DropRoll] treats numerators as relative weights
 * it rescales harmlessly rather than distorting any row against the others. An earlier version
 * of this file described the table as summing to 128; it never did.
 *
 * **The mega-rare row is real now.** It used to be folded into [NOTHING_WEIGHT] because
 * [RareDropTable.MEGA_RARE] did not exist to point at. It does, so the weight is back where it
 * belongs and [roll] resolves it. The Legends' Quest condition on this particular route is not
 * enforced - see [RareDropTable] for why.
 *
 * **The ring of wealth `altrarity` column is modelled**, as the [wealth] flag on [roll]: wearing one
 * removes the `Nothing` row outright and rescales the rest, which is what roughly doubles the chance
 * of a gem and multiplies the mega-rare route by about 16.8. It is passed down from whichever
 * monster plugin rolled the table, because it depends on the killer rather than on the table.
 *
 * The chaos and nature talismans are both included as published. In real OSRS which one
 * you get depends on whether you are above or below ground - "typically, a nature talisman
 * will drop above ground level and a chaos talisman will drop underground" - but that is a
 * per-tile rule with no hook here, and every monster wired to this table so far declares
 * both anyway.
 */
internal object GemDropTable {
    /** The published `Nothing` weight. */
    private const val NOTHING_WEIGHT = 63

    /** How often this table sends you on to [RareDropTable.MEGA_RARE]. */
    private const val MEGA_RARE_WEIGHT = 1

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

    private val TOTAL_WEIGHT = TABLE.sumOf { it.weight } + MEGA_RARE_WEIGHT

    /**
     * Roll the gem table, resolving its 1/128 step into the mega-rare table.
     *
     * Prefer this over `DropRoll.pick(TABLE, world)`: picking [TABLE] directly skips the mega-rare
     * row entirely, because that row names no item and so cannot live in the list.
     *
     * A `Nothing` outcome comes back as the row itself, with a null [WeightedDrop.item] - not as a
     * null roll - which is why every caller unwraps it with `picked.item?.let`. That happens on
     * roughly half of all rolls, or never when [wealth] says the killer was wearing a ring of wealth.
     */
    fun roll(
        world: World,
        wealth: Boolean = false,
    ): WeightedDrop? {
        /*
         * The mega-rare row's weight is not itself removed by the ring - only the `Nothing` rows
         * inside the tables are - so the 1/128 step keeps its odds against a table that has shrunk,
         * which is exactly why the ring makes the mega-rare route so much more likely.
         */
        val total = if (wealth) TABLE.sumOf { if (it.item == null) 0 else it.weight } + MEGA_RARE_WEIGHT else TOTAL_WEIGHT
        return if (world.random(total - 1) < MEGA_RARE_WEIGHT) {
            DropRoll.pick(RareDropTable.MEGA_RARE, world, skipNothing = wealth)
        } else {
            DropRoll.pick(TABLE, world, skipNothing = wealth)
        }
    }
}
