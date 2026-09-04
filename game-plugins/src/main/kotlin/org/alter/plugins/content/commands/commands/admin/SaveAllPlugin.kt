package org.alter.plugins.content.commands.commands.admin

import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.saving.PlayerSaving

/**
 * `::saveall` - writes every online player to disk immediately.
 *
 * There is an autosave on a timer and a shutdown hook, but neither helps when the server is stopped
 * the way it usually is during development: a JVM with no console can only be terminated forcefully
 * on Windows, which runs no shutdown hook. Running this before a restart makes the stop lossless.
 */
class SaveAllPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onCommand("saveall", Privilege.ADMIN_POWER, description = "Save every online player to disk now") {
            val saved = PlayerSaving.saveAll(world)
            player.message("Saved $saved player(s).")
        }
    }
}
