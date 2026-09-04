package org.alter.plugins.content.npcs.critters

import org.alter.api.ElementalWeakness
import org.alter.api.Elements
import org.alter.game.model.combat.CombatStyle
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * Chickens, rats and spiders - the three smallest attackable monsters in the game, and
 * until now three of the many that fought with [org.alter.game.model.combat.NpcCombatDef]'s
 * defaults: 10 hitpoints and zero in every stat, regardless of what the wiki says.
 *
 * They share a package because they are the same *kind* of thing - one- or two-hitpoint
 * critters with deeply negative bonuses that exist to be killed at level 1 - and because
 * the Goblin Cave's boxes and crates spawn all three from one roll (see
 * `areas/goblincave/objs/SearchBoxesPlugin`). They do not share stats, a drop table, or
 * even a combat style, so each keeps its own [CritterVariant].
 *
 * **Every version the wiki publishes is defined, not just the ones currently spawned.**
 * Same reasoning as `content/npcs/goblin`: a version left out silently inherits the
 * 10-hitpoint default the day someone spawns it.
 *
 * Three things here are easy to get wrong and worth stating outright:
 *
 * 1. **`npc.spider` (2478) is the Stronghold of Security spider, combat level 24** - not
 *    the common level 1 spider, which is 3019. It has 22 hitpoints, 21s across the board,
 *    a max hit of 6, positive bonuses where the level 1 spiders have -35/-58, and it is
 *    the only **aggressive** monster in this file. The bare, unsuffixed rscm name being
 *    the dangerous one is a genuine trap.
 * 2. **Chickens are immune to poison and venom** (`poisonresistance = 100`,
 *    `venomresistance = 100` on the wiki). Rats and spiders are not.
 * 3. **Regular rats drop nothing at all** - no bones. Only the Stronghold of Security
 *    rats drop bones. That is a published difference between the two versions, not an
 *    oversight here.
 *
 * Animations are the project's own observed sets for these exact npc ids, from
 * `npc-animations/openosrs-animations.json`, cross-checked against
 * `named-combat-media.json`: chicken 1173 observes [5388, 5387, 5389] against that file's
 * `CHICKEN` entry, and rat 2854 observes [2706, 2705, 2707] against its `RAT` entry, both
 * an exact match. The spiders have **no** named entry - `SPIDER` is absent and the
 * `GIANT_SPIDER`/`SMALL_CRYPT_SPIDER` keys do not match "Spider" through
 * `MonsterAnimationsPlugin`'s prefix/suffix fallback - but all three spider ids observe
 * the same [6249, 6250, 6251] set, which is exactly `SMALL_CRYPT_SPIDER`'s
 * attack/block/death. They have to be restated here regardless, because setting a combat
 * def at all takes an npc off that plugin's resolver path; it still fills in the sounds.
 */
internal data class CritterVariant(
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
    /**
     * The defence bonus for all five slots. Every chicken, rat and spider gets one number
     * from the wiki; monsters whose slots differ override the ones that do, below.
     */
    val defenceBonus: Int,
    val slayerXp: Double,
    val respawnCycles: Int,
    val attackAnimation: Int,
    val blockAnimation: Int,
    val deathAnimation: Int,
    val drops: CritterDrops,
    /** Aggro radius in tiles, or 0 for a passive critter - all but the Stronghold spider. */
    val aggroRadius: Int = 0,
    val poisonImmune: Boolean = false,
    val venomImmune: Boolean = false,
    /** The wiki's `elementalweaknesstype`/`percent`, or null where the page omits them. */
    val elementalWeakness: ElementalWeakness? = null,
    // Per-slot defence overrides, for the monsters whose five slots are not all the same
    // number. Only the giant bat needs them so far - it is 10 stab, 10 slash, 12 crush,
    // 10 magic, 8 ranged - so the rest fall back to [defenceBonus].
    val defenceStab: Int = defenceBonus,
    val defenceSlash: Int = defenceBonus,
    val defenceCrush: Int = defenceBonus,
    val defenceMagic: Int = defenceBonus,
    val defenceRanged: Int = defenceBonus,
)

/**
 * What a critter leaves behind. Deliberately not a single weighted table like the goblins'
 * - these three publish almost nothing, and what they do publish is mostly guaranteed
 * rather than rolled.
 *
 * **Not modelled**, flagged rather than faked:
 * - **Key (medium)** on the chicken and **Rat's tail** / **Rat bone** on the rats. All
 *   three are conditional on content that does not exist here - a medium clue step, and
 *   the Witch's Potion and Rag and Bone Man II quests - so they would have to drop
 *   unconditionally or never. They drop never.
 * - The spider's **Wilderness Slayer tertiary table**, which needs a Wilderness Slayer
 *   system this server does not have.
 */
internal data class CritterDrops(
    /** Dropped on every kill, one each. */
    val always: List<String> = emptyList(),
    /** One weighted roll, if non-empty. */
    val table: List<WeightedDrop> = emptyList(),
    /** Independent tertiary roll, or 0 for none. */
    val beginnerClueChance: Double = 0.0,
    /**
     * Chance a Wilderness kill also drops a looting bag, or 0 for none. Per-variant rather
     * than shared: the wiki gives most of these 1/15 but the giant bat 1/5.
     */
    val wildernessLootingBagChance: Double = 0.0,
)

internal object Critters {
    /** Wiki `mage = 1` and `range = 1` on all three pages. */
    const val MAGIC_LEVEL = 1
    const val RANGED_LEVEL = 1

    /** The wiki's usual Wilderness-only looting bag rate. The giant bat is the exception. */
    const val LOOTING_BAG_CHANCE = 1.0 / 15.0

    /**
     * Chickens: one unversioned stat block covering all four versions (Normal,
     * Miscellania, Falador Farm, Gordon and Mary's Farm), so all twelve ids are
     * mechanically identical and differ only in appearance - the same shape
     * `content/npcs/guard` handles for Varrock, Edgeville and Ardougne.
     *
     * Which of them stands where is [ChickenSpawns]' business, and it places eleven of the
     * twelve: 9488 is left out because the wiki calls it combat level 1 with the other two
     * normal chickens while this cache calls it level 3. It is defined here regardless, on
     * the same terms as the goblin and critter versions that have no spawn yet - so that
     * whoever does place it gets these stats rather than the 10-hitpoint default.
     */
    val CHICKEN_IDS =
        listOf(1173, 1174, 9488, 3661, 3662, 2804, 2805, 2806, 10494, 10495, 10496, 10497)
            .map { "npc.chicken_$it" }

    val RAT_IDS = listOf(2854, 2855).map { "npc.rat_$it" }
    val STRONGHOLD_RAT_IDS = listOf(2492, 2513).map { "npc.rat_$it" }

    /**
     * The common level 1 spider and its Underground Pass counterpart. The wiki lists them
     * as separate versions but gives them identical levels, bonuses, respawn and slayer
     * xp; the only published difference is a max hit of 0 against 1, which this server
     * derives from the stats through `MeleeCombatFormula` rather than storing. So they
     * are one variant here.
     */
    val SPIDER_IDS = listOf("npc.spider_3019", "npc.spider_4561")

    /** The level 24 Stronghold of Security spider. Note the bare rscm name - see [Critters]. */
    val STRONGHOLD_SPIDER_IDS = listOf("npc.spider")

    val VARIANTS: List<CritterVariant> =
        listOf(
            CritterVariant(
                name = "Chicken",
                combatLevel = 1,
                npcKeys = CHICKEN_IDS,
                hitpoints = 3,
                attack = 1,
                strength = 1,
                defence = 1,
                attackSpeed = 4,
                combatStyle = CombatStyle.STAB,
                attackBonus = -47,
                strengthBonus = -42,
                defenceBonus = -42,
                slayerXp = 3.0,
                // Wiki respawn = 25, in game ticks - used as published.
                respawnCycles = 25,
                attackAnimation = 5387,
                blockAnimation = 5388,
                deathAnimation = 5389,
                poisonImmune = true,
                venomImmune = true,
                drops =
                    CritterDrops(
                        always = listOf("item.bones", "item.raw_chicken"),
                        table =
                            listOf(
                                WeightedDrop(getRSCM("item.feather"), 5, weight = 64),
                                WeightedDrop(getRSCM("item.feather"), 15, weight = 32),
                                WeightedDrop(item = null, weight = 32),
                            ),
                        beginnerClueChance = 1.0 / 300.0,
                    ),
            ),
            CritterVariant(
                name = "Rat",
                combatLevel = 1,
                npcKeys = RAT_IDS,
                hitpoints = 2,
                attack = 1,
                strength = 1,
                defence = 1,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                attackBonus = -47,
                strengthBonus = -53,
                defenceBonus = -42,
                slayerXp = 2.0,
                // Wiki respawn1 = 2 - the fastest respawn of anything wired up here, and
                // right: rats are back almost before you have turned around.
                respawnCycles = 2,
                attackAnimation = 2705,
                blockAnimation = 2706,
                deathAnimation = 2707,
                // No bones. See [Critters].
                drops = CritterDrops(wildernessLootingBagChance = LOOTING_BAG_CHANCE),
            ),
            CritterVariant(
                name = "Rat (Stronghold of Security)",
                combatLevel = 1,
                npcKeys = STRONGHOLD_RAT_IDS,
                hitpoints = 2,
                attack = 1,
                strength = 1,
                defence = 1,
                attackSpeed = 4,
                combatStyle = CombatStyle.CRUSH,
                attackBonus = -47,
                strengthBonus = -53,
                defenceBonus = -42,
                slayerXp = 2.0,
                // Wiki respawn2 = 30, an order of magnitude slower than the regular rats.
                respawnCycles = 30,
                attackAnimation = 2705,
                blockAnimation = 2706,
                deathAnimation = 2707,
                // The one version that does drop bones.
                drops = CritterDrops(always = listOf("item.bones")),
            ),
            CritterVariant(
                name = "Spider",
                combatLevel = 1,
                npcKeys = SPIDER_IDS,
                hitpoints = 2,
                attack = 1,
                strength = 1,
                defence = 1,
                attackSpeed = 4,
                combatStyle = CombatStyle.STAB,
                attackBonus = -35,
                strengthBonus = -58,
                defenceBonus = -53,
                slayerXp = 2.0,
                respawnCycles = 35,
                attackAnimation = 6249,
                blockAnimation = 6250,
                deathAnimation = 6251,
                elementalWeakness = ElementalWeakness(Elements.FIRE, FIRE_WEAKNESS_PERCENT),
                drops = CritterDrops(beginnerClueChance = 1.0 / 128.0, wildernessLootingBagChance = LOOTING_BAG_CHANCE),
            ),
            CritterVariant(
                name = "Spider (Stronghold of Security)",
                combatLevel = 24,
                npcKeys = STRONGHOLD_SPIDER_IDS,
                hitpoints = 22,
                attack = 21,
                strength = 21,
                defence = 21,
                attackSpeed = 4,
                combatStyle = CombatStyle.STAB,
                attackBonus = 35,
                strengthBonus = 58,
                defenceBonus = 53,
                slayerXp = 22.0,
                respawnCycles = 27,
                attackAnimation = 6249,
                blockAnimation = 6250,
                deathAnimation = 6251,
                // The only `aggressive = Yes` on any of the three pages.
                aggroRadius = 4,
                elementalWeakness = ElementalWeakness(Elements.FIRE, FIRE_WEAKNESS_PERCENT),
                drops = CritterDrops(beginnerClueChance = 1.0 / 128.0, wildernessLootingBagChance = LOOTING_BAG_CHANCE),
            ),
        )

    /**
     * `elementalweaknesstype = Fire`, `elementalweaknesspercent = 50` - stated once,
     * unversioned, on the Spider page, so it applies to all three spider versions. Neither
     * the chicken nor the rat page carries the field at all.
     */
    private const val FIRE_WEAKNESS_PERCENT = 50
}
