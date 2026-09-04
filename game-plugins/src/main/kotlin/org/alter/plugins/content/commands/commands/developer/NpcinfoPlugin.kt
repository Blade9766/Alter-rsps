package org.alter.plugins.content.commands.commands.developer

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR
import org.alter.game.model.attr.FACING_PAWN_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * `::npcinfo` - reports which npc you are actually looking at and what animations it will
 * play, for telling apart ids that share a name.
 *
 * Written for the Varrock guards, where seven ids all called "Guard" at combat level 21 are
 * dealt round-robin over 37 tiles and split into three different model sets - 11911/11912/
 * 11913, 11914/11915/11916 and 11917. Nothing in-game distinguishes them, so a report of
 * "the guards look wrong" could not be narrowed to a subset of ids without this.
 *
 * It reports the *live* `combatDef`, not the wired one, so it also catches an animation that
 * something replaced at spawn - `MonsterAnimationsPlugin` rewrites `combatDef` for any npc
 * without an explicit def, which is easy to forget when reading the plugin source alone.
 */
class NpcinfoPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onCommand("npcinfo", Privilege.DEV_POWER, description = "Report the id, models and animations of a nearby npc") {
            val target = targetOf(player)

            if (target == null) {
                player.message("No npc in combat with you, facing you, or within $SEARCH_RADIUS tiles.")
                return@onCommand
            }

            val def = target.def
            val combat = target.combatDef

            player.message("<col=801700>${def.name}</col> id <col=801700>${target.id}</col> lvl ${def.combatLevel} at ${target.tile.x},${target.tile.z},${target.tile.height}")
            player.message("rig: stand ${def.standAnim} walk ${def.walkAnim} rotBack ${def.rotateBackAnim}")
            player.message(
                "combat: attack ${combat.attackAnimation} block ${combat.blockAnimation} " +
                    "death ${combat.deathAnimation.joinToString(separator = "/")} speed ${combat.attackSpeed}",
            )
            player.message("models: ${def.models?.joinToString(separator = ", ") ?: "none"}")

            /*
             * Nothing in this engine writes an npc's occupancy into `world.collision`, so npcs
             * are invisible to pathing and two of them can stand on one tile. Two models drawn
             * on the same spot, each idling 808 from its own start phase, reads in-game as one
             * model whose head sways or jerks. Reporting the tile's occupants makes that
             * visible instead of leaving it to be guessed at from how the composite looks.
             */
            /*
             * A guard that looks like it is standing but is really walking a route it cannot
             * finish looks the same from outside as one that is genuinely idle. Reporting the
             * queue separates "idle model animating oddly" from "npc micro-stepping".
             */
            player.message(
                "state: moving ${if (target.hasMoveDestination()) "YES" else "no"} " +
                    "| spawn ${target.spawnTile.x},${target.spawnTile.z},${target.spawnTile.height} " +
                    "| walkRadius ${target.walkRadius} | facing ${target.attr[FACING_PAWN_ATTR]?.get() != null}",
            )

            val sharing = npcsOn(target)
            if (sharing.size > 1) {
                player.message("<col=ff0000>${sharing.size} npcs share this tile:</col> " + sharing.joinToString(separator = ", ") { "${it.def.name} ${it.id} (index ${it.index})" })
            } else {
                player.message("tile is occupied by this npc alone")
            }
        }
    }

    /**
     * Whatever the player is engaged with, else whatever is closest - so the command still
     * answers when you are not in combat, handy for checking a guard before provoking it.
     */
    private fun targetOf(player: Player): Npc? {
        (player.attr[COMBAT_TARGET_FOCUS_ATTR]?.get() as? Npc)?.let { return it }
        (player.attr[FACING_PAWN_ATTR]?.get() as? Npc)?.let { return it }

        var closest: Npc? = null
        var closestDistance = Int.MAX_VALUE
        world.npcs.forEach { npc ->
            if (npc.tile.height != player.tile.height) return@forEach
            val distance = npc.tile.getDistance(player.tile)
            if (distance <= SEARCH_RADIUS && distance < closestDistance) {
                closest = npc
                closestDistance = distance
            }
        }
        return closest
    }

    /** Every npc standing on [target]'s exact tile, including [target] itself. */
    private fun npcsOn(target: Npc): List<Npc> {
        val found = mutableListOf<Npc>()
        world.npcs.forEach { npc ->
            if (npc.tile.sameAs(target.tile)) {
                found.add(npc)
            }
        }
        return found
    }

    private companion object {
        const val SEARCH_RADIUS = 10
    }
}
