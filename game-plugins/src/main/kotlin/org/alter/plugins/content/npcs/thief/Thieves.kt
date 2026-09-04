package org.alter.plugins.content.npcs.thief

/**
 * The `Thief` monster - eleven published versions across three combat levels - and every place one
 * stands.
 *
 * See [ThiefDrops] for the table, [ThiefPlugin] for the wiring and [ThiefSpawnPlugin] for the
 * placement.
 *
 * ## Why there is no `setCombatDef` in this package
 *
 * The reason `content/npcs/zombie` and `content/npcs/dwarf` give: every id below already carries its
 * exact wiki stat block in `data/cfg/npcs/monsterStats.json`, and `World.setNpcDefaults` reads that
 * table **only** for npcs no plugin declares a def for.
 *
 * ## Two of the wiki's eleven ids are not monsters in this cache
 *
 * `The Warrens, 1` and `The Warrens, 2` - ids **7914** and **7915** - are combat level **0** here,
 * with a single `Talk-to` option and no `Attack`, and neither carries a stat row. Only
 * `The Warrens, 3` (7916) is attackable at the published level 21. So the Warrens contribute one
 * version, not three, and the two talk-only ids are left entirely alone; giving either a drop table
 * or a Slayer value would be wiring loot onto an npc nobody can fight.
 *
 * Every other id was checked the same way: all nine are `Thief`, size 1, with an `Attack` option,
 * at the level the wiki gives them.
 *
 * ## Two of them need their animations named
 *
 * The unarmed thieves are observed playing the human set 425 / 422 / 836 that `MAN` and `WOMAN`
 * already use. **4247** (`Monk's Friend`) and **7916** (`The Warrens, 3`) are not: they carry a
 * weapon and play **386 / 388 / 836**, and
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationResolver] gets those two backwards -
 * with no frame sounds to separate them it falls through to duration, and the block (388, ten
 * frames) is longer than the attack (386, six). A thief would have parried when it meant to swing.
 *
 * So the plugin maps those two ids at `ARMED_HUMAN` in `named-combat-media.json`, and the unarmed
 * ones at a `THIEF` entry holding the same values the resolver would have reached anyway - stated
 * rather than derived, because 5218, 5219 and 5220 have no observed animations at all and would
 * otherwise have fallen back to the engine's bare `NpcCombatDef.DEFAULT`.
 */
internal data class ThiefVariant(
    /** The wiki's own version label, kept verbatim so a row can be found again. */
    val name: String,
    val combatLevel: Int,
    val npcKey: String,
    /** Wiki `respawn`, in game ticks - 25 on the mainland versions and 30 in The Warrens. */
    val respawnCycles: Int,
    /**
     * Whether this version rolls the main drop table.
     *
     * The page splits its loot in two: a full table, and a `Drops (Plain)` section whose whole
     * content is bones, for "the thieves found in the Clock Tower Dungeon, one of the two thieves in
     * Port Sarim, the thief in the Port Sarim jail, and the thief in The Warrens".
     */
    val rollsTable: Boolean,
)

/** One published `LocLine`: a place, its plane, the ids that stand there, and the tiles. */
internal data class ThiefCamp(
    val location: String,
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
    /**
     * The plane the camp stands on.
     *
     * Only The Warrens is not 0, and it is the plane the wiki does not give you: its `LocLine` says
     * `plane = 0`, but in this cache every one of its three pins is `BLOCK_WALK` on plane 0 and
     * standable on **plane 2**, where mapsquare 28_158 carries 1,671 painted tiles. Piscarilius'
     * underground is built on the upper planes of its own mapsquare.
     */
    val plane: Int = 0,
)

internal object Thieves {
    /** Wiki `respawn1`..`respawn8`, in game ticks - and `NpcCombatDef.DEFAULT`'s own value. */
    const val MAINLAND_RESPAWN_CYCLES = 25

    /** Wiki `respawn9`..`respawn11`, The Warrens. */
    const val WARRENS_RESPAWN_CYCLES = 30

    /**
     * How far a spawned thief wanders from its pin.
     *
     * Every version is `aggressive = No`, so this is the only thing that moves one. Kept tight
     * because three of the nine stand inside single rooms - a jail cell, a gang hideout - where a
     * wide radius would walk a thief through the door.
     */
    const val WALK_RADIUS = 3

    // ------------------------------------------------------------------ the ids

    /** `Standard`, `id1` - the version that rolls the full table. */
    const val STANDARD = "npc.thief_3252"

    /** `Port Sarim`, `id2`, and `Port Sarim jail`, `id3`. Both plain. */
    const val PORT_SARIM = "npc.thief_3253"

    const val PORT_SARIM_JAIL = "npc.thief"

    /** `Varrock Gang, 1`..`4` - `id4`..`id7`. All four roll the full table. */
    val VARROCK_GANG = listOf("npc.thief_5217", "npc.thief_5218", "npc.thief_5219", "npc.thief_5220")

    /** `Monk's Friend`, `id8` - the level 14 Clock Tower Dungeon thief, plain. */
    const val MONKS_FRIEND = "npc.thief_4247"

    /** `The Warrens, 3`, `id11` - the only attackable Warrens id in this cache. Plain. */
    const val WARRENS = "npc.thief_7916"

    /** The two ids that swing a weapon rather than a fist - see the file doc. */
    val ARMED_KEYS = listOf(MONKS_FRIEND, WARRENS)

    val VARIANTS: List<ThiefVariant> =
        listOf(
            ThiefVariant("Standard", 16, STANDARD, MAINLAND_RESPAWN_CYCLES, rollsTable = true),
            ThiefVariant("Port Sarim", 16, PORT_SARIM, MAINLAND_RESPAWN_CYCLES, rollsTable = false),
            ThiefVariant("Port Sarim jail", 16, PORT_SARIM_JAIL, MAINLAND_RESPAWN_CYCLES, rollsTable = false),
            ThiefVariant("Varrock Gang, 1", 16, "npc.thief_5217", MAINLAND_RESPAWN_CYCLES, rollsTable = true),
            ThiefVariant("Varrock Gang, 2", 16, "npc.thief_5218", MAINLAND_RESPAWN_CYCLES, rollsTable = true),
            ThiefVariant("Varrock Gang, 3", 16, "npc.thief_5219", MAINLAND_RESPAWN_CYCLES, rollsTable = true),
            ThiefVariant("Varrock Gang, 4", 16, "npc.thief_5220", MAINLAND_RESPAWN_CYCLES, rollsTable = true),
            ThiefVariant("Monk's Friend", 14, MONKS_FRIEND, MAINLAND_RESPAWN_CYCLES, rollsTable = false),
            ThiefVariant("The Warrens, 3", 21, WARRENS, WARRENS_RESPAWN_CYCLES, rollsTable = false),
        )

    /**
     * Every `LocLine` on the page, with the ids assigned by what the version *names* say rather than
     * by the `levels` column, which only ever reads "16".
     *
     * The one judgement call is **Port Sarim**, whose two pins carry one `Standard` thief and one
     * `Port Sarim` thief: the page's `Drops (Plain)` section says "one of the two thieves in Port
     * Sarim" is plain, which is the `Port Sarim` version, so the two ids are dealt one to each pin.
     * Which pin gets which is not published and does not matter - they are two tiles apart.
     *
     * `Thief (Varlamore)` is a separate page with its own ids and is not modelled here.
     */
    val CAMPS: List<ThiefCamp> =
        listOf(
            // ------------------------------------------------------------------ Misthalin
            ThiefCamp("Varrock", listOf(STANDARD), listOf(3230 to 3391)),
            ThiefCamp("Jolly Boar Inn", listOf(STANDARD), listOf(3285 to 3501)),
            ThiefCamp(
                location = "Varrock (Black Arm Gang hideout)",
                npcKeys = VARROCK_GANG,
                tiles = listOf(3184 to 3385, 3184 to 3389, 3185 to 3390),
            ),
            ThiefCamp(
                location = "Phoenix Gang Hideout under Varrock",
                npcKeys = VARROCK_GANG,
                tiles =
                    listOf(
                        3237 to 9769, 3238 to 9764, 3245 to 9769,
                        3245 to 9772, 3247 to 9776, 3251 to 9768,
                    ),
            ),
            // ------------------------------------------------------------------ Asgarnia
            ThiefCamp("Port Sarim", listOf(STANDARD, PORT_SARIM), listOf(3014 to 3232, 3016 to 3232)),
            ThiefCamp("Port Sarim jail", listOf(PORT_SARIM_JAIL), listOf(3014 to 3195)),
            // ------------------------------------------------------------------ Karamja
            ThiefCamp(
                location = "Brimhaven",
                npcKeys = listOf(STANDARD),
                tiles = listOf(2785 to 3179, 2796 to 3177, 2803 to 3179, 2812 to 3159),
            ),
            // ------------------------------------------------------------------ Kandarin
            ThiefCamp("Clock Tower Dungeon", listOf(MONKS_FRIEND), listOf(2564 to 9609, 2566 to 9605)),
            // ------------------------------------------------------------------ Kourend
            ThiefCamp(
                location = "The Warrens, under Port Piscarilius",
                npcKeys = listOf(WARRENS),
                tiles = listOf(1827 to 10154, 1828 to 10143, 1831 to 10152),
                plane = 2,
            ),
        )

    /** Every thief key this package defines, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { VARIANTS.map { it.npcKey } }
}
