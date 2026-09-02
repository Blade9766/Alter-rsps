package org.alter.plugins.content.mechanics.water

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.model.entity.Player
import org.alter.rscm.RSCM.getRSCM

/**
 * An item that holds water, in its empty and full forms.
 *
 * Ids are held as RSCM keys and resolved lazily rather than at construction: these live on
 * an `enum class`'s constants, and an enum initialises the moment anything touches the
 * class. Resolving eagerly would call `getRSCM` from a static initialiser, which throws
 * ("RSCM List is empty.") if anything ever reaches this class before `RSCM.init()`.
 */
class WaterContainer(val unfilledKey: String, val filledKey: String) {
    val unfilled: Int by lazy { getRSCM(unfilledKey) }
    val filled: Int by lazy { getRSCM(filledKey) }

    fun fill(
        player: Player,
        message: String,
    ) {
        // always succeeds
        player.autoReplace(unfilled, filled, growingDelay = false, slotAware = true, perform = {
            player.queue {
                player.animate(FILL_ANIM)
                player.playSound(Sound.TAP_FILL, 1, 0)
            }
        }, success = { player.message(message) })
    }

    fun empty(player: Player) {
        if (player.replaceItemInSlot(filled, unfilled, player.getInteractingItemSlot())) {
            player.queue {
                // only some make sounds when emptying
                if (unfilled.getItemName().contains(Regex("Bowl|Bucket|Jug"))) {
                    player.playSound(Sound.LIQUID, 1, 0)
                }
                player.message("You empty the contents of the ${unfilled.getItemName(lowercase = true)} on the floor.")
            }
        }
    }

    private companion object {
        /**
         * 832 is the game's generic "use item on object" animation, which is what filling
         * a container plays. [Animation] exposes it under three names, all of them about
         * some other use of the same animation, so it is spelled out here instead.
         */
        const val FILL_ANIM = Animation.USE_ITEM_ON_OBJECT_THAT_CAN_STORE_OBJECTS
    }
}

/**
 * Every container that can be filled with water at a [WaterSources].
 *
 * A waterskin fills straight from empty to full: the cache has `waterskin0` through
 * `waterskin4`, but OSRS fills it in one go rather than a dose at a time.
 */
enum class WaterContainers(val container: WaterContainer) {
    BOWL(WaterContainer("item.bowl", "item.bowl_of_water")),
    BUCKET(WaterContainer("item.bucket", "item.bucket_of_water")),
    CAN(WaterContainer("item.watering_can", "item.watering_can8")),
    CUP(WaterContainer("item.empty_cup", "item.cup_of_water")),
    JUG(WaterContainer("item.jug", "item.jug_of_water")),
    VIAL(WaterContainer("item.vial", "item.vial_of_water")),
    WATERSKIN(WaterContainer("item.waterskin0", "item.waterskin4")),
}
