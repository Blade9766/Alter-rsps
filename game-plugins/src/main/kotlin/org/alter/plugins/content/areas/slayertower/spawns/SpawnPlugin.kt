package org.alter.plugins.content.areas.slayertower.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The monsters of the Slayer Tower, north-east of Canifis.
 *
 * Every tile is a published map pin from the monster's own wiki page - the tower article names its
 * residents per floor but carries no coordinates, so each list came from the monster's side, the
 * same sourcing `areas/taverleydungeon/spawns` uses. Combat stats, drops and Slayer requirements
 * live in `content/npcs/slayer`.
 *
 * Three floors, and the floor is what the pins do *not* carry: a `LocLine` publishes x and z but
 * takes its plane from the `{{FloorNumber}}` in its location text. So the plane is read off that
 * text rather than the pin - ground floor 0 for the crawling hands and banshees, 1 for the infernal
 * mages, bloodvelds and aberrant spectres, 2 for the gargoyles, nechryaels and abyssal demons.
 *
 * **The pins do not say which *version* stands where.** The crawling hand's Slayer Tower `LocLine`
 * is a single block reading `levels = 8, 12` over all 24 pins, not one block per version - the
 * location genuinely holds a mix and the wiki does not record which is which. The two versions are
 * therefore alternated across the pins, which reproduces the published mix without inventing a
 * per-tile claim the source does not make. Note the level *7* hand is deliberately absent: it is not
 * in that `levels =` list, so it does not stand in this tower at all.
 *
 * **The basement is not built.** Its bloodvelds, gargoyles, nechryaels and abyssal demons have their
 * own pins at z 9928-9974, and every one of them is task-only - the wiki is explicit that the
 * basement monsters cannot be attacked without an active assignment for them. `Slayer.isOnTask` can
 * answer that now, but the basement also needs its own entrance and the gate wiring in
 * `Combat.canEngage`, so it goes in as its own piece of work rather than half-built here.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    /** Ground floor. `levels = 8, 12` over one shared pin list - see the class comment. */
    private val crawlingHands =
        listOf(
            3412 to 3549, 3412 to 3563, 3412 to 3575, 3413 to 3559,
            3413 to 3571, 3420 to 3551, 3421 to 3573, 3424 to 3558,
            3424 to 3567, 3428 to 3544, 3428 to 3553, 3430 to 3571,
            3439 to 3574, 3410 to 3570, 3411 to 3546, 3411 to 3560,
            3417 to 3570, 3421 to 3544, 3423 to 3555, 3429 to 3574,
            3441 to 3571, 3419 to 3574, 3427 to 3548, 3428 to 3569,
        )

    private val crawlingHandRotation =
        listOf(
            "npc.crawling_hand_448",
            "npc.crawling_hand_453",
            "npc.crawling_hand_449",
            "npc.crawling_hand_454",
            "npc.crawling_hand_451",
            "npc.crawling_hand_456",
            "npc.crawling_hand_452",
            "npc.crawling_hand_457",
        )

    /** Ground floor. */
    private val banshees =
        listOf(
            3429 to 3564, 3433 to 3552, 3436 to 3559, 3439 to 3539,
            3439 to 3544, 3440 to 3560, 3443 to 3546, 3444 to 3537,
        )

    /** First floor, north half. */
    private val infernalMages =
        listOf(
            3433 to 3556, 3434 to 3563, 3434 to 3570, 3434 to 3575,
            3435 to 3559, 3438 to 3555, 3439 to 3569, 3439 to 3575,
            3440 to 3564, 3442 to 3556, 3443 to 3560, 3443 to 3572,
            3446 to 3569, 3446 to 3575, 3448 to 3573,
        )

    private val infernalMageRotation =
        listOf(
            "npc.infernal_mage_443",
            "npc.infernal_mage_444",
            "npc.infernal_mage_445",
            "npc.infernal_mage_446",
            "npc.infernal_mage_447",
        )

    /** First floor, west half. */
    private val bloodvelds =
        listOf(
            3409 to 3571, 3411 to 3567, 3411 to 3576, 3412 to 3560,
            3416 to 3557, 3416 to 3561, 3416 to 3573, 3417 to 3565,
            3419 to 3559, 3421 to 3574, 3422 to 3567, 3424 to 3560,
            3424 to 3564, 3426 to 3557, 3426 to 3573,
        )

    private val bloodveldRotation =
        listOf("npc.bloodveld_484", "npc.bloodveld_485", "npc.bloodveld_486", "npc.bloodveld_487")

    /** First floor, south half. */
    private val aberrantSpectres =
        listOf(
            3413 to 3550, 3424 to 3551, 3428 to 3543, 3428 to 3551,
            3438 to 3549, 3411 to 3534, 3423 to 3542, 3435 to 3545,
            3442 to 3550, 3413 to 3545, 3420 to 3537, 3427 to 3539,
            3431 to 3548, 3442 to 3544,
        )

    private val aberrantSpectreRotation =
        listOf(
            "npc.aberrant_spectre_2",
            "npc.aberrant_spectre_3",
            "npc.aberrant_spectre_4",
            "npc.aberrant_spectre_5",
            "npc.aberrant_spectre_6",
            "npc.aberrant_spectre_7",
        )

    /** Top floor, south half. */
    private val gargoyles =
        listOf(
            3432 to 3540, 3435 to 3548, 3437 to 3537, 3439 to 3542,
            3439 to 3548, 3443 to 3548, 3445 to 3541, 3446 to 3535,
        )

    private val gargoyleRotation = listOf("npc.gargoyle_412", "npc.gargoyle_413")

    /** Top floor, east half. */
    private val nechryaels =
        listOf(
            3433 to 3570, 3435 to 3574, 3437 to 3566, 3439 to 3570,
            3441 to 3574, 3443 to 3570, 3444 to 3566, 3445 to 3560,
            3446 to 3574, 3447 to 3563, 3449 to 3572,
        )

    /** Top floor, west half. */
    private val abyssalDemons =
        listOf(
            3408 to 3573, 3411 to 3564, 3411 to 3570, 3411 to 3576,
            3413 to 3573, 3414 to 3562, 3415 to 3569, 3418 to 3567,
            3419 to 3562, 3421 to 3573, 3422 to 3570, 3423 to 3565,
            3426 to 3569, 3427 to 3572,
        )

    private val abyssalDemonRotation = listOf("npc.abyssal_demon_415", "npc.abyssal_demon_416")

    init {
        place(crawlingHands, crawlingHandRotation, height = 0)
        place(banshees, listOf("npc.banshee_414"), height = 0)

        place(infernalMages, infernalMageRotation, height = 1)
        place(bloodvelds, bloodveldRotation, height = 1)
        place(aberrantSpectres, aberrantSpectreRotation, height = 1)

        place(gargoyles, gargoyleRotation, height = 2)
        place(nechryaels, listOf("npc.nechryael_8"), height = 2)
        place(abyssalDemons, abyssalDemonRotation, height = 2)
    }

    /**
     * Spawn one npc per published pin, cycling through [rotation] so a monster's several cache ids
     * are all used rather than one id standing in for the lot. Facing is cycled the same way, which
     * is cosmetic - the wiki does not publish which way any of them face.
     */
    private fun place(
        tiles: List<Pair<Int, Int>>,
        rotation: List<String>,
        height: Int,
    ) {
        tiles.forEachIndexed { index, (x, z) ->
            spawnNpc(
                npc = rotation[index % rotation.size],
                x = x,
                z = z,
                height = height,
                walkRadius = WALK_RADIUS,
                direction = FACINGS[index % FACINGS.size],
            )
        }
    }

    private companion object {
        /**
         * The tower's rooms are small and its corridors narrow; this is the radius
         * `areas/taverleydungeon/spawns` settled on for dungeon monsters that should stay in the
         * room they were placed in.
         */
        const val WALK_RADIUS = 5

        val FACINGS = listOf(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
    }
}
