package org.alter.plugins.content.combat

import org.alter.api.EquipmentType
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR
import org.alter.game.model.attr.FACING_PAWN_ATTR
import org.alter.game.model.attr.INTERACTING_PLAYER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.move.MovementQueue.StepType
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.move.stopMovement
import org.alter.game.model.move.walkRoute
import org.alter.game.model.move.isRooted
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.MeleeCombatStrategy
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.plugins.content.interfaces.attack.AttackTab
import java.util.*

class CombatPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        setCombatLogic {
            pawn.attr[COMBAT_TARGET_FOCUS_ATTR]?.get()?.let { target ->
                pawn.facePawn(target)
            }
            pawn.queue {
                while (true) {
                    // @TODO Npc can follow player up to 16 tiles from spawn point, some npc will have exceptional range so property for overwrite should be added.
                    if (!cycle(pawn, this)) {
                        break
                    }
                    wait(1)
                }
            }
        }

        onPlayerOption("Attack") {
            val target = pawn.attr[INTERACTING_PLAYER_ATTR]?.get() ?: return@onPlayerOption
            player.attack(target)
        }
    }

    /**
     * @TODO Bigger creatures seem to have bugged range + their route finding sucks due to conditions given.
     */
    suspend fun cycle(pawn: Pawn, queue: QueueTask): Boolean {
        val target = pawn.getCombatTarget() ?: return false
        val strategy = CombatConfigs.getCombatStrategy(pawn)
        val attackRange = strategy.getAttackRange(pawn)
        var routeLogic = 1
        if (target != pawn.attr[FACING_PAWN_ATTR]?.get()) {
            return false
        }
        if (pawn.entityType.isNpc) {
            routeLogic = (pawn as Npc).routeLogic
        }
        var reached = world.reachStrategy.reached(
            flags = world.collision,
            level = pawn.tile.height,
            srcX = pawn.tile.x ,
            srcZ = pawn.tile.z,
            destX = target.tile.x,
            destZ = target.tile.z,
            destWidth = target.getSize(),
            destLength = target.getSize(),
            srcSize = pawn.getSize(),
            locShape = -2
        )
        /*
         * Being close enough is not the same as being able to see the target. This
         * used to declare the target reached on distance alone, which is why an npc
         * would happily shoot, cast at, or swing through a fence, a wall or a closed
         * door: nothing anywhere in the combat path tested line of sight, so the only
         * thing standing between an "unreachable" player and a full attack loop was
         * how many tiles away they stood.
         *
         * Melee uses line of *walk* (if you cannot step there you cannot swing there);
         * ranged and magic use line of *sight*, which is the looser test objects opt
         * into for projectiles - shooting over a low fence stays legal, shooting
         * through a wall does not.
         *
         * This has to be settled *before* a route is asked for. `reachStrategy.reached`
         * only ever answers the melee question - are the two footprints bordering - so
         * routing on it alone sends a pawn that can already shoot walking towards melee
         * distance. The route was thrown away again by the `stopMovement` below, but the
         * pawn still paid for the pathfind every tick and still had a walk flag planted
         * on the target, and any tick the range test came back false for another reason
         * left it walking in for real.
         *
         * When sight is blocked the pawn is deliberately left un-reached rather than
         * having its combat reset, so it walks the route issued below and re-engages the
         * moment it comes round the obstacle. A target that cannot be pathed to at all -
         * a genuine safespot - simply leaves the pawn standing there facing them, which
         * is the intended outcome.
         */
        val projectileAttack = strategy !== MeleeCombatStrategy
        if (!reached &&
            Combat.edgeDistance(pawn, target) <= attackRange &&
            Combat.hasAttackLineOfSight(pawn, target, projectile = projectileAttack)
        ) {
            reached = true
        }
        if (reached) {
            pawn.stopMovement()
        } else if (!pawn.isRooted()) {
            /*
             * A frozen or stunned pawn stays where it is. [org.alter.game.model.move.walkRoute]
             * refuses it anyway, but the chase is the one caller that runs every tick, so the
             * route is not worth finding in the first place.
             */
            when (routeLogic) {
                1 -> {
                    val route = world.smartRouteFinder.findRoute(
                        level = pawn.tile.height,
                        srcX = pawn.tile.x,
                        srcZ = pawn.tile.z,
                        destX = target.tile.x,
                        destZ = target.tile.z,
                        locShape = -2,
                        destWidth = target.getSize(),
                        destLength = target.getSize()
                    )
                    pawn.walkRoute(route, StepType.NORMAL)
                }
                0 -> {
                    val route = LinkedList<Tile>()
                    val destination = world.dumbRouteFinder.naiveDestination(
                        sourceX = pawn.tile.x,
                        sourceZ = pawn.tile.z,
                        sourceWidth = pawn.getSize(),
                        sourceLength = pawn.getSize(),
                        targetX = target.tile.x,
                        targetZ = target.tile.z,
                        targetWidth = target.getSize(),
                        targetLength = target.getSize()
                    )
                    val dx = destination.x - pawn.tile.x
                    val dz = destination.z - pawn.tile.z
                    // Try diagonal move (both x and z)
                    val diagonalMove = Tile(pawn.tile.x + dx.coerceIn(-1, 1), pawn.tile.z + dz.coerceIn(-1, 1))
                    if (!world.canTraverse(pawn.tile, Direction.between(pawn.tile, diagonalMove), pawn, pawn.getSize())) {
                        // If diagonal blocked, try horizontal (east/west)
                        val horizontalMove = Tile(pawn.tile.x + dx.coerceIn(-1, 1), pawn.tile.z)
                        if (!world.canTraverse(pawn.tile, Direction.between(pawn.tile, horizontalMove), pawn, pawn.getSize())) {
                            // If horizontal blocked, try vertical (north/south)
                            val verticalMove = Tile(pawn.tile.x, pawn.tile.z + dz.coerceIn(-1, 1))
                            if (world.canTraverse(pawn.tile, Direction.between(pawn.tile, verticalMove), pawn, pawn.getSize())) {
                                route.add(verticalMove)
                            }
                        } else {
                            route.add(horizontalMove)
                        }
                    } else {
                        route.add(diagonalMove)
                    }
                    if (route.isEmpty()) {
                        pawn.forceChat("Broke")
                        return true
                    }
                    pawn.walkRoute(route, stepType = StepType.NORMAL)
                }
            }
        }
        if (pawn.hasMoveDestination() || !reached) {
            if (!target.isAlive()) {
                return false
            }
            return cycle(pawn, queue)
        }
        if (!Combat.canEngage(pawn, target)) {
            Combat.reset(pawn)
            pawn.resetFacePawn()
            return false
        }
        if (!pawn.lock.canAttack()) {
            Combat.reset(pawn)
            return false
        }
        if (pawn is Player) {
            pawn.setVarp(Combat.PRIORITY_PID_VARP, target.index)
            if (!pawn.attr.has(Combat.CASTING_SPELL) && pawn.getVarbit(Combat.SELECTED_AUTOCAST_VARBIT) != 0) {
                val spell =
                    CombatSpell.values.firstOrNull { it.autoCastId == pawn.getVarbit(Combat.SELECTED_AUTOCAST_VARBIT) }
                if (spell != null) {
                    pawn.attr[Combat.CASTING_SPELL] = spell
                }
            }
        }
        if (target != pawn.attr[FACING_PAWN_ATTR]?.get()) {
            return false
        }
        if (Combat.isAttackDelayReady(pawn)) {
            if (Combat.canAttack(pawn, target, strategy)) {
                if (pawn is Player && AttackTab.isSpecialEnabled(pawn) && pawn.getEquipment(EquipmentType.WEAPON) != null) {
                    AttackTab.disableSpecial(pawn)
                    if (SpecialAttacks.execute(pawn, target, world)) {
                        Combat.postAttack(pawn, target)
                        return true
                    }
                    pawn.message("You don't have enough power left.")
                }
                strategy.attack(pawn, target)
                Combat.postAttack(pawn, target)
            } else {
                Combat.reset(pawn)
                return false
            }
        }
        return true
    }
}
