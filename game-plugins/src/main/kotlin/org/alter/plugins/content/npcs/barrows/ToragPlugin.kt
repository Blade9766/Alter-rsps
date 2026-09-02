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

class ToragPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        spawnNpc("npc.torag_the_corrupted", 3552, 3283, 0, 2)
        spawnNpc("npc.torag_the_corrupted", 3551, 3280, 0, 2)
        spawnNpc("npc.torag_the_corrupted", 3551, 3285, 0, 2)
        spawnNpc("npc.torag_the_corrupted", 3554, 3280, 0, 2)
        spawnNpc("npc.torag_the_corrupted", 3556, 3284, 0, 2)

        setCombatDef("npc.torag_the_corrupted") {
            configs {
                attackSpeed = 5
                respawnDelay = 50
            }

            stats {
                hitpoints = 100
                attack = 100
                strength = 100
                defence = 100
                // Melee brother: Magic 1, not 100. This stats block was a copy of
                // Ahrim's - the only mage of the six - which also left attack and
                // strength at the DSL default of 1 instead of 100.
                magic = 1
            }

            // Equipment bonuses from the monster infobox. There was no bonuses
            // block at all before, so every one of these sat at 0 - the brothers
            // wear full Barrows armour and defended like they were naked.
            bonuses {
                attackMagic = -33
                attackRanged = -11
                strengthBonus = 72
                defenceStab = 221
                defenceSlash = 235
                defenceCrush = 222
                defenceMagic = 0
                defenceRanged = 221
            }

            anims {
                // Was 729 (STUN_SPELL_CAST) with block 2079 - and 2079 is
                // HUMAN_AHRIMS_STAFF_DEFEND, i.e. this brother was animating as
                // Ahrim. No named hammers defend exists, so the generic HUMAN_DEFEND is used.
                attack = Animation.HUMAN_TORAGS_HAMMERS_SWING
                block = Animation.HUMAN_DEFEND
                death = 2925
            }
        }
    }
}
