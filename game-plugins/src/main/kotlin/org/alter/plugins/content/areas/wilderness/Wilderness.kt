package org.alter.plugins.content.areas.wilderness

import org.alter.game.model.Area
import org.alter.game.model.Tile

/**
 * The static description of the Wilderness: where it is, what is in it, and which of its
 * sub-areas change the rules.
 *
 * The level maths itself lives on the tile ([org.alter.api.ext.getWildernessLevel]) because the
 * combat and teleport code in `content/combat` and `content/magic` already depended on it there.
 * This file is everything built on top of that: named places, multi-combat zones and the obelisk
 * network.
 *
 * ## On the coordinates in here
 *
 * Two different standards are applied deliberately, because two different things are at stake:
 *
 * - **Anything that drives a mechanic is exact and cache-verified.** The obelisk centres in
 *   [OBELISKS], and the lever tiles in [WildernessLever], were read out of this project's own
 *   cache with `gradlew :game-server:agilityLocDump` rather than copied off the wiki.
 * - **The named areas in [LOCATIONS] are approximate bounding boxes.** They exist to tell a
 *   player where they are and to give the obelisk network somewhere to name, and being a few
 *   tiles out at an edge costs nothing. They are not survey data and should not be relied on if
 *   something mechanical ever comes to depend on them.
 *
 * [MULTI_AREAS] sits between the two: it is mechanical, but the real boundaries are not published
 * as coordinates anywhere and are not in the cache either - multi-combat is server-side state in
 * the real game, which is why nothing in this codebase could read it. The boxes are drawn around
 * the twelve wilderness areas the wiki's Multicombat area page lists, which is the best source
 * available. They are a large improvement on what was here before (the Wilderness had no
 * multi-combat zones at all), but they are not tile-exact, and a fight standing right on a
 * boundary may disagree with the real game.
 */
object Wilderness {
    /**
     * Above this level ordinary teleports refuse to fire. The members-only methods capped at
     * [DEEP_TELEPORT_CAP] keep working for another ten levels.
     */
    const val TELEPORT_CAP = 20

    /** The cap for the glory/wealth-tier methods, which is also where the deep Wilderness starts. */
    const val DEEP_TELEPORT_CAP = 30

    /** The deepest level the map reaches, in the far north. */
    const val MAX_LEVEL = 56

    /**
     * The six Wilderness obelisks.
     *
     * Every field here came out of a cache scan for objects named `Obelisk`, which found exactly
     * these six ids placed in the Wilderness, each as four pillars on the corners of a 5x5 - so
     * the centre is the pillars' midpoint. The levels are then what
     * [org.alter.api.ext.getWildernessLevel] returns for those centres, and they agree with the
     * six levels the wiki publishes (13/19/27/35/44/50), which is the cross-check that both these
     * coordinates and the corrected level formula are right.
     */
    val OBELISKS: List<Obelisk> =
        listOf(
            Obelisk(level = 13, obj = "object.obelisk_14829", centre = Tile(3156, 3620)),
            Obelisk(level = 19, obj = "object.obelisk_14830", centre = Tile(3227, 3667)),
            Obelisk(level = 27, obj = "object.obelisk_14827", centre = Tile(3035, 3732)),
            Obelisk(level = 35, obj = "object.obelisk_14828", centre = Tile(3106, 3794)),
            Obelisk(level = 44, obj = "object.obelisk_14826", centre = Tile(2980, 3866)),
            Obelisk(level = 50, obj = "object.obelisk_14831", centre = Tile(3307, 3916)),
        )

    /** Named places, roughly south to north. Approximate boxes - see this object's doc comment. */
    val LOCATIONS: List<WildernessLocation> =
        listOf(
            WildernessLocation("Ferox Enclave", Area(3125, 3617, 3155, 3646)),
            WildernessLocation("the Dark Warriors' Fortress", Area(3010, 3620, 3040, 3646)),
            WildernessLocation("the Chaos Temple", Area(3222, 3600, 3250, 3625)),
            WildernessLocation("the Bone Yard", Area(3025, 3630, 3060, 3670)),
            WildernessLocation("the Graveyard of Shadows", Area(3140, 3650, 3190, 3690)),
            WildernessLocation("the Wilderness Bandit Camp", Area(3028, 3684, 3060, 3712)),
            WildernessLocation("the Wilderness God Wars Dungeon", Area(3010, 3705, 3070, 3740)),
            WildernessLocation("the Forgotten Cemetery", Area(2965, 3730, 3010, 3765)),
            WildernessLocation("the Abandoned Farm", Area(3315, 3690, 3355, 3720)),
            WildernessLocation("Venenatis' web", Area(3310, 3730, 3350, 3760)),
            WildernessLocation("the Lava Maze", Area(3010, 3840, 3075, 3890)),
            WildernessLocation("Callisto's den", Area(3280, 3820, 3320, 3860)),
            WildernessLocation("the Demonic Ruins", Area(3270, 3870, 3300, 3900)),
            WildernessLocation("Lava Dragon Isle", Area(3170, 3800, 3230, 3855)),
            WildernessLocation("the Deserted Keep", Area(3140, 3910, 3170, 3940)),
            WildernessLocation("the Wilderness Resource Area", Area(3170, 3920, 3200, 3945)),
            WildernessLocation("the Mage Arena", Area(3090, 3930, 3120, 3965)),
            WildernessLocation("the Wilderness Agility Course", Area(2985, 3900, 3010, 3960)),
            WildernessLocation("Rogues' Castle", Area(3270, 3915, 3310, 3950)),
            WildernessLocation("the Scorpion Pit", Area(3220, 3940, 3250, 3960)),
        )

    /**
     * The wilderness areas the wiki lists as multi-combat. Approximate - see this object's own
     * doc comment for the reason there is no better source than the wiki's prose here.
     */
    val MULTI_AREAS: List<Area> =
        listOf(
            Area(3010, 3620, 3040, 3646), // Dark Warriors' Fortress
            Area(3222, 3600, 3250, 3625), // Chaos Temple (the eastern one, level 13)
            Area(3140, 3650, 3190, 3690), // Graveyard of Shadows
            Area(3028, 3684, 3060, 3712), // Wilderness Bandit Camp
            Area(2965, 3730, 3010, 3765), // Forgotten Cemetery
            Area(3315, 3690, 3355, 3720), // Abandoned Farm
            Area(3010, 3840, 3075, 3890), // Lava Maze
            Area(3170, 3800, 3230, 3855), // Lava Dragon Isle
            Area(3270, 3870, 3300, 3900), // Demonic Ruins
            Area(3255, 3880, 3300, 3920), // Deep Wilderness castle ruins
            Area(3270, 3915, 3310, 3950), // Rogues' Castle
            Area(3220, 3940, 3250, 3960), // Scorpion Pit
            Area(2985, 3900, 3010, 3930), // Wilderness Agility Course, southern half only
        )

    /** The name of the place [tile] falls in, or null if it is somewhere unremarkable. */
    fun locationAt(tile: Tile): String? = LOCATIONS.firstOrNull { it.area.contains(tile) }?.label

    /** Whether [tile] is in one of the Wilderness' multi-combat zones. */
    fun isMultiCombat(tile: Tile): Boolean = MULTI_AREAS.any { it.contains(tile) }
}

/** One of the six teleport obelisks. [obj] is the RSCM name of its four corner pillars. */
data class Obelisk(
    val level: Int,
    val obj: String,
    val centre: Tile,
)

/** A named slice of the Wilderness, used for player-facing messaging. */
data class WildernessLocation(
    val label: String,
    val area: Area,
)
