package org.alter.plugins.content.items.skillcapes

import org.alter.api.EquipmentType
import org.alter.api.ext.getWildernessLevel
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.LockState
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.DEFENCE_CAPE_ARDOUGNE_RESPAWN_ATTR
import org.alter.game.model.attr.DEFENCE_CAPE_EFFECT_ATTR
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.Plugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.magic.TeleportType
import org.alter.plugins.content.magic.teleport

/**
 * The Defence cape's "Toggle Effect" - the one skillcape perk this project's [SkillCape] comment
 * flagged as content that "does not exist here yet". It does now: the cape works as a wearable,
 * reusable Ring of Life.
 *
 * Real mechanics, from the wiki: the effect fires the first time a hit leaves the wearer above
 * 0 hp but at or below 10% of max hp - a single hit from above that threshold straight to 0 is
 * *not* saved, only a hit that leaves them alive but critical. It teleports to the respawn point
 * (or East Ardougne, if the second option - "Toggle Respawn" - is on), works up to level 30
 * Wilderness like a Ring of Life or Amulet of Glory, and does not itself cure poison or prevent
 * death by any other means.
 *
 * Both options are free toggles here rather than gated behind the medium Ardougne diary the real
 * game uses for "Toggle Respawn" - there is no diary system in this project, and stubbing the gate
 * would only make the second option permanently unreachable, the same call already made for
 * Oziach's Dragon Slayer gate and Sir Vyvin's armoury rank.
 *
 * The Ardougne destination is Aemad's shop tile (2614, 3293) - the one wiki-verified East
 * Ardougne market coordinate already in this codebase (`AemadPlugin`), reused rather than a new
 * guess at the diary reward's exact tile, which the wiki does not publish.
 *
 * Unlike a Ring of Life, the cape is never consumed - it is a worn cape, not a charge item - so
 * it can save the wearer repeatedly. Re-triggering mid-save is not possible: [teleport] locks the
 * pawn with [LockState.FULL_WITH_DAMAGE_IMMUNITY] synchronously before the escape even finishes
 * queuing, and [onPlayerLowHealth] only acts while the lock is [LockState.NONE].
 */
class DefenceCapePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    companion object {
        private val DEFENCE_CAPES = arrayOf("item.defence_cape", "item.defence_capet")

        /** Aemad's shop tile - see the class comment for why this tile specifically. */
        private val ARDOUGNE_RESPAWN = Tile(2614, 3293)
    }

    init {
        onEquipmentOption("item.defence_cape", option = "Toggle Effect") { toggleEffect() }
        onEquipmentOption("item.defence_capet", option = "Toggle Effect") { toggleEffect() }
        onEquipmentOption("item.defence_cape", option = "Toggle Respawn") { toggleRespawn() }
        onEquipmentOption("item.defence_capet", option = "Toggle Respawn") { toggleRespawn() }

        onPlayerLowHealth {
            if (player.attr[DEFENCE_CAPE_EFFECT_ATTR] != true) {
                return@onPlayerLowHealth
            }
            if (!player.hasEquipped(EquipmentType.CAPE, *DEFENCE_CAPES)) {
                return@onPlayerLowHealth
            }
            if (player.lock != LockState.NONE) {
                return@onPlayerLowHealth
            }
            if (player.tile.getWildernessLevel() > 30) {
                return@onPlayerLowHealth
            }

            val instancedMap = world.instanceAllocator.getMap(player.tile)
            val destination = when {
                instancedMap != null -> instancedMap.exitTile
                player.attr[DEFENCE_CAPE_ARDOUGNE_RESPAWN_ATTR] == true -> ARDOUGNE_RESPAWN
                else -> world.gameContext.home
            }

            player.message("Your cape saves you.")
            player.teleport(destination, TeleportType.GLORY)
        }
    }

    private fun Plugin.toggleEffect() {
        val enabled = !(player.attr[DEFENCE_CAPE_EFFECT_ATTR] ?: false)
        player.attr[DEFENCE_CAPE_EFFECT_ATTR] = enabled
        if (enabled) {
            player.message("Your cape's magical effect has been activated.")
        } else {
            player.message("Your cape's magical effect has been deactivated.")
        }
    }

    private fun Plugin.toggleRespawn() {
        val ardougne = !(player.attr[DEFENCE_CAPE_ARDOUGNE_RESPAWN_ATTR] ?: false)
        player.attr[DEFENCE_CAPE_ARDOUGNE_RESPAWN_ATTR] = ardougne
        if (ardougne) {
            player.message("Your cape will now save you to East Ardougne.")
        } else {
            player.message("Your cape will now save you to your respawn point.")
        }
    }
}
