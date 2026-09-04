package org.alter.plugins.content.npcs.citizen

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
 * The citizen drop table - one table, shared by every level 2 Man and Woman.
 *
 * The `Man` and `Woman` wiki pages publish byte-identical drop sections, so this is not
 * duplicated per sex; the only split is [Table], which decides whether a variant rolls it
 * at all. West Ardougne, East Ardougne and the unused woman drop bones and nothing else -
 * see [Citizens] for why.
 *
 * ## The weights really are the wiki's, and they really do sum to 128
 *
 * Unusually for this codebase, the relative-weight approximation [DropRoll] documents costs
 * nothing here. Excluding the energy potion the numerators add to **exactly 128**, which is
 * also the check that confirms the two rows below are correctly handled:
 *
 * - **The 10-coin row (23/128) is dropped**, because it is flagged "Only dropped in
 *   free-to-play" and this server runs members content. Keeping it would have pushed the
 *   table to 151.
 * - **The `Nothing` row is the members 8/128**, not the "30/128 in F2P" alternative the same
 *   row publishes.
 *
 * **Energy potion(3)** is the one published gap: rarity "Common", with no numerator. It gets
 * weight 1 - the rarest labelled tier on the table - which is the same approximation the dark
 * wizard and goblin tables already make for this identical missing value, and rescales
 * everything else by 128/129.
 *
 * ## What is modelled that the older tables could not model
 *
 * The **herb sub-table** (`{{HerbDropLines|23/128}}`) is a real roll here, through
 * [HerbDropTable] - see [HERB_ROW]. The goblin and guard tables flagged their herb and seed
 * rows as unmodelled because neither table existed yet; both do now.
 *
 * ## What is left out
 *
 * - **Key (medium).** "Only dropped when completing a medium clue scroll asking the player to
 *   kill a Man" - there is no clue-step system to condition it on, so it would have to drop
 *   unconditionally or never. It drops never.
 * - **Rocky**, the Thieving pet, at 1/257211. No pet system.
 *
 * Coins are `item.coins_995`, the stackable currency item - **not** `item.coins` (617), which
 * is a real cache item named "Coins" but with `stackable=false`.
 */
internal object CitizenDrops {
    /** Which table a [CitizenVariant] rolls on death. */
    enum class Table {
        /** The full published citizen table, plus clue scrolls. */
        FULL,

        /** Bones only - what the East Ardougne and West Ardougne pages publish. */
        BONES_ONLY,
    }

    /**
     * The wiki's `{{HerbDropLines|23/128}}` row, standing in for the whole standard herb table.
     *
     * It is a member of [TABLE] rather than a separate roll on purpose: 23 of the table's 128
     * slots *lead to* the herb table, so a kill yields a herb **instead of** a coin pile, never
     * as well as one. Rolling it independently - the shape [
     * org.alter.plugins.content.npcs.slayer.SlayerMonsterPlugin] uses, where the wiki really
     * does publish a second roll - would hand out 23/128 extra drops.
     *
     * Matched by identity in [rollOnDeath], which is why it is a `val` and not an inline
     * `WeightedDrop(...)`: the `Nothing` row is also `item = null`.
     */
    private val HERB_ROW = WeightedDrop(item = null, weight = 23)

    val TABLE: List<WeightedDrop> =
        listOf(
            // Weapons and armour.
            WeightedDrop(getRSCM("item.bronze_med_helm"), 1, weight = 2),
            WeightedDrop(getRSCM("item.iron_dagger"), 1, weight = 1),
            // Runes and ammunition.
            WeightedDrop(getRSCM("item.bronze_bolts"), 2, 12, weight = 22),
            WeightedDrop(getRSCM("item.bronze_arrow"), 7, weight = 3),
            WeightedDrop(getRSCM("item.earth_rune"), 4, weight = 2),
            WeightedDrop(getRSCM("item.fire_rune"), 6, weight = 2),
            WeightedDrop(getRSCM("item.mind_rune"), 9, weight = 2),
            WeightedDrop(getRSCM("item.chaos_rune"), 2, weight = 1),
            // Herbs.
            HERB_ROW,
            // Coins.
            WeightedDrop(getRSCM("item.coins_995"), 3, weight = 38),
            WeightedDrop(getRSCM("item.coins_995"), 5, weight = 9),
            WeightedDrop(getRSCM("item.coins_995"), 15, weight = 4),
            WeightedDrop(getRSCM("item.coins_995"), 25, weight = 1),
            // Other.
            WeightedDrop(getRSCM("item.fishing_bait"), 1, weight = 5),
            WeightedDrop(getRSCM("item.copper_ore"), 1, weight = 2),
            WeightedDrop(getRSCM("item.earth_talisman"), 1, weight = 2),
            WeightedDrop(getRSCM("item.cabbage"), 1, weight = 1),
            WeightedDrop(getRSCM("item.energy_potion3"), 1, weight = 1),
            WeightedDrop(item = null, weight = 8),
        )

    /** `{{DropsLineClue|type=beginner|rarity=1/90}}`. */
    private const val BEGINNER_CLUE_CHANCE = 1.0 / 90.0

    /** `{{DropsLineClue|type=easy|rarity=1/128|f2p=yes}}`. */
    private const val EASY_CLUE_CHANCE = 1.0 / 128.0

    /**
     * `{{DropsLine|name=Looting bag|...|rarity=1/15}}`, Wilderness only.
     *
     * Published on the `Man` page and not on the `Woman` page, which is why it is a per-variant
     * flag rather than a property of this table. No citizen spawns in the Wilderness on this
     * server, so the asymmetry costs nothing today and is correct the day one does.
     */
    private const val LOOTING_BAG_CHANCE = 1.0 / 15.0

    /**
     * Drops guaranteed bones plus, for [Table.FULL], one roll on [TABLE] and the two clue
     * scroll tertiaries, at the citizen's tile and owned by whoever killed it.
     *
     * Rolled here rather than through the combat DSL's `drops { }` block, which builds a loot
     * table `NpcDeathAction` never actually rolls.
     */
    fun rollOnDeath(
        npc: Npc,
        variant: CitizenVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)

        if (variant.dropTable == Table.FULL) {
            DropRoll.pick(TABLE, world)?.let { picked ->
                when {
                    picked === HERB_ROW ->
                        DropRoll.pick(HerbDropTable.TABLE, world)?.let { herb ->
                            herb.item?.let { loot.add(it to DropRoll.amount(herb, world)) }
                        }

                    else -> picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
                }
            }

            if (world.randomDouble() <= BEGINNER_CLUE_CHANCE) {
                loot.add(getRSCM("item.clue_scroll_beginner") to 1)
            }
            if (world.randomDouble() <= EASY_CLUE_CHANCE) {
                loot.add(getRSCM("item.clue_scroll_easy") to 1)
            }
            if (variant.wildernessLootingBag && killer.inWilderness() && world.randomDouble() <= LOOTING_BAG_CHANCE) {
                loot.add(getRSCM("item.looting_bag") to 1)
            }
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }
}
