package org.alter.plugins.content.items.jewellery

import org.alter.api.EquipmentType
import org.alter.api.ext.getEquipment
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.rscm.RSCM.getRSCM

/**
 * The ring of wealth's two passive effects, which are the two things it does that have nothing to do
 * with its teleports (those are [ChargedJewellery]'s).
 *
 * Both work **whether or not the ring is charged**, and both only need it worn - "note that the ring
 * only needs to be worn by the player while dealing the killing blow before the loot appears". So
 * neither reads a charge, and the id set below is every ring of wealth in the game: uncharged,
 * charged 1-5, and the imbued variants of both. The imbued ring's own perk (double clue scrolls in
 * the Wilderness) is not modelled - there are no clue scrolls here - but it is still a ring of wealth
 * and gets these two.
 *
 * ## Rare drop enhancement
 *
 * Wearing it removes the "nothing" outcome from the **shared** rare, gem and mega-rare drop tables.
 * That is the whole effect: "the ring's rare drop enhancement only affects the specific drops that
 * are accessed through the rare and gem drop tables. It does not boost the chances of receiving
 * monster-specific unique drops, nor does it remove any 'nothing' drops that are specific to a
 * monster's own drop table." So `MonsterDropTable` keeps its own `Nothing` rows and only passes the
 * flag down to the gem table.
 *
 * ## Currency collection
 *
 * Coins, Tokkul and numulites dropped by a monster the player kills go straight to the inventory
 * instead of onto the ground, and the player can turn that off. This is deliberately applied at
 * `MonsterLoot`, the shared monster-drop helper, rather than at `World.spawn` - the effect is
 * specific to monster loot, and hooking every ground item spawned in the game would also swallow
 * the player's own drops, duel-arena stake returns and search-box rewards.
 *
 * @see <a href="https://oldschool.runescape.wiki/w/Ring_of_wealth">Ring of wealth - OSRS Wiki</a>
 */
object RingOfWealth {
    /**
     * Whether currency collection is switched **off**. Stored as the negative so that a player who
     * has never touched the toggle gets the default-on behaviour without a login-time write.
     */
    val COLLECTION_DISABLED_ATTR = AttributeKey<Boolean>(persistenceKey = "ring_of_wealth_collection_off")

    private val RINGS =
        arrayOf(
            "item.ring_of_wealth",
            "item.ring_of_wealth_1",
            "item.ring_of_wealth_2",
            "item.ring_of_wealth_3",
            "item.ring_of_wealth_4",
            "item.ring_of_wealth_5",
            "item.ring_of_wealth_i",
            "item.ring_of_wealth_i1",
            "item.ring_of_wealth_i2",
            "item.ring_of_wealth_i3",
            "item.ring_of_wealth_i4",
            "item.ring_of_wealth_i5",
        )

    /** Every ring of wealth id, resolved lazily - `RSCM.init()` runs after class loading. */
    val ringIds: Set<Int> by lazy { RINGS.map { getRSCM(it) }.toSet() }

    /** The three currencies the ring collects. */
    val currencyIds: Set<Int> by lazy {
        setOf(getRSCM("item.coins_995"), getRSCM("item.tokkul"), getRSCM("item.numulite"))
    }

    /** The keys, for the plugin that binds the toggle and for `JewelleryVerify`. */
    val ringKeys: Array<String> get() = RINGS

    /** True while [player] is wearing any ring of wealth. */
    fun isWorn(player: Player): Boolean = player.getEquipment(EquipmentType.RING)?.id in ringIds

    /** The six *imbued* ids out of [RINGS] - the `(i)` variants and their four charge states. */
    val imbuedIds: Set<Int> by lazy { RINGS.filter { "_i" in it.removePrefix("item.ring_of_wealth") }.map { getRSCM(it) }.toSet() }

    /**
     * True while [player] is wearing a ring of wealth **(i)** specifically.
     *
     * Separate from [isWorn] because a handful of published drop rates name the imbued ring rather
     * than the ordinary one - the hellhound's hard clue scroll is the first in this tree
     * ("increases to 1/32 if a ring of wealth (i) is worn and fought in the Wilderness"). Every
     * *shared-table* effect still keys off [isWorn]: the ordinary ring removes the `Nothing` rows
     * just as the imbued one does, and only the Wilderness clue improvement is imbued-only.
     */
    fun isImbued(player: Player): Boolean = player.getEquipment(EquipmentType.RING)?.id in imbuedIds

    /**
     * Whether the shared drop tables should drop their `Nothing` rows for this kill. A null killer -
     * which no caller has today, but the drop code is written defensively - is treated as no ring.
     */
    fun enhancesDropTables(player: Player?): Boolean = player != null && isWorn(player)

    /** Whether [item] should go straight into [player]'s inventory rather than onto the ground. */
    fun collects(
        player: Player,
        item: Int,
    ): Boolean = item in currencyIds && isWorn(player) && player.attr[COLLECTION_DISABLED_ATTR] != true
}
