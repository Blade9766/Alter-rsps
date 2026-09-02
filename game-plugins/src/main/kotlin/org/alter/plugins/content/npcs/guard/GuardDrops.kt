package org.alter.plugins.content.npcs.guard

import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.plugins.content.npcs.DropRoll
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The city guard drop table, shared by every guard in the game.
 *
 * This lives on its own because the OSRS Wiki's Guard page publishes exactly **one** drop
 * table with no `dropversion=` on any `DropsTableHead` - Edgeville, Falador, Varrock and
 * Ardougne guards all roll the same one, despite having different stats and combat levels.
 * Duplicating it per city would invite the two copies to drift.
 *
 * Weights are the wiki's rarity numerators used as relative weights - see [DropRoll] for
 * why that is the right treatment given the sections below that aren't modelled.
 *
 * **What isn't modelled**, both flagged rather than faked:
 * - **Seeds** (`GeneralSeedDropLines`) - a template-expanded sub-table whose contents are
 *   not on the page, and there is no Farming skill here to use them.
 * - **The whole Tertiary section** - a medium clue scroll (1/128) and a Key (medium) that
 *   only drops mid-clue-step. Both need a clue/casket system this server doesn't have.
 *
 * **Members-world reading**: rows the wiki marks `{{(m)}}` (iron bolts, blood rune) are
 * kept, `{{(f)}}` free-to-play-only rows are dropped, and where a row carries a
 * free-to-play `altrarity` (the body talisman, 3/128 rising to 4/128 in f2p) the members
 * value is used. Consistent with the White Knights, and with this server already running
 * members content such as Barrows and the KBD.
 *
 * Coins are `item.coins_995`, the stackable currency item - **not** `item.coins` (617),
 * which is a real cache item named "Coins" but with `stackable=false`, so it would never
 * merge into a player's money.
 */
internal object GuardDrops {
    val TABLE: List<WeightedDrop> =
        listOf(
            // Runes and ammunition.
            WeightedDrop(getRSCM("item.iron_bolts"), 2, 12, weight = 10),
            WeightedDrop(getRSCM("item.steel_arrow"), 1, weight = 4),
            WeightedDrop(getRSCM("item.bronze_arrow"), 1, weight = 3),
            WeightedDrop(getRSCM("item.air_rune"), 6, weight = 2),
            WeightedDrop(getRSCM("item.earth_rune"), 3, weight = 2),
            WeightedDrop(getRSCM("item.fire_rune"), 2, weight = 2),
            WeightedDrop(getRSCM("item.bronze_arrow"), 2, weight = 2),
            WeightedDrop(getRSCM("item.blood_rune"), 1, weight = 1),
            WeightedDrop(getRSCM("item.chaos_rune"), 1, weight = 1),
            WeightedDrop(getRSCM("item.nature_rune"), 1, weight = 1),
            WeightedDrop(getRSCM("item.steel_arrow"), 5, weight = 1),
            // Coins.
            WeightedDrop(getRSCM("item.coins_995"), 1, weight = 19),
            WeightedDrop(getRSCM("item.coins_995"), 7, weight = 16),
            WeightedDrop(getRSCM("item.coins_995"), 12, weight = 9),
            WeightedDrop(getRSCM("item.coins_995"), 4, weight = 8),
            WeightedDrop(getRSCM("item.coins_995"), 25, weight = 4),
            WeightedDrop(getRSCM("item.coins_995"), 17, weight = 4),
            WeightedDrop(getRSCM("item.coins_995"), 30, weight = 2),
            // Other.
            WeightedDrop(getRSCM("item.iron_dagger"), 1, weight = 6),
            WeightedDrop(getRSCM("item.body_talisman"), 1, weight = 3),
            WeightedDrop(getRSCM("item.grain"), 1, weight = 1),
            WeightedDrop(getRSCM("item.iron_ore"), 1, weight = 1),
            WeightedDrop(item = null, weight = 8),
        )

    /**
     * Drops guaranteed bones plus one roll on [TABLE] at the guard's tile, owned by whoever
     * killed it. Rolled here rather than through the combat DSL's `drops { }` block, which
     * builds a loot table `NpcDeathAction` never actually rolls.
     */
    fun rollOnDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val drops = mutableListOf(getRSCM("item.bones") to 1)
        val picked = DropRoll.pick(TABLE, world)
        val pickedItem = picked?.item
        if (picked != null && pickedItem != null) {
            drops.add(pickedItem to DropRoll.amount(picked, world))
        }

        drops.forEach { (item, amount) ->
            world.spawn(GroundItem(item = item, amount = amount, tile = npc.tile, owner = killer))
        }
    }
}
