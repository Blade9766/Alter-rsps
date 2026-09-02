package org.alter.plugins.content.npcs

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
import org.alter.plugins.content.combat.isBeingAttacked
import org.alter.rscm.RSCM.getRSCM

/**
 *  @author <a href="https://github.com/CloudS3c">Cl0ud</a>
 *  @author <a href="https://www.rune-server.ee/members/376238-cloudsec/">Cl0ud</a>
 *
 */
class CowPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        val cows = listOf("npc.cow")

        val lumbridgeCowSpawns =
            listOf(
                3255 to 3259,
                3257 to 3262,
                3259 to 3259,
                3262 to 3261,
                3263 to 3265,
                3260 to 3270,
                3258 to 3274,
                3261 to 3277,
                3256 to 3280,
                3262 to 3285,
                3261 to 3291,
                3256 to 3292,
                3253 to 3288,
                3248 to 3290,
                3248 to 3284,
                3253 to 3282,
            )

        lumbridgeCowSpawns.forEach { (x, z) ->
            spawnNpc(
                npc = "npc.cow",
                x = x,
                z = z,
                // Keep random destinations close to the spawn tile so cows
                // cannot select tiles on the far side of the pasture fence.
                walkRadius = 1,
                direction = Direction.SOUTH,
            )
        }

        val COW_YELL_DELAY = TimerKey()

        cows.forEach { cow ->
            onNpcSpawn(npc = cow) {
                val npc = npc
                npc.timers[COW_YELL_DELAY] = world.random(100..200)
            }
        }

        onTimer(COW_YELL_DELAY) {
            val npc = npc
            if (!npc.isBeingAttacked()) {
                npc.forceChat("Moo")
            }
            npc.timers[COW_YELL_DELAY] = world.random(100..200)
        }

        cows.forEach { cow ->
            setCombatDef(cow) {
                configs {
                    attackSpeed = 4
                    respawnDelay = 45
                    poisonChance = 0.0
                    venomChance = 0.0
                }
                stats {
                    hitpoints = 8
                    attack = 1
                    strength = 1
                    defence = 1
                    magic = 1
                    ranged = 1
                }

                bonuses {
                    defenceStab = -21
                    defenceSlash = -21
                    defenceCrush = -21
                    defenceMagic = -21
                    defenceRanged = -21
                }

                anims {
                    attack = Animation.COW_ATTACK
                    block = Animation.COW_HIT
                    death = Animation.COW_DEATH
                }

                sound {
                    attackSound = Sound.COW_ATTACK
                    blockSound = Sound.COW_HIT
                    deathSound = Sound.COW_DEATH
                }
            }

            onNpcDeath(cow) {
                val npc = npc
                val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return@onNpcDeath

                listOf("item.bones", "item.cowhide", "item.raw_beef").forEach { item ->
                    world.spawn(
                        GroundItem(
                            item = getRSCM(item),
                            amount = 1,
                            tile = npc.tile,
                            owner = killer,
                        ),
                    )
                }
            }
        }
    }
}
