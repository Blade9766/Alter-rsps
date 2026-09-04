package org.alter.plugins.content.mechanics.dairy

import dev.openrune.cache.CacheManager
import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * Milking a dairy cow: an empty bucket in, a bucket of milk out.
 *
 * Nothing on this server produced a bucket of milk before this. That left
 * [org.alter.plugins.content.skills.cooking.DairyChurnPlugin] - and through it every potato
 * topping - with no reachable starting point, and the same for the cake and chocolate cake
 * chain in `skills/cooking/CookingRecipePlugin`, which all take milk. Gertrude's Cat asks
 * for a bucket of milk outright, so that quest could not be finished either.
 *
 * The dairy cow is **scenery, not an NPC**: `content/npcs/CowPlugin` handles the killable
 * cow, and neither it nor anything else touched object 8689. Cows are found by scanning the
 * cache for objects carrying a real "Milk" action, the discipline every cache-scanning
 * plugin here uses, narrowed to those named "dairy cow" - see [MILKABLE_NAMES].
 *
 * **Both routes OSRS offers are bound**, because the wiki documents both and they differ
 * only in their first delay: clicking the cow with a bucket in the inventory (3 ticks), and
 * using a bucket on it (4 ticks). After the first bucket, milking repeats on its own every
 * 8 ticks until the player runs out of empty buckets or walks away, which is the behaviour
 * the Bucket of milk article's timings describe.
 *
 * **Two things are stated rather than sourced.** The wiki records no chat message for a
 * successful milking, so none is printed - the item changing in the inventory is the
 * feedback, as it is in game. And no source gives the player's milking animation id, so the
 * generic "use item on object" animation stands in, exactly as
 * [org.alter.plugins.content.mechanics.water.WaterContainer] does for filling a bucket. The
 * sound is real: `Sound.MILK_COW` is already in this repo's verified sound list.
 *
 * ## Steal-cowbell
 *
 * The dairy cow objects carry more than one option, and Milk was the only one bound. Read out
 * of this cache: 8689 is `[Milk, Steal-cowbell]` and 12111, the Zanaris cow, is
 * `[Talk-to, Milk, Steal-cowbell]`. Clicking Steal-cowbell did nothing at all - the silent
 * dead click, not an error - so it is implemented here beside the milking, on the same objects
 * found by the same scan. It lives here rather than under `skills/thieving` because this file
 * is "what you can do to a dairy cow"; the three plugins there are each driven by their own
 * JSON table of many objects, and a single one-off object with no respawn and a fixed reward
 * does not fit any of them.
 *
 * The numbers are published and are used as published: **Thieving 15**, **16 Thieving xp**,
 * and a success chance interpolated from **128/255 at level 1 to 200/255 at level 99** - Mod
 * Ash's own figures, quoted on both the Dairy cow and Cowbells articles, and the reason the
 * wiki says "around 54% at level 15" (this formula gives 54.2%).
 *
 * **One published requirement is deliberately not enforced, and this is the flag for it:**
 * OSRS also requires the player to have *started* [Cold War]. That quest does not exist on
 * this server, so enforcing it would make the option permanently dead - which is the same
 * silent dead click in a different costume. The level gate is real and the quest gate is a
 * one-line addition to [stealCowbell] the day Cold War lands. Two other pieces of that quest's
 * content are likewise absent and are **not** faked here: the Zanaris cow's **Talk-to**, which
 * in OSRS is a conversation that hands over a cowbell, and playing the bells, which needs a
 * clockwork suit.
 *
 * The failure message and the animation are ours - no source gives either, so the stall
 * thieving animation stands in, the same way the milking animation does above.
 */
class DairyCowPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onWorldInit {
            CacheManager.getObjects().forEach { (id, def) ->
                val name = def.name?.lowercase() ?: return@forEach
                if (name !in MILKABLE_NAMES) return@forEach
                val milk =
                    def.actions.filterNotNull().firstOrNull { it.equals(MILK_OPTION, ignoreCase = true) }
                        ?: return@forEach

                onObjOption(obj = id, option = milk) {
                    player.queue(TaskPriority.STANDARD) { milk(this, player, CLICK_DELAY) }
                }
                onItemOnObj(obj = id, item = getRSCM(BUCKET)) {
                    player.queue(TaskPriority.STANDARD) { milk(this, player, ITEM_ON_DELAY) }
                }

                // Bound off the cache's own spelling rather than a literal, the same as Milk
                // above: both dairy cow ids carry this, and nothing was listening to it.
                def.actions
                    .filterNotNull()
                    .firstOrNull { it.equals(STEAL_OPTION, ignoreCase = true) }
                    ?.let { steal ->
                        onObjOption(obj = id, option = steal) {
                            val obj = player.getInteractingGameObj()
                            player.queue(TaskPriority.STANDARD) { stealCowbell(this, player, obj.tile) }
                        }
                    }
            }

            /*
             * Pouring the milk away. A bucket of milk carries an "Empty" option like every
             * water container does, but it is not one of `mechanics/water`'s containers - milk
             * is not water - so nothing was bound to it and the option did nothing.
             */
            onItemOption(item = BUCKET_OF_MILK, option = EMPTY_OPTION) {
                val slot = player.getInteractingItemSlot()
                if (player.replaceItemInSlot(getRSCM(BUCKET_OF_MILK), getRSCM(BUCKET), slot)) {
                    player.playSound(Sound.LIQUID)
                    player.message("You empty the contents of the bucket on the floor.")
                }
            }
        }
    }

    /**
     * Fills every empty bucket the player is carrying, one at a time.
     *
     * [firstDelay] is the wait before the first bucket, which is the only thing that differs
     * between clicking the cow and using a bucket on it; every bucket after that waits
     * [REPEAT_DELAY].
     */
    private suspend fun milk(
        task: QueueTask,
        player: Player,
        firstDelay: Int,
    ) {
        val bucket = getRSCM(BUCKET)
        val bucketOfMilk = getRSCM(BUCKET_OF_MILK)

        if (!player.inventory.contains(bucket)) {
            player.message(NO_BUCKET)
            return
        }

        var delay = firstDelay
        while (player.inventory.contains(bucket) && !player.hasMoveDestination()) {
            player.animate(MILK_ANIMATION)
            player.playSound(Sound.MILK_COW)
            task.wait(delay)

            // Re-checked after the wait: the bucket may have gone while the animation ran.
            if (!player.inventory.replace(bucket, bucketOfMilk)) {
                break
            }
            delay = REPEAT_DELAY
        }
    }

    /**
     * One attempt at a pair of cowbells.
     *
     * Single-shot rather than repeating, unlike [milk]: nothing published says Steal-cowbell
     * repeats on its own, and the two actions are not alike - milking is a gathering loop over
     * the buckets you carry, this is one roll against your Thieving level.
     *
     * The quest requirement is the missing check here, on purpose - see the class doc.
     */
    private suspend fun stealCowbell(
        task: QueueTask,
        player: Player,
        cow: Tile,
    ) {
        val level = player.getSkills().getCurrentLevel(Skills.THIEVING)
        if (level < Cowbells.LEVEL) {
            player.message("You need a Thieving level of ${Cowbells.LEVEL} to steal cowbells.")
            return
        }

        val cowbells = getRSCM(COWBELLS)
        // Cowbells do not stack, so a full inventory means there is nowhere to put them.
        if (player.inventory.isFull) {
            player.message("You don't have enough inventory space to steal cowbells.")
            return
        }

        player.faceTile(cow)
        player.lock()
        try {
            player.animate(STEAL_ANIMATION)
            task.wait(STEAL_DELAY)

            if (player.world.randomDouble() <= Cowbells.successChance(level)) {
                if (player.inventory.add(item = cowbells, amount = 1).hasFailed()) {
                    player.message("You don't have enough inventory space to steal cowbells.")
                    return
                }
                player.addXp(Skills.THIEVING, Cowbells.XP)
                player.message("You steal a pair of cowbells.")
            } else {
                player.message("You fail to steal the cowbells.")
            }
        } finally {
            player.unlock()
        }
    }

    private companion object {
        const val MILK_OPTION = "Milk"
        const val STEAL_OPTION = "Steal-cowbell"
        const val COWBELLS = "item.cowbells"

        /** Ticks before the attempt resolves. Not published; the stall steal's 2 is reused. */
        const val STEAL_DELAY = 2

        /**
         * The stall thieving animation, standing in because no source gives the real one - the
         * same admission [MILK_ANIMATION] makes.
         */
        const val STEAL_ANIMATION = Animation.THIEVING_STALL
        const val BUCKET = "item.bucket"
        const val BUCKET_OF_MILK = "item.bucket_of_milk"
        const val EMPTY_OPTION = "Empty"

        /**
         * Objects that carry a "Milk" action are 8689 and 12111, both named some casing of
         * "Dairy cow", and 52576, a `Dairy Buffalo`. The buffalo is left out: nothing says a
         * buffalo fills the same bucket of milk, and a name scan should not decide that on
         * its own.
         */
        val MILKABLE_NAMES = setOf("dairy cow")

        /** Clicking the cow fills the first bucket after 3 ticks; using a bucket on it, 4. */
        const val CLICK_DELAY = 3
        const val ITEM_ON_DELAY = 4

        /** Milking then continues on its own, a bucket every 8 ticks. */
        const val REPEAT_DELAY = 8

        /**
         * Gillie Groats' own words for milking with nothing to catch it in; the wording is
         * the one the wiki records for the Zanaris cow.
         */
        const val NO_BUCKET = "You'll need an empty bucket to collect the milk."

        /**
         * 832, the game's generic "use item on object" animation. No source gives the real
         * milking animation, and this is the same stand-in the water containers use.
         */
        const val MILK_ANIMATION = Animation.USE_ITEM_ON_OBJECT_THAT_CAN_STORE_OBJECTS
    }
}
