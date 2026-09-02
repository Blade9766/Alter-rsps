package org.alter.plugins.content.mechanics.milling

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * Grain milling: picking wheat, grinding it at a windmill, and coming away with a pot of
 * flour.
 *
 * This is the last missing link in the Cooking chain. `skills/cooking/CookingRecipePlugin`
 * turns a pot of flour and a container of water into dough, and
 * `mechanics/water/WaterPlugin` now fills the container - but until this, a pot of flour
 * had no source in the game beyond a White Knight drop. The Lumbridge cooking tutor has
 * been teaching the recipe all along.
 *
 * **The loop, as in OSRS:** pick wheat from a field for grain; carry it to the top floor of
 * a windmill and put it in the hopper; pull the hopper controls to grind it; go down to the
 * ground floor and use an empty pot on the flour bin. No skill requirement and no
 * experience at any step - the wiki is explicit that "making flour grants no Cooking
 * experience".
 *
 * **Hopper and bin contents are per player and persist across logout.** In OSRS these are
 * varbit-backed and private to you; making them shared server state would let one player
 * walk off with another's flour, and dropping them on logout would eat it. They are stored
 * as [MILL_HOPPER_GRAIN] and [MILL_FLOUR_BIN] instead.
 *
 * **Capacity** is [BIN_CAPACITY], from the wiki's "maximum capacity: 30 flour before
 * becoming full". Grinding into a full bin leaves the surplus in the hopper rather than
 * destroying it.
 *
 * **Both ways in work.** Grain can be used on the hopper or the hopper's own "Fill" option
 * clicked; a pot can be used on the bin or the bin's "Empty" option clicked. Each of those
 * handles one item per interaction, which is what using an item on the object does in OSRS,
 * and keeps the two routes behaving identically rather than having the menu option do
 * something subtly different.
 *
 * **Deliberately approximate:** the wheat-picking animation. [Animation] has no wheat entry
 * and the cache carries no sound data (sound ids in this codebase come from verified
 * external sources, never guesses), so picking reuses the farming vegetable-picking
 * animation and the generic pick-up sound rather than inventing ids for either.
 */
class MillingPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onWorldInit {
            val grain = getRSCM("item.grain")
            val pot = getRSCM("item.pot")

            MillObjects.WHEAT.scan().forEach { (obj, option) ->
                onObjOption(obj = obj, option = option) { pickWheat(player, grain) }
            }

            MillObjects.HOPPER.scan().forEach { (obj, option) ->
                onObjOption(obj = obj, option = option) { fillHopper(player, grain) }
                onItemOnObj(obj = obj, item = grain) { fillHopper(player, grain) }
            }

            MillObjects.CONTROLS.scan().forEach { (obj, option) ->
                onObjOption(obj = obj, option = option) { operateControls(player) }
            }

            MillObjects.BIN.scan().forEach { (obj, option) ->
                onObjOption(obj = obj, option = option) { fillPot(player, pot) }
                onItemOnObj(obj = obj, item = pot) { fillPot(player, pot) }
            }
        }
    }

    /** Wheat is inexhaustible in OSRS - the plant stays put however much you pick. */
    private fun pickWheat(
        player: Player,
        grain: Int,
    ) {
        if (player.inventory.isFull) {
            player.message("Your inventory is too full to hold any more grain.")
            return
        }
        player.animate(PICK_ANIMATION)
        player.playSound(Sound.PICKUP_ITEM)
        player.inventory.add(item = grain, amount = 1)
        player.message("You pick some grain.")
    }

    private fun fillHopper(
        player: Player,
        grain: Int,
    ) {
        if (!player.inventory.contains(grain)) {
            player.message("You have no grain to put in the hopper.")
            return
        }

        val held = player.attr[MILL_HOPPER_GRAIN] ?: 0
        if (held >= BIN_CAPACITY) {
            player.message("The hopper is full.")
            return
        }

        if (player.inventory.remove(item = grain, amount = 1).hasFailed()) {
            return
        }
        player.attr[MILL_HOPPER_GRAIN] = held + 1
        player.message("You put the grain in the hopper.")
    }

    private fun operateControls(player: Player) {
        val held = player.attr[MILL_HOPPER_GRAIN] ?: 0
        if (held <= 0) {
            player.message("You need to put some grain in the hopper first.")
            return
        }

        val stored = player.attr[MILL_FLOUR_BIN] ?: 0
        val room = BIN_CAPACITY - stored
        if (room <= 0) {
            player.message("The flour bin is full.")
            return
        }

        // Surplus stays in the hopper rather than being ground into a bin that can't hold
        // it - grinding grain into nothing would be a silent loss of the player's items.
        val ground = minOf(held, room)
        player.attr[MILL_HOPPER_GRAIN] = held - ground
        player.attr[MILL_FLOUR_BIN] = stored + ground

        player.animate(Animation.TAKE_ITEM)
        player.message("You operate the hopper. The grain slides down the chute.")
        if (held > ground) {
            player.message("The flour bin is now full; the rest of your grain is still in the hopper.")
        }
    }

    private fun fillPot(
        player: Player,
        pot: Int,
    ) {
        val stored = player.attr[MILL_FLOUR_BIN] ?: 0
        if (stored <= 0) {
            player.message("The flour bin is empty.")
            return
        }
        if (!player.inventory.contains(pot)) {
            player.message("You need an empty pot to hold the flour.")
            return
        }

        if (player.inventory.remove(item = pot, amount = 1).hasFailed()) {
            return
        }
        player.inventory.add(item = getRSCM("item.pot_of_flour"), amount = 1)
        player.attr[MILL_FLOUR_BIN] = stored - 1
        player.message("You fill the pot with flour.")
    }

    companion object {
        /** The wiki's figure: the bin holds 30 flour before it is full. */
        const val BIN_CAPACITY = 30

        /**
         * Grain waiting in the hopper, per player. Persisted so a player who logs out
         * mid-mill doesn't lose it.
         */
        val MILL_HOPPER_GRAIN = AttributeKey<Int>(persistenceKey = "mill_hopper_grain")

        /** Flour waiting in the bin, per player. Persisted for the same reason. */
        val MILL_FLOUR_BIN = AttributeKey<Int>(persistenceKey = "mill_flour_bin")

        /**
         * No wheat-picking animation is named in [Animation]; the farming vegetable pick is
         * the closest real one and reads correctly for pulling grain off a stalk.
         */
        const val PICK_ANIMATION = Animation.FARMING_PICKING_VEGETABLE
    }
}
