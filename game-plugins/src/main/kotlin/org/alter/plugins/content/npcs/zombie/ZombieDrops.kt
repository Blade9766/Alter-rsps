package org.alter.plugins.content.npcs.zombie

import org.alter.game.model.World
import org.alter.plugins.content.npcs.DropRoll
import org.alter.plugins.content.npcs.GemDropTable
import org.alter.plugins.content.npcs.HerbDropTable
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The three zombie drop tables, and the roll that resolves one.
 *
 * ## The members reading, and why it is not a preference here
 *
 * Every coin row on both pages publishes two rates - a free-to-play `rarity` and a members
 * `altrarity` - and the two other monster packages that met this, `content/npcs/goblin` and
 * `content/npcs/guard`, chose members on the general grounds that this server already runs members
 * content. Here the choice is not a judgement call: **only the members numbers add up.**
 *
 * - Level 13 with the f2p `36/128` coin row sums to **153/128**; with the members `11/128` it sums
 *   to exactly **128**.
 * - Level 24 with `40/128` sums to 131; with `10/128`, exactly 128.
 * - Wilderness with `19/128` sums to 141; with `6/128`, exactly 128.
 *
 * Three independent tables landing on 128 to the unit is not a coincidence, so the members column
 * is the internally consistent set and the f2p column is the one with the published inflation. The
 * practical consequence is that [DropRoll]'s relative-weight treatment is *exact* on these tables
 * rather than a rescaling - unlike the goblin and guard tables, where sub-tables that could not be
 * modelled left the numerators short of 128.
 *
 * The same reading picks **dark fishing bait** over fishing bait on the Wilderness table, where the
 * wiki says one "is replaced by" the other on members worlds. The mainland tables carry no such
 * note and keep ordinary fishing bait.
 *
 * ## The herb and gem rows are rows, not extra rolls
 *
 * `content/npcs/slayer` and `content/npcs/dungeon` roll the shared herb and gem tables as
 * *independent* chances alongside the main table, which is right for their monsters. It would be
 * wrong here: because these tables sum to exactly 128 *including* the `HerbDropTableInfo|25/128`
 * and `GemDropTable|1/128` lines, those lines are rows competing with everything else, and rolling
 * them separately would inflate every other row by a quarter. So [ZombieTable] carries them as
 * weights and [roll] resolves one d128 across herb, gem and the ordinary rows in that order.
 *
 * ## What is not modelled
 *
 * - **Zombie bone, 1/4.** "Only dropped during Rag and Bone Man II" - a quest that does not exist
 *   on this server. A 1/4 drop is far too common to hand out unconditionally just because the
 *   condition is unbuilt, so it drops never rather than always.
 * - **The Wilderness Slayer tertiary table.** `{{WildernessSlayerDropTable}}` needs Krystilia's
 *   larder and the Wilderness emblem chain, none of which is built.
 * - **The ring of wealth column** on the gem table, which [GemDropTable] already documents as
 *   absent for want of any ring-of-wealth behaviour to hang it on.
 *
 * The **Zombie champion scroll** is kept at its real 1/5000 even though there is no Champions'
 * Challenge to hand it in to - the same call `content/npcs/goblin` makes about the goblin scroll,
 * and for the same reason: faking a rarity to hide a missing system is worse than dropping an item
 * that currently only sits in a bank.
 */
internal class ZombieTable(
    /**
     * The ordinary item rows. Their weights sum to `128 - herbWeight - gemWeight`, which
     * `ZombieVerify` asserts rather than trusting.
     */
    val rows: List<WeightedDrop>,
    /** The `HerbDropTableInfo` numerator, or 0 where the table has no herb row. */
    val herbWeight: Int = 0,
    /** The `GemDropTable` numerator, or 0 where the table has no gem row. */
    val gemWeight: Int = 0,
)

internal object ZombieDrops {
    /** Every one of these tables is published out of 128, and all three reach it exactly. */
    const val TABLE_SIZE = 128

    /** Wiki tertiary, on all five variants. */
    const val CHAMPION_SCROLL_ONE_IN = 5000

    /**
     * `Only dropped by those found in the Wilderness`, 1/3.
     *
     * Gated on where the *killer* is standing rather than on which variant died, matching
     * `content/npcs/slayer` and `content/npcs/critters`. That is both the wiki's own wording and
     * the more robust reading: a Wilderness zombie hand-spawned into Lumbridge should not print
     * looting bags there.
     */
    const val LOOTING_BAG_ONE_IN = 3

    /**
     * `Drops (level 13)`. Rows 103, herb 25 - 128.
     *
     * The 37/128 fishing bait row is the single commonest outcome on any zombie table, which is
     * exactly what the page shows: a level 13 zombie is a bait dispenser with a small chance of a
     * bronze med helm.
     */
    val LEVEL_13 =
        ZombieTable(
            herbWeight = 25,
            rows =
                listOf(
                    // Weapons and armour.
                    WeightedDrop(getRSCM("item.bronze_med_helm"), 1, weight = 4),
                    WeightedDrop(getRSCM("item.bronze_longsword"), 1, weight = 1),
                    WeightedDrop(getRSCM("item.iron_axe"), 1, weight = 1),
                    // Runes and ammunition.
                    WeightedDrop(getRSCM("item.iron_arrow"), 5, weight = 7),
                    WeightedDrop(getRSCM("item.body_rune"), 6, weight = 5),
                    WeightedDrop(getRSCM("item.mind_rune"), 5, weight = 5),
                    WeightedDrop(getRSCM("item.air_rune"), 13, weight = 4),
                    WeightedDrop(getRSCM("item.iron_arrow"), 8, weight = 4),
                    WeightedDrop(getRSCM("item.steel_arrow"), 5, weight = 2),
                    WeightedDrop(getRSCM("item.nature_rune"), 6, weight = 1),
                    // Coins. The 10-coin row is the members 11/128 - see the file doc.
                    WeightedDrop(getRSCM("item.coins_995"), 10, weight = 11),
                    WeightedDrop(getRSCM("item.coins_995"), 4, weight = 4),
                    WeightedDrop(getRSCM("item.coins_995"), 18, weight = 3),
                    WeightedDrop(getRSCM("item.coins_995"), 13, weight = 2),
                    WeightedDrop(getRSCM("item.coins_995"), 28, weight = 2),
                    // Other.
                    WeightedDrop(getRSCM("item.fishing_bait"), 5, weight = 37),
                    WeightedDrop(item = null, weight = 8),
                    WeightedDrop(getRSCM("item.copper_ore"), 1, weight = 2),
                ),
        )

    /**
     * `Drops (level 24)`. Rows 97, herb 30, gem 1 - 128.
     *
     * The only mainland zombie that reaches the shared gem table, and through it - at 1/128 of that
     * 1/128 - the mega-rare table. See [GemDropTable.roll], which resolves that step; picking
     * `GemDropTable.TABLE` directly would silently drop it.
     */
    val LEVEL_24 =
        ZombieTable(
            herbWeight = 30,
            gemWeight = 1,
            rows =
                listOf(
                    // Weapons and armour.
                    WeightedDrop(getRSCM("item.iron_mace"), 1, weight = 3),
                    WeightedDrop(getRSCM("item.iron_dagger"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.bronze_kiteshield"), 1, weight = 1),
                    // Runes and ammunition.
                    WeightedDrop(getRSCM("item.mithril_arrow"), 1, weight = 3),
                    WeightedDrop(getRSCM("item.air_rune"), 3, weight = 3),
                    WeightedDrop(getRSCM("item.body_rune"), 3, weight = 2),
                    WeightedDrop(getRSCM("item.chaos_rune"), 4, weight = 1),
                    WeightedDrop(getRSCM("item.cosmic_rune"), 2, weight = 1),
                    WeightedDrop(getRSCM("item.fire_rune"), 7, weight = 1),
                    // Coins. The 10-coin row is the members 10/128.
                    WeightedDrop(getRSCM("item.coins_995"), 10, weight = 10),
                    WeightedDrop(getRSCM("item.coins_995"), 18, weight = 21),
                    WeightedDrop(getRSCM("item.coins_995"), 26, weight = 8),
                    WeightedDrop(getRSCM("item.coins_995"), 35, weight = 6),
                    WeightedDrop(getRSCM("item.coins_995"), 1, weight = 2),
                    // Other.
                    WeightedDrop(getRSCM("item.fishing_bait"), 7, weight = 26),
                    WeightedDrop(item = null, weight = 3),
                    WeightedDrop(getRSCM("item.tinderbox"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.eye_of_newt"), 1, weight = 1),
                    WeightedDrop(getRSCM("item.tin_ore"), 1, weight = 1),
                ),
        )

    /**
     * `Zombie (Wilderness)` - one table shared by its level 18 and level 24 versions. Rows 114,
     * herb 13, gem 1 - 128.
     *
     * Better runes than either mainland table and, at 70/128 across three rows, mostly dark fishing
     * bait. Note it has **no `Nothing` row at all**, which the mainland tables both do.
     */
    val WILDERNESS =
        ZombieTable(
            herbWeight = 13,
            gemWeight = 1,
            rows =
                listOf(
                    // Weapons and armour.
                    WeightedDrop(getRSCM("item.bronze_kiteshield"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.iron_dagger"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.iron_mace"), 1, weight = 1),
                    // Runes.
                    WeightedDrop(getRSCM("item.cosmic_rune"), 2, weight = 6),
                    WeightedDrop(getRSCM("item.chaos_rune"), 4, weight = 5),
                    WeightedDrop(getRSCM("item.air_rune"), 3, weight = 3),
                    WeightedDrop(getRSCM("item.body_rune"), 3, weight = 3),
                    WeightedDrop(getRSCM("item.fire_rune"), 7, weight = 1),
                    // Coins. The 10-coin row is the members 6/128.
                    WeightedDrop(getRSCM("item.coins_995"), 10, weight = 6),
                    WeightedDrop(getRSCM("item.coins_995"), 26, weight = 5),
                    WeightedDrop(getRSCM("item.coins_995"), 35, weight = 4),
                    WeightedDrop(getRSCM("item.coins_995"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.coins_995"), 18, weight = 1),
                    // Other.
                    WeightedDrop(getRSCM("item.dark_fishing_bait"), 3, weight = 42),
                    WeightedDrop(getRSCM("item.dark_fishing_bait"), 4, weight = 25),
                    WeightedDrop(getRSCM("item.dark_fishing_bait"), 7, weight = 3),
                    WeightedDrop(getRSCM("item.eye_of_newt"), 1, weight = 2),
                    WeightedDrop(getRSCM("item.tinderbox"), 1, weight = 1),
                ),
        )

    /**
     * The table for [id], or null for the level 18 mainland zombie, which genuinely has none - it
     * drops bones and tertiaries only.
     */
    fun tableFor(id: ZombieTableId): ZombieTable? =
        when (id) {
            ZombieTableId.LEVEL_13 -> LEVEL_13
            ZombieTableId.LEVEL_18 -> null
            ZombieTableId.LEVEL_24 -> LEVEL_24
            ZombieTableId.WILDERNESS -> WILDERNESS
        }

    /**
     * One roll on [table], as item id to amount, or null for a `Nothing` row or a `Nothing` result
     * out of the gem table.
     *
     * A single d128 walks herb, then gem, then the ordinary rows, so the three are mutually
     * exclusive exactly as they are on the page. [wealth] - whether the killer wore a ring of wealth
     * - reaches the gem row only; the ring does not touch a monster's own `Nothing` rows. The fall-through to [DropRoll.pick] is then
     * conditional on having missed both sub-tables, and since [ZombieTable.rows] carries the
     * remaining weight, the composite is exact rather than approximate.
     */
    fun roll(
        table: ZombieTable,
        world: World,
        wealth: Boolean = false,
    ): Pair<Int, Int>? {
        var roll = world.randomDouble() * TABLE_SIZE

        if (roll < table.herbWeight) {
            return DropRoll.pick(HerbDropTable.TABLE, world)?.resolve(world)
        }
        roll -= table.herbWeight

        if (roll < table.gemWeight) {
            return GemDropTable.roll(world, wealth)?.resolve(world)
        }

        return DropRoll.pick(table.rows, world)?.resolve(world)
    }

    private fun WeightedDrop.resolve(world: World): Pair<Int, Int>? = item?.let { it to DropRoll.amount(this, world) }
}
