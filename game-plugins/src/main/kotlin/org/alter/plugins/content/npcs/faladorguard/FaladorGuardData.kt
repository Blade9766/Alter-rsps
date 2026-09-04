package org.alter.plugins.content.npcs.faladorguard

import org.alter.game.model.combat.CombatStyle
import org.alter.plugins.content.combat.WeaponSounds

/**
 * The city guards of Falador, from the OSRS Wiki's Falador `Infobox Monster` block - the
 * one whose versions are named "Falador (sword, 1)" through "Falador (longbow, 3)".
 *
 * The wiki's eleven versions collapse into four stat groups, which is what [Group] models:
 * the four sword guards are identical to each other, and so on. Every id's cache combat
 * level was checked against the wiki's before wiring and all eleven match.
 *
 * Two things fall out of the data that are worth stating, because they look like mistakes
 * and are not:
 * - **The wall guards are all archers.** The wiki's second location block ("Falador Walls",
 *   plane 1) is `levels = 22`, and every level-22 Falador version is a crossbow or longbow
 *   guard - `range = 26` where the melee ones have `range = 1`. So the guards patrolling
 *   the battlements really do shoot down at you, and the ones in the streets don't.
 * - **The battleaxe guard's attack style is Crush, not Slash**, per `attack style7 =
 *   [[Crush]]`, even though its observed attack animation is `HUMAN_AXE_SWING`. As with the
 *   White Knights, the wiki's style is the mechanic and wins over the animation's name.
 *
 * Version 9's id is written `11947,hist3272` on the wiki - 3272 is the historical id and
 * 11947 the current one, so 11947 is what gets used.
 */
internal object FaladorGuardData {
    data class Group(
        val name: String,
        val npcKeys: List<String>,
        val combatLevel: Int,
        val hitpoints: Int,
        val attack: Int,
        val strength: Int,
        val defence: Int,
        val ranged: Int,
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
        /**
         * The weapon this group carries, which with [attackAnimation] picks the attack clip.
         * The weapon is stated rather than the published attack style: the style decides
         * which defence bonus the roll goes against, the sound is what is in the model's
         * hand - so the battleaxe group sounds like an axe even though the wiki gives it
         * Crush, and which clip of the axe set plays follows the animation, so the sound can
         * never drift from the swing on screen. Block and death are the shared human clips -
         * see
         * [org.alter.plugins.content.npcs.guard.CityGuard.HUMAN_BLOCK_SOUND] for why any of
         * this has to be stated at all.
         */
        val weapon: WeaponSounds.Weapon,
        /** Projectile spotanim, or -1 for the melee groups. */
        val projectile: Int = -1,
        /** Drawback spotanim, or -1 where the codebase's own ammo data defines none. */
        val drawback: Int = -1,
    ) {
        /** The clip this group's weapon makes performing [attackAnimation]. */
        val attackSound: Int get() = WeaponSounds.forAnimation(weapon, attackAnimation)
    }

    data class Spawn(val npcKey: String, val x: Int, val z: Int, val height: Int)

    const val DEATH_ANIMATION = 836 // Animation.HUMAN_DEATH

    /** The wiki's `respawn = 50`, in game ticks - see WhiteKnightData for the same note. */
    const val RESPAWN_CYCLES = 50

    // Attack/block pairs. The three groups that have observed animation sets in this
    // cache use them, resolved through Animation.kt's named constants (the reliable
    // method - MonsterAnimationResolver's heuristic mislabels humanoid attack vs block).
    // The unarmed and longbow guards have no observed set at all, so they take the
    // obvious named constants for their weapon instead.
    private const val SWORD_ATTACK = 386 // Animation.HUMAN_DAGGER_STAB
    private const val SWORD_DEFEND = 388 // Animation.HUMAN_SLASH_SWORD_DEFEND
    private const val CROSSBOW_ATTACK = 2075 // Animation.HUMAN_KARILS_CROSSBOW_ATTACK
    private const val RANGED_DEFEND = 425 // Animation.HUMAN_DEFEND_COWARDLY
    private const val AXE_ATTACK = 395 // Animation.HUMAN_AXE_SWING
    private const val AXE_DEFEND = 397 // Animation.HUMAN_BLUNT_DEFEND
    private const val PUNCH_ATTACK = 422 // Animation.HUMAN_PUNCH
    private const val PUNCH_DEFEND = 424 // Animation.HUMAN_DEFEND
    private const val BOW_ATTACK = 426 // Animation.HUMAN_BOW_ATTACK

    /**
     * Bolt spotanim, taken from this codebase's own `RangedProjectile.BOLTS` (gfx 27),
     * which defines no drawback - so neither does this. Fits the iron bolts on their own
     * drop table.
     */
    private const val BOLT_PROJECTILE = 27

    /**
     * Bronze arrow spotanim and drawback, the same pair the barbarian archer uses and the
     * one the combat DSL's own doc comment gives as its example. The wiki never names what
     * ammo these guards fire; bronze is the consistent choice against their own drop table,
     * not a documented fact.
     */
    private const val ARROW_PROJECTILE = 10
    private const val ARROW_DRAWBACK = 19

    val SWORD =
        Group(
            name = "sword",
            npcKeys = listOf("npc.guard_3269", "npc.guard_11942", "npc.guard_11943", "npc.guard_11944"),
            combatLevel = 21,
            hitpoints = 22,
            attack = 19,
            strength = 18,
            defence = 14,
            ranged = 1,
            combatStyle = CombatStyle.STAB,
            attackSpeed = 4,
            defenceStab = 18,
            defenceSlash = 25,
            defenceCrush = 19,
            defenceMagic = -4,
            defenceRanged = 20,
            attackBonus = 4,
            strengthBonus = 5,
            attackAnimation = SWORD_ATTACK,
            blockAnimation = SWORD_DEFEND,
            weapon = WeaponSounds.Weapon.SWORD,
        )

    val CROSSBOW =
        Group(
            name = "crossbow",
            npcKeys = listOf("npc.guard_3270", "npc.guard_11945"),
            combatLevel = 22,
            hitpoints = 22,
            attack = 15,
            strength = 15,
            defence = 16,
            ranged = 26,
            combatStyle = CombatStyle.RANGED,
            attackSpeed = 6,
            defenceStab = 13,
            defenceSlash = 17,
            defenceCrush = 14,
            defenceMagic = -4,
            defenceRanged = 15,
            attackBonus = 6,
            strengthBonus = 10,
            attackAnimation = CROSSBOW_ATTACK,
            blockAnimation = RANGED_DEFEND,
            weapon = WeaponSounds.Weapon.CROSSBOW,
            projectile = BOLT_PROJECTILE,
        )

    val BATTLEAXE =
        Group(
            name = "battleaxe",
            npcKeys = listOf("npc.guard_3271"),
            combatLevel = 19,
            hitpoints = 22,
            attack = 15,
            strength = 15,
            defence = 16,
            ranged = 1,
            combatStyle = CombatStyle.CRUSH,
            attackSpeed = 6,
            defenceStab = 5,
            defenceSlash = 5,
            defenceCrush = 5,
            defenceMagic = -4,
            defenceRanged = 5,
            attackBonus = 6,
            strengthBonus = 10,
            attackAnimation = AXE_ATTACK,
            blockAnimation = AXE_DEFEND,
            weapon = WeaponSounds.Weapon.AXE,
        )

    val UNARMED =
        BATTLEAXE.copy(
            name = "unarmed",
            npcKeys = listOf("npc.guard_11946"),
            attackAnimation = PUNCH_ATTACK,
            blockAnimation = PUNCH_DEFEND,
            weapon = WeaponSounds.Weapon.UNARMED,
        )

    val LONGBOW =
        Group(
            name = "longbow",
            npcKeys = listOf("npc.guard_11947", "npc.guard_3273", "npc.guard_3274"),
            combatLevel = 22,
            hitpoints = 22,
            attack = 15,
            strength = 15,
            defence = 16,
            ranged = 26,
            combatStyle = CombatStyle.RANGED,
            attackSpeed = 6,
            defenceStab = 5,
            defenceSlash = 5,
            defenceCrush = 5,
            defenceMagic = -4,
            defenceRanged = 5,
            attackBonus = 6,
            strengthBonus = 10,
            attackAnimation = BOW_ATTACK,
            blockAnimation = RANGED_DEFEND,
            weapon = WeaponSounds.Weapon.BOW,
            projectile = ARROW_PROJECTILE,
            drawback = ARROW_DRAWBACK,
        )

    /** In the wiki's own version order, which is what the spawn assignment below cycles. */
    val GROUPS = listOf(SWORD, CROSSBOW, BATTLEAXE, UNARMED, LONGBOW)

    /** The eleven ids in wiki version order. */
    private val ALL_VARIANTS: List<String> =
        SWORD.npcKeys + CROSSBOW.npcKeys + BATTLEAXE.npcKeys + UNARMED.npcKeys + LONGBOW.npcKeys

    /** The level-22 ids, in wiki version order - the only ranks that stand on the walls. */
    private val LEVEL_22_VARIANTS: List<String> = CROSSBOW.npcKeys + LONGBOW.npcKeys

    fun groupOf(npcKey: String): Group = GROUPS.first { npcKey in it.npcKeys }

    /**
     * The wiki's street-level guard pins, `plane = 0`, `levels = 19, 21, 22`.
     */
    private val STREET_TILES =
        listOf(
            2948 to 3353, 3036 to 3355, 3041 to 3355, 2944 to 3376, 2950 to 3376,
            2956 to 3382, 2962 to 3381, 2964 to 3376, 2965 to 3384, 2965 to 3391,
            2968 to 3379, 2962 to 3398, 2964 to 3394, 2966 to 3396, 2967 to 3393,
            3006 to 3321, 3006 to 3323, 3007 to 3323,
        )

    /**
     * The wiki's "Falador Walls" pins, `plane = 1`, `levels = 22`.
     */
    private val WALL_TILES =
        listOf(
            3031 to 3389, 3039 to 3388, 3049 to 3389, 3056 to 3389, 3064 to 3384,
            3028 to 3329, 3038 to 3329, 3051 to 3329, 3059 to 3330,
        )

    /**
     * **Which variant stands on which tile is not published** - unlike the White Knights,
     * whose pins carry a `title:` id each, the guard pins carry only coordinates. So the
     * ids are dealt round-robin over the tiles in the wiki's own listing order: all eleven
     * versions across the 18 street tiles, and only the level-22 (ranged) versions across
     * the 9 wall tiles, since the wall block is explicitly `levels = 22`. That is a
     * deliberate stable assignment, not an observed fact - the same call already made for
     * the barbarians and the dark wizard clusters.
     */
    val SPAWNS: List<Spawn> =
        STREET_TILES.mapIndexed { index, (x, z) ->
            Spawn(ALL_VARIANTS[index % ALL_VARIANTS.size], x, z, 0)
        } +
            WALL_TILES.mapIndexed { index, (x, z) ->
                Spawn(LEVEL_22_VARIANTS[index % LEVEL_22_VARIANTS.size], x, z, 1)
            }
}
