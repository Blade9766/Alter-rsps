package org.alter.plugins.content.npcs.slayer

import org.alter.api.NpcSpecies
import org.alter.game.model.combat.CombatStyle
import org.alter.plugins.content.npcs.WeightedDrop

/**
 * The sixteen Slayer-only monsters of the Slayer Tower and the Fremennik Slayer Dungeon.
 *
 * These are the roster the Slayer skill was actually built around, and until now not one of them
 * existed on this server - `content/skills/slayer` shipped with the full published assignment tables
 * and only fifteen categories live, every one of them a generic monster (rats, goblins, demons) that
 * happened to already be in the world. This file is what turns the other half of Turael's list and
 * most of Chaeldar's, Nieve's and Duradel's on.
 *
 * Shaped after `content/npcs/dungeon`, deliberately: one immutable data class per monster, a
 * separate [SlayerMonsterDrops] for the tables, and a plugin that is only wiring. Spawns are not
 * here - they belong to the areas, under `content/areas/slayertower/spawns` and
 * `content/areas/fremennikslayerdungeon/spawns`, exactly as the Taverley Dungeon monsters are
 * defined centrally and placed per-area.
 *
 * ## Sourcing
 *
 * Every stat, bonus, max hit, attack speed, aggression flag, immunity, respawn time, Slayer level
 * and Slayer experience value is the monster's own wiki `Infobox Monster`. Npc ids were then checked
 * against this cache by name *and* combat level - all 16 matched, and the mismatches that turned up
 * were informative: ids 120, 121, 122, 123 and 124 carry the right names but combat level 0 and no
 * `Attack` option, so they are display variants and are excluded rather than wired up as monsters
 * that cannot be fought.
 *
 * Animations come from this project's own observed-animation resource cross-checked against
 * `named-combat-media.json`, the method `content/npcs/dungeon` documents. Every one of the sixteen
 * resolved to a named group; the infernal mage is the only one without a group of its own, and its
 * observed set (1162 cast, 415 block, 836 death) is the standard human caster triple.
 *
 * ## Where the published data stops, and what was done about it
 *
 * - **"Magical melee" is modelled as melee.** The banshee, bloodveld, pyrefiend and jelly all use
 *   it: melee range and melee damage, but the accuracy roll goes against the target's *magic*
 *   defence. This engine has no such style - `CombatStyle` is stab/slash/crush/ranged/magic and
 *   magic means casting a spell - so they are wired as crush. They hit for the right damage at the
 *   right range and speed; only the roll they are checked against is wrong.
 * - **Turoth ids 426, 431 and 432 are left out.** The cache holds seven turoth ids at combat 83-89;
 *   the wiki publishes stats for four of them and its own trivia section says the level 86 and 88
 *   versions "exist in the game's data files" unused. The four published ones are here.
 * - **Basilisk id 418** is defined alongside 417 even though the infobox names only 417: the cache
 *   entries are identical in name, size and combat level, which is the same grouping every other
 *   entry in this file makes.
 * - **No protective-equipment mechanics.** Earmuffs against a banshee, a nose peg against an
 *   aberrant spectre and a mirror shield against a basilisk or cockatrice all prevent a stat drain
 *   in the real game, and the drain is not built - so those items currently do nothing and the
 *   monsters simply never drain. That is a missing punishment, not a missing monster. The same goes
 *   for the rockslug's bag of salt and the gargoyle's rock hammer, which are finishing-blow
 *   mechanics tied to the "Slug Salter" and "Gargoyle Smasher" reward unlocks.
 * - **The kurask and turoth weapon requirement *is* built** - see [SlayerImmunity]. It is the
 *   one of these rules that changes whether the monster can be killed at all rather than how
 *   comfortable the kill is.
 */
internal data class SlayerMonster(
    val name: String,
    val combatLevel: Int,
    val npcKeys: List<String>,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val magic: Int = 1,
    val ranged: Int = 1,
    val attackSpeed: Int,
    val combatStyle: CombatStyle,
    val attackBonus: Int = 0,
    val strengthBonus: Int = 0,
    val defenceStab: Int = 0,
    val defenceSlash: Int = 0,
    val defenceCrush: Int = 0,
    val defenceMagic: Int = 0,
    val defenceRanged: Int = 0,
    val slayerLevel: Int,
    val slayerXp: Double,
    /** Respawn delay in game cycles - the wiki's seconds at 0.6s per tick. */
    val respawnCycles: Int,
    val attackAnimation: Int,
    val blockAnimation: Int,
    val deathAnimation: Int,
    val attackSound: Int = -1,
    val blockSound: Int = -1,
    val deathSound: Int = -1,
    /** Aggro radius in tiles, or 0 where the wiki says the monster is not aggressive. */
    val aggroRadius: Int = 0,
    /**
     * The monster's species attributes, which gear keys off - [org.alter.api.NpcSpecies.UNDEAD] is
     * what the salve amulet looks for.
     *
     * Only what the wiki's Undead article actually lists is recorded. The attribute is narrower
     * than it reads: a bloodveld, a nechryael and an abyssal demon are demons, a gargoyle is
     * neither, and the Barrows brothers are *spectral* rather than undead - so a salve amulet does
     * nothing against any of them.
     */
    val species: List<NpcSpecies> = emptyList(),
    /**
     * The wiki's `poisonous = Yes (N)` value: the damage this monster's poison starts at, or 0 if
     * it cannot poison.
     *
     * This was `poisonChance: Double` and read as a chance, which was two mistakes at once - the
     * wiki publishes damage and no rate at all, and nothing in the engine read the field either
     * way. The cave crawler's `1.0` was neither its damage (8) nor a usable chance.
     */
    val poisonDamage: Int = 0,
    val poisonImmune: Boolean = false,
    val venomImmune: Boolean = false,
    /** rscm item keys dropped on every kill - the wiki's 100% section. */
    val guaranteedDrops: List<String> = emptyList(),
    /** One roll on this table per kill, if non-empty. */
    val table: List<WeightedDrop> = emptyList(),
    /**
     * Chance of rolling the shared *primary* rare drop table, or null.
     *
     * Distinct from [gemTableChance] and not a substitute for it: on the wiki a monster carrying
     * `{{RareDropTable|2/128|5/128}}` reaches **both**, at the two different rates, and only the
     * abyssal demon and nechryael do here. Everything else is `{{GemDropTable}}` alone.
     */
    val rareTableChance: Double? = null,
    /** Chance of also rolling the shared gem drop table, or null if this monster never does. */
    val gemTableChance: Double? = null,
    /** Chance of also rolling the shared herb drop table, or null. */
    val herbTableChance: Double? = null,
    /** Chance of a clue scroll, paired with its rscm item key. */
    val clueScroll: Pair<String, Double>? = null,
    /**
     * Independent rolls made alongside the main table rather than as a row in it - the wiki's
     * "pre-roll" section plus the unconditional part of its "tertiary" section. Each is rolled on
     * its own, so a kill can produce one of these *and* a normal drop, which is exactly how an
     * abyssal whip or a leaf-bladed sword actually lands.
     */
    val preRolls: List<Pair<String, Double>> = emptyList(),
    /**
     * Published max hit, set only on the two monsters that actually attack with magic.
     *
     * Every other monster here is melee and the engine derives its max hit from the strength stat
     * through `MeleeCombatFormula`, so storing the wiki's figure for them would be a number nothing
     * reads. The casters are the exception: [SlayerCasterPlugin] rolls damage directly and has
     * nowhere else to get it from.
     */
    val magicMaxHit: Int? = null,
    /** The shared seed table this monster reaches, if any. */
    val seedRoll: SeedRoll? = null,
)

/**
 * A monster's claim on one of the shared seed tables in
 * [org.alter.plugins.content.npcs.SeedDropTable]: which table, how often, and how many times.
 *
 * [rolls] is 1 for everything here but the nechryael, whose page states outright that "when the rare
 * seed table is rolled, it is rolled twice" - so a nechryael that reaches its table gets two seeds,
 * not one.
 */
internal data class SeedRoll(
    val table: SeedTable,
    val chance: Double,
    val rolls: Int = 1,
)

/**
 * Which shared seed table a monster reaches. `GENERAL` is the odd one out - it is six sub-tables
 * chosen by the monster's own combat level, so it is rolled through
 * [org.alter.plugins.content.npcs.SeedDropTable.rollGeneral] rather than as a flat list.
 */
internal enum class SeedTable {
    ALLOTMENT,
    GENERAL,
    RARE,
}

internal object SlayerMonsters {
    /** 0.6 seconds per game cycle - the wiki publishes respawn in seconds. */
    private const val TICKS_PER_SECOND = 5.0 / 3.0

    private fun seconds(value: Int): Int = (value * TICKS_PER_SECOND).toInt()

    /**
     * Every aggressive monster here uses the same radius. The wiki publishes `aggressive = Yes`
     * without a distance for all five of them, and this is the value `content/npcs/dungeon` already
     * settled on for its aggressive monsters.
     */
    private const val AGGRO_RADIUS = 4

    val TOWER: List<SlayerMonster> =
        listOf(
            SlayerMonster(
                name = "Crawling Hand (level 7)",
                species = listOf(NpcSpecies.UNDEAD),
                combatLevel = 7,
                npcKeys = listOf("npc.crawling_hand_450"),
                hitpoints = 15,
                attack = 7,
                strength = 3,
                defence = 3,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                slayerLevel = 5,
                slayerXp = 12.0,
                respawnCycles = seconds(15),
                attackAnimation = 1592,
                blockAnimation = 1591,
                deathAnimation = 1590,
                guaranteedDrops = listOf("item.bones"),
                table = SlayerMonsterDrops.CRAWLING_HAND,
                gemTableChance = 2.0 / 128.0,
                preRolls =
                    listOf(
                        "item.crawling_hand" to 1.0 / 500.0,
                    ),
            ),
            SlayerMonster(
                name = "Crawling Hand (level 8)",
                species = listOf(NpcSpecies.UNDEAD),
                combatLevel = 8,
                npcKeys =
                    listOf(
                        "npc.crawling_hand_448",
                        "npc.crawling_hand_449",
                        "npc.crawling_hand_451",
                        "npc.crawling_hand_452",
                    ),
                hitpoints = 16,
                attack = 8,
                strength = 4,
                defence = 4,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                slayerLevel = 5,
                slayerXp = 16.0,
                respawnCycles = seconds(15),
                attackAnimation = 1592,
                blockAnimation = 1591,
                deathAnimation = 1590,
                guaranteedDrops = listOf("item.bones"),
                table = SlayerMonsterDrops.CRAWLING_HAND,
                gemTableChance = 2.0 / 128.0,
                preRolls =
                    listOf(
                        "item.crawling_hand" to 1.0 / 500.0,
                    ),
            ),
            SlayerMonster(
                name = "Crawling Hand (level 12)",
                species = listOf(NpcSpecies.UNDEAD),
                combatLevel = 12,
                npcKeys =
                    listOf(
                        "npc.crawling_hand_453",
                        "npc.crawling_hand_454",
                        "npc.crawling_hand_456",
                        "npc.crawling_hand_457",
                    ),
                hitpoints = 19,
                attack = 11,
                strength = 7,
                defence = 7,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                slayerLevel = 5,
                slayerXp = 19.0,
                respawnCycles = seconds(15),
                attackAnimation = 1592,
                blockAnimation = 1591,
                deathAnimation = 1590,
                guaranteedDrops = listOf("item.bones"),
                table = SlayerMonsterDrops.CRAWLING_HAND,
                gemTableChance = 2.0 / 128.0,
                preRolls =
                    listOf(
                        "item.crawling_hand" to 1.0 / 500.0,
                    ),
            ),
            SlayerMonster(
                name = "Banshee",
                species = listOf(NpcSpecies.UNDEAD),
                combatLevel = 23,
                npcKeys = listOf("npc.banshee_414"),
                hitpoints = 22,
                attack = 22,
                strength = 15,
                defence = 22,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                defenceStab = 5,
                defenceSlash = 5,
                defenceCrush = 5,
                slayerLevel = 15,
                slayerXp = 22.0,
                respawnCycles = seconds(15),
                attackAnimation = 1525,
                blockAnimation = 1523,
                deathAnimation = 1524,
                attackSound = 282,
                blockSound = 286,
                deathSound = 285,
                aggroRadius = AGGRO_RADIUS,
                table = SlayerMonsterDrops.BANSHEE,
                gemTableChance = 2.0 / 128.0,
                herbTableChance = 34.0 / 128.0,
                clueScroll = "item.clue_scroll_easy" to 1.0 / 128.0,
                preRolls =
                    listOf(
                        "item.mystic_gloves_dark" to 1.0 / 512.0,
                    ),
            ),
            SlayerMonster(
                name = "Infernal Mage",
                combatLevel = 66,
                npcKeys =
                    listOf(
                        "npc.infernal_mage_443",
                        "npc.infernal_mage_444",
                        "npc.infernal_mage_445",
                        "npc.infernal_mage_446",
                        "npc.infernal_mage_447",
                    ),
                hitpoints = 60,
                attack = 1,
                strength = 1,
                defence = 60,
                magic = 75,
                attackSpeed = 4,
                combatStyle = CombatStyle.MAGIC,
                defenceMagic = 40,
                slayerLevel = 45,
                slayerXp = 60.0,
                respawnCycles = seconds(15),
                attackAnimation = 1162,
                blockAnimation = 415,
                deathAnimation = 836,
                guaranteedDrops = listOf("item.bones"),
                table = SlayerMonsterDrops.INFERNAL_MAGE,
                preRolls =
                    listOf(
                        "item.lava_battlestaff" to 1.0 / 1000.0,
                        "item.mystic_boots_dark" to 1.0 / 512.0,
                        "item.mystic_hat_dark" to 1.0 / 512.0,
                    ),
                magicMaxHit = 8,
            ),
            SlayerMonster(
                name = "Bloodveld",
                combatLevel = 76,
                npcKeys =
                    listOf(
                        "npc.bloodveld_484",
                        "npc.bloodveld_485",
                        "npc.bloodveld_486",
                        "npc.bloodveld_487",
                    ),
                hitpoints = 120,
                attack = 75,
                strength = 45,
                defence = 30,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                slayerLevel = 50,
                slayerXp = 120.0,
                respawnCycles = seconds(21),
                attackAnimation = 1552,
                blockAnimation = 1550,
                deathAnimation = 1553,
                attackSound = 312,
                blockSound = 314,
                deathSound = 313,
                guaranteedDrops = listOf("item.vile_ashes"),
                table = SlayerMonsterDrops.BLOODVELD,
                gemTableChance = 1.0 / 32.0,
                herbTableChance = 1.0 / 128.0,
                clueScroll = "item.clue_scroll_hard" to 1.0 / 256.0,
            ),
            SlayerMonster(
                name = "Aberrant spectre",
                species = listOf(NpcSpecies.UNDEAD),
                combatLevel = 96,
                npcKeys =
                    listOf(
                        "npc.aberrant_spectre_2",
                        "npc.aberrant_spectre_3",
                        "npc.aberrant_spectre_4",
                        "npc.aberrant_spectre_5",
                        "npc.aberrant_spectre_6",
                        "npc.aberrant_spectre_7",
                    ),
                hitpoints = 90,
                attack = 1,
                strength = 1,
                defence = 90,
                magic = 105,
                attackSpeed = 4,
                combatStyle = CombatStyle.MAGIC,
                defenceStab = 20,
                defenceSlash = 20,
                defenceCrush = 20,
                defenceRanged = -15,
                slayerLevel = 60,
                slayerXp = 90.0,
                respawnCycles = seconds(15),
                attackAnimation = 1507,
                blockAnimation = 1509,
                deathAnimation = 1508,
                attackSound = 272,
                blockSound = 275,
                deathSound = 274,
                aggroRadius = AGGRO_RADIUS,
                table = SlayerMonsterDrops.ABERRANT_SPECTRE,
                gemTableChance = 5.0 / 128.0,
                herbTableChance = 78.0 / 128.0,
                clueScroll = "item.clue_scroll_hard" to 1.0 / 128.0,
                preRolls =
                    listOf(
                        "item.mystic_robe_bottom_dark" to 1.0 / 512.0,
                    ),
                magicMaxHit = 8,
                seedRoll = SeedRoll(SeedTable.RARE, 19.0 / 128.0),
            ),
            SlayerMonster(
                name = "Gargoyle",
                combatLevel = 111,
                npcKeys = listOf("npc.gargoyle_412", "npc.gargoyle_413"),
                hitpoints = 105,
                attack = 75,
                strength = 105,
                defence = 107,
                attackSpeed = 4,
                combatStyle = CombatStyle.SLASH,
                defenceStab = 50,
                defenceSlash = 60,
                defenceCrush = -20,
                defenceMagic = 20,
                defenceRanged = 20,
                slayerLevel = 75,
                slayerXp = 105.0,
                respawnCycles = seconds(15),
                attackAnimation = 1517,
                blockAnimation = 1519,
                deathAnimation = 1520,
                attackSound = 428,
                blockSound = 430,
                deathSound = 429,
                aggroRadius = AGGRO_RADIUS,
                table = SlayerMonsterDrops.GARGOYLE,
                gemTableChance = 5.0 / 128.0,
                clueScroll = "item.clue_scroll_hard" to 1.0 / 128.0,
                preRolls =
                    listOf(
                        "item.granite_maul" to 1.0 / 256.0,
                        "item.mystic_robe_top_dark" to 1.0 / 512.0,
                    ),
            ),
            SlayerMonster(
                name = "Nechryael",
                combatLevel = 115,
                npcKeys = listOf("npc.nechryael_8"),
                hitpoints = 105,
                attack = 97,
                strength = 97,
                defence = 105,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                defenceStab = 20,
                defenceSlash = 20,
                defenceCrush = 20,
                defenceRanged = 20,
                slayerLevel = 80,
                slayerXp = 105.0,
                respawnCycles = seconds(15),
                attackAnimation = 1528,
                blockAnimation = 1531,
                deathAnimation = 1530,
                guaranteedDrops = listOf("item.malicious_ashes"),
                table = SlayerMonsterDrops.NECHRYAEL,
                clueScroll = "item.clue_scroll_hard" to 1.0 / 128.0,
                seedRoll = SeedRoll(SeedTable.RARE, 18.0 / 116.0, rolls = 2),
                rareTableChance = 1.0 / 116.0,
                gemTableChance = 5.0 / 116.0,
            ),
            SlayerMonster(
                name = "Abyssal demon",
                combatLevel = 124,
                npcKeys = listOf("npc.abyssal_demon_415", "npc.abyssal_demon_416"),
                hitpoints = 150,
                attack = 97,
                strength = 67,
                defence = 135,
                attackSpeed = 4,
                combatStyle = CombatStyle.STAB,
                defenceStab = 20,
                defenceSlash = 20,
                defenceCrush = 20,
                defenceRanged = 20,
                slayerLevel = 85,
                slayerXp = 150.0,
                respawnCycles = seconds(12),
                attackAnimation = 1537,
                blockAnimation = 2309,
                deathAnimation = 1538,
                guaranteedDrops = listOf("item.abyssal_ashes"),
                table = SlayerMonsterDrops.ABYSSAL_DEMON,
                gemTableChance = 5.0 / 128.0,
                herbTableChance = 19.0 / 128.0,
                clueScroll = "item.clue_scroll_hard" to 1.0 / 128.0,
                preRolls =
                    listOf(
                        "item.abyssal_whip" to 1.0 / 512.0,
                        "item.abyssal_dagger" to 1.0 / 32000.0,
                    ),
                rareTableChance = 2.0 / 128.0,
            ),
        )

    val FREMENNIK: List<SlayerMonster> =
        listOf(
            SlayerMonster(
                name = "Cave crawler",
                combatLevel = 23,
                npcKeys =
                    listOf(
                        "npc.cave_crawler_406",
                        "npc.cave_crawler_407",
                        "npc.cave_crawler_408",
                        "npc.cave_crawler_409",
                    ),
                hitpoints = 22,
                attack = 22,
                strength = 18,
                defence = 18,
                attackSpeed = 4,
                combatStyle = CombatStyle.STAB,
                defenceStab = 10,
                defenceSlash = 10,
                defenceCrush = 5,
                defenceMagic = 5,
                defenceRanged = 10,
                slayerLevel = 10,
                slayerXp = 22.0,
                respawnCycles = seconds(15),
                attackAnimation = 227,
                blockAnimation = 1504,
                deathAnimation = 228,
                attackSound = 341,
                blockSound = 343,
                deathSound = 342,
                aggroRadius = AGGRO_RADIUS,
                poisonDamage = 8,
                table = SlayerMonsterDrops.CAVE_CRAWLER,
                gemTableChance = 1.0 / 128.0,
                herbTableChance = 22.0 / 128.0,
                seedRoll = SeedRoll(SeedTable.ALLOTMENT, 26.0 / 128.0),
            ),
            SlayerMonster(
                name = "Rockslug",
                combatLevel = 29,
                npcKeys = listOf("npc.rockslug_421", "npc.rockslug_422"),
                hitpoints = 27,
                attack = 22,
                strength = 27,
                defence = 27,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                slayerLevel = 20,
                slayerXp = 27.0,
                respawnCycles = seconds(15),
                attackAnimation = 1567,
                blockAnimation = 1565,
                deathAnimation = 1568,
                attackSound = 729,
                blockSound = 731,
                deathSound = 730,
                table = SlayerMonsterDrops.ROCKSLUG,
                gemTableChance = 6.0 / 128.0,
                preRolls =
                    listOf(
                        "item.mystic_gloves_light" to 1.0 / 512.0,
                    ),
                seedRoll = SeedRoll(SeedTable.GENERAL, 9.0 / 128.0),
            ),
            SlayerMonster(
                name = "Cockatrice",
                combatLevel = 37,
                npcKeys = listOf("npc.cockatrice_419"),
                hitpoints = 37,
                attack = 22,
                strength = 37,
                defence = 37,
                attackSpeed = 4,
                combatStyle = CombatStyle.STAB,
                defenceStab = 10,
                defenceSlash = 10,
                defenceMagic = 10,
                slayerLevel = 25,
                slayerXp = 37.0,
                respawnCycles = seconds(15),
                attackAnimation = 1562,
                blockAnimation = 1560,
                deathAnimation = 1563,
                attackSound = 363,
                blockSound = 365,
                deathSound = 364,
                guaranteedDrops = listOf("item.bones"),
                table = SlayerMonsterDrops.COCKATRICE,
                gemTableChance = 2.0 / 128.0,
                herbTableChance = 10.0 / 128.0,
                clueScroll = "item.clue_scroll_medium" to 1.0 / 128.0,
                preRolls =
                    listOf(
                        "item.mystic_boots_light" to 1.0 / 512.0,
                        "item.cockatrice_head" to 1.0 / 1000.0,
                    ),
                seedRoll = SeedRoll(SeedTable.GENERAL, 18.0 / 128.0),
            ),
            SlayerMonster(
                name = "Pyrefiend",
                combatLevel = 43,
                npcKeys =
                    listOf(
                        "npc.pyrefiend_433",
                        "npc.pyrefiend_434",
                        "npc.pyrefiend_435",
                        "npc.pyrefiend_436",
                    ),
                hitpoints = 45,
                attack = 52,
                strength = 30,
                defence = 22,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                defenceStab = 10,
                defenceSlash = 10,
                defenceCrush = 10,
                defenceRanged = 10,
                slayerLevel = 30,
                slayerXp = 45.0,
                respawnCycles = seconds(15),
                attackAnimation = 1582,
                blockAnimation = 1581,
                deathAnimation = 1580,
                guaranteedDrops = listOf("item.fiendish_ashes"),
                table = SlayerMonsterDrops.PYREFIEND,
                gemTableChance = 3.0 / 128.0,
                clueScroll = "item.clue_scroll_medium" to 1.0 / 128.0,
            ),
            SlayerMonster(
                name = "Basilisk",
                combatLevel = 61,
                npcKeys = listOf("npc.basilisk_417", "npc.basilisk_418"),
                hitpoints = 75,
                attack = 30,
                strength = 45,
                defence = 75,
                attackSpeed = 4,
                combatStyle = CombatStyle.SLASH,
                defenceStab = 20,
                defenceSlash = 20,
                defenceMagic = 20,
                slayerLevel = 40,
                slayerXp = 75.0,
                respawnCycles = seconds(15),
                attackAnimation = 1546,
                blockAnimation = 1547,
                deathAnimation = 1548,
                attackSound = 287,
                blockSound = 290,
                deathSound = 289,
                guaranteedDrops = listOf("item.bones"),
                table = SlayerMonsterDrops.BASILISK,
                gemTableChance = 5.0 / 128.0,
                herbTableChance = 35.0 / 128.0,
                preRolls =
                    listOf(
                        "item.mystic_hat_light" to 1.0 / 512.0,
                        "item.basilisk_head" to 1.0 / 2000.0,
                    ),
            ),
            SlayerMonster(
                name = "Jelly",
                combatLevel = 78,
                npcKeys =
                    listOf(
                        "npc.jelly_437",
                        "npc.jelly_438",
                        "npc.jelly_439",
                        "npc.jelly_440",
                        "npc.jelly_441",
                        "npc.jelly_442",
                    ),
                hitpoints = 75,
                attack = 45,
                strength = 45,
                defence = 120,
                magic = 45,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                slayerLevel = 52,
                slayerXp = 75.0,
                respawnCycles = seconds(15),
                attackAnimation = 1586,
                blockAnimation = 1585,
                deathAnimation = 1587,
                attackSound = 547,
                blockSound = 550,
                deathSound = 549,
                table = SlayerMonsterDrops.JELLY,
                gemTableChance = 4.0 / 128.0,
                clueScroll = "item.clue_scroll_hard" to 1.0 / 128.0,
            ),
            SlayerMonster(
                name = "Turoth (level 83)",
                combatLevel = 83,
                npcKeys = listOf("npc.turoth_430"),
                hitpoints = 76,
                attack = 53,
                strength = 83,
                defence = 83,
                attackSpeed = 4,
                combatStyle = CombatStyle.STAB,
                defenceSlash = 20,
                defenceCrush = 20,
                slayerLevel = 55,
                slayerXp = 76.0,
                respawnCycles = seconds(15),
                attackAnimation = 1595,
                blockAnimation = 1596,
                deathAnimation = 1597,
                attackSound = 873,
                blockSound = 875,
                deathSound = 874,
                guaranteedDrops = listOf("item.bones"),
                table = SlayerMonsterDrops.TUROTH,
                gemTableChance = 5.0 / 128.0,
                herbTableChance = 31.0 / 128.0,
                clueScroll = "item.clue_scroll_hard" to 1.0 / 128.0,
                preRolls =
                    listOf(
                        "item.leafbladed_sword" to 1.0 / 500.0,
                        "item.mystic_robe_bottom_light" to 1.0 / 512.0,
                    ),
                seedRoll = SeedRoll(SeedTable.RARE, 18.0 / 128.0),
            ),
            SlayerMonster(
                name = "Turoth (level 85)",
                combatLevel = 85,
                npcKeys = listOf("npc.turoth_429"),
                hitpoints = 77,
                attack = 54,
                strength = 84,
                defence = 84,
                attackSpeed = 4,
                combatStyle = CombatStyle.STAB,
                defenceSlash = 20,
                defenceCrush = 20,
                slayerLevel = 55,
                slayerXp = 77.0,
                respawnCycles = seconds(15),
                attackAnimation = 1595,
                blockAnimation = 1596,
                deathAnimation = 1597,
                attackSound = 873,
                blockSound = 875,
                deathSound = 874,
                guaranteedDrops = listOf("item.bones"),
                table = SlayerMonsterDrops.TUROTH,
                gemTableChance = 5.0 / 128.0,
                herbTableChance = 31.0 / 128.0,
                clueScroll = "item.clue_scroll_hard" to 1.0 / 128.0,
                preRolls =
                    listOf(
                        "item.leafbladed_sword" to 1.0 / 500.0,
                        "item.mystic_robe_bottom_light" to 1.0 / 512.0,
                    ),
                seedRoll = SeedRoll(SeedTable.RARE, 18.0 / 128.0),
            ),
            SlayerMonster(
                name = "Turoth (level 87)",
                combatLevel = 87,
                npcKeys = listOf("npc.turoth_428"),
                hitpoints = 79,
                attack = 56,
                strength = 86,
                defence = 86,
                attackSpeed = 4,
                combatStyle = CombatStyle.STAB,
                defenceSlash = 20,
                defenceCrush = 20,
                slayerLevel = 55,
                slayerXp = 79.0,
                respawnCycles = seconds(15),
                attackAnimation = 1595,
                blockAnimation = 1596,
                deathAnimation = 1597,
                attackSound = 873,
                blockSound = 875,
                deathSound = 874,
                guaranteedDrops = listOf("item.bones"),
                table = SlayerMonsterDrops.TUROTH,
                gemTableChance = 5.0 / 128.0,
                herbTableChance = 31.0 / 128.0,
                clueScroll = "item.clue_scroll_hard" to 1.0 / 128.0,
                preRolls =
                    listOf(
                        "item.leafbladed_sword" to 1.0 / 500.0,
                        "item.mystic_robe_bottom_light" to 1.0 / 512.0,
                    ),
                seedRoll = SeedRoll(SeedTable.RARE, 18.0 / 128.0),
            ),
            SlayerMonster(
                name = "Turoth (level 89)",
                combatLevel = 89,
                npcKeys = listOf("npc.turoth_427"),
                hitpoints = 81,
                attack = 58,
                strength = 88,
                defence = 88,
                attackSpeed = 4,
                combatStyle = CombatStyle.STAB,
                defenceSlash = 20,
                defenceCrush = 20,
                slayerLevel = 55,
                slayerXp = 81.0,
                respawnCycles = seconds(15),
                attackAnimation = 1595,
                blockAnimation = 1596,
                deathAnimation = 1597,
                attackSound = 873,
                blockSound = 875,
                deathSound = 874,
                guaranteedDrops = listOf("item.bones"),
                table = SlayerMonsterDrops.TUROTH,
                gemTableChance = 5.0 / 128.0,
                herbTableChance = 31.0 / 128.0,
                clueScroll = "item.clue_scroll_hard" to 1.0 / 128.0,
                preRolls =
                    listOf(
                        "item.leafbladed_sword" to 1.0 / 500.0,
                        "item.mystic_robe_bottom_light" to 1.0 / 512.0,
                    ),
                seedRoll = SeedRoll(SeedTable.RARE, 18.0 / 128.0),
            ),
            SlayerMonster(
                name = "Kurask",
                combatLevel = 106,
                npcKeys = listOf("npc.kurask_410", "npc.kurask_411"),
                hitpoints = 97,
                attack = 67,
                strength = 105,
                defence = 105,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                defenceSlash = 20,
                defenceCrush = 20,
                slayerLevel = 70,
                slayerXp = 97.0,
                respawnCycles = seconds(15),
                attackAnimation = 1512,
                blockAnimation = 1514,
                deathAnimation = 1513,
                attackSound = 588,
                blockSound = 590,
                deathSound = 589,
                aggroRadius = AGGRO_RADIUS,
                poisonImmune = true,
                venomImmune = true,
                guaranteedDrops = listOf("item.bones"),
                table = SlayerMonsterDrops.KURASK,
                gemTableChance = 6.0 / 124.0,
                herbTableChance = 18.0 / 124.0,
                clueScroll = "item.clue_scroll_hard" to 1.0 / 128.0,
                preRolls =
                    listOf(
                        "item.leafbladed_sword" to 1.0 / 384.0,
                        "item.mystic_robe_top_light" to 1.0 / 512.0,
                        "item.leafbladed_battleaxe" to 1.0 / 1026.0,
                    ),
                seedRoll = SeedRoll(SeedTable.RARE, 15.0 / 124.0),
            ),
        )

    val ALL: List<SlayerMonster> = TOWER + FREMENNIK
}
