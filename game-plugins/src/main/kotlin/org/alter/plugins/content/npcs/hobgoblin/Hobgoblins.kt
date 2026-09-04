package org.alter.plugins.content.npcs.hobgoblin

/**
 * The seven published versions of `Hobgoblin` and every place the OSRS Wiki puts one.
 *
 * See [HobgoblinDrops] for the two tables, [HobgoblinPlugin] for the wiring and
 * [HobgoblinSpawnPlugin] for the placement.
 *
 * ## Why there is no `setCombatDef` in this package
 *
 * The reason `content/npcs/zombie` and `content/npcs/dwarf` give: all seven ids already carry their
 * exact wiki stat block in `data/cfg/npcs/monsterStats.json` - including the two attack speeds this
 * monster has (the armed level 42 and the plain level 28 both swing every 6 cycles; the recoloured
 * level 28s every 4) - and `World.setNpcDefaults` reads that table **only** for npcs no plugin
 * declares a def for.
 *
 * It would also have cost the animations: `named-combat-media.json`'s `HOBGOBLIN` entry is already
 * correct - 164 / 165 / 167, frame group 425 - and
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationResolver] even carries a hand-written
 * case for this family (`resolve(death = 167, block = 165, attacks = listOf(164, 4784))`) because
 * the armed brown hobgoblin swings 4784 rather than 164.
 *
 * ## Which ids, and which stand where
 *
 * `id1`..`id7` off the infobox, all seven checked against this cache: every one is `Hobgoblin`,
 * size 1, with an `Attack` option, at the level the wiki gives it.
 *
 * Unusually for this tree, the `LocLine`s **name their ids** - `{{^|id=3049,3050}}` and so on - so
 * nothing here is inferred from a `levels` column. Edgeville Dungeon goes further and titles each
 * pin with its level, so those five tiles carry an explicit id each rather than a dealt pool.
 *
 * The one excluded location is **Tree Gnome Village dungeon (in prison)**, mapID 10155 - a quest
 * instance of the same dungeon. Spawning hobgoblins there permanently would put seven of them in a
 * copy of the map no player can reach outside a quest this server does not have. The zombie package
 * excluded the same map for the same reason. The roaming Tree Gnome Village dungeon line (mapID
 * 10154) is the real one and is included.
 *
 * Also not modelled: the **Shaman Caves**, which the page lists as a bullet rather than a `LocLine`
 * because hobgoblins there are a random encounter from smashing a barrel, not a spawn.
 */
internal data class HobgoblinVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    val combatLevel: Int,
    val npcKey: String,
    val slayerXp: Double,
    /** Wiki `dropversion` - `Unarmed` or `Armed`. */
    val dropTable: HobgoblinTableId,
)

/** Which of the page's two `dropversion` tables a hobgoblin rolls. */
internal enum class HobgoblinTableId {
    UNARMED,
    ARMED,
}

/** One published `LocLine`: a place, its plane, the ids that stand there, and the tiles. */
internal data class HobgoblinCamp(
    val location: String,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
    /**
     * The plane the camp stands on.
     *
     * Only the God Wars Dungeon is not 0, and it is the plane the wiki does not give you: its
     * `LocLine` says `plane = 0`, but mapsquare 44_83 is **entirely unpainted on planes 0, 1 and 3
     * and fully painted (all 4,096 tiles) on plane 2**. All eleven published pins are standable
     * there and nowhere else. The whole dungeon is built on plane 2.
     */
    val plane: Int = 0,
)

internal object Hobgoblins {
    /**
     * Aggression radius in tiles, matching every other aggressive monster package in this tree.
     *
     * Every version is `aggressive = Yes`. The God Wars Dungeon one is "Yes, unless wearing a
     * Bandos-affiliated item", which is not modelled - see [HobgoblinPlugin].
     */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a hobgoblin stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`.
     *
     * This has to be stated rather than left alone. A def built from `monsterStats.json` starts from
     * `NpcCombatDef.DEFAULT`, whose `aggressiveTimer` is **0**, and `NpcAggroPlugin`'s default
     * aggressiveness reads a zero timer as "stop being aggressive" - so an aggression radius alone
     * never fires once. The zombie package found this the hard way.
     */
    const val AGGRO_TIMER = 1000

    /**
     * How far a spawned hobgoblin wanders from its pin.
     *
     * Kept modest deliberately. These are dense populations - 26 of them share the Bandit Camp mine
     * and 22 the Dibber - and a wide radius on an aggressive monster turns a mining site into a wall
     * of them following the player from three screens away.
     */
    const val WALK_RADIUS = 4

    // ------------------------------------------------------------------ the ids

    /** `Hobgoblin`, `id1` - the plain level 28, unarmed. */
    const val PLAIN = "npc.hobgoblin_3049"

    /** `Hobgoblin (armed)`, `id2` - the level 42 spearman. */
    const val ARMED_42 = "npc.hobgoblin_3050"

    /** `Hobgoblin (brown)`, `(grey)` and `(khaki)` - `id3`..`id5`, all level 28 and unarmed. */
    val RECOLOURS = listOf("npc.hobgoblin_3286", "npc.hobgoblin_3287", "npc.hobgoblin_3288")

    /** `Hobgoblin (brown, armed)`, `id6` - level 28 but wielding a spear, so it rolls the armed table. */
    const val BROWN_ARMED = "npc.hobgoblin_3289"

    /** `Hobgoblin (GWD)`, `id7` - the level 47 God Wars Dungeon version. */
    const val GOD_WARS = "npc.hobgoblin_2241"

    val VARIANTS: List<HobgoblinVariant> =
        listOf(
            HobgoblinVariant("Hobgoblin", 28, PLAIN, 29.0, HobgoblinTableId.UNARMED),
            HobgoblinVariant("Hobgoblin (armed)", 42, ARMED_42, 49.0, HobgoblinTableId.ARMED),
            HobgoblinVariant("Hobgoblin (brown)", 28, "npc.hobgoblin_3286", 29.0, HobgoblinTableId.UNARMED),
            HobgoblinVariant("Hobgoblin (grey)", 28, "npc.hobgoblin_3287", 29.0, HobgoblinTableId.UNARMED),
            HobgoblinVariant("Hobgoblin (khaki)", 28, "npc.hobgoblin_3288", 29.0, HobgoblinTableId.UNARMED),
            HobgoblinVariant("Hobgoblin (brown, armed)", 28, BROWN_ARMED, 29.0, HobgoblinTableId.ARMED),
            /*
             * The unarmed table's beginner clue carries the note "Not dropped by those found in the
             * God Wars Dungeon". No flag is needed for it: the God Wars hobgoblin is
             * `dropversion = Armed`, and the armed tertiary section's whole content is the champion
             * scroll - so it never reaches that clue in the first place.
             */
            HobgoblinVariant("Hobgoblin (GWD)", 47, GOD_WARS, 52.0, HobgoblinTableId.ARMED),
        )

    /**
     * Every `LocLine` on the page but the quest instance, with the ids the page itself names.
     *
     * Ids are dealt by [org.alter.plugins.content.npcs.SpawnDealer] where a line names more than
     * one - except Edgeville Dungeon, whose pins carry their own titles. Its pool is a bespoke
     * per-tile ordering rather than a set, so it is unique, starts at cursor zero and is therefore
     * dealt positionally exactly as written.
     */
    val CAMPS: List<HobgoblinCamp> =
        listOf(
            // ------------------------------------------------------------------ Misthalin
            /*
             * The one location that publishes a level per pin - "title:Level 42", "title:Level 28" -
             * so the ids are listed tile-for-tile in that order rather than as a pool.
             */
            HobgoblinCamp(
                location = "Edgeville Dungeon",
                npcKeys = listOf(ARMED_42, PLAIN, ARMED_42, PLAIN, ARMED_42),
                tiles =
                    listOf(
                        3118 to 9871, 3122 to 9874, 3123 to 9879,
                        3126 to 9875, 3128 to 9876,
                    ),
            ),
            // ------------------------------------------------------------------ Wilderness
            HobgoblinCamp(
                location = "Bandit Camp mine",
                npcKeys = listOf(PLAIN),
                tiles =
                    listOf(
                        3075 to 3748, 3076 to 3774, 3078 to 3742, 3078 to 3754, 3078 to 3764,
                        3080 to 3750, 3080 to 3759, 3081 to 3770, 3083 to 3766, 3085 to 3742,
                        3085 to 3757, 3085 to 3773, 3087 to 3752, 3088 to 3758, 3088 to 3765,
                        3089 to 3770, 3092 to 3756, 3092 to 3763, 3094 to 3746, 3094 to 3772,
                        3096 to 3767, 3097 to 3756, 3097 to 3761, 3099 to 3770, 3105 to 3763,
                        3107 to 3758,
                    ),
            ),
            // ------------------------------------------------------------------ Asgarnia
            HobgoblinCamp(
                location = "Hobgoblin Peninsula, west of the Crafting Guild",
                npcKeys = RECOLOURS + BROWN_ARMED,
                tiles =
                    listOf(
                        2905 to 3295, 2906 to 3288, 2908 to 3291, 2909 to 3282, 2909 to 3285,
                        2909 to 3294, 2912 to 3279, 2915 to 3273, 2917 to 3269, 2920 to 3266,
                    ),
            ),
            HobgoblinCamp(
                location = "Asgarnian Ice Dungeon",
                npcKeys = listOf(PLAIN, ARMED_42),
                tiles =
                    listOf(
                        3010 to 9578, 3010 to 9593, 3010 to 9595, 3011 to 9579,
                        3016 to 9578, 3016 to 9594, 3019 to 9591,
                    ),
            ),
            HobgoblinCamp(
                location = "God Wars Dungeon",
                npcKeys = listOf(GOD_WARS),
                plane = 2,
                tiles =
                    listOf(
                        2876 to 5318, 2878 to 5317, 2878 to 5319, 2879 to 5318, 2879 to 5321,
                        2854 to 5313, 2855 to 5312, 2856 to 5314, 2869 to 5344, 2870 to 5343,
                        2870 to 5345,
                    ),
            ),
            // ------------------------------------------------------------------ Kandarin
            HobgoblinCamp(
                location = "Outpost",
                npcKeys = listOf(PLAIN, ARMED_42),
                tiles =
                    listOf(
                        2448 to 3329, 2448 to 3331, 2451 to 3330, 2443 to 3359,
                        2445 to 3359, 2445 to 3362, 2446 to 3357, 2447 to 3361,
                    ),
            ),
            HobgoblinCamp("Clock Tower Dungeon", listOf(PLAIN), listOf(2566 to 9632, 2573 to 9626)),
            HobgoblinCamp(
                location = "Witchaven Dungeon",
                npcKeys = listOf(BROWN_ARMED),
                tiles = listOf(2694 to 9687, 2696 to 9689, 2697 to 9693, 2699 to 9688),
            ),
            HobgoblinCamp(
                location = "Tree Gnome Village dungeon (roaming)",
                npcKeys = listOf(PLAIN),
                tiles =
                    listOf(
                        2513 to 9569, 2515 to 9568, 2515 to 9571, 2515 to 9573, 2519 to 9561,
                        2519 to 9571, 2522 to 9561, 2529 to 9555, 2533 to 9556,
                    ),
            ),
            // ------------------------------------------------------------------ Fremennik
            HobgoblinCamp(
                location = "North of Rellekka",
                npcKeys = listOf(ARMED_42),
                // 2656,3720 / 2656,3727 / 2661,3731 are `BLOCK_WALK` in this cache with no
                // standable tile within six in any direction, so they are dropped rather than
                // guessed at. The two that survive are the eastern half of the published line.
                tiles = listOf(2669 to 3725, 2675 to 3731),
            ),
            HobgoblinCamp("Waterbirth Island", listOf(PLAIN), listOf(2503 to 3730, 2526 to 3739)),
            // ------------------------------------------------------------------ Karamja
            HobgoblinCamp(
                location = "South of Tai Bwo Wannai",
                npcKeys = listOf(PLAIN),
                tiles = listOf(2787 to 3013, 2791 to 3013, 2794 to 3013),
            ),
            HobgoblinCamp(
                location = "Crandor",
                npcKeys = listOf(PLAIN),
                // The published 2834,3305 is `BLOCK_WALK`; moved one tile south onto the floor.
                tiles = listOf(2818 to 3291, 2822 to 3282, 2822 to 3288, 2824 to 3298, 2834 to 3304),
            ),
            // ------------------------------------------------------------------ Morytania
            HobgoblinCamp("The Hollows", listOf(PLAIN, ARMED_42), listOf(3469 to 9796, 3469 to 9797)),
            // ------------------------------------------------------------------ Kourend
            HobgoblinCamp(
                location = "The Dibber, Hosidius",
                // `{{^|id=3286,3287,3289}}` - the khaki recolour is not one of them.
                npcKeys = listOf("npc.hobgoblin_3286", "npc.hobgoblin_3287", BROWN_ARMED),
                tiles =
                    listOf(
                        1832 to 3599, 1832 to 3608, 1832 to 3618, 1832 to 3624, 1833 to 3605,
                        1833 to 3628, 1834 to 3615, 1835 to 3621, 1836 to 3608, 1836 to 3612,
                        1836 to 3631, 1836 to 3635, 1837 to 3628, 1838 to 3614, 1838 to 3617,
                        1838 to 3640, 1840 to 3636, 1840 to 3642, 1841 to 3633, 1842 to 3638,
                        1842 to 3641, 1808 to 3627,
                    ),
            ),
        )

    /** Every hobgoblin key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.map { it.npcKey } }
}
