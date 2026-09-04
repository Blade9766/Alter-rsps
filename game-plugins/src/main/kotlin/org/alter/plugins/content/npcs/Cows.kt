package org.alter.plugins.content.npcs

import org.alter.api.cfg.Sound

/**
 * The bovines: cows and cow calves, where they stand and what they are made of. Every pin the
 * OSRS Wiki's Cow and Cow calf pages publish, from their `Locations` tables' `LocLine` rows.
 * [CowPlugin] is the wiring.
 *
 * **This replaces sixteen invented tiles.** The plugin used to spawn one hand-written list
 * called `lumbridgeCowSpawns`, sixteen coordinates in the 3248-3263 / 3259-3292 block. They
 * are not on the wiki. They land roughly across the Lumbridge East farm, so in game they
 * looked plausible, which is exactly why they survived - but they were somebody's estimate of
 * where cows go, and the real fields are published to the tile. The whole list is gone.
 *
 * **The tiles are cache-verified.** `CowVerify` reads every one out of this project's own map
 * files and asserts it has a floor and is not flagged BLOCK_WALK - across the whole
 * **footprint**, not just the pin, because these are size-2 npcs and a spawn tile can be clear
 * while the square the animal actually occupies is not. All 163 pass in every mapsquare, so
 * nothing published is dropped for being off the map.
 *
 * Left out, each for a reason the wiki row states itself:
 *
 * - **"North-west of Prifddinas (Gwenith) during Song of the Elves"** (`mapID = 29`). It is
 *   the quest instance of the Gwenith herd, not a second herd - the same seven cows re-pinned
 *   inside the instanced map. The real-world row at 2189,3418 is included, so those cows exist
 *   exactly once.
 * - **The dairy cow.** Not an npc on this server at all: it is scenery with a Milk action,
 *   handled by `mechanics/dairy`.
 *
 * **Which id.** Both pages list versions that share one unversioned stat block, so within a
 * page the choice is cosmetic and the ids are dealt round-robin - the same stable-but-arbitrary
 * choice the bankers and goblins make. Two ids are held back from the rotation:
 *
 * - **Cow 2795 and 5842 are not held back but are location-bound**: 2795 is used only on the
 *   Isle of Souls and 5842 only in Zanaris, because that is what their version names say. Note
 *   5842 additionally carries a **Talk-to** option in this cache (it is the Fairy Tale I cow);
 *   no dialogue is bound to it, so that option currently does nothing.
 * - **Calf 2801 is held back.** The Cow calf page files it as version 3, "Farmland", and gives
 *   it `size3 = 1` where the other two calves are `size = 2` - which this cache confirms. That
 *   is a *mechanical* difference, not a cosmetic one, so it cannot be dealt round-robin with
 *   2792 and 2794. The page publishes no location-to-version mapping, and guessing that
 *   "Farmland" means the farms would be inventing one. It keeps its combat def and stays
 *   unspawned, on the same terms as chicken 9488 in `critters/ChickenSpawns`.
 *
 * `npc.cow` is 2790 - the bare, unsuffixed rscm name is version 1, and it was the only id the
 * plugin ever defined. Every other cow id was inheriting
 * [org.alter.game.model.combat.NpcCombatDef.DEFAULT]'s 10 hitpoints and zeroed stats.
 */
internal data class CowHerd(
    /** The wiki's own `location` text, kept verbatim so a row can be found again. */
    val location: String,
    /** The ids for this herd, dealt round-robin across [tiles]. */
    val npcKeys: List<String>,
    val tiles: List<Pair<Int, Int>>,
    val walkRadius: Int = Cows.PASTURE_RADIUS,
)

/**
 * One page's worth of combat numbers. Cows and calves differ in every one of these, which is
 * why the calves could not simply reuse the cow's def: a calf has 6 hitpoints to a cow's 8,
 * worse bonuses across the board, and pays 6 Slayer xp rather than 8.
 *
 * Everything *not* here is shared and lives as a constant on [Cows], because both pages publish
 * the same value: attack speed 4, respawn 45, level 1 in all six skills, Crush, `aggressive =
 * No`, 100% poison and venom resistance, the same three 100% drops and the same 1/128 beginner
 * clue.
 */
internal data class Bovine(
    val label: String,
    /** Every id that gets a combat def, whether or not anything spawns it. */
    val combatDefIds: List<String>,
    val hitpoints: Int,
    val attackBonus: Int,
    val strengthBonus: Int,
    val defenceBonus: Int,
    val slayerXp: Double,
    /**
     * Combat sounds. The one thing the two pages genuinely differ on beyond the numbers - a
     * calf has its own attack, hit and death sounds. Animations are *not* here: every cow and
     * calf id in `npc-animations/openosrs-animations.json` observes the same 5849/5850/5851
     * set, so they share the cow animations.
     */
    val attackSound: Int,
    val blockSound: Int,
    val deathSound: Int,
    val herds: List<CowHerd>,
)

internal object Cows {
    // ---- Shared by both pages ----

    /** Wiki `attack speed = 4` and `respawn = 45`, both already in game ticks. */
    const val ATTACK_SPEED = 4
    const val RESPAWN_CYCLES = 45

    /** Wiki `att`, `str`, `def`, `mage` and `range`, all 1 on both pages. */
    const val ATTACK = 1
    const val STRENGTH = 1
    const val DEFENCE = 1
    const val MAGIC = 1
    const val RANGED = 1

    /** The only rolled loot either page has. Everything else is a 100% drop. */
    const val BEGINNER_CLUE_CHANCE = 1.0 / 128.0

    /**
     * How far a penned animal strays from its spawn tile.
     *
     * Kept at 1, as it already was, and the original comment had the right reason:
     * [org.alter.plugins.content.mechanics.npcwalk.NpcRandomWalkPlugin] picks a tile in a
     * square around the spawn point without checking whether anything can walk there, so a
     * generous radius on a penned animal picks tiles on the far side of the fence. It matters
     * more here than anywhere else: these are 2x2, so they need more clearance than the tile
     * count suggests.
     */
    const val PASTURE_RADIUS = 1

    /** For herds on open hillside and farmland, where wandering a few tiles hits nothing. */
    const val OPEN_RADIUS = 3

    // ---- Cow ids. Versions 1-3 are graphical variants; 4 and 5 are location-bound. ----

    val STANDARD_IDS = listOf("npc.cow", "npc.cow_2791", "npc.cow_2793")
    val ISLE_OF_SOULS_IDS = listOf("npc.cow_2795")
    val ZANARIS_IDS = listOf("npc.cow_5842")

    // ---- Calf ids. 2801 is defined but not spawned - see the class doc. ----

    val CALF_IDS = listOf("npc.cow_calf", "npc.cow_calf_2794")
    val FARMLAND_CALF_ID = "npc.cow_calf_2801"

    val COW =
        Bovine(
            label = "Cow",
            combatDefIds = STANDARD_IDS + ISLE_OF_SOULS_IDS + ZANARIS_IDS,
            hitpoints = 8,
            // Wiki `attbns`/`strbns`. Neither was set before - the plugin declared only the
            // five defence bonuses - so every cow fought with the builder's 0 default.
            attackBonus = -15,
            strengthBonus = -15,
            defenceBonus = -21,
            slayerXp = 8.0,
            attackSound = Sound.COW_ATTACK,
            blockSound = Sound.COW_HIT,
            deathSound = Sound.COW_DEATH,
            herds =
                listOf(
                    // ---- Misthalin ----
                    CowHerd(
                        // The wiki's largest single row by a wide margin: one LocLine covering
                        // the whole strip from the Champions' Guild field down the west side of
                        // the river to Lumbridge. Kept as one herd because that is how it is
                        // published.
                        location = "Field south of the Champions' Guild",
                        npcKeys = STANDARD_IDS,
                        tiles =
                            listOf(
                                3155 to 3331, 3156 to 3342, 3158 to 3336, 3160 to 3329, 3162 to 3335,
                                3164 to 3340, 3169 to 3335, 3169 to 3341, 3170 to 3329, 3173 to 3332,
                                3174 to 3341, 3177 to 3333, 3180 to 3340, 3182 to 3329, 3184 to 3334,
                                3184 to 3341, 3187 to 3336, 3189 to 3332, 3195 to 3335, 3197 to 3331,
                                3154 to 3325, 3155 to 3321, 3160 to 3318, 3165 to 3320, 3165 to 3326,
                                3173 to 3323, 3178 to 3318, 3182 to 3321, 3186 to 3326, 3187 to 3322,
                                3188 to 3314, 3188 to 3318, 3191 to 3311, 3194 to 3293, 3195 to 3287,
                                3196 to 3283, 3196 to 3311, 3197 to 3291, 3197 to 3297, 3197 to 3300,
                                3197 to 3316, 3197 to 3320, 3200 to 3284, 3201 to 3295, 3202 to 3289,
                                3203 to 3286, 3204 to 3293, 3204 to 3298, 3206 to 3290, 3208 to 3294,
                                3209 to 3288, 3209 to 3300, 3211 to 3291,
                            ),
                    ),
                    CowHerd(
                        // The field the invented sixteen were guessing at.
                        location = "Lumbridge East farm",
                        npcKeys = STANDARD_IDS,
                        tiles =
                            listOf(
                                3254 to 3255, 3254 to 3258, 3254 to 3262, 3258 to 3260, 3261 to 3259,
                                3243 to 3295, 3244 to 3283, 3244 to 3289, 3246 to 3293, 3247 to 3284,
                                3250 to 3293, 3252 to 3282, 3252 to 3288, 3254 to 3267, 3255 to 3278,
                                3256 to 3292, 3257 to 3286, 3258 to 3265, 3261 to 3270, 3261 to 3282,
                                3262 to 3277,
                            ),
                    ),

                    // ---- Asgarnia ----
                    CowHerd(
                        location = "Crafting Guild",
                        npcKeys = STANDARD_IDS,
                        tiles =
                            listOf(
                                2917 to 3289, 2920 to 3285, 2921 to 3287, 2924 to 3277, 2924 to 3288,
                                2925 to 3282, 2926 to 3271, 2930 to 3269, 2933 to 3274, 2936 to 3274,
                            ),
                    ),
                    CowHerd(
                        location = "South Falador Farm",
                        npcKeys = STANDARD_IDS,
                        tiles =
                            listOf(
                                3025 to 3307, 3029 to 3305, 3029 to 3311, 3030 to 3300, 3033 to 3305,
                                3037 to 3306, 3040 to 3310,
                            ),
                    ),

                    // ---- Kandarin ----
                    CowHerd(
                        location = "Ardougne Farm",
                        npcKeys = STANDARD_IDS,
                        tiles =
                            listOf(
                                2657 to 3341, 2658 to 3351, 2660 to 3344, 2664 to 3341, 2664 to 3348,
                                2664 to 3352, 2666 to 3344, 2670 to 3348, 2671 to 3342, 2672 to 3354,
                            ),
                    ),
                    CowHerd(
                        location = "Sinclair Mansion",
                        npcKeys = STANDARD_IDS,
                        tiles = listOf(2733 to 3561),
                    ),
                    CowHerd(
                        location = "Nightmare Zone",
                        npcKeys = STANDARD_IDS,
                        tiles = listOf(2604 to 3114),
                        walkRadius = OPEN_RADIUS,
                    ),
                    CowHerd(
                        location = "West of Nightmare Zone",
                        npcKeys = STANDARD_IDS,
                        tiles = listOf(2580 to 3117, 2582 to 3122, 2588 to 3119),
                        walkRadius = OPEN_RADIUS,
                    ),

                    // ---- Tirannwn ----
                    CowHerd(
                        // The real-world Gwenith herd. Its Song of the Elves twin is not
                        // spawned - see the class doc.
                        location = "North-west of Prifddinas (Gwenith)",
                        npcKeys = STANDARD_IDS,
                        tiles =
                            listOf(
                                2189 to 3418, 2190 to 3415, 2191 to 3420, 2192 to 3416, 2193 to 3419,
                                2194 to 3421, 2195 to 3416,
                            ),
                        walkRadius = OPEN_RADIUS,
                    ),

                    // ---- Kourend and the Kebos Lowlands ----
                    CowHerd(
                        location = "North of Hosidius Town Square",
                        npcKeys = STANDARD_IDS,
                        tiles = listOf(1747 to 3640, 1751 to 3636, 1757 to 3638, 1758 to 3643, 1763 to 3638),
                        walkRadius = OPEN_RADIUS,
                    ),
                    CowHerd(
                        location = "Kebos Lowlands - Gordon's farm",
                        npcKeys = STANDARD_IDS,
                        tiles = listOf(1261 to 3692, 1262 to 3689),
                    ),
                    CowHerd(
                        location = "Kebos Lowlands - Keith's farm",
                        npcKeys = STANDARD_IDS,
                        tiles = listOf(1306 to 3713, 1310 to 3717),
                    ),

                    // ---- The two location-bound versions ----
                    CowHerd(
                        location = "Isle of Souls",
                        npcKeys = ISLE_OF_SOULS_IDS,
                        tiles = listOf(2148 to 2804, 2150 to 2800, 2151 to 2807),
                        walkRadius = OPEN_RADIUS,
                    ),
                    CowHerd(
                        location = "Zanaris",
                        npcKeys = ZANARIS_IDS,
                        tiles = listOf(2435 to 4442, 2439 to 4457, 2441 to 4448),
                        walkRadius = OPEN_RADIUS,
                    ),
                ),
        )

    val CALF =
        Bovine(
            label = "Cow calf",
            combatDefIds = CALF_IDS + FARMLAND_CALF_ID,
            hitpoints = 6,
            attackBonus = -20,
            strengthBonus = -20,
            defenceBonus = -26,
            slayerXp = 6.0,
            attackSound = Sound.CALF_ATTACK,
            blockSound = Sound.CALF_HIT,
            deathSound = Sound.CALF_DEATH,
            herds =
                listOf(
                    CowHerd(
                        location = "Lumbridge East farm",
                        npcKeys = CALF_IDS,
                        tiles =
                            listOf(
                                3247 to 3279, 3247 to 3287, 3254 to 3291, 3256 to 3273, 3257 to 3280,
                                3261 to 3273, 3261 to 3289, 3262 to 3287,
                            ),
                    ),
                    CowHerd(
                        // The calves have a Lumbridge West farm row of their own; the cows do
                        // not - their west-side pins are folded into the Champions' Guild row.
                        location = "Lumbridge West farm",
                        npcKeys = CALF_IDS,
                        tiles = listOf(3194 to 3289, 3194 to 3300, 3200 to 3299, 3205 to 3285, 3207 to 3299),
                    ),
                    CowHerd(
                        location = "Field south of Champions' Guild",
                        npcKeys = CALF_IDS,
                        tiles =
                            listOf(
                                3155 to 3336, 3158 to 3329, 3160 to 3342, 3173 to 3336, 3177 to 3339,
                                3187 to 3331, 3191 to 3336, 3156 to 3326, 3171 to 3326, 3177 to 3322,
                                3181 to 3315,
                            ),
                    ),
                    CowHerd(
                        location = "South Falador Farm",
                        npcKeys = CALF_IDS,
                        tiles = listOf(3027 to 3308, 3031 to 3311, 3042 to 3311),
                    ),
                    CowHerd(
                        location = "Crafting Guild",
                        npcKeys = CALF_IDS,
                        tiles = listOf(2920 to 3288, 2933 to 3276),
                    ),
                    CowHerd(
                        location = "North of Hosidius Town Square",
                        npcKeys = CALF_IDS,
                        tiles = listOf(1747 to 3637, 1754 to 3637, 1761 to 3642),
                        walkRadius = OPEN_RADIUS,
                    ),
                    CowHerd(
                        location = "Kebos Lowlands - Keith's farm",
                        npcKeys = CALF_IDS,
                        tiles = listOf(1309 to 3714),
                    ),
                    CowHerd(
                        location = "Kebos Lowlands - Gordon's farm",
                        npcKeys = CALF_IDS,
                        tiles = listOf(1257 to 3691),
                    ),
                    CowHerd(
                        location = "South-west Isle of Souls",
                        npcKeys = CALF_IDS,
                        tiles = listOf(2152 to 2803),
                        walkRadius = OPEN_RADIUS,
                    ),
                ),
        )

    val ALL = listOf(COW, CALF)

    /**
     * The one place a published calf pin and a published cow pin put two size-2 footprints on
     * the same ground: calf (2920, 3288) and cow (2921, 3287), both at the Crafting Guild.
     *
     * Both are on the wiki, and both are kept. These pins are observed positions of animals
     * that wander, so a pair of them overlapping says the two were photographed a tile apart,
     * not that the spawn data is wrong. It is harmless on this server for a concrete reason:
     * [org.alter.game.model.World.spawn] writes no collision for an npc, so nothing in this
     * engine models one animal occupying another's square - they simply stand together for a
     * moment and then wander apart.
     *
     * `CowVerify` asserts this is the *only* such pair, so a genuine transcription slip that
     * stacks two animals still fails the build. If npc occupancy is ever added to the engine,
     * this is the note to come back to.
     */
    val KNOWN_OVERLAP = (2920 to 3288) to (2921 to 3287)
}
