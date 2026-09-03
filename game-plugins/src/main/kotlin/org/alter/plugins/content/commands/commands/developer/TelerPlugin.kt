package org.alter.plugins.content.commands.commands.developer

import org.alter.api.ext.getCommandArgs
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.move.moveTo
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

class TelerPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onCommand("teler", Privilege.DEV_POWER, description = "Teleport to region") {
            val region = player.getCommandArgs().firstOrNull()?.toIntOrNull()
            if (region == null) {
                player.message("Usage: ::teler <region> - e.g. ::teler 13363")
                return@onCommand
            }
            val tile = Tile.fromRegion(region)
            player.moveTo(tile)
            player.message("Teleported to region $region ($tile).")
        }
    }
}
