package org.alter.plugins.content.npcs.dwarf

/**
 * The fourteen published versions of the `Dwarf` monster, and every place the OSRS Wiki puts one.
 *
 * See [DwarfDrops] for the table, [DwarfPlugin] for the wiring and [DwarfSpawnPlugin] for the
 * placement.
 *
 * ## Why there is no `setCombatDef` in this package
 *
 * The same reason `content/npcs/zombie` gives. All fourteen ids already carry their exact wiki stat
 * block in `data/cfg/npcs/monsterStats.json` - hitpoints, attack, strength, defence, attack speed,
 * attack style and bonuses - and `World.setNpcDefaults` consults that table **only** for npcs no
 * plugin declares a def for. Writing one here to gain a respawn delay would have thrown all of it
 * away and required every number to be retyped by hand, where it could then drift from the table
 * the rest of the server reads.
 *
 * It would also have cost the animations:
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] chooses attack/block/death
 * for exactly the monsters that carry no hand-written def, and `named-combat-media.json`'s `DWARF`
 * entry is already correct for these - 99 / 100 / 102, frame group 410, which is the group every
 * dwarf model in this cache is rigged to.
 *
 * ## Which ids, and which stand where
 *
 * `id1`..`id14` off the infobox, all fourteen checked against this cache: every one is `Dwarf`,
 * size 1, with an `Attack` option, at the combat level the wiki gives it.
 *
 * The `LocLine`s publish **levels, not ids** - "levels = 7, 10, 11" and nothing to say which pin is
 * which. But the version *names* do name places ("Mining Guild (1)", "Worker (3)"), so the ids are
 * pooled by name and then dealt across a location's tiles by
 * [org.alter.plugins.content.npcs.SpawnDealer], which reproduces the published mix without
 * inventing a pin-to-id mapping. Where a location publishes a level, the pool used contains it: the
 * two Mining Guild lines publish 10 and 11, and the three Mining Guild ids are exactly one level 10
 * and two level 11.
 *
 * Excluded, all of them different monsters on their own pages: `Dwarf gang member` (1354-1356),
 * `Black Guard`, `Chaos dwarf`, and the unnamed cache ids 4512 / 5170-5175 / 5904 / 7712-7715 /
 * 7721 / 12708-12712, none of which the wiki's `Dwarf` infobox lists and none of which carries a
 * stat row.
 */
internal data class DwarfVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    val combatLevel: Int,
    val npcKey: String,
    /**
     * Wiki `slayxp`, which is not always the version's hitpoints - `Worker (6)` publishes 16
     * against 18 hitpoints, and `Worker (7)` 13 against 13.
     */
    val slayerXp: Double,
    /** Wiki `respawn`, in game ticks. Blank on twelve of the fourteen; see [Dwarves.RESPAWN_CYCLES]. */
    val respawnCycles: Int = Dwarves.RESPAWN_CYCLES,
)

/** One published `LocLine`: a place, the id pool that stands there, and the tiles. */
internal data class DwarfCamp(
    val location: String,
    val plane: Int,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object Dwarves {
    /**
     * Wiki `respawn1 = 25`, in game ticks - and also `NpcCombatDef.DEFAULT`'s own value, so the
     * twelve versions whose `respawn` field is blank keep exactly what the engine would have given
     * them anyway. Only `Worker (8)` publishes something else, and it says 50.
     */
    const val RESPAWN_CYCLES = 25

    /** `Worker (8)`, the one version with a published respawn of its own. */
    const val WORKER_8_RESPAWN_CYCLES = 50

    /**
     * How far a spawned dwarf wanders from its pin.
     *
     * Dwarves are `aggressive = No` on every version, so this is the only thing that moves one -
     * and the Dwarven Mine's 36 pins are dense enough that a wide radius would shuffle the whole
     * population into one corner of it.
     */
    const val WALK_RADIUS = 4

    // ------------------------------------------------------------------ the id pools

    /** `Standard (Level 10)` and `Standard (Level 20)` - the two plain dwarves. */
    const val STANDARD_10 = "npc.dwarf_290"

    const val STANDARD_20 = "npc.dwarf_292"

    /** `Mining Guild (1)`, `(2)` and `(3)` - levels 11, 11 and 10. */
    val MINING_GUILD = listOf("npc.dwarf_294", "npc.dwarf_295", "npc.dwarf_296")

    /** `Worker (1)`..`Worker (9)` - levels 11, 11, 11, 11, 10, 11, 11, 7 and 11. */
    val WORKERS =
        listOf(
            "npc.dwarf_1401", "npc.dwarf_1402", "npc.dwarf_1403", "npc.dwarf_1404",
            "npc.dwarf_1405", "npc.dwarf_1406", "npc.dwarf_1407", "npc.dwarf_1408",
            "npc.dwarf_8496",
        )

    /** The workers at level 10 and 11 only - every worker but `Worker (8)`, which is level 7. */
    val WORKERS_10_11 = WORKERS - "npc.dwarf_1408"

    val VARIANTS: List<DwarfVariant> =
        listOf(
            DwarfVariant("Standard (Level 10)", 10, STANDARD_10, slayerXp = 16.0),
            DwarfVariant("Standard (Level 20)", 20, STANDARD_20, slayerXp = 26.0),
            DwarfVariant("Mining Guild (1)", 11, "npc.dwarf_294", slayerXp = 16.0),
            DwarfVariant("Mining Guild (2)", 11, "npc.dwarf_295", slayerXp = 16.0),
            DwarfVariant("Mining Guild (3)", 10, "npc.dwarf_296", slayerXp = 12.0),
            DwarfVariant("Worker (1)", 11, "npc.dwarf_1401", slayerXp = 16.0),
            DwarfVariant("Worker (2)", 11, "npc.dwarf_1402", slayerXp = 16.0),
            DwarfVariant("Worker (3)", 11, "npc.dwarf_1403", slayerXp = 16.0),
            DwarfVariant("Worker (4)", 11, "npc.dwarf_1404", slayerXp = 16.0),
            DwarfVariant("Worker (5)", 10, "npc.dwarf_1405", slayerXp = 12.0),
            DwarfVariant("Worker (6)", 11, "npc.dwarf_1406", slayerXp = 16.0),
            DwarfVariant("Worker (7)", 11, "npc.dwarf_1407", slayerXp = 13.0),
            DwarfVariant(
                "Worker (8)",
                7,
                "npc.dwarf_1408",
                slayerXp = 10.0,
                respawnCycles = WORKER_8_RESPAWN_CYCLES,
            ),
            DwarfVariant("Worker (9)", 11, "npc.dwarf_8496", slayerXp = 18.0),
        )

    /**
     * Every `LocLine` on the page. All 105 tiles are checked to be standable by `BestiaryVerify`
     * before anyone trusts them - most of these are inside a mine, where a published pin landing in
     * a rock face is common.
     */
    val CAMPS: List<DwarfCamp> =
        listOf(
            // ------------------------------------------------------------------ Asgarnia
            DwarfCamp(
                location = "Ice Mountain",
                plane = 0,
                // Published as levels 7, 10 and 11: one id per level, taken from the standard and
                // worker pools, since no version is named for this hillside.
                npcKeys = listOf(STANDARD_10, "npc.dwarf_1408", "npc.dwarf_1401"),
                tiles =
                    listOf(
                        3008 to 3436, 3015 to 3435, 3016 to 3450, 3017 to 3447, 3016 to 3448,
                        3021 to 3437, 3021 to 3447, 3029 to 3445, 3009 to 3458, 3023 to 3458,
                        3024 to 3463, 3030 to 3464, 2998 to 3445, 2999 to 3453,
                    ),
            ),
            DwarfCamp(
                location = "Surface of Mining Guild",
                plane = 0,
                npcKeys = MINING_GUILD,
                tiles = listOf(3012 to 3337, 3019 to 3333, 3025 to 3347),
            ),
            DwarfCamp(
                location = "Falador Mining Guild (1st floor)",
                plane = 1,
                npcKeys = MINING_GUILD,
                tiles = listOf(3010 to 3341, 3017 to 3348),
            ),
            DwarfCamp(
                location = "Dwarven Mine",
                plane = 0,
                npcKeys = listOf(STANDARD_10),
                tiles =
                    listOf(
                        3044 to 9830, 3048 to 9823, 3050 to 9813, 3050 to 9827, 3051 to 9817,
                        3053 to 9826, 3054 to 9821, 2984 to 9807, 2992 to 9844, 2994 to 9826,
                        2995 to 9805, 2998 to 9841, 2999 to 9817, 3000 to 9809, 3000 to 9826,
                        3001 to 9796, 3001 to 9844, 3002 to 9829, 3005 to 9799, 3010 to 9813,
                        3013 to 9811, 3014 to 9814, 3017 to 9819, 3017 to 9851, 3018 to 9809,
                        3018 to 9814, 3018 to 9835, 3019 to 9845, 3020 to 9849, 3022 to 9851,
                        3023 to 9820, 3025 to 9828, 3026 to 9833, 3028 to 9816, 3034 to 9848,
                        3037 to 9844,
                    ),
            ),
            DwarfCamp(
                location = "Black Knights' Base (jail)",
                plane = 0,
                npcKeys = listOf(STANDARD_10),
                tiles = listOf(2930 to 9699, 2931 to 9703),
            ),
            DwarfCamp(
                location = "White Wolf Mountain Tunnels",
                plane = 0,
                npcKeys = listOf(STANDARD_20),
                tiles =
                    listOf(
                        2860 to 9874, 2860 to 9877, 2862 to 9874, 2862 to 9877,
                        2867 to 9879, 2868 to 9874, 2869 to 9879, 2870 to 9874,
                    ),
            ),
            // ------------------------------------------------------------------ Kandarin
            DwarfCamp(
                location = "South of Yanille",
                plane = 0,
                npcKeys = listOf(STANDARD_10),
                tiles = listOf(2596 to 3056, 2600 to 3062, 2605 to 3057, 2608 to 3061),
            ),
            // ------------------------------------------------------------------ Fremennik
            DwarfCamp(
                location = "Keldagrim",
                plane = 0,
                npcKeys = listOf(STANDARD_20),
                tiles =
                    listOf(
                        2854 to 10125, 2866 to 10126, 2872 to 10118, 2826 to 10152,
                        2837 to 10148, 2854 to 10164, 2861 to 10167,
                    ),
            ),
            // ------------------------------------------------------------------ Kourend
            DwarfCamp(
                location = "Lovakengj",
                plane = 0,
                npcKeys = WORKERS,
                tiles =
                    listOf(
                        1527 to 3831, 1530 to 3830, 1520 to 3810, 1487 to 3810, 1537 to 3721,
                        1480 to 3767, 1482 to 3756, 1498 to 3762, 1503 to 3770, 1506 to 3760,
                        1507 to 3753, 1509 to 3770, 1512 to 3769, 1541 to 3806, 1541 to 3810,
                        1543 to 3780, 1548 to 3808, 1554 to 3796, 1555 to 3785, 1485 to 3832,
                        1497 to 3832,
                    ),
            ),
            DwarfCamp(
                location = "Lovakengj (1st floor buildings)",
                plane = 1,
                npcKeys = WORKERS_10_11,
                tiles =
                    listOf(
                        1429 to 3796, 1507 to 3755, 1511 to 3768, 1544 to 3781,
                        1555 to 3785, 1555 to 3795, 1517 to 3834, 1444 to 3784,
                    ),
            ),
        )

    /** Every dwarf key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.map { it.npcKey } }
}
