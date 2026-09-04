package org.alter.plugins.content.npcs.citizen

import org.alter.game.model.combat.CombatStyle

/**
 * Every attackable **Man** and **Woman** in this cache - the generic townsfolk that fill
 * Lumbridge, Varrock, Falador, Al Kharid, Karamja, Ardougne and Shayzien.
 *
 * See [CitizenPlugin] for the wiring and [CitizenDrops] for the drop table.
 *
 * ## Which ids are here, and why those
 *
 * The cache holds 66 npcs literally named `Man` or `Woman`. Only **38** of them are
 * monsters: the rest are combat level 0 with no `Attack` option - quest and cutscene
 * bodies such as 13872 (a 3x3 "Man" with a single `Check-on` option, lying down), the five
 * Shantay Pass men 4268-4272, and the Kourend slum residents 7919-7922. Those are scenery
 * with legs and are deliberately absent; giving them a combat def would not make them
 * attackable, only lie about them.
 *
 * The 38 that remain split three ways:
 *
 * - **The generic citizen, combat level 2** (30 ids). Identical everywhere: 7 hitpoints,
 *   1 in every combat level, a 4-cycle [CombatStyle.CRUSH] punch for a maximum of 1. The
 *   OSRS Wiki publishes these across four pages - `Man`, `Woman`, `Man (East Ardougne)`
 *   and `Woman (East Ardougne)` - and every one of them agrees on the block above.
 * - **West Ardougne** (8 ids). A different monster wearing the same face: levels 3 to 14,
 *   10 to 23 hitpoints, a 50-cycle respawn, and *positive or zero* defence bonuses rather
 *   than the citizen's -21. The level 12 and 14 women have 10 attack, 10 strength and 10
 *   defence and hit a 2 - they are the only Man or Woman in the game that can hurt you.
 * - **One unused Woman**, 3268. Never released in Old School; see [Citizens.UNUSED_WOMAN_ID].
 *
 * Two ids the wiki lists that this rev-228 cache does not have: **14920** and **14921**,
 * the Port Roberts man and woman added by the Sailing update. Nothing to define.
 *
 * ## What this changes about npcs that already worked
 *
 * These npcs were *not* stat-less before: `data/cfg/npcs/monsterStats.json` already
 * supplies the combat block for most of them, and `World.setNpcDefaults` applies it to
 * anything no plugin describes. What that table cannot carry is a respawn delay, a drop
 * table, or animations - so what a citizen actually gains here is **loot** (they dropped
 * nothing at all, not even bones), the wiki's real respawn timer, and the fix below.
 *
 * ## The animation swap
 *
 * `npc-animations/named-combat-media.json` had the `MAN` and `WOMAN` entries' attack and
 * block the wrong way round - `attack = 425`, `block = 422` - which is
 * [org.alter.api.cfg.Animation.HUMAN_DEFEND_COWARDLY] as the swing and
 * [org.alter.api.cfg.Animation.HUMAN_PUNCH] as the flinch. The two ids are simply the
 * OpenOSRS observation list `[425, 422, 836]` read in order, the same attack/block
 * mislabelling the barbarians hit through
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationResolver]. Both entries are
 * corrected, which also fixes every other npc whose cache name resolves to them (`Old man`
 * and friends), and the right way round is repeated in
 * [Citizens.ATTACK_ANIMATION]/[Citizens.BLOCK_ANIMATION] here because setting a combat def
 * at all takes an npc off that plugin's resolver path. Combat *sounds* still come from the
 * same `MAN`/`WOMAN` entries.
 */
internal data class CitizenVariant(
    /** Wiki page and version this row came from, for the verify test's error messages. */
    val name: String,
    val combatLevel: Int,
    val npcKeys: List<String>,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val defenceStab: Int,
    val defenceSlash: Int,
    val defenceCrush: Int,
    val defenceMagic: Int,
    val defenceRanged: Int,
    /** Wiki `respawn`, in game ticks - this engine's cycles - used as published. */
    val respawnCycles: Int,
    val dropTable: CitizenDrops.Table,
    /**
     * Whether this variant rolls the Wilderness looting bag. Only the `Man` page publishes
     * that row; see [CitizenDrops.rollOnDeath].
     */
    val wildernessLootingBag: Boolean = false,
)

internal object Citizens {
    /** Wiki `attack speed = 4` on every page. */
    const val ATTACK_SPEED = 4

    /**
     * Wiki `attack style = Crush` on every page.
     *
     * Declared through the combat def's `configs { }` block rather than an `onNpcSpawn`
     * hook - `World.setNpcDefaults` copies [org.alter.game.model.combat.NpcCombatDef.combatStyle]
     * onto the npc now, which it did not when the goblin and guard packages were written.
     * Without it these would roll against the player's *stab* defence.
     */
    val COMBAT_STYLE = CombatStyle.CRUSH

    /** Wiki `mage = 1` and `range = 1` everywhere. */
    const val MAGIC_LEVEL = 1
    const val RANGED_LEVEL = 1

    /** Wiki `attbns = 0`, `strbns = 0` everywhere. */
    const val ATTACK_BONUS = 0
    const val STRENGTH_BONUS = 0

    /**
     * [org.alter.api.cfg.Animation.HUMAN_PUNCH], `HUMAN_DEFEND_COWARDLY` and `HUMAN_DEATH`.
     *
     * The unarmed human set, and the cowardly defend rather than the ordinary 424 one:
     * `openosrs-animations.json` observes exactly `[425, 422, 836]` on every citizen id, so
     * 424 is not an animation these npcs ever play. See the class comment for the swap.
     */
    const val ATTACK_ANIMATION = 422
    const val BLOCK_ANIMATION = 425
    const val DEATH_ANIMATION = 836

    /** Wiki `Man` `respawn = 25`. */
    private const val MAN_RESPAWN = 25

    /** Wiki `Woman` `respawn = 24`. The one number the two pages disagree on. */
    private const val WOMAN_RESPAWN = 24

    /** Wiki `Woman (West Ardougne)` and `Man (level 4)` `respawn = 50`. */
    private const val WEST_ARDOUGNE_RESPAWN = 50

    /** Wiki `dstab` through `dheavy` on the level 2 citizen. */
    private const val CITIZEN_DEFENCE = -21

    /**
     * The unused Woman. Released to RuneScape in February 2006 in a Falador mansion, removed
     * in July 2007 when King's Ransom replaced it with the Party Room, and so never present
     * in Old School - but still in this cache, still attackable, still pickpocketable, and
     * reachable from the cheat menu's npc search. It is defined so that spawning it produces
     * a citizen rather than a 10-hitpoint zero-stat punching bag; its examine, "One of
     * Gielinor's many citizens. She looks rich.", is the only thing that marks it out.
     *
     * No drop table was ever published for it, so it drops bones and nothing else.
     */
    const val UNUSED_WOMAN_ID = "npc.woman_3268"

    private fun citizen(
        name: String,
        npcKeys: List<String>,
        respawnCycles: Int,
        dropTable: CitizenDrops.Table,
        defenceBonus: Int = CITIZEN_DEFENCE,
        wildernessLootingBag: Boolean = false,
    ) = CitizenVariant(
        name = name,
        combatLevel = 2,
        npcKeys = npcKeys,
        hitpoints = 7,
        attack = 1,
        strength = 1,
        defence = 1,
        defenceStab = defenceBonus,
        defenceSlash = defenceBonus,
        defenceCrush = defenceBonus,
        defenceMagic = defenceBonus,
        defenceRanged = defenceBonus,
        respawnCycles = respawnCycles,
        dropTable = dropTable,
        wildernessLootingBag = wildernessLootingBag,
    )

    /**
     * Wiki `Man` versions 1-5 and 7-13, every id of which is in this cache except 14920.
     *
     * Within this list the id is purely cosmetic - it picks the colour of the shirt, or the
     * Musa Point explorer's backpack - which is why the town spawn lists deal them out
     * round-robin rather than spawning sixteen copies of `man_3106`.
     */
    private val MAN =
        citizen(
            name = "Man",
            npcKeys =
                listOf(
                    // Versions 1-3: the blue, red and plain shirts that fill every town.
                    "npc.man_3106", "npc.man_6818", "npc.man_6987",
                    "npc.man_3107", "npc.man_6988",
                    "npc.man_3108", "npc.man_6989",
                    // Versions 4-5: the backpack men, Port Sarim and Musa Point.
                    "npc.man_3109", "npc.man_6815", "npc.man_3110",
                    // Versions 7-10: Falador (two), Karamja, Varrock.
                    "npc.man_3264", "npc.man_3265", "npc.man_3652", "npc.man_3014",
                    // Versions 11-12: Shayzien.
                    "npc.man_11057", "npc.man_11058",
                ),
            respawnCycles = MAN_RESPAWN,
            dropTable = CitizenDrops.Table.FULL,
            wildernessLootingBag = true,
        )

    /**
     * Wiki `Man` version 6, Al Kharid - `dstab6` through `dheavy6` are **0**, not -21, and
     * the cache backs that up by giving 3261 no `Talk-to` option where every other man has
     * one. Same drop table as the rest of the page.
     */
    private val AL_KHARID_MAN =
        citizen(
            name = "Man (Al Kharid)",
            npcKeys = listOf("npc.man_3261"),
            respawnCycles = MAN_RESPAWN,
            dropTable = CitizenDrops.Table.FULL,
            defenceBonus = 0,
            wildernessLootingBag = true,
        )

    /** Wiki `Woman` versions 1-6; 14921 is the version this cache does not have. */
    private val WOMAN =
        citizen(
            name = "Woman",
            npcKeys =
                listOf(
                    "npc.woman_3111", "npc.woman_6990", "npc.woman_10728",
                    "npc.woman_3112", "npc.woman_6991",
                    "npc.woman_3113", "npc.woman_6992",
                    // Varrock, then Shayzien.
                    "npc.woman_3015", "npc.woman_11053", "npc.woman_11054",
                ),
            respawnCycles = WOMAN_RESPAWN,
            dropTable = CitizenDrops.Table.FULL,
        )

    /**
     * `Man (East Ardougne)` and `Woman (East Ardougne)`: the same combat block as the pages
     * above, but their own wiki pages, and **neither publishes anything past bones**.
     *
     * That is more likely a documentation gap than a real difference - they are ordinary
     * citizens - but the published table is what is reproduced, the same call the goblin and
     * guard tables make about their unmodelled sections. If Ardougne citizens ever look
     * conspicuously poor next to Varrock's, this comment is why, and the fix is one word:
     * [CitizenDrops.Table.FULL].
     */
    private val EAST_ARDOUGNE =
        listOf(
            citizen(
                name = "Man (East Ardougne)",
                npcKeys = listOf("npc.man_3298"),
                respawnCycles = MAN_RESPAWN,
                dropTable = CitizenDrops.Table.BONES_ONLY,
            ),
            citizen(
                name = "Woman (East Ardougne)",
                npcKeys = listOf("npc.woman_3299"),
                respawnCycles = MAN_RESPAWN,
                dropTable = CitizenDrops.Table.BONES_ONLY,
            ),
        )

    /** See [UNUSED_WOMAN_ID]. */
    private val UNUSED_WOMAN =
        citizen(
            name = "Woman (historical)",
            npcKeys = listOf(UNUSED_WOMAN_ID),
            respawnCycles = WOMAN_RESPAWN,
            dropTable = CitizenDrops.Table.BONES_ONLY,
        )

    private fun westArdougne(
        name: String,
        combatLevel: Int,
        npcKeys: List<String>,
        hitpoints: Int,
        attack: Int,
        strength: Int,
        defence: Int,
        meleeDefenceBonus: Int,
    ) = CitizenVariant(
        name = name,
        combatLevel = combatLevel,
        npcKeys = npcKeys,
        hitpoints = hitpoints,
        attack = attack,
        strength = strength,
        defence = defence,
        defenceStab = meleeDefenceBonus,
        defenceSlash = meleeDefenceBonus,
        defenceCrush = meleeDefenceBonus,
        // `dmagic`, `dlight`, `dstandard` and `dheavy` are 0 on every West Ardougne version.
        defenceMagic = 0,
        defenceRanged = 0,
        respawnCycles = WEST_ARDOUGNE_RESPAWN,
        dropTable = CitizenDrops.Table.BONES_ONLY,
    )

    /**
     * The plague city behind the wall. Wiki `Woman (West Ardougne)` versions 1-7 and
     * `Man (level 4)` version 1; both pages publish bones and nothing else.
     *
     * `Man (level 4)` version 2 - npc **1138** - is left out on purpose. The wiki marks it
     * "Unused" and this cache agrees: it has combat level 0 and an entirely empty option
     * list, so it cannot be attacked whatever stats it is given.
     *
     * No entry exists for any of these ids in `openosrs-animations.json`, so they inherit the
     * unarmed human set the level 2 citizens are observed using. If the level 12 and 14 women
     * turn out to swing a weapon in the real game, that is the line to change.
     */
    private val WEST_ARDOUGNE =
        listOf(
            westArdougne(
                name = "Man (West Ardougne)",
                combatLevel = 4,
                npcKeys = listOf("npc.man_1118"),
                hitpoints = 13,
                attack = 2,
                strength = 1,
                defence = 1,
                meleeDefenceBonus = 1,
            ),
            // Versions 1 and 2 of the level 3 women are stat-for-stat identical; merged.
            // `npc.woman` with no id suffix really is 1119 - it is the one Man or Woman in
            // the whole cache whose rscm key carries no number.
            westArdougne(
                name = "Woman (West Ardougne, level 3)",
                combatLevel = 3,
                npcKeys = listOf("npc.woman", "npc.woman_1131"),
                hitpoints = 10,
                attack = 2,
                strength = 1,
                defence = 1,
                meleeDefenceBonus = 1,
            ),
            westArdougne(
                name = "Woman (West Ardougne, level 3, version 3)",
                combatLevel = 3,
                npcKeys = listOf("npc.woman_1141"),
                hitpoints = 10,
                attack = 1,
                strength = 1,
                defence = 2,
                meleeDefenceBonus = 0,
            ),
            westArdougne(
                name = "Woman (West Ardougne, level 4, version 1)",
                combatLevel = 4,
                npcKeys = listOf("npc.woman_1130"),
                hitpoints = 13,
                attack = 2,
                strength = 1,
                defence = 1,
                meleeDefenceBonus = 1,
            ),
            westArdougne(
                name = "Woman (West Ardougne, level 4, version 2)",
                combatLevel = 4,
                npcKeys = listOf("npc.woman_1139"),
                hitpoints = 13,
                attack = 2,
                strength = 1,
                defence = 1,
                meleeDefenceBonus = 0,
            ),
            westArdougne(
                name = "Woman (West Ardougne, level 12)",
                combatLevel = 12,
                npcKeys = listOf("npc.woman_1140"),
                hitpoints = 13,
                attack = 10,
                strength = 10,
                defence = 10,
                meleeDefenceBonus = 0,
            ),
            westArdougne(
                name = "Woman (West Ardougne, level 14)",
                combatLevel = 14,
                npcKeys = listOf("npc.woman_1142"),
                hitpoints = 23,
                attack = 10,
                strength = 10,
                defence = 10,
                meleeDefenceBonus = 0,
            ),
        )

    val VARIANTS: List<CitizenVariant> =
        listOf(MAN, AL_KHARID_MAN, WOMAN, UNUSED_WOMAN) + EAST_ARDOUGNE + WEST_ARDOUGNE

    /**
     * Every id that carries the level 2 citizen's `Pickpocket` option.
     *
     * Read by the verify test to check `data/cfg/thieving/pickpockets.json` covers all of
     * them - it listed eight, so Varrock, Falador, Al Kharid, Karamja, Ardougne and Shayzien
     * citizens all had a `Pickpocket` menu entry that did nothing at all. The West Ardougne
     * ids are correctly absent: none of them has the option in this cache.
     */
    val PICKPOCKET_IDS: List<String> =
        MAN.npcKeys + AL_KHARID_MAN.npcKeys + WOMAN.npcKeys +
            UNUSED_WOMAN.npcKeys + EAST_ARDOUGNE.flatMap { it.npcKeys }
}
