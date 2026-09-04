package org.alter.plugins.content.areas.fremennikslayerdungeon.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The monsters of the Fremennik Slayer Dungeon, under the mountain south-east of Rellekka.
 *
 * Every tile is a published map pin from the monster's own wiki page, the same sourcing the Slayer
 * Tower and Taverley Dungeon spawns use. Combat stats, drops and Slayer requirements live in
 * `content/npcs/slayer`.
 *
 * The whole dungeon is one plane - z 9992 to 10038 is the underground mapsquare, not an upper floor
 * - so unlike the tower nothing here needs a height beyond 0.
 *
 * It reads west to east as a difficulty ramp, and the pins bear that out: kurask and jelly at the
 * far west around x 2694-2711, then turoth, basilisks, pyrefiends, and the cave crawlers, rockslugs
 * and cockatrices in the east around x 2781-2804. That is the dungeon's actual shape, not an
 * arrangement chosen here.
 *
 * **The turoth's four levels share one pin list.** Its `LocLine` is a single block reading
 * `levels = 83, 85, 87, 89` across all 22 pins rather than one block per version, so the four are
 * cycled across them - the published mix, without inventing a per-tile claim the wiki does not make.
 *
 * **The kurask task-only area is not built.** Its nine pins at z 9959-9977 sit behind a gate that
 * only opens on an active kurask assignment; the five non-task pins are here and the rest go in with
 * the Slayer Tower basement, which needs the same mechanic.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    private val kurasks =
        listOf(
            2694 to 9995, 2694 to 9999, 2698 to 9992, 2699 to 10001, 2701 to 9996,
        )

    private val jellies =
        listOf(
            2699 to 10029, 2701 to 10023, 2701 to 10027, 2703 to 10025, 2703 to 10031,
            2704 to 10028, 2706 to 10029, 2708 to 10024, 2708 to 10027, 2711 to 10029,
        )

    private val turoths =
        listOf(
            2717 to 10007, 2718 to 10006, 2718 to 10008, 2719 to 10011, 2720 to 10003,
            2721 to 10003, 2721 to 10009, 2721 to 10014, 2722 to 10008, 2723 to 9994,
            2723 to 10002, 2723 to 10010, 2724 to 9996, 2724 to 10013, 2725 to 9995,
            2725 to 10012, 2726 to 10010, 2726 to 10013, 2727 to 9995, 2727 to 10011,
            2728 to 9996, 2728 to 9997,
        )

    private val basilisks =
        listOf(
            2738 to 10008, 2740 to 10003, 2740 to 10014, 2742 to 10008,
            2743 to 10018, 2745 to 10002, 2746 to 10006, 2746 to 10015,
        )

    private val pyrefiends =
        listOf(
            2757 to 10008, 2758 to 10001, 2758 to 10004, 2758 to 10012, 2760 to 10007,
            2760 to 10009, 2761 to 9998, 2761 to 10003, 2763 to 9999, 2763 to 10001,
            2763 to 10007, 2765 to 10002, 2765 to 10005,
        )

    private val caveCrawlers =
        listOf(
            2784 to 9998, 2795 to 9994, 2787 to 9996, 2781 to 10000,
            2785 to 9992, 2790 to 9994, 2782 to 9995, 2793 to 9998,
        )

    private val rockslugs =
        listOf(
            2793 to 10016, 2795 to 10017, 2798 to 10018, 2800 to 10020,
            2801 to 10015, 2803 to 10017, 2804 to 10019,
        )

    private val cockatrices =
        listOf(
            2785 to 10035, 2786 to 10038, 2789 to 10032, 2791 to 10036,
            2795 to 10033, 2797 to 10036, 2799 to 10031,
        )

    init {
        place(kurasks, listOf("npc.kurask_410", "npc.kurask_411"))
        place(jellies, JELLY_ROTATION)
        place(turoths, TUROTH_ROTATION)
        place(basilisks, listOf("npc.basilisk_417", "npc.basilisk_418"))
        place(pyrefiends, PYREFIEND_ROTATION)
        place(caveCrawlers, CAVE_CRAWLER_ROTATION)
        place(rockslugs, listOf("npc.rockslug_421", "npc.rockslug_422"))
        place(cockatrices, listOf("npc.cockatrice_419"))
    }

    /**
     * Spawn one npc per published pin, cycling through [rotation] so a monster's several cache ids
     * are all used rather than one standing in for the lot. Facing is cycled too, which is cosmetic:
     * the wiki does not publish which way any of them face.
     */
    private fun place(
        tiles: List<Pair<Int, Int>>,
        rotation: List<String>,
    ) {
        tiles.forEachIndexed { index, (x, z) ->
            spawnNpc(
                npc = rotation[index % rotation.size],
                x = x,
                z = z,
                height = 0,
                walkRadius = WALK_RADIUS,
                direction = FACINGS[index % FACINGS.size],
            )
        }
    }

    private companion object {
        const val WALK_RADIUS = 5

        val FACINGS = listOf(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)

        val JELLY_ROTATION =
            listOf(
                "npc.jelly_437",
                "npc.jelly_438",
                "npc.jelly_439",
                "npc.jelly_440",
                "npc.jelly_441",
                "npc.jelly_442",
            )

        /** Cycled across one shared pin list - see the class comment. */
        val TUROTH_ROTATION =
            listOf("npc.turoth_430", "npc.turoth_429", "npc.turoth_428", "npc.turoth_427")

        val PYREFIEND_ROTATION =
            listOf(
                "npc.pyrefiend_433",
                "npc.pyrefiend_434",
                "npc.pyrefiend_435",
                "npc.pyrefiend_436",
            )

        val CAVE_CRAWLER_ROTATION =
            listOf(
                "npc.cave_crawler_406",
                "npc.cave_crawler_407",
                "npc.cave_crawler_408",
                "npc.cave_crawler_409",
            )
    }
}
