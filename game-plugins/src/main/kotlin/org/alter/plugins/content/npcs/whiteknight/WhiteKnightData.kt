package org.alter.plugins.content.npcs.whiteknight

import org.alter.game.model.combat.CombatStyle

/**
 * The White Knights of Falador, from the OSRS Wiki's four `Infobox Monster` blocks - one
 * per rank (Initiate 36, Proselyte 38, Acolyte 39, Partisan 42). Each rank has a male and
 * a female npc id, and the two share every stat, so ranks are modelled once and the two
 * ids hang off them.
 *
 * **Only the ids the wiki's own map data actually places are used.** The Initiate rank
 * also lists ids 8149 and 11892, but neither appears on any map pin - and a cache check
 * before wiring found both have `actions=[null,null,null,null,null]`, i.e. they are not
 * even attackable, the same trap `npc.aubury` sprang earlier. The eight ids below all
 * carry a real "Attack" option and their cache combat levels match the wiki exactly
 * (asserted in `FaladorCombatVerify`).
 *
 * **Attack style is Slash, from the wiki, even though the attack animation this cache
 * observes for them is named `HUMAN_2H_SWORD_CRUSH`.** That is not a contradiction to fix:
 * the animation constant is just this codebase's label for the client sequence a two-handed
 * sword plays, while `attack style = [[Slash]]` is the mechanic deciding which of the
 * player's defence bonuses gets rolled against. The mechanic wins.
 *
 * Ranged defence: the wiki now splits it into `dlight`/`dstandard`/`dheavy`, but
 * `NpcCombatDef` has a single `defenceRanged`. `dstandard` is used, being the
 * standard-ammo value and the closest single-value analogue.
 *
 * Max hits (6/6/6/7) are not stored - as with the barbarians, feeding these levels and
 * bonuses into this server's own melee formula already reproduces them.
 */
internal object WhiteKnightData {
    data class Rank(
        val name: String,
        val maleKey: String,
        val femaleKey: String,
        val combatLevel: Int,
        val hitpoints: Int,
        val attack: Int,
        val strength: Int,
        val defence: Int,
        val defenceStab: Int,
        val defenceSlash: Int,
        val defenceCrush: Int,
        val defenceMagic: Int,
        val defenceRanged: Int,
        val attackBonus: Int,
        val strengthBonus: Int,
    ) {
        val npcKeys: List<String> get() = listOf(maleKey, femaleKey)
    }

    /** One spawn pin: which npc id stands on which tile, on which plane. */
    data class Spawn(val npcKey: String, val x: Int, val z: Int, val height: Int)

    const val ATTACK_ANIMATION = 406 // Animation.HUMAN_2H_SWORD_CRUSH
    const val BLOCK_ANIMATION = 410 // Animation.HUMAN_2H_SWORD_DEFEND
    const val DEATH_ANIMATION = 836 // Animation.HUMAN_DEATH

    /** Every rank shares `attack speed = 7` and `aggressive = No`. */
    const val ATTACK_SPEED = 7

    /**
     * The wiki's `respawn = 50`, in game ticks and so used as published - the same reading
     * [org.alter.plugins.content.npcs.barbarian.BarbarianData] already applies to the same
     * field, kept consistent deliberately. The infobox does not state its unit.
     */
    const val RESPAWN_CYCLES = 50

    /** Slash for all four ranks, per each infobox's `attack style`. */
    val COMBAT_STYLE = CombatStyle.SLASH

    val INITIATE =
        Rank(
            name = "Initiate",
            maleKey = "npc.white_knight",
            femaleKey = "npc.white_knight_11948",
            combatLevel = 36,
            hitpoints = 52,
            attack = 27,
            strength = 29,
            defence = 21,
            defenceStab = 83,
            defenceSlash = 76,
            defenceCrush = 70,
            defenceMagic = -11,
            defenceRanged = 74,
            attackBonus = 30,
            strengthBonus = 31,
        )

    val PROSELYTE =
        INITIATE.copy(
            name = "Proselyte",
            maleKey = "npc.white_knight_1799",
            femaleKey = "npc.white_knight_11949",
            combatLevel = 38,
            attack = 30,
            defence = 25,
        )

    val ACOLYTE =
        INITIATE.copy(
            name = "Acolyte",
            maleKey = "npc.white_knight_1800",
            femaleKey = "npc.white_knight_11950",
            combatLevel = 39,
            attack = 32,
            defence = 27,
        )

    val PARTISAN =
        INITIATE.copy(
            name = "Partisan",
            maleKey = "npc.white_knight_1829",
            femaleKey = "npc.white_knight_11951",
            combatLevel = 42,
            hitpoints = 55,
            attack = 32,
            strength = 35,
            defence = 27,
            defenceRanged = 85,
        )

    val RANKS = listOf(INITIATE, PROSELYTE, ACOLYTE, PARTISAN)

    /** Which rank a given npc key belongs to. */
    fun rankOf(npcKey: String): Rank = RANKS.first { npcKey in it.npcKeys }

    /**
     * Every White Knight spawn in the White Knights' Castle.
     *
     * Unusually, the wiki's map data annotates each pin with the npc id standing on it
     * (`x:2956,y:3337,title:1798`), so - unlike the barbarians, where variant-to-tile had
     * to be assigned arbitrarily - every knight below is on its real published tile with
     * its real published id. Nothing here is a guess.
     *
     * The four map lines are the castle's four floors, planes 0 to 3. One ground-floor pin
     * (2987, 3332) carries no `title:` annotation at all in the wikitext, so there is no
     * way to know which rank stands there; it is omitted rather than filled in with a
     * guess, leaving 34 of the wiki's 35 pins placed.
     */
    val SPAWNS =
        listOf(
            // Ground floor.
            Spawn("npc.white_knight", 2956, 3337, 0),
            Spawn("npc.white_knight_11948", 2962, 3338, 0),
            Spawn("npc.white_knight", 2972, 3345, 0),
            Spawn("npc.white_knight_11949", 2973, 3347, 0),
            Spawn("npc.white_knight_11949", 2976, 3339, 0),
            Spawn("npc.white_knight_11948", 2978, 3348, 0),
            Spawn("npc.white_knight_11948", 2983, 3330, 0),
            Spawn("npc.white_knight_11948", 2983, 3334, 0),
            Spawn("npc.white_knight", 2983, 3343, 0),
            Spawn("npc.white_knight_11948", 2987, 3341, 0),
            Spawn("npc.white_knight", 2996, 3340, 0),
            // First floor.
            Spawn("npc.white_knight_11948", 2958, 3341, 1),
            Spawn("npc.white_knight", 2961, 3351, 1),
            Spawn("npc.white_knight_1799", 2963, 3336, 1),
            Spawn("npc.white_knight", 2969, 3329, 1),
            Spawn("npc.white_knight_11948", 2969, 3338, 1),
            Spawn("npc.white_knight", 2974, 3337, 1),
            Spawn("npc.white_knight_11948", 2978, 3329, 1),
            Spawn("npc.white_knight_11949", 2982, 3345, 1),
            Spawn("npc.white_knight_1799", 2983, 3335, 1),
            Spawn("npc.white_knight_1800", 2987, 3345, 1),
            Spawn("npc.white_knight_11949", 2989, 3338, 1),
            // Second floor.
            Spawn("npc.white_knight_1829", 2957, 3341, 2),
            Spawn("npc.white_knight_1800", 2964, 3352, 2),
            Spawn("npc.white_knight_1800", 2966, 3328, 2),
            Spawn("npc.white_knight_1799", 2972, 3329, 2),
            Spawn("npc.white_knight_11950", 2975, 3328, 2),
            Spawn("npc.white_knight_11948", 2981, 3339, 2),
            Spawn("npc.white_knight_1799", 2984, 3350, 2),
            Spawn("npc.white_knight_1799", 2985, 3343, 2),
            Spawn("npc.white_knight_1800", 2990, 3346, 2),
            // Third floor.
            Spawn("npc.white_knight_1829", 2960, 3338, 3),
            Spawn("npc.white_knight_11950", 2963, 3340, 3),
            Spawn("npc.white_knight_11951", 2982, 3352, 3),
        )
}
