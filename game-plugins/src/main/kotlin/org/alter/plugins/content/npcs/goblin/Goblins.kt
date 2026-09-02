package org.alter.plugins.content.npcs.goblin

import org.alter.game.model.combat.CombatStyle

/**
 * The four real goblin combat variants, straight off the OSRS Wiki's Goblin page
 * `Infobox Monster` (`version1`..`version4`). See [GoblinPlugin] for the wiring and
 * [GoblinDrops] for the two drop tables.
 *
 * Goblins are the one monster on this server where "which id" is a purely cosmetic
 * choice *within* a variant and a mechanical one *between* variants:
 *
 * - **Level 2** (30 ids) and **level 2 armed** (7 ids) look completely different from
 *   each other - the armed ones carry a weapon and shield - yet share 5 hitpoints. They
 *   differ in attack/defence and in which drop table they roll.
 * - The **level 5** ids are the only goblins that fight with [CombatStyle.STAB] and the
 *   only ones with *positive* attack and strength bonuses (+12/+12 against the level 2s'
 *   -21/-15), on a slower 6-cycle attack.
 * - The **level 13** id is a different monster wearing the same face: 16 hitpoints,
 *   attack 12, strength 13, a max hit of 2, and real positive defence bonuses.
 *
 * Within a variant the numbered ids are interchangeable - every one of them carries the
 * same combat block on the wiki, and all thirty level-2 ids share the examine text
 * "An ugly green creature." in this cache's own npcs.csv. What they encode is the mail
 * colour, which is why the Lumbridge spawn list deals them out round-robin rather than
 * spawning thirty copies of `goblin_3028`.
 *
 * All four variants are defined here even though only level 2 and level 5 are spawned in
 * Lumbridge today: the armed and level 13 goblins live in members areas (Goblin Cave, the
 * Clock Tower and Underground Pass dungeons, The Hollows) that aren't built yet, and
 * defining them now means those spawns are correct the day they are added rather than
 * silently inheriting [org.alter.game.model.combat.NpcCombatDef.DEFAULT]'s 10 hitpoints
 * and zeroed stats, which is exactly what every goblin on this server had until now.
 */
internal data class GoblinVariant(
    val name: String,
    val combatLevel: Int,
    val npcKeys: List<String>,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val attackSpeed: Int,
    val combatStyle: CombatStyle,
    val attackBonus: Int,
    val strengthBonus: Int,
    val defenceStab: Int,
    val defenceSlash: Int,
    val defenceCrush: Int,
    val defenceMagic: Int,
    val defenceRanged: Int,
    val slayerXp: Double,
    val dropTable: GoblinDrops.Table,
    /**
     * Aggro radius in tiles, or 0 for a passive goblin - which is all of them but the
     * Goblin Cave sentry. See [Goblins.CAVE_SENTRY_ID].
     */
    val aggroRadius: Int = 0,
    /**
     * Combat animations. Defaulted to the ordinary goblin set, which is right for every
     * variant except the level 5s - see [Goblins.LEVEL_5_IDS].
     */
    val attackAnimation: Int = Goblins.ATTACK_ANIMATION,
    val blockAnimation: Int = Goblins.BLOCK_ANIMATION,
    val deathAnimation: Int = Goblins.DEATH_ANIMATION,
)

internal object Goblins {
    /**
     * Wiki `respawn = 35`. That field is in **game ticks**, not seconds, and a tick is
     * this engine's cycle, so it is used as published with no conversion.
     *
     * These files previously divided it by 0.6 as though it were seconds, which made every
     * respawn on the server about 1.67x too long. Corrected across all the monster packages
     * at once; if a respawn ever looks wrong again, check that first.
     */
    const val RESPAWN_CYCLES = 35

    /**
     * Goblin-specific animations and combat sounds are already bundled in this project's
     * own `npc-animations/named-combat-media.json` under the key `GOBLIN`, which is where
     * these three come from. They have to be repeated here because
     * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] only *resolves*
     * animations for npcs that have no explicit combat def - once [GoblinPlugin] sets one,
     * that plugin keeps the def's animations and only fills in the sounds (469/472/471)
     * from the same named entry.
     */
    const val ATTACK_ANIMATION = 6184
    const val BLOCK_ANIMATION = 6183
    const val DEATH_ANIMATION = 6182

    /** Wiki `mage = 1` and `range = 1` on every version. */
    const val MAGIC_LEVEL = 1
    const val RANGED_LEVEL = 1

    /**
     * Level 2, unarmed, free-to-play - the goblins that fill the fields around Lumbridge.
     * Wiki `id1`, every one of them present in this cache's `npc.rscm`.
     */
    val LEVEL_2_IDS =
        listOf(
            3028, 3029, 3030, 3031, 3032, 3033, 3034, 3035, 3036, 3037, 3038, 3039,
            3040, 3041, 3042, 3043, 3044, 3051, 3052, 3053, 3054,
            5195, 5196, 5197, 5198, 5199, 5200, 5201, 5202, 5203,
        ).map { "npc.goblin_$it" }

    /**
     * The one aggressive goblin in the game outside the God Wars Dungeons. The wiki
     * describes it as "the single armed goblin with a blue mail", aggressive "no matter
     * what level you are", patrolling almost the whole Goblin Cave and yelling at
     * intruders.
     *
     * **Which armed id is the blue-mail one is not published anywhere** - the ids encode
     * the mail colour but no source maps them - so this is a stand-in: the first id of
     * the armed set, reserved for this one npc and used nowhere else. If the real id ever
     * turns up, changing it here and in [LEVEL_2_ARMED_IDS] is the whole fix.
     *
     * It is split out as its own variant purely so the aggro can be declared on it: the
     * combat DSL keys defs by npc id, so an `aggro { }` block on a shared id would make
     * every goblin wearing that mail aggressive everywhere.
     */
    const val CAVE_SENTRY_ID = "npc.goblin_5192"

    /**
     * Level 2, holding a weapon and/or shield, members. Wiki `id2`, less
     * [CAVE_SENTRY_ID].
     */
    val LEVEL_2_ARMED_IDS = listOf(5193, 5204, 5205, 5206, 5207, 5208).map { "npc.goblin_$it" }

    /** Level 5. Wiki `id3`. */
    val LEVEL_5_IDS = listOf(3045, 3073, 3074, 3075, 3076).map { "npc.goblin_$it" }

    /** Level 13, members - "These goblins have grown strong." Wiki `id4`. */
    val LEVEL_13_IDS = listOf("npc.goblin_3046")

    /**
     * Every goblin npc key across all four variants, for code that needs to recognise
     * "is this a goblin" rather than act on one particular variant - currently the Goblin
     * Cave's boxes and crates, which anger a nearby goblin when searched.
     */
    val ALL_IDS: List<String> by lazy { VARIANTS.flatMap { it.npcKeys } }

    val VARIANTS: List<GoblinVariant> =
        listOf(
            GoblinVariant(
                name = "Goblin (level 2)",
                combatLevel = 2,
                npcKeys = LEVEL_2_IDS,
                hitpoints = 5,
                attack = 1,
                strength = 1,
                defence = 1,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                attackBonus = -21,
                strengthBonus = -15,
                defenceStab = -15,
                defenceSlash = -15,
                defenceCrush = -15,
                defenceMagic = -15,
                defenceRanged = -15,
                slayerXp = 5.0,
                dropTable = GoblinDrops.Table.ONE,
            ),
            GoblinVariant(
                name = "Goblin (level 2, armed)",
                combatLevel = 2,
                npcKeys = LEVEL_2_ARMED_IDS,
                hitpoints = 5,
                attack = 3,
                strength = 1,
                defence = 4,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                attackBonus = -21,
                strengthBonus = -15,
                defenceStab = -15,
                defenceSlash = -15,
                defenceCrush = -15,
                defenceMagic = -15,
                defenceRanged = -15,
                slayerXp = 5.0,
                dropTable = GoblinDrops.Table.TWO,
            ),
            GoblinVariant(
                name = "Goblin (level 2, armed, Goblin Cave sentry)",
                combatLevel = 2,
                npcKeys = listOf(CAVE_SENTRY_ID),
                hitpoints = 5,
                attack = 3,
                strength = 1,
                defence = 4,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                attackBonus = -21,
                strengthBonus = -15,
                defenceStab = -15,
                defenceSlash = -15,
                defenceCrush = -15,
                defenceMagic = -15,
                defenceRanged = -15,
                slayerXp = 5.0,
                dropTable = GoblinDrops.Table.TWO,
                // Mechanically an ordinary armed goblin - the only thing that makes it
                // the cave's sentry is that it comes at you. A modest radius on purpose:
                // what the wiki says is huge is its *wander* range, not its aggro range,
                // and NpcAggroPlugin.checkRadius scans a full (2r+1)^2 tile square every
                // few cycles, so a cave-sized radius here would be thousands of chunk
                // lookups per tick for no fidelity gain. The wander range is set by the
                // walkRadius on its spawn instead.
                aggroRadius = 5,
            ),
            GoblinVariant(
                name = "Goblin (level 5)",
                combatLevel = 5,
                npcKeys = LEVEL_5_IDS,
                hitpoints = 12,
                attack = 3,
                strength = 1,
                defence = 4,
                attackSpeed = 6,
                combatStyle = CombatStyle.STAB,
                attackBonus = 12,
                strengthBonus = 12,
                defenceStab = 0,
                defenceSlash = 0,
                defenceCrush = 0,
                defenceMagic = 0,
                defenceRanged = 0,
                slayerXp = 12.0,
                dropTable = GoblinDrops.Table.TWO,
                // The level 5s are the one variant with their own animation set. Every
                // other goblin id in this cache observes {6182, 6183, 6184}; 3045 and
                // 3073-3076 observe {6188, 6189, 6190} - Animation.GOBLIN_STAFF_ATTACK,
                // BANDOSIAN_SPEAR_GOBLIN_DEFEND and BANDOSIAN_SPEAR_GOBLIN_DEATH. They are
                // the armed, spear-carrying model, so they swing and die differently.
                attackAnimation = 6188,
                blockAnimation = 6189,
                deathAnimation = 6190,
            ),
            GoblinVariant(
                name = "Goblin (level 13)",
                combatLevel = 13,
                npcKeys = LEVEL_13_IDS,
                hitpoints = 16,
                attack = 12,
                strength = 13,
                defence = 7,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                attackBonus = 0,
                strengthBonus = 0,
                defenceStab = 4,
                defenceSlash = 6,
                defenceCrush = 8,
                defenceMagic = 4,
                defenceRanged = 4,
                slayerXp = 16.0,
                dropTable = GoblinDrops.Table.ONE,
            ),
        )
}
