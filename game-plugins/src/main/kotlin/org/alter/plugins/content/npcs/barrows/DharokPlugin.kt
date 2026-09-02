package org.alter.plugins.content.npcs.barrows

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

class DharokPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        spawnNpc("npc.dharok_the_wretched", 3576, 3298, 0, 2)
        spawnNpc("npc.dharok_the_wretched", 3576, 3300, 0, 2)
        spawnNpc("npc.dharok_the_wretched", 3573, 3299, 0, 2)
        spawnNpc("npc.dharok_the_wretched", 3578, 3296, 0, 2)
        spawnNpc("npc.dharok_the_wretched", 3574, 3295, 0, 2)

        setCombatDef("npc.dharok_the_wretched") {
            configs {
                attackSpeed = 7
                respawnDelay = 50
            }

            stats {
                hitpoints = 100
                attack = 100
                strength = 100
                defence = 100
            }

            // Equipment bonuses from the monster infobox. There was no bonuses
            // block at all before, so every one of these sat at 0 - the brothers
            // wear full Barrows armour and defended like they were naked.
            bonuses {
                attackMagic = -58
                attackRanged = -18
                strengthBonus = 105
                defenceStab = 252
                defenceSlash = 250
                defenceCrush = 244
                defenceMagic = -11
                defenceRanged = 249
            }

            anims {
                // Already correct - 2067 is HUMAN_DHAROKS_GREATAXE_SWING. Named for clarity.
                attack = Animation.HUMAN_DHAROKS_GREATAXE_SWING
                block = Animation.HUMAN_DEFEND
                death = 2925
            }
        }
    }
}
