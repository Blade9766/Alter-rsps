package org.alter.plugins.content.areas.wilderness

import org.alter.api.ext.inWilderness
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Binds [WildernessRisk.handleDeath] to dying in the Wilderness.
 *
 * The pre-death hook is the right one of the two: it runs while the player is still standing on
 * the tile they died on, before the respawn moves them, so the pile lands where the fight was.
 * `onPlayerDeath` fires after the walk back and would drop everything at the respawn point.
 *
 * The death is not claimed (`DEATH_HANDLED_ATTR` is left alone), so
 * [org.alter.game.action.PlayerDeathAction] still plays the animation, restores stats and
 * respawns as normal - all this adds is the item loss. That is the opposite of what
 * [org.alter.plugins.content.areas.duelarena.DuelArenaPlugin] does with the same hook, which
 * claims the death because a duel has its own idea of where the loser goes.
 */
class WildernessRiskPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onPlayerPreDeath {
            if (!player.inWilderness()) {
                return@onPlayerPreDeath
            }
            WildernessRisk.handleDeath(player, world)
        }
    }
}
