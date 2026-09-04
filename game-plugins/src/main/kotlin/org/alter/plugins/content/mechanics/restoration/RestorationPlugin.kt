package org.alter.plugins.content.mechanics.restoration

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*

class RestorationPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        onLogin {
            player.timers[Restoration.HITPOINT_REGEN] = Restoration.hitpointInterval(player)
            player.timers[Restoration.STAT_RESTORE] = Restoration.RESTORE_INTERVAL
        }

        onTimer(Restoration.HITPOINT_REGEN) {
            // Re-read the interval each cycle so equipping a regen bracelet takes effect.
            player.timers[Restoration.HITPOINT_REGEN] = Restoration.hitpointInterval(player)
            Restoration.regenHitpoints(player)
        }

        onTimer(Restoration.STAT_RESTORE) {
            player.timers[Restoration.STAT_RESTORE] = Restoration.RESTORE_INTERVAL
            Restoration.restoreStats(player)
        }
    }
}
