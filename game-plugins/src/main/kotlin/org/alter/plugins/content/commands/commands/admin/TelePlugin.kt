package org.alter.plugins.content.commands.commands.admin

import org.alter.api.ext.getCommandArgs
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.move.moveTo
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

class TelePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onCommand("tele", Privilege.ADMIN_POWER, description = "Teleport to coordinates") {
            /*
             * Indexing straight into the args threw a raw stack trace at the console for anything
             * that was not exactly "::tele <x> <y>" - a bare "::tele" and a stray comma between the
             * coordinates being the two easy ways to get one - while telling the player nothing at
             * all. Commas are now tolerated and everything else explains itself.
             */
            val values = player.getCommandArgs().flatMap { it.split(',') }.filter { it.isNotBlank() }
            if (values.size < 2) {
                player.message("Usage: ::tele <x> <y> [height] - e.g. ::tele 3366 3272")
                return@onCommand
            }

            val x = values[0].toIntOrNull()
            val y = values[1].toIntOrNull()
            val height = if (values.size > 2) values[2].toIntOrNull() else 0
            if (x == null || y == null || height == null) {
                player.message("Usage: ::tele <x> <y> [height] - coordinates must be whole numbers.")
                return@onCommand
            }
            if (height !in 0..3) {
                player.message("Height must be 0-3.")
                return@onCommand
            }

            player.moveTo(x, y, height)
            player.message("Teleported to $x, $y, $height.")
        }
    }
}
