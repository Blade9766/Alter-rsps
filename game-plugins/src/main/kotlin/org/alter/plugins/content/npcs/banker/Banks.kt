package org.alter.plugins.content.npcs.banker

import org.alter.game.model.Direction

/**
 * A teller's post: the tile behind a bank booth, and the [Direction] they face across it.
 *
 * These are not free-standing coordinates. Every one of them was derived from this server's own
 * cache: each bankable object (anything carrying a "Bank" action - bank booths, and the bank
 * tables and counters that a few banks use instead) was read out of the landscape files, and the
 * teller stands on the tile immediately behind it.
 *
 * **Which side is "behind"** comes from the object's rotation: `0` puts the teller south of the
 * booth, `1` west, `2` north, `3` east. That mapping was read off four banks whose layout settles
 * it beyond doubt, and it holds at every other bank checked since:
 *
 * - **Varrock west** - booths at x=3186 with the building's walls at x=3179 and x=3191. The east
 *   strip holds the staircase and the vault chests, the west half is the open floor players crowd
 *   into, so rotation 3 must be east.
 * - **Varrock east** - the "Climb-up" staircase sits north of the booth row and the bank tables
 *   sit south of it, so rotation 0 must be south.
 * - **Draynor** - the bank tables are west of the booth line, so rotation 1 must be west.
 * - **Edgeville** - the four booths form an L, two facing west and two facing north; the rule puts
 *   the tellers inside the corner the L encloses, which is exactly the pen Edgeville's bankers
 *   stand in.
 *
 * Two later checks are worth recording because they came out right with **opposite** rotations,
 * which is what a wrong rule would have failed: at Civitas illa Fortis west the rule puts the
 * tellers north and the player-side deposit box is south, and at Fortis east it puts them south
 * and the deposit box is north.
 *
 * The one exception is Lletya, whose bank counter (object 20127) is a different model with the
 * reversed default orientation, so its two tellers are mirrored to the other side. That is not a
 * guess either: the tiles north of Lletya's counters are walled off from the lounge beyond them
 * by a solid wall on their north edge, leaving a one-tile strip that can only be the teller side.
 */
internal data class Teller(val x: Int, val z: Int, val height: Int, val facing: Direction)

/** One banking location, and the npc ids OSRS staffs it with. */
internal data class Bank(
    val name: String,
    val npcKeys: List<String>,
    val tellers: List<Teller>,
) {
    /**
     * **Which id stands at which booth is not published.** Where a bank has more than one banker
     * id they are dealt round-robin over its tellers, the same deliberate-but-arbitrary assignment
     * the city guards use - it is a stable choice, not an observed fact, and it buys the thing that
     * actually reads as right in game: a bank staffed by a mix of faces rather than one clone.
     */
    val spawns: List<BankerSpawn>
        get() =
            tellers.mapIndexed { index, teller ->
                BankerSpawn(npcKeys[index % npcKeys.size], teller.x, teller.z, teller.height, teller.facing)
            }
}

internal data class BankerSpawn(
    val npcKey: String,
    val x: Int,
    val z: Int,
    val height: Int,
    val facing: Direction,
)

/**
 * Every town and city bank staffed by bankers, and where each of its bankers stands.
 *
 * **Ids.** Most banks in OSRS are staffed by the generic [GENERIC_BANKERS] - the five versions the
 * wiki's own Banker page lists (`1613,3094`, `1618`, `1633`, `1634`, `3089`). The banks that are
 * not use a location-specific npc instead, and every one of those ids below was resolved through
 * the wiki's `Special:Lookup?type=npc&id=` redirect, which names the page an id belongs to - so
 * Varrock's bankers really are 2897/2898, Al Kharid's 3090/3091, Canifis' 2633, and so on. Two of
 * Hosidius' four ids (6939, 6941) redirect to sections the wiki marks *removed*, so only 6940 and
 * 6942 are used.
 *
 * **Counts.** One banker per booth, as asked. Where the wiki publishes a teller count for a bank
 * it is recorded next to it: most match exactly or within one, the exceptions being Seers' Village
 * (8 tellers over 6 booths) and Canifis (2 tellers over 6 booths), where OSRS simply does not fill
 * every booth. Trimming those is a one-line edit here.
 *
 * **Gaps.** A booth whose back tile is unwalkable gets no banker - that tile is either void terrain
 * or holds a safe, a chest or a chair. Those are noted per bank. Notably this is why Lumbridge
 * Castle gets one banker rather than two, and it lines up with the wiki's teller counts more often
 * than not (Al Kharid, Falador west and Nardah all land on the published number this way).
 *
 * **Not here.** Banks reached through a bank chest have no teller to place, so Zanaris, Shilo
 * Village, Sophanem, Burgh de Rott, Port Khazard, Neitiznot, Jatizso, Corsair Cove, Ver Sinhaza,
 * Ferox Enclave and the Hosidius Kitchen are all absent by construction. So are the guild and
 * activity banks (Cooks', Warriors', Fishing, Farming and Hunter guilds, Castle Wars, the Duel
 * Arena, Void Knights' Outpost, the Clan Hall), which are not towns. The Grand Exchange is staffed
 * by Grand Exchange clerks, not bankers. Etceteria's single bank counter and the Varlamore bank
 * tables at Cam Torum, Mistrock, Quetzacalli Gorge, Aldarin and Auburnvale are left out
 * deliberately: their back tile is blocked or their geometry does not read cleanly enough from the
 * cache to place a teller without guessing.
 */
internal object Banks {
    /** The five versions of the plain Banker; `1613` and `3094` are both version 1. */
    val GENERIC_BANKERS =
        listOf(
            "npc.banker_1613",
            "npc.banker_3094",
            "npc.banker_1618",
            "npc.banker_1633",
            "npc.banker_1634",
            "npc.banker_3089",
        )

    val ALL: List<Bank> = listOf(

        // ---- Misthalin -----------------------------------------------------

        Bank(
            name = "Varrock west bank",
            npcKeys =
                listOf(
                    "npc.banker_2897",
                    "npc.banker_2898",
                ),
            // wiki: 6 tellers.
            tellers =
                listOf(
                    Teller(3187, 3436, 0, Direction.WEST), // booth 3186, 3436
                    Teller(3187, 3438, 0, Direction.WEST), // booth 3186, 3438
                    Teller(3187, 3440, 0, Direction.WEST), // booth 3186, 3440
                    Teller(3187, 3442, 0, Direction.WEST), // booth 3186, 3442
                    Teller(3187, 3444, 0, Direction.WEST), // booth 3186, 3444
                ),
        ),

        Bank(
            name = "Varrock east bank",
            npcKeys =
                listOf(
                    "npc.banker_2897",
                    "npc.banker_2898",
                ),
            // wiki: 6 tellers.
            tellers =
                listOf(
                    Teller(3251, 3418, 0, Direction.NORTH), // booth 3251, 3419
                    Teller(3252, 3418, 0, Direction.NORTH), // booth 3252, 3419
                    Teller(3253, 3418, 0, Direction.NORTH), // booth 3253, 3419
                    Teller(3254, 3418, 0, Direction.NORTH), // booth 3254, 3419
                    Teller(3255, 3418, 0, Direction.NORTH), // booth 3255, 3419
                    Teller(3256, 3418, 0, Direction.NORTH), // booth 3256, 3419
                ),
        ),

        Bank(
            name = "Lumbridge Castle bank",
            npcKeys = GENERIC_BANKERS,
            // no teller tile behind (3208, 3221).
            tellers =
                listOf(
                    Teller(3209, 3222, 2, Direction.SOUTH), // booth 3209, 3221
                ),
        ),

        Bank(
            name = "Draynor bank",
            npcKeys = GENERIC_BANKERS,
            // wiki: 4 tellers.
            tellers =
                listOf(
                    Teller(3090, 3242, 0, Direction.EAST), // booth 3091, 3242
                    Teller(3090, 3243, 0, Direction.EAST), // booth 3091, 3243
                    Teller(3090, 3245, 0, Direction.EAST), // booth 3091, 3245
                ),
        ),

        Bank(
            name = "Edgeville bank",
            npcKeys = GENERIC_BANKERS,
            // wiki: 4 tellers.
            tellers =
                listOf(
                    Teller(3096, 3489, 0, Direction.WEST), // booth 3095, 3489
                    Teller(3096, 3491, 0, Direction.WEST), // booth 3095, 3491
                    Teller(3096, 3492, 0, Direction.NORTH), // booth 3096, 3493
                    Teller(3098, 3492, 0, Direction.NORTH), // booth 3098, 3493
                ),
        ),

        // ---- Asgarnia ------------------------------------------------------

        Bank(
            name = "Falador west bank",
            npcKeys = GENERIC_BANKERS,
            // wiki: 5 tellers; no teller tile behind (2946, 3367).
            tellers =
                listOf(
                    Teller(2945, 3368, 0, Direction.SOUTH), // booth 2945, 3367
                    Teller(2947, 3368, 0, Direction.SOUTH), // booth 2947, 3367
                    Teller(2948, 3368, 0, Direction.SOUTH), // booth 2948, 3367
                    Teller(2949, 3368, 0, Direction.SOUTH), // booth 2949, 3367
                ),
        ),

        Bank(
            name = "Falador east bank",
            npcKeys = GENERIC_BANKERS,
            // wiki: 5 tellers.
            tellers =
                listOf(
                    Teller(3010, 3353, 0, Direction.NORTH), // booth 3010, 3354
                    Teller(3011, 3353, 0, Direction.NORTH), // booth 3011, 3354
                    Teller(3012, 3353, 0, Direction.NORTH), // booth 3012, 3354
                    Teller(3013, 3353, 0, Direction.NORTH), // booth 3013, 3354
                    Teller(3014, 3353, 0, Direction.NORTH), // booth 3014, 3354
                    Teller(3015, 3353, 0, Direction.NORTH), // booth 3015, 3354
                ),
        ),

        // ---- Kharidian Desert ----------------------------------------------

        Bank(
            name = "Al Kharid bank",
            npcKeys =
                listOf(
                    "npc.banker_3090",
                    "npc.banker_3091",
                ),
            // wiki: 5 tellers; no teller tile behind (3268, 3166).
            tellers =
                listOf(
                    Teller(3267, 3164, 0, Direction.EAST), // booth 3268, 3164
                    Teller(3267, 3167, 0, Direction.EAST), // booth 3268, 3167
                    Teller(3267, 3168, 0, Direction.EAST), // booth 3268, 3168
                    Teller(3267, 3169, 0, Direction.EAST), // booth 3268, 3169
                ),
        ),

        Bank(
            name = "Nardah bank",
            npcKeys = listOf("npc.nardah_banker"),
            // wiki: 3 tellers.
            tellers =
                listOf(
                    Teller(3425, 2889, 0, Direction.EAST), // booth 3426, 2889
                    Teller(3425, 2891, 0, Direction.EAST), // booth 3426, 2891
                    Teller(3425, 2893, 0, Direction.EAST), // booth 3426, 2893
                    Teller(3425, 2894, 0, Direction.EAST), // booth 3426, 2894
                ),
        ),

        // ---- Kandarin ------------------------------------------------------

        Bank(
            name = "Catherby bank",
            npcKeys = GENERIC_BANKERS,
            // wiki: 4 tellers.
            tellers =
                listOf(
                    Teller(2807, 3441, 0, Direction.NORTH), // booth 2807, 3442
                    Teller(2809, 3441, 0, Direction.NORTH), // booth 2809, 3442
                    Teller(2810, 3441, 0, Direction.NORTH), // booth 2810, 3442
                    Teller(2811, 3441, 0, Direction.NORTH), // booth 2811, 3442
                ),
        ),

        Bank(
            name = "Seers' Village bank",
            npcKeys = GENERIC_BANKERS,
            // wiki: 8 tellers; no teller tile behind (2721, 3494).
            tellers =
                listOf(
                    Teller(2722, 3495, 0, Direction.SOUTH), // booth 2722, 3494
                    Teller(2724, 3495, 0, Direction.SOUTH), // booth 2724, 3494
                    Teller(2727, 3495, 0, Direction.SOUTH), // booth 2727, 3494
                    Teller(2728, 3495, 0, Direction.SOUTH), // booth 2728, 3494
                    Teller(2729, 3495, 0, Direction.SOUTH), // booth 2729, 3494
                ),
        ),

        Bank(
            name = "Ardougne north bank",
            npcKeys = GENERIC_BANKERS,
            // wiki: 3 tellers.
            tellers =
                listOf(
                    Teller(2615, 3332, 0, Direction.SOUTH), // booth 2615, 3331
                    Teller(2618, 3332, 0, Direction.SOUTH), // booth 2618, 3331
                    Teller(2619, 3332, 0, Direction.SOUTH), // booth 2619, 3331
                ),
        ),

        Bank(
            name = "Ardougne south bank",
            npcKeys = GENERIC_BANKERS,
            // wiki: 3 tellers.
            tellers =
                listOf(
                    Teller(2655, 3280, 0, Direction.EAST), // booth 2656, 3280
                    Teller(2655, 3283, 0, Direction.EAST), // booth 2656, 3283
                    Teller(2655, 3286, 0, Direction.EAST), // booth 2656, 3286
                ),
        ),

        Bank(
            name = "Yanille bank",
            npcKeys = GENERIC_BANKERS,
            // wiki: 3 tellers.
            tellers =
                listOf(
                    Teller(2613, 3091, 0, Direction.EAST), // booth 2614, 3091
                    Teller(2613, 3092, 0, Direction.EAST), // booth 2614, 3092
                    Teller(2613, 3094, 0, Direction.EAST), // booth 2614, 3094
                ),
        ),

        Bank(
            name = "Tree Gnome Stronghold south bank",
            npcKeys = listOf("npc.gnome_banker"),
            tellers =
                listOf(
                    Teller(2443, 3424, 1, Direction.EAST), // booth 2444, 3424
                    Teller(2443, 3425, 1, Direction.EAST), // booth 2444, 3425
                    Teller(2446, 3424, 1, Direction.EAST), // booth 2447, 3424
                    Teller(2446, 3427, 1, Direction.EAST), // booth 2447, 3427
                ),
        ),

        Bank(
            name = "Tree Gnome Stronghold north bank",
            npcKeys = listOf("npc.gnome_banker"),
            tellers =
                listOf(
                    Teller(2442, 3487, 1, Direction.WEST), // booth 2441, 3487
                    Teller(2442, 3488, 1, Direction.WEST), // booth 2441, 3488
                    Teller(2442, 3489, 1, Direction.WEST), // booth 2441, 3489
                    Teller(2448, 3482, 1, Direction.SOUTH), // booth 2448, 3481
                    Teller(2449, 3482, 1, Direction.SOUTH), // booth 2449, 3481
                    Teller(2450, 3482, 1, Direction.SOUTH), // booth 2450, 3481
                ),
        ),

        // ---- Morytania -----------------------------------------------------

        Bank(
            name = "Canifis bank",
            npcKeys = listOf("npc.banker_2633"),
            // wiki: 2 tellers; no teller tile behind (3513, 3480).
            tellers =
                listOf(
                    Teller(3512, 3478, 1, Direction.EAST), // booth 3513, 3478
                    Teller(3512, 3479, 1, Direction.EAST), // booth 3513, 3479
                    Teller(3512, 3481, 1, Direction.EAST), // booth 3513, 3481
                    Teller(3512, 3482, 1, Direction.EAST), // booth 3513, 3482
                    Teller(3512, 3483, 1, Direction.EAST), // booth 3513, 3483
                ),
        ),

        Bank(
            name = "Port Phasmatys bank",
            npcKeys = listOf("npc.ghost_banker"),
            tellers =
                listOf(
                    Teller(3687, 3464, 0, Direction.NORTH), // booth 3687, 3465
                    Teller(3688, 3464, 0, Direction.NORTH), // booth 3688, 3465
                    Teller(3690, 3464, 0, Direction.NORTH), // booth 3690, 3465
                    Teller(3691, 3464, 0, Direction.NORTH), // booth 3691, 3465
                ),
        ),

        Bank(
            name = "Darkmeyer bank",
            npcKeys =
                listOf(
                    "npc.banker_9718",
                    "npc.banker_9719",
                ),
            tellers =
                listOf(
                    Teller(3603, 3367, 0, Direction.NORTH), // booth 3603, 3368
                    Teller(3604, 3367, 0, Direction.NORTH), // booth 3604, 3368
                    Teller(3605, 3367, 0, Direction.NORTH), // booth 3605, 3368
                    Teller(3607, 3367, 0, Direction.NORTH), // booth 3607, 3368
                ),
        ),

        // ---- Fremennik -----------------------------------------------------

        Bank(
            name = "Lunar Isle bank",
            npcKeys = GENERIC_BANKERS,
            // wiki: 3 tellers; no teller tile behind (2097, 3920).
            tellers =
                listOf(
                    Teller(2098, 3919, 0, Direction.NORTH), // booth 2098, 3920
                    Teller(2099, 3919, 0, Direction.NORTH), // booth 2099, 3920
                ),
        ),

        Bank(
            name = "Keldagrim bank",
            npcKeys =
                listOf(
                    "npc.banker_2368",
                    "npc.banker_2369",
                ),
            // wiki: 2 tellers.
            tellers =
                listOf(
                    Teller(2836, 10205, 0, Direction.NORTH), // booth 2836, 10206
                    Teller(2838, 10205, 0, Direction.NORTH), // booth 2838, 10206
                ),
        ),

        // ---- Great Kourend -------------------------------------------------

        Bank(
            name = "Arceuus bank",
            npcKeys =
                listOf(
                    "npc.banker_7057",
                    "npc.banker_7058",
                    "npc.banker_7059",
                    "npc.banker_7060",
                ),
            tellers =
                listOf(
                    Teller(1622, 3740, 0, Direction.EAST), // booth 1623, 3740
                    Teller(1622, 3743, 0, Direction.EAST), // booth 1623, 3743
                    Teller(1622, 3746, 0, Direction.EAST), // booth 1623, 3746
                    Teller(1622, 3749, 0, Direction.EAST), // booth 1623, 3749
                    Teller(1637, 3740, 0, Direction.WEST), // booth 1636, 3740
                    Teller(1637, 3743, 0, Direction.WEST), // booth 1636, 3743
                    Teller(1637, 3746, 0, Direction.WEST), // booth 1636, 3746
                    Teller(1637, 3749, 0, Direction.WEST), // booth 1636, 3749
                ),
        ),

        Bank(
            name = "Hosidius bank",
            npcKeys =
                listOf(
                    "npc.banker_6940",
                    "npc.banker_6942",
                ),
            tellers =
                listOf(
                    Teller(1748, 3598, 0, Direction.WEST), // booth 1747, 3598
                    Teller(1748, 3599, 0, Direction.WEST), // booth 1747, 3599
                    Teller(1748, 3600, 0, Direction.WEST), // booth 1747, 3600
                ),
        ),

        Bank(
            name = "Kourend Castle bank",
            npcKeys = GENERIC_BANKERS,
            // no teller tile behind (1610, 3680), (1613, 3680).
            tellers =
                listOf(
                    Teller(1611, 3679, 2, Direction.NORTH), // booth 1611, 3680
                    Teller(1612, 3679, 2, Direction.NORTH), // booth 1612, 3680
                ),
        ),

        Bank(
            name = "Lovakengj bank",
            npcKeys =
                listOf(
                    "npc.banker_7077",
                    "npc.banker_7078",
                    "npc.banker_7079",
                    "npc.banker_7080",
                    "npc.banker_7081",
                    "npc.banker_7082",
                ),
            // no teller tile behind (1530, 3737).
            tellers =
                listOf(
                    Teller(1520, 3740, 0, Direction.NORTH), // booth 1520, 3741
                    Teller(1522, 3738, 0, Direction.SOUTH), // booth 1522, 3737
                    Teller(1522, 3740, 0, Direction.NORTH), // booth 1522, 3741
                    Teller(1524, 3740, 0, Direction.NORTH), // booth 1524, 3741
                    Teller(1526, 3740, 0, Direction.NORTH), // booth 1526, 3741
                    Teller(1528, 3740, 0, Direction.NORTH), // booth 1528, 3741
                    Teller(1530, 3740, 0, Direction.NORTH), // booth 1530, 3741
                ),
        ),

        Bank(
            name = "Port Piscarilius bank",
            npcKeys =
                listOf(
                    "npc.banker_6969",
                    "npc.banker_6970",
                ),
            tellers =
                listOf(
                    Teller(1796, 3792, 0, Direction.SOUTH), // booth 1796, 3791
                    Teller(1800, 3792, 0, Direction.SOUTH), // booth 1800, 3791
                    Teller(1804, 3792, 0, Direction.SOUTH), // booth 1804, 3791
                    Teller(1808, 3792, 0, Direction.SOUTH), // booth 1808, 3791
                ),
        ),

        Bank(
            name = "Shayzien bank",
            npcKeys =
                listOf(
                    "npc.banker_6859",
                    "npc.banker_6860",
                    "npc.banker_6861",
                    "npc.banker_6862",
                    "npc.banker_6863",
                    "npc.banker_6864",
                ),
            tellers =
                listOf(
                    Teller(1486, 3591, 0, Direction.EAST), // booth 1487, 3591
                    Teller(1486, 3592, 0, Direction.EAST), // booth 1487, 3592
                    Teller(1486, 3593, 0, Direction.EAST), // booth 1487, 3593
                ),
        ),

        // ---- Tirannwn ------------------------------------------------------

        Bank(
            name = "Lletya bank",
            npcKeys =
                listOf(
                    "npc.banker_1479",
                    "npc.banker_1480",
                ),
            tellers =
                listOf(
                    Teller(2351, 3166, 0, Direction.SOUTH), // booth 2351, 3165
                    Teller(2354, 3166, 0, Direction.SOUTH), // booth 2354, 3165
                ),
        ),

        Bank(
            name = "Prifddinas north bank",
            npcKeys =
                listOf(
                    "npc.banker_9127",
                    "npc.banker_9128",
                    "npc.banker_9129",
                    "npc.banker_9130",
                    "npc.banker_9131",
                    "npc.banker_9132",
                ),
            // wiki: 4 tellers.
            tellers =
                listOf(
                    Teller(3255, 6105, 0, Direction.SOUTH), // booth 3255, 6104
                    Teller(3256, 6105, 0, Direction.SOUTH), // booth 3256, 6104
                    Teller(3256, 6109, 0, Direction.NORTH), // booth 3256, 6110
                    Teller(3258, 6105, 0, Direction.SOUTH), // booth 3258, 6104
                    Teller(3258, 6109, 0, Direction.NORTH), // booth 3258, 6110
                ),
        ),

        Bank(
            name = "Prifddinas south bank",
            npcKeys =
                listOf(
                    "npc.banker_9127",
                    "npc.banker_9128",
                    "npc.banker_9129",
                    "npc.banker_9130",
                    "npc.banker_9131",
                    "npc.banker_9132",
                ),
            // wiki: 3 tellers.
            tellers =
                listOf(
                    Teller(3295, 6058, 0, Direction.EAST), // booth 3296, 6058
                    Teller(3295, 6059, 0, Direction.EAST), // booth 3296, 6059
                    Teller(3295, 6060, 0, Direction.EAST), // booth 3296, 6060
                ),
        ),

        // ---- Varlamore -----------------------------------------------------

        Bank(
            name = "Civitas illa Fortis west bank",
            npcKeys =
                listOf(
                    "npc.banker_13212",
                    "npc.banker_13213",
                    "npc.banker_13214",
                    "npc.banker_13215",
                ),
            tellers =
                listOf(
                    Teller(1646, 3120, 0, Direction.SOUTH), // booth 1646, 3119
                    Teller(1648, 3120, 0, Direction.SOUTH), // booth 1648, 3119
                ),
        ),

        Bank(
            name = "Civitas illa Fortis east bank",
            npcKeys =
                listOf(
                    "npc.banker_13216",
                    "npc.banker_13217",
                    "npc.banker_13218",
                    "npc.banker_13219",
                ),
            tellers =
                listOf(
                    Teller(1778, 3092, 0, Direction.NORTH), // booth 1778, 3093
                    Teller(1780, 3092, 0, Direction.NORTH), // booth 1780, 3093
                ),
        ),

        // ---- Dorgesh-Kaan --------------------------------------------------

        Bank(
            name = "Dorgesh-Kaan bank",
            npcKeys =
                listOf(
                    "npc.banker_2292",
                    "npc.banker_2293",
                ),
            // no teller tile behind (2700, 5348), (2700, 5351).
            tellers =
                listOf(
                    Teller(2701, 5347, 0, Direction.WEST), // booth 2700, 5347
                    Teller(2701, 5349, 0, Direction.WEST), // booth 2700, 5349
                    Teller(2701, 5350, 0, Direction.WEST), // booth 2700, 5350
                    Teller(2701, 5352, 0, Direction.WEST), // booth 2700, 5352
                ),
        ),

        // ---- Mos Le'Harmless -----------------------------------------------

        Bank(
            name = "Mos Le'Harmless bank",
            npcKeys =
                listOf(
                    "npc.banker_4054",
                    "npc.banker_4055",
                ),
            tellers =
                listOf(
                    Teller(3680, 2981, 0, Direction.EAST), // booth 3681, 2981
                    Teller(3680, 2982, 0, Direction.EAST), // booth 3681, 2982
                    Teller(3680, 2983, 0, Direction.EAST), // booth 3681, 2983
                ),
        ),
    )

    /** Every banker id these banks spawn, for the plugin to wire options and dialogue onto. */
    val NPC_KEYS: List<String> = ALL.flatMap { it.npcKeys }.distinct()
}
