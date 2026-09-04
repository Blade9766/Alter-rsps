package org.alter.plugins.content.npcs.dungeon

import org.alter.api.ElementalWeakness
import org.alter.api.Elements
import org.alter.game.model.combat.CombatStyle
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.plugins.content.npcs.slayer.SeedRoll
import org.alter.plugins.content.npcs.slayer.SeedTable

/**
 * The residents of the Ogre Enclave, the Temple of Ikov, Taverley Dungeon and the Yanille
 * Agility Dungeon - the four dungeons where the area packages had already placed giant bats and
 * everything standing next to them was still running on
 * [org.alter.game.model.combat.NpcCombatDef]'s 10-hitpoint defaults.
 *
 * Every number here is the wiki's, read off each monster's `Infobox Monster` for the exact
 * combat level that dungeon lists, and every npc id was checked against this cache's own
 * `npc.rscm` before wiring. Where a page publishes several versions of one level - nine
 * hill giants, twenty ghosts, ten black demons - all of that level's ids are included,
 * since they are mechanically identical and differ only in appearance.
 *
 * **Respawns are the wiki's `respawn` field verbatim.** That field is in game ticks,
 * which are this engine's cycles one-for-one, so nothing is converted. The suit of
 * armour and the baby black dragon are the only two whose field is blank on the wiki;
 * both fall back to 30 ticks, the commonest value across the rest of this file.
 *
 * **This file is stats, not mechanics.** Seven monsters from these dungeons are deliberately
 * absent, because they need behaviour this server does not have and a stat block alone would
 * make them spawn half-working:
 *
 * - **Blue dragon** (111) and **black dragon** (227) - dragonfire. Without it they would be
 *   ordinary melee monsters carrying a boss's stats, and antifire gear would do nothing.
 * - **Cerberus** (318) - a full boss: summoned souls, lava pools, phase behaviour.
 * - **Ogre shaman** (113) and **Salarin the twisted** (70) - magic attackers. This codebase
 *   has no generic NPC magic path at all (see `content/npcs/darkwizard`), so each needs its
 *   own attack loop.
 * - **Fire Warrior of Lesarkus** (84) - damageable only with ice arrows, a per-monster
 *   damage gate that does not exist here.
 * - **Guardian of Armadyl** - a quest NPC with no standalone combat block.
 *
 * The **baby** dragons are here rather than in that list: baby blue and baby black dragons
 * have no dragonfire in OSRS, so they genuinely are melee stat blocks.
 *
 * **The poison scorpion (`Yes (3)`) and poison spider (`Yes (6)`) do poison**, through
 * `poisonDamage` below. That used to be impossible - `NpcCombatDef.poisonChance` existed and the
 * DSL set it, but nothing in the engine read it, so filling it in would have been theatre. It is
 * read now; see [org.alter.plugins.content.mechanics.poison.CombatPoison].
 *
 * **The chaos druid has moved out**, to `content/npcs/chaosdruid`. This file used to carry it and
 * flag its magic as something the engine could not express; that spell is real now, and it needed
 * a package of its own to hold it, a herb table that drops one *or two* herbs, and the three
 * locations of its five that are not dungeons. The **chaos druid warrior** is a separate monster
 * with a separate page and stays here: no magic, no herb table.
 *
 * **Drops are wired in full**, in [DungeonDrops]: every 100% row, every weighted main-table
 * row, and a roll on the shared [org.alter.plugins.content.npcs.GemDropTable] for the eleven
 * monsters that reach it, at each one's own published rate (1/128 to 5/128).
 *
 * **Drop tables are read per drop version, not per combat level**, because the wiki splits
 * them that way and the two do not line up. A level 22 skeleton has one `Unarmed` id with a
 * 23-row table and three `Plain` ids that drop bones and almost nothing else; the same is
 * true at 25 and 45. Wiring one table per level would have handed three quarters of the
 * skeletons in these dungeons a table they never roll. The demons are the mirror image:
 * their id lists span `Regular` and `Chasm of Fire`, and only the `Regular` table is used
 * here - the handful of Chasm ids among them will roll the wrong one until that area exists
 * and can claim its own ids.
 *
 * Animations come from this project's own observed sets per npc id, cross-checked against
 * [org.alter.api.cfg.Animation]'s named constants - the reliable method, since
 * `MonsterAnimationResolver`'s duration heuristic mislabels attack against block for
 * humanoids. Every triple matched a named constant group (`SKELETON_*`, `DEMON_*`,
 * `GIANT_*`, `DWARF_*`, `OGRE_*`, `SCORPION_*`, `MAGIC_AXE_*`, `BABYDRAGON_*`,
 * `HUMAN_SLASH_SWORD_*`, `GHOST` in `named-combat-media.json`) except the hellhound, whose
 * block animation is the remaining member of its observed set once `FOX_ATTACK` and
 * `WOLF_DEATH` are accounted for.
 *
 * The suit of armour is `aggressive = Yes to whoever causes them to animate` - conditional
 * on an animate mechanic that does not exist here - so it is wired passive rather than
 * aggressive to everyone, which is what a plain reading of "Yes" would have produced.
 */
internal data class DungeonMonster(
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
    val respawnCycles: Int,
    val attackAnimation: Int,
    val blockAnimation: Int,
    val deathAnimation: Int,
    /** Aggro radius in tiles, or 0 where the wiki says the monster is not aggressive. */
    val aggroRadius: Int = 0,
    /**
     * The wiki's `poisonous = Yes (N)` value: the damage this monster's poison starts at, or 0 if
     * it cannot poison. The rate is not published for any monster and is left to
     * [org.alter.game.model.combat.NpcCombatDef.DEFAULT_POISON_CHANCE].
     */
    val poisonDamage: Int = 0,
    val elementalWeakness: ElementalWeakness? = null,
    /** rscm item keys dropped on every kill - the wiki's 100% section. */
    val guaranteedDrops: List<String> = emptyList(),
    /** One roll on this table per kill, if non-empty. */
    val table: List<WeightedDrop> = emptyList(),
    /** Chance of also rolling the shared gem drop table, or null if this monster never does. */
    val gemTableChance: Double? = null,
    /**
     * Chance of also rolling the shared herb table, or null if this monster never does.
     *
     * An independent roll, not a row in [table] - which is what the wiki means by "There is a
     * 7/128 chance of rolling the herb drop table". The same shape `content/npcs/slayer` uses.
     */
    val herbTableChance: Double? = null,
    /** The shared seed table this monster reaches, if any. Independent of [table], like the herbs. */
    val seedRoll: SeedRoll? = null,
    /**
     * Drops rolled **independently** of [table], each with its own published chance.
     *
     * This is the distinction the hill giant table used to lose. A tertiary at 1/5000 folded into
     * a weighted table as one row among ninety-odd is not a 1/5000 drop, it is a ~1/94 drop - the
     * giant champion scroll was fifty times too common, the long bone four times, and the ensouled
     * head nearly four times. Tertiaries belong here, not in [table].
     */
    val tertiaryDrops: List<TertiaryDrop> = emptyList(),
)

/**
 * One independently rolled drop.
 *
 * @param wildernessOnly when true, only rolled if the killer is in the Wilderness - the wiki's
 *   "Looting bags are only dropped by those found in the Wilderness".
 */
internal data class TertiaryDrop(
    val item: String,
    val chance: Double,
    val wildernessOnly: Boolean = false,
)

internal object DungeonMonsters {
    /** Wiki: "There is a 7/128 chance of rolling the herb drop table for members." */
    private const val HILL_GIANT_HERB_CHANCE = 7.0 / 128.0

    /**
     * Wiki: "There is an 18/128 chance of rolling the seed drop table for members", through
     * `{{GeneralSeedDropLines|18/128|28|f2p=yes}}` - the general seed table, whose sub-table is
     * chosen by the monster's own combat level, which is why it is rolled as [SeedTable.GENERAL]
     * rather than as a flat list.
     */
    private const val HILL_GIANT_SEED_CHANCE = 18.0 / 128.0

    /**
     * The hill giant tertiaries, shared by the regular and Giants' Plateau entries.
     *
     * The looting bag is Wilderness-only on the wiki, and so is the second giant key roll: the
     * page gives the key 1/128 normally and 2/128 inside the Wilderness. The base 1/128 is a row
     * of [DungeonDrops.HILL_GIANT]; this adds the second 1/128 where the doubling applies, which
     * reproduces the published rate on both sides of the ditch even though the real mechanism is
     * a pre-roll rather than two rolls.
     */
    private val HILL_GIANT_TERTIARIES: List<TertiaryDrop> =
        DungeonDrops.HILL_GIANT_TERTIARY.map { (item, chance) -> TertiaryDrop(item, chance) } +
            listOf(
                TertiaryDrop("item.looting_bag", 1.0 / 5.0, wildernessOnly = true),
                TertiaryDrop("item.giant_key", 1.0 / 128.0, wildernessOnly = true),
            )

    /** Wiki mage = 1 and range = 1 on every monster in this file. */
    const val MAGIC_LEVEL = 1
    const val RANGED_LEVEL = 1

    val ALL: List<DungeonMonster> =
        listOf(
        DungeonMonster(
            name = "Skeleton (level 22, unarmed)",
            combatLevel = 22,
            npcKeys = listOf("npc.skeleton"),
            hitpoints = 29,
            attack = 15,
            strength = 18,
            defence = 17,
            attackSpeed = 4,
            combatStyle = CombatStyle.CRUSH,
            attackBonus = 0,
            strengthBonus = 0,
            defenceStab = 5,
            defenceSlash = 5,
            defenceCrush = -5,
            defenceMagic = 0,
            defenceRanged = 5,
            slayerXp = 29.0,
            respawnCycles = 70,
            attackAnimation = 5485,
            blockAnimation = 5489,
            deathAnimation = 5491,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.EARTH, 35),
            guaranteedDrops = listOf("item.bones"),
            table = DungeonDrops.SKELETON_LEVEL_22_UNARMED,
            tertiaryDrops = DungeonDrops.SKELETON_LEVEL_22_UNARMED_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = 1.0 / 128.0,
        ),
        DungeonMonster(
            name = "Skeleton (level 22)",
            combatLevel = 22,
            npcKeys = listOf("npc.skeleton_71", "npc.skeleton_72", "npc.skeleton_73"),
            hitpoints = 29,
            attack = 15,
            strength = 18,
            defence = 17,
            attackSpeed = 4,
            combatStyle = CombatStyle.CRUSH,
            attackBonus = 0,
            strengthBonus = 0,
            defenceStab = 5,
            defenceSlash = 5,
            defenceCrush = -5,
            defenceMagic = 0,
            defenceRanged = 5,
            slayerXp = 29.0,
            respawnCycles = 70,
            attackAnimation = 5485,
            blockAnimation = 5489,
            deathAnimation = 5491,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.EARTH, 35),
            guaranteedDrops = listOf("item.bones"),
            table = DungeonDrops.SKELETON_LEVEL_22,
            tertiaryDrops = DungeonDrops.SKELETON_LEVEL_22_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = null,
        ),
        DungeonMonster(
            name = "Skeleton (level 25, armed)",
            combatLevel = 25,
            npcKeys = listOf("npc.skeleton_77"),
            hitpoints = 17,
            attack = 24,
            strength = 24,
            defence = 24,
            attackSpeed = 4,
            combatStyle = CombatStyle.SLASH,
            attackBonus = 15,
            strengthBonus = 14,
            defenceStab = 9,
            defenceSlash = 11,
            defenceCrush = -2,
            defenceMagic = 1,
            defenceRanged = 4,
            slayerXp = 17.0,
            respawnCycles = 60,
            attackAnimation = 5485,
            blockAnimation = 5489,
            deathAnimation = 5491,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.EARTH, 35),
            guaranteedDrops = listOf("item.bones"),
            table = DungeonDrops.SKELETON_LEVEL_25_ARMED,
            tertiaryDrops = DungeonDrops.SKELETON_LEVEL_25_ARMED_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = 2.0 / 128.0,
        ),
        DungeonMonster(
            name = "Skeleton (level 25)",
            combatLevel = 25,
            npcKeys = listOf("npc.skeleton_78", "npc.skeleton_79", "npc.skeleton_80", "npc.skeleton_81"),
            hitpoints = 17,
            attack = 24,
            strength = 24,
            defence = 24,
            attackSpeed = 4,
            combatStyle = CombatStyle.SLASH,
            attackBonus = 15,
            strengthBonus = 14,
            defenceStab = 9,
            defenceSlash = 11,
            defenceCrush = -2,
            defenceMagic = 1,
            defenceRanged = 4,
            slayerXp = 17.0,
            respawnCycles = 60,
            attackAnimation = 5485,
            blockAnimation = 5489,
            deathAnimation = 5491,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.EARTH, 35),
            guaranteedDrops = listOf("item.bones"),
            table = DungeonDrops.SKELETON_LEVEL_25,
            tertiaryDrops = DungeonDrops.SKELETON_LEVEL_25_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = null,
        ),
        DungeonMonster(
            name = "Skeleton (level 45, armed)",
            combatLevel = 45,
            npcKeys = listOf("npc.skeleton_82"),
            hitpoints = 59,
            attack = 32,
            strength = 35,
            defence = 36,
            attackSpeed = 4,
            combatStyle = CombatStyle.SLASH,
            attackBonus = 15,
            strengthBonus = 14,
            defenceStab = 9,
            defenceSlash = 11,
            defenceCrush = -2,
            defenceMagic = 1,
            defenceRanged = 4,
            slayerXp = 59.0,
            respawnCycles = 60,
            attackAnimation = 5499,
            blockAnimation = 5501,
            deathAnimation = 5503,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.EARTH, 35),
            guaranteedDrops = listOf("item.bones"),
            table = DungeonDrops.SKELETON_LEVEL_45_ARMED,
            tertiaryDrops = DungeonDrops.SKELETON_LEVEL_45_ARMED_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = 2.0 / 128.0,
        ),
        DungeonMonster(
            name = "Skeleton (level 45)",
            combatLevel = 45,
            npcKeys = listOf("npc.skeleton_83"),
            hitpoints = 59,
            attack = 32,
            strength = 35,
            defence = 36,
            attackSpeed = 4,
            combatStyle = CombatStyle.SLASH,
            attackBonus = 15,
            strengthBonus = 14,
            defenceStab = 9,
            defenceSlash = 11,
            defenceCrush = -2,
            defenceMagic = 1,
            defenceRanged = 4,
            slayerXp = 59.0,
            respawnCycles = 60,
            attackAnimation = 5499,
            blockAnimation = 5501,
            deathAnimation = 5503,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.EARTH, 35),
            guaranteedDrops = listOf("item.bones"),
            table = DungeonDrops.SKELETON_LEVEL_45,
            tertiaryDrops = DungeonDrops.SKELETON_LEVEL_45_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = null,
        ),
        DungeonMonster(
            name = "Ghost",
            combatLevel = 19,
            npcKeys = listOf("npc.ghost", "npc.ghost_86", "npc.ghost_87", "npc.ghost_88", "npc.ghost_89", "npc.ghost_90", "npc.ghost_91", "npc.ghost_92", "npc.ghost_93", "npc.ghost_95", "npc.ghost_97", "npc.ghost_99", "npc.ghost_472", "npc.ghost_473", "npc.ghost_474", "npc.ghost_505", "npc.ghost_506", "npc.ghost_507", "npc.ghost_7263", "npc.ghost_7264"),
            hitpoints = 25,
            attack = 13,
            strength = 13,
            defence = 18,
            attackSpeed = 4,
            combatStyle = CombatStyle.CRUSH,
            attackBonus = 0,
            strengthBonus = 0,
            defenceStab = 5,
            defenceSlash = 5,
            defenceCrush = 5,
            defenceMagic = -5,
            defenceRanged = 5,
            slayerXp = 25.0,
            respawnCycles = 40,
            attackAnimation = 5532,
            blockAnimation = 5533,
            deathAnimation = 5534,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.AIR, 50),
            guaranteedDrops = emptyList(),
            table = DungeonDrops.GHOST,
            gemTableChance = null,
        ),
        DungeonMonster(
            name = "Suit of armour",
            combatLevel = 19,
            npcKeys = listOf("npc.suit_of_armour_5043"),
            hitpoints = 29,
            attack = 16,
            strength = 14,
            defence = 9,
            attackSpeed = 5,
            combatStyle = CombatStyle.SLASH,
            attackBonus = 8,
            strengthBonus = 10,
            defenceStab = 46,
            defenceSlash = 50,
            defenceCrush = 45,
            defenceMagic = -12,
            defenceRanged = 45,
            slayerXp = 0.0,
            respawnCycles = 30,
            attackAnimation = 390,
            blockAnimation = 388,
            deathAnimation = 836,
            aggroRadius = 0,
            guaranteedDrops = emptyList(),
            table = DungeonDrops.SUIT_OF_ARMOUR,
            gemTableChance = null,
        ),
        DungeonMonster(
            name = "Dwarf",
            combatLevel = 10,
            npcKeys = listOf("npc.dwarf_290", "npc.dwarf_296", "npc.dwarf_1405"),
            hitpoints = 16,
            attack = 8,
            strength = 8,
            defence = 6,
            attackSpeed = 5,
            combatStyle = CombatStyle.STAB,
            attackBonus = 5,
            strengthBonus = 7,
            defenceStab = 0,
            defenceSlash = 0,
            defenceCrush = 0,
            defenceMagic = 5,
            defenceRanged = 0,
            slayerXp = 16.0,
            respawnCycles = 25,
            attackAnimation = 99,
            blockAnimation = 100,
            deathAnimation = 102,
            aggroRadius = 0,
            guaranteedDrops = listOf("item.bones"),
            table = DungeonDrops.DWARF,
            gemTableChance = 1.0 / 128.0,
        ),
        DungeonMonster(
            name = "Chaos dwarf",
            combatLevel = 48,
            npcKeys = listOf("npc.chaos_dwarf"),
            hitpoints = 61,
            attack = 38,
            strength = 42,
            defence = 28,
            attackSpeed = 4,
            combatStyle = CombatStyle.CRUSH,
            attackBonus = 13,
            strengthBonus = 9,
            defenceStab = 40,
            defenceSlash = 34,
            defenceCrush = 25,
            defenceMagic = 10,
            defenceRanged = 35,
            slayerXp = 61.0,
            respawnCycles = 14,
            attackAnimation = 99,
            blockAnimation = 100,
            deathAnimation = 102,
            aggroRadius = 4,
            guaranteedDrops = listOf("item.bones"),
            table = DungeonDrops.CHAOS_DWARF,
            tertiaryDrops = DungeonDrops.CHAOS_DWARF_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = 5.0 / 128.0,
        ),
        DungeonMonster(
            name = "Chaos druid warrior",
            combatLevel = 37,
            npcKeys = listOf("npc.chaos_druid_warrior"),
            hitpoints = 40,
            attack = 32,
            strength = 34,
            defence = 25,
            attackSpeed = 5,
            combatStyle = CombatStyle.CRUSH,
            attackBonus = 9,
            strengthBonus = 5,
            defenceStab = 13,
            defenceSlash = 17,
            defenceCrush = 14,
            defenceMagic = -4,
            defenceRanged = 14,
            slayerXp = 40.0,
            respawnCycles = 50,
            attackAnimation = 401,
            blockAnimation = 403,
            deathAnimation = 836,
            aggroRadius = 4,
            guaranteedDrops = listOf("item.bones"),
            table = DungeonDrops.CHAOS_DRUID_WARRIOR,
            tertiaryDrops = DungeonDrops.CHAOS_DRUID_WARRIOR_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = 1.0 / 128.0,
        ),
        DungeonMonster(
            name = "Poison scorpion",
            poisonDamage = 3,
            combatLevel = 20,
            npcKeys = listOf("npc.poison_scorpion"),
            hitpoints = 23,
            attack = 16,
            strength = 17,
            defence = 15,
            attackSpeed = 4,
            combatStyle = CombatStyle.STAB,
            attackBonus = 0,
            strengthBonus = 0,
            defenceStab = 5,
            defenceSlash = 15,
            defenceCrush = 15,
            defenceMagic = 0,
            defenceRanged = 5,
            slayerXp = 23.0,
            respawnCycles = 25,
            attackAnimation = 6254,
            blockAnimation = 6255,
            deathAnimation = 6256,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.FIRE, 25),
            guaranteedDrops = emptyList(),
            table = DungeonDrops.POISON_SCORPION,
            tertiaryDrops = DungeonDrops.POISON_SCORPION_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = null,
        ),
        DungeonMonster(
            name = "Hill giant",
            combatLevel = 28,
            // Every published id whose drops match the regular table: the nine Regular ones, the
            // three Varlamore ones (the page gives them no drop differences), and the Catacombs
            // giant 7261, which was simply missing. The three Giants' Plateau ids are NOT here -
            // they swap four rows of the drop table, so they are their own entry below.
            npcKeys = HillGiantSpawns.REGULAR_IDS + HillGiantSpawns.VARLAMORE_IDS + HillGiantSpawns.KOUREND_IDS,
            hitpoints = 35,
            attack = 18,
            strength = 22,
            defence = 26,
            attackSpeed = 6,
            combatStyle = CombatStyle.CRUSH,
            attackBonus = 18,
            strengthBonus = 16,
            defenceStab = 0,
            defenceSlash = 0,
            defenceCrush = 0,
            defenceMagic = 0,
            defenceRanged = 0,
            slayerXp = 35.0,
            respawnCycles = 30,
            attackAnimation = 4652,
            blockAnimation = 4651,
            deathAnimation = 4653,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.EARTH, 25),
            guaranteedDrops = listOf("item.big_bones"),
            table = DungeonDrops.HILL_GIANT,
            gemTableChance = 3.0 / 128.0,
            herbTableChance = HILL_GIANT_HERB_CHANCE,
            seedRoll = SeedRoll(SeedTable.GENERAL, chance = HILL_GIANT_SEED_CHANCE),
            tertiaryDrops = HILL_GIANT_TERTIARIES,
        ),
        /*
         * The Giants' Plateau hill giants, from the Hill Giant page's fourth infobox. Identical
         * to the entry above in every combat number - the page repeats the same block - and
         * separate only because of the drops: the wiki flags `iron med helm` and `steel scimitar`
         * as "Only dropped by giants on the Giants' Plateau", and `iron full helm` and
         * `steel longsword` as "Only dropped by giants not on the Giants' Plateau".
         */
        DungeonMonster(
            name = "Hill giant (Giants' Plateau)",
            combatLevel = 28,
            npcKeys = HillGiantSpawns.PLATEAU_IDS,
            hitpoints = 35,
            attack = 18,
            strength = 22,
            defence = 26,
            attackSpeed = 6,
            combatStyle = CombatStyle.CRUSH,
            attackBonus = 18,
            strengthBonus = 16,
            defenceStab = 0,
            defenceSlash = 0,
            defenceCrush = 0,
            defenceMagic = 0,
            defenceRanged = 0,
            slayerXp = 35.0,
            respawnCycles = 30,
            attackAnimation = 4652,
            blockAnimation = 4651,
            deathAnimation = 4653,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.EARTH, 25),
            guaranteedDrops = listOf("item.big_bones"),
            table = DungeonDrops.HILL_GIANT_PLATEAU,
            gemTableChance = 3.0 / 128.0,
            herbTableChance = HILL_GIANT_HERB_CHANCE,
            seedRoll = SeedRoll(SeedTable.GENERAL, chance = HILL_GIANT_SEED_CHANCE),
            tertiaryDrops = HILL_GIANT_TERTIARIES,
        ),
        DungeonMonster(
            name = "Magic axe",
            combatLevel = 42,
            npcKeys = listOf("npc.magic_axe"),
            hitpoints = 44,
            attack = 38,
            strength = 38,
            defence = 29,
            attackSpeed = 4,
            combatStyle = CombatStyle.SLASH,
            attackBonus = 0,
            strengthBonus = 0,
            defenceStab = 10,
            defenceSlash = 5,
            defenceCrush = 15,
            defenceMagic = 5,
            defenceRanged = 10,
            slayerXp = 44.0,
            respawnCycles = 30,
            attackAnimation = 185,
            blockAnimation = 186,
            deathAnimation = 188,
            aggroRadius = 4,
            // The normal magic axe has no weighted table at all: the wiki gives it one 100% drop
            // and one tertiary. The five-battleaxe roll belongs to the Catacombs version below.
            guaranteedDrops = listOf("item.iron_battleaxe"),
            gemTableChance = null,
            tertiaryDrops = listOf(TertiaryDrop("item.looting_bag", 1.0 / 3.0, wildernessOnly = true)),
        ),
        /*
         * The Catacombs of Kourend magic axe, npc 7269 - the page's `version2`.
         *
         * Every combat number is identical to the normal axe above; the page publishes one
         * unversioned block for both. What differs is the drops, and they differ completely: no
         * guaranteed iron battleaxe but a five-way battleaxe roll out of 500, no Wilderness
         * looting bag (it is not in the Wilderness), and a medium clue scroll instead.
         *
         * Its **Catacombs tertiary** table is not modelled - that is a shared Kourend drop system
         * this server does not have, the same gap the Catacombs hill giant has.
         */
        DungeonMonster(
            name = "Magic axe (Catacombs of Kourend)",
            combatLevel = 42,
            npcKeys = listOf("npc.magic_axe_7269"),
            hitpoints = 44,
            attack = 38,
            strength = 38,
            defence = 29,
            attackSpeed = 4,
            combatStyle = CombatStyle.SLASH,
            attackBonus = 0,
            strengthBonus = 0,
            defenceStab = 10,
            defenceSlash = 5,
            defenceCrush = 15,
            defenceMagic = 5,
            defenceRanged = 10,
            slayerXp = 44.0,
            respawnCycles = 30,
            attackAnimation = 185,
            blockAnimation = 186,
            deathAnimation = 188,
            aggroRadius = 4,
            table = DungeonDrops.MAGIC_AXE_CATACOMBS,
            gemTableChance = null,
            /*
             * Wiki `{{DropsLineClue|type=medium|rarity=1/256|altrarity=1/128}}`. The base rate is
             * used; the alternate and the 1/64 the page footnotes for a ring of wealth (i) both
             * need a wealth-ring check this server does not have.
             */
            tertiaryDrops = listOf(TertiaryDrop("item.clue_scroll_medium", 1.0 / 256.0)),
        ),
        DungeonMonster(
            name = "Jailer",
            combatLevel = 47,
            npcKeys = listOf("npc.jailer"),
            hitpoints = 47,
            attack = 40,
            strength = 40,
            defence = 40,
            attackSpeed = 4,
            combatStyle = CombatStyle.CRUSH,
            attackBonus = 0,
            strengthBonus = 0,
            defenceStab = 79,
            defenceSlash = 63,
            defenceCrush = 47,
            defenceMagic = 0,
            defenceRanged = 0,
            slayerXp = 0.0,
            respawnCycles = 50,
            attackAnimation = 422,
            blockAnimation = 425,
            deathAnimation = 836,
            aggroRadius = 0,
            guaranteedDrops = listOf("item.bones"),
            table = DungeonDrops.JAILER,
            gemTableChance = null,
        ),
        DungeonMonster(
            name = "Black Knight",
            combatLevel = 33,
            npcKeys = listOf("npc.black_knight_517", "npc.black_knight_4331", "npc.black_knight_11953", "npc.black_knight", "npc.black_knight_11952"),
            hitpoints = 42,
            attack = 25,
            strength = 25,
            defence = 25,
            attackSpeed = 5,
            combatStyle = CombatStyle.SLASH,
            attackBonus = 18,
            strengthBonus = 16,
            defenceStab = 73,
            defenceSlash = 76,
            defenceCrush = 70,
            defenceMagic = -11,
            defenceRanged = 72,
            slayerXp = 42.0,
            respawnCycles = 25,
            attackAnimation = 390,
            blockAnimation = 388,
            deathAnimation = 836,
            aggroRadius = 4,
            guaranteedDrops = listOf("item.bones"),
            table = DungeonDrops.BLACK_KNIGHT,
            tertiaryDrops = DungeonDrops.BLACK_KNIGHT_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = 3.0 / 128.0,
        ),
        DungeonMonster(
            name = "Poison spider",
            poisonDamage = 6,
            combatLevel = 64,
            npcKeys = listOf("npc.poison_spider", "npc.poison_spider_11999"),
            hitpoints = 64,
            attack = 50,
            strength = 58,
            defence = 52,
            attackSpeed = 4,
            combatStyle = CombatStyle.STAB,
            attackBonus = 0,
            strengthBonus = 0,
            defenceStab = 20,
            defenceSlash = 17,
            defenceCrush = 10,
            defenceMagic = 14,
            defenceRanged = 14,
            slayerXp = 64.0,
            respawnCycles = 30,
            attackAnimation = 5327,
            blockAnimation = 5328,
            deathAnimation = 5329,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.FIRE, 40),
            guaranteedDrops = emptyList(),
            table = DungeonDrops.POISON_SPIDER,
            tertiaryDrops = DungeonDrops.POISON_SPIDER_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = null,
        ),
        DungeonMonster(
            name = "Ice spider",
            combatLevel = 61,
            npcKeys = listOf("npc.ice_spider", "npc.ice_spider_10722", "npc.ice_spider_13798"),
            hitpoints = 65,
            attack = 50,
            strength = 55,
            defence = 43,
            attackSpeed = 4,
            combatStyle = CombatStyle.CRUSH,
            attackBonus = 0,
            strengthBonus = 0,
            defenceStab = 20,
            defenceSlash = 17,
            defenceCrush = 12,
            defenceMagic = 13,
            defenceRanged = 13,
            slayerXp = 65.0,
            respawnCycles = 30,
            attackAnimation = 5327,
            blockAnimation = 5328,
            deathAnimation = 5329,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.FIRE, 100),
            guaranteedDrops = emptyList(),
            table = DungeonDrops.ICE_SPIDER,
            tertiaryDrops = DungeonDrops.ICE_SPIDER_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = null,
        ),
        DungeonMonster(
            name = "Black demon",
            combatLevel = 172,
            npcKeys = listOf("npc.black_demon", "npc.black_demon_2048", "npc.black_demon_2049", "npc.black_demon_2050", "npc.black_demon_2051", "npc.black_demon_2052", "npc.black_demon_5874", "npc.black_demon_5875", "npc.black_demon_5876", "npc.black_demon_5877"),
            hitpoints = 157,
            attack = 145,
            strength = 148,
            defence = 152,
            attackSpeed = 4,
            combatStyle = CombatStyle.SLASH,
            attackBonus = 0,
            strengthBonus = 0,
            defenceStab = 0,
            defenceSlash = 0,
            defenceCrush = 0,
            defenceMagic = -10,
            defenceRanged = 0,
            slayerXp = 157.0,
            respawnCycles = 30,
            attackAnimation = 64,
            blockAnimation = 65,
            deathAnimation = 67,
            aggroRadius = 4,
            elementalWeakness = ElementalWeakness(Elements.WATER, 40),
            guaranteedDrops = listOf("item.malicious_ashes"),
            table = DungeonDrops.BLACK_DEMON,
            tertiaryDrops = DungeonDrops.BLACK_DEMON_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = 5.0 / 128.0,
        ),
        DungeonMonster(
            name = "Ogre chieftain",
            combatLevel = 81,
            npcKeys = listOf("npc.ogre_chieftain"),
            hitpoints = 60,
            attack = 75,
            strength = 71,
            defence = 75,
            attackSpeed = 4,
            combatStyle = CombatStyle.CRUSH,
            attackBonus = 5,
            strengthBonus = 7,
            defenceStab = 10,
            defenceSlash = 21,
            defenceCrush = 16,
            defenceMagic = 0,
            defenceRanged = 0,
            slayerXp = 60.0,
            respawnCycles = 300,
            attackAnimation = 359,
            blockAnimation = 360,
            deathAnimation = 361,
            aggroRadius = 0,
            elementalWeakness = ElementalWeakness(Elements.EARTH, 20),
            guaranteedDrops = listOf("item.big_bones"),
            table = DungeonDrops.OGRE_CHIEFTAIN,
            tertiaryDrops = DungeonDrops.OGRE_CHIEFTAIN_TERTIARY.map { (item, chance, wild) -> TertiaryDrop(item, chance, wild) },
            gemTableChance = null,
        ),
        )
}
