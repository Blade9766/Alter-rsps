package org.alter.plugins.content.mechanics.run

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

class RunEnergyPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onLogin {
            player.timers[RunEnergy.RUN_DRAIN] = 1
        }

        onTimer(RunEnergy.RUN_DRAIN) {
            player.timers[RunEnergy.RUN_DRAIN] = 1
            RunEnergy.drain(player)
        }

        /**
         * Button by minimap.
         */
        onButton(interfaceId = 160, component = 27) {
            RunEnergy.toggle(player)
        }

        /**
         * The "Toggle Run" button on the Settings tab.
         *
         * This used to be bound to 116:71, which is a graphic inside the Audio tab button and has no
         * op of its own, so it never fired. 30 is the button that actually carries "Toggle Run".
         */
        onButton(interfaceId = 116, component = 30) {
            RunEnergy.toggle(player)
        }
    }
}
