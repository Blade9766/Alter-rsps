package org.alter.plugins.content.npcs.chaosdruid

import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The chaos druid (npc 520, combat level 13) - stats and loot, straight off its
 * `Infobox Monster` and drop tables at https://oldschool.runescape.wiki/w/Chaos_druid.
 *
 * ## Why this monster has its own package
 *
 * It used to live in `content/npcs/dungeon` as one entry among thirty, and outgrew that file in
 * three directions at once:
 *
 * - **It casts.** Its published attack style is "Crush, Magic", and `DungeonMonsters` said so
 *   itself while wiring it crush-only, because a shared stat-block file has nowhere to put a
 *   spell. That spell is now real - see [ChaosDruidCombatPlugin].
 * - **Its herb table is not a flat rate.** Every other herb-table monster in this codebase rolls
 *   the table once at a published rate; the chaos druid rolls it at 46/128 for *one or two*
 *   herbs (35/128 and 11/128 respectively), which `DungeonMonsterPlugin`'s single
 *   `herbTableChance` cannot express. Herbs are the whole reason anyone kills these, so getting
 *   the split right matters more here than anywhere else it appears.
 * - **Three of its five locations are not dungeons.** Taverley and the Yanille Agility Dungeon
 *   are; the Chaos Druid Tower, the roof of Slepe church and the Edgeville Dungeon are not.
 *
 * The **chaos druid warrior** (npc 532, level 37) is a separate monster with a separate page and
 * stays in `content/npcs/dungeon` - it has no magic attack and no herb table, so none of the
 * above applies to it.
 */
internal object ChaosDruids {
    const val NPC_KEY = "npc.chaos_druid"

    // Infobox Monster, verbatim.
    const val COMBAT_LEVEL = 13
    const val HITPOINTS = 20
    const val ATTACK_LEVEL = 8
    const val STRENGTH_LEVEL = 8
    const val DEFENCE_LEVEL = 12
    const val MAGIC_LEVEL = 10
    const val RANGED_LEVEL = 1

    /** `attbns`/`strbns` and all six defence bonuses are published as 0. */
    const val ATTACK_BONUS = 0
    const val STRENGTH_BONUS = 0

    const val ATTACK_SPEED = 4

    /** The wiki's `respawn` field is in game ticks, which are this engine's cycles one-for-one. */
    const val RESPAWN_CYCLES = 25

    const val SLAYER_XP = 20.0

    /** `aggressive = Yes`. No `alwaysAggro` - the page makes no claim to ignore combat level. */
    const val AGGRO_RADIUS = 4

    /**
     * The four animations this project's own resolver observed for npc 520: `[425, 710, 422, 836]`.
     *
     * 422 is the unarmed punch, which is exactly what the page describes ("they attack with their
     * fists"); 710 is [org.alter.api.cfg.Animation.DRUID_BIND], the cast used in
     * [ChaosDruidCombatPlugin]; 425 and 836 are the standard human block and death.
     */
    const val ATTACK_ANIMATION = 422
    const val BLOCK_ANIMATION = 425
    const val DEATH_ANIMATION = 836
    const val BIND_ANIMATION = 710

    /** Dropped on every kill. */
    val GUARANTEED_DROPS = listOf("item.bones")

    /**
     * The "Runes and ammunition", "Coins" and "Other" tables rolled as one, which is how the wiki
     * publishes them - one shared denominator of 128 across all three, not three separate rolls.
     *
     * Weights are the published numerators used as relative weights (see
     * [org.alter.plugins.content.npcs.DropRoll]); the `Nothing` row is the wiki's own 33/128 plus
     * the 47/128 of that denominator the herb and gem tables account for, which are rolled
     * separately in [ChaosDruidPlugin] because the wiki quotes them as their own chances rather
     * than as rows here.
     *
     * The **mithril bolts** row is the post-Observatory Quest rate (4/128, against 5/128 before),
     * and the **unholy mould** is dropped unconditionally though the wiki gates it on that same
     * quest - the same treatment quest-gated content gets everywhere else in this codebase, where
     * quests that do not exist yet are treated as completed.
     */
    val TABLE: List<WeightedDrop> =
        listOf(
            // Runes and ammunition.
            WeightedDrop(getRSCM("item.law_rune"), 2, weight = 7),
            WeightedDrop(getRSCM("item.mithril_bolts"), 2, 12, weight = 4),
            WeightedDrop(getRSCM("item.air_rune"), 36, weight = 3),
            WeightedDrop(getRSCM("item.body_rune"), 9, weight = 2),
            WeightedDrop(getRSCM("item.earth_rune"), 9, weight = 2),
            WeightedDrop(getRSCM("item.mind_rune"), 12, weight = 2),
            WeightedDrop(getRSCM("item.nature_rune"), 3, weight = 1),
            // Coins.
            WeightedDrop(getRSCM("item.coins_995"), 3, weight = 5),
            WeightedDrop(getRSCM("item.coins_995"), 8, weight = 5),
            WeightedDrop(getRSCM("item.coins_995"), 29, weight = 3),
            WeightedDrop(getRSCM("item.coins_995"), 35, weight = 1),
            // Other.
            WeightedDrop(getRSCM("item.vial_of_water"), 1, weight = 10),
            WeightedDrop(getRSCM("item.bronze_longsword"), 1, weight = 1),
            WeightedDrop(getRSCM("item.snape_grass"), 1, weight = 1),
            WeightedDrop(getRSCM("item.unholy_mould"), 1, weight = 1),
            WeightedDrop(item = null, weight = NOTHING_WEIGHT),
        )

    /**
     * `{{HerbDropLines|46/128|1-2}}`, with the split the page states in prose: "The chance of one
     * herb is 35/128 and the chance of two herbs is 11/128".
     *
     * Rolled against [DENOMINATOR] rather than as two independent chances, so the two outcomes are
     * mutually exclusive and total exactly 46/128.
     */
    /** Cumulative thresholds against [DENOMINATOR], not weights: below 35 is one herb, 35-45 is two. */
    const val ONE_HERB_THRESHOLD = 35
    const val TWO_HERB_THRESHOLD = 46

    /** `{{GemDropTable|1/128}}`. Its chaos and nature talisman rows are already in the shared table. */
    const val GEM_TABLE_CHANCE = 1.0 / 128.0

    /** The shared denominator every rate on the page is published against. */
    const val DENOMINATOR = 128

    /**
     * The published `Nothing` row (33/128) plus the 47/128 the separately-rolled herb (46) and gem
     * (1) tables occupy, so [TABLE]'s own weights stay at their published numerators and still
     * total 128.
     */
    private const val NOTHING_WEIGHT = 33 + 46 + 1

    /**
     * Tertiaries, each an independent roll on top of the main table.
     *
     * The looting bag is Wilderness-only, which for chaos druids means the Edgeville Dungeon
     * spawns and nothing else.
     *
     * Not modelled: `{{WildernessSlayerDropTable}}`. It needs a Wilderness Slayer task to be on,
     * and Krystilia - the master who assigns the "Chaos Druids" category this monster belongs to -
     * is not among the Slayer masters this server has yet.
     */
    val TERTIARY_DROPS =
        listOf(
            TertiaryDrop("item.looting_bag", 1.0 / 11.0, wildernessOnly = true),
            TertiaryDrop("item.ensouled_chaos_druid_head", 1.0 / 35.0),
        )
}

internal data class TertiaryDrop(
    val item: String,
    val chance: Double,
    val wildernessOnly: Boolean = false,
)
