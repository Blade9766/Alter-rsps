package org.alter.plugins.content.npcs.bear

/**
 * The black bears - one published version, one id, and eight places the OSRS Wiki puts one.
 *
 * See [BlackBearPlugin] for the wiring and everything else. Stats come from
 * `data/cfg/npcs/monsterStats.json` (25 hitpoints, 15/16/13, 30% Fire weakness) and animations from
 * the existing `BEAR` entry in `named-combat-media.json`, 4925 / 4927 / 4929 - `Black bear`
 * suffix-matches `BEAR`, which the second bestiary audit confirmed is the right rig for it.
 *
 * ## Its Slayer category was empty
 *
 * `data/cfg/slayer/tasks.json` carried a `Bears` category with an **empty `monsters` list** - an
 * assignable task naming nothing, which `SlayerService.markAvailable` reads as not assignable. It
 * names `Black bear` now. Only the black bear, not every bear in the cache: a task that named
 * monsters nobody has spawned would be worse than no task, and this is the one bear this pass
 * places.
 *
 * ## It is passive, and stays that way
 *
 * `aggressive = No`, so no aggression radius is set and none of the aggression constants the rest of
 * this bestiary pass needs appears here. A black bear fights back and nothing more.
 */
internal object BlackBears {
    /** The one id the infobox publishes. */
    const val NPC_KEY = "npc.black_bear"

    const val COMBAT_LEVEL = 19

    /** Wiki `respawn = 50`, in game ticks, which are this engine's cycles one-for-one. */
    const val RESPAWN_CYCLES = 50

    /** Wiki `slayxp = 25`. */
    const val SLAYER_XP = 25.0

    /** Bears are size 2, so this is kept modest. */
    const val WALK_RADIUS = 5

    /**
     * The `100%` section - three rows, all `Always`, which is the whole of what a black bear drops
     * besides its two tertiaries.
     *
     * **`Bear ribs` is not here**, though it is also published as `Always`: its footnote makes it a
     * Rag and Bone Man II drop, and that quest does not exist on this server. Handing it out
     * unconditionally would put a quest item in every player's inventory - the same call
     * `content/npcs/mossgiant` makes about the moss giant bone.
     */
    val GUARANTEED = listOf("item.bones", "item.bear_fur", "item.raw_bear_meat")

    /** Wiki tertiary. */
    const val ENSOULED_HEAD_ONE_IN = 25

    /** Wiki `DropsLineClue|type=beginner`. */
    const val BEGINNER_CLUE_ONE_IN = 90

    /** One published `LocLine`: a place and its tiles. All eight are plane 0. */
    internal data class Camp(
        val location: String,
        val tiles: List<Pair<Int, Int>>,
    )

    /**
     * Seven of the eight published `LocLine`s. `Custodia Mountains` publishes no coordinates at all
     * - the line is there with an empty pin list - so there is nothing to place.
     *
     * These are mostly one- and two-bear lines, which is what a black bear population looks like:
     * they are scenery in seven places rather than a camp in one.
     */
    val CAMPS: List<Camp> =
        listOf(
            Camp("Varrock south-east mine", listOf(3296 to 3347)),
            Camp("South-east of Gu'Tanoth", listOf(2606 to 3008)),
            Camp("Gnome Maze", listOf(2501 to 3159)),
            Camp("Outside the Mind Altar", listOf(2967 to 3480, 2973 to 3482, 2979 to 3501, 2966 to 3456)),
            Camp("East of the Farming Guild", listOf(1322 to 3713, 1319 to 3710)),
            Camp("North-east of the Observatory", listOf(2460 to 3200, 2478 to 3200)),
            Camp("Entrana", listOf(2809 to 3376)),
        )
}
