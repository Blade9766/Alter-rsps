package org.alter.plugins.content.skills.strength

/**
 * One crossbow-and-grapple shortcut.
 *
 * Every level here is quoted from the shortcut's own wiki page rather than from the mith grapple
 * page's summary table, because that table's three columns are easy to read in the wrong order -
 * the Water Obelisk row reads "36 Agility, 22 Strength, 39 Ranged", and a second source had the
 * Strength at 38. Each row below was checked against the page for that individual obstacle.
 *
 * [maxCrossing] is how far past the object the far side may be, in tiles. The landing itself is not
 * written down: the plugin walks outward from the object along the axis the player approached on and
 * stops at the first tile they can actually stand on. That keeps every one of these two-way without
 * a tile per direction, and - unlike a hard-coded landing - it cannot drop a player onto a blocked
 * tile if the cache moves underneath it. The cost is that a crossing wider than [maxCrossing] simply
 * reports that it cannot be used, rather than teleporting someone into the water.
 */
data class GrappleShortcut(
    val name: String,
    val objects: List<String>,
    val agility: Int,
    val strength: Int,
    val ranged: Int,
    val maxCrossing: Int,
    val ticks: Int = 2,
)

/**
 * The grapple shortcuts, and the objects in this cache that carry them.
 *
 * A scan of every map region for the objects whose only action is "Grapple" found nine placements at
 * ground level. Five are the shortcuts below. The other four are inside content this server does not
 * build yet - the Dorgesh-Kaan agility course pylons, a pillar and a pair of beams in dungeon
 * regions, and the Prifddinas pipes - so they are deliberately left unbound rather than wired to a
 * place no player can reach.
 */
object GrappleShortcuts {
    /**
     * The ammunition-slot mith grapple is item **9419**, not the 9418 that plain `item.mith_grapple`
     * resolves to. Both are named "Mith grapple" in the cache and only 9419 has an equip slot at
     * all, so checking for 9418 would have meant no player could ever satisfy the requirement.
     */
    const val MITH_GRAPPLE = "item.mith_grapple_9419"

    val ALL =
        listOf(
            /*
             * North Falador wall - 11 Agility, 37 Strength, 19 Ranged. Two wall locs sit back to
             * back at (3033, 3390) and (3032, 3389); both carry the same crossing.
             */
            GrappleShortcut(
                name = "the Falador wall",
                objects = listOf("object.wall_17049", "object.wall_17050"),
                agility = 11,
                strength = 37,
                ranged = 19,
                maxCrossing = 4,
            ),
            /*
             * Yanille's south-west wall - 39 Agility, 38 Strength, 21 Ranged. Also a back-to-back
             * pair, at (2556, 3073) and (2556, 3074), sharing one object id.
             */
            GrappleShortcut(
                name = "the Yanille wall",
                objects = listOf("object.wall_17047"),
                agility = 39,
                strength = 38,
                ranged = 21,
                maxCrossing = 4,
            ),
            /*
             * The broken raft across the River Lum at (3252, 3179) - 8 Agility, 19 Strength, 37
             * Ranged. The wiki's prose says 17 Strength and its own table says 19; the table is
             * taken here because it is the one the shortcut infobox renders from.
             */
            GrappleShortcut(
                name = "the River Lum",
                objects = listOf("object.broken_raft"),
                agility = 8,
                strength = 19,
                ranged = 37,
                maxCrossing = 12,
            ),
            /*
             * The strong tree south of the Karamja volcano at (2873, 3134) - 53 Agility, 21
             * Strength, 42 Ranged, per the hard Karamja diary task that uses it.
             */
            GrappleShortcut(
                name = "the Karamja shore",
                objects = listOf("object.strong_tree_17074"),
                agility = 53,
                strength = 21,
                ranged = 42,
                maxCrossing = 14,
            ),
            /*
             * Water Obelisk Island to the Catherby shore - 36 Agility, 22 Strength, 39 Ranged. The
             * crossbow tree at (2841, 3434) is the anchor; the rocks at (2869, 3429) are the other
             * end of the same stretch of water and carry the same requirement.
             */
            GrappleShortcut(
                name = "the Catherby shore",
                objects = listOf("object.crossbow_tree_17062", "object.rocks_17042"),
                agility = 36,
                strength = 22,
                ranged = 39,
                maxCrossing = 14,
            ),
        )
}
