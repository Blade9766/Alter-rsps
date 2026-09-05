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

            // ---------------------------------------------------------------------------
            // Underground. Every dungeon in the game was silent until 2026-09-04 - the
            // coverage audit found 38 of 48 content areas with no ambience at all, and
            // *none* of the underground ones had any.
            //
            // Tiles are the densest spawn clusters of each dungeon's own content, taken
            // from the area packages rather than picked by eye, so a source sits where
            // players actually are. Radius is capped at 15 by the protocol, so a large
            // dungeon needs several rather than one loud one.
            //
            // The clips are the cache's generic cave family - a contiguous block at
            // 2039-2052 plus CAVE_WIND_LOOP - deliberately not the BRIMSTONE_DUNGEON_*
            // set, which is Mount Karuulm's own and would be wrong anywhere else.
            // ---------------------------------------------------------------------------
            // Taverley Dungeon. The largest silent area in the game - 137 spawn points and no sound at all. Four sources
            // on its four densest spawn clusters, deliberately different clips so the dungeon does
            // not sound like one repeating noise as you walk through it.
            AmbientSound(
                name = "Taverley Dungeon (entrance halls)",
                soundId = Sound.STALAGTITE_DRIP_LOOP1,
                tile = Tile(2910, 9689),
                radius = 12,
                volume = 18,
                intervalTicks = 40,
            ),
            AmbientSound(
                name = "Taverley Dungeon (deep south)",
                soundId = Sound.CAVE_RUMBLING_LOOP_1,
                tile = Tile(2914, 9825),
                radius = 12,
                volume = 16,
                intervalTicks = 55,
            ),
            AmbientSound(
                name = "Taverley Dungeon (west chambers)",
                soundId = Sound.CAVE_STALAGTITE_LOOP_1,
                tile = Tile(2889, 9821),
                radius = 12,
                volume = 18,
                intervalTicks = 45,
            ),
            AmbientSound(
                name = "Taverley Dungeon (east passage)",
                soundId = Sound.CAVE_WIND_LOOP,
                tile = Tile(2934, 9845),
                radius = 12,
                volume = 16,
                intervalTicks = 50,
            ),
            // Goblin Cave. Three clusters within about 40 tiles, so the clips differ to keep them distinguishable.
            AmbientSound(
                name = "Goblin Cave (main chamber)",
                soundId = Sound.STALAGTITE_DRIP_LOOP1,
                tile = Tile(2585, 9803),
                radius = 12,
                volume = 18,
                intervalTicks = 40,
            ),
            AmbientSound(
                name = "Goblin Cave (north tunnels)",
                soundId = Sound.CAVE_RUMBLING_LOOP_QUIETER,
                tile = Tile(2588, 9825),
                radius = 12,
                volume = 15,
                intervalTicks = 55,
            ),
            AmbientSound(
                name = "Goblin Cave (west end)",
                soundId = Sound.CAVE_BUBBLING_LOOP_1,
                tile = Tile(2567, 9843),
                radius = 10,
                volume = 16,
                intervalTicks = 35,
            ),
            // Yanille Agility Dungeon. Long and thin; sources sit on the two ends and the middle so there is no silent stretch.
            AmbientSound(
                name = "Yanille Agility Dungeon (east)",
                soundId = Sound.CAVE_RUMBLING_LOOP_1,
                tile = Tile(2608, 9485),
                radius = 12,
                volume = 16,
                intervalTicks = 55,
            ),
            AmbientSound(
                name = "Yanille Agility Dungeon (centre)",
                soundId = Sound.STALAGTITE_DRIP_LOOP1,
                tile = Tile(2582, 9499),
                radius = 12,
                volume = 18,
                intervalTicks = 40,
            ),
            AmbientSound(
                name = "Yanille Agility Dungeon (west)",
                soundId = Sound.CAVE_WIND_LOOP,
                tile = Tile(2540, 9454),
                radius = 12,
                volume = 16,
                intervalTicks = 50,
            ),
            // Observatory Dungeon. Small and spread out; two sources cover the populated halves.
            AmbientSound(
                name = "Observatory Dungeon (north)",
                soundId = Sound.STALAGTITE_DRIP_LOOP1,
                tile = Tile(2340, 9386),
                radius = 12,
                volume = 18,
                intervalTicks = 40,
            ),
            AmbientSound(
                name = "Observatory Dungeon (south)",
                soundId = Sound.CAVE_RUMBLING_LOOP_QUIETER,
                tile = Tile(2342, 9363),
                radius = 12,
                volume = 15,
                intervalTicks = 55,
            ),
            // Temple of Ikov. Its two occupied ends are 70 tiles apart, so one source each.
            AmbientSound(
                name = "Temple of Ikov (north)",
                soundId = Sound.CAVE_STALAGTITE_LOOP_1,
                tile = Tile(2652, 9889),
                radius = 12,
                volume = 18,
                intervalTicks = 45,
            ),
            AmbientSound(
                name = "Temple of Ikov (south)",
                soundId = Sound.STALAGTITE_DRIP_LOOP1,
                tile = Tile(2667, 9823),
                radius = 12,
                volume = 18,
                intervalTicks = 40,
            ),
            // Ogre Enclave. Two clusters either side of the enclave.
            AmbientSound(
                name = "Ogre Enclave (west)",
                soundId = Sound.CAVE_RUMBLING_LOOP_1,
                tile = Tile(2584, 9431),
                radius = 12,
                volume = 16,
                intervalTicks = 55,
            ),
            AmbientSound(
                name = "Ogre Enclave (east)",
                soundId = Sound.CAVE_BUBBLING_LOOP_1,
                tile = Tile(2611, 9447),
                radius = 12,
                volume = 16,
                intervalTicks = 35,
            ),
            // Underground Pass. A long corridor; wind at the mouth and drips further in.
            AmbientSound(
                name = "Underground Pass (entrance)",
                soundId = Sound.CAVE_WIND_LOOP,
                tile = Tile(2387, 9655),
                radius = 12,
                volume = 16,
                intervalTicks = 50,
            ),
            AmbientSound(
                name = "Underground Pass (deep)",
                soundId = Sound.STALAGTITE_DRIP_LOOP1,
                tile = Tile(2439, 9698),
                radius = 12,
                volume = 18,
                intervalTicks = 40,
            ),
            // Clock Tower basement. Four dense clusters inside about 40 tiles; two sources is enough.
            AmbientSound(
                name = "Clock Tower basement (east)",
                soundId = Sound.CAVE_RUMBLING_LOOP_QUIETER,
                tile = Tile(2606, 9641),
                radius = 12,
                volume = 15,
                intervalTicks = 55,
            ),
            AmbientSound(
                name = "Clock Tower basement (west)",
                soundId = Sound.STALAGTITE_DRIP_LOOP1,
                tile = Tile(2585, 9655),
                radius = 12,
                volume = 18,
                intervalTicks = 40,
            ),
            // Smaller dungeons. One source each - a single cluster apiece.
            AmbientSound(
                name = "Edgeville Dungeon",
                soundId = Sound.STALAGTITE_DRIP_LOOP1,
                tile = Tile(3109, 9935),
                radius = 12,
                volume = 18,
                intervalTicks = 40,
            ),
            AmbientSound(
                name = "Karamja Dungeon",
                soundId = Sound.CAVE_BUBBLING_LOOP_1,
                tile = Tile(2856, 9575),
                radius = 12,
                volume = 16,
                intervalTicks = 35,
            ),
            AmbientSound(
                name = "Heroes' Guild basement",
                soundId = Sound.CAVE_RUMBLING_LOOP_1,
                tile = Tile(2936, 9891),
                radius = 12,
                volume = 16,
                intervalTicks = 55,
            ),
            AmbientSound(
                name = "Legends' Guild basement",
                soundId = Sound.CAVE_STALAGTITE_LOOP_1,
                tile = Tile(2731, 9771),
                radius = 12,
                volume = 18,
                intervalTicks = 45,
            ),
            AmbientSound(
                name = "Chaos Druid Tower basement",
                soundId = Sound.STALAGTITE_DRIP_LOOP1,
                tile = Tile(2567, 9752),
                radius = 10,
                volume = 18,
                intervalTicks = 40,
            ),
            AmbientSound(
                name = "The Hollows",
                soundId = Sound.CAVE_WIND_LOOP,
                tile = Tile(3465, 9797),
                radius = 12,
                volume = 16,
                intervalTicks = 50,
            ),
            // Mines. `MINING_LOOP` rather than the cave set - these are worked mines, not caves, and the
            // clip is the pick-and-rubble one.
            AmbientSound(
                name = "Grand Tree Mine",
                soundId = Sound.MINING_LOOP,
                tile = Tile(2451, 9860),
                radius = 12,
                volume = 16,
                intervalTicks = 30,
            ),
            AmbientSound(
                name = "Abandoned Mine",
                soundId = Sound.MINING_LOOP,
                tile = Tile(3412, 9624),
                radius = 12,
                volume = 16,
                intervalTicks = 30,
            ),

            // ---------------------------------------------------------------------------
            // Surface. The second half of the 2026-09-04 coverage pass, after the
            // underground one above. These are the populated areas the audit found with
            // no ambience at all - Ardougne (a whole city), the Wilderness, and the
            // entirety of Kourend among them.
            //
            // Tiles are again the densest spawn clusters of each area's own content
            // rather than points picked by eye. Clips are chosen for the environment:
            // wind on exposed ground, surf on coasts, swamp bubbles in Kebos, lava on
            // Karuulm, birdsong in farmland and woodland.
            // ---------------------------------------------------------------------------
            // Ardougne. A major city with no ambience at all until now. Centred on its market cluster.
            AmbientSound(
                name = "Ardougne market",
                soundId = Sound.MARKET_AMBIENCE_1,
                tile = Tile(2658, 3295),
                radius = 14,
                volume = 24,
                intervalTicks = 30,
            ),
            // Wilderness. Desolate and windy; three sources on the spawn clusters, spread far enough apart that a
            // player only ever hears one. Different wind clips so the three do not sound identical.
            AmbientSound(
                name = "Wilderness (south-west)",
                soundId = Sound.BLUSTERY_WIND_LOOP_2,
                tile = Tile(2982, 3858),
                radius = 14,
                volume = 16,
                intervalTicks = 50,
            ),
            AmbientSound(
                name = "Wilderness (north-east)",
                soundId = Sound.BLUSTERY_WIND_LOOP_3,
                tile = Tile(3297, 3836),
                radius = 14,
                volume = 16,
                intervalTicks = 50,
            ),
            AmbientSound(
                name = "Wilderness (east)",
                soundId = Sound.BLUSTERY_WIND_LOOP_4,
                tile = Tile(3318, 3739),
                radius = 14,
                volume = 16,
                intervalTicks = 50,
            ),
            // Fishing Guild. On the coast - surf at the water's edge and wind over the rest.
            AmbientSound(
                name = "Fishing Guild (waterfront)",
                soundId = Sound.SEASHORE_1,
                tile = Tile(2566, 3444),
                radius = 14,
                volume = 20,
                intervalTicks = 35,
            ),
            AmbientSound(
                name = "Fishing Guild (grounds)",
                soundId = Sound.COASTAL_WIND_LOOP,
                tile = Tile(2583, 3417),
                radius = 14,
                volume = 16,
                intervalTicks = 45,
            ),
            // Kebos Lowlands. Swamp.
            AmbientSound(
                name = "Kebos Lowlands (swamp)",
                soundId = Sound.SWAMP_BUBBLE_LOOP_SLOW_1,
                tile = Tile(1322, 3488),
                radius = 14,
                volume = 18,
                intervalTicks = 40,
            ),
            // Mount Karuulm. Volcanic - lava below, exposed wind above.
            AmbientSound(
                name = "Mount Karuulm (lava flows)",
                soundId = Sound.LAVA_GLOOP_LOOP_1,
                tile = Tile(1286, 3841),
                radius = 14,
                volume = 18,
                intervalTicks = 35,
            ),
            AmbientSound(
                name = "Mount Karuulm (upper slopes)",
                soundId = Sound.BLUSTERY_WIND_LOOP_4,
                tile = Tile(1349, 3785),
                radius = 14,
                volume = 16,
                intervalTicks = 50,
            ),
            // Lovakengj. Sulphur and industry.
            AmbientSound(
                name = "Lovakengj",
                soundId = Sound.LAVA_FIRE_LOOP_1,
                tile = Tile(1503, 3739),
                radius = 14,
                volume = 16,
                intervalTicks = 40,
            ),
            // Arceuus and the Dark Altar. The dark corner of Kourend; the Morytania wind clips suit it.
            AmbientSound(
                name = "Arceuus",
                soundId = Sound.MORY_WIND_LOOP_1,
                tile = Tile(1706, 3708),
                radius = 14,
                volume = 16,
                intervalTicks = 50,
            ),
            AmbientSound(
                name = "Dark Altar",
                soundId = Sound.MORY_WIND_LOOP_2,
                tile = Tile(1704, 3858),
                radius = 14,
                volume = 16,
                intervalTicks = 50,
            ),
            // Slepe. Morytania proper.
            AmbientSound(
                name = "Slepe (south)",
                soundId = Sound.MORYTANIA_WIND_LOOP_1,
                tile = Tile(3738, 3316),
                radius = 14,
                volume = 16,
                intervalTicks = 50,
            ),
            AmbientSound(
                name = "Slepe (north)",
                soundId = Sound.MORY_WIND_LOOP_1,
                tile = Tile(3752, 3398),
                radius = 14,
                volume = 16,
                intervalTicks = 50,
            ),
            // Countryside and woodland. Birdsong rather than wind. These clips are one-shots rather than loops, which suits them -
            // an occasional call at a sparse interval reads as countryside, where a continuous
            // loop would read as a machine.
            AmbientSound(
                name = "Hosidius",
                soundId = Sound.BIRD_TWITTER_1,
                tile = Tile(1775, 3505),
                radius = 14,
                volume = 14,
                intervalTicks = 65,
            ),
            AmbientSound(
                name = "Coal trucks (Seers' woods)",
                soundId = Sound.BIRD_TWITTER_2,
                tile = Tile(2587, 3475),
                radius = 14,
                volume = 14,
                intervalTicks = 65,
            ),
            AmbientSound(
                name = "Keep Le Faye",
                soundId = Sound.BIRD_TWITTER_3,
                tile = Tile(2753, 3401),
                radius = 12,
                volume = 14,
                intervalTicks = 65,
            ),
            AmbientSound(
                name = "Tree Gnome Village",
                soundId = Sound.BIRD_TWITTER_4,
                tile = Tile(2552, 3195),
                radius = 14,
                volume = 14,
                intervalTicks = 65,
            ),
            // Mountains and passes. Exposed, high and cold.
            AmbientSound(
                name = "Custodia Mountains (south)",
                soundId = Sound.ICY_WIND_LOOP_1,
                tile = Tile(1288, 3331),
                radius = 14,
                volume = 16,
                intervalTicks = 50,
            ),
            AmbientSound(
                name = "Custodia Mountains (north)",
                soundId = Sound.ICY_WIND_LOOP_2,
                tile = Tile(1289, 3410),
                radius = 14,
                volume = 16,
                intervalTicks = 50,
            ),
            AmbientSound(
                name = "Arandar pass",
                soundId = Sound.ICY_WIND_LOOP_3,
                tile = Tile(2379, 3336),
                radius = 14,
                volume = 16,
                intervalTicks = 50,
            ),
            AmbientSound(
                name = "Gloomthorn trail",
                soundId = Sound.BLUSTERY_WIND_LOOP_3,
                tile = Tile(1366, 3240),
                radius = 14,
                volume = 16,
                intervalTicks = 50,
            ),
        )
}
