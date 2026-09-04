package org.alter.plugins.content.npcs.dragon

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The dragon drop tables - seven of them, one per published drop version that this package can
 * actually roll.
 *
 * Every table sums to **exactly 128** with its herb, gem and rare-table lines counted as rows, which
 * is the reading [MonsterDropTable] documents and `BestiaryVerify` re-checks. Two things had to be
 * read correctly for that to come out, and both are facts about the source rather than preferences:
 *
 * ## Monkey Madness II
 *
 * Four of these tables carry a row conditioned on that quest, and on three of them the condition is
 * a straight swap between two rows of equal weight:
 *
 * | Table | Not completed | Completed |
 * | --- | --- | --- |
 * | Red dragon | 330 coins, 10/128 | Dragon javelin tips, 10/128 |
 * | Black dragon | *nothing*, 6/128 | Dragon javelin tips, 6/128 |
 * | Black dragon (Wilderness) | 10 rune knives, 6/128 | Dragon javelin tips, 6/128 |
 *
 * Both readings sum to 128, so the arithmetic does not pick between them - but this server has no
 * Monkey Madness II, so "not completed" is simply the true state and is what is wired. The black
 * dragon's row is the clearest case: its own footnote says "If players have not completed the quest,
 * they will receive nothing", so those six slots are written as the `Nothing` row they are rather
 * than removed.
 *
 * There is a second reason the completed reading was not available: **`Dragon javelin tips` is not
 * in this rev-228 cache at all.** There is no item to drop.
 *
 * ## The tertiaries that are not modelled
 *
 * - **Elite clue scrolls and reward caskets published as `Always`** on the green dragon, with the
 *   note that they come from completing an elite clue step that asks you to kill one. That is a clue
 *   condition, not a rate; handed out unconditionally every kill would give a casket. The black
 *   dragon's elite clue is different - it has a real 1/500 - and *is* wired.
 * - **Brimstone keys**, on five of these pages, conditioned on a Konar quo Maten Slayer task.
 *   `data/cfg/slayer/masters.json` has six masters and Konar is not one of them, so the condition
 *   can never be true and there is no Brimstone chest to spend a key in.
 * - **Tattered pages and grubby keys** on the red and baby red dragons: "only dropped by those found
 *   in the Forthos Dungeon", a dungeon whose mapsquares this cache does not ship, so no red dragon
 *   here stands in it. The page also states all three page rows are one 1/10 roll that hands over
 *   "the page they have the fewest of", which is a Scroll of Redirection mechanic that does not
 *   exist here either.
 * - **Frozen tears** and the **moon key halves** on the blue and baby blue dragons, both
 *   `leagueRegion=Varlamore` content postdating this cache.
 * - **The Catacombs of Kourend tertiary table** on the bronze dragon, and the **Wilderness Slayer**
 *   tables - whole shared tables nothing here implements, the gap `content/npcs/mossgiant` records.
 *
 * The **draconic visage** is kept at its real 1/10000, and the **ensouled dragon head** at its
 * per-version rate, even though there is no Arceuus reanimation spell: faking a rarity to hide a
 * missing system is worse than dropping an item that currently only sits in a bank.
 */
internal object DragonDrops {
    private fun drop(
        item: String,
        min: Int = 1,
        max: Int = min,
        weight: Int,
    ) = WeightedDrop(getRSCM(item), min, max, weight)

    private fun coins(
        min: Int,
        max: Int = min,
        weight: Int,
    ) = WeightedDrop(getRSCM("item.coins_995"), min, max, weight)

    // ------------------------------------------------------------------------- shared rates

    /** `Ensouled dragon head`, green and black. */
    const val ENSOULED_HEAD_35 = 35

    /** `Ensouled dragon head`, red. */
    const val ENSOULED_HEAD_40 = 40

    /** `Ensouled dragon head`, blue. */
    const val ENSOULED_HEAD_50 = 50

    /** `Looting bag`, Wilderness only, on the green and black dragons. */
    const val LOOTING_BAG_ONE_IN = 3

    /** `DropsLineClue|type=hard|rarity=1/128` - every adult dragon here publishes the same rate. */
    const val HARD_CLUE_ONE_IN = 128

    /**
     * The hard clue's `altrarity`. Unlike the hellhound's, the dragons' footnote names no place:
     * "the hard clue scroll rarity changes to 1/64 if a ring of wealth (i) is worn" - so this is
     * gated on the imbued ring alone, with no Wilderness test.
     */
    const val HARD_CLUE_WEALTH_ONE_IN = 64

    /** Black dragon `DropsLineClue|type=elite`. */
    const val ELITE_CLUE_ONE_IN = 500

    const val ELITE_CLUE_WEALTH_ONE_IN = 250

    /** Black dragon tertiary, the only draconic visage source in this package. */
    const val VISAGE_ONE_IN = 10000

    /** Black dragon (Wilderness Slayer Cave) tertiary - twice as likely as the level 227's. */
    const val WILDERNESS_VISAGE_ONE_IN = 5000

    /** Baby blue dragon tertiary. */
    const val BABY_DRAGON_BONE_ONE_IN = 4

    /** Baby blue dragon tertiary. */
    const val SCALY_BLUE_HIDE_BABY_ONE_IN = 200

    /** Blue dragon tertiary. */
    const val SCALY_BLUE_HIDE_ONE_IN = 50

    // ------------------------------------------------------------------------------- tables

    /** Bronze dragon, `Standard` - rows 113, rare 1, gem 4, and the two sub-table rows make 128. */
    val BRONZE =
        MonsterDropTable(
            denominator = 128,
            rareWeight = 1,
            gemWeight = 4,
            rows =
                listOf(
                    // Weapons and armour - 23.
                    drop("item.adamant_dartp", 16, weight = 7),
                    drop("item.mithril_2h_sword", weight = 4),
                    drop("item.mithril_axe", weight = 3),
                    drop("item.mithril_battleaxe", weight = 3),
                    drop("item.rune_knife", 2, weight = 3),
                    drop("item.mithril_kiteshield", weight = 1),
                    drop("item.adamant_platebody", weight = 1),
                    drop("item.rune_longsword", weight = 1),
                    // Runes and ammunition - 43.
                    drop("item.adamant_javelin", 30, weight = 20),
                    drop("item.fire_rune", 50, weight = 8),
                    drop("item.mithril_bolts", 2, 12, weight = 6),
                    drop("item.law_rune", 10, weight = 5),
                    drop("item.blood_rune", 15, weight = 3),
                    drop("item.death_rune", 25, weight = 1),
                    // Coins - 51.
                    coins(196, weight = 40),
                    coins(330, weight = 10),
                    coins(690, weight = 1),
                    // Other - 6.
                    drop("item.adamantite_bar", weight = 3),
                    drop("item.swordfish", 2, weight = 2),
                    drop("item.swordfish", weight = 1),
                ),
        )

    /** Green dragon, `Regular` - rows 108, herbs 15, gem 5. */
    val GREEN =
        MonsterDropTable(
            denominator = 128,
            herbWeight = 15,
            gemWeight = 5,
            rows =
                listOf(
                    // Weapons and armour - 15.
                    drop("item.steel_platelegs", weight = 4),
                    drop("item.steel_battleaxe", weight = 3),
                    drop("item.mithril_axe", weight = 3),
                    drop("item.mithril_spear", weight = 2),
                    drop("item.mithril_kiteshield", weight = 1),
                    drop("item.adamant_full_helm", weight = 1),
                    drop("item.rune_dagger", weight = 1),
                    // Runes - 17.
                    drop("item.water_rune", 75, weight = 8),
                    drop("item.nature_rune", 15, weight = 5),
                    drop("item.law_rune", 3, weight = 3),
                    drop("item.fire_rune", 37, weight = 1),
                    // Coins - 70.
                    coins(44, weight = 29),
                    coins(132, weight = 25),
                    coins(200, weight = 10),
                    coins(11, weight = 5),
                    coins(440, weight = 1),
                    // Other - 6.
                    drop("item.bass", weight = 3),
                    drop("item.adamantite_ore", weight = 3),
                ),
        )

    /** Green dragon, `Wilderness Slayer Cave` - rows 108, herbs 15, gem 5. */
    val GREEN_WILDERNESS =
        MonsterDropTable(
            denominator = 128,
            herbWeight = 15,
            gemWeight = 5,
            rows =
                listOf(
                    // Weapons and armour - 12.
                    drop("item.mithril_2h_sword", weight = 5),
                    drop("item.mithril_platelegs", weight = 2),
                    drop("item.mithril_battleaxe", weight = 1),
                    drop("item.adamant_dagger", weight = 1),
                    drop("item.adamant_sword", weight = 1),
                    drop("item.adamant_full_helm", weight = 1),
                    drop("item.adamant_kiteshield", weight = 1),
                    // Runes - 6.
                    drop("item.law_rune", 3, 10, weight = 3),
                    drop("item.nature_rune", 10, weight = 1),
                    drop("item.death_rune", 15, weight = 1),
                    drop("item.blood_rune", 5, weight = 1),
                    // Coins - 80.
                    coins(100, 199, weight = 29),
                    coins(250, 499, weight = 25),
                    coins(500, 749, weight = 14),
                    coins(110, weight = 6),
                    coins(500, 999, weight = 6),
                    // Other - 10. The mithril ore row is published noted; there is no note-on-drop
                    // mechanic here, so it drops as two ore, the same call RareDropTable makes.
                    drop("item.monkfish", weight = 7),
                    drop("item.mithril_ore", 2, weight = 3),
                ),
        )

    /**
     * Blue dragon - the same 128 as the green level 79, row for row, with a different fish and a
     * different rune ordering. Kept as its own table rather than aliased to [GREEN], because the two
     * pages are separate sources and nothing guarantees they stay identical.
     */
    val BLUE =
        MonsterDropTable(
            denominator = 128,
            herbWeight = 15,
            gemWeight = 5,
            rows =
                listOf(
                    // Weapons and armour - 15.
                    drop("item.steel_platelegs", weight = 4),
                    drop("item.mithril_axe", weight = 3),
                    drop("item.steel_battleaxe", weight = 3),
                    drop("item.mithril_spear", weight = 2),
                    drop("item.adamant_full_helm", weight = 1),
                    drop("item.mithril_kiteshield", weight = 1),
                    drop("item.rune_dagger", weight = 1),
                    // Runes - 17.
                    drop("item.water_rune", 75, weight = 8),
                    drop("item.nature_rune", 15, weight = 5),
                    drop("item.law_rune", 3, weight = 3),
                    drop("item.fire_rune", 37, weight = 1),
                    // Coins - 70.
                    coins(44, weight = 29),
                    coins(132, weight = 25),
                    coins(200, weight = 10),
                    coins(11, weight = 5),
                    coins(440, weight = 1),
                    // Other - 6.
                    drop("item.adamantite_ore", weight = 3),
                    drop("item.bass", weight = 3),
                ),
        )

    /** Red dragon - rows 121, herbs 2, gem 5. See the file doc for the Monkey Madness II reading. */
    val RED =
        MonsterDropTable(
            denominator = 128,
            herbWeight = 2,
            gemWeight = 5,
            rows =
                listOf(
                    // Weapons and armour - 17.
                    drop("item.mithril_2h_sword", weight = 4),
                    drop("item.mithril_axe", weight = 3),
                    drop("item.mithril_battleaxe", weight = 3),
                    drop("item.rune_dart", 8, weight = 3),
                    drop("item.mithril_javelin", 20, weight = 1),
                    drop("item.mithril_kiteshield", weight = 1),
                    drop("item.adamant_platebody", weight = 1),
                    drop("item.rune_longsword", weight = 1),
                    // Runes and ammunition - 20.
                    drop("item.rune_arrow", 4, weight = 8),
                    drop("item.law_rune", 4, weight = 5),
                    drop("item.blood_rune", 2, weight = 4),
                    drop("item.death_rune", 5, weight = 3),
                    // Coins - 80. The 330 row is the Monkey Madness II swap; see the file doc.
                    coins(196, weight = 40),
                    coins(66, weight = 29),
                    coins(330, weight = 10),
                    coins(690, weight = 1),
                    // Other - 4, the dragon javelin tips row being the other half of that swap.
                    drop("item.chocolate_cake", 3, weight = 3),
                    drop("item.adamantite_bar", weight = 1),
                ),
        )

    /** Black dragon, `Regular` - rows 123, rare 2, gem 3. */
    val BLACK =
        MonsterDropTable(
            denominator = 128,
            rareWeight = 2,
            gemWeight = 3,
            rows =
                listOf(
                    // Weapons and armour - 16.
                    drop("item.mithril_2h_sword", weight = 4),
                    drop("item.mithril_axe", weight = 3),
                    drop("item.mithril_battleaxe", weight = 3),
                    drop("item.rune_knife", 2, weight = 3),
                    drop("item.mithril_kiteshield", weight = 1),
                    drop("item.adamant_platebody", weight = 1),
                    drop("item.rune_longsword", weight = 1),
                    // Runes and ammunition - 44.
                    drop("item.adamant_javelin", 30, weight = 20),
                    drop("item.fire_rune", 50, weight = 8),
                    drop("item.adamant_dartp", 16, weight = 7),
                    drop("item.law_rune", 10, weight = 5),
                    drop("item.blood_rune", 15, weight = 3),
                    drop("item.air_rune", 75, weight = 1),
                    // Coins - 51.
                    coins(196, weight = 40),
                    coins(330, weight = 10),
                    coins(690, weight = 1),
                    // Other - 12. The dragon javelin tips row is the six `Nothing` slots: its own
                    // footnote says "if players have not completed the quest, they will receive
                    // nothing", and there is no Monkey Madness II here.
                    WeightedDrop(item = null, weight = 6),
                    drop("item.adamantite_bar", weight = 3),
                    drop("item.chocolate_cake", weight = 3),
                ),
        )

    /** Black dragon, `Wilderness Slayer Cave` - rows 123, rare 2, gem 3. */
    val BLACK_WILDERNESS =
        MonsterDropTable(
            denominator = 128,
            rareWeight = 2,
            gemWeight = 3,
            rows =
                listOf(
                    // Weapons and armour - 49. The 10-knife row is the not-completed half of the
                    // Monkey Madness II swap and so is present; the javelin tips are not.
                    drop("item.adamant_platebody", weight = 20),
                    drop("item.rune_knife", 15, weight = 7),
                    drop("item.rune_knife", 10, weight = 6),
                    drop("item.adamant_2h_sword", weight = 4),
                    drop("item.adamant_battleaxe", weight = 3),
                    drop("item.rune_axe", weight = 3),
                    drop("item.rune_knife", 5, weight = 3),
                    drop("item.rune_med_helm", weight = 1),
                    drop("item.rune_kiteshield", weight = 1),
                    drop("item.rune_longsword", weight = 1),
                    // Runes and ammunition - 17.
                    drop("item.death_rune", 10, weight = 8),
                    drop("item.law_rune", 10, weight = 5),
                    drop("item.blood_rune", 10, weight = 3),
                    drop("item.chaos_rune", 25, weight = 1),
                    // Coins - 51.
                    coins(500, 999, weight = 40),
                    coins(1000, 1999, weight = 10),
                    coins(690, weight = 1),
                    // Other - 6.
                    drop("item.adamantite_bar", weight = 3),
                    drop("item.monkfish", 2, weight = 3),
                ),
        )

    /**
     * Every table by the label [DragonVariant.dropTable] names, so [DragonPlugin] can look one up
     * without a `when` that has to be kept in step with [Dragons.VARIANTS].
     *
     * The three baby labels are absent on purpose: **no baby dragon has a main drop table at all.**
     * Their pages publish a `100%` section and a `Tertiary` section and nothing in between, which is
     * why the baby rows in [Dragons.VARIANTS] name tables that are not here and [DragonPlugin] treats
     * a missing label as "no roll" rather than as an error.
     */
    val BY_LABEL: Map<String, MonsterDropTable> =
        mapOf(
            "bronze" to BRONZE,
            "green" to GREEN,
            "green wilderness" to GREEN_WILDERNESS,
            "blue" to BLUE,
            "red" to RED,
            "black" to BLACK,
            "black wilderness" to BLACK_WILDERNESS,
        )
}
