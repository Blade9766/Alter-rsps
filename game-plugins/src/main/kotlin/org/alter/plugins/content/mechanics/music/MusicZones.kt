package org.alter.plugins.content.mechanics.music

import org.alter.game.model.Tile

/**
 * Hand-curated shuffle/play zones, for areas where real OSRS plays specific tracks in
 * specific sub-areas (or shuffles between several) rather than always playing one
 * fixed track everywhere - the generic `music_by_region.yaml` data [MusicService]
 * loads is a flat one-track-per-region mapping and can't represent either of those,
 * and was confirmed wrong for Varrock specifically (it mapped Varrock's main region
 * to "Xenophobe", not any of the real Varrock tracks) - so zones defined here take
 * priority over that file wherever they overlap.
 *
 * A zone matches on whole map regions (`regionIds`, cheap and good enough when the
 * real area is close to rectangular and region-aligned, like Varrock's) and/or exact
 * tile rectangles (`areas`, for when the real area's boundary cuts through a region
 * other content already occupies, or needs to be split more finely than a whole
 * region - see the Lumbridge zones' comment). Region ids are computed the same way
 * [org.alter.game.model.Tile.regionId] does: `(x shr 6 shl 8) or (z shr 6)`.
 */
data class TileArea(val x1: Int, val z1: Int, val x2: Int, val z2: Int) {
    fun contains(
        x: Int,
        z: Int,
    ): Boolean = x in x1..x2 && z in z1..z2
}

data class MusicZone(
    val name: String,
    /** The pool this zone plays from - picked at random when size > 1 (a shuffle). */
    val trackIds: List<Int>,
    /**
     * Extra tracks to mark unlocked (but not play) on entry - for zones that are part
     * of a wider "genre" that unlocks together even though this specific spot only
     * plays one of them.
     */
    val alsoUnlock: List<Int> = emptyList(),
    val regionIds: Set<Int> = emptySet(),
    val areas: List<TileArea> = emptyList(),
) {
    fun contains(tile: Tile): Boolean = tile.regionId in regionIds || areas.any { it.contains(tile.x, tile.z) }
}

object MusicZones {
    // The 6 tracks that make up the "Lumbridge" music genre - shared by every
    // Lumbridge sub-zone below so arriving anywhere in Lumbridge unlocks all of them
    // (matching "unlocked when the player first arrives in Lumbridge", true of every
    // one of these 6 per its own OSRS Wiki infobox), even though each specific spot
    // only plays its own one.
    private val LUMBRIDGE_GENRE =
        listOf(
            2, // Autumn Voyage
            64, // Book of Spells
            327, // Dream
            163, // Flute Salad
            76, // Harmony
            145, // Yesteryear
        )

    // Draynor Village's two tracks - both carry the identical unlock hint ("This
    // track unlocks in Draynor Village."), so entering either half unlocks both,
    // the same way the Lumbridge genre above works. Draynor Manor's "Spooky" is
    // deliberately NOT in here: it has its own distinct hint and unlocks separately.
    private val DRAYNOR_VILLAGE_GENRE =
        listOf(
            3, // Unknown Land
            151, // Start
        )

    // Al Kharid's two tracks - both wiki infoboxes give the same hint ("This track
    // unlocks in Al Kharid."), so they unlock as a pair.
    private val AL_KHARID_GENRE =
        listOf(
            50, // Al Kharid
            123, // Arabian 2
        )

    // Falador's three "inside the city walls" tracks. All three carry the identical
    // infobox hint ("This track unlocks in Falador.") and unlockdetail ("Unlocked
    // inside Falador city walls."), so entering anywhere inside the walls unlocks all
    // three - same genre pattern as Lumbridge and Draynor above.
    private val FALADOR_INSIDE_GENRE =
        listOf(
            186, // Arrival
            72, // Fanfare
            15, // Workshop
        )

    // The three "outside the walls, south" tracks - all three share the hint "This
    // track unlocks south of Falador." Note Wander is deliberately NOT here: despite
    // sitting in this same southern band on classic mode, its hint is "This track
    // unlocks in Draynor Village.", so it unlocks on its own.
    private val FALADOR_SOUTH_GENRE =
        listOf(
            12, // Long Way Home
            107, // Miles Away
            127, // Nightfall
        )

    // The two "outside the walls, north" tracks - both share the hint "This track
    // unlocks north of Falador." and the same unlockdetail about Ice Mountain and the
    // Monastery.
    private val FALADOR_NORTH_GENRE =
        listOf(
            113, // Lightness
            54, // Scape Soft
        )

    val zones =
        listOf(
            // "Unlocked inside Varrock city walls, excluding the Grand Exchange" - per
            // the OSRS Wiki infobox for each of these 4 tracks, which all shuffle
            // together in that area. Region (49,53) and (50,53) cover the wiki's
            // given unlock-area polygons (x 3136-3264, z 3392-3455); the Grand
            // Exchange (3165,3490) sits in region (49,54), one band further north,
            // so it's naturally excluded rather than needing special-casing.
            MusicZone(
                name = "Varrock",
                regionIds = setOf(regionId(49, 53), regionId(50, 53)),
                trackIds =
                    listOf(
                        125, // Garden
                        177, // Adventure
                        157, // Medieval
                        175, // Spirit
                    ),
            ),

            // Lumbridge: modern OSRS shuffles all 6 tracks across the whole zone, but
            // *classic*-mode OSRS split it into 6 distinct geographic sub-areas, each
            // playing just one track - confirmed from each track's own wiki page
            // ("Classic mode"/"before 2021" Map polygon), which line up into a clean
            // 2x3 grid over the same overall x:3136-3264, z:3136-3328 area. Rebuilt
            // as that classic-style split per request, since a single citywide
            // shuffle pool meant only re-rolling when leaving Lumbridge entirely
            // (hence "only Flute Salad ever plays" once it happened to be picked).
            //
            // Grid (x split at 3200, z split at 3200 and 3264):
            //   North  (z 3264-3327): Flute Salad (west) | Autumn Voyage (east)
            //   Middle (z 3200-3263): Dream        (west) | Harmony       (east)
            //   South  (z 3136-3199): Book of Spells(west)| Yesteryear    (east)
            MusicZone(
                name = "Lumbridge (Mill Lane / Fred the Farmer's)",
                areas = listOf(TileArea(x1 = 3136, z1 = 3264, x2 = 3199, z2 = 3327)),
                trackIds = listOf(163), // Flute Salad
                alsoUnlock = LUMBRIDGE_GENRE,
            ),
            MusicZone(
                name = "Lumbridge (north)",
                areas = listOf(TileArea(x1 = 3200, z1 = 3264, x2 = 3264, z2 = 3327)),
                trackIds = listOf(2), // Autumn Voyage
                alsoUnlock = LUMBRIDGE_GENRE,
            ),
            MusicZone(
                name = "Lumbridge (Draynor path)",
                areas = listOf(TileArea(x1 = 3136, z1 = 3200, x2 = 3199, z2 = 3263)),
                trackIds = listOf(327), // Dream
                alsoUnlock = LUMBRIDGE_GENRE,
            ),
            MusicZone(
                name = "Lumbridge Castle",
                areas = listOf(TileArea(x1 = 3200, z1 = 3200, x2 = 3264, z2 = 3263)),
                trackIds = listOf(76), // Harmony
                alsoUnlock = LUMBRIDGE_GENRE,
            ),
            MusicZone(
                name = "Lumbridge Swamp (west)",
                areas = listOf(TileArea(x1 = 3136, z1 = 3136, x2 = 3199, z2 = 3199)),
                trackIds = listOf(64), // Book of Spells
                alsoUnlock = LUMBRIDGE_GENRE,
            ),
            MusicZone(
                name = "Lumbridge Swamp (east)",
                areas = listOf(TileArea(x1 = 3200, z1 = 3136, x2 = 3264, z2 = 3199)),
                trackIds = listOf(145), // Yesteryear
                alsoUnlock = LUMBRIDGE_GENRE,
            ),

            // Draynor Village: two tracks sharing the identical unlock hint ("This
            // track unlocks in Draynor Village.", per both tracks' own wiki infobox),
            // so arriving anywhere in Draynor unlocks the pair - but classic mode
            // plays each in its own half, same split-by-sub-area pattern as Lumbridge
            // above. Areas are the tracks' own "Classic mode" Map polygons taken from
            // the wiki's raw wikitext (the rendered page drops the coordinates):
            //   Unknown Land: 3065,3250|3065,3254|3068,3257|3068,3264|3136,3264|3136,3200|3065,3200
            //   Start:        3072,3264|3072,3328|3136,3328|3136,3264
            // Both simplified to their bounding rectangle - the only detail lost is
            // Unknown Land's small jagged north-west corner around x3065-3068.
            // Eastern edge pulled in to x=3135 so it can't collide with Lumbridge's
            // x1=3136 zones above rather than relying on list order to break the tie.
            MusicZone(
                name = "Draynor Village",
                areas = listOf(TileArea(x1 = 3065, z1 = 3200, x2 = 3135, z2 = 3263)),
                trackIds = listOf(3), // Unknown Land
                alsoUnlock = DRAYNOR_VILLAGE_GENRE,
            ),
            MusicZone(
                name = "Draynor Village (north)",
                areas = listOf(TileArea(x1 = 3072, z1 = 3264, x2 = 3135, z2 = 3327)),
                trackIds = listOf(151), // Start
                alsoUnlock = DRAYNOR_VILLAGE_GENRE,
            ),

            // Draynor Manor - its own separate unlock ("This track unlocks in Draynor
            // Manor."), not part of the village pair above. Area is Spooky's own
            // "Location before 2022, current Classic mode unlock area" polygon:
            // 3072,3328|3072,3392|3136,3392|3136,3328 - already a clean rectangle.
            MusicZone(
                name = "Draynor Manor",
                areas = listOf(TileArea(x1 = 3072, z1 = 3328, x2 = 3136, z2 = 3391)),
                trackIds = listOf(333), // Spooky
            ),

            // Al Kharid: like Draynor, two tracks share one unlock hint ("This track
            // unlocks in Al Kharid." on both wiki infoboxes) but sit in different
            // places, so they unlock together and play separately.
            //
            // Al Kharid's own classic polygon is 3264,3136|3264,3200|3392,3200|3392,3136.
            // Deliberately extended north from z=3200 to z=3230: the classic polygon
            // stops short of the real city's northern market, where this repo's own
            // Al Kharid content actually sits (gem trader 3287,3212 and silk trader
            // 3298,3202 in areas/alkharid/npcs/stores), which would otherwise fall
            // outside every zone and drop back to the generic per-region file. This
            // is the one boundary here not taken straight from the wiki.
            //
            // Western edge starts at x=3265 rather than the polygon's 3264, since
            // Lumbridge's zones above end on x2=3264 - same tie-break reasoning as
            // Draynor's eastern edge.
            MusicZone(
                name = "Al Kharid",
                areas = listOf(TileArea(x1 = 3265, z1 = 3136, x2 = 3392, z2 = 3230)),
                trackIds = listOf(50), // Al Kharid
                alsoUnlock = AL_KHARID_GENRE,
            ),
            // Arabian 2's classic polygon: 3264,3264|3264,3328|3328,3328|3328,3264 -
            // the band north of the city, which is why this repo's own music data
            // file describes it as unlocking "north of Al Kharid" even though the
            // wiki infobox hint just says "in Al Kharid".
            // Edgeville. Forever's own classic polygon is the exact block
            // 3072,3456|3072,3520|3136,3520|3136,3456 - another clean 64x64 rectangle, like
            // Falador's. Its hint is "This track unlocks in Edgeville." and its unlockdetail
            // "Unlocked in Edgeville and the Edgeville Dungeon.", so the two zones below
            // share the one track and unlock it either way.
            //
            // Worth knowing: **Oziach's hut (3070, 3517) falls just outside this block**,
            // by two tiles. That is not an error - his hut really does sit west of the town
            // proper, and the classic polygon starts at x=3072. It lands in the Ice Mountain
            // zone below instead. Forever's *modern* polygon does reach x=3065 and would
            // include him, but the rest of this table is classic-mode, so extending it here
            // would be inventing a boundary rather than sourcing one.
            MusicZone(
                name = "Edgeville",
                areas = listOf(TileArea(x1 = 3072, z1 = 3456, x2 = 3135, z2 = 3519)),
                trackIds = listOf(98), // Forever
            ),
            // The Edgeville Dungeon, underneath. This one uses Forever's *current* dungeon
            // polygon (3072,9792|3136,10048) rather than its "before 2021" one
            // (3072,9856|3136,10048): the current is a strict superset, nothing else claims
            // any of the 9xxx band, and the classic/modern split exists because surface
            // tracks were redistributed in 2021 - which never applied to the dungeon.
            MusicZone(
                name = "Edgeville Dungeon",
                areas = listOf(TileArea(x1 = 3072, z1 = 9792, x2 = 3135, z2 = 10047)),
                trackIds = listOf(98), // Forever
            ),

            // Ice Mountain and the Edgeville Monastery - Alone (cacheid 102).
            //
            // Its wiki hint reads "This track unlocks at Ice Mountain." and says nothing
            // about the monastery, but its classic polygon genuinely covers both: Abbot
            // Langley stands at (3052, 3490), well inside it. So the track that plays at the
            // Edgeville Monastery really is Alone, even though the hint never says so.
            //
            // The polygon is jagged and interlocks with Lightness's along a shared border
            // around x 2979-3008, so it is NOT reduced to a bounding box the way Draynor's
            // was - that would overlap the Lightness zone above and make `lookup()` resolve
            // by declaration order. Instead these two rectangles are chosen to sit wholly
            // inside Alone's polygon and wholly east of Lightness's x2=3007. The second one
            // stops at z=3503 because the polygon notches inward above that
            // (3008,3512|3017,3512|3023,3506|3024,3506|3024,3504|3032,3504). This
            // under-covers the jagged fringe rather than risking an overlap.
            MusicZone(
                name = "Ice Mountain / Edgeville Monastery",
                areas =
                    listOf(
                        TileArea(x1 = 3032, z1 = 3456, x2 = 3071, z2 = 3519),
                        TileArea(x1 = 3008, z1 = 3456, x2 = 3031, z2 = 3503),
                    ),
                trackIds = listOf(102), // Alone
            ),

            // The Body Altar, reached from the ruin south of the Edgeville Monastery.
            // Its map polygon is the altar's own interior coordinates (2496-2559, 4800-4863),
            // not overworld ones - the unlockdetail describes where the *entrance* is, which
            // is why this looks nowhere near Edgeville.
            MusicZone(
                name = "Body Altar",
                areas = listOf(TileArea(x1 = 2496, z1 = 4800, x2 = 2559, z2 = 4863)),
                trackIds = listOf(190), // Heart and Mind
            ),

            // East Ardougne. Each track's own "Location before 2026 and in Classic mode"
            // polygon is used, keeping this table classic-mode throughout. In the *current*
            // game the three instead share one big unified polygon (Map:Ardougne music) and
            // shuffle; the classic split gives each part of the city its own track, which is
            // how Lumbridge and Falador are handled here.
            //
            // **These three do NOT unlock as a genre**, unlike Falador's or Lumbridge's.
            // The wiki's current infobox gives all three the same hint ("This track unlocks
            // in East Ardougne."), which would suggest one shared unlock - but this cache's
            // own hints are three distinct strings: Knightly "in the East Ardougne Castle.",
            // Baroque "in Ardougne.", The Tower "north-west of East Ardougne.". Each matches
            // its classic polygon exactly, so the cache agrees with the classic split the
            // rest of this entry uses, and the tracks unlock area by area. The cache is what
            // the client actually shows the player, so it wins over the wiki's newer wording.
            //
            // Knightly and Baroque tile the southern half cleanly at x=2624, and The Tower
            // takes the band north of them.
            //
            // **Known gap, deliberate**: the three classic polygons leave x 2624-2687 above
            // z=3327 uncovered - the city's north-east corner, where three of the Ardougne
            // guards stand (2635,3339 / 2636,3340 / 2637,3339). Those tiles fall through to
            // the generic per-region file. The modern unified polygon does cover that corner,
            // but it does not say which of the three plays there, so extending one of these
            // northward would be inventing a boundary rather than sourcing one.
            MusicZone(
                name = "East Ardougne (centre)",
                areas = listOf(TileArea(x1 = 2558, z1 = 3264, x2 = 2623, z2 = 3327)),
                trackIds = listOf(191), // Knightly
            ),
            MusicZone(
                name = "East Ardougne (east)",
                areas = listOf(TileArea(x1 = 2624, z1 = 3264, x2 = 2687, z2 = 3327)),
                trackIds = listOf(99), // Baroque
            ),
            // The Tower's polygon is the block x 2560-2623 z 3328-3391 plus a two-tile strip
            // running down its west side (2558,3328|2558,3336|2560,3336) - reproduced rather
            // than rounded off, since squaring it up would have overlapped Knightly.
            MusicZone(
                name = "East Ardougne (north of the castle)",
                areas =
                    listOf(
                        TileArea(x1 = 2560, z1 = 3328, x2 = 2623, z2 = 3391),
                        TileArea(x1 = 2558, z1 = 3328, x2 = 2559, z2 = 3335),
                    ),
                trackIds = listOf(133), // The Tower
            ),
            // Both tracks also have published underground areas - the tunnels and sewers
            // beneath the city. Knightly's is a clean rectangle; The Tower's is the block
            // x 2496-2623 z 9728-9791 with a notch cut out of its south-west
            // (2528,9728|2528,9760|2560,9760|2560,9728), so it is expressed as the three
            // rectangles that tile the real shape rather than as a bounding box.
            MusicZone(
                name = "Ardougne underground (centre)",
                areas = listOf(TileArea(x1 = 2560, z1 = 9694, x2 = 2583, z2 = 9727)),
                trackIds = listOf(191), // Knightly
            ),
            MusicZone(
                name = "Ardougne underground (north)",
                areas =
                    listOf(
                        TileArea(x1 = 2496, z1 = 9728, x2 = 2527, z2 = 9791),
                        TileArea(x1 = 2528, z1 = 9760, x2 = 2623, z2 = 9791),
                        TileArea(x1 = 2560, z1 = 9728, x2 = 2623, z2 = 9759),
                    ),
                trackIds = listOf(133), // The Tower
            ),

            MusicZone(
                name = "Al Kharid (north)",
                areas = listOf(TileArea(x1 = 3265, z1 = 3264, x2 = 3328, z2 = 3327)),
                trackIds = listOf(123), // Arabian 2
                alsoUnlock = AL_KHARID_GENRE,
            ),

            // Falador. Every one of these nine tracks' classic-mode polygon is an exact
            // 64x64 block, and together they tile the area cleanly - the same
            // one-track-per-sub-area pattern as Lumbridge, but here it falls out of the
            // wiki data directly rather than needing to be reconstructed. All polygons
            // below are each track's own "Location before 2021 and on Classic mode" Map
            // template, taken from the raw wikitext.
            //
            // Laid out (x bands 2880-2943 / 2944-3007 / 3008-3071):
            //   z 3456-3519  ..            | Lightness      | ..            (far north)
            //   z 3392-3455  ..            | Scape Soft     | ..            (north)
            //   z 3328-3391  Arrival       | Fanfare        | Workshop      (inside walls)
            //   z 3264-3327  Miles Away    | Nightfall      | Wander        (outside, south)
            //   z 3200-3263  ..            | Long Way Home  | ..            (far south)
            //
            // The three inside-walls blocks are what the city itself sits on: this
            // repo's own Falador content lands on Fanfare (the shops, Rising Sun Inn,
            // White Knights' Castle) and Workshop (the Party Room, Falador Park), while
            // Wayne's Chains - which really does stand outside the south gate at
            // z=3313 - correctly falls through to Nightfall.
            MusicZone(
                name = "Falador (west, inside walls)",
                areas = listOf(TileArea(x1 = 2880, z1 = 3328, x2 = 2943, z2 = 3391)),
                trackIds = listOf(186), // Arrival
                alsoUnlock = FALADOR_INSIDE_GENRE,
            ),
            MusicZone(
                name = "Falador (central, inside walls)",
                areas = listOf(TileArea(x1 = 2944, z1 = 3328, x2 = 3007, z2 = 3391)),
                trackIds = listOf(72), // Fanfare
                alsoUnlock = FALADOR_INSIDE_GENRE,
            ),
            MusicZone(
                name = "Falador (east, inside walls)",
                areas = listOf(TileArea(x1 = 3008, z1 = 3328, x2 = 3071, z2 = 3391)),
                trackIds = listOf(15), // Workshop
                alsoUnlock = FALADOR_INSIDE_GENRE,
            ),
            MusicZone(
                name = "Falador (south-west, outside walls)",
                areas = listOf(TileArea(x1 = 2880, z1 = 3264, x2 = 2943, z2 = 3327)),
                trackIds = listOf(107), // Miles Away
                alsoUnlock = FALADOR_SOUTH_GENRE,
            ),
            MusicZone(
                name = "Falador (south, outside walls)",
                areas = listOf(TileArea(x1 = 2944, z1 = 3264, x2 = 3007, z2 = 3327)),
                trackIds = listOf(127), // Nightfall
                alsoUnlock = FALADOR_SOUTH_GENRE,
            ),
            MusicZone(
                name = "Falador (far south)",
                areas = listOf(TileArea(x1 = 2944, z1 = 3200, x2 = 3007, z2 = 3263)),
                trackIds = listOf(12), // Long Way Home
                alsoUnlock = FALADOR_SOUTH_GENRE,
            ),
            MusicZone(
                name = "Falador (north)",
                areas = listOf(TileArea(x1 = 2944, z1 = 3392, x2 = 3007, z2 = 3455)),
                trackIds = listOf(54), // Scape Soft
                alsoUnlock = FALADOR_NORTH_GENRE,
            ),
            // Lightness's classic polygon is the one Falador track that is NOT a clean
            // rectangle - it is a jagged shape tracing the paths around Ice Mountain and
            // the Monastery. Simplified to its bounding box (x 2944-3007, z 3456-3519),
            // the same treatment Draynor's Unknown Land got. That over-covers some
            // genuinely empty mountainside, which is harmless here since nothing else
            // claims it, but it is an approximation rather than the exact polygon.
            MusicZone(
                name = "North of Falador (west Ice Mountain approach)",
                areas = listOf(TileArea(x1 = 2944, z1 = 3456, x2 = 3007, z2 = 3519)),
                trackIds = listOf(113), // Lightness
                alsoUnlock = FALADOR_NORTH_GENRE,
            ),
            // Falador Farm. Wander sits in the Falador grid geographically but is not a
            // Falador-genre track: its hint reads "This track unlocks in Draynor
            // Village.", and its unlockdetail spells out the split - "Unlocked around
            // Draynor Village, or only at the Falador Farm when on classic mode." So it
            // unlocks alone here, and does not unlock (or get unlocked by) either the
            // Falador genres or the Draynor Village pair above. This is the only place
            // in the zone table that plays Wander.
            MusicZone(
                name = "Falador Farm",
                areas = listOf(TileArea(x1 = 3008, z1 = 3264, x2 = 3071, z2 = 3327)),
                trackIds = listOf(49), // Wander
            ),

            // Barbarian Village. A single track with its own hint ("This track unlocks
            // at Barbarian Village."), so no genre and no `alsoUnlock`.
            //
            // This zone exists to *fix* a real bug rather than to add a shuffle: the
            // flat `music_by_region.yaml` lists region 12341 eleven separate times, and
            // [MusicService] parses it with a plain `put`, so the last entry silently
            // wins. Barbarianism (141) is the *first* of those eleven and was being
            // overwritten - the region ended up resolving to 145, Yesteryear, which is
            // the Lumbridge theme. That file evidently lists every track whose unlock
            // area touches a region rather than the one track that plays there, which is
            // the same defect already found for Varrock.
            //
            // Area is the wiki's own polygon captioned "Location before 2022 and on
            // Classic mode" - x 3072-3135, z 3392-3455, a clean region-aligned 64x64
            // block, hence expressing it as region (48,53) = 12341 directly. The track's
            // other polygon (x 3072-3135, z 9792-9855) is captioned "Location before
            // 2021" and is deliberately not used: it is the superseded underground
            // location, and it would collide head-on with the Edgeville Dungeon zone.
            MusicZone(
                name = "Barbarian Village",
                regionIds = setOf(regionId(48, 53)),
                trackIds = listOf(141), // Barbarianism
            ),

            // North of East Ardougne. This is the corner the three Ardougne zones
            // knowingly leave uncovered, and the answer turned out not to be any of them:
            // it belongs to Wonderous, whose own classic-mode polygon
            // (2624,3328 -> 2688,3392, captioned "Unlock area before 2026 and in Classic
            // mode") is *exactly* this region, and whose unlockdetail reads "Unlocked near
            // the north of East Ardougne."
            //
            // A region audit showed this tile was already resolving to Wonderous - but by
            // accident, as the last of nine `music_by_region.yaml` entries for region
            // 10548. Naming it here makes the right answer intentional and testable
            // instead of dependent on that file's line order.
            //
            // No `alsoUnlock`: its hint ("This track unlocks south of Seers' Village.")
            // is its own and does not group with the East Ardougne tracks.
            MusicZone(
                name = "North of East Ardougne",
                regionIds = setOf(regionId(41, 52)),
                trackIds = listOf(81), // Wonderous
            ),
        )

    private fun regionId(
        regionX: Int,
        regionZ: Int,
    ): Int = (regionX shl 8) or regionZ

    fun lookup(tile: Tile): MusicZone? = zones.firstOrNull { it.contains(tile) }
}
