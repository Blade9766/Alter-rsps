package org.alter.plugins.content.npcs.goblin

import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.plugins.content.npcs.DropRoll
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The two goblin drop tables, exactly as the wiki splits them.
 *
 * The split is by *drop table*, not by level - the page publishes "Drop table 1" and
 * "Drop table 2" as separate sections and the infobox assigns one to each version, so
 * they are reproduced that way rather than re-grouped by combat level:
 *
 * - **[Table.ONE]** - the unarmed level 2 goblins (everything around Lumbridge) and the
 *   level 13s. Cheap: mostly a hammer, small coin piles, and a 35/128 "Nothing".
 * - **[Table.TWO]** - the armed level 2 goblins and every level 5. Noticeably better -
 *   bronze weapons, nature and chaos runes, and only an 8/128 "Nothing".
 *
 * Weights are the wiki's rarity numerators used as relative weights; see [DropRoll] for
 * why that is the right treatment when some rows are not modelled.
 *
 * **What is not modelled**, flagged rather than faked:
 * - **Table 2's herb sub-table** (`HerbDropLines`, 2/128) - a template-expanded table
 *   whose contents are not on the page, and there is no Herblore or Farming content here
 *   to use the herbs; the same call [org.alter.plugins.content.npcs.guard.GuardDrops]
 *   makes about the guards' seed table.
 * - **Goblin skull** - "only dropped during Rag and Bone Man I", a quest that does not
 *   exist on this server, so it would have to drop unconditionally or never. It drops
 *   never.
 *
 * **Energy potions** are the one published gap: all doses are listed as rarity "Common"
 * with no numerator on either table. They get weight 1 each - the rarest labelled tier on
 * the same table - the same approximation the dark wizard table already makes for the
 * identical missing value.
 *
 * That weight has to come from somewhere, because [DropRoll.pick] weights each row against
 * the **table total**, not against 128: a table that sums to anything else silently rescales
 * every row on it. The two tables pay for the potions differently, because the page leaves
 * different amounts of room:
 *
 * - **Table 2** publishes 128 across its `n/128` rows including a free-to-play-only 10 coin
 *   row worth 2/128. Dropping that row (see the members-world note below) frees exactly the
 *   2 the two potions need, so every other row keeps its published rate untouched.
 * - **Table 1** publishes a full 128 with no removable row, so its three potions have
 *   nowhere to go - the page is over-subscribed by 3. The 3 is taken out of the `Nothing`
 *   filler, 38 -> 35, which is the only row that can pay it without moving a real item off
 *   its published `n/128`. A filler row existing at all is what makes the denominator real;
 *   see `DungeonDropsVerify`, which fails any table that does not add up.
 *
 * The alternative was to drop the potions the way the herb sub-table below is dropped. They
 * are kept because, unlike the herbs, the items exist here and the page does list them - only
 * their rarity is missing, not their membership.
 *
 * **Members-world reading** matches the rest of the monster files: `{{(m)}}` rows (bronze
 * spear, goblin book, bronze javelin) are kept and the `{{(f)}}` free-to-play-only 10
 * coin row on table 2 is dropped. This server already runs members content such as
 * Barrows and the KBD.
 *
 * Coins are `item.coins_995`, the stackable currency item - **not** `item.coins` (617),
 * which is a real cache item named "Coins" but with `stackable=false`.
 */
internal object GoblinDrops {
    enum class Table { ONE, TWO }

    /** Accessed by most level 2 goblins and by the level 13s. */
    val TABLE_ONE: List<WeightedDrop> =
        listOf(
            // Weapons and armour.
            WeightedDrop(getRSCM("item.bronze_spear"), 1, weight = 4),
            WeightedDrop(getRSCM("item.bronze_sq_shield"), 1, weight = 3),
            // Runes and ammunition.
            WeightedDrop(getRSCM("item.water_rune"), 6, weight = 6),
            WeightedDrop(getRSCM("item.body_rune"), 7, weight = 5),
            WeightedDrop(getRSCM("item.earth_rune"), 4, weight = 3),
            WeightedDrop(getRSCM("item.bronze_bolts"), 8, weight = 3),
            // Coins.
            WeightedDrop(getRSCM("item.coins_995"), 5, weight = 28),
            WeightedDrop(getRSCM("item.coins_995"), 9, weight = 3),
            WeightedDrop(getRSCM("item.coins_995"), 15, weight = 3),
            WeightedDrop(getRSCM("item.coins_995"), 20, weight = 2),
            WeightedDrop(getRSCM("item.coins_995"), 1, weight = 1),
            // Other.
            WeightedDrop(getRSCM("item.hammer"), 1, weight = 15),
            WeightedDrop(getRSCM("item.goblin_mail"), 1, weight = 5),
            WeightedDrop(getRSCM("item.chefs_hat"), 1, weight = 3),
            WeightedDrop(getRSCM("item.goblin_book"), 1, weight = 2),
            WeightedDrop(getRSCM("item.beer"), 1, weight = 2),
            WeightedDrop(getRSCM("item.brass_necklace"), 1, weight = 1),
            WeightedDrop(getRSCM("item.air_talisman"), 1, weight = 1),
            WeightedDrop(getRSCM("item.energy_potion1"), 1, weight = 1),
            WeightedDrop(getRSCM("item.energy_potion2"), 1, weight = 1),
            WeightedDrop(getRSCM("item.energy_potion4"), 1, weight = 1),
            WeightedDrop(item = null, weight = 35),
        )

    /** Accessed by the armed level 2 goblins and every level 5. */
    val TABLE_TWO: List<WeightedDrop> =
        listOf(
            // Weapons and armour.
            WeightedDrop(getRSCM("item.bronze_spear"), 1, weight = 9),
            WeightedDrop(getRSCM("item.bronze_axe"), 1, weight = 3),
            WeightedDrop(getRSCM("item.bronze_scimitar"), 1, weight = 1),
            // Runes and ammunition.
            WeightedDrop(getRSCM("item.bronze_arrow"), 7, weight = 3),
            WeightedDrop(getRSCM("item.mind_rune"), 2, weight = 3),
            WeightedDrop(getRSCM("item.earth_rune"), 4, weight = 3),
            WeightedDrop(getRSCM("item.body_rune"), 2, weight = 3),
            WeightedDrop(getRSCM("item.bronze_javelin"), 5, weight = 2),
            WeightedDrop(getRSCM("item.chaos_rune"), 1, weight = 1),
            WeightedDrop(getRSCM("item.nature_rune"), 1, weight = 1),
            // Coins.
            WeightedDrop(getRSCM("item.coins_995"), 1, weight = 34),
            WeightedDrop(getRSCM("item.coins_995"), 3, weight = 13),
            WeightedDrop(getRSCM("item.coins_995"), 5, weight = 8),
            WeightedDrop(getRSCM("item.coins_995"), 16, weight = 7),
            WeightedDrop(getRSCM("item.coins_995"), 24, weight = 3),
            // Other.
            WeightedDrop(getRSCM("item.goblin_mail"), 1, weight = 10),
            WeightedDrop(getRSCM("item.hammer"), 1, weight = 9),
            WeightedDrop(getRSCM("item.goblin_book"), 1, weight = 2),
            WeightedDrop(getRSCM("item.grapes"), 1, weight = 1),
            WeightedDrop(getRSCM("item.red_cape"), 1, weight = 1),
            WeightedDrop(getRSCM("item.tin_ore"), 1, weight = 1),
            WeightedDrop(getRSCM("item.energy_potion1"), 1, weight = 1),
            WeightedDrop(getRSCM("item.energy_potion2"), 1, weight = 1),
            WeightedDrop(item = null, weight = 8),
        )

    /**
     * Drops guaranteed bones, one roll on the variant's main table, and the tertiary
     * rolls, at the goblin's tile and owned by whoever killed it.
     *
     * Rolled here rather than through the combat DSL's `drops { }` block, which builds a
     * loot table [org.alter.game.action.NpcDeathAction] never actually rolls - so a
     * goblin killed before this file existed dropped literally nothing, not even bones.
     */
    fun rollOnDeath(
        npc: Npc,
        table: Table,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val drops = mutableListOf(getRSCM("item.bones") to 1)

        val main = if (table == Table.ONE) TABLE_ONE else TABLE_TWO
        DropRoll.pick(main, world)?.let { picked ->
            picked.item?.let { drops.add(it to DropRoll.amount(picked, world)) }
        }

        drops += rollTertiary(table, world)

        drops.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }

    /**
     * The tertiary rolls, which are independent of the main table and of each other.
     * Table 2 is the kinder one on the ensouled head (1/30 vs 1/35); the beginner clue is
     * the one tertiary where table *1* is better (1/64 vs 1/80).
     */
    private fun rollTertiary(
        table: Table,
        world: World,
    ): List<Pair<Int, Int>> {
        val drops = mutableListOf<Pair<Int, Int>>()

        val ensouledChance = if (table == Table.ONE) 1.0 / 35.0 else 1.0 / 30.0
        if (world.randomDouble() <= ensouledChance) {
            drops.add(getRSCM("item.ensouled_goblin_head") to 1)
        }

        val beginnerClueChance = if (table == Table.ONE) 1.0 / 64.0 else 1.0 / 80.0
        if (world.randomDouble() <= beginnerClueChance) {
            drops.add(getRSCM("item.clue_scroll_beginner") to 1)
        }

        if (world.randomDouble() <= 1.0 / 128.0) {
            drops.add(getRSCM("item.clue_scroll_easy") to 1)
        }

        // Kept at its real 1/5000 even though there is no Champions' Challenge content to
        // hand it in to - it is a real cache item, and faking the rarity to hide a missing
        // system would be worse than dropping an item that currently only sits in a bank.
        if (world.randomDouble() <= 1.0 / 5000.0) {
            drops.add(getRSCM("item.goblin_champion_scroll") to 1)
        }

        return drops
    }
}
