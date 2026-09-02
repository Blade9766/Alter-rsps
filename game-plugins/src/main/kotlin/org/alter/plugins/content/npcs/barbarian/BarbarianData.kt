package org.alter.plugins.content.npcs.barbarian

import org.alter.game.model.combat.CombatStyle

/**
 * Every real Barbarian Village barbarian variant, straight from the OSRS Wiki's
 * versioned `Infobox Monster` parameters (`hitpoints2`, `att2`, `dstab2`, ... one
 * numbered set per variant), plus Gunthor the Brave.
 *
 * The wiki lists 18 barbarian versions. Version 1 (npc 3262) is the Blue Moon Inn
 * barbarian in Varrock - a different location with its own stats (level 8, stab, 6-tick
 * attack speed) - so it is deliberately not included here; this file is Barbarian
 * Village only. That leaves exactly 17 variants, which is exactly the number of
 * barbarian spawn pins the wiki's map has for the village, so each variant is placed on
 * one tile.
 *
 * **Which variant stands on which tile is not published.** The wiki gives the 17 tiles
 * and the 17 ids but never pairs them up, so they are paired here in the wiki's own
 * listing order (tiles sorted as the map template lists them, variants in version
 * order). This is a deliberate stable assignment, not an observed fact - the same call
 * already made for the Dark wizard spawn clusters.
 *
 * Attack and block animations are per-variant and come from this cache's own observed
 * animation sets (`npc-animations/openosrs-animations.json`), resolved by looking each
 * observed id up in [org.alter.api.cfg.Animation]'s named constants rather than by
 * running [org.alter.plugins.content.npcs.animations.MonsterAnimationResolver]. The
 * resolver's duration/priority heuristic mislabels these: it picks the *defend*
 * animation as the attack for several of them (e.g. it calls 3238 - which this codebase
 * already names `HUMAN_GREATAXE_DEFEND` - a barbarian's attack). Every barbarian's
 * observed set is exactly one attack, one block and 836 (`HUMAN_DEATH`), so the named
 * constants disambiguate them exactly. Combat sounds are left unset: the generic
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] fills those in
 * from the cache's own sequence data once the animations are known.
 *
 * Max hits are not stored here because they don't need to be. Feeding the wiki's real
 * levels and bonuses into this server's own melee/ranged formulas already reproduces
 * the wiki's published max hits exactly for all four groups (2 / 2 / 2 / 3, and 4 for
 * Gunthor) - checked by hand against `MeleeCombatFormula`/`RangedCombatFormula` before
 * choosing to leave them declarative.
 */
internal object BarbarianData {
    /** Which of the wiki's two barbarian drop tables a variant rolls on. */
    enum class DropTier { LOW, HIGH }

    data class Variant(
        val npcKey: String,
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
        val dropTier: DropTier,
        val spawnX: Int,
        val spawnZ: Int,
    )

    const val DEATH_ANIMATION = 836 // Animation.HUMAN_DEATH

    /** Wiki `respawn = 50` (every village variant, and Gunthor), in game ticks. */
    const val RESPAWN_CYCLES = 50

    // Attack/block animation pairs, by the weapon each barbarian model carries.
    private const val GREATAXE_SWING = 2067 // Animation.HUMAN_DHAROKS_GREATAXE_SWING
    private const val GREATAXE_DEFEND = 3238 // Animation.HUMAN_GREATAXE_DEFEND
    private const val SWORD_SLASH = 390 // Animation.HUMAN_SLASH_SWORD_ATTACK
    private const val SWORD_DEFEND = 3239
    private const val BLUNT_SWING = 401 // Animation.HUMAN_BLUNT_SWING
    private const val BLUNT_DEFEND = 3240 // Animation.HUMAN_SPIKE_DEFEND
    private const val SPEAR_STAB = 428 // Animation.HUMAN_SPEAR_STAB
    private const val SPEAR_DEFEND = 430 // Animation.HUMAN_SPEAR_DEFEND
    private const val BOW_ATTACK = 426 // Animation.HUMAN_BOW_ATTACK
    private const val BOW_DEFEND = 425 // Animation.HUMAN_DEFEND_COWARDLY

    /**
     * Wiki version 2: the lone level 9 barbarian, and the only village variant whose
     * defensive bonuses differ from the rest (9/10/10 melee, 5 ranged, +16 strength).
     */
    private fun level9(
        npcKey: String,
        attackAnimation: Int,
        blockAnimation: Int,
        spawnX: Int,
        spawnZ: Int,
    ) = Variant(
        npcKey = npcKey,
        combatLevel = 9,
        hitpoints = 20,
        attack = 6,
        strength = 7,
        defence = 3,
        ranged = 1,
        combatStyle = CombatStyle.CRUSH,
        attackSpeed = 4,
        defenceStab = 9,
        defenceSlash = 10,
        defenceCrush = 10,
        defenceMagic = -3,
        defenceRanged = 5,
        attackBonus = 9,
        strengthBonus = 16,
        attackAnimation = attackAnimation,
        blockAnimation = blockAnimation,
        dropTier = DropTier.LOW,
        spawnX = spawnX,
        spawnZ = spawnZ,
    )

    /** Wiki versions 3-12: the ten level 10 barbarians. */
    private fun level10(
        npcKey: String,
        attackAnimation: Int,
        blockAnimation: Int,
        spawnX: Int,
        spawnZ: Int,
    ) = Variant(
        npcKey = npcKey,
        combatLevel = 10,
        hitpoints = 18,
        attack = 8,
        strength = 7,
        defence = 3,
        ranged = 1,
        combatStyle = CombatStyle.CRUSH,
        attackSpeed = 4,
        defenceStab = 0,
        defenceSlash = 3,
        defenceCrush = 2,
        defenceMagic = -3,
        defenceRanged = 2,
        attackBonus = 9,
        strengthBonus = 15,
        attackAnimation = attackAnimation,
        blockAnimation = blockAnimation,
        dropTier = DropTier.LOW,
        spawnX = spawnX,
        spawnZ = spawnZ,
    )

    /** Wiki versions 14-18: the five level 17 barbarians. */
    private fun level17(
        npcKey: String,
        attackAnimation: Int,
        blockAnimation: Int,
        spawnX: Int,
        spawnZ: Int,
    ) = Variant(
        npcKey = npcKey,
        combatLevel = 17,
        hitpoints = 24,
        attack = 15,
        strength = 14,
        defence = 10,
        ranged = 1,
        combatStyle = CombatStyle.CRUSH,
        attackSpeed = 4,
        defenceStab = 0,
        defenceSlash = 3,
        defenceCrush = 2,
        defenceMagic = -3,
        defenceRanged = 2,
        attackBonus = 9,
        strengthBonus = 15,
        attackAnimation = attackAnimation,
        blockAnimation = blockAnimation,
        dropTier = DropTier.HIGH,
        spawnX = spawnX,
        spawnZ = spawnZ,
    )

    /**
     * Wiki version 13 - Aitan, the level 15 barbarian archer, and the only village
     * variant that isn't a melee attacker. Ranged level 15, Strength 3.
     */
    private val ARCHER =
        Variant(
            npcKey = "npc.barbarian_3068",
            combatLevel = 15,
            hitpoints = 24,
            attack = 15,
            strength = 3,
            defence = 10,
            ranged = 15,
            combatStyle = CombatStyle.RANGED,
            attackSpeed = 4,
            defenceStab = 0,
            defenceSlash = 3,
            defenceCrush = 2,
            defenceMagic = -3,
            defenceRanged = 2,
            attackBonus = 9,
            strengthBonus = 15,
            attackAnimation = BOW_ATTACK,
            blockAnimation = BOW_DEFEND,
            dropTier = DropTier.HIGH,
            spawnX = 3083,
            spawnZ = 3429,
        )

    val VILLAGE_VARIANTS: List<Variant> =
        listOf(
            level9("npc.barbarian_3072", SPEAR_STAB, SPEAR_DEFEND, 3075, 3420),
            level10("npc.barbarian_3056", SWORD_SLASH, SWORD_DEFEND, 3075, 3445),
            level10("npc.barbarian_3059", GREATAXE_SWING, GREATAXE_DEFEND, 3076, 3414),
            level10("npc.barbarian_3060", SWORD_SLASH, SWORD_DEFEND, 3078, 3419),
            level10("npc.barbarian_3061", GREATAXE_SWING, GREATAXE_DEFEND, 3078, 3436),
            level10("npc.barbarian_3064", BLUNT_SWING, BLUNT_DEFEND, 3078, 3440),
            level10("npc.barbarian_3065", SWORD_SLASH, SWORD_DEFEND, 3078, 3442),
            level10("npc.barbarian_3066", GREATAXE_SWING, GREATAXE_DEFEND, 3079, 3409),
            level10("npc.barbarian_3067", BLUNT_SWING, BLUNT_DEFEND, 3079, 3437),
            level10("npc.barbarian_3070", SPEAR_STAB, SPEAR_DEFEND, 3079, 3444),
            level10("npc.barbarian_3071", SPEAR_STAB, SPEAR_DEFEND, 3080, 3423),
            ARCHER,
            level17("npc.barbarian", GREATAXE_SWING, GREATAXE_DEFEND, 3084, 3426),
            level17("npc.barbarian_3057", GREATAXE_SWING, GREATAXE_DEFEND, 3085, 3409),
            level17("npc.barbarian_3058", BLUNT_SWING, BLUNT_DEFEND, 3086, 3419),
            level17("npc.barbarian_3062", BLUNT_SWING, BLUNT_DEFEND, 3086, 3440),
            level17("npc.barbarian_3069", SPEAR_STAB, SPEAR_DEFEND, 3096, 3432),
        )

    /**
     * Gunthor the Brave, the tribe's chieftain, in the longhall at (3081, 3444).
     * Unlike the rest of the village he *is* aggressive, hits a 6-tick slash attack,
     * and rolls the same drop table as the level 15/17 barbarians.
     */
    val GUNTHOR =
        Variant(
            npcKey = "npc.gunthor_the_brave",
            combatLevel = 29,
            hitpoints = 35,
            attack = 22,
            strength = 22,
            defence = 25,
            ranged = 1,
            combatStyle = CombatStyle.SLASH,
            attackSpeed = 6,
            defenceStab = 12,
            defenceSlash = 14,
            defenceCrush = 10,
            defenceMagic = -1,
            defenceRanged = 11,
            attackBonus = 8,
            strengthBonus = 13,
            attackAnimation = 395, // Animation.HUMAN_AXE_SWING
            blockAnimation = 398, // Animation.HUMAN_BAXE_DEFEND
            dropTier = DropTier.HIGH,
            spawnX = 3081,
            spawnZ = 3444,
        )
}
