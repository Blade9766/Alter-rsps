package org.alter.plugins.content.npcs.elderchaosdruid

import org.alter.game.model.World
import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The Elder Chaos druids of the Wilderness Chaos Temple - one id, one location, and the biggest herb
 * table in the game.
 *
 * A package of its own rather than a row in `content/npcs/chaosdruid`, for the reason that file
 * already gives about the chaos druid warrior: this is a **separate page and a separate monster**.
 * It shares nothing with the level 13 chaos druid but a name - not its level (129 against 13), not
 * its spell (a real damaging Wind Wave against a Confuse-and-Bind that deals none), not its table,
 * not its rig, and not its location.
 *
 * See [ElderChaosDruidCombatStrategy] for the spell, [ElderChaosDruidPlugin] for the wiring. Stats
 * come from `data/cfg/npcs/monsterStats.json`: 150 hitpoints, 98/98/65 melee, **magic level 110**.
 *
 * ## The animations were wrong before this package
 *
 * The resolver had attack and block backwards: it played `HUMAN_DEFEND_COWARDLY` (425) as the attack
 * and `MAGIC_WAVE_CAST` (727) as the block. 425 being a block on this rig is the same fact
 * `content/npcs/chaosdruid` records for npc 520. Pinned now as 727 / 425 / 836; see
 * `npc-animations/README.md`.
 *
 * ## What is not modelled
 *
 * Two published mechanics, both stated here rather than left to be discovered:
 *
 * - **Tele Block.** "Capable of teleblocking players and re-applying it consistently." There is no
 *   Tele Block timer in this codebase for an npc to apply.
 * - **Teleporting fleeing players to melee distance**, at a published maximum range of 20 tiles and
 *   preventable with a teleport anchoring scroll. That is a target-relocation attack with its own
 *   damage-tracking target selection ("the player running that has done the most damage"), and none
 *   of the three pieces exists.
 *
 * The damage half is real and complete: see [ElderChaosDruidCombatStrategy].
 */
internal object ElderChaosDruids {
    private fun drop(
        item: String,
        min: Int = 1,
        max: Int = min,
        weight: Int,
    ) = WeightedDrop(getRSCM(item), min, max, weight)

    private fun coins(
        min: Int,
        weight: Int,
    ) = WeightedDrop(getRSCM("item.coins_995"), min, min, weight)

    /** The one id the infobox publishes. */
    const val NPC_KEY = "npc.elder_chaos_druid"

    const val COMBAT_LEVEL = 129

    /** Wiki `respawn = 25`, in game ticks, which are this engine's cycles one-for-one. */
    const val RESPAWN_CYCLES = 25

    /** Wiki `slayxp = 150`. The `Chaos Druids` category is not in `data/cfg/slayer/tasks.json`. */
    const val SLAYER_XP = 150.0

    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a druid stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`, stated
     * because a def built from `monsterStats.json` starts with a **0** timer, which
     * `NpcAggroPlugin` reads as "stop being aggressive".
     */
    const val AGGRO_TIMER = 1000

    /** Kept tight: these thirteen stand inside one small temple. */
    const val WALK_RADIUS = 3

    // ------------------------------------------------------------------------------ the herbs

    /**
     * The published denominator - 129, not 128, which is unique to this page in the whole tree.
     *
     * [TABLE] is built against [FINE_DENOMINATOR] instead; see its doc for why.
     */
    const val DENOMINATOR = 129

    /**
     * 129 x 11, which is the denominator the page's own robe rows are published against
     * (`Zamorak monk top ... 4/1419`).
     *
     * [TABLE] uses it so the five robe rows can be ordinary rows rather than a nested sub-table.
     * Multiplying every other numerator by 11 changes no rate at all - it is the same fractions over
     * a common denominator - and it removes the one thing that would otherwise be ambiguous: a
     * `Nothing` row standing in for the robe table would be indistinguishable, at the point of
     * rolling, from a `Nothing` out of the gem table.
     */
    const val FINE_DENOMINATOR = 1419

    /** What each published `/129` numerator is multiplied by to reach [FINE_DENOMINATOR]. */
    const val FINE = 11

    /**
     * The herb section's own numerator, and the largest on any table here: **55 of 129 kills roll
     * herbs**, which is why anybody comes to this temple.
     *
     * It is not a flat rate either. The page's `override` spells the split out: "there is a 15/55
     * chance of dropping 1 herb, 20/55 chance of dropping 2 herbs, 15/55 chance of dropping 3 herbs,
     * and 5/55 chance of dropping 4 herbs leading to an average of 2.18 herbs per roll". The four
     * thresholds below are that distribution as cumulative bounds, the same shape
     * `content/npcs/chaosdruid` uses for its own one-or-two split.
     */
    const val HERB_WEIGHT = 55

    /** Cumulative thresholds within [HERB_WEIGHT]: below 15 is one herb, 15-34 two, 35-49 three, else four. */
    const val ONE_HERB_THRESHOLD = 15
    const val TWO_HERB_THRESHOLD = 35
    const val THREE_HERB_THRESHOLD = 50

    /**
     * How many herbs this roll of the herb table hands out, on the page's published 15/20/15/5 split.
     *
     * Passed to [MonsterDropTable.herbRolls], so the count is decided *inside* the one d1419 that
     * already chose the herb row - a second independent roll would be a different thing.
     */
    fun herbCount(world: World): Int {
        val roll = world.random(HERB_WEIGHT - 1)
        return when {
            roll < ONE_HERB_THRESHOLD -> 1
            roll < TWO_HERB_THRESHOLD -> 2
            roll < THREE_HERB_THRESHOLD -> 3
            else -> 4
        }
    }

    // -------------------------------------------------------------------------- the robe table

    // ----------------------------------------------------------------------------- tertiaries

    /** Wiki tertiary. */
    const val ENSOULED_HEAD_ONE_IN = 20

    /** Wiki tertiary. Every druid here stands in the Wilderness, so this needs no position test. */
    const val LOOTING_BAG_ONE_IN = 3

    /** `DropsLineClue|type=hard`, published against 128 rather than the table's 129. */
    const val HARD_CLUE_ONE_IN = 128

    /** The clue's `altrarity`, on a worn ring of wealth (i). The footnote names no place. */
    const val HARD_CLUE_WEALTH_ONE_IN = 64

    /**
     * The main table, against [FINE_DENOMINATOR] - which is the published 129 with every numerator
     * multiplied by [FINE], so that the `Zamorak robes` sub-table's five rows can sit in it directly
     * at the rates the page already publishes for them. In `/129` terms it is rows 71, robes 1,
     * herbs 55, rare 1 and gem 1.
     *
     * ## The Observatory Quest reading
     *
     * Two rows are conditioned on that quest, in opposite directions, and they are the same slot:
     *
     * - **Mithril bolts** are 6/129 normally and, by their own footnote, **7/129 if the Observatory
     *   Quest has not been completed**.
     * - **The unholy mould** at 1/129 is "only dropped after completion of the Observatory Quest".
     *
     * So the bolt row absorbs the mould's slot when the quest is unfinished, and both readings land
     * on 129 exactly. This server has no Observatory Quest, so the unfinished reading is simply the
     * true one: bolts at 7, no unholy mould. That is a fact about the state of the server rather than
     * a preference, and the arithmetic checks either way, so nothing is being papered over.
     */
    val TABLE =
        MonsterDropTable(
            denominator = FINE_DENOMINATOR,
            herbWeight = HERB_WEIGHT * FINE,
            herbRolls = ::herbCount,
            rareWeight = FINE,
            gemWeight = FINE,
            rows =
                listOf(
                    // Runes and ammunition - 40/129, the bolts at their no-Observatory-Quest 7.
                    drop("item.law_rune", 6, weight = 7 * FINE),
                    drop("item.mithril_bolts", 8, 28, weight = 7 * FINE),
                    drop("item.air_rune", 56, weight = 5 * FINE),
                    drop("item.body_rune", 19, weight = 5 * FINE),
                    drop("item.chaos_rune", 7, weight = 5 * FINE),
                    drop("item.earth_rune", 19, weight = 5 * FINE),
                    drop("item.mind_rune", 22, weight = 5 * FINE),
                    drop("item.nature_rune", 12, weight = 1 * FINE),
                    // The Zamorak robe sub-table - 1/129 in total, split 8:3 between the monk robes
                    // and the elder chaos set, at the page's own /1419 rates.
                    drop("item.zamorak_monk_top", weight = 4),
                    drop("item.zamorak_monk_bottom", weight = 4),
                    drop("item.elder_chaos_hood", weight = 1),
                    drop("item.elder_chaos_robe", weight = 1),
                    drop("item.elder_chaos_top", weight = 1),
                    // Coins - 13/129.
                    coins(80, weight = 7 * FINE),
                    coins(250, weight = 6 * FINE),
                    // Other - 18/129. The unholy mould's slot is the one the bolts took.
                    drop("item.vial_of_water", 4, weight = 10 * FINE),
                    drop("item.steel_longsword", weight = 5 * FINE),
                    drop("item.dark_fishing_bait", 10, 24, weight = 2 * FINE),
                    drop("item.snape_grass", 4, weight = 1 * FINE),
                ),
        )

    /**
     * The Chaos Temple, the only place the page puts one - thirteen pins.
     *
     * Six are written `x:3229,y:3613` and seven as a bare `3231,3610`; both forms are the same thing
     * and all thirteen are here. `content/npcs/mossgiant` hit the bare form once too, on a Varlamore
     * line, and dropped it - this file does not, because the seven are half the population.
     */
    val TILES: List<Pair<Int, Int>> =
        listOf(
            3229 to 3613, 3231 to 3607, 3235 to 3615, 3245 to 3616, 3249 to 3608,
            3249 to 3611, 3231 to 3610, 3232 to 3615, 3239 to 3613, 3240 to 3603,
            3241 to 3607, 3247 to 3613, 3249 to 3606,
        )
}
