package org.alter.plugins.content.skills.prayer

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.BURY_BONE_DELAY
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The Prayer skill's ground floor: burying a bone, or scattering demonic ashes, for the
 * experience the OSRS Wiki's `Prayer info` template calls `xp`.
 *
 * Levels and experience come from each item's own wiki page rather than from the Ectofuntus
 * comparison table, because the two disagree: wyrmling bones are worth 21 buried but 120 at
 * the Ectofuntus, which is not the 4x every other bone follows. Every entry here was read
 * off the item's page one at a time for that reason. The list lives in
 * `data/cfg/prayer/offerings.json` and is loaded by [PrayerService].
 *
 * **One offering per two ticks.** The wiki's action-length table gives burying as "2 ticks
 * per bone", and that is all the delay is - the bone, the experience and the animation all
 * land on the click itself, and a second click inside the window is dropped rather than
 * queued. [BURY_BONE_DELAY] already existed in the engine's timer keys for this; nothing
 * had ever used it. Scattering has no published figure, so it shares the same two ticks.
 *
 * Deliberately does not call [org.alter.game.model.entity.Pawn.lock], for the reason
 * documented on `WoodcuttingPlugin`: a full lock stops `walkTo()` from processing a
 * movement click, stranding the player mid-skill. In OSRS you can walk away from a bury
 * and it still completes, which is what falling through to the timer gives.
 *
 * **Only one Prayer level requirement exists.** Superior dragon bones need 70; everything
 * else is level 1, which is why the config leaves the field out for the other rows.
 *
 * **Deliberately not implemented:**
 * - The scatter animation. The cache has no name to search and the wiki does not publish
 *   the id, so ashes reuse the bury animation and sound. It is the one cosmetic guess here
 *   and is labelled as such rather than dressed up as sourced.
 * - The five duplicate "Bones" items (2530, 3187, 24655, 25199) and the Zogre Flesh Eaters
 *   cooking chain (burnt/pasty/marinated jogre bones), which belong to quests and
 *   minigames that don't exist here yet and whose experience the wiki does not publish.
 * - Hopespear's Will bones (Snothead's, Snailfeet's, Mosschin's, Redeyes', Strongbones'),
 *   which pay 1,250 each but only when buried in Yu'biusk - a location check that needs
 *   the miniquest to mean anything.
 * - The bonecrusher and ash sanctifier, which bury on a kill rather than on a click, and
 *   the Demonic Offering spell, which belongs to the Arceuus spellbook.
 */
class BuryingPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        loadService(PrayerService())

        onWorldInit {
            val service = world.getService(PrayerService::class.java) ?: return@onWorldInit

            service.offerings.forEach { entry ->
                onItemOption(item = entry.item, option = entry.action.option) { offer(player, entry) }
            }
        }
    }

    private fun offer(
        player: Player,
        entry: OfferingEntry,
    ) {
        if (player.isDead() || player.timers.has(BURY_BONE_DELAY)) {
            return
        }

        // The base level, not the current one: a Prayer drain must not lock a player out of
        // the only way they have to train it back up.
        if (player.getSkills().getBaseLevel(Skills.PRAYER) < entry.levelRequired) {
            player.message(
                "You need a Prayer level of ${entry.levelRequired} to " +
                    "${entry.action.option.lowercase()} these.",
            )
            return
        }

        val slot = player.getInteractingSlot()
        if (player.inventory.remove(item = entry.itemId, amount = 1, beginSlot = slot).hasFailed()) {
            return
        }

        // OSRS prints the two lines in this order for a bone - the hole first, then what
        // went into it. Ashes have no first line.
        entry.action.prelude?.let { player.message(it) }

        player.animate(Animation.BURY_BONE_ANIM)
        player.playSound(Sound.BURYING_BONE)
        player.addXp(Skills.PRAYER, entry.experience)
        player.message(entry.action.message)
        player.timers[BURY_BONE_DELAY] = OFFERING_DELAY
    }

    private companion object {
        /** "Burying bones: 2 ticks per bone" - OSRS Wiki, Game tick/Action lengths. */
        const val OFFERING_DELAY = 2
    }
}
