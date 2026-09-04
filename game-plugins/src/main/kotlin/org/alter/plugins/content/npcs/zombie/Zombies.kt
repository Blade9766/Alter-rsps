package org.alter.plugins.content.npcs.zombie

/**
 * The five combat variants of the generic zombie, and every place the OSRS Wiki publishes one
 * standing.
 *
 * Zombies were in this cache and nowhere in this world: 85 zombie ids carry correct wiki stats in
 * `data/cfg/npcs/monsterStats.json`, and not one of them was spawned, aggressive, worth Slayer
 * experience, or dropping so much as a bone. This package is the other half.
 *
 * See [ZombieDrops] for the three drop tables, [ZombiePlugin] for the combat wiring and
 * [ZombieSpawnPlugin] for the placement.
 *
 * ## Why there is no `setCombatDef` anywhere in this package
 *
 * The same call `areas/wilderness/bosses/WildernessBossPlugin` documents. `monsterStats.json`
 * already carries every zombie's hitpoints, attack, strength, defence, attack speed, combat style,
 * attack and defence bonuses, the `UNDEAD` species the salve amulet keys off, and the 50% Fire
 * elemental weakness added in the 25 June 2025 "Summer Sweep Up: Combat" update - all of it
 * wiki-sourced and all of it correct. `World.setNpcDefaults` consults that table **only** for npcs
 * no plugin declares a combat def for, so declaring one here to gain an aggression radius would
 * have thrown the lot away and required every number to be re-typed by hand, where it could then
 * drift from the table the rest of the server reads.
 *
 * Instead the def is taken as given and patched at spawn with the four fields the table has no
 * opinion about - respawn delay, aggression radius, aggression timer and Slayer experience. That
 * also keeps animations working: [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin]
 * resolves attack/block/death for exactly the monsters that carry no hand-written def, and
 * `named-combat-media.json` already has a `ZOMBIE` entry (5568 / 5567 / 5569 with sounds
 * 918 / 923 / 922). Writing a def would have silently replaced all six with the human fallbacks.
 *
 * ## Which ids, and why
 *
 * The Zombie page is a `Multi Infobox` of three versions - level 13, 18 and 24 - and
 * `Zombie (Wilderness)` is a fourth page carrying two more at the same two higher levels. The
 * Wilderness versions are **mechanically identical** to their mainland twins and differ only in
 * their drop table, which is why they are split into their own variants here rather than folded in.
 *
 * Every id below was checked against this cache by name, combat level, size and options: all 40 are
 * `Zombie`, size 1, with an `Attack` option, at the level the wiki gives them. Two groups are
 * deliberately excluded:
 *
 * - **53**, which the level 24 id list skips. `monsterStats.json` marks it `Zombie (Melzar's Maze)`
 *   alongside 3980 and 3981 - a separate page with its own drop table - so it belongs to a
 *   Melzar's Maze slice, not this one.
 * - **64-68**, the Entrana Dungeon zombies. Combat level 25, not 24, with 21 defence on a 4-cycle
 *   attack; a different monster on a different page.
 *
 * Three ids **are** included that the wiki's own list omits: **33, 35 and 36**. All three are level
 * 13 `Zombie` in this cache with the same "Dead man/woman walking" examine as their neighbours, and
 * `monsterStats.json` already gives them the level 13 stat block. The infobox's `id` list is
 * generated from its drop-table versions, so an unlisted id means "no published drop version", not
 * "different monster". Leaving them out would have left three zombies that are passive, worth no
 * Slayer experience and drop nothing; they are not spawned by [ZombieSpawnPlugin], so this only
 * matters for a hand-spawned one.
 */
internal enum class ZombieTableId {
    /** The level 13 table - hammer-tier junk, fishing bait and small coin piles. */
    LEVEL_13,

    /**
     * The level 18 "table", which is bones and the tertiaries and nothing else. That is not an
     * omission here: the wiki's `Drops (level 18)` section publishes a 100% row and a Tertiary row
     * and no others.
     */
    LEVEL_18,

    /** The level 24 table - iron weapons, better runes, and the only mainland gem-table route. */
    LEVEL_24,

    /** Shared by both Wilderness variants, level 18 and level 24 alike. */
    WILDERNESS,
}

internal data class ZombieVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    val combatLevel: Int,
    val npcKeys: List<String>,
    val slayerXp: Double,
    val dropTable: ZombieTableId,
)

/**
 * One published `LocLine` from either Zombie page: a location, the ids that stand there, and the
 * tiles.
 *
 * Ids are dealt round-robin across [tiles] the way
 * [org.alter.plugins.content.npcs.dungeon.GiantCamp] deals hill giants. Within a location the wiki
 * does not say which id stands on which pin - the ids differ only in the model's clothing - so
 * round-robin reproduces the published mix without inventing a mapping.
 */
internal data class ZombieCamp(
    val location: String,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object Zombies {
    /**
     * Wiki `respawn = 35` on all five versions, in **game ticks**, used as published.
     *
     * Without this patch every zombie would respawn on [org.alter.game.model.combat.NpcCombatDef]'s
     * default of 25, since `monsterStats.json` carries no respawn column and its defs are built by
     * copying `DEFAULT`.
     */
    const val RESPAWN_CYCLES = 35

    /**
     * Aggression radius in tiles, matching what `content/npcs/dungeon` uses for its aggressive
     * monsters. `NpcAggroPlugin.checkRadius` scans a full (2r+1)^2 square every sweep, so this is
     * kept tight on purpose - it is how far a zombie notices you, not how far it wanders.
     */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a zombie stays interested, in cycles - ten minutes, the engine's own
     * `DEFAULT_AGGRO_TIMER`.
     *
     * This has to be stated rather than left alone. A def built from `monsterStats.json` starts
     * from `NpcCombatDef.DEFAULT`, whose `aggressiveTimer` is **0**, and
     * `NpcAggroPlugin.defaultAggressiveness` reads a zero timer as "this player has been in the
     * area longer than 0 cycles, stop being aggressive" - so the zombies would have had an
     * aggression radius that never once fired.
     *
     * Left at the ordinary timer rather than `alwaysAggro()`, because zombies are ordinary
     * aggressive monsters: the same function's `p.combatLevel <= npcLvl * 2` check is exactly the
     * real rule that a level 24 zombie ignores anyone above combat level 48.
     */
    const val AGGRO_TIMER = 1000

    /**
     * How far a spawned zombie wanders from its pin.
     *
     * Kept modest deliberately. These are dense populations - 21 of them share the Graveyard of
     * Shadows - and a wide radius on an aggressive monster in a corridor turns a sewer into a wall
     * of zombies following the player from three rooms away.
     */
    const val WALK_RADIUS = 4

    /**
     * Wiki `id1`..`id13` on the level 13 infobox, plus the three unlisted ids - see the file doc.
     *
     * Id 26 is `npc.zombie` in `npc.rscm`, unsuffixed, because it is the first npc in the cache to
     * carry the name; every other zombie is `npc.zombie_<id>`.
     */
    val LEVEL_13_IDS = (26..41).map { if (it == 26) "npc.zombie" else "npc.zombie_$it" }

    /** Wiki `id1`..`id3` on the level 18 infobox. */
    val LEVEL_18_IDS = listOf(42, 43, 44).map { "npc.zombie_$it" }

    /** `Zombie (Wilderness)` level 18, `id1`..`id7`. */
    val WILDERNESS_18_IDS = listOf(45, 46, 47, 48, 6596, 6597, 6598).map { "npc.zombie_$it" }

    /** Wiki `id1`..`id9` on the level 24 infobox. 53 is Melzar's Maze and is not one of them. */
    val LEVEL_24_IDS = listOf(49, 50, 51, 52, 54, 55, 56, 57, 58).map { "npc.zombie_$it" }

    /** `Zombie (Wilderness)` level 24, `id1`..`id5`. */
    val WILDERNESS_24_IDS = listOf(59, 60, 61, 62, 63).map { "npc.zombie_$it" }

    val VARIANTS: List<ZombieVariant> =
        listOf(
            ZombieVariant(
                name = "Zombie (level 13)",
                combatLevel = 13,
                npcKeys = LEVEL_13_IDS,
                slayerXp = 22.0,
                dropTable = ZombieTableId.LEVEL_13,
            ),
            ZombieVariant(
                name = "Zombie (level 18)",
                combatLevel = 18,
                npcKeys = LEVEL_18_IDS,
                slayerXp = 24.0,
                dropTable = ZombieTableId.LEVEL_18,
            ),
            ZombieVariant(
                name = "Zombie (Wilderness, level 18)",
                combatLevel = 18,
                npcKeys = WILDERNESS_18_IDS,
                slayerXp = 24.0,
                dropTable = ZombieTableId.WILDERNESS,
            ),
            ZombieVariant(
                name = "Zombie (level 24)",
                combatLevel = 24,
                npcKeys = LEVEL_24_IDS,
                slayerXp = 30.0,
                dropTable = ZombieTableId.LEVEL_24,
            ),
            ZombieVariant(
                name = "Zombie (Wilderness, level 24)",
                combatLevel = 24,
                npcKeys = WILDERNESS_24_IDS,
                slayerXp = 30.0,
                dropTable = ZombieTableId.WILDERNESS,
            ),
        )

    /**
     * Every `LocLine` on both Zombie pages, less one.
     *
     * All 105 tiles below were read out of this project's own map files and checked to have a floor
     * and no `BLOCK_WALK` flag before being wired - the verify-before-wire method
     * `content/npcs/dungeon/HillGiantSpawns` documents, and re-run by `ZombieVerify`. Every one
     * passed, which is worth stating because most of them are in sewers and dungeons where a
     * published pin landing inside a wall is common.
     *
     * **The omission** is the Tree Gnome Village dungeon *during Waterfall Quest* row (mapID 10155,
     * around 2603, 4446). Those coordinates are a quest instance of the same dungeon; spawning
     * zombies there permanently would put three of them in a copy of the map no player can reach
     * outside a quest this server does not have.
     *
     * Members locations are included. This server already runs members content - Barrows, the KBD,
     * the Warriors' Guild - and the Ardougne, Underground Pass and Tree Gnome Village area packages
     * all exist, so these are placements in built areas rather than speculative ones.
     */
    val CAMPS: List<ZombieCamp> =
        listOf(
            // ------------------------------------------------------------- Misthalin (free-to-play)
            ZombieCamp(
                location = "Varrock Sewers (hallway)",
                npcKeys = listOf("npc.zombie_39", "npc.zombie_41"),
                tiles = listOf(3243 to 9893, 3259 to 9891),
            ),
            ZombieCamp(
                location = "Varrock Sewers (room with Ladder)",
                npcKeys = listOf("npc.zombie_55", "npc.zombie_56", "npc.zombie_57", "npc.zombie_58"),
                tiles = listOf(3224 to 9906, 3226 to 9903, 3230 to 9903, 3230 to 9906),
            ),
            ZombieCamp(
                location = "Draynor Sewers",
                npcKeys =
                    listOf(
                        "npc.zombie_38", "npc.zombie_39", "npc.zombie_40",
                        "npc.zombie_55", "npc.zombie_56", "npc.zombie_57", "npc.zombie_58",
                    ),
                tiles =
                    listOf(
                        3119 to 9647, 3122 to 9658, 3124 to 9651, 3124 to 9662,
                        3086 to 9674, 3088 to 9672, 3096 to 9672,
                    ),
            ),
            ZombieCamp(
                location = "Edgeville Dungeon",
                npcKeys = listOf("npc.zombie_42", "npc.zombie_43", "npc.zombie_44"),
                tiles =
                    listOf(
                        3139 to 9885, 3140 to 9893, 3143 to 9904, 3146 to 9892, 3147 to 9883,
                        3147 to 9899, 3148 to 9904, 3150 to 9889, 3150 to 9907, 3151 to 9884,
                    ),
            ),
            ZombieCamp(
                location = "Edgeville Dungeon mine",
                npcKeys = listOf("npc.zombie_55", "npc.zombie_56", "npc.zombie_57", "npc.zombie_58"),
                tiles = listOf(3119 to 9862, 3124 to 9861, 3126 to 9865, 3130 to 9860),
            ),
            // ------------------------------------------------------------ Wilderness (free-to-play)
            ZombieCamp(
                location = "Ruins (east) (Wilderness)",
                npcKeys = WILDERNESS_18_IDS,
                tiles =
                    listOf(
                        3145 to 3742, 3146 to 3734, 3149 to 3725, 3153 to 3729, 3153 to 3740,
                        3156 to 3731, 3158 to 3746, 3159 to 3723, 3163 to 3726, 3167 to 3743,
                        3173 to 3728, 3174 to 3737, 3177 to 3729, 3177 to 3733, 3170 to 3743,
                    ),
            ),
            /*
             * The one location the wiki gives both Wilderness levels for, so both id sets are dealt
             * across it - which is what makes the graveyard the only place a level 18 and a level 24
             * zombie stand side by side.
             */
            ZombieCamp(
                location = "Graveyard of Shadows (Wilderness)",
                npcKeys = WILDERNESS_18_IDS + WILDERNESS_24_IDS,
                tiles =
                    listOf(
                        3152 to 3674, 3153 to 3658, 3153 to 3676, 3157 to 3681, 3158 to 3676,
                        3162 to 3665, 3163 to 3685, 3165 to 3678, 3166 to 3663, 3168 to 3668,
                        3172 to 3669, 3161 to 3679, 3169 to 3680, 3164 to 3682, 3165 to 3672,
                        3167 to 3686, 3168 to 3684, 3169 to 3688, 3170 to 3672, 3175 to 3665,
                        3180 to 3669,
                    ),
            ),
            // ---------------------------------------------------------------- Kandarin (members)
            ZombieCamp(
                location = "West Ardougne cemetery",
                npcKeys =
                    listOf(
                        "npc.zombie_32", "npc.zombie_34", "npc.zombie_37",
                        "npc.zombie_52", "npc.zombie_54",
                    ),
                tiles = listOf(2497 to 3292, 2499 to 3286, 2500 to 3283, 2506 to 3284, 2511 to 3287),
            ),
            ZombieCamp(
                location = "Ardougne Sewers",
                npcKeys = listOf("npc.zombie_49", "npc.zombie_50", "npc.zombie_51"),
                tiles = listOf(2675 to 9680, 2676 to 9684, 2676 to 9687),
            ),
            ZombieCamp(
                location = "Wizards' Guild dungeon",
                npcKeys = listOf("npc.zombie_49", "npc.zombie_50", "npc.zombie_51"),
                tiles =
                    listOf(
                        2583 to 9493, 2584 to 9491, 2587 to 9492, 2590 to 9491,
                        2590 to 9493, 2591 to 9492, 2593 to 9491, 2593 to 9492,
                    ),
            ),
            ZombieCamp(
                location = "Glarial's Tomb",
                npcKeys =
                    listOf(
                        "npc.zombie", "npc.zombie_29", "npc.zombie_30",
                        "npc.zombie_50", "npc.zombie_51",
                    ),
                tiles = listOf(2533 to 9842, 2540 to 9823, 2540 to 9843, 2542 to 9840, 2546 to 9842),
            ),
            ZombieCamp(
                location = "Tree Gnome Village dungeon",
                npcKeys = listOf("npc.zombie_27", "npc.zombie_28", "npc.zombie_49"),
                tiles = listOf(2539 to 9566, 2541 to 9569, 2548 to 9567),
            ),
            // ---------------------------------------------------------------- Tirannwn (members)
            ZombieCamp(
                location = "Underground Pass (north-western area)",
                npcKeys =
                    listOf(
                        "npc.zombie", "npc.zombie_27", "npc.zombie_28", "npc.zombie_29",
                        "npc.zombie_31", "npc.zombie_49", "npc.zombie_50", "npc.zombie_51",
                    ),
                tiles =
                    listOf(
                        2394 to 9707, 2396 to 9710, 2397 to 9702, 2398 to 9704, 2398 to 9707,
                        2399 to 9712, 2400 to 9701, 2401 to 9714, 2405 to 9713, 2406 to 9709,
                        2407 to 9702, 2408 to 9712, 2409 to 9704, 2410 to 9707,
                    ),
            ),
            ZombieCamp(
                location = "Underground Pass (south-eastern area)",
                npcKeys = listOf("npc.zombie", "npc.zombie_28", "npc.zombie_30", "npc.zombie_49"),
                tiles = listOf(2452 to 9684, 2453 to 9680, 2455 to 9677, 2455 to 9680),
            ),
        )

    /** Every zombie key this package defines, for code that needs "is this one of ours". */
    val ALL_IDS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }
}
