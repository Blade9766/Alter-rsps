package org.alter.plugins.content.npcs.guard

import org.alter.game.model.combat.CombatStyle
import org.alter.plugins.content.combat.WeaponSounds

/**
 * A city's guards, where the OSRS Wiki gives that city **one unversioned stat block** -
 * every version shares `att = 19` rather than `att1`, `att2`, ... - so all of its numbered
 * ids are mechanically identical and differ only in appearance.
 *
 * Falador is deliberately not modelled here: its eleven versions split into four different
 * stat groups and include ranged archers, so it keeps its own
 * [org.alter.plugins.content.npcs.faladorguard.FaladorGuardData]. The drop table is shared
 * with it regardless - see [GuardDrops].
 */
internal data class CityGuard(
    val city: String,
    val npcKeys: List<String>,
    val combatLevel: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val combatStyle: CombatStyle,
    val attackSpeed: Int,
    val defenceStab: Int,
    val defenceSlash: Int,
    val defenceCrush: Int,
    val defenceMagic: Int,
    val defenceRanged: Int,
    val attackBonus: Int,
    val strengthBonus: Int,
    val attackAnimation: Int,
    val blockAnimation: Int,
    val deathAnimation: Int,
    /**
     * The weapon this city's guard models carry. It is the *weapon*, not the attack, that
     * is stated here: which clip of that weapon's set plays follows [attackAnimation], so
     * the sound can never drift from the swing on screen. The block and death clips are the
     * generic human ones every city shares - see [CityGuard.HUMAN_BLOCK_SOUND].
     */
    val weapon: WeaponSounds.Weapon,
    /** The wiki's published pins, as (x, z, plane). */
    val tiles: List<Triple<Int, Int, Int>>,
) {
    /** The clip this guard's weapon makes performing [attackAnimation]. */
    val attackSound: Int get() = WeaponSounds.forAnimation(weapon, attackAnimation)

    /**
     * **Which id stands on which tile is not published** for any of these cities - unlike
     * the White Knights, whose pins carry a `title:` id each, guard pins carry only
     * coordinates. The ids are dealt round-robin over the tiles in the wiki's own listing
     * order: a deliberate stable assignment, not an observed fact.
     */
    val spawns: List<GuardSpawn>
        get() =
            tiles.mapIndexed { index, (x, z, height) ->
                GuardSpawn(npcKeys[index % npcKeys.size], x, z, height)
            }

    companion object {
        /** Every city guard shares these: 22 hitpoints, no magic or ranged, `aggressive = No`. */
        const val HITPOINTS = 22
        const val MAGIC_LEVEL = 1
        const val RANGED_LEVEL = 1

        /**
         * The wiki's `respawn = 50`. That field is in **game ticks**, which are this
         * engine's cycles one-for-one, so it is used as published - the same reading
         * applied to the same field across the other monster files here.
         */
        const val RESPAWN_CYCLES = 50

        /**
         * Combat sounds. **Nothing gives these guards a sound unless it is set here.**
         * `MonsterAnimationsPlugin` resolves an npc's combat audio in two steps - a name
         * lookup in `named-combat-media.json`, then a fallback that reads whatever sound is
         * baked into the resolved animation - and both come back empty for guards: their
         * cache name is the bare string `Guard`, which matches no key in that file, and
         * this cache carries **zero** embedded sound data on any animation sequence (386,
         * 388, 401, 403 and 836 all decode with `sounds={}` and `soundEffects=[]`). So both
         * paths fall through to -1 and `MeleeCombatStrategy` / `Combat.postDamage` /
         * `NpcDeathAction` skip their `> 0` guards entirely. That is why guards were silent
         * in every city, not just the one it was noticed in.
         *
         * Ids are `Sound.kt`'s named constants, each confirmed to exist as a real archive
         * in cache index 4 (`SOUNDEFFECTS`) by `CityGuardVerify` - a sound id that has no
         * archive is written to the client and then silently dropped, which looks identical
         * to having no sound at all.
         *
         * Note that RuneLite's `SoundEffectID` calls 511 `ZERO_DAMAGE_SPLAT` where
         * `Sound.kt` calls it `HUMAN_BLOCK_1`; they are describing the same clip from
         * different ends (it is what a human plays when a hit is parried). 511/512 are kept
         * because they are already what the working `MAN` and `WOMAN` entries in
         * `named-combat-media.json` use, and guards are the same human models.
         */
        const val HUMAN_BLOCK_SOUND = 511 // Sound.HUMAN_BLOCK_1
        const val HUMAN_DEATH_SOUND = 512 // Sound.HUMAN_DEATH

        /**
         * Combat audio is spawned as an [org.alter.game.model.entity.AreaSound] rather than
         * played to the one player involved, which is what OSRS does - you hear fights you
         * are walking past. Radius 5 and volume 1 are the same pair
         * `MeleeCombatStrategy` already uses for the player's own weapon swing.
         *
         * Volume is really the `loops` field of `SynthSound`/`SoundArea`, not a gain, so it
         * must be 1. `SoundBuilder` defaults it to **50**, which would play every swing
         * fifty times over.
         */
        const val SOUND_RADIUS = 5
        const val SOUND_LOOPS = 1
    }
}

internal data class GuardSpawn(val npcKey: String, val x: Int, val z: Int, val height: Int)

/**
 * The single-stat-group city guards: Varrock, Edgeville and Ardougne.
 *
 * All ids and stats come from each city's own `Infobox Monster` block on the wiki's Guard
 * page, and every id's cache combat level was checked against the wiki's before wiring.
 * Animations come from each city's observed animation set resolved through
 * [org.alter.api.cfg.Animation]'s named constants - the reliable method, since
 * `MonsterAnimationResolver`'s duration heuristic mislabels attack vs block for humanoids.
 *
 * Note how genuinely different the three are despite all being called "Guard": Varrock and
 * Ardougne are Crush while Edgeville is Stab, and Ardougne is a level lower and slower
 * (attack speed 5 vs 4) with the only *positive* magic defence of the three. They are not
 * interchangeable.
 *
 * **All three use generic human animations**, because all of their ids are modelled on the
 * standard human skeleton - `standAnim = 808`, `walkAnim = 819`.
 *
 * **Compatibility is decided by which transform groups a sequence drives, not by its frame
 * group and not by its framemap alone.** Both of the looser tests give false answers here:
 *
 * - The frame group is only a bundle of frames. 808 (group 207) and 401 (group 209) drive
 *   the same player without a seam, and `FaladorGuardData` puts crossbow 2075 (group 219) on
 *   npc 3270, which stands on group 1088.
 * - The framemap - the skeleton id in the first two bytes of every frame file in cache index
 *   0 - is necessary but not sufficient. Every human frame group in this cache resolves to
 *   **framemap 0**: 207, 209, 219, 197, 233, 245, 1088 *and 1056*. Sharing it did not make
 *   1056 usable here; see the Varrock note.
 *
 * Framemap 0 declares 245 transform groups, and animations built for different models use
 * disjoint blocks of them. Every standard human sequence - stand 808, walk 819, 386, 388,
 * 390, 395, 401, 422, 2075, death 836 - drives groups in the **0-61** range. The old guard
 * rig's 6488/6489/6490 drive **134-215** and touch nothing in 0-61 at all. An animation whose
 * groups do not overlap the ones the npc's own stand and walk animations drive is moving
 * bones the model does not have posed, which is what deforms it. `CityGuardVerify` asserts
 * that overlap.
 */
internal object CityGuards {
    /**
     * **These replace three inert npcs.** `areas/varrock/spawns/SpawnPlugin` previously
     * spawned `guard_998`, `guard_999` and `guard_1000` on invented coordinates. A cache
     * check showed all three are combat level 0 with `actions=[null,null,null,null,null]` -
     * not attackable, talkable or pickpocketable, pure scenery.
     *
     * The wiki writes versions 1 and 7 as `11911,hist3010` and `11917,hist3011`; 3010 and
     * 3011 are the historical ids and are not used.
     *
     * **Do not give these guards the `VARROCK_GUARD_ATTACK`/`_HIT`/`_DEATH` constants**
     * (6489/6488/6490), even though `Animation.kt` carries them under that name and
     * `npc-animations/openosrs-animations.json` observes exactly that set on id 3010.
     * They belong to id 3010, which the 9 November 2022 "Diversity & Inclusion Changes"
     * update replaced: the live ids 11911-11917 are new models, and 3010's observed set is
     * evidence about a model that no longer exists rather than about these.
     *
     * They are also geometrically wrong, though not for the reason first given. This file
     * used to say group 1056 was "a foreign rig"; group 1056 in fact resolves to framemap 0,
     * the same skeleton 207/209/219 resolve to, so frame group and framemap both say these
     * sequences are compatible. Decoding the frames shows they are not: 6488/6489/6490 drive
     * framemap-0 transform groups 134-215, and every animation these models are actually
     * posed by - their own stand 808 and walk 819 included - drives 0-61. The two sets do not
     * intersect at all, so those sequences move bones nothing else in the model's repertoire
     * touches.
     *
     * The set used instead is the one **every human guard in the game is observed to use**:
     * 386 attack, 388 block, 836 death - `openosrs-animations.json` records `[388, 386,
     * 836]` for Edgeville's 3254 and `[388, 836, 386]` for Falador's 3269, both of which
     * are the same longsword-and-kiteshield guard as these. 11911-11917 have no observed
     * set of their own, so they inherit their siblings' rather than an inference from the
     * weapon model. The published Crush attack style stays as it is and only drives which
     * defence bonus the hit rolls against, not which animation is played.
     *
     * **Specifically 386, not 390.** These guards were first given 390, reasoned to from the
     * longsword in their model list rather than taken from any observed set, and that is the
     * only thing about Varrock's wiring that differed from the two cities reported as fine
     * (Edgeville swings 386, Ardougne 401). It was reported in-game as the guards' heads
     * jerking on every swing, Varrock only.
     *
     * Be careful how much weight that carries. 390 is not a broken sequence: it is observed
     * on 117 npcs including guards 995, 4521-4525, 4669 and 5141, and this repo keeps it
     * wired on the barbarians (3056/3060/3065) and Ardougne paladins (3293/3294), all
     * observation-backed. Nor is it structurally foreign the way 6488-6490 are - it drives
     * groups 2, 4, 5, 7-9, 14, 15, 22-56, squarely inside the 0-61 human block.
     *
     * The one frame-level oddity, recorded because it is the only difference left and not
     * because it is proven to be the cause: **390 is the only sequence in any of the four
     * cities' sets that drives group 2 - the whole-model *rotation* - without also driving
     * group 1, the whole-model *translation*.** 386, 388, 395, 397, 403, 424 and 425 all
     * drive both; 401, 422, 426, 2075, 808 drive group 1 alone. Only 390 has the rotation
     * without the translation.
     *
     * What is solid is the provenance: 11911-11917 have no observed set, their family does,
     * and 386 makes their wiring identical to Edgeville's, which behaves.
     *
     * Untested prediction worth knowing: `ArdougneKnightData` gives 390 to paladins
     * 11930-11933 and knight 11936, which are 2022 remodels with no observed set of their
     * own, inheriting 3293/3294's. If the remodelled human models are what 390 disagrees
     * with, those five jerk too - and if they do not, 390 was not the cause here either.
     *
     * Tiles are the wiki's five Varrock location blocks: the city, the walls, and the
     * castle's three floors.
     */
    val VARROCK =
        CityGuard(
            city = "Varrock",
            npcKeys =
                listOf(
                    "npc.guard_11911", "npc.guard_11912", "npc.guard_11913", "npc.guard_11914",
                    "npc.guard_11915", "npc.guard_11916", "npc.guard_11917",
                ),
            combatLevel = 21,
            attack = 19,
            strength = 18,
            defence = 14,
            combatStyle = CombatStyle.CRUSH,
            attackSpeed = 4,
            defenceStab = 24,
            defenceSlash = 30,
            defenceCrush = 25,
            defenceMagic = -9,
            defenceRanged = 25,
            attackBonus = 4,
            strengthBonus = 5,
            attackAnimation = 386, // Animation.HUMAN_DAGGER_STAB
            blockAnimation = 388, // Animation.HUMAN_SLASH_SWORD_DEFEND
            deathAnimation = 836, // Animation.HUMAN_DEATH
            // The longsword these models carry, matching the sword animation above rather
            // than the published Crush style - the style decides which defence the roll
            // goes against, the sound is what the weapon in the model's hand sounds like.
            weapon = WeaponSounds.Weapon.SWORD,
            tiles =
                listOf(
                    // Varrock, plane 0.
                    Triple(3205, 3379, 0), Triple(3211, 3378, 0), Triple(3211, 3381, 0),
                    Triple(3173, 3429, 0), Triple(3175, 3423, 0), Triple(3175, 3428, 0),
                    Triple(3240, 3500, 0), Triple(3246, 3501, 0), Triple(3271, 3428, 0),
                    Triple(3273, 3430, 0), Triple(3275, 3422, 0), Triple(3275, 3428, 0),
                    Triple(3180, 3401, 0),
                    // Varrock Walls, plane 1.
                    Triple(3175, 3403, 1), Triple(3175, 3414, 1),
                    // Varrock Castle, ground floor.
                    Triple(3206, 3462, 0), Triple(3211, 3463, 0), Triple(3212, 3462, 0),
                    Triple(3204, 3496, 0), Triple(3213, 3465, 0), Triple(3215, 3464, 0),
                    Triple(3217, 3461, 0),
                    // Varrock Castle, first floor.
                    Triple(3201, 3495, 1), Triple(3204, 3491, 1), Triple(3205, 3494, 1),
                    Triple(3205, 3498, 1),
                    // Varrock Castle, second floor.
                    Triple(3203, 3479, 2), Triple(3204, 3477, 2), Triple(3206, 3478, 2),
                    Triple(3206, 3481, 2), Triple(3207, 3476, 2), Triple(3208, 3473, 2),
                    Triple(3209, 3471, 2), Triple(3210, 3474, 2), Triple(3211, 3468, 2),
                    Triple(3221, 3483, 2), Triple(3222, 3470, 2),
                ),
        )

    /**
     * Edgeville's six guards, by the bridge and the general store.
     *
     * Mechanically identical to Falador's *sword* guards - same level, stats, bonuses,
     * animations and Stab attack style - which is why they reuse the same generic human
     * dagger/sword animations rather than anything bespoke.
     */
    val EDGEVILLE =
        CityGuard(
            city = "Edgeville",
            npcKeys = listOf("npc.guard_3254", "npc.guard_11922", "npc.guard_11923", "npc.guard_11924"),
            combatLevel = 21,
            attack = 19,
            strength = 18,
            defence = 14,
            combatStyle = CombatStyle.STAB,
            attackSpeed = 4,
            defenceStab = 18,
            defenceSlash = 25,
            defenceCrush = 19,
            defenceMagic = -4,
            defenceRanged = 20,
            attackBonus = 4,
            strengthBonus = 5,
            attackAnimation = 386, // Animation.HUMAN_DAGGER_STAB
            blockAnimation = 388, // Animation.HUMAN_SLASH_SWORD_DEFEND
            deathAnimation = 836, // Animation.HUMAN_DEATH
            weapon = WeaponSounds.Weapon.SWORD, // The same longsword guard as Varrock's.
            tiles =
                listOf(
                    Triple(3085, 3518, 0), Triple(3093, 3518, 0), Triple(3109, 3513, 0),
                    Triple(3110, 3515, 0), Triple(3114, 3512, 0), Triple(3114, 3517, 0),
                ),
        )

    /**
     * East Ardougne's ten guards - the only members-world city of the three
     * (`members = Yes`), and the only guards that are not level 21.
     *
     * Their stat line is the odd one out in several ways, all real: level 20 rather than
     * 21, attack 17 and defence 13 rather than 19/14, attack speed 5 rather than 4, a
     * noticeably higher attack bonus (9 vs 4) and strength bonus (7 vs 5), and the only
     * *positive* magic defence among any guards (+4, where the others are -4 or -9). Their
     * blunt-weapon animations match the Crush attack style.
     */
    val ARDOUGNE =
        CityGuard(
            city = "Ardougne",
            npcKeys = listOf("npc.guard_5418", "npc.guard_11937", "npc.guard_11938", "npc.guard_11939"),
            combatLevel = 20,
            attack = 17,
            strength = 18,
            defence = 13,
            combatStyle = CombatStyle.CRUSH,
            attackSpeed = 5,
            defenceStab = 24,
            defenceSlash = 14,
            defenceCrush = 19,
            defenceMagic = 4,
            defenceRanged = 16,
            attackBonus = 9,
            strengthBonus = 7,
            attackAnimation = 401, // Animation.HUMAN_BLUNT_SWING
            blockAnimation = 403, // Animation.HUMAN_BLUNT_DEFEND2
            deathAnimation = 836, // Animation.HUMAN_DEATH
            // The mace that goes with the blunt animations and the Crush style - the one
            // city of the three whose guards do not swing a sword.
            weapon = WeaponSounds.Weapon.MACE,
            tiles =
                listOf(
                    Triple(2651, 3307, 0), Triple(2659, 3309, 0), Triple(2660, 3309, 0),
                    Triple(2661, 3309, 0), Triple(2663, 3301, 0), Triple(2664, 3318, 0),
                    Triple(2665, 3300, 0), Triple(2635, 3339, 0), Triple(2636, 3340, 0),
                    Triple(2637, 3339, 0),
                ),
        )

    val ALL = listOf(VARROCK, EDGEVILLE, ARDOUGNE)
}
