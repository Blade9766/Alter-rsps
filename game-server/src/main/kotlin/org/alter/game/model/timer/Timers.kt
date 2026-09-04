package org.alter.game.model.timer

/**
 * A decoupled file that holds TimerKeys that require read-access from our
 * game module. Any timer keys that can be stored on the plugin classes themselves,
 * should do so. When storing them in a class, remember the TimerKey must be
 * a singleton, meaning it should only have a single state.
 *
 * @author Tom <rspsmods@gmail.com>
 */

/**
 * A timer for npcs to reset their pawn face attribute.
 */
internal val RESET_PAWN_FACING_TIMER = TimerKey()

/**
 * A timer for removing a skull icon.
 */
val SKULL_ICON_DURATION_TIMER = TimerKey()

/**
 * Timer key set when a pawn is attacked either in PvP or in PvM.
 */
val ACTIVE_COMBAT_TIMER = TimerKey()

/**
 * Timer key used to force a player disconnect, usually used so that if a
 * player's channel has been inactive (disconnected) for X amount of time,
 * we disconnect them so that they can play again.
 */
val FORCE_DISCONNECTION_TIMER = TimerKey()

/**
 * Timer key set when frozen.
 */
val FROZEN_TIMER = TimerKey()

/**
 * Timer key set when stunned.
 */
val STUN_TIMER = TimerKey()

/**
 * Timer key set while a pawn's overhead protection prayers are being ignored by incoming attacks.
 *
 * The dragon scimitar's Sever special is the only thing that sets it: the prayer stays on and its
 * icon stays up - the target has not lost the prayer, attacks are simply going straight through it
 * for the duration.
 */
val PROTECTION_PRAYER_BLOCK_TIMER = TimerKey()

/**
 * Timer keys for the two temporary damage modifiers special attacks apply.
 *
 * Both are the lifetime of an attribute rather than an effect in themselves: the multiplier lives on
 * `Combat.DAMAGE_TAKE_MULTIPLIER` / `Combat.MELEE_DAMAGE_TAKE_MULTIPLIER`, and
 * `SpecialAttackTemporariesPlugin` clears it when the matching timer lapses. Kept as timers so they
 * tick down on npcs too - the plugin's `onTimer` fires for any pawn.
 */
val DAMAGE_TAKEN_MODIFIER_TIMER = TimerKey()
val MELEE_DAMAGE_TAKEN_MODIFIER_TIMER = TimerKey()

/**
 * Timer key for poison ticks.
 */
val POISON_TIMER = TimerKey()

/**
 * Timer key for dragonfire protection ticking down.
 */
val ANTIFIRE_TIMER = TimerKey()

/**
 * Timer key for the delay in between a pawn's attack.
 */
val ATTACK_DELAY = TimerKey()

/**
 * Timer key for delay in between drinking potions.
 */
val POTION_DELAY = TimerKey()

/**
 * Timer key for delay in between eating food.
 */
val FOOD_DELAY = TimerKey()

/**
 * Timer key for delay in between eating "combo" food.
 */
val COMBO_FOOD_DELAY = TimerKey()

/**
 * Timer key for delay in between burying bones.
 */
val BURY_BONE_DELAY = TimerKey()

/**
 * Timer key for delay in between burying bones.
 */
val BONE_OFFER_DELAY = TimerKey()

/**
 * Ticks a pawn's outstanding burn damage down.
 *
 * See [org.alter.game.model.attr.SPECIAL_ATTACK_BURN_ATTR] for why burn is a counter with a timer
 * rather than a set of queued hits.
 */
val SPECIAL_ATTACK_BURN_TIMER = TimerKey()

/**
 * Counts down a weapon's built-up stacks - soul stacks on the soulreaper axe, sunlight stacks on
 * the sunlight spear - which fall off one at a time after fifty ticks without attacking.
 */
val WEAPON_STACK_DECAY_TIMER = TimerKey()
