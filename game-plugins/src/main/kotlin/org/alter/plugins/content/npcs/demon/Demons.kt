package org.alter.plugins.content.npcs.demon

/**
 * The lesser and greater demons - every published version, and every place the OSRS Wiki puts one.
 *
 * Two wiki pages in one package because they are the same monster at two sizes: same rig, same
 * `Slash` attack, same -10 magic defence, same Water weakness, same shape of drop table, same
 * `Vile ashes` in place of bones. The only structural difference is the footprint - a lesser demon
 * is size 2 and a greater demon size 3 - which is a field rather than a package.
 *
 * See [DemonDrops] for the tables, [DemonPlugin] for the wiring and [DemonSpawnPlugin] for the
 * placement.
 *
 * ## Where the stats come from
 *
 * `data/cfg/npcs/monsterStats.json`. Every id below carries its exact wiki stat block there,
 * including the `DEMON` species tag that makes demonbane weapons work and the 40% Water elemental
 * weakness. No `setCombatDef` anywhere in this package, for the reason `content/npcs/mossgiant`
 * records: one would replace those numbers *and* take the demon off
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin]'s media path, which is where
 * the shared `DEMON` animation set (64 / 65 / 67) comes from.
 *
 * ## Moved out of `content/npcs/dungeon`
 *
 * `Lesser demon` and `Greater demon` were two `DungeonMonster` rows with hand-written combat defs,
 * placed by three area spawn plugins - five lesser demons in `areas/taverleydungeon`, three in
 * `areas/templeofikov`, five greater demons in `areas/ogreenclave`. All of that is gone and this
 * package owns it, because `PluginRepository.bindNpcDeath` overwrites rather than stacks: two
 * plugins claiming one id means load order silently decides the drop table.
 *
 * Those thirteen pins were also hand-picked rather than published. The camps below are the wiki's
 * own coordinates, and cover eleven locations rather than three.
 *
 * The dungeon file's rows also only ever covered the `Regular` drop version while listing ids that
 * span `Regular`, `Chasm of Fire` and the Catacombs - a mismatch its own doc called out. The
 * versions are separated properly here.
 *
 * ## Which ids are excluded
 *
 * **7866 and 7867**, two of the three Wilderness Slayer Cave lesser demons, have **no row in
 * `monsterStats.json`** and so would spawn as `NpcCombatDef.DEFAULT` - a 10-hitpoint creature with
 * no species and no weakness. Their sibling 7865 has a row and stands for that version.
 */
internal data class DemonVariant(
    /** The wiki page and version this row is, kept verbatim so it can be found again. */
    val name: String,
    val combatLevel: Int,
    /** Cache footprint. Lesser demons are 2, greater demons 3. */
    val size: Int,
    /** The `Infobox Monster` `name`, which is what the cache calls every one of these ids. */
    val cacheName: String,
    val npcKeys: List<String>,
    val slayerXp: Double,
    /** Which of [DemonDrops]' tables this version rolls, by the label its doc uses. */
    val dropTable: String,
)

/** One published `LocLine`: a place, the ids that stand there, and the tiles. */
internal data class DemonCamp(
    val location: String,
    val plane: Int,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object Demons {
    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a demon stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`, stated
     * because a def built from `monsterStats.json` starts with a **0** timer and `NpcAggroPlugin`
     * reads a zero timer as "stop being aggressive".
     */
    const val AGGRO_TIMER = 1000

    /**
     * Wiki `respawn = 30` on every version here except the level 92 greater demon, which publishes
     * 15 and gets its own constant.
     */
    const val RESPAWN_CYCLES = 30

    /** `Greater demon` `respawn1`. */
    const val GREATER_92_RESPAWN_CYCLES = 15

    /** Lesser demons are size 2; a modest radius keeps a Taverley corridor walkable. */
    const val LESSER_WALK_RADIUS = 4

    /** Greater demons are size 3 and wedge easily; smaller still. */
    const val GREATER_WALK_RADIUS = 3

    // ------------------------------------------------------------------------ lesser demon ids

    /** `Lesser demon` `id1` - the level 82, and the only version with more than one id. */
    val LESSER_82_IDS =
        listOf(
            "npc.lesser_demon", "npc.lesser_demon_2006", "npc.lesser_demon_2007", "npc.lesser_demon_2008",
            "npc.lesser_demon_2018", "npc.lesser_demon_7656", "npc.lesser_demon_7657", "npc.lesser_demon_7664",
        )

    /** `Lesser demon` `id2` - the level 87 Catacombs version. */
    val LESSER_87_IDS = listOf("npc.lesser_demon_7247")

    /** `Lesser demon` `id3` - the level 94 Catacombs version. */
    val LESSER_94_IDS = listOf("npc.lesser_demon_7248")

    /** `Lesser demon` `id4`, less 7866 and 7867 - see the file doc. */
    val LESSER_WILDERNESS_IDS = listOf("npc.lesser_demon_7865")

    // ----------------------------------------------------------------------- greater demon ids

    /** `Greater demon` `id1` - the level 92. */
    val GREATER_92_IDS =
        listOf(
            "npc.greater_demon", "npc.greater_demon_2026", "npc.greater_demon_2027", "npc.greater_demon_2028",
            "npc.greater_demon_2029", "npc.greater_demon_2030", "npc.greater_demon_2031", "npc.greater_demon_2032",
        )

    /** `Greater demon` `id2` - the level 100 Catacombs version. */
    val GREATER_100_IDS = listOf("npc.greater_demon_7245")

    /** `Greater demon` `id3` - the level 101 Catacombs version. */
    val GREATER_101_IDS = listOf("npc.greater_demon_7244")

    /** `Greater demon` `id4` - the level 104 Wilderness Slayer Cave version. */
    val GREATER_WILDERNESS_IDS =
        listOf("npc.greater_demon_7871", "npc.greater_demon_7872", "npc.greater_demon_7873")

    /** `Greater demon` `id5` - the level 113 Catacombs version. */
    val GREATER_113_IDS = listOf("npc.greater_demon_7246")

    val VARIANTS: List<DemonVariant> =
        listOf(
            DemonVariant("Lesser demon (level 82)", 82, 2, "Lesser demon", LESSER_82_IDS, 79.0, "lesser"),
            DemonVariant("Lesser demon (level 87)", 87, 2, "Lesser demon", LESSER_87_IDS, 85.0, "lesser"),
            DemonVariant("Lesser demon (level 94)", 94, 2, "Lesser demon", LESSER_94_IDS, 98.0, "lesser"),
            DemonVariant(
                "Lesser demon (level 94, Wilderness Slayer Cave)",
                94,
                2,
                "Lesser demon",
                LESSER_WILDERNESS_IDS,
                110.0,
                "lesser wilderness",
            ),
            DemonVariant("Greater demon (level 92)", 92, 3, "Greater demon", GREATER_92_IDS, 87.0, "greater"),
            DemonVariant("Greater demon (level 100)", 100, 3, "Greater demon", GREATER_100_IDS, 115.0, "greater"),
            DemonVariant("Greater demon (level 101)", 101, 3, "Greater demon", GREATER_101_IDS, 120.0, "greater"),
            DemonVariant(
                "Greater demon (level 104, Wilderness Slayer Cave)",
                104,
                3,
                "Greater demon",
                GREATER_WILDERNESS_IDS,
                120.0,
                "greater wilderness",
            ),
            DemonVariant("Greater demon (level 113)", 113, 3, "Greater demon", GREATER_113_IDS, 130.0, "greater"),
        )

    /**
     * Every published `LocLine` this rev-228 cache can hold.
     *
     * Greater demons are **size 3**, so a pin needs a clear 3x3 with its own tile as the south-west
     * corner; `BestiaryVerify` checks the whole footprint. Pins whose footprint runs into dungeon
     * geometry are dropped rather than nudged, for the reason `content/npcs/mossgiant` gives.
     *
     * **What is not here**: the **Charred Dungeon** and **Wilderness Slayer Cave** lines, whose
     * mapsquares this cache does not ship or whose area has no entrance built; and the **Chasm of
     * Fire**, whose whole point is the contract drops [DemonDrops] explains are not modelled. The
     * Catacombs of Kourend lines keep their variants but are not placed, for the same
     * no-entrance reason.
     */
    val CAMPS: List<DemonCamp> =
        listOf(
            // -------------------------------------------------------------- lesser, Misthalin
            DemonCamp(
                location = "Wizards' Tower - cage on the top floor",
                plane = 2,
                npcKeys = LESSER_82_IDS,
                tiles = listOf(3110 to 3157),
            ),
            // ----------------------------------------------------------------- lesser, Karamja
            DemonCamp(
                location = "Crandor",
                plane = 0,
                npcKeys = LESSER_82_IDS,
                tiles = listOf(2832 to 3278, 2837 to 3280),
            ),
            DemonCamp(
                location = "Crandor and Karamja Dungeon",
                plane = 0,
                npcKeys = LESSER_82_IDS,
                tiles =
                    listOf(
                        2835 to 9602, 2837 to 9623, 2838 to 9610, 2839 to 9604, 2845 to 9612, 2831 to 9562,
                        2836 to 9558, 2837 to 9565, 2840 to 9552, 2841 to 9559, 2843 to 9557,
                    ),
            ),
            // ---------------------------------------------------------------- lesser, Asgarnia
            DemonCamp(
                location = "Taverley Dungeon",
                plane = 0,
                npcKeys = LESSER_82_IDS,
                tiles = listOf(2926 to 9802, 2931 to 9798, 2931 to 9807, 2932 to 9810, 2936 to 9793),
            ),
            // -------------------------------------------------------------- lesser, Wilderness
            DemonCamp(
                location = "Lava Maze, by the muddy chest",
                plane = 0,
                npcKeys = LESSER_82_IDS,
                tiles = listOf(3091 to 3861, 3094 to 3869),
            ),
            DemonCamp(
                location = "South-east of the Demonic Ruins",
                plane = 0,
                npcKeys = LESSER_82_IDS,
                tiles = listOf(3311 to 3845, 3324 to 3856),
            ),
            DemonCamp(
                location = "King Black Dragon Lair entrance",
                plane = 0,
                npcKeys = LESSER_82_IDS,
                tiles = listOf(3014 to 3848, 3015 to 3851, 3016 to 3845, 3017 to 3852),
            ),
            // ---------------------------------------------------------------- lesser, Kandarin
            DemonCamp(
                location = "Temple of Ikov",
                plane = 0,
                npcKeys = LESSER_82_IDS,
                tiles = listOf(2630 to 9867, 2631 to 9880, 2632 to 9874),
            ),
            // ----------------------------------------------------------------- greater, Karamja
            DemonCamp(
                location = "Brimhaven Dungeon (upper level)",
                // Plane 2, not the `plane = 1` the LocLine gives: `SpawnTileProbe` found plane 0
                // entirely BLOCK_WALK here, plane 1 carrying four painted tiles in the whole
                // mapsquare, and plane 2 carrying 889 with all seven pins on real floor.
                plane = 2,
                npcKeys = GREATER_92_IDS,
                tiles =
                    listOf(
                        2630 to 9482, 2631 to 9505, 2633 to 9491, 2635 to 9477,
                        2638 to 9501, 2642 to 9506, 2646 to 9476,
                    ),
            ),
            // ---------------------------------------------------------------- greater, Asgarnia
            DemonCamp(
                location = "Entrana Dungeon",
                plane = 0,
                npcKeys = GREATER_92_IDS,
                tiles = listOf(2861 to 9747, 2862 to 9751),
            ),
            // -------------------------------------------------------------- greater, Wilderness
            DemonCamp(
                location = "Near the Demonic Ruins",
                plane = 0,
                npcKeys = GREATER_92_IDS,
                tiles = listOf(3282 to 3880, 3287 to 3894, 3304 to 3886, 3296 to 3872),
            ),
            DemonCamp(
                location = "Lava Maze Dungeon",
                plane = 0,
                npcKeys = GREATER_92_IDS,
                tiles = listOf(3028 to 10250, 3030 to 10259, 3035 to 10245),
            ),
            // ---------------------------------------------------------------- greater, Kandarin
            DemonCamp(
                location = "Ogre Enclave",
                plane = 0,
                npcKeys = GREATER_92_IDS,
                tiles = listOf(2586 to 9458, 2612 to 9423, 2615 to 9423, 2615 to 9426, 2618 to 9424),
            ),
        )

    /** Every demon key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }

    /** Which respawn a variant takes - only the level 92 greater demon differs. */
    fun respawnFor(variant: DemonVariant): Int =
        if (variant.npcKeys === GREATER_92_IDS) GREATER_92_RESPAWN_CYCLES else RESPAWN_CYCLES
}
