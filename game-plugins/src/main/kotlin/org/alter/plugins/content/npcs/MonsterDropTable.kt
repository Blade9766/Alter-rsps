package org.alter.plugins.content.npcs

import org.alter.game.model.World

/** Which of [SeedDropTable]'s published tables a monster's `Seeds` section names. */
internal enum class SeedTableId {
    /** `{{GeneralSeedDropTableInfo}}` - six tiers, and the monster's own combat level picks one. */
    GENERAL,

    /** `{{UncommonSeedDropTableInfo}}` - a flat list. */
    UNCOMMON,

    /** `{{RareSeedDropTableInfo}}` - the herb-seed list. The dagannoth is the first table to reach it. */
    RARE,
}

/**
 * One monster's main drop table, with the shared sub-tables carried as *rows* rather than as
 * separate rolls.
 *
 * ## Why this exists rather than another per-package copy
 *
 * `content/npcs/zombie` worked out the reading this class generalises: when a wiki drop table's
 * numerators sum to its stated denominator **including** its `{{HerbDropTableInfo|n/128}}`,
 * `{{...SeedDropTableInfo|n/128}}` and `{{GemDropTable|n/128}}` lines, those lines are rows
 * competing with everything else on one roll - not extra independent chances. Rolling them
 * separately, the way `content/npcs/slayer` and `content/npcs/dungeon` correctly do for *their*
 * monsters, would inflate every other row on these tables by a quarter or more.
 *
 * Every table built on this class was checked to sum exactly to [denominator] before being wired,
 * and `BestiaryVerify` re-checks all of them rather than trusting the arithmetic. That exactness is
 * what makes [DropRoll]'s relative-weight treatment precise here instead of a rescaling.
 *
 * ## The members reading
 *
 * Where a coin row publishes both a free-to-play `rarity` and a members `altrarity`, or is marked
 * `{{(f)}}` free-to-play-only, the members column is the one used - and on all five tables built on
 * this class it is the members column that reaches the denominator exactly while the free-to-play
 * one over-sums. That makes it a fact about the source rather than a house preference, the same way
 * it was for the zombie tables.
 */
internal class MonsterDropTable(
    /** The wiki's published denominator - 128 on every table so far except the level 130 bandit. */
    val denominator: Int,
    /** The ordinary item rows, `null` item being the page's `Nothing` line. */
    val rows: List<WeightedDrop>,
    /** The `HerbDropTableInfo` numerator, or 0 where the page has no herb row. */
    val herbWeight: Int = 0,
    /** The seed-table numerator, or 0 where the page has no seed row. */
    val seedWeight: Int = 0,
    /** Which seed table that numerator reaches. Required when [seedWeight] is non-zero. */
    val seedTable: SeedTableId? = null,
    /**
     * The monster's own combat level, which [SeedDropTable.rollGeneral] needs to pick a tier.
     * Only read for [SeedTableId.GENERAL].
     */
    val combatLevel: Int = 0,
    /** The `GemDropTable` numerator, or 0 where the page has no gem row. */
    val gemWeight: Int = 0,
    /**
     * The `RareDropTable` numerator, or 0 where the page has no rare-table row.
     *
     * A page that reaches the rare table writes both numerators in one template -
     * `{{RareDropTable|2/128|3/128}}` is "2/128 to the rare table, 3/128 to the gem table" - so this
     * and [gemWeight] are two rows of one section rather than alternatives. The bronze and black
     * dragons are the first tables in this tree to reach it; every earlier one stopped at the gem
     * table, which is why this parameter did not exist until they did.
     */
    val rareWeight: Int = 0,
    /**
     * How many herbs one herb-row result hands out, or null for the one-herb reading every table but
     * the Elder Chaos druid's uses.
     *
     * The `{{HerbDropTableInfo}}` template usually means "roll the herb table once". The Elder Chaos
     * druid's carries an `override` that spells out a distribution instead - one herb 15/55 of the
     * time, two 20/55, three 15/55 and four 5/55 - and that is a real part of why the temple is worth
     * visiting, so it is expressed rather than flattened. `content/npcs/chaosdruid` makes the same
     * point about its own one-or-two split, which it rolls in its plugin because this class did not
     * yet have anywhere to put it.
     */
    val herbRolls: ((World) -> Int)? = null,
) {
    init {
        require(seedWeight == 0 || seedTable != null) { "a seed weight needs a seed table" }
        require(seedTable != SeedTableId.GENERAL || combatLevel > 0) {
            "the general seed table needs the monster's combat level"
        }
    }

    /** What the rows and sub-table rows actually come to, for [BestiaryDropTables] to assert on. */
    val total: Int get() = rows.sumOf { it.weight } + herbWeight + seedWeight + gemWeight + rareWeight

    /**
     * One roll on this table, as item id to amount, or null for a `Nothing` row or a `Nothing`
     * result out of the gem table.
     *
     * A single d[denominator] walks herb, seeds, gem and then the ordinary rows, so all four stay
     * mutually exclusive exactly as they are on the page.
     *
     * [wealth] reaches only the gem row. The ring of wealth "does not remove any 'nothing' drops that
     * are specific to a monster's own drop table", so [rows] is picked without it even when the
     * killer is wearing one.
     */
    fun roll(
        world: World,
        wealth: Boolean = false,
    ): Pair<Int, Int>? = rollAll(world, wealth).firstOrNull()

    /**
     * One roll on this table as a *list*, which is the same single item [roll] returns for every
     * table but one.
     *
     * The exception is [herbRolls]: the Elder Chaos druid's herb row hands out one, two, three or
     * four herbs on a published distribution, so its herb result is genuinely several drops rather
     * than one. Every other table leaves [herbRolls] null and this returns at most one entry.
     */
    fun rollAll(
        world: World,
        wealth: Boolean = false,
    ): List<Pair<Int, Int>> {
        var roll = world.randomDouble() * denominator

        if (roll < herbWeight) {
            val count = herbRolls?.invoke(world) ?: 1
            return (0 until count).mapNotNull { DropRoll.pick(HerbDropTable.TABLE, world)?.resolve(world) }
        }
        roll -= herbWeight

        if (roll < seedWeight) {
            return when (seedTable) {
                SeedTableId.GENERAL -> SeedDropTable.rollGeneral(combatLevel, world)
                SeedTableId.UNCOMMON -> DropRoll.pick(SeedDropTable.UNCOMMON, world)
                SeedTableId.RARE -> DropRoll.pick(SeedDropTable.RARE, world)
                null -> null
            }?.resolve(world).asList()
        }
        roll -= seedWeight

        if (roll < gemWeight) {
            // GemDropTable.roll, not DropRoll.pick(GemDropTable.TABLE): picking the list directly
            // skips its 1/128 step into the mega-rare table, which names no item and so cannot be
            // a row in it.
            return GemDropTable.roll(world, wealth)?.resolve(world).asList()
        }
        roll -= gemWeight

        if (roll < rareWeight) {
            // Likewise RareDropTable.roll rather than a list pick: the primary table's own route
            // into the gem and mega-rare tables is inside that function, not in a row.
            return RareDropTable.roll(world, wealth)?.resolve(world).asList()
        }

        return DropRoll.pick(rows, world)?.resolve(world).asList()
    }

    private fun WeightedDrop.resolve(world: World): Pair<Int, Int>? = item?.let { it to DropRoll.amount(this, world) }

    /** A `Nothing` row is an empty list rather than a list holding null. */
    private fun Pair<Int, Int>?.asList(): List<Pair<Int, Int>> = if (this == null) emptyList() else listOf(this)
}
