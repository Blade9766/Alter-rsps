package org.alter.plugins.content.npcs.hellhound

/**
 * The three published versions of `Hellhound` and every place the OSRS Wiki puts one.
 *
 * See [HellhoundDrops] for the tables, [HellhoundPlugin] for the wiring and [HellhoundSpawnPlugin]
 * for the placement.
 *
 * ## Where the stats come from
 *
 * `data/cfg/npcs/monsterStats.json`, not this file - the same call every package in `content/npcs`
 * has made since `content/npcs/zombie`. Every id below already carries its exact wiki stat block
 * there: 116 hitpoints at 105/104/102 for the level 122, 116 at 107/116/106 for the God Wars
 * version, 150 at 105/120/102 for the Wilderness Slayer Cave one, all at attack speed 4, all
 * `DEMON`, all with the 50% Water elemental weakness the page leads with. `World.setNpcDefaults`
 * reads that table for any npc no plugin declares a def for, so declaring one here would *replace*
 * those numbers and also drop the hellhound off
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin]'s media path.
 *
 * ## Moved out of `content/npcs/dungeon`
 *
 * Ids 104, 105 and 7256 used to be one `DungeonMonster` row, wired with a hand-written combat def
 * so that Taverley Dungeon had something real standing in it. That row is gone and this package
 * owns them now, for two reasons that bite at once:
 *
 * - Two `onNpcDeath` handlers for one id do not stack - `PluginRepository.bindNpcDeath` overwrites -
 *   so whichever plugin happened to load last would silently decide the drop table.
 * - The dungeon row covered three of the five published ids and one of the six locations this cache
 *   can hold. The God Wars and Wilderness Slayer Cave versions, with their different respawn,
 *   Slayer experience and tertiaries, had nowhere to live.
 *
 * `content/areas/taverleydungeon/spawns` lost its thirteen hellhound `spawnNpc` lines in the same
 * change; those pins are the [TAVERLEY] camp below, at the wiki's own coordinates rather than the
 * hand-picked ones the area plugin had.
 *
 * ## Which locations are not here
 *
 * The page publishes ten. Four are dropped, each because this rev-228 cache has no map to stand on
 * rather than as a preference:
 *
 * - **Charred Dungeon** and **Buccaneers' Laboratory**, both written `mapID = -1`, are later
 *   content whose mapsquares this cache does not ship.
 * - **Karuulm Slayer Dungeon**, which `SpawnTileProbe` found is not built here at all: its mapsquare
 *   exists but carries 133 painted tiles on plane 0 and nothing on any other plane, and not one of
 *   the twelve published pins has a floor under it.
 * - **God Wars Dungeon** and **Wilderness Slayer Cave** carry the level 127 and level 136 versions.
 *   Both are real places in this cache, but neither is built here - the God Wars Dungeon has no
 *   killcount door, no Zamorak-affiliation check for the aggression the page conditions on, and the
 *   Wilderness Slayer Cave has no entrance. Their variants stay defined, so their stats, respawn,
 *   Slayer experience and drop tables are all in place the moment either area is built; they are
 *   simply not placed.
 */
internal data class HellhoundVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    val combatLevel: Int,
    val npcKeys: List<String>,
    /** Wiki `respawn`, in game ticks, which are this engine's cycles one-for-one. */
    val respawnCycles: Int,
    val slayerXp: Double,
)

/** One published `LocLine`: a place, the ids that stand there, and the tiles. */
internal data class HellhoundCamp(
    val location: String,
    val plane: Int,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object Hellhounds {
    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a hellhound stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`.
     *
     * Stated rather than left alone, for the reason `content/npcs/mossgiant` records: a def built
     * from `monsterStats.json` starts from `NpcCombatDef.DEFAULT`, whose `aggressiveTimer` is
     * **0**, and `NpcAggroPlugin` reads a zero timer as "stop being aggressive" - so an aggression
     * radius on its own never fires once.
     *
     * Left at the ordinary timer rather than `alwaysAggro()`. At combat level 122 the default
     * `p.combatLevel <= npcLvl * 2` check never excludes anybody who can reach these places, so the
     * two behave identically here and the ordinary one carries no claim the page does not make.
     */
    const val AGGRO_TIMER = 1000

    /**
     * How far a spawned hellhound wanders from its pin. These are size 2 and thirteen of them share
     * one Taverley corridor, so this is kept modest.
     */
    const val WALK_RADIUS = 4

    /** `id1` - the ordinary level 122 hellhound. */
    val LEVEL_122_IDS = listOf("npc.hellhound_104", "npc.hellhound_105", "npc.hellhound_7256")

    /** `id2` - the God Wars Dungeon version, level 127. Defined but not placed; see the file doc. */
    val GOD_WARS_IDS = listOf("npc.hellhound_3133")

    /** `id3` - the Wilderness Slayer Cave version, level 136. Defined but not placed. */
    val WILDERNESS_CAVE_IDS = listOf("npc.hellhound_7877")

    val VARIANTS: List<HellhoundVariant> =
        listOf(
            HellhoundVariant("Hellhound (level 122)", 122, LEVEL_122_IDS, respawnCycles = 89, slayerXp = 116.0),
            HellhoundVariant("Hellhound (level 127, God Wars)", 127, GOD_WARS_IDS, respawnCycles = 25, slayerXp = 116.0),
            HellhoundVariant(
                "Hellhound (level 136, Wilderness Slayer Cave)",
                136,
                WILDERNESS_CAVE_IDS,
                respawnCycles = 50,
                slayerXp = 150.0,
            ),
        )

    /** Taverley Dungeon, named because `content/areas/taverleydungeon` used to own these pins. */
    val TAVERLEY =
        HellhoundCamp(
            location = "Taverley Dungeon",
            plane = 0,
            npcKeys = LEVEL_122_IDS,
            tiles =
                listOf(
                    2851 to 9849, 2855 to 9837, 2856 to 9847, 2857 to 9841, 2859 to 9852,
                    2861 to 9837, 2864 to 9851, 2867 to 9840, 2868 to 9833, 2869 to 9829,
                    2870 to 9826, 2871 to 9819, 2871 to 9822,
                ),
        )

    /**
     * The five published `LocLine`s this cache can hold, with ids dealt across the camps by
     * [org.alter.plugins.content.npcs.SpawnDealer] - within a location the wiki does not say which
     * of 104, 105 and 7256 stands on which pin, and they differ only in the model.
     *
     * Hellhounds are size 2, so a pin needs a standable 2x2 rather than a standable tile;
     * `BestiaryVerify` checks the whole footprint, which is what these dungeon pins need.
     */
    val CAMPS: List<HellhoundCamp> =
        listOf(
            // ------------------------------------------------------------------ Wilderness
            HellhoundCamp(
                location = "Outside the Wilderness Resource Area",
                plane = 0,
                npcKeys = LEVEL_122_IDS,
                tiles = listOf(3169 to 3956, 3172 to 3944, 3176 to 3950, 3180 to 3906, 3180 to 3917, 3191 to 3914),
            ),
            // ------------------------------------------------------------------ Kandarin
            HellhoundCamp(
                location = "Witchaven Dungeon",
                plane = 0,
                npcKeys = LEVEL_122_IDS,
                tiles =
                    listOf(
                        2734 to 9683, 2734 to 9688, 2734 to 9693, 2740 to 9688,
                        2740 to 9698, 2741 to 9678, 2744 to 9683, 2744 to 9691,
                    ),
            ),
            HellhoundCamp(
                location = "Stronghold Slayer Cave",
                plane = 0,
                npcKeys = LEVEL_122_IDS,
                tiles =
                    listOf(
                        2409 to 9786, 2412 to 9784, 2415 to 9786, 2426 to 9773, 2426 to 9782,
                        2429 to 9770, 2429 to 9786, 2430 to 9775, 2410 to 9801, 2413 to 9797,
                        2417 to 9796, 2433 to 9771, 2433 to 9784,
                    ),
            ),
            // ------------------------------------------------------------------ Asgarnia
            TAVERLEY,
            // ------------------------------------------------------------------ Kourend
            HellhoundCamp(
                location = "Catacombs of Kourend",
                plane = 0,
                npcKeys = LEVEL_122_IDS,
                tiles =
                    listOf(
                        1639 to 10060, 1639 to 10063, 1642 to 10068, 1643 to 10063, 1645 to 10071,
                        1646 to 10066, 1647 to 10062, 1648 to 10058, 1651 to 10062,
                    ),
            ),
        )

    /** Every hellhound key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }
}
