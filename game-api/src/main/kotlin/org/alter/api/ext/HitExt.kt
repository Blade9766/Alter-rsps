package org.alter.api.ext

import org.alter.game.model.combat.PawnHit

fun PawnHit.landed(): Boolean = landed

fun PawnHit.blocked(): Boolean = !landed

/** Client cycles in one game tick - 600ms of ticks over 20ms cycles. */
private const val CLIENT_CYCLES_PER_TICK = 30

/**
 * When this hit lands, expressed in the 20ms client cycles that spotanim and projectile
 * delays are measured in.
 *
 * [org.alter.game.model.Hit.damageDelay] is in game ticks; this used to multiply by 50,
 * which stretched every delay it timed by two thirds.
 */
fun PawnHit.getClientHitDelay(): Int = hit.damageDelay * CLIENT_CYCLES_PER_TICK
