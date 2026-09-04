package org.alter.plugins.content.npcs.ogre

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.SeedTableId
import org.alter.plugins.content.npcs.WeightedDrop

/**
 * The three published versions of `Ogre` and every place the OSRS Wiki puts one.
 *
 * See [OgrePlugin] for the wiring and [OgreSpawnPlugin] for the placement. Stats come from
 * `data/cfg/npcs/monsterStats.json` - no `setCombatDef` here, for the reason
 * `content/npcs/mossgiant` records - and animations from the existing `OGRE` entry in
 * `named-combat-media.json`, 359 / 360 / 361, which the second bestiary audit checked and left
 * alone.
 *
 * The `Ogre chieftain` is a **different monster with a different page** and stays in
 * `content/npcs/dungeon`, where `content/areas/ogreenclave` already places six of them.
 *
 * ## The drop table is almost entirely `Nothing`
 *
 * 109 of its 128 slots, published as an explicit `Nothing` row. The other 19 are the uncommon seed
 * table, and there are no ordinary item rows at all - an ogre's value is its big bones and its
 * tertiaries. That is unusual enough to be worth stating: this is not a truncated table.
 */
internal data class OgreVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    val combatLevel: Int,
    val npcKeys: List<String>,
    /** Wiki `respawn`, in game ticks, which are this engine's cycles one-for-one. */
    val respawnCycles: Int,
    val slayerXp: Double,
)

/** One published `LocLine`: a place, the ids that stand there, and the tiles. */
internal data class OgreCamp(
    val location: String,
    val plane: Int,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object Ogres {
    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long an ogre stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`, stated
     * because a def built from `monsterStats.json` starts with a **0** timer, which
     * `NpcAggroPlugin` reads as "stop being aggressive".
     *
     * The level 53 ogre's `aggressive` field is `Yes (depends on location)` and the page never says
     * which locations. Read as plain `Yes`, with the engine's own combat-level check left in place -
     * which is itself a location-shaped filter, since it stops an ogre bothering anybody above
     * combat level 106, and that is most of who walks through Gu'Tanoth.
     */
    const val AGGRO_TIMER = 1000

    /** Ogres are size 2; a modest radius keeps the Chaos Druid Tower corridors walkable. */
    const val WALK_RADIUS = 4

    /** `id1` - the ordinary level 53 ogre. */
    val LEVEL_53_IDS = listOf("npc.ogre", "npc.ogre_2095", "npc.ogre_2096")

    /** `id2` - the God Wars Dungeon version, level 58. Defined but not placed; see [CAMPS]. */
    val GOD_WARS_IDS = listOf("npc.ogre_2233")

    /** `id3` - the level 63 Combat Training Camp ogre. */
    val LEVEL_63_IDS = listOf("npc.ogre_1153")

    val VARIANTS: List<OgreVariant> =
        listOf(
            OgreVariant("Ogre (level 53)", 53, LEVEL_53_IDS, respawnCycles = 30, slayerXp = 60.0),
            OgreVariant("Ogre (level 58, God Wars)", 58, GOD_WARS_IDS, respawnCycles = 25, slayerXp = 60.0),
            OgreVariant("Ogre (level 63, Combat Training Camp)", 63, LEVEL_63_IDS, respawnCycles = 50, slayerXp = 60.0),
        )

    /** Wiki tertiary, Wilderness only. */
    const val LOOTING_BAG_ONE_IN = 3

    /** Wiki tertiary. */
    const val ENSOULED_HEAD_ONE_IN = 30

    /** Wiki tertiary. */
    const val LONG_BONE_ONE_IN = 400

    /**
     * Wiki tertiary, published as **1/5012.5** - a non-integer rate, so it is a Double and rolled
     * against `World.randomDouble` rather than through `World.chance`, the same way
     * `content/npcs/mossgiant` handles the identical row.
     */
    const val CURVED_BONE_ONE_IN = 5012.5

    /**
     * The whole main table: nineteen slots of uncommon seed table and 109 of `Nothing`, summing to
     * the published 128.
     *
     * **`Ogre ribs` at 1/4 is not here.** Its footnote says "dropped only during Rag and Bone Man
     * II", a quest this server does not have. A 1/4 drop is far too common to hand out
     * unconditionally just because its condition is unbuilt, so it drops never rather than always -
     * the same call `content/npcs/mossgiant` makes about the moss giant bone.
     *
     * **The ecumenical key** (`{{DropsLineEcumenical}}`) is not here either: it is a God Wars
     * Dungeon drop, and the God Wars ogre is not placed.
     */
    val TABLE =
        MonsterDropTable(
            denominator = 128,
            seedWeight = 19,
            seedTable = SeedTableId.UNCOMMON,
            rows = listOf(WeightedDrop(item = null, weight = 109)),
        )

    /**
     * Every published `LocLine` this rev-228 cache can hold, with ids dealt across the camps by
     * [org.alter.plugins.content.npcs.SpawnDealer].
     *
     * Ogres are size 2, so a pin needs a standable 2x2 rather than a standable tile;
     * `BestiaryVerify` checks the whole footprint.
     *
     * **Not here**: the **God Wars Dungeon** and **Wilderness God Wars Dungeon** lines, whose
     * aggression the page conditions on a Bandos-affiliated item - a mechanic that does not exist
     * here - and whose areas have no killcount door built.
     */
    val CAMPS: List<OgreCamp> =
        listOf(
            // ------------------------------------------------------------------- Feldip Hills
            OgreCamp(
                location = "Feldip Hills",
                plane = 0,
                npcKeys = LEVEL_53_IDS,
                tiles =
                    listOf(
                        2584 to 2967, 2585 to 2977, 2592 to 2965, 2504 to 2965, 2504 to 2984,
                        2551 to 2955, 2552 to 2961, 2608 to 2980, 2609 to 2990, 2614 to 2998,
                        2568 to 2991, 2574 to 2988, 2520 to 2973, 2525 to 2986, 2533 to 2977,
                        2536 to 2975, 2539 to 2990, 2549 to 2979,
                    ),
            ),
            OgreCamp(
                location = "Gu'Tanoth",
                plane = 0,
                npcKeys = LEVEL_53_IDS,
                tiles =
                    listOf(
                        2510 to 3044, 2520 to 3046, 2522 to 3055, 2523 to 3057, 2526 to 3039,
                        2529 to 3032, 2513 to 3026, 2561 to 3020, 2567 to 3046, 2571 to 3027,
                        2572 to 3031, 2575 to 3024, 2578 to 3031, 2550 to 3043, 2550 to 3046,
                        2553 to 3047, 2539 to 3018, 2541 to 3016,
                    ),
            ),
            OgreCamp(
                location = "West of Yanille",
                plane = 0,
                npcKeys = LEVEL_53_IDS,
                tiles =
                    listOf(
                        2496 to 3092, 2497 to 3089, 2500 to 3092, 2500 to 3097, 2502 to 3114,
                        2503 to 3095, 2504 to 3133, 2505 to 3111, 2507 to 3108, 2508 to 3119,
                        2509 to 3087, 2510 to 3084, 2513 to 3087, 2514 to 3081,
                    ),
            ),
            OgreCamp(
                location = "Gnome Maze",
                plane = 0,
                npcKeys = LEVEL_53_IDS,
                tiles = listOf(2554 to 3192),
            ),
            // ----------------------------------------------------------------------- dungeons
            OgreCamp(
                location = "Witchaven Dungeon",
                plane = 0,
                npcKeys = LEVEL_53_IDS,
                tiles = listOf(2719 to 9715, 2720 to 9702, 2723 to 9707, 2724 to 9713, 2719 to 9668, 2722 to 9669),
            ),
            OgreCamp(
                location = "Clock Tower Dungeon",
                plane = 0,
                npcKeys = LEVEL_53_IDS,
                tiles = listOf(2585 to 9608, 2586 to 9614, 2588 to 9606),
            ),
            OgreCamp(
                location = "Chaos Druid Tower Dungeon",
                plane = 0,
                npcKeys = LEVEL_53_IDS,
                tiles =
                    listOf(
                        2581 to 9738, 2581 to 9742, 2584 to 9734, 2585 to 9738,
                        2587 to 9733, 2587 to 9740, 2590 to 9737,
                    ),
            ),
            OgreCamp(
                location = "Underground Pass",
                plane = 0,
                npcKeys = LEVEL_53_IDS,
                tiles = listOf(2387 to 9685, 2389 to 9683),
            ),
            // ------------------------------------------------------------- the level 63 ogres
            OgreCamp(
                location = "Combat Training Camp",
                plane = 0,
                npcKeys = LEVEL_63_IDS,
                tiles =
                    listOf(
                        2523 to 3373, 2523 to 3376, 2526 to 3373, 2526 to 3376,
                        2529 to 3373, 2529 to 3376, 2531 to 3376, 2532 to 3373,
                    ),
            ),
        )

    /** Every ogre key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }
}
