package org.alter.plugins.content.npcs.critters

import org.alter.api.ElementalWeakness
import org.alter.api.Elements
import org.alter.game.model.combat.CombatStyle
import org.alter.rscm.RSCM.getRSCM

/**
 * Giant spiders, giant rats, dungeon rats and giant bats - the overgrown dungeon fauna
 * that shares its floors with the three in [Critters], and a much wider spread than the
 * name suggests: a level 2 giant spider is a starter monster, a level 50 one has 50
 * hitpoints and hits 7.
 *
 * They reuse [CritterVariant] because that record is just a stat block - ids, levels,
 * bonuses, style, respawn, animations, drops - with nothing level-1-specific in it. All but
 * the bats take one uniform defence bonus across the five slots, which is what
 * [CritterVariant.defenceBonus] gives them; the bats override the two slots that differ.
 *
 * The name is the wiki's own for most of them: the giant rat and the large dungeon rat both
 * examine as "Overgrown vermin." The giant bat is here because it lives in the same
 * dungeons and needed the same treatment, not because anyone would call it vermin.
 *
 * **The bare rscm names are the dangerous ones again, and worse here than in [Critters]:**
 * - `npc.giant_spider` is **2477, the level 50** Stronghold of Security spider - 50
 *   hitpoints, +10 bonuses, aggressive. The level 2 is `giant_spider_3017`.
 * - `npc.giant_rat` is **2510, the level 26** Stronghold rat. The level 3 is
 *   `giant_rat_2856`.
 * - `npc.dungeon_rat` is 2865, the full-tail one - same combat as the others, but the only
 *   dungeon rat with its own drop table.
 *
 * **Both giant families are aggressive at every level** (`aggressive = Yes` unversioned on
 * both pages), including the level 2 and level 3 ones that stand around Lumbridge. That is
 * correct - giant spiders and giant rats really do come at you - but it means these defs
 * change how those areas play, where the [Critters] ones mostly did not.
 *
 * **Animations are stated explicitly, and here that is load-bearing rather than routine.**
 * `MonsterAnimationsPlugin`'s name fallback would actively mislabel three of these four:
 * "Giant rat" normalises to `GIANT_RAT`, which has no entry, so its suffix rule matches
 * the plain `RAT` key and would hand a giant rat the small rat's 2705/2706/2707. "Dungeon
 * rat" would do the same. The real sets come from this project's own observed data - giant
 * rat 2856 and dungeon rat 2865 both observe [4933, 4934, 4935], which is exactly the
 * bundled `GIANT_CRYPT_RAT` entry's attack/block/death, and every giant spider id observes
 * [5327, 5328, 5329], exactly the `GIANT_SPIDER` entry. The small dungeon rats (3607)
 * observe only a subset of the same set, so they take it too.
 *
 * The giant bat is the sharpest case. "Giant bat" suffix-matches the bundled `BAT` key,
 * which gives death animation **4918** - and 4918 is not in giant bat 2834's observed set
 * at all. Its real set is [4915, 4916, 4917], so the fallback would have handed it a death
 * animation it never plays. Attack and block agree with `BAT`; only the death differs,
 * because `BAT` describes the ordinary bat, a different monster.
 */
internal object GiantVermin {
    /** Both giant spider pages carry `elementalweaknesstype = Fire` at 50%, unversioned. */
    private const val FIRE_WEAKNESS_PERCENT = 50

    /** Aggro sweep radius for the aggressive families. */
    private const val AGGRO_RADIUS = 4

    private const val SPIDER_ATTACK = 5327
    private const val SPIDER_BLOCK = 5328
    private const val SPIDER_DEATH = 5329

    private const val RAT_ATTACK = 4933
    private const val RAT_BLOCK = 4934
    private const val RAT_DEATH = 4935

    val GIANT_SPIDER_L2_IDS = listOf("npc.giant_spider_3017")
    val GIANT_SPIDER_L27_IDS = listOf("npc.giant_spider_3018")
    val GIANT_SPIDER_L50_IDS = listOf("npc.giant_spider")

    val GIANT_RAT_L3_IDS = listOf(2856, 2857, 2858, 2859, 2860, 2861).map { "npc.giant_rat_$it" }
    val GIANT_RAT_L6_IDS = listOf(2862, 2863, 2864).map { "npc.giant_rat_$it" }
    val GIANT_RAT_L26_IDS = listOf("npc.giant_rat", "npc.giant_rat_2511", "npc.giant_rat_2512")

    /** The size 1 dungeon rats. Faster respawn than the large ones, same everything else. */
    val DUNGEON_RAT_SMALL_IDS = listOf(3607, 3608, 3609).map { "npc.dungeon_rat_$it" }

    /** The size 2 dungeon rats missing part of their tail. */
    val DUNGEON_RAT_SHORT_TAIL_IDS = listOf("npc.dungeon_rat_2866", "npc.dungeon_rat_2867")

    /**
     * The one full-tailed large dungeon rat - the wiki notes it stands "once in the Goblin
     * Cave and once in the western part of the Clock Tower Dungeon", both of which this
     * server now has goblins in. It is the only dungeon rat that drops raw rat meat.
     */
    val DUNGEON_RAT_FULL_TAIL_IDS = listOf("npc.dungeon_rat")

    /**
     * The giant spiders' shared table. **No 100% drop at all** - not even bones, which is
     * published, not an omission. Wilderness Slayer tertiary is not modelled.
     */
    private val SPIDER_DROPS =
        CritterDrops(beginnerClueChance = 1.0 / 128.0, wildernessLootingBagChance = Critters.LOOTING_BAG_CHANCE)

    /** Levels 3 and 6 share one table; the Stronghold level 26 has its own. */
    private val GIANT_RAT_DROPS =
        CritterDrops(
            always = listOf("item.bones", "item.raw_rat_meat"),
            beginnerClueChance = 1.0 / 128.0,
            wildernessLootingBagChance = Critters.LOOTING_BAG_CHANCE,
        )

    /**
     * Level 26 giant rats drop bones and nothing else - their whole tertiary section is
     * quest-gated (Witch's Potion, Rag and Bone Man I and II), so none of it applies here.
     */
    private val STRONGHOLD_RAT_DROPS = CritterDrops(always = listOf("item.bones"))

    /**
     * "All dungeon rats besides one drop only bones and the rat's tail" - and the rat's
     * tail is Witch's Potion only, so: bones.
     */
    private val DUNGEON_RAT_DROPS = CritterDrops(always = listOf("item.bones"))

    /** The full-tail rat's own table, which the wiki says resembles a giant rat's. */
    private val DUNGEON_RAT_FULL_TAIL_DROPS =
        CritterDrops(
            always = listOf("item.bones", "item.raw_rat_meat"),
            beginnerClueChance = 1.0 / 128.0,
        )

    /** The giant bats' 1/5 Wilderness looting bag - three times the usual rate. */
    private const val BAT_LOOTING_BAG_CHANCE = 1.0 / 5.0

    private const val BAT_ATTACK = 4915
    private const val BAT_BLOCK = 4916
    private const val BAT_DEATH = 4917

    /** The normal, aggressive giant bat - the one in the Goblin Cave and most dungeons. */
    val GIANT_BAT_IDS = listOf("npc.giant_bat")

    /** The Arceuus giant bats. Identical in every published number except that they are passive. */
    val ARCEUUS_BAT_IDS = listOf("npc.giant_bat_6824")

    /** Bat bones, not bones. Tertiary is the Wilderness looting bag and a quest-only wing. */
    private val BAT_DROPS =
        CritterDrops(
            always = listOf("item.bat_bones"),
            wildernessLootingBagChance = BAT_LOOTING_BAG_CHANCE,
        )

    val VARIANTS: List<CritterVariant> =
        listOf(
            // --- Giant bats. Stab, speed 4, Air weakness 10%, and the only monsters in this
            // --- package whose five defence slots are not all the same number.
            giantBat("Giant bat", GIANT_BAT_IDS, aggroRadius = AGGRO_RADIUS),
            // The Arceuus version is `aggressive = No` and its respawn field is blank on the
            // wiki, so it keeps the normal bat's 35 ticks rather than inventing a different one.
            giantBat("Giant bat (Arceuus)", ARCEUUS_BAT_IDS, aggroRadius = 0),
            // --- Giant spiders. Stab, speed 4, aggressive, Fire weakness 50%. ---
            giantSpider(
                name = "Giant spider (level 2)",
                combatLevel = 2,
                npcKeys = GIANT_SPIDER_L2_IDS,
                hitpoints = 5,
                attack = 1,
                strength = 1,
                defence = 1,
                bonus = -10,
                slayerXp = 5.0,
                // Wiki respawn1 = 30, in game ticks - used as published.
                respawnCycles = 30,
            ),
            giantSpider(
                name = "Giant spider (level 27)",
                combatLevel = 27,
                npcKeys = GIANT_SPIDER_L27_IDS,
                hitpoints = 32,
                attack = 20,
                strength = 24,
                defence = 21,
                bonus = 0,
                slayerXp = 33.0,
                respawnCycles = 30,
            ),
            giantSpider(
                name = "Giant spider (level 50)",
                combatLevel = 50,
                npcKeys = GIANT_SPIDER_L50_IDS,
                hitpoints = 50,
                attack = 41,
                strength = 51,
                defence = 31,
                bonus = 10,
                slayerXp = 50.0,
                // Wiki respawn3 = 25 - the level 50 comes back faster than the other two.
                respawnCycles = 25,
            ),
            // --- Giant rats. Stab, speed 4, aggressive, all bonuses 0, respawn 30. ---
            giantRat(
                name = "Giant rat (level 3)",
                combatLevel = 3,
                npcKeys = GIANT_RAT_L3_IDS,
                hitpoints = 5,
                attack = 2,
                strength = 3,
                defence = 2,
                slayerXp = 5.0,
                drops = GIANT_RAT_DROPS,
            ),
            giantRat(
                name = "Giant rat (level 6)",
                combatLevel = 6,
                npcKeys = GIANT_RAT_L6_IDS,
                hitpoints = 10,
                attack = 6,
                strength = 5,
                defence = 2,
                slayerXp = 10.0,
                drops = GIANT_RAT_DROPS,
            ),
            giantRat(
                name = "Giant rat (Stronghold of Security)",
                combatLevel = 26,
                npcKeys = GIANT_RAT_L26_IDS,
                hitpoints = 25,
                attack = 22,
                strength = 23,
                defence = 22,
                slayerXp = 25.0,
                drops = STRONGHOLD_RAT_DROPS,
            ),
            // --- Dungeon rats. Stab, speed 4, NOT aggressive, all bonuses 0. ---
            dungeonRat(
                name = "Dungeon rat (small)",
                npcKeys = DUNGEON_RAT_SMALL_IDS,
                // Wiki respawn = 25 on the size 1 infobox.
                respawnCycles = 25,
                drops = DUNGEON_RAT_DROPS,
            ),
            dungeonRat(
                name = "Dungeon rat (short tail)",
                npcKeys = DUNGEON_RAT_SHORT_TAIL_IDS,
                // Wiki respawn = 50 on the size 2 infobox - twice the small ones'.
                respawnCycles = 50,
                drops = DUNGEON_RAT_DROPS,
            ),
            dungeonRat(
                name = "Dungeon rat (full tail)",
                npcKeys = DUNGEON_RAT_FULL_TAIL_IDS,
                respawnCycles = 50,
                drops = DUNGEON_RAT_FULL_TAIL_DROPS,
            ),
        )

    private fun giantSpider(
        name: String,
        combatLevel: Int,
        npcKeys: List<String>,
        hitpoints: Int,
        attack: Int,
        strength: Int,
        defence: Int,
        bonus: Int,
        slayerXp: Double,
        respawnCycles: Int,
    ) = CritterVariant(
        name = name,
        combatLevel = combatLevel,
        npcKeys = npcKeys,
        hitpoints = hitpoints,
        attack = attack,
        strength = strength,
        defence = defence,
        attackSpeed = 4,
        combatStyle = CombatStyle.STAB,
        attackBonus = bonus,
        strengthBonus = bonus,
        defenceBonus = bonus,
        slayerXp = slayerXp,
        respawnCycles = respawnCycles,
        attackAnimation = SPIDER_ATTACK,
        blockAnimation = SPIDER_BLOCK,
        deathAnimation = SPIDER_DEATH,
        aggroRadius = AGGRO_RADIUS,
        elementalWeakness = ElementalWeakness(Elements.FIRE, FIRE_WEAKNESS_PERCENT),
        drops = SPIDER_DROPS,
    )

    private fun giantRat(
        name: String,
        combatLevel: Int,
        npcKeys: List<String>,
        hitpoints: Int,
        attack: Int,
        strength: Int,
        defence: Int,
        slayerXp: Double,
        drops: CritterDrops,
    ) = CritterVariant(
        name = name,
        combatLevel = combatLevel,
        npcKeys = npcKeys,
        hitpoints = hitpoints,
        attack = attack,
        strength = strength,
        defence = defence,
        attackSpeed = 4,
        combatStyle = CombatStyle.STAB,
        attackBonus = 0,
        strengthBonus = 0,
        defenceBonus = 0,
        slayerXp = slayerXp,
        respawnCycles = 30,
        attackAnimation = RAT_ATTACK,
        blockAnimation = RAT_BLOCK,
        deathAnimation = RAT_DEATH,
        aggroRadius = AGGRO_RADIUS,
        drops = drops,
    )

    /**
     * Every giant bat is combat 27 with 32 hitpoints and 22s across the board. The only
     * thing that varies is whether it comes at you.
     */
    private fun giantBat(
        name: String,
        npcKeys: List<String>,
        aggroRadius: Int,
    ) = CritterVariant(
        name = name,
        combatLevel = 27,
        npcKeys = npcKeys,
        hitpoints = 32,
        attack = 22,
        strength = 22,
        defence = 22,
        attackSpeed = 4,
        combatStyle = CombatStyle.STAB,
        attackBonus = 0,
        strengthBonus = 0,
        // Not one number: 10 stab, 10 slash, 12 crush, 10 magic, 8 ranged. Crush is its
        // weakest-looking slot on paper and its strongest in fact.
        defenceBonus = 10,
        defenceCrush = 12,
        defenceRanged = 8,
        slayerXp = 32.0,
        // Wiki respawn1 = 35, in game ticks - used as published.
        respawnCycles = 35,
        attackAnimation = BAT_ATTACK,
        blockAnimation = BAT_BLOCK,
        deathAnimation = BAT_DEATH,
        aggroRadius = aggroRadius,
        elementalWeakness = ElementalWeakness(Elements.AIR, 10),
        drops = BAT_DROPS,
    )

    /** Every dungeon rat is combat 12 with 12 hitpoints and 10s across the board. */
    private fun dungeonRat(
        name: String,
        npcKeys: List<String>,
        respawnCycles: Int,
        drops: CritterDrops,
    ) = CritterVariant(
        name = name,
        combatLevel = 12,
        npcKeys = npcKeys,
        hitpoints = 12,
        attack = 10,
        strength = 10,
        defence = 10,
        attackSpeed = 4,
        combatStyle = CombatStyle.STAB,
        attackBonus = 0,
        strengthBonus = 0,
        defenceBonus = 0,
        slayerXp = 12.0,
        respawnCycles = respawnCycles,
        attackAnimation = RAT_ATTACK,
        blockAnimation = RAT_BLOCK,
        deathAnimation = RAT_DEATH,
        drops = drops,
    )

    init {
        // Fail fast at start-up rather than at first kill if a drop name ever goes stale.
        VARIANTS.asSequence().flatMap { it.drops.always.asSequence() }.distinct().forEach { getRSCM(it) }
    }
}
