package org.alter.plugins.content.npcs.ghost

/**
 * The three published versions of the common `Ghost` - levels 19, 76 and 77 - and every place the
 * OSRS Wiki puts one.
 *
 * See [GhostPlugin] for the wiring and [GhostSpawnPlugin] for the placement. There is no
 * `*Drops.kt`: a ghost's entire published loot is two tertiaries. It does not even drop bones,
 * which is the page's own summary of why nobody trains on them.
 *
 * ## Why there is no `setCombatDef` in this package
 *
 * The reason `content/npcs/zombie` and `content/npcs/dwarf` give. All 28 ids already carry their
 * exact wiki stat block in `data/cfg/npcs/monsterStats.json` - including the `SPECTRAL` and
 * `UNDEAD` species the salve amulet and Crumble Undead key off, and the 50% Air elemental weakness
 * added in the 29 May 2024 "Project Rebalance: Combat Changes" update - and `World.setNpcDefaults`
 * reads that table **only** for npcs no plugin declares a def for.
 *
 * ## There are two ghost rigs in this cache, and only one was reachable
 *
 * Ghosts come in two models with two different animation sets, and `named-combat-media.json` is
 * keyed by **name**, so all 28 were getting the same one:
 *
 * - **Frame group 1438** - models 21143-21149, stand 5530 / walk 5531, animations 5532 / 5533 /
 *   5534. That is the `GHOST` entry, and it is right for 25 of the 28 ids.
 * - **Frame group 1439** - model 21154, stand 5538 / walk 5539, animations 5540 / 5541 / 5542.
 *   That is the `ALT_GHOST` entry, which has been in the file all along and which nothing could ever
 *   select, because every npc carrying that model is also called "Ghost".
 *
 * Three of the ids this package spawns are on the second rig - [ALT_RIG_IDS] - and would have played
 * animations belonging to a model they are not built from. So
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] gained an id-keyed override
 * consulted ahead of the name map, and `ALT_GHOST` is live for the first time.
 *
 * ## Which ids
 *
 * `id1`..`id3` off the infobox, and all 28 checked against this cache: every one is `Ghost`, size 1,
 * with an `Attack` option, at the level the wiki gives it.
 *
 * The level 19 list skips **94, 96 and 98**, which do exist in this cache as `Ghost`. As with the
 * zombie ids the wiki omits, an unlisted id means "no published drop version", not "different
 * monster" - but a ghost's loot is two tertiaries either way, so unlike the zombies there is nothing
 * to be gained by adopting them, and they are left out to match the page exactly. Also excluded:
 * 3975-3979 (`Ghost (Melzar's Maze)`, a separate page), 5370, 12254, and every named ghost
 * (`Ghost villager`, `Ghost sailor` and the rest), none of which the infobox lists.
 */
internal data class GhostVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    val combatLevel: Int,
    val npcKeys: List<String>,
    val slayerXp: Double,
)

/** One published `LocLine`: a place, its plane, the ids that stand there, and the tiles. */
internal data class GhostCamp(
    val location: String,
    val plane: Int,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object Ghosts {
    /** Wiki `respawn = 40` on all three versions, in game ticks, used as published. */
    const val RESPAWN_CYCLES = 40

    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a ghost stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`.
     *
     * This has to be stated rather than left alone. A def built from `monsterStats.json` starts from
     * `NpcCombatDef.DEFAULT`, whose `aggressiveTimer` is **0**, and `NpcAggroPlugin`'s default
     * aggressiveness reads a zero timer as "stop being aggressive" - so an aggression radius alone
     * never fires once. The zombie package found this the hard way.
     *
     * Left at the ordinary timer rather than `alwaysAggro()`, because the same function's
     * `p.combatLevel <= npcLvl * 2` check is exactly the real rule that keeps a level 19 ghost from
     * bothering anyone above combat level 38 - and, at the other end, lets a level 77 one in the
     * Sepulchre of Death bother almost anybody.
     */
    const val AGGRO_TIMER = 1000

    /**
     * How far a spawned ghost wanders from its pin.
     *
     * Kept modest deliberately. These are dense populations - seventeen of them share the Catacombs
     * of Kourend and fourteen the Taverley Dungeon - and a wide radius on an aggressive monster in a
     * corridor turns a crypt into a wall of ghosts following the player from three rooms away.
     */
    const val WALK_RADIUS = 4

    // ------------------------------------------------------------------ the ids

    /** `id1` - the level 19 ghost, twenty ids. */
    val LEVEL_19_IDS =
        listOf(85, 86, 87, 88, 89, 90, 91, 92, 93, 95, 97, 99, 472, 473, 474, 505, 506, 507, 7263, 7264)
            .map { if (it == 85) "npc.ghost" else "npc.ghost_$it" }

    /** `id2` - the level 76 ghost, Stronghold of Security. */
    val LEVEL_76_IDS = listOf(2531, 2532, 2533, 2534).map { "npc.ghost_$it" }

    /** `id3` - the level 77 ghost, Stronghold of Security. */
    val LEVEL_77_IDS = listOf(2527, 2528, 2529, 2530).map { "npc.ghost_$it" }

    /**
     * The ids built from model 21154, whose animations are `ALT_GHOST`'s 5540 / 5541 / 5542 rather
     * than `GHOST`'s - see the file doc.
     *
     * Read out of this cache rather than guessed: these are exactly the ids among the 28 whose
     * `standAnim` is 5538 instead of 5530.
     */
    val ALT_RIG_IDS = listOf("npc.ghost", "npc.ghost_93", "npc.ghost_2527")

    val VARIANTS: List<GhostVariant> =
        listOf(
            GhostVariant("Ghost (level 19)", 19, LEVEL_19_IDS, slayerXp = 25.0),
            GhostVariant("Ghost (level 76)", 76, LEVEL_76_IDS, slayerXp = 75.0),
            GhostVariant("Ghost (level 77)", 77, LEVEL_77_IDS, slayerXp = 80.0),
        )

    /**
     * Every `LocLine` on the page.
     *
     * Ids are dealt across the camps by [org.alter.plugins.content.npcs.SpawnDealer]: within a
     * location the wiki does not say which id stands on which pin - they differ only in the shade of
     * the model - so dealing reproduces the published mix without inventing a mapping.
     *
     * This is the species that forced the dealer to key its cursor on the id **pool** rather than on
     * the camp. The level 19 ghost has twenty ids and its locations have two, three and five pins; a
     * per-camp count would have stood npc 85 in all twelve of them and left five ghosts nowhere.
     *
     * Every tile is checked to be standable by `BestiaryVerify`, which matters here more than
     * anywhere: most of these are in crypts, dungeons and a manor's upper floor, where a published
     * pin landing in a wall or on a floor that does not exist at that height is common.
     */
    val CAMPS: List<GhostCamp> =
        listOf(
            // ------------------------------------------------------------------ Misthalin
            GhostCamp(
                location = "Draynor Manor (1st floor)",
                plane = 1,
                npcKeys = LEVEL_19_IDS,
                tiles = listOf(3094 to 3356, 3098 to 3359, 3101 to 3356, 3108 to 3358, 3123 to 3356),
            ),
            GhostCamp(
                location = "Varrock Sewers",
                plane = 0,
                npcKeys = LEVEL_19_IDS,
                tiles = listOf(3239 to 9915, 3241 to 9913, 3243 to 9915),
            ),
            /*
             * The one place the wiki gives both high-level versions, so both id sets are dealt
             * across it - which makes the Sepulchre the only room where a level 76 and a level 77
             * ghost stand side by side.
             */
            GhostCamp(
                location = "Stronghold of Security: Sepulchre of Death",
                plane = 0,
                npcKeys = LEVEL_76_IDS + LEVEL_77_IDS,
                /*
                 * The published line has two clusters and only the eastern one is real in this
                 * cache. Its twenty-one western pins (1953-1974, 4936-4958) sit in mapsquare 30_77,
                 * which carries 437 painted tiles in total and none under any of them - the nearest
                 * floor is eighteen tiles west. The eleven eastern pins are in 31_76, which is 3,078
                 * tiles painted, and every one of them stands. Only those are wired.
                 */
                tiles =
                    listOf(
                        1996 to 4906, 2000 to 4901, 2000 to 4912, 2001 to 4909, 2002 to 4906,
                        2002 to 4912, 2002 to 4914, 2004 to 4905, 2004 to 4916, 2005 to 4906,
                    ),
            ),
            // ------------------------------------------------------------------ Wilderness
            GhostCamp(
                location = "The Forgotten Cemetery",
                plane = 0,
                npcKeys = LEVEL_19_IDS,
                tiles =
                    listOf(
                        2962 to 3755, 2968 to 3749, 2968 to 3761,
                        2985 to 3749, 2985 to 3761, 2990 to 3755,
                    ),
            ),
            GhostCamp(
                location = "North-east of the Chaos Temple",
                plane = 0,
                npcKeys = LEVEL_19_IDS,
                tiles =
                    listOf(
                        3272 to 3664, 3278 to 3663, 3279 to 3654,
                        3282 to 3662, 3288 to 3650, 3290 to 3653,
                    ),
            ),
            GhostCamp(
                location = "North-west of Ferox Enclave",
                plane = 0,
                npcKeys = LEVEL_19_IDS,
                tiles = listOf(3088 to 3700, 3092 to 3692, 3097 to 3700, 3105 to 3696, 3113 to 3689),
            ),
            // ------------------------------------------------------------------ Asgarnia
            GhostCamp(
                location = "Taverley Dungeon",
                plane = 0,
                npcKeys = LEVEL_19_IDS,
                tiles =
                    listOf(
                        2900 to 9819, 2907 to 9818, 2912 to 9820, 2920 to 9818, 2888 to 9849,
                        2894 to 9849, 2901 to 9848, 2905 to 9851, 2909 to 9848, 2915 to 9851,
                        2920 to 9848, 2933 to 9838, 2936 to 9829, 2938 to 9836,
                    ),
            ),
            // ------------------------------------------------------------------ Kandarin
            GhostCamp(
                location = "West Ardougne",
                plane = 0,
                npcKeys = LEVEL_19_IDS,
                tiles = listOf(2501 to 3289, 2501 to 3294),
            ),
            // ------------------------------------------------------------------ Kourend
            GhostCamp(
                location = "North-east of the dense essence mine",
                plane = 0,
                npcKeys = LEVEL_19_IDS,
                tiles = listOf(1826 to 3864, 1817 to 3887, 1828 to 3885, 1774 to 3896, 1790 to 3892),
            ),
            GhostCamp(
                location = "Catacombs of Kourend",
                plane = 0,
                npcKeys = LEVEL_19_IDS,
                tiles =
                    listOf(
                        1686 to 10062, 1687 to 10060, 1688 to 10062, 1689 to 10065, 1690 to 10063,
                        1691 to 10061, 1694 to 10063, 1696 to 10065, 1698 to 10062, 1699 to 10065,
                        1665 to 10022, 1666 to 10024, 1659 to 10023, 1661 to 10024, 1663 to 10021,
                        1663 to 10023, 1663 to 10026,
                    ),
            ),
            // ------------------------------------------------------------------ Tirannwn
            GhostCamp(
                location = "Mynydd",
                plane = 0,
                npcKeys = LEVEL_19_IDS,
                tiles = listOf(2164 to 3431, 2167 to 3429, 2167 to 3432, 2168 to 3427, 2170 to 3428),
            ),
            GhostCamp(
                location = "Death Altar",
                plane = 0,
                npcKeys = LEVEL_19_IDS,
                tiles =
                    listOf(
                        2190 to 4820, 2194 to 4824, 2198 to 4825, 2206 to 4824, 2210 to 4824,
                        2216 to 4829, 2218 to 4818, 2218 to 4837, 2218 to 4843, 2223 to 4848,
                        2225 to 4850, 2226 to 4855, 2227 to 4819, 2188 to 4855, 2192 to 4852,
                        2193 to 4845, 2197 to 4849,
                    ),
            ),
            // ------------------------------------------------------------------ Desert
            GhostCamp(
                location = "Ruins of Uzer Temple basement",
                plane = 0,
                npcKeys = LEVEL_19_IDS,
                tiles = listOf(2712 to 4898, 2712 to 4908, 2714 to 4892, 2730 to 4907, 2731 to 4893),
            ),
        )

    /** Every ghost key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }
}
