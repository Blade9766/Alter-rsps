package org.alter.plugins.content.npcs.unicorn

/**
 * The `Unicorn` and `Unicorn Foal` pages - the adult at combat level 15 and its offspring at 12 -
 * and every place either stands.
 *
 * Both are modelled here rather than in two packages because the foal is the same animal on the
 * same rig, found "around adult unicorns", sharing two of its three locations with them. See
 * [UnicornPlugin] for the wiring and [UnicornSpawnPlugin] for the placement.
 *
 * ## Why there is no `setCombatDef` in this package
 *
 * The reason `content/npcs/zombie` and `content/npcs/dwarf` give: 2837, 14043 and 3910 already
 * carry their exact wiki stat blocks in `data/cfg/npcs/monsterStats.json`, and
 * `World.setNpcDefaults` reads that table **only** for npcs no plugin declares a def for.
 *
 * ## The animations had to be named rather than resolved
 *
 * Unicorns are observed playing **6375 / 6376 / 6377**, and
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationResolver] gets two of the three
 * backwards on them. It picks the attack by frame sound and then by duration, and this rig carries
 * no frame sounds at all: 6375 is three cycles against 6376's two, so it wins the attack slot even
 * though 6375 is the *block* (`forcedPriority` 5, the value every correct entry in
 * `named-combat-media.json` uses for a block, against 6376's 6 for an attack).
 *
 * Two independent checks agree. `crystalline_unicorn` and `corrupted_unicorn` are the only npcs on
 * this rig observed playing just **two** of the three, and the two they play are 6376 and 6377 -
 * which is what a monster that only ever attacks and dies would show. And `named-combat-media.json`
 * already carried a hand-written `STARLIGHT` row - the unicorn from the Fremennik Trials, same rig -
 * reading exactly `6376 / 6375 / 6377`.
 *
 * So a `UNICORN` entry naming 6376 / 6375 / 6377 is added rather than leaving the resolver to it,
 * and `Unicorn Foal` picks the same entry up through `findNamedCombatMedia`'s prefix match.
 *
 * ## What is deliberately not built
 *
 * Both pages say the animal "will retreat from combat at low health". There is no retreat behaviour
 * anywhere in this engine to hook - no monster in the tree has one - so this is left alone rather
 * than invented for one species.
 */
internal data class UnicornVariant(
    /** The wiki page title, kept verbatim. */
    val name: String,
    val combatLevel: Int,
    val npcKeys: List<String>,
    /**
     * Whether this version drops the horn and the ensouled head.
     *
     * The foal's page states the exception outright: "It does **not** drop unicorn horns or unicorn
     * bones."
     */
    val adult: Boolean,
)

/** One published `LocLine`: a place, the ids that stand there, and the tiles. */
internal data class UnicornCamp(
    val location: String,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object Unicorns {
    /**
     * Wiki `respawn = 90` on both pages, in game ticks.
     *
     * The adult page describes the consequence in its own words - "They respawn slowly, at a rate of
     * ~53 seconds" - which is exactly 90 ticks, so this is used as published rather than shortened.
     */
    const val RESPAWN_CYCLES = 90

    /**
     * How far a spawned unicorn wanders from its pin.
     *
     * Both versions are `aggressive = No`, so this is the only thing that moves one, and the page's
     * own description is that they "wander around forested areas" - the widest radius in this tree,
     * because a unicorn standing still on a pin in an open field is the wrong picture.
     */
    const val WALK_RADIUS = 6

    /** `Unicorn` `id = 2837,14043`. Both are level 15 `Unicorn`, size 2, `Attack`, in this cache. */
    val ADULT_IDS = listOf("npc.unicorn", "npc.unicorn_14043")

    /** `Unicorn Foal` `id = 3910`. */
    const val FOAL_ID = "npc.unicorn_foal"

    val VARIANTS: List<UnicornVariant> =
        listOf(
            UnicornVariant("Unicorn", 15, ADULT_IDS, adult = true),
            UnicornVariant("Unicorn Foal", 12, listOf(FOAL_ID), adult = false),
        )

    /**
     * Every `LocLine` on both pages.
     *
     * The two adult ids are dealt across the camps by [org.alter.plugins.content.npcs.SpawnDealer]:
     * the infobox names both ids for every location and nothing anywhere says which pin is which, so
     * alternating them reproduces the published mix without inventing a mapping. Six of these camps
     * are a single pin, which is exactly the case the dealer's shared cursor exists for - a per-camp
     * count would have put npc 2837 on all six and 14043 on none of them.
     *
     * Unicorns are size 2, so a pin needs a standable 2x2 rather than a standable tile -
     * `BestiaryVerify` checks the whole footprint.
     *
     * **The omission is Lledrith Island**, which both pages list (`leagueRegion = N/A`) and which
     * postdates this rev-228 cache - there is no map there to stand on. Every other published pin is
     * placed.
     */
    val CAMPS: List<UnicornCamp> =
        listOf(
            // ------------------------------------------------------------------ Misthalin
            UnicornCamp("West of Lumbridge", ADULT_IDS, listOf(3140 to 3209)),
            UnicornCamp("South of Edgeville", ADULT_IDS, listOf(3084 to 3454, 3090 to 3450)),
            UnicornCamp("South-east Varrock mine", ADULT_IDS, listOf(3283 to 3355, 3286 to 3349)),
            UnicornCamp("Isle of Souls", ADULT_IDS, listOf(2208 to 2992)),
            // ------------------------------------------------------------------ Asgarnia
            UnicornCamp(
                location = "Entrana",
                npcKeys = ADULT_IDS,
                tiles = listOf(2838 to 3370, 2850 to 3378, 2860 to 3375, 2863 to 3378),
            ),
            // ------------------------------------------------------------------ Kandarin
            UnicornCamp("Ardougne Zoo", ADULT_IDS, listOf(2629 to 3266, 2634 to 3265)),
            UnicornCamp("West of Catherby farming patches", ADULT_IDS, listOf(2783 to 3466, 2791 to 3460)),
            UnicornCamp("East of the Ranging Guild", ADULT_IDS, listOf(2700 to 3423, 2700 to 3442)),
            UnicornCamp("South of Yanille", ADULT_IDS, listOf(2571 to 3058, 2579 to 3066)),
            // ------------------------------------------------------------------ Fremennik
            UnicornCamp("South-east of Rellekka", ADULT_IDS, listOf(2741 to 3605)),
            UnicornCamp("South-east of Rellekka (foals)", listOf(FOAL_ID), listOf(2739 to 3605, 2743 to 3605)),
            // ------------------------------------------------------------------ Kourend
            UnicornCamp("North-east of Hosidius Town Square", ADULT_IDS, listOf(1792 to 3617)),
            UnicornCamp("South-west of Hosidius town square", ADULT_IDS, listOf(1725 to 3559)),
            UnicornCamp("Saltpetre mine, north-east of the Woodcutting Guild", ADULT_IDS, listOf(1696 to 3521)),
            UnicornCamp(
                location = "South of the Woodcutting Guild",
                npcKeys = ADULT_IDS,
                tiles = listOf(1600 to 3453, 1607 to 3437, 1611 to 3447, 1617 to 3438, 1626 to 3450),
            ),
            UnicornCamp(
                location = "Hosidius (foals)",
                npcKeys = listOf(FOAL_ID),
                tiles = listOf(1717 to 3566, 1719 to 3562, 1724 to 3567, 1793 to 3624, 1798 to 3619),
            ),
            // ------------------------------------------------------------------ Varlamore
            UnicornCamp("North Aldarin", ADULT_IDS, listOf(1395 to 2986, 1404 to 2992)),
        )

    /** Every unicorn key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }
}
