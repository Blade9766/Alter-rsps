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

class KarilPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        spawnNpc("npc.karil_the_tainted", 3565, 3275, 0, 2)
        spawnNpc("npc.karil_the_tainted", 3563, 3272, 0, 2)
        spawnNpc("npc.karil_the_tainted", 3563, 3278, 0, 2)
        spawnNpc("npc.karil_the_tainted", 3567, 3272, 0, 2)
        spawnNpc("npc.karil_the_tainted", 3567, 3278, 0, 2)

        setCombatDef("npc.karil_the_tainted") {
            configs {
                attackSpeed = 4
                respawnDelay = 50
            }

            stats {
                hitpoints = 100
                // Karil is the ranged brother: Ranged 100, Magic 1. This block was a
                // copy of Ahrim's (the mage), so his Ranged level sat at the DSL
                // default of 1 and his ranged attack rolled against almost nothing.
                ranged = 100
                magic = 1
                defence = 100
            }

            // Equipment bonuses from the monster infobox. There was no bonuses
            // block at all before, so every one of these sat at 0 - the brothers
            // wear full Barrows armour and defended like they were naked.
            bonuses {
                attackMagic = -26
                attackRanged = 134
                // Karil is the ranged brother, so his damage comes from ranged strength
                // (wiki rngbns 55), not melee strength. Both are supported separately.
                strengthBonus = 0
                rangedStrengthBonus = 55
                defenceStab = 79
                defenceSlash = 71
                defenceCrush = 90
                defenceMagic = 106
                defenceRanged = 100
            }

            // Karil is the ranged brother, but every npc defaulted to CombatClass.MELEE
            // and there was no way to say otherwise, so he walked into melee range and
            // punched. This makes him actually shoot: bolt rack drawback, bolt in
            // flight.
            ranged {
                projectile = Graphic.BOLT_PROJECTILE
                drawback = Graphic.BOLT_RACK_DRAWBACK
                type = ProjectileType.BOLT
            }

            anims {
                // Was 729 (STUN_SPELL_CAST) with block 2079 - and 2079 is
                // HUMAN_AHRIMS_STAFF_DEFEND, i.e. this brother was animating as
                // Ahrim. No named crossbow defend exists, so the generic HUMAN_DEFEND is used.
                attack = Animation.HUMAN_KARILS_CROSSBOW_ATTACK
                block = Animation.HUMAN_DEFEND
                death = 2925
            }
        }
    }
}
