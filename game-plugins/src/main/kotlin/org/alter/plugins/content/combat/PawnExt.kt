package org.alter.plugins.content.combat

import org.alter.api.HitType
import org.alter.api.ProjectileType
import org.alter.api.ext.hit
import org.alter.api.ext.playSound
import org.alter.game.model.Tile
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR
import org.alter.game.model.attr.ONE_HIT_KILL_ATTR
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.PawnHit
import org.alter.game.model.entity.AreaSound
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.entity.Projectile
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.timer.ACTIVE_COMBAT_TIMER
import org.alter.plugins.content.combat.formula.CombatFormula
import org.alter.plugins.content.items.jewellery.RingOfRecoil
import org.alter.plugins.content.mechanics.poison.CombatPoison
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
    /*
     * Owner-only one-hit-kill cheat. Every attack in the game funnels through here, so this is the
     * one place it has to be applied for specials and multi-hit attacks to obey it too.
     *
     * The damage sent is the target's *max* hp rather than its current hp: the hit may only land
     * several cycles from now, and [Pawn.hitsCycle] clamps a hitmark down to whatever hp is left
     * when it does, so an over-large number always resolves to exactly a killing blow.
     */
    val oneHitKill = this is Player && target is Npc && attr[ONE_HIT_KILL_ATTR] == true
    val dealtDamage = if (oneHitKill) target.getMaxHp().coerceAtLeast(1) else damage
    val landed = landHit || oneHitKill

    val hit =
        if (landed) {
            if (maxHit && this@dealExactHit is Player) {
                target.hit(damage = dealtDamage, type = HitType.HIT_MAX, delay = delay, attackersIndex = this.index) // maxhit type
            } else {
                target.hit(damage = dealtDamage, delay = delay, attackersIndex = this.index)
            }
        } else {
            target.hit(damage = 0, type = HitType.BLOCK, delay = delay, attackersIndex = this.index)
        }

    val pawnHit = PawnHit(hit, landed)

    hit.setCancelIf { isDead() }
    hit.addAction { onHit(pawnHit) }
    hit.addAction {
        val pawn = this@dealExactHit
        Combat.postDamage(pawn, target)
    }
    if (landed) {
        hit.addAction {
            val pawn = this@dealExactHit
            target.damageMap.add(pawn, hit.hitmarks.sumOf { it.damage })
        }
    }

    /*
     * Poison - a player's poisoned weapons and ammo, the abyssal tentacle and the smoke spells, and
     * an npc's own poisonDamage. Every attack in the game funnels through here, which is what makes
     * this the one place it has to be wired: specials and multi-projectile attacks get it for free,
     * and each of their hits is its own chance to poison, as in OSRS.
     *
     * The source is resolved *now*, while the attack is still being thrown, but rolled when the hit
     * resolves - the weapon, spell or npc that threw the attack is the one that poisons, not
     * whatever happens to be equipped when the projectile arrives.
     *
     * Deliberately outside the landHit branch above: npc poison applies whether or not the attack
     * did any damage, and CombatPoison.apply is what decides which of the two rules to use.
     */
    val poison = CombatPoison.sourceFor(this)
    if (poison != null) {
        hit.addAction { CombatPoison.apply(this@dealExactHit, target, poison, landed = landed) }
    }

    /*
     * Ring of recoil, wired here for the same reason as poison: every attack funnels through this
     * function, so specials and multi-hit attacks recoil per hit without each of them knowing about
     * the ring. Rolled when the hit resolves, off the damage actually dealt - a hit that lands for
     * zero recoils nothing, which is the published rule.
     */
    if (landed && target is Player) {
        hit.addAction {
            RingOfRecoil.apply(
                attacker = this@dealExactHit,
                target = target,
                damage = hit.hitmarks.sumOf { it.damage },
            )
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
            .setTimes(delay = type.delay, lifespan = Combat.getProjectileLifespan(this, target, type))

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
            .setTimes(delay = type.delay, lifespan = Combat.getProjectileLifespan(this, target.tile, type))

    return builder.build()
}

/**
 * Plays the attack clip an npc's combat def carries, to whoever it is swinging at.
 *
 * `MonsterAnimationsPlugin` fills `defaultAttackSound` for every npc it can - from an explicit entry
 * in `named-combat-media.json`, the animation's own frame sound, or `WeaponSounds` - but only
 * [org.alter.plugins.content.combat.strategy.MeleeCombatStrategy] and
 * [org.alter.plugins.content.combat.strategy.RangedCombatStrategy] ever read it back. **A monster
 * that attacks through its own `CombatStrategy` or an `onNpcCombat` loop therefore swings in
 * silence**, however carefully its sound was sourced.
 *
 * That gap was real and had been shipped: the aberrant spectre carries `attackSound = 272` in its own
 * combat def and had never once played it, and the Chaos Elemental had a full sourced set it could
 * not reach. This exists so a bespoke attack can opt back in with one line.
 *
 * It is a straight extraction of the block the two ordinary strategies had a copy of each, so the
 * behaviour - area sound versus a clip played to the target, and the radius and volume used - is
 * unchanged for them.
 */
fun Npc.playAttackSound(target: Pawn) {
    val def = combatDef
    if (def.defaultAttackSound <= 0) {
        return
    }
    if (def.defaultAttackSoundArea) {
        world.spawn(AreaSound(tile, def.defaultAttackSound, def.defaultAttackSoundRadius, def.defaultAttackSoundVolume))
    } else if (target is Player) {
        target.playSound(def.defaultAttackSound, def.defaultAttackSoundVolume)
    }
}
