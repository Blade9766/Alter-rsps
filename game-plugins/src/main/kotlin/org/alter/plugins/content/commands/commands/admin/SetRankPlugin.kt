package org.alter.plugins.content.commands.commands.admin

import org.alter.api.ext.getCommandArgs
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * `::setrank <player> <rank>` - grants a privilege level to an online player.
 *
 * There was no way to do this in game at all: a privilege lived only in the player's save file, so
 * the whole procedure was "log the account out, hand-edit `data/saves/details/<name>`, log back in"
 * - and doing it while the account was online silently achieved nothing, because logging out writes
 * the save back over the edit. That is a sharp edge to leave lying around on a dev server.
 *
 * The change applies immediately and is persisted by the player's next save.
 */
class SetRankPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onCommand("setrank", Privilege.OWNER_POWER, description = "Grant a privilege level to an online player") {
            val args = player.getCommandArgs()
            if (args.size < 2) {
                val ranks = world.privileges.joinToString(", ") { it.name }
                player.message("Usage: ::setrank <player> <rank> - ranks: $ranks")
                return@onCommand
            }

            val targetName = args[0]
            val rankName = args[1]

            val target = world.players.firstOrNull { it.username.equals(targetName, ignoreCase = true) }
            if (target == null) {
                player.message("No player named '$targetName' is online.")
                return@onCommand
            }

            val privilege = world.privileges.get(rankName)
            if (privilege == null) {
                val ranks = world.privileges.joinToString(", ") { it.name }
                player.message("No such rank: '$rankName'. Ranks: $ranks")
                return@onCommand
            }

            target.privilege = privilege
            player.message("Set ${target.username}'s rank to ${privilege.name}.")
            if (target != player) {
                target.message("Your rank has been set to ${privilege.name}.")
            }
        }
    }
}
