package org.alter.plugins.content.npcs.mossgiant

/**
 * The three published versions of `Moss giant` and every place the OSRS Wiki puts one.
 *
 * See [MossGiantDrops] for the table, [MossGiantPlugin] for the wiring and [MossGiantSpawnPlugin]
 * for the placement.
 *
 * ## Why there is no `setCombatDef` in this package
 *
 * The reason `content/npcs/zombie` and `content/npcs/dwarf` give: every id below already carries its
 * exact wiki stat block in `data/cfg/npcs/monsterStats.json` - the 30/30/30 combat levels, the
 * 6-cycle attack speed, the +33 attack and +31 strength bonuses, and the 50% Fire elemental weakness
 * the page leads with - and `World.setNpcDefaults` reads that table **only** for npcs no plugin
 * declares a def for.
 *
 * It would also have cost the animations: `named-combat-media.json`'s `MOSS_GIANT` entry is already
 * correct - 4658 / 4657 / 4659, frame group 1211, which is what every moss giant in this cache is
 * observed playing.
 *
 * ## Which ids
 *
 * `id1`..`id3` off the infobox, checked against this cache: all ten are `Moss giant`, size 2, with
 * an `Attack` option, at the level the wiki gives them.
 *
 * Two exclusions:
 *
 * - **14422**, which `id1` lists and which **does not exist in this rev-228 cache** - the highest
 *   `Moss giant` id here is 12847. It postdates the cache, so there is nothing to spawn.
 * - **8736**, which is in this cache as `Moss Giant` with 120 hitpoints and doubled bonuses. That is
 *   `Moss Giant (Iorwerth Dungeon)`, the level 84 version, which is its own page with its own drop
 *   table - the page even points at it. It belongs to a Tirannwn package, not this one.
 */
internal data class MossGiantVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    val combatLevel: Int,
    val npcKeys: List<String>,
    val slayerXp: Double,
)

/** One published `LocLine`: a place, the ids that stand there, and the tiles. */
internal data class MossGiantCamp(
    val location: String,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object MossGiants {
    /** Wiki `respawn = 30` on all three versions, in game ticks, used as published. */
    const val RESPAWN_CYCLES = 30

    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a moss giant stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`.
     *
     * This has to be stated rather than left alone. A def built from `monsterStats.json` starts from
     * `NpcCombatDef.DEFAULT`, whose `aggressiveTimer` is **0**, and `NpcAggroPlugin`'s default
     * aggressiveness reads a zero timer as "stop being aggressive" - so an aggression radius alone
     * never fires once. The zombie package found this the hard way.
     *
     * Left at the ordinary timer rather than `alwaysAggro()`, because the same function's
     * `p.combatLevel <= npcLvl * 2` check is exactly the real rule that lets anyone above combat
     * level 84 walk through the Varrock Sewers unbothered - which is most of the people who go
     * there.
     */
    const val AGGRO_TIMER = 1000

    /**
     * How far a spawned moss giant wanders from its pin.
     *
     * Kept modest: these are size 2 and aggressive, and twenty of them share Brimhaven Dungeon.
     */
    const val WALK_RADIUS = 4

    /** `id1` less 14422 - see the file doc. */
    val LEVEL_42_IDS =
        listOf("npc.moss_giant", "npc.moss_giant_2091", "npc.moss_giant_2092", "npc.moss_giant_2093", "npc.moss_giant_7262")

    /** `id2` - the level 48 Pirates' Cove version. */
    val LEVEL_48_IDS = listOf("npc.moss_giant_3851", "npc.moss_giant_3852")

    /** `id3` - the Varlamore recolours, level 42 with the same stats. */
    val VARLAMORE_IDS =
        listOf("npc.moss_giant_12844", "npc.moss_giant_12845", "npc.moss_giant_12846", "npc.moss_giant_12847")

    val VARIANTS: List<MossGiantVariant> =
        listOf(
            MossGiantVariant("Moss giant (level 42)", 42, LEVEL_42_IDS, slayerXp = 60.0),
            MossGiantVariant("Moss giant (level 48)", 48, LEVEL_48_IDS, slayerXp = 85.0),
            MossGiantVariant("Moss giant (level 42, Varlamore)", 42, VARLAMORE_IDS, slayerXp = 60.0),
        )

    /**
     * Every `LocLine` on the page bar four, with ids dealt across the camps by
     * [org.alter.plugins.content.npcs.SpawnDealer] - within a location the wiki does not say which
     * id stands on which pin, and they differ only in the model's moss. Three of these camps have
     * fewer pins than the five level 42 ids, which is the case the dealer's shared cursor exists
     * for.
     *
     * Moss giants are size 2, so a pin needs a standable 2x2 rather than a standable tile -
     * `BestiaryVerify` checks the whole footprint, which matters in Brimhaven Dungeon and the Varrock
     * Sewers where a pin an inch into a wall would leave a giant nobody can reach.
     *
     * **The four omissions**, each for a stated reason:
     *
     * - **North-east of Prifddinas**, which the page's own footnote says "cannot actually be fought,
     *   as this particular giant is only visible when the player is inside the walls of Prifddinas".
     *   Spawning it would put a giant in a wall.
     * - **Laguna Aurorae** and **Tonali Cavern**, later Varlamore content that postdates this
     *   rev-228 cache - there is no map there to stand on. (The Laguna line is also the one written
     *   `1220,2804` rather than `x:1220,y:2804`.)
     * - **Shaman Caves**, listed as a bullet rather than a `LocLine` because a moss giant there is a
     *   random encounter from smashing a barrel, not a spawn.
     */
    val CAMPS: List<MossGiantCamp> =
        listOf(
            // ------------------------------------------------------------------ Misthalin
            MossGiantCamp(
                location = "Varrock Sewers",
                npcKeys = LEVEL_42_IDS,
                tiles =
                    listOf(
                        3155 to 9903, 3156 to 9906, 3158 to 9895, 3158 to 9898, 3158 to 9904,
                        3159 to 9901, 3163 to 9877, 3164 to 9880, 3166 to 9883, 3167 to 9880,
                    ),
            ),
            MossGiantCamp(
                location = "Isle of Souls",
                npcKeys = LEVEL_42_IDS,
                tiles =
                    listOf(
                        2222 to 2817, 2223 to 2821, 2226 to 2819,
                        2230 to 2821, 2232 to 2816, 2227 to 2815,
                    ),
            ),
            // ------------------------------------------------------------------ Wilderness
            MossGiantCamp(
                location = "Wilderness Pond",
                npcKeys = LEVEL_42_IDS,
                tiles =
                    listOf(
                        3139 to 3818, 3140 to 3805, 3141 to 3826,
                        3144 to 3822, 3145 to 3812, 3147 to 3805,
                    ),
            ),
            // ------------------------------------------------------------------ Karamja
            MossGiantCamp(
                location = "Moss Giant Island, west of Brimhaven",
                npcKeys = LEVEL_42_IDS,
                tiles = listOf(2691 to 3215, 2692 to 3204, 2695 to 3216, 2698 to 3206, 2699 to 3212),
            ),
            MossGiantCamp(
                location = "Crandor",
                npcKeys = LEVEL_42_IDS,
                tiles = listOf(2827 to 3246, 2831 to 3243, 2834 to 3242),
            ),
            MossGiantCamp(
                location = "Brimhaven Dungeon",
                npcKeys = LEVEL_42_IDS,
                tiles =
                    listOf(
                        2642 to 9492, 2646 to 9484, 2647 to 9487, 2682 to 9573, 2673 to 9592,
                        2680 to 9594, 2636 to 9544, 2640 to 9583, 2647 to 9554, 2648 to 9537,
                        2651 to 9564, 2653 to 9574, 2654 to 9587, 2659 to 9548, 2663 to 9556,
                        2670 to 9544, 2680 to 9544, 2636 to 9528, 2642 to 9518, 2645 to 9532,
                    ),
            ),
            // ------------------------------------------------------------------ Kandarin
            MossGiantCamp(
                location = "West of the Fishing Guild",
                npcKeys = LEVEL_42_IDS,
                tiles = listOf(2549 to 3408, 2554 to 3401, 2554 to 3409, 2556 to 3406),
            ),
            // ------------------------------------------------------------------ Fremennik
            MossGiantCamp(
                location = "Pirates' Cove",
                npcKeys = LEVEL_48_IDS,
                tiles =
                    listOf(
                        2195 to 3808, 2196 to 3822, 2197 to 3805, 2197 to 3813, 2201 to 3812,
                        2202 to 3824, 2203 to 3804, 2207 to 3813, 2211 to 3818, 2214 to 3819,
                    ),
            ),
            // ------------------------------------------------------------------ Kourend
            MossGiantCamp(
                location = "Kourend Woodland",
                npcKeys = LEVEL_42_IDS,
                tiles = listOf(1513 to 3505, 1516 to 3501, 1519 to 3508, 1522 to 3504),
            ),
            MossGiantCamp(
                location = "Catacombs of Kourend",
                npcKeys = LEVEL_42_IDS,
                tiles =
                    listOf(
                        1685 to 10049, 1688 to 10050, 1686 to 10030, 1687 to 10034, 1687 to 10044,
                        1689 to 10047, 1690 to 10036, 1691 to 10030, 1693 to 10033,
                        // The line's four western pins - 1655,10012 / 1658,10014 / 1662,10014 /
                        // 1638,10017 - are all `BLOCK_WALK` across their whole 2x2 footprint in this
                        // cache, with the nearest clear 2x2 four to six tiles away in each case.
                        // Dropped rather than nudged: moving a giant that far is inventing a spawn,
                        // and the nine pins above are the same room's eastern half.
                    ),
            ),
            MossGiantCamp(
                location = "Giants' Den",
                npcKeys = LEVEL_42_IDS,
                tiles =
                    listOf(
                        1419 to 9862, 1419 to 9869, 1424 to 9864, 1425 to 9870, 1429 to 9862,
                        1446 to 9865, 1452 to 9862, 1454 to 9866, 1459 to 9867, 1460 to 9862,
                    ),
            ),
            // ------------------------------------------------------------------ Varlamore
            MossGiantCamp(
                location = "West of Ralos' Rise",
                npcKeys = VARLAMORE_IDS,
                tiles =
                    listOf(
                        1394 to 3123, 1398 to 3108, 1393 to 3111,
                        1398 to 3120, 1389 to 3117, 1396 to 3115,
                    ),
            ),
        )

    /** Every moss giant key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }
}
