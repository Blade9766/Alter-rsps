package org.alter.plugins.content.items.jewellery

import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * The other half of charged jewellery: putting the charges back.
 *
 * Without this a glory is a one-way item - four teleports and then a permanently blank amulet - so
 * it belongs with [ChargedJewelleryPlugin] rather than as separate content.
 *
 * All three stations already carry a "Recharge-jewellery" option in this project's cache, so each is
 * a plain `onObjOption` and no new option had to be invented. What each one charges, and to what,
 * comes from the wiki:
 *
 *  - **Fountain of Heroes** (Heroes' Guild basement): amulets of glory only, to 4 charges.
 *  - **Totem Pole** (Legends' Guild): skills necklaces and combat bracelets only, to 4 charges.
 *  - **Fountain of Rune** (Wilderness, east of the Demonic Ruins): every dragonstone piece, to its
 *    full ladder - 6 for the glory, skills necklace and combat bracelet, 5 for the ring of wealth,
 *    which the wiki notes can *only* be charged here.
 *
 * Charging fills every applicable piece the player is carrying **and wearing** at once, unnoted
 * only, and works on a partly-used piece as well as an empty one - all three per the wiki.
 *
 * Not implemented, because none of the content behind them exists here: the Fountain of Uhld (Myths'
 * Guild, gated on Dragon Slayer II - and absent from this cache, which holds no object of that name
 * carrying the option), the Recharge Dragonstone lunar spell, and the charge dragonstone jewellery
 * scroll. The quest gates on the three stations that *do* exist (Heroes' Quest, Legends' Quest) are
 * left off for the usual reason - those quests are not in this project, so the gate would only make
 * the stations permanently useless.
 *
 * @see <a href="https://oldschool.runescape.wiki/w/Fountain_of_Rune">Fountain of Rune - OSRS Wiki</a>
 */
class JewelleryRechargePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    /**
     * A place that recharges jewellery.
     *
     * A null [charges] means "fill the piece to its own maximum", which is what the Fountain of Rune
     * does; the other two stations cap at [TRADITIONAL_CHARGES] regardless of how much the piece
     * could hold.
     */
    private class Station(
        val obj: String,
        val accepts: Set<ChargedJewellery>,
        val charges: Int?,
        val eternalGlory: Boolean,
    )

    init {
        val glories = setOf(ChargedJewellery.AMULET_OF_GLORY, ChargedJewellery.AMULET_OF_GLORY_TRIMMED)
        val legendsPieces = setOf(ChargedJewellery.SKILLS_NECKLACE, ChargedJewellery.COMBAT_BRACELET)

        val stations =
            listOf(
                Station(
                    obj = "object.fountain_of_heroes_31881",
                    accepts = glories,
                    charges = TRADITIONAL_CHARGES,
                    eternalGlory = false,
                ),
                Station(
                    obj = "object.totem_pole_31879",
                    accepts = legendsPieces,
                    charges = TRADITIONAL_CHARGES,
                    eternalGlory = false,
                ),
                Station(
                    obj = "object.fountain_of_rune_31942",
                    accepts =
                        glories + legendsPieces +
                            setOf(ChargedJewellery.RING_OF_WEALTH, ChargedJewellery.RING_OF_WEALTH_IMBUED),
                    charges = null,
                    eternalGlory = true,
                ),
            )

        stations.forEach { station ->
            onObjOption(station.obj, option = "Recharge-jewellery") {
                recharge(player, station)
            }
        }
    }

    private fun recharge(
        player: Player,
        station: Station,
    ) {
        var recharged = 0
        var eternal = false

        station.accepts.forEach { entry ->
            val target = station.charges?.coerceAtMost(entry.maxCharges) ?: entry.maxCharges
            val full = getRSCM(entry.chargeItems[target - 1])

            // Every id below the target charge, plus the uncharged one: a piece already at or above
            // the target is left alone, which is what stops a Fountain of Heroes dip from knocking a
            // six-charge glory back down to four.
            val replaceable =
                (entry.chargeItems.take(target - 1) + listOfNotNull(entry.uncharged))
                    .map { getRSCM(it) }
                    .toSet()

            for (slot in 0 until player.inventory.capacity) {
                if (player.inventory[slot]?.id in replaceable) {
                    player.inventory[slot] = rechargedItem(player, entry, station, full) { eternal = true }
                    recharged++
                }
            }

            val worn = player.equipment[entry.slot.id]
            if (worn != null && worn.id in replaceable) {
                player.equipment[entry.slot.id] = rechargedItem(player, entry, station, full) { eternal = true }
                recharged++
            }
        }

        when {
            eternal ->
                player.message(
                    "The power of the fountain is transferred into an amulet of eternal glory. " +
                        "It will now have unlimited charges.",
                )
            recharged > 0 -> player.message("You recharge your jewellery.")
            else -> player.message("You have nothing here that this can recharge.")
        }
    }

    /**
     * The item a charged-up piece becomes - normally the full-charge id, but at the Fountain of Rune
     * a glory has a 1 in 25,000 chance of coming out as an amulet of eternal glory instead.
     */
    private fun rechargedItem(
        player: Player,
        entry: ChargedJewellery,
        station: Station,
        full: Int,
        onEternal: () -> Unit,
    ): Item {
        val glory = entry == ChargedJewellery.AMULET_OF_GLORY || entry == ChargedJewellery.AMULET_OF_GLORY_TRIMMED
        // `World.random` is bound-*inclusive*, so a 1-in-N roll asks for 0..N-1.
        if (station.eternalGlory && glory && player.world.random(ETERNAL_GLORY_ONE_IN - 1) == 0) {
            onEternal()
            return Item(getRSCM("item.amulet_of_eternal_glory"))
        }
        return Item(full)
    }

    private companion object {
        /**
         * What the Fountain of Heroes and the Legends' Guild totem pole give. The wiki calls this
         * "the typical four that would be received at the traditional charging location", as opposed
         * to the Fountain of Rune's six.
         */
        private const val TRADITIONAL_CHARGES = 4

        /** 1 in 25,000, at the Fountain of Rune only. */
        private const val ETERNAL_GLORY_ONE_IN = 25_000
    }
}
