package org.alter.plugins.content.items.jewellery

import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.ext.getEquipment
import org.alter.api.ext.getWildernessLevel
import org.alter.api.ext.heal
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.LockState
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.RING_OF_LIFE_ARDOUGNE_RESPAWN_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.magic.TeleportType
import org.alter.plugins.content.magic.teleport
import org.alter.rscm.RSCM.getRSCM

/**
 * The three pieces of enchanted jewellery that spend themselves to keep the wearer alive.
 *
 * All three fire off the engine's low-health hook, and they are in one plugin on purpose: OSRS gives
 * them a strict order of precedence - "if it is worn along with a phoenix necklace, the necklace
 * effect will trigger first" - and separate `onPlayerLowHealth` registrations would run in whatever
 * order the classpath scan happened to find them in.
 *
 *  - **Phoenix necklace**: at 20% hp or below, heals `floor(Hitpoints level * 3/10)` and is
 *    destroyed.
 *  - **Necklace of faith**: at 20% hp or below, restores 25% of the Prayer *level* in prayer points
 *    and is destroyed. (Both are amulet-slot, so only one can ever be worn.)
 *  - **Ring of life**: at 10% hp or below, teleports to the respawn point and is destroyed.
 *
 * The necklaces run first, and because the phoenix heals *before* the ring is considered, a player
 * wearing both is pulled back above 10% and keeps the ring - which is exactly the behaviour the wiki
 * describes.
 *
 * Not reproduced: the phoenix necklace also clears the wearer's pending attack buffer, cancelling
 * in-flight hits from delayed attacks. That would need the hit queue to expose a way to drop pending
 * hits on a pawn, which it does not, and faking it by zeroing damage would change how those hits
 * render. The heal, the destruction and the ordering - the parts that decide whether a player lives
 * - are all here.
 *
 * @see <a href="https://oldschool.runescape.wiki/w/Phoenix_necklace">Phoenix necklace - OSRS Wiki</a>
 * @see <a href="https://oldschool.runescape.wiki/w/Ring_of_life">Ring of life - OSRS Wiki</a>
 */
class SurvivalJewelleryPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    private val phoenixNecklace = getRSCM("item.phoenix_necklace")
    private val necklaceOfFaith = getRSCM("item.necklace_of_faith")
    private val ringOfLife = getRSCM("item.ring_of_life")

    init {
        onItemOption("item.ring_of_life", option = "Toggle-respawn") {
            val ardougne = !(player.attr[RING_OF_LIFE_ARDOUGNE_RESPAWN_ATTR] ?: false)
            player.attr[RING_OF_LIFE_ARDOUGNE_RESPAWN_ATTR] = ardougne
            if (ardougne) {
                player.message("Your ring will now save you to East Ardougne.")
            } else {
                player.message("Your ring will now save you to your respawn point.")
            }
        }

        onPlayerLowHealth {
            val player = player
            if (player.lock != LockState.NONE) {
                return@onPlayerLowHealth
            }

            val amulet = player.getEquipment(EquipmentType.AMULET)?.id
            when (amulet) {
                phoenixNecklace -> {
                    player.equipment[EquipmentType.AMULET.id] = null
                    // Base Hitpoints level, not current hp and not the boosted level.
                    player.heal(player.getSkills().getBaseLevel(Skills.HITPOINTS) * 3 / 10)
                    player.message("Your phoenix necklace heals you, then crumbles to dust.")
                }
                necklaceOfFaith -> {
                    player.equipment[EquipmentType.AMULET.id] = null
                    player.getSkills().alterCurrentLevel(
                        skill = Skills.PRAYER,
                        value = player.getSkills().getBaseLevel(Skills.PRAYER) / 4,
                    )
                    player.message("Your necklace of faith restores your prayer, then crumbles to dust.")
                }
            }

            /*
             * Re-read the hp: a phoenix necklace that just fired may well have pulled the player
             * back above the ring's threshold, which is what stops both from being spent on one hit.
             */
            if (player.getCurrentHp() > player.getMaxHp() / 10) {
                return@onPlayerLowHealth
            }
            if (player.getEquipment(EquipmentType.RING)?.id != ringOfLife) {
                return@onPlayerLowHealth
            }
            // The ring of life is one of the few teleports that works to level 30 Wilderness, the
            // same allowance TeleportType.GLORY carries - but above that it simply does not save.
            if (player.tile.getWildernessLevel() > TeleportType.GLORY.wildLvlRestriction) {
                return@onPlayerLowHealth
            }

            player.equipment[EquipmentType.RING.id] = null
            player.message("Your Ring of Life saves you and is destroyed in the process.")

            val instancedMap = world.instanceAllocator.getMap(player.tile)
            val destination =
                when {
                    instancedMap != null -> instancedMap.exitTile
                    player.attr[RING_OF_LIFE_ARDOUGNE_RESPAWN_ATTR] == true -> ARDOUGNE_RESPAWN
                    else -> world.gameContext.home
                }
            player.teleport(destination, TeleportType.GLORY)
        }
    }

    private companion object {
        /**
         * Aemad's shop tile, the one wiki-verified East Ardougne market coordinate already in this
         * codebase - reused from `DefenceCapePlugin`, which faced the same choice for the same
         * medium-Ardougne-diary destination.
         */
        private val ARDOUGNE_RESPAWN = Tile(2614, 3293)
    }
}
