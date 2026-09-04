package org.alter.plugins.content.npcs.dragon

/**
 * Every chromatic and metal dragon this server places, and every place the OSRS Wiki puts one.
 *
 * Nine wiki pages live in this one package - `Bronze dragon`, `Green dragon`, `Blue dragon`,
 * `Red dragon`, `Black dragon` and the four babies - because they are one mechanical thing with
 * nine skins. The adults share a stat shape, a rig, a drop-table shape and, above all,
 * **dragonfire**; the babies are the same monster without the breath. Splitting them into nine
 * packages would have produced nine copies of [DragonfireCombatStrategy]'s wiring and nine copies
 * of the "adult dragons are size 4, babies are size 2" note.
 *
 * See [DragonDrops] for the tables, [DragonPlugin] for the wiring, [DragonSpawnPlugin] for the
 * placement and [DragonfireCombatStrategy] for the breath.
 *
 * ## Where the stats come from
 *
 * `data/cfg/npcs/monsterStats.json`, not this file. Every id below already carries its exact wiki
 * stat block there, including the `DRACONIC` and `FIERY` species tags and the 50% Water elemental
 * weakness (Earth for the bronze). Declaring a combat def here would replace all of it *and* take
 * the dragon off [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin]'s media path -
 * which for this species would be an expensive mistake, because that path is where the corrected
 * chromatic rig now lives. See `npc-animations/README.md`: the resolver was playing the
 * *dragonfire breath* as the melee attack and the *melee claw* as the block on every adult dragon
 * in the game, and had all four baby dragons' attack and block backwards.
 *
 * The one thing this package does add to the def is [org.alter.api.NpcSpecies.BASIC_DRAGON], which
 * `monsterStats.json` does not carry and which
 * [org.alter.plugins.content.combat.formula.DragonfireFormula] reads to decide whether Protect from
 * Magic reduces the breath. See [DragonPlugin].
 *
 * ## Moved out of `content/npcs/dungeon`
 *
 * `Baby blue dragon` and `Baby black dragon` were two `DungeonMonster` rows, wired with hand-written
 * combat defs. They are gone from there and owned here, for the reason `content/npcs/hellhound`
 * records: `PluginRepository.bindNpcDeath` overwrites rather than stacks, so two plugins claiming
 * one id means load order silently decides the drop table.
 *
 * That file's own note - "Blue dragon (111) and black dragon (227) - dragonfire. Without it they
 * would be ordinary melee monsters carrying a boss's stats, and antifire gear would do nothing" -
 * is the gap this package closes.
 *
 * ## Which ids are excluded
 *
 * Five ids the infoboxes list are left out because they have **no row in `monsterStats.json`**, and
 * an npc with no row spawns as `NpcCombatDef.DEFAULT`: a 10-hitpoint, zero-defence creature with no
 * species and no elemental weakness. They are 8073 and 8076 (`Green dragon`), 8075 and 8078
 * (`Red dragon`) and 8084 (`Black dragon`) - the Dragon Slayer II encounter copies, whose live
 * siblings 8082, 8079 and 8085 do have rows and are included.
 *
 * ## More black dragon ids than pins
 *
 * [BLACK_IDS] names nine ids and this cache can host four black dragon pins, because the twelve-pin
 * Taverley upper level is not built here. Five of the nine therefore stand nowhere. They are kept in
 * the list rather than trimmed to fit: they are real published ids with real stats, they differ only
 * in the model, and the moment a Taverley upper level or a Wilderness Slayer Cave exists the dealer
 * will use them without this file changing.
 *
 * `Bestiary2Verify` checks the property that matters instead - that
 * [org.alter.plugins.content.npcs.SpawnDealer] uses as many *distinct* ids as it has pins, rather
 * than standing one dragon in all four places while eight others stand nowhere.
 */
internal data class DragonVariant(
    /** The wiki page and version this row is, kept verbatim so it can be found again. */
    val name: String,
    val combatLevel: Int,
    /** Cache footprint. Adults are 4, babies are 2. */
    val size: Int,
    /** The `Infobox Monster` `name`, which is what the cache calls every one of these ids. */
    val cacheName: String,
    val npcKeys: List<String>,
    /** Whether this dragon breathes. Every adult does; no baby does. */
    val breathesFire: Boolean,
    /**
     * Wiki `respawn`, in game ticks, or null where the page publishes none - in which case the
     * engine default is left alone rather than a number being invented.
     */
    val respawnCycles: Int?,
    /**
     * Wiki `slayxp`. Zero where the page publishes none, which for the **baby green dragon** is a
     * real fact rather than a gap: it is the one dragon here with no `cat` either, so it belongs to
     * no Slayer category and awards no Slayer experience.
     */
    val slayerXp: Double,
    /** Which of [DragonDrops]' tables this version rolls, by the label its doc uses. */
    val dropTable: String,
)

/** One published `LocLine`: a place, the ids that stand there, and the tiles. */
internal data class DragonCamp(
    val location: String,
    val plane: Int,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object Dragons {
    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a dragon stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`.
     *
     * Stated rather than left alone for the reason `content/npcs/mossgiant` records: a def built
     * from `monsterStats.json` starts from `NpcCombatDef.DEFAULT`, whose `aggressiveTimer` is 0,
     * and `NpcAggroPlugin` reads a zero timer as "stop being aggressive", so a radius on its own
     * never fires once.
     */
    const val AGGRO_TIMER = 1000

    /**
     * How far a spawned dragon wanders from its pin.
     *
     * Deliberately small for the adults, which are **size 4**: a 4x4 monster with a wide walk radius
     * spends most of its time wedged against dungeon geometry, and sixteen red dragons share one
     * Brimhaven room.
     */
    const val ADULT_WALK_RADIUS = 3

    /** Babies are size 2 and can afford a little more room. */
    const val BABY_WALK_RADIUS = 4

    // ------------------------------------------------------------------------------- adult ids

    /** `Bronze dragon` `id1`. */
    val BRONZE_IDS = listOf("npc.bronze_dragon", "npc.bronze_dragon_271")

    /** `Bronze dragon` `id2` - the Catacombs of Kourend version, level 143. */
    val BRONZE_CATACOMBS_IDS = listOf("npc.bronze_dragon_7253")

    /** `Green dragon` `id1`, less 8073 and 8076 - see the file doc. */
    val GREEN_IDS =
        listOf(
            "npc.green_dragon", "npc.green_dragon_261", "npc.green_dragon_262",
            "npc.green_dragon_263", "npc.green_dragon_264", "npc.green_dragon_8082",
        )

    /** `Green dragon` `id2` - the Wilderness Slayer Cave version, level 88. */
    val GREEN_WILDERNESS_IDS = listOf("npc.green_dragon_7868", "npc.green_dragon_7869", "npc.green_dragon_7870")

    /** `Blue dragon` `id1`..`id5`, all one version. */
    val BLUE_IDS =
        listOf("npc.blue_dragon", "npc.blue_dragon_266", "npc.blue_dragon_267", "npc.blue_dragon_268", "npc.blue_dragon_269")

    /** `Red dragon` `id1`..`id5`, less 8075 and 8078. */
    val RED_IDS =
        listOf(
            "npc.red_dragon", "npc.red_dragon_248", "npc.red_dragon_249",
            "npc.red_dragon_250", "npc.red_dragon_251", "npc.red_dragon_8079",
        )

    /** `Black dragon` `id1`, less 8084. */
    val BLACK_IDS =
        listOf(
            "npc.black_dragon", "npc.black_dragon_253", "npc.black_dragon_254", "npc.black_dragon_255",
            "npc.black_dragon_256", "npc.black_dragon_257", "npc.black_dragon_258", "npc.black_dragon_259",
            "npc.black_dragon_8085",
        )

    /** `Black dragon` `id2` - the Wilderness Slayer Cave version, level 247. */
    val BLACK_WILDERNESS_IDS = listOf("npc.black_dragon_7861", "npc.black_dragon_7862", "npc.black_dragon_7863")

    // -------------------------------------------------------------------------------- baby ids

    val BABY_GREEN_IDS = listOf("npc.baby_green_dragon", "npc.baby_green_dragon_5872", "npc.baby_green_dragon_5873")

    val BABY_BLUE_IDS = listOf("npc.baby_blue_dragon", "npc.baby_blue_dragon_242", "npc.baby_blue_dragon_243")

    val BABY_RED_IDS = listOf("npc.baby_red_dragon_244", "npc.baby_red_dragon_245", "npc.baby_red_dragon_246")

    val BABY_BLACK_IDS = listOf("npc.baby_black_dragon", "npc.baby_black_dragon_1872")

    /** `Baby black dragon` `id2` - the Myths' Guild version, which is not aggressive. */
    val BABY_BLACK_MYTHS_IDS = listOf("npc.baby_black_dragon_7955")

    val VARIANTS: List<DragonVariant> =
        listOf(
            // ------------------------------------------------------------------------- adults
            DragonVariant(
                name = "Bronze dragon (level 131)",
                combatLevel = 131,
                size = 4,
                cacheName = "Bronze dragon",
                npcKeys = BRONZE_IDS,
                breathesFire = true,
                respawnCycles = 30,
                slayerXp = 125.0,
                dropTable = "bronze",
            ),
            DragonVariant(
                name = "Bronze dragon (level 143, Catacombs)",
                combatLevel = 143,
                size = 4,
                cacheName = "Bronze dragon",
                npcKeys = BRONZE_CATACOMBS_IDS,
                breathesFire = true,
                respawnCycles = 30,
                slayerXp = 125.0,
                dropTable = "bronze",
            ),
            DragonVariant(
                name = "Green dragon (level 79)",
                combatLevel = 79,
                size = 4,
                cacheName = "Green dragon",
                npcKeys = GREEN_IDS,
                breathesFire = true,
                respawnCycles = 30,
                slayerXp = 75.0,
                dropTable = "green",
            ),
            DragonVariant(
                name = "Green dragon (level 88, Wilderness Slayer Cave)",
                combatLevel = 88,
                size = 4,
                cacheName = "Green dragon",
                npcKeys = GREEN_WILDERNESS_IDS,
                breathesFire = true,
                respawnCycles = 30,
                slayerXp = 100.0,
                dropTable = "green wilderness",
            ),
            DragonVariant(
                name = "Blue dragon (level 111)",
                combatLevel = 111,
                size = 4,
                cacheName = "Blue dragon",
                npcKeys = BLUE_IDS,
                breathesFire = true,
                respawnCycles = 30,
                slayerXp = 105.0,
                dropTable = "blue",
            ),
            DragonVariant(
                name = "Red dragon (level 152)",
                combatLevel = 152,
                size = 4,
                cacheName = "Red dragon",
                npcKeys = RED_IDS,
                breathesFire = true,
                respawnCycles = 30,
                // Published as 143.4 with the wiki's own comment "not a typo, there's some weird bug
                // in the bonus experience formula making this 143.4 instead of 143.5". Taken as
                // published, since the page went out of its way to say it is deliberate.
                slayerXp = 143.4,
                dropTable = "red",
            ),
            DragonVariant(
                name = "Black dragon (level 227)",
                combatLevel = 227,
                size = 4,
                cacheName = "Black dragon",
                npcKeys = BLACK_IDS,
                breathesFire = true,
                respawnCycles = 30,
                slayerXp = 194.7,
                dropTable = "black",
            ),
            DragonVariant(
                name = "Black dragon (level 247, Wilderness Slayer Cave)",
                combatLevel = 247,
                size = 4,
                cacheName = "Black dragon",
                npcKeys = BLACK_WILDERNESS_IDS,
                breathesFire = true,
                respawnCycles = 30,
                slayerXp = 262.0,
                dropTable = "black wilderness",
            ),
            // ------------------------------------------------------------------------- babies
            DragonVariant(
                name = "Baby green dragon (level 48)",
                combatLevel = 48,
                size = 2,
                cacheName = "Baby green dragon",
                npcKeys = BABY_GREEN_IDS,
                breathesFire = false,
                respawnCycles = 30,
                // The page publishes neither `slayxp` nor `cat` - alone among the nine here. It is
                // in no Slayer category, so it awards none.
                slayerXp = 0.0,
                dropTable = "baby",
            ),
            DragonVariant(
                name = "Baby blue dragon (level 48)",
                combatLevel = 48,
                size = 2,
                cacheName = "Baby blue dragon",
                npcKeys = BABY_BLUE_IDS,
                breathesFire = false,
                respawnCycles = 30,
                slayerXp = 50.0,
                dropTable = "baby blue",
            ),
            DragonVariant(
                name = "Baby red dragon (level 48)",
                combatLevel = 48,
                size = 2,
                cacheName = "Baby red dragon",
                npcKeys = BABY_RED_IDS,
                breathesFire = false,
                respawnCycles = 30,
                slayerXp = 50.0,
                dropTable = "baby red",
            ),
            DragonVariant(
                name = "Baby black dragon (level 83)",
                combatLevel = 83,
                size = 2,
                cacheName = "Baby black dragon",
                npcKeys = BABY_BLACK_IDS,
                breathesFire = false,
                // The page publishes no `respawn` at all, so the engine default survives - the same
                // call `content/npcs/hobgoblin` makes.
                respawnCycles = null,
                slayerXp = 80.0,
                dropTable = "baby",
            ),
            DragonVariant(
                name = "Baby black dragon (level 83, Myths' Guild)",
                combatLevel = 83,
                size = 2,
                cacheName = "Baby black dragon",
                npcKeys = BABY_BLACK_MYTHS_IDS,
                breathesFire = false,
                respawnCycles = null,
                slayerXp = 80.0,
                dropTable = "baby",
            ),
        )

    /**
     * The baby black dragon is `aggressive = Yes (except in Myths' Guild and Charred Dungeon)`, and
     * the Myths' Guild exception has its own id - so it is expressible exactly rather than as a
     * position test. Neither of the two excepted places is spawned here, but the id is honest about
     * itself in case one is built.
     */
    val PASSIVE_KEYS: Set<String> = BABY_BLACK_MYTHS_IDS.toSet()

    /**
     * Every published `LocLine` this rev-228 cache can actually hold, with ids dealt across the
     * camps by [org.alter.plugins.content.npcs.SpawnDealer].
     *
     * Adult dragons are **size 4**, which makes the pins unusually demanding: a spawn needs a clear
     * 4x4 with its own tile as the south-west corner, and `BestiaryVerify` checks all sixteen tiles.
     * Several published pins sit a tile or two into dungeon walls and are dropped rather than nudged,
     * for the reason `content/npcs/mossgiant` gives: moving a spawn several tiles is inventing one.
     *
     * **What is not here, and why**
     *
     * - **Charred Dungeon**, **Dragon Nest**, **Forthos Dungeon**, **Corsair Cove Dungeon /
     *   Myths' Guild basement** and **Ruins of Tapoyauik** are all written `mapID = -1` or point at
     *   mapsquares this cache does not ship. There is no floor to stand on.
     * - **The Wilderness Slayer Cave** green and black dragons, and the **Catacombs of Kourend**
     *   bronze dragons, keep their variants - stats, respawn, Slayer experience and their own drop
     *   tables are all wired - but are not placed, because neither area has an entrance built.
     * - **Taverley Dungeon (upper level)**, the twelve-pin black dragon resource area, which
 *   `SpawnTileProbe` found is not built in this cache: its mapsquare has 864 painted tiles on plane
 *   0 and none on any other plane, and not one of the twelve pins has a floor under it.
 * - **Evil Chicken's Lair** is a Recipe for Disaster subquest instance.
     * - **Mynydd** needs Song of the Elves.
     */
    val CAMPS: List<DragonCamp> =
        listOf(
            // ------------------------------------------------------ green, all Wilderness
            DragonCamp(
                location = "East of the Bone Yard",
                plane = 0,
                npcKeys = GREEN_IDS,
                tiles =
                    listOf(
                        3331 to 3672, 3332 to 3693, 3333 to 3685, 3333 to 3699, 3337 to 3701,
                        3338 to 3676, 3339 to 3683, 3339 to 3694, 3346 to 3707, 3347 to 3694,
                        3348 to 3685,
                    ),
            ),
            DragonCamp(
                location = "West of the Dark Warriors' Fortress",
                plane = 0,
                npcKeys = GREEN_IDS,
                tiles = listOf(2973 to 3620, 2977 to 3611, 2982 to 3618),
            ),
            DragonCamp(
                location = "North of the Graveyard of Shadows",
                plane = 0,
                npcKeys = GREEN_IDS,
                tiles = listOf(3137 to 3707, 3140 to 3700, 3144 to 3707, 3149 to 3695, 3158 to 3708),
            ),
            DragonCamp(
                location = "South of the Lava Maze",
                plane = 0,
                npcKeys = GREEN_IDS,
                tiles = listOf(3078 to 3810, 3092 to 3810, 3098 to 3821, 3107 to 3812, 3118 to 3820),
            ),
            // ------------------------------------------------------------------------- blue
            DragonCamp(
                location = "Taverley Dungeon",
                plane = 0,
                npcKeys = BLUE_IDS,
                tiles =
                    listOf(
                        2897 to 9797, 2899 to 9802, 2904 to 9802, 2903 to 9780, 2903 to 9786,
                        2906 to 9774, 2911 to 9787, 2912 to 9779, 2920 to 9784,
                    ),
            ),
            DragonCamp(
                location = "Heroes' Guild basement",
                plane = 0,
                npcKeys = BLUE_IDS,
                tiles = listOf(2908 to 9905),
            ),
            DragonCamp(
                location = "Ogre Enclave",
                plane = 0,
                npcKeys = BLUE_IDS,
                tiles = listOf(2590 to 9461, 2592 to 9431, 2609 to 9459, 2568 to 9437, 2579 to 9445, 2604 to 9443),
            ),
            // -------------------------------------------------------------------------- red
            DragonCamp(
                location = "Brimhaven Dungeon",
                plane = 0,
                npcKeys = RED_IDS,
                tiles =
                    listOf(
                        2697 to 9506, 2702 to 9504, 2703 to 9522, 2703 to 9532, 2706 to 9516,
                        2708 to 9508, 2711 to 9500, 2714 to 9526, 2717 to 9516, 2721 to 9522,
                        2724 to 9516, 2704 to 9539, 2704 to 9546, 2711 to 9537, 2711 to 9550,
                        2712 to 9543,
                    ),
            ),
            // ------------------------------------------------------------------------ black
            DragonCamp(
                location = "Taverley Dungeon",
                plane = 0,
                npcKeys = BLACK_IDS,
                tiles = listOf(2829 to 9826, 2835 to 9824),
            ),
            DragonCamp(
                location = "Lava Maze Dungeon",
                plane = 0,
                npcKeys = BLACK_IDS,
                tiles = listOf(3048 to 10266, 3054 to 10269),
            ),
            // ------------------------------------------------------------------- baby blue
            DragonCamp(
                location = "Taverley Dungeon",
                plane = 0,
                npcKeys = BABY_BLUE_IDS,
                tiles =
                    listOf(
                        2895 to 9765, 2895 to 9769, 2900 to 9763, 2892 to 9799, 2904 to 9796,
                        2909 to 9806, 2911 to 9797, 2911 to 9808, 2917 to 9801, 2918 to 9792,
                        2923 to 9793, 2925 to 9788,
                    ),
            ),
            DragonCamp(
                location = "Isle of Souls Dungeon",
                plane = 0,
                npcKeys = BABY_BLUE_IDS,
                tiles = listOf(2125 to 9306, 2126 to 9303, 2131 to 9298),
            ),
            // -------------------------------------------------------------------- baby red
            DragonCamp(
                location = "Brimhaven Dungeon",
                plane = 0,
                npcKeys = BABY_RED_IDS,
                tiles =
                    listOf(
                        2696 to 9501, 2698 to 9524, 2708 to 9500, 2708 to 9526, 2711 to 9516,
                        2714 to 9505, 2721 to 9511, 2721 to 9530, 2730 to 9512, 2701 to 9548,
                        2709 to 9545, 2720 to 9536,
                    ),
            ),
            // ------------------------------------------------------------------ baby green
            DragonCamp(
                location = "Brimhaven Dungeon (upper level)",
                // Plane 2, not the `plane = 0` the LocLine claims and not the 1 that "upper level"
                // suggests: `SpawnTileProbe` found plane 0 entirely BLOCK_WALK here, plane 1 empty
                // and plane 2 carrying 1078 painted tiles with all eleven pins on real floor. The
                // same correction the God Wars Dungeon needed in the first bestiary pass.
                plane = 2,
                npcKeys = BABY_GREEN_IDS,
                tiles =
                    listOf(
                        2657 to 9570, 2660 to 9582, 2662 to 9573, 2663 to 9578, 2664 to 9581,
                        2667 to 9582, 2669 to 9577, 2670 to 9580, 2673 to 9576, 2676 to 9574,
                        2677 to 9570,
                    ),
            ),
        )

    /** Every dragon key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }

    /** Every adult dragon key - the ones [DragonfireCombatStrategy] is registered against. */
    val BREATHING_KEYS: List<String> by lazy { VARIANTS.filter { it.breathesFire }.flatMap { it.npcKeys } }
}
