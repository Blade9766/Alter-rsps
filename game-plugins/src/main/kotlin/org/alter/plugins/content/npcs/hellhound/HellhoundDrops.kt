package org.alter.plugins.content.npcs.hellhound

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The hellhound drop tables - one per published drop version, because the three differ.
 *
 * ## The death rune table is a 50-slot table, not a 128-slot one
 *
 * The `Runes` section of the level 122 version is published out of **50**: 6/50 for 10 death runes,
 * 3/50 for 20 and 1/50 for 50. Those are the only item rows the hellhound has, so the remaining
 * 40 slots are a `Nothing` row rather than anything else - which is what [DEATH_RUNES] carries.
 *
 * Writing it that way rather than as "a 10/50 chance of a sub-roll" is deliberate: it is the same
 * single-roll reading [MonsterDropTable] documents, it keeps the table's own arithmetic checkable
 * by `BestiaryVerify`, and the two are numerically identical.
 *
 * The other two versions have **no main table at all** - the God Wars hellhound publishes only
 * `100%` and `Tertiary`, and the Wilderness Slayer Cave one only those plus its own smouldering
 * stone rate. That is why [HellhoundPlugin] rolls a table only for the level 122 version.
 *
 * ## What is not modelled
 *
 * - **The elite clue scroll and its reward casket**, both published as `Always` with the note
 *   "only dropped when completing an elite clue scroll asking you to kill a hellhound". That is a
 *   clue-step condition, not a drop rate; there is no Treasure Trails step system here, and a row
 *   marked `Always` handed out unconditionally would give every kill an elite casket. Dropped
 *   rather than faked, the same call `content/npcs/mossgiant` makes about the moss giant bone.
 * - **The Catacombs of Kourend tertiary table** (`{{CatacombsDropTable}}`) and the **Wilderness
 *   Slayer tertiary table**, both whole shared tables with their own economies that nothing in this
 *   codebase implements - the gap `content/npcs/mossgiant` and `content/npcs/chaosdruid` already
 *   record.
 * - **The brimstone key**, published at `{{Brimstone rarity|122}}` and conditioned on "killed on a
 *   Slayer task given by Konar quo Maten". `data/cfg/slayer/masters.json` has six masters and Konar
 *   is not among them, so the condition can never be true and the key can never be spent - there is
 *   no Brimstone chest either. Left out rather than given to every Slayer task, which is the only
 *   other reading available.
 *
 * The **smouldering stone** is kept at its real 1/32768 even though there is no Wilderness altar to
 * use it on, and the **ensouled hellhound head** at 1/40 with no Arceuus reanimation spell - the
 * same call `content/npcs/goblin` makes about the goblin champion scroll.
 */
internal object HellhoundDrops {
    /** Wiki tertiary, all three versions. */
    const val ENSOULED_HEAD_ONE_IN = 40

    /** Wiki tertiary, Wilderness only. */
    const val LOOTING_BAG_ONE_IN = 3

    /** Wiki tertiary, `DropsLineClue|type=hard|rarity=1/64`. */
    const val HARD_CLUE_ONE_IN = 64

    /**
     * The clue's `altrarity`: "the hard clue scroll drop rate increases to 1/32 if a ring of wealth
     * (i) is worn and fought in the Wilderness". Both halves of that condition are checked in
     * [HellhoundPlugin.rollHardClue] - it is one of the very few published drop rates in this tree
     * that depends on the *imbued* ring specifically rather than on any ring of wealth.
     */
    const val HARD_CLUE_WEALTH_ONE_IN = 32

    /** `Other`, level 122 only. */
    const val SMOULDERING_STONE_ONE_IN = 32768

    /** `Other`, Wilderness Slayer Cave version - twice as common as the level 122 one. */
    const val WILDERNESS_SMOULDERING_STONE_ONE_IN = 16384

    /**
     * The level 122 `Runes` section, published out of 50 and reaching it exactly once the 40 unused
     * slots are written as the `Nothing` row they are.
     */
    val DEATH_RUNES =
        MonsterDropTable(
            denominator = 50,
            rows =
                listOf(
                    WeightedDrop(getRSCM("item.death_rune"), 10, weight = 6),
                    WeightedDrop(getRSCM("item.death_rune"), 20, weight = 3),
                    WeightedDrop(getRSCM("item.death_rune"), 50, weight = 1),
                    WeightedDrop(item = null, weight = 40),
                ),
        )
}
