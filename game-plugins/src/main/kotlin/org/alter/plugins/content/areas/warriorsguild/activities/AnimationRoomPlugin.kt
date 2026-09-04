package org.alter.plugins.content.areas.warriorsguild.activities

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.areas.warriorsguild.WarriorsGuild
import org.alter.rscm.RSCM.getRSCM

/**
 * The animation room: feed a Magical Animator a matching set of armour, fight what stands up.
 *
 * The player needs a full helm, platebody and platelegs of one metal in their inventory. Clicking
 * `Animate` consumes them and spawns the suit, which attacks whoever created it. Killing it pays
 * warrior guild tokens and hands most or all of the armour back - see [AnimatedArmour] for the
 * per-tier numbers and which of them are extrapolated.
 *
 * ## What is not modelled
 *
 * **The suit does not flee at low health.** In OSRS it runs when nearly dead and stops fighting
 * while it runs, which is the reason the wiki tells players to have run mode on. Reproducing that
 * needs a flee behaviour the engine has no notion of - npcs here fight until they die - and faking
 * it with a teleport would be worse than leaving it out. The effect is that suits are slightly
 * easier to finish than in the real game.
 *
 * **The armour is consumed the moment the animator is used**, not held in escrow. So a player who
 * logs out mid-fight loses the set. That matches what the items are worth far better than the
 * alternative of handing them back on spawn would.
 */
class AnimationRoomPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        AnimatedArmour.values.forEach { armour ->
            setCombatDef(armour.npc) {
                configs {
                    attackSpeed = AnimatedArmour.ATTACK_SPEED
                    respawnDelay = AnimatedArmour.RESPAWN_CYCLES
                }
                stats {
                    hitpoints = armour.hitpoints
                    attack = armour.combatLevels
                    strength = armour.combatLevels
                    defence = armour.combatLevels
                    magic = 1
                    ranged = 1
                }
                anims {
                    attack = AnimatedArmour.ATTACK_ANIM
                    block = AnimatedArmour.NO_BLOCK_ANIM
                    death = AnimatedArmour.DEATH_ANIM
                }
            }

            onNpcSpawn(npc = armour.npc) { npc.combatStyle = CombatStyle.CRUSH }

            onNpcDeath(armour.npc) { onDeath(npc, armour) }
        }

        onObjOption(obj = ANIMATOR, option = "animate", lineOfSightDistance = 1) {
            animate(player)
        }
    }

    /**
     * Consumes the best set the player is carrying and stands it up.
     *
     * "Best" so that someone holding two sets gets the one worth more tokens, which is what they
     * came for - and so the choice is never ambiguous.
     */
    private fun animate(player: Player) {
        val armour =
            AnimatedArmour.values
                .filter { set -> set.pieces.all { player.inventory.contains(getRSCM(it)) } }
                .maxByOrNull { it.tokens }

        if (armour == null) {
            player.message("You need a full helm, platebody and platelegs of the same metal")
            player.message("in your inventory to animate a suit of armour.")
            return
        }

        armour.pieces.forEach { player.inventory.remove(it, 1) }

        val tile = player.tile.transform(0, 1)
        val suit = Npc(getRSCM(armour.npc), tile, world)
        suit.respawns = false
        world.spawn(suit)
        suit.combatStyle = CombatStyle.CRUSH
        suit.forceChat("I'm ALIVE!")
        suit.attack(player)

        player.message("The magical animator hums, and the armour rises to its feet.")
    }

    /**
     * Tokens always, then each armour piece at the suit's own return chance.
     *
     * Rolled per piece rather than per set: the wiki says "pieces will occasionally be lost", one
     * at a time, which is why a bronze suit can hand back two of three.
     */
    private fun onDeath(
        npc: Npc,
        armour: AnimatedArmour,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        world.spawn(
            GroundItem(item = getRSCM(WarriorsGuild.TOKEN), amount = armour.tokens, tile = npc.tile, owner = killer),
        )

        var lost = 0
        armour.pieces.forEach { piece ->
            if (world.randomDouble() <= armour.pieceReturnChance) {
                world.spawn(GroundItem(item = getRSCM(piece), amount = 1, tile = npc.tile, owner = killer))
            } else {
                lost++
            }
        }

        if (lost > 0) {
            killer.message("Some of the armour was destroyed in the fight.")
        }
    }

    private companion object {
        const val ANIMATOR = "object.magical_animator"
    }
}
