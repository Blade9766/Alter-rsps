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

class VeracPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        spawnNpc("npc.verac_the_defiled", 3557, 3297, 0, 2)
        spawnNpc("npc.verac_the_defiled", 3559, 3300, 0, 2)
        spawnNpc("npc.verac_the_defiled", 3555, 3297, 0, 2)
        spawnNpc("npc.verac_the_defiled", 3555, 3294, 0, 2)
        spawnNpc("npc.verac_the_defiled", 3559, 3294, 0, 2)

        setCombatDef("npc.verac_the_defiled") {
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
                attackMagic = -42
                attackRanged = -14
                strengthBonus = 72
                defenceStab = 227
                defenceSlash = 230
                defenceCrush = 221
                defenceMagic = 0
                defenceRanged = 225
            }

            anims {
                // Was 729 (STUN_SPELL_CAST) with block 2079 - and 2079 is
                // HUMAN_AHRIMS_STAFF_DEFEND, i.e. this brother was animating as
                // Ahrim. Verac has both a named attack and defend animation.
                attack = Animation.HUMAN_VERACS_FLAIL_ATTACK
                block = Animation.HUMAN_VERACS_FLAIL_DEFEND
                death = 2925
            }
        }
    }
}
