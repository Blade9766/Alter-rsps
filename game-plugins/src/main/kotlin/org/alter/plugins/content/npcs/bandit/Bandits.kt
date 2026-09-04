package org.alter.plugins.content.npcs.bandit

/**
 * The Wilderness `Bandit` - level 22 and level 130 - and the two places the OSRS Wiki puts one.
 *
 * See [BanditDrops] for the two tables, [BanditPlugin] for the wiring and [BanditSpawnPlugin] for
 * the placement.
 *
 * ## Which bandit this is
 *
 * The one at the [Bandit Camp (Wilderness)](https://oldschool.runescape.wiki/w/Bandit), ids **1026**
 * and **6605**. The cache also holds `Bandit` ids 690-695, 734-737, 11063-11065 and 12663-13290 -
 * the Kharidian Desert bandit camp, Pollnivneach, and the Bandit Camp's own `Guard Bandit`s - each
 * of which is a different wiki page with its own levels and its own drop table. They are left for
 * their own packages rather than folded in here on the strength of a shared name.
 *
 * Both ids were checked against this cache: `Bandit`, size 1, `Attack`, at combat level 22 and 130.
 *
 * ## Why there is no `setCombatDef` in this package
 *
 * The reason `content/npcs/zombie` and `content/npcs/dwarf` give: both ids already carry their exact
 * wiki stat block in `data/cfg/npcs/monsterStats.json`, and `World.setNpcDefaults` reads that table
 * **only** for npcs no plugin declares a def for.
 *
 * ## The animations
 *
 * Bandits are observed playing **386 / 388 / 836** - the armed human set - and
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationResolver] gets the first two backwards
 * on them: with no frame sounds to separate the two actions it falls through to duration, and the
 * block (388, ten frames) is longer than the attack (386, six). A bandit would have parried when it
 * meant to swing.
 *
 * So a `BANDIT` entry naming 386 / 388 / 836 is added to `named-combat-media.json`, which also
 * covers the desert and Pollnivneach bandits and `Bandit champion` through
 * `findNamedCombatMedia`'s prefix match - all of them armed humans on the same rig.
 */
internal data class BanditVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    val combatLevel: Int,
    val npcKey: String,
    val slayerXp: Double,
    /** The table this version rolls. */
    val dropTable: BanditTableId,
    /** Wiki tertiary: how often this version drops a looting bag. */
    val lootingBagOneIn: Int,
)

/** Which of the page's two `dropversion` tables a bandit rolls. */
internal enum class BanditTableId {
    LEVEL_22,
    LEVEL_130,
}

/** One published `LocLine`: a place, the ids that stand there, and the tiles. */
internal data class BanditCamp(
    val location: String,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
)

internal object Bandits {
    /** Wiki `respawn = 100` on both versions, in game ticks, used as published. */
    const val RESPAWN_CYCLES = 100

    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a bandit stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`.
     *
     * This has to be stated rather than left alone. A def built from `monsterStats.json` starts from
     * `NpcCombatDef.DEFAULT`, whose `aggressiveTimer` is **0**, and `NpcAggroPlugin`'s default
     * aggressiveness reads a zero timer as "stop being aggressive" - so an aggression radius alone
     * never fires once. The zombie package found this the hard way.
     *
     * Left at the ordinary timer rather than `alwaysAggro()`, because the same function's
     * `p.combatLevel <= npcLvl * 2` check is the real rule - and at level 130 it is what makes the
     * camp dangerous to everyone rather than only to low levels.
     */
    const val AGGRO_TIMER = 1000

    /**
     * How far a spawned bandit wanders from its pin.
     *
     * Kept tight. The camp is 23 aggressive pins inside a small walled compound with two huts in it,
     * and the page's own strategy section turns on bandits being trappable in those huts - a wide
     * radius would walk them out again.
     */
    const val WALK_RADIUS = 3

    /** `id1` and `id2`. */
    const val LEVEL_22 = "npc.bandit_1026"

    const val LEVEL_130 = "npc.bandit_6605"

    val VARIANTS: List<BanditVariant> =
        listOf(
            BanditVariant(
                name = "Bandit (level 22)",
                combatLevel = 22,
                npcKey = LEVEL_22,
                slayerXp = 27.0,
                dropTable = BanditTableId.LEVEL_22,
                lootingBagOneIn = 6,
            ),
            BanditVariant(
                name = "Bandit (level 130)",
                combatLevel = 130,
                npcKey = LEVEL_130,
                // Wiki `slayxp2 = 158.8`, the one fractional Slayer value in this tree. `slayerXp`
                // is a Double on NpcCombatDef, so it survives as published.
                slayerXp = 158.8,
                dropTable = BanditTableId.LEVEL_130,
                lootingBagOneIn = 3,
            ),
        )

    /**
     * Both `LocLine`s on the page.
     *
     * The camp line publishes "levels = 22, 130" over one pin list, so the two ids are dealt
     * alternately across it by [org.alter.plugins.content.npcs.SpawnDealer] - the wiki does not say
     * which pin is which, and the models are identical. Eight of its 23 coordinates are written `3036,3665` rather than `x:3036,y:3665`;
     * that is a formatting slip in the source, not a different kind of pin, and they are read the
     * same way.
     */
    val CAMPS: List<BanditCamp> =
        listOf(
            BanditCamp(
                location = "Bandit Camp (Wilderness)",
                npcKeys = listOf(LEVEL_22, LEVEL_130),
                tiles =
                    listOf(
                        3024 to 3696, 3029 to 3701, 3029 to 3706, 3031 to 3685, 3031 to 3698,
                        3032 to 3695, 3033 to 3705, 3035 to 3703, 3036 to 3669, 3038 to 3656,
                        3037 to 3683, 3038 to 3691, 3040 to 3683, 3040 to 3703, 3044 to 3703,
                        3036 to 3665, 3036 to 3672, 3037 to 3661, 3037 to 3667, 3038 to 3670,
                        3038 to 3675, 3039 to 3658, 3039 to 3660,
                        // The published 3037,3656 is `BLOCK_WALK`; moved one tile east, which is
                        // the nearest floor and still inside the camp's southern hut row.
                    ),
            ),
            BanditCamp(
                location = "Wilderness Slayer Cave",
                npcKeys = listOf(LEVEL_130),
                // The published 3430,10070 is unpainted void in this cache; the cave's floor starts
                // four tiles west, so the pin is moved to 3426,10070 rather than dropped.
                tiles = listOf(3426 to 10070),
            ),
        )

    /** Every bandit key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.map { it.npcKey } }
}
