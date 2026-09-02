package org.alter.plugins.content.combat

import org.alter.api.HitType
import org.alter.api.ProjectileType
import org.alter.api.ext.hit
import org.alter.game.model.Tile
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.PawnHit
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.entity.Projectile
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.timer.ACTIVE_COMBAT_TIMER
import org.alter.plugins.content.combat.formula.CombatFormula
import java.lang.ref.WeakReference

/**
 * @author Tom <rspsmods@gmail.com>
 */

fun Pawn.isAttacking(): Boolean = attr[COMBAT_TARGET_FOCUS_ATTR]?.get() != null

fun Pawn.isBeingAttacked(): Boolean = timers.has(ACTIVE_COMBAT_TIMER)

fun Pawn.getCombatTarget(): Pawn? = attr[COMBAT_TARGET_FOCUS_ATTR]?.get()

fun Pawn.setCombatTarget(target: Pawn) = target.also { attr[COMBAT_TARGET_FOCUS_ATTR] = it as WeakReference<Pawn> }

fun Pawn.removeCombatTarget() = attr.remove(COMBAT_TARGET_FOCUS_ATTR)

fun Pawn.canEngageCombat(target: Pawn): Boolean = Combat.canEngage(this, target)

fun Pawn.canAttack(
    target: Pawn,
    combatClass: CombatClass,
): Boolean = Combat.canAttack(this, target, combatClass)

fun Pawn.isAttackDelayReady(): Boolean = Combat.isAttackDelayReady(this)

fun Pawn.combatRaycast(
    target: Pawn,
    distance: Int,
    projectile: Boolean,
): Boolean = Combat.raycast(this, target, distance, projectile)

suspend fun Pawn.canAttackMelee(
    it: QueueTask,
    target: Pawn,
    moveIfNeeded: Boolean,
): Boolean =
    Combat.areBordering(tile.x, tile.z, getSize(), getSize(), target.tile.x, target.tile.z, target.getSize(), target.getSize()) ||
        moveIfNeeded && moveToAttackRange(it, target, distance = 0, projectile = false)

fun Pawn.dealHit(
    target: Pawn,
    formula: CombatFormula,
    delay: Int,
    onHit: (PawnHit) -> Unit = {},
): PawnHit {
    val accuracy = formula.getAccuracy(this, target)
    val maxHit = formula.getMaxHit(this, target)
    val landHit = accuracy >= world.randomDouble()
    return dealHit(target, maxHit, landHit, delay, onHit)
}

fun Pawn.dealHit(
    target: Pawn,
    maxHit: Int,
    landHit: Boolean,
    delay: Int,
    onHit: (PawnHit) -> Unit = {},
): PawnHit {
    val damage = if (landHit) world.random(maxHit) else 0
    return dealExactHit(
        target = target,
        damage = damage,
        landHit = landHit,
        delay = delay,
        maxHit = landHit && damage == maxHit,
        onHit = onHit,
    )
}

/**
 * Deals an already-rolled [damage] instead of rolling `0..maxHit` internally.
 *
 * Needed by anything that adjusts the *rolled* damage rather than the max hit:
 * enchanted bolt effects add a flat bonus on top of the roll (opal, pearl,
 * dragonstone) or replace it outright (ruby's percentage of the target's current
 * hitpoints), neither of which can be expressed by scaling a max hit.
 *
 * [landHit] is kept separate from [damage] because a landed attack that rolls a 0 is
 * still a landed attack - it renders as a block splat but counts as a hit for
 * everything reading [PawnHit.landed].
 */
fun Pawn.dealExactHit(
    target: Pawn,
    damage: Int,
    landHit: Boolean,
    delay: Int,
    maxHit: Boolean = false,
    onHit: (PawnHit) -> Unit = {},
): PawnHit {
    val hit =
        if (landHit) {
            if (maxHit && this@dealExactHit is Player) {
                target.hit(damage = damage, type = HitType.HIT_MAX, delay = delay, attackersIndex = this.index) // maxhit type
            } else {
                target.hit(damage = damage, delay = delay, attackersIndex = this.index)
            }
        } else {
            target.hit(damage = 0, type = HitType.BLOCK, delay = delay, attackersIndex = this.index)
        }

    val pawnHit = PawnHit(hit, landHit)

    hit.setCancelIf { isDead() }
    hit.addAction { onHit(pawnHit) }
    hit.addAction {
        val pawn = this@dealExactHit
        Combat.postDamage(pawn, target)
    }
    if (landHit) {
        hit.addAction {
            val pawn = this@dealExactHit
            target.damageMap.add(pawn, hit.hitmarks.sumOf { it.damage })
        }
    }
    return pawnHit
}

suspend fun Pawn.moveToAttackRange(
    it: QueueTask,
    target: Pawn,
    distance: Int,
    projectile: Boolean,
): Boolean = Combat.moveToAttackRange(it, this, target, distance, projectile)

fun Pawn.postAttackLogic(target: Pawn) = Combat.postAttack(this, target)

fun Pawn.createProjectile(
    target: Tile,
    gfx: Int,
    type: ProjectileType,
    endHeight: Int = -1,
): Projectile {
    val builder =
        Projectile.Builder()
            .setTiles(start = tile, target = target)
            .setGfx(gfx = gfx)
            .setHeights(startHeight = type.startHeight, endHeight = if (endHeight != -1) endHeight else type.endHeight)
            .setSlope(angle = type.angle, steepness = type.steepness)
            .setTimes(delay = type.delay, lifespan = type.delay + Combat.getProjectileLifespan(this, target, type))

    return builder.build()
}

fun Pawn.createProjectile(
    target: Pawn,
    gfx: Int,
    type: ProjectileType,
    endHeight: Int = -1,
): Projectile {
    val builder =
        Projectile.Builder()
            .setTiles(start = tile, target = target)
            .setGfx(gfx = gfx)
            .setHeights(startHeight = type.startHeight, endHeight = if (endHeight != -1) endHeight else type.endHeight)
            .setSlope(angle = type.angle, steepness = type.steepness)
            .setTimes(delay = type.delay, lifespan = type.delay + Combat.getProjectileLifespan(this, target.tile, type))

    return builder.build()
}
