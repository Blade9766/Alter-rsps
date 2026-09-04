package org.alter.plugins.content.npcs

import org.alter.game.model.World
import org.alter.rscm.RSCM.getRSCM

/**
 * The rare drop table - the shared sub-table the game's mid and high level monsters reach for their
 * best non-unique drops, and the last of the four shared tables alongside [GemDropTable],
 * [HerbDropTable] and [SeedDropTable].
 *
 * ## It is three tables, not one
 *
 * The published structure nests, and modelling it flat would be wrong:
 *
 * ```
 * rare drop table (128)
 *   ├─ 93  its own item rows
 *   ├─ 20  -> gem drop table  (GemDropTable, 128 slots)
 *   │            └─ 1 -> mega-rare drop table
 *   └─ 15  -> mega-rare drop table (128 slots, 113 of them Nothing)
 * ```
 *
 * So a monster on the rare drop table reaches the mega-rare table two ways - directly at 15/128, and
 * again through the 20/128 gem row's own 1/128 - and a monster on the *gem* table only ever reaches
 * it the second way. [roll] resolves that nesting; [GemDropTable.roll] resolves its half.
 *
 * ## Which table a monster is on
 *
 * The two are separate templates on the wiki and mean different things.
 * `{{GemDropTable|5/128}}` is the gem table alone. `{{RareDropTable|2/128|5/128}}` is *both*: the
 * first parameter is the rare drop table access rate and the second the gem table's. Of the sixteen
 * Slayer monsters only the abyssal demon (2/128) and the nechryael (1/116) reach the primary table;
 * every other one that rolls anything here is `{{GemDropTable}}` only, gargoyles and kurasks
 * included.
 *
 * ## Two honest notes on the numbers
 *
 * - **The item rows sum to exactly 128** with the two sub-table rows included (93 + 20 + 15), which
 *   is the check that the transcription is right. The wiki's own per-section subtotals do *not*
 *   agree with its rows; the rows were taken as the source.
 * - **[GemDropTable]'s rows sum to 131, not the 128 it claims.** Its `Nothing` row is published as
 *   `rarity=63/128` while its ten item rows and mega-rare row come to 68. That discrepancy is in the
 *   source, not in the transcription, and since [DropRoll] treats numerators as relative weights it
 *   rescales harmlessly rather than distorting any one row against the others.
 *
 * **The Legends' Quest gate is not enforced.** In the real game the gem table's mega-rare row is
 * replaced by a talisman until Legends' Quest is complete - but only when reached *that* way; the
 * primary table's own 15/128 has never needed the quest. There is no quest framework here, so the
 * gate is treated as passed, which is the same call `content/areas/edgeville/npcs/stores` documents
 * for Oziach and the reading that leaves content reachable rather than permanently closed.
 */
internal object RareDropTable {
    private fun drop(
        item: String,
        min: Int = 1,
        max: Int = min,
        weight: Int,
    ) = WeightedDrop(getRSCM(item), min, max, weight)

    /** How often the primary table sends you to the gem table. */
    private const val GEM_WEIGHT = 20

    /** How often the primary table sends you straight to the mega-rare table. */
    private const val MEGA_RARE_WEIGHT = 15

    /**
     * The mega-rare drop table. 113 of its 128 slots are `Nothing`, which is the whole character of
     * the thing: reaching it is not the same as getting something from it.
     *
     * Also reached from [GemDropTable.roll] at its own 1/128.
     */
    val MEGA_RARE: List<WeightedDrop> =
        listOf(
            WeightedDrop(item = null, weight = 113),
            drop("item.rune_spear", weight = 8),
            drop("item.shield_left_half", weight = 4),
            drop("item.dragon_spear", weight = 3),
        )

    /**
     * The primary table's own item rows - 93 of its 128 slots. The other 35 are the two sub-table
     * rows, which are not rows in this list because they do not name an item; [roll] holds them.
     *
     * Noted quantities are dropped unnoted, as everywhere else here: there is no note-on-drop
     * mechanic, so the silver ore row is 100 ore.
     */
    private val ITEMS: List<WeightedDrop> =
        listOf(
            WeightedDrop(getRSCM("item.coins_995"), 3000, weight = 21),
            drop("item.loop_half_of_key", weight = 20),
            drop("item.tooth_half_of_key", weight = 20),
            drop("item.runite_bar", weight = 5),
            drop("item.nature_rune", 67, weight = 3),
            drop("item.rune_2h_sword", weight = 3),
            drop("item.rune_battleaxe", weight = 3),
            drop("item.adamant_javelin", 20, weight = 2),
            drop("item.death_rune", 45, weight = 2),
            drop("item.law_rune", 45, weight = 2),
            drop("item.rune_arrow", 42, weight = 2),
            drop("item.steel_arrow", 150, weight = 2),
            drop("item.rune_sq_shield", weight = 2),
            drop("item.dragonstone", weight = 2),
            drop("item.silver_ore", 100, weight = 2),
            drop("item.dragon_med_helm", weight = 1),
            drop("item.rune_kiteshield", weight = 1),
        )

    private val TOTAL_WEIGHT = ITEMS.sumOf { it.weight } + GEM_WEIGHT + MEGA_RARE_WEIGHT

    /**
     * Roll the rare drop table, resolving whichever sub-table the roll lands in.
     *
     * A `Nothing` outcome comes back as the row itself, with a null [WeightedDrop.item] - not as a
     * null roll - which is why every caller unwraps it with `picked.item?.let`. That is most of the
     * time.
     *
     * [wealth] is whether the killer was wearing a ring of wealth. It removes the `Nothing` rows from
     * the gem and mega-rare tables - the two sub-tables that have them - and so multiplies the
     * mega-rare route's odds by about 8.5 from here. This table's *own* 93 item rows have no
     * `Nothing` row to remove, which is why they are picked without the flag.
     */
    fun roll(
        world: World,
        wealth: Boolean = false,
    ): WeightedDrop? {
        val slot = world.random(TOTAL_WEIGHT - 1)
        return when {
            slot < GEM_WEIGHT -> GemDropTable.roll(world, wealth)
            slot < GEM_WEIGHT + MEGA_RARE_WEIGHT -> DropRoll.pick(MEGA_RARE, world, skipNothing = wealth)
            else -> DropRoll.pick(ITEMS, world)
        }
    }

    /**
     * Touch every table so their rscm keys resolve at start-up rather than inside a death handler.
     * See [SeedDropTable.warmUp] for why.
     */
    fun warmUp(): Int = ITEMS.size + MEGA_RARE.size
}
