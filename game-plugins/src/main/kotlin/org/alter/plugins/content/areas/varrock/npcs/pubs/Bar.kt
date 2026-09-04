package org.alter.plugins.content.areas.varrock.npcs.pubs

import org.alter.api.ext.message
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.rscm.RSCM.getRSCM

/**
 * The one thing all four Varrock pubs do: take coins over the counter and hand something back.
 *
 * Every bartender's transcript spells out the same two outcomes - paid, or too poor - so the money
 * handling lives here once rather than four times over. What differs between the pubs is only the
 * wording around it, which stays in each pub's own file, which is why [canAfford] is separate from
 * [pay]: the "you haven't got enough" line is different at every bar (and at the Dancing Donkey
 * there isn't one), so each caller checks first and says its own piece.
 */
internal object Bar {
    /** Beer is 2 coins in all four Varrock pubs; the Ratpit's stew is the only other price. */
    const val BEER_PRICE = 2
    const val STEW_PRICE = 20

    private const val COINS = "item.coins_995"

    fun Player.canAfford(price: Int): Boolean = inventory.getItemCount(getRSCM(COINS)) >= price

    /**
     * Take [price] coins and give one [item]. Returns false having taken nothing if the player
     * cannot pay, or - the only case that says anything itself - if the drink will not fit.
     *
     * The order matters. Coins come out first because a player holding exactly [price] coins in a
     * full inventory frees the coin slot by paying, and that slot is what the drink then goes into;
     * checking free space up front would refuse a purchase that actually fits. If the drink still
     * does not fit the coins go straight back, and that refund cannot fail: it either returns to a
     * surviving stack or to the slot the removal just emptied.
     */
    suspend fun QueueTask.pay(player: Player, item: String, price: Int): Boolean {
        if (!player.canAfford(price)) return false

        player.inventory.remove(COINS, price)

        if (player.inventory.add(item, 1).hasFailed()) {
            player.inventory.add(COINS, price)
            player.message("You don't have enough inventory space to carry that.")
            return false
        }

        return true
    }
}
