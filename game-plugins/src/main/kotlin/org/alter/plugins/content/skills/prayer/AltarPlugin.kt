package org.alter.plugins.content.skills.prayer

import dev.openrune.cache.CacheManager
import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.model.timer.BONE_OFFER_DELAY
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.mechanics.prayer.Prayers

/**
 * Altars: praying at one to recharge, and offering bones at the one altar in the game that
 * takes them and is reachable from here.
 *
 * **Recharging** is bound by scanning the cache rather than by naming altars one at a time,
 * the same way [org.alter.plugins.content.skills.smithing.AnvilPlugin] finds anvils. Any
 * object carrying a "Pray-at" or "Pray" option gets it - which is a little over eighty
 * objects, from the Lumbridge church altar to the Ape Atoll statues to the god altars in
 * the God Wars Dungeon - so every altar in the map data works with no per-area
 * configuration. Recharging refills Prayer to the base level and clears the drain counter,
 * which is the "resets the timer on any Prayer point draining effects" the 5 April 2023
 * update added; see [Prayers.resetDrain].
 *
 * **Offering** is the Chaos Temple in level 38 Wilderness, west of the Lava Maze - the
 * altar the wiki's training guide calls simply "the chaos altar". It pays 350% of the
 * burying experience, the same rate as a gilded altar with both incense burners lit, and
 * on top of that each bone has a 50% chance not to be consumed, which is what doubles its
 * effective rate per inventory.
 *
 * The catch is that its object id, 411, is shared by five other chaos altars scattered
 * around the map, none of which take bones in OSRS. A loc dump of the cache puts the
 * Chaos Temple's at exactly one tile - `(2947, 3820)` - so [CHAOS_TEMPLE_ALTAR] gates on
 * the tile rather than the id, and every other altar answers with the wrath-of-the-gods
 * line OSRS gives for putting the wrong thing on an altar. Ashes are not bound at all:
 * demonic ashes cannot be offered at an altar in OSRS, only scattered.
 *
 * **Deliberately not implemented:**
 * - The gilded altar and its incense burners, which live in a player-owned house. There is
 *   no Construction skill and no house here, so the 250%/300%/350% burner tiers have
 *   nowhere to be earned. Their objects (13179-13199, 40872-40878) still recharge Prayer,
 *   since that half of an altar works anywhere.
 * - The Ectofuntus, the libation bowl and blessed bone shards, and the Sinister Offering
 *   and Demonic Offering spells - each is its own piece of area or spellbook content
 *   rather than a property of an altar.
 * - The handful of altars whose "Pray-at" does something extra in OSRS: the Yanille Agility
 *   dungeon altar opens a trapdoor, the Elidinis Statuette heals and cures, and the broken
 *   sun altar cannot be prayed at at all. They all simply recharge here.
 */
class AltarPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onWorldInit {
            val service = world.getService(PrayerService::class.java) ?: return@onWorldInit

            CacheManager.getObjects().forEach { (id, def) ->
                val actions = def.actions.filterNotNull().filter { it.isNotBlank() }

                // "Pray-at" and "Pray" never appear together on the same object, but the
                // first match is taken regardless: binding an option index twice throws,
                // and that would take this plugin - and with it every altar - offline.
                val pray = actions.firstOrNull { it.equals(PRAY_AT_OPTION, true) || it.equals(PRAY_OPTION, true) }
                if (pray != null) {
                    onObjOption(obj = id, option = pray) {
                        player.queue(TaskPriority.STANDARD) { recharge(this, player) }
                    }
                }

                // Only objects actually called an altar take bones. The statues and shrines
                // that recharge above are not somewhere OSRS lets you put a bone down.
                val name = def.name ?: return@forEach
                if (!name.contains("altar", ignoreCase = true)) return@forEach

                service.bones.forEach { bone ->
                    onItemOnObj(obj = id, item = bone.itemId) {
                        player.queue(TaskPriority.STANDARD) { offer(this, player, bone) }
                    }
                }
            }
        }
    }

    private suspend fun recharge(
        task: QueueTask,
        player: Player,
    ) {
        val skills = player.getSkills()

        player.animate(Animation.PRAY_AT_ALTAR_ANIM)
        player.playSound(Sound.ALTAR_PRAY)
        task.wait(RECHARGE_TICKS)

        skills.setCurrentLevel(Skills.PRAYER, skills.getBaseLevel(Skills.PRAYER))
        Prayers.resetDrain(player)
        player.message("You recharge your Prayer points.")
    }

    private suspend fun offer(
        task: QueueTask,
        player: Player,
        bone: OfferingEntry,
    ) {
        if (player.getInteractingGameObj().tile != CHAOS_TEMPLE_ALTAR) {
            player.message("You fear the wrath of the gods!")
            return
        }

        if (player.timers.has(BONE_OFFER_DELAY) || !player.inventory.contains(bone.itemId)) {
            return
        }
        player.timers[BONE_OFFER_DELAY] = OFFER_TICKS

        player.animate(OFFER_ANIMATION)
        player.playSound(Sound.POH_OFFER_BONES)
        task.wait(OFFER_TICKS)

        // Re-checked after the wait: the bone may have been dropped while the animation
        // was playing.
        if (!player.inventory.contains(bone.itemId)) {
            return
        }

        // Half of all offerings survive the altar. The experience is paid either way -
        // that is the whole point of the place, and why the wiki quotes it as double the
        // effective rate of a gilded altar rather than a higher rate per bone.
        if (world.chance(1, 2)) {
            if (player.inventory.remove(item = bone.itemId, amount = 1).hasFailed()) {
                return
            }
        }

        player.addXp(Skills.PRAYER, bone.experience * CHAOS_ALTAR_MULTIPLIER)
        player.message("The gods are very pleased with your offering.")
    }

    private companion object {
        const val PRAY_AT_OPTION = "Pray-at"
        const val PRAY_OPTION = "Pray"

        /** The Chaos Temple altar in level 38 Wilderness, from a cache loc dump of object 411. */
        val CHAOS_TEMPLE_ALTAR = Tile(2947, 3820)

        /** 350% of the burying experience, as at a gilded altar with both burners lit. */
        const val CHAOS_ALTAR_MULTIPLIER = 3.5

        /** RuneLite's `AnimationID.USING_GILDED_ALTAR`. */
        const val OFFER_ANIMATION = 3705

        /** "Player-owned house altar offerings: 3 ticks" - OSRS Wiki, Game tick/Action lengths. */
        const val OFFER_TICKS = 3

        const val RECHARGE_TICKS = 2
    }
}
