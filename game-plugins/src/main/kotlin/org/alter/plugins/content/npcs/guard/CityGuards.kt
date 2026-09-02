package org.alter.plugins.content.npcs.guard

import org.alter.game.model.combat.CombatStyle

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
    /** The wiki's published pins, as (x, z, plane). */
    val tiles: List<Triple<Int, Int, Int>>,
) {
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
 * standard human rig - `standAnim = 808`, `walkAnim = 819`. Animations are only
 * interchangeable within a rig, so the *first* thing to check before wiring an animation
 * onto an npc is that the animation lives in the same frame group family as that npc's own
 * stand/walk animations. See the Varrock note below for what happens when it does not.
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
     * Those three sequences live in frame group 1056, a closed rig of fifteen sequences
     * carrying its own stand, walk, attack, block and death, used only by a handful of
     * pre-2022 npcs - the old Guard 3010/3011, Trainee Guard, Captain, Sir Mordred,
     * Lucien, Cleaner. The 9 November 2022 "Diversity & Inclusion Changes" update
     * re-modelled every Varrock guard onto the standard human rig, so 11911-11917 all
     * carry `standAnim = 808` / `walkAnim = 819` like the other two cities. Playing a
     * group-1056 sequence on that rig deforms the model instead of animating it.
     *
     * The replacement is the generic human *sword* set, because animation follows the
     * weapon the model actually holds: every one of these ids composes item model 518, the
     * longsword. That is the same rule `FaladorGuardData` applies - its battleaxe guard is
     * Crush but swings an axe - so the published Crush attack style stays as it is and only
     * drives which defence bonus the hit rolls against, not which animation is played.
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
            attackAnimation = 390, // Animation.HUMAN_SLASH_SWORD_ATTACK
            blockAnimation = 388, // Animation.HUMAN_SLASH_SWORD_DEFEND
            deathAnimation = 836, // Animation.HUMAN_DEATH
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
