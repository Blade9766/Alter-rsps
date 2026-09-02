package org.alter.plugins.content.mechanics.ambience

import org.alter.api.cfg.Sound
import org.alter.game.model.Tile

/**
 * Declarative table of looping background sound sources, played by
 * [AmbientSoundPlugin].
 *
 * Two things worth knowing before adding entries here:
 *
 * 1. **There is no such thing as a looping [org.alter.game.model.entity.AreaSound].**
 *    The engine hardcodes the protocol's repeat field to 1
 *    (`org.alter.game.model.region.update.SoundAreaUpdate`), so every area sound is a
 *    one-shot. "Ambience" is therefore simulated by re-spawning the sound on a fixed
 *    [intervalTicks] cadence - which means [intervalTicks] should roughly match the
 *    real length of the sound clip, or it will either overlap itself or leave gaps.
 *    The values below are deliberately on the sparse side (a gap sounds far less
 *    broken than a stutter).
 *
 * 2. **[radius] can never exceed 15** - `AreaSound`'s own constructor `check`s this and
 *    throws otherwise, because the protocol only allocates 4 bits for it.
 *
 * The sound ids come from [Sound], this codebase's bundled named-id table. Unlike the
 * combat and woodcutting ids used elsewhere, these particular ones could not be
 * cross-checked against a second source (RuneLite's `SoundEffectID` carries no ambient
 * entries at all), so they rest on that table's naming alone - if one turns out to be
 * the wrong clip, it's the id here that's wrong, not the plumbing.
 *
 * Placements are **read out of the cache's own map data**, not remembered OSRS tiles:
 * every scenery coordinate below came from decoding each region's `l{x}_{z}` location
 * file (`loadLocations`, the same call `DefinitionSet.createRegion` uses at runtime)
 * and matching the object's name. So a fountain source sits exactly where this cache
 * actually puts a Fountain. Worth knowing if you regenerate these: this cache's map
 * files are **not** xtea-encrypted - they decode with empty keys, and passing the
 * stale keys in `data/xteas.json.backup` makes every read fail silently.
 *
 * The market-ambience entries are the exception - "a market" is not an object, so
 * those stay centred on the shopkeeper spawns this repo places.
 */
data class AmbientSound(
    val name: String,
    val soundId: Int,
    val tile: Tile,
    /** Tiles from [tile] the sound carries. Hard protocol maximum of 15. */
    val radius: Int = MAX_RADIUS,
    /** Roughly 0-100, matching the scale the NPC combat DSL uses (its default is 50). */
    val volume: Int = 30,
    /** Ticks between re-spawns. 1 tick = 0.6s, so 100 ticks = one minute. */
    val intervalTicks: Int,
) {
    init {
        require(radius in 1..MAX_RADIUS) { "Ambient sound radius must be 1..$MAX_RADIUS (protocol limit): $name" }
        require(intervalTicks >= 1) { "Ambient sound interval must be at least 1 tick: $name" }
        require(volume >= 0) { "Ambient sound volume cannot be negative: $name" }
    }

    companion object {
        /** [org.alter.game.model.entity.AreaSound] throws above this. */
        const val MAX_RADIUS = 15
    }
}

object AmbientSounds {
    val sources =
        listOf(
            // Varrock square. Centred between this repo's four Varrock shopkeeper
            // spawns (Zaff 3203,3434 / Thessalia 3206,3416 / Lowe 3235,3424 /
            // Horvik 3230,3437, in areas/varrock/npcs/stores), so the 15-tile radius
            // covers the market end of the square rather than an arbitrary point.
            AmbientSound(
                name = "Varrock square (market)",
                soundId = Sound.MARKET_AMBIENCE_1,
                tile = Tile(3213, 3425),
                intervalTicks = 55,
            ),
            // Horvik is the armourer, so his shop is where Varrock's anvils are. The
            // map has two Anvils at 3228,3434 and 3228,3436 - centred between them
            // here, correcting an earlier estimate of 3230,3437. Shorter radius than
            // the market hum since a smithing clang shouldn't carry the whole square.
            AmbientSound(
                name = "Varrock anvils (Horvik's)",
                soundId = Sound.ANVIL,
                tile = Tile(3228, 3435),
                radius = 8,
                volume = 25,
                intervalTicks = 17,
            ),
            // Al Kharid's northern market, centred on the gem trader (3287,3212) and
            // silk trader (3298,3202) spawns in areas/alkharid/npcs/stores.
            AmbientSound(
                name = "Al Kharid market",
                soundId = Sound.MARKET_AMBIENCE_1,
                tile = Tile(3293, 3207),
                intervalTicks = 55,
            ),
            // Draynor Village market, centred on this repo's two Draynor spawns
            // (3085,3253 and 3078,3251 in areas/draynor).
            AmbientSound(
                name = "Draynor Village market",
                soundId = Sound.MARKET_AMBIENCE_1,
                tile = Tile(3082, 3252),
                volume = 25,
                intervalTicks = 65,
            ),

            // --- Fountains -------------------------------------------------------
            // Varrock Square: the map places four Fountain locs (id 7143) at
            // 3210/3214 x 3426/3430, i.e. a 2x2 centred on 3212,3428. One source in
            // the middle covers the lot, and sits right beside the market hum above.
            AmbientSound(
                name = "Varrock Square fountain",
                soundId = Sound.FOUNTAIN_LOOP_1,
                tile = Tile(3212, 3428),
                radius = 10,
                volume = 22,
                intervalTicks = 30,
            ),
            // Lumbridge castle grounds has two Fountains (id 879) 16 tiles apart, so
            // they get one source each rather than a single stretched one.
            AmbientSound(
                name = "Lumbridge Castle fountain (south)",
                soundId = Sound.FOUNTAIN_LOOP_1,
                tile = Tile(3221, 3210),
                radius = 8,
                volume = 20,
                intervalTicks = 30,
            ),
            AmbientSound(
                name = "Lumbridge Castle fountain (north)",
                soundId = Sound.FOUNTAIN_LOOP_1,
                tile = Tile(3221, 3226),
                radius = 8,
                volume = 20,
                intervalTicks = 30,
            ),
            // Draynor Manor's fountain (id 153) - falls inside the Draynor Manor
            // music zone, so it layers under "Spooky".
            AmbientSound(
                name = "Draynor Manor fountain",
                soundId = Sound.FOUNTAIN_LOOP_1,
                tile = Tile(3087, 3334),
                radius = 8,
                volume = 20,
                intervalTicks = 30,
            ),

            // --- Furnaces --------------------------------------------------------
            // The Al Kharid furnace (id 24009), the one everybody actually smelts at.
            AmbientSound(
                name = "Al Kharid furnace",
                soundId = Sound.FURNACE,
                tile = Tile(3272, 3185),
                radius = 8,
                volume = 28,
                intervalTicks = 25,
            ),
            AmbientSound(
                name = "Lumbridge furnace",
                soundId = Sound.FURNACE,
                tile = Tile(3226, 3256),
                radius = 8,
                volume = 28,
                intervalTicks = 25,
            ),

            // --- More anvils -----------------------------------------------------
            // West Varrock's three Anvils (id 2097) run 3188,3421 -> 3188,3426;
            // centred on the middle one.
            AmbientSound(
                name = "Varrock anvils (west)",
                soundId = Sound.ANVIL,
                tile = Tile(3188, 3424),
                radius = 8,
                volume = 25,
                intervalTicks = 19,
            ),

            // --- Falador ---------------------------------------------------------
            // Falador's only Furnace (id 24009) per the map data.
            AmbientSound(
                name = "Falador furnace",
                soundId = Sound.FURNACE,
                tile = Tile(2976, 3368),
                radius = 8,
                volume = 28,
                intervalTicks = 25,
            ),
            // The one Fountain (id 24102) inside Falador's bounds, at the south-east
            // corner of Falador Park. Note the scan found *no* Anvil anywhere in
            // Falador (2934-3070, 3320-3400), so there's no smithing source here to
            // match Varrock's - that's the map, not an omission.
            AmbientSound(
                name = "Falador Park fountain",
                soundId = Sound.FOUNTAIN_LOOP_1,
                tile = Tile(3038, 3353),
                radius = 10,
                volume = 22,
                intervalTicks = 30,
            ),

            // --- Edgeville -------------------------------------------------------
            // The Edgeville furnace (id 16469) - a different object id to the 24009
            // used elsewhere, but the same Furnace name and the same sound fits.
            AmbientSound(
                name = "Edgeville furnace",
                soundId = Sound.FURNACE,
                tile = Tile(3110, 3499),
                radius = 8,
                volume = 28,
                intervalTicks = 25,
            ),
            // Four Fountains (id 879) at 3048/3054 x 3486/3494 form a 2x2 courtyard
            // centred here - the monastery west of Edgeville. Members' content, but
            // it's the only water feature in the area and sits right on Edgeville's
            // doorstep.
            AmbientSound(
                name = "Edgeville Monastery fountain",
                soundId = Sound.FOUNTAIN_LOOP_1,
                tile = Tile(3051, 3490),
                radius = 8,
                volume = 20,
                intervalTicks = 30,
            ),

            // --- Barbarian Village -----------------------------------------------
            // The map has no Furnace, Anvil or Fountain anywhere in Barbarian Village
            // (only barrels, a spinning wheel at 3081,3430 and a potter's wheel at
            // 3087,3409) - none of which make sense as constant background noise,
            // since they only make a sound when someone uses them. What actually
            // defines the place is the River Lum running down its east side.
            //
            // Anchored between this repo's own two Barbarian Village rod fishing
            // spots (3110,3434 and 3104,3424 in FishingPlugin.ROD_LURE_BAIT_SPOTS),
            // which sit on the riverbank and came from real in-game click
            // coordinates - so the water is genuinely where this points.
            AmbientSound(
                name = "Barbarian Village (River Lum)",
                soundId = Sound.RIVER_LOOP_1,
                tile = Tile(3107, 3429),
                radius = 10,
                volume = 22,
                intervalTicks = 28,
            ),

            // --- Port Sarim ------------------------------------------------------
            // A port's ambience is the sea, and there is no "sea" object to hang it
            // on - so this is anchored to the waterfront clutter the map does place:
            // the Fishing net cluster at 3011-3012,3222-3229 and the barrels down at
            // 3010-3011,3210. Centred between the two so one 14-tile source covers
            // the whole western dock frontage rather than doubling up two sources
            // that would overlap and stack the same clip on itself.
            AmbientSound(
                name = "Port Sarim waterfront",
                soundId = Sound.SEASHORE_1,
                tile = Tile(3012, 3222),
                radius = 14,
                volume = 24,
                intervalTicks = 30,
            ),

            // --- Draynor Manor ---------------------------------------------------
            // The manor already has its Fountain source above, out at the front gate
            // (3087,3334). This covers the building itself: centred on the middle of
            // the 15 Candles locs the map scatters through the ground floor
            // (3097-3105 x 3354-3373), which is as good a centre-of-building marker
            // as the map offers - there is no "manor" object to anchor to.
            //
            // The sound is a judgement call rather than a lookup: MORYTANIA_WIND_LOOP_1
            // is Morytania's haunted-wind ambience, borrowed here because Draynor
            // Manor is the F2P haunted house and nothing in the sound table is named
            // for it specifically. Swap it for BLUSTERY_WIND_LOOP_1 if it reads as
            // too much.
            AmbientSound(
                name = "Draynor Manor (haunted wind)",
                soundId = Sound.MORYTANIA_WIND_LOOP_1,
                tile = Tile(3101, 3363),
                radius = 15,
                volume = 20,
                intervalTicks = 34,
            ),
            // The lone Gravestone (id 12125) behind the manor. Far enough north that
            // its 8-tile radius doesn't overlap the manor source above (which reaches
            // z 3378), so the two never stack.
            AmbientSound(
                name = "Draynor Manor graveyard",
                soundId = Sound.MORYTANIA_WIND_LOOP_1,
                tile = Tile(3106, 3384),
                radius = 8,
                volume = 16,
                intervalTicks = 40,
            ),

            // --- Rimmington ------------------------------------------------------
            // Hetty the witch's Cauldron (id 2024) is the one genuinely characterful
            // object the village has. BUBBLING_LOOP_1 rather than the CAULDRON_SHAKE
            // ids, which read as one-shot "someone stirred it" sounds rather than a
            // loop. Small radius - it's a one-room cottage.
            AmbientSound(
                name = "Rimmington (Hetty's cauldron)",
                soundId = Sound.BUBBLING_LOOP_1,
                tile = Tile(2967, 3205),
                radius = 6,
                volume = 22,
                intervalTicks = 22,
            ),
            // Village-wide wind, anchored on Rimmington's Well (id 884, 2956,3212) as
            // the centre-of-village landmark. Like the manor wind above this is a
            // thematic pick, not a sourced one - Rimmington has no furnace, anvil,
            // fountain or market for the map to point at.
            AmbientSound(
                name = "Rimmington village",
                soundId = Sound.BLUSTERY_WIND_LOOP_1,
                tile = Tile(2956, 3212),
                radius = 15,
                volume = 15,
                intervalTicks = 38,
            ),

            // --- Karamja (Musa Point) --------------------------------------------
            // The map puts 33 Banana trees (id 2073) across x 2906-2930, z 3155-3170,
            // centroid 2918,3161 - the Musa Point plantation, and the single clearest
            // "this is jungle" marker on the island. Pulled six tiles south of that
            // centroid so one 15-tile source reaches the village Well at 2916,3142
            // as well as the plantation, instead of needing a second overlapping one.
            AmbientSound(
                name = "Karamja (Musa Point jungle)",
                soundId = Sound.JUNGLE_AMBIENCE_LOOP,
                tile = Tile(2918, 3155),
                radius = 15,
                volume = 24,
                intervalTicks = 32,
            ),
            // Dockside. Weaker grounding than the rest of this table: the only map
            // objects out this way are two Crates (2935,3156 and 2943,3151) on the
            // approach to the pier, so this is placed near them by eye rather than on
            // a named dock object - Musa Point's pier has none. Positioned east of the
            // jungle source's reach (which stops at x 2933) so the two don't stack.
            AmbientSound(
                name = "Karamja (Musa Point dock)",
                soundId = Sound.SEASHORE_1,
                tile = Tile(2948, 3150),
                radius = 12,
                volume = 22,
                intervalTicks = 30,
            ),

            // --- Wizards' Tower --------------------------------------------------
            // Centred on the centroid of the tower's seven ground-floor Bookcases
            // (3104-3114 x 3155-3160) - the tower has no single object marking its
            // middle. RUNE_TEMPLE_AMBIENCE is the closest thing the sound table has
            // to a "magical building hum"; PORTAL_LOOP_1 (3139) is the alternative if
            // this reads as too temple-like for a wizards' tower.
            AmbientSound(
                name = "Wizards' Tower",
                soundId = Sound.RUNE_TEMPLE_AMBIENCE,
                tile = Tile(3110, 3157),
                radius = 10,
                volume = 20,
                intervalTicks = 32,
            ),
            // A Fountain (id 879) sits just north-east of the tower. Its radius does
            // overlap the tower hum above, but that's layering two different clips
            // rather than stacking one on itself, which is the thing to avoid.
            AmbientSound(
                name = "Wizards' Tower fountain",
                soundId = Sound.FOUNTAIN_LOOP_1,
                tile = Tile(3116, 3167),
                radius = 8,
                volume = 20,
                intervalTicks = 30,
            ),
        )
}
