package org.alter.plugins.content.npcs.darkwarrior

import org.alter.api.ext.inWilderness
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.plugins.content.npcs.DropRoll
import org.alter.plugins.content.npcs.HerbDropTable
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The three published Dark warrior drop tables, and the roll that runs on death.
 *
 * Drops are rolled here rather than through [org.alter.api.dsl.NpcCombatDsl]'s `drops { }` block:
 * that block builds a loot table [org.alter.game.action.NpcDeathAction] never actually rolls, so
 * configuring it produces no drops at all. Every other monster package in this codebase takes the
 * same route.
 *
 * ## The numbers
 *
 * Weights are the wiki's rarity numerators used as relative weights, as everywhere else here. All
 * three tables sum to **exactly 128**, which is the check that the transcription is complete - see
 * `DarkWarriorVerify`, which asserts it rather than trusting this comment. Two things had to be
 * right for that to hold:
 *
 * - The level 8 table's `Coins (10)` row is marked `{{(f)}}`, "only dropped in free-to-play
 *   worlds". It is excluded, which is what makes that table 128 instead of 131 - and is the same
 *   members-world reading the White Knight and citizen packages already apply.
 * - The `Nothing` rows (18/128 on the level 8 table, 16/128 on the level 145 one) are real rows
 *   and are carried as `item = null`. The Great Kourend table publishes none, and so has none.
 *
 * ## The herb table
 *
 * All three versions carry `{{HerbDropLines|3/128}}` - a 3/128 row that leads to the shared
 * [HerbDropTable] rather than naming any herb itself. [HERB_ROW] is that row: one shared sentinel
 * instance, matched by identity in [rollOnDeath], which then rolls the herb table proper. Herbs
 * are dropped grimy, as they are everywhere else.
 *
 * ## What is deliberately not modelled
 *
 * The level 8 and level 145 pages both end with `{{WildernessSlayerDropTable}}` - Larran's keys
 * and slayer's enchantments, which drop **only while on a Krystilia task**. Krystilia is not
 * implemented (see [org.alter.plugins.content.skills.slayer.Slayer]), so there is no task state
 * to gate those rows on and dropping them unconditionally would be strictly wrong. They are left
 * out until there is a Wilderness Slayer master to hang them off.
 *
 * The level 145's hard clue also has an `altrarity` of 1/64 for a player wearing a ring of wealth
 * (i); there is no imbued ring of wealth here, so the base 1/128 is used.
 */
internal object DarkWarriorDrops {
    private fun drop(
        item: String,
        min: Int = 1,
        max: Int = min,
        weight: Int,
    ) = WeightedDrop(getRSCM(item), min, max, weight)

    /**
     * `{{HerbDropLines|3/128}}`, shared by all three tables. Matched by identity (`===`) in
     * [rollOnDeath], so one instance in all three lists is correct and intended.
     */
    val HERB_ROW = WeightedDrop(item = null, weight = 3)

    /** The free-to-play original's table - `dropversion = Level 8`. */
    val LEVEL_8_TABLE: List<WeightedDrop> =
        listOf(
            // Weapons and armour.
            drop("item.bronze_med_helm", weight = 3),
            drop("item.iron_mace", weight = 1),
            drop("item.black_med_helm", weight = 1),
            drop("item.black_mace", weight = 1),
            // Runes and ammunition.
            drop("item.bronze_arrow", 8, weight = 4),
            drop("item.mind_rune", 2, weight = 3),
            drop("item.water_rune", 3, weight = 2),
            drop("item.nature_rune", 3, weight = 2),
            drop("item.earth_rune", 2, weight = 1),
            drop("item.chaos_rune", 2, weight = 1),
            // Herbs.
            HERB_ROW,
            // Coins. The 10-coin row is free-to-play only and excluded; see this object's comment.
            drop("item.coins_995", 1, weight = 31),
            drop("item.coins_995", 2, weight = 20),
            drop("item.coins_995", 6, weight = 20),
            drop("item.coins_995", 13, weight = 7),
            drop("item.coins_995", 20, weight = 6),
            drop("item.coins_995", 30, weight = 2),
            // Other.
            WeightedDrop(item = null, weight = 18),
            drop("item.iron_ore", weight = 1),
            drop("item.sardine", weight = 1),
        )

    /** Shared by levels 37, 51 and 62 - `dropversion = Great Kourend`. */
    val KOUREND_TABLE: List<WeightedDrop> =
        listOf(
            // Weapons and armour.
            drop("item.steel_med_helm", weight = 3),
            drop("item.steel_mace", weight = 1),
            drop("item.black_med_helm", weight = 1),
            drop("item.black_mace", weight = 1),
            // Runes and ammunition.
            drop("item.bronze_arrow", 12, weight = 4),
            drop("item.mind_rune", 10, weight = 3),
            drop("item.water_rune", 10, weight = 2),
            drop("item.nature_rune", 8, weight = 2),
            drop("item.earth_rune", 10, weight = 1),
            drop("item.chaos_rune", 8, weight = 1),
            // Herbs.
            HERB_ROW,
            // Coins.
            drop("item.coins_995", 50, weight = 31),
            drop("item.coins_995", 20, weight = 20),
            drop("item.coins_995", 30, weight = 20),
            drop("item.coins_995", 10, weight = 18),
            drop("item.coins_995", 80, weight = 9),
            drop("item.coins_995", 100, weight = 6),
            // Other. This table publishes no "Nothing" row.
            drop("item.sardine", 2, weight = 1),
            drop("item.iron_ore", 3, weight = 1),
        )

    /** The Wilderness rewrite's table - `dropversion = Level 145`. */
    val LEVEL_145_TABLE: List<WeightedDrop> =
        listOf(
            // Weapons and armour.
            drop("item.adamant_med_helm", weight = 3),
            drop("item.black_mace", weight = 1),
            drop("item.black_med_helm", weight = 1),
            drop("item.mithril_mace", weight = 1),
            // Runes and ammunition.
            drop("item.bronze_arrow", 28, weight = 4),
            drop("item.mind_rune", 12, weight = 3),
            drop("item.water_rune", 12, weight = 2),
            drop("item.nature_rune", 9, weight = 2),
            drop("item.earth_rune", 12, weight = 1),
            drop("item.chaos_rune", 8, weight = 1),
            // Herbs.
            HERB_ROW,
            // Coins.
            drop("item.coins_995", 100, weight = 31),
            drop("item.coins_995", 20, weight = 20),
            drop("item.coins_995", 60, weight = 20),
            drop("item.coins_995", 130, weight = 7),
            drop("item.coins_995", 200, weight = 6),
            drop("item.coins_995", 90, weight = 2),
            // Other.
            WeightedDrop(item = null, weight = 16),
            drop("item.dark_fishing_bait", 10, 24, weight = 2),
            drop("item.iron_ore", 3, weight = 1),
            drop("item.sardine", 3, weight = 1),
        )

    val TABLES: Map<DarkWarriors.Table, List<WeightedDrop>> =
        mapOf(
            DarkWarriors.Table.LEVEL_8 to LEVEL_8_TABLE,
            DarkWarriors.Table.KOUREND to KOUREND_TABLE,
            DarkWarriors.Table.LEVEL_145 to LEVEL_145_TABLE,
        )

    /**
     * `{{DropsLine|name=Looting bag|...|rarity=1/15}}` on the level 8 page, whose note reads
     * "Looting bags are only dropped by those found in the Wilderness" - hence the
     * [Player.inWilderness] gate. The level 145 row is published at 1/3 with no such note, because
     * every level 145 is in the Wilderness by definition; the gate is applied to both anyway, so
     * that a dark warrior spawned somewhere else by a future plugin behaves sensibly.
     */
    private const val LEVEL_8_LOOTING_BAG_CHANCE = 1.0 / 15.0
    private const val LEVEL_145_LOOTING_BAG_CHANCE = 1.0 / 3.0

    /** `{{DropsLineClue|type=medium|rarity=1/96}}`, Great Kourend only. */
    private const val MEDIUM_CLUE_CHANCE = 1.0 / 96.0

    /** `{{DropsLineClue|type=hard|rarity=1/128}}`, level 145 only. */
    private const val HARD_CLUE_CHANCE = 1.0 / 128.0

    /**
     * Drops guaranteed bones, one roll on the variant's main table, and its tertiaries, at the
     * warrior's tile and owned by whoever killed it.
     */
    fun rollOnDeath(
        npc: Npc,
        variant: DarkWarriors.Variant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)

        DropRoll.pick(TABLES.getValue(variant.table), world)?.let { picked ->
            when {
                picked === HERB_ROW ->
                    DropRoll.pick(HerbDropTable.TABLE, world)?.let { herb ->
                        herb.item?.let { loot.add(it to DropRoll.amount(herb, world)) }
                    }

                else -> picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
            }
        }

        when (variant.table) {
            DarkWarriors.Table.LEVEL_8 ->
                if (killer.inWilderness() && world.randomDouble() <= LEVEL_8_LOOTING_BAG_CHANCE) {
                    loot.add(getRSCM("item.looting_bag") to 1)
                }

            DarkWarriors.Table.KOUREND ->
                if (world.randomDouble() <= MEDIUM_CLUE_CHANCE) {
                    loot.add(getRSCM("item.clue_scroll_medium") to 1)
                }

            DarkWarriors.Table.LEVEL_145 -> {
                if (killer.inWilderness() && world.randomDouble() <= LEVEL_145_LOOTING_BAG_CHANCE) {
                    loot.add(getRSCM("item.looting_bag") to 1)
                }
                if (world.randomDouble() <= HARD_CLUE_CHANCE) {
                    loot.add(getRSCM("item.clue_scroll_hard") to 1)
                }
            }
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }
}
