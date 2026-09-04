package org.alter.game.model.attr

import org.alter.game.model.container.ItemTransaction
import org.alter.game.model.entity.*
import org.alter.game.model.item.Item
import org.alter.game.model.shop.Shop
import java.lang.ref.WeakReference

/**
 * A decoupled file that holds AttributeKeys that require read-access from our
 * game module. Any attributes that can be stored on the plugin classes themselves,
 * should do so. When storing them in a class, remember the AttributeKey must be
 * a singleton, meaning it should only have a single state.
 *
 * @author Tom <rspsmods@gmail.com>
 */

/**
 * Indicates the last [Date.time] the player logged in
 *   Note| due to GSON/JSON limitation on types, storing [Long] as [String] instead
 *   despite imposed costs
 */
val LAST_LOGIN_ATTR = AttributeKey<String>("last_login")

/**
 * Indicates the amount of time the player has membership
 *   Note| due to GSON/JSON limitation on types, storing [Long] as [String] instead
 *   despite imposed costs
 */
val MEMBERS_EXPIRES_ATTR = AttributeKey<String>("members_expires")

/**
 * A flag which indicates if the player's account was just created/logged in for
 * the first time.
 */
val NEW_ACCOUNT_ATTR = AttributeKey<Boolean>()

/**
 * Indicates the last [Date.time] the player claimed a free bond (tradeable)
 *   Note| due to GSON/JSON limitation on types, storing [Long] as [String] instead
 *   despite imposed costs
 */
val FREE_BOND_CLAIMED_ATTR = AttributeKey<String>("bond_claimed")

/**
 * A flag which indicates if the player's appearance has been set by the player.
 * Opting for persistence and modifying on_login behavior this will allow OSRS-like
 * behavior such that player can logout and will still be allowed to set appearance
 * at next login. Additionally this flag can be set by admins to allow change on next login.
 */
val APPEARANCE_SET_ATTR = AttributeKey<Boolean>("appearance_set")

/**
 * A flag which indicates that the player will not take collision into account
 * when walking.
 */
val NO_CLIP_ATTR = AttributeKey<Boolean>()

/**
 * A flag that indicates whether or not this player has protect-item
 * prayer active.
 */
val PROTECT_ITEM_ATTR = AttributeKey<Boolean>()

/**
 * The display mode that the player has submitted as a message.
 */
val DISPLAY_MODE_CHANGE_ATTR = AttributeKey<Int>()

/**
 * The distance a [Pawm] keeps facing their [FACING_PAWN_ATTR].
 */
val RESET_FACING_PAWN_DISTANCE_ATTR = AttributeKey<Int>()

/**
 * The [Pawn] which another pawn is facing.
 */
val FACING_PAWN_ATTR = AttributeKey<WeakReference<Pawn>>()

/**
 * An [Npc] that has us as their [FACING_PAWN_ATTR].
 */
val NPC_FACING_US_ATTR = AttributeKey<WeakReference<Npc>>()

/**
 * The current viewed shop.
 */
val CURRENT_SHOP_ATTR = AttributeKey<Shop>()

/**
 * The [Pawn] which another pawn wants to initiate combat with, whether they meet
 * the criteria to attack or not (including being in attack range).
 */
val COMBAT_TARGET_FOCUS_ATTR = AttributeKey<WeakReference<Pawn>>()

/**
 * The npc that has claimed this pawn in **single-way combat** - the one aggressive monster allowed
 * to be fighting them at a time outside a multi-combat area.
 *
 * Written by [org.alter.plugins.content.mechanics.aggro.NpcAggroPlugin] the moment an aggressive
 * npc engages, and never cleared: it is validated on read instead, against whether the npc it names
 * is still active and still has this pawn as its own combat target. That is what makes it safe
 * without a clear-down path on every route out of combat - death, leashing, logout, teleporting
 * away and simply losing interest all drop the npc's combat target, and the claim goes stale with
 * it.
 *
 * Deliberately *not* consulted for retaliation. Being hit and hitting back is not the same thing as
 * a second monster picking you unprompted, and blocking it would leave a monster you attacked
 * standing there taking it.
 */
val SINGLE_COMBAT_ATTACKER_ATTR = AttributeKey<WeakReference<Pawn>>()

/**
 * The [Pawn] that killed another pawn.
 */
val KILLER_ATTR = AttributeKey<WeakReference<Pawn>>()

/**
 * The last [Pawn] that the owner of this attribute has hit.
 */
val LAST_HIT_ATTR = AttributeKey<WeakReference<Pawn>>()

/**
 * The last [Pawn] who has hit the owner of this attribute.
 */
val LAST_HIT_BY_ATTR = AttributeKey<WeakReference<Pawn>>()

/**
 * The amount of "poison ticks" left before the poison wears off.
 */
val POISON_TICKS_LEFT_ATTR = AttributeKey<Int>(persistenceKey = "poison_ticks_left", resetOnDeath = true)

/**
 * The amount of antifire potion charges left.
 */
val ANTIFIRE_POTION_CHARGES_ATTR = AttributeKey<Int>(persistenceKey = "antifire_potion_charges", resetOnDeath = true)

/**
 * If full dragonfire immunity is enabled.
 */
val DRAGONFIRE_IMMUNITY_ATTR = AttributeKey<Boolean>(persistenceKey = "dragonfire_immunity", resetOnDeath = true)

/**
 * Whether the Defence cape's "Toggle Effect" (its Ring of Life-style low-health save) is
 * switched on. Off by default, same as the ring it mirrors requiring no action to carry but
 * never activating unless equipped - the cape instead needs an explicit opt-in.
 */
val DEFENCE_CAPE_EFFECT_ATTR = AttributeKey<Boolean>(persistenceKey = "defence_cape_effect")

/**
 * Whether the Defence cape's save teleports to East Ardougne instead of the player's normal
 * respawn point. Real OSRS gates this choice behind the medium Ardougne diary; that diary
 * system does not exist here, so the "Toggle Respawn" option is left as a free preference.
 */
val DEFENCE_CAPE_ARDOUGNE_RESPAWN_ATTR = AttributeKey<Boolean>(persistenceKey = "defence_cape_ardougne_respawn")

/**
 * Whether the ring of life's save teleports to East Ardougne instead of the player's normal respawn
 * point - the ring's own "Toggle-respawn" inventory option, which the cache gives it. Kept separate
 * from [DEFENCE_CAPE_ARDOUGNE_RESPAWN_ATTR] because OSRS toggles the two items independently, even
 * though the same medium Ardougne diary unlocks both.
 */
val RING_OF_LIFE_ARDOUGNE_RESPAWN_ATTR = AttributeKey<Boolean>(persistenceKey = "ring_of_life_ardougne_respawn")

/*
 * The charge pools for the perk jewellery - ring of recoil, dodgy necklace, ring of forging,
 * bracelet of clay, the two Slayer bracelets and the amulet of chemistry - are declared on
 * `org.alter.plugins.content.items.jewellery.PerkJewellery` instead of here. All seven are the same
 * shape and belong to that one table; their persistence keys are "<item>_charges", except the ring
 * of recoil's, which is "ring_of_recoil_damage_left".
 */

/**
 * The command that the player has submitted to the server using the '::' prefix.
 */
val COMMAND_ATTR = AttributeKey<String>()

/**
 * The arguments to the last command that was submitted by the player. This does
 * not include the command itself, if you want the command itself, use the
 * [COMMAND_ATTR] attribute.
 */
val COMMAND_ARGS_ATTR = AttributeKey<Array<String>>()

/**
 * The option that was last selected on any entity message.
 * For example: object action one will set this attribute to [1].
 */
val INTERACTING_OPT_ATTR = AttributeKey<Int>()

/**
 * The slot that was last selected on any entity message.
 */
val INTERACTING_SLOT_ATTR = AttributeKey<Int>()

/**
 * The [GroundItem] that was last clicked on.
 */
val INTERACTING_GROUNDITEM_ATTR = AttributeKey<WeakReference<GroundItem>>()

/**
 * The last [ItemTransaction] to occur when a ground item is picked up
 * from the ground.
 */
val GROUNDITEM_PICKUP_TRANSACTION = AttributeKey<WeakReference<ItemTransaction>>()

/**
 * The [GameObject] that was last clicked on.
 */
val INTERACTING_OBJ_ATTR = AttributeKey<WeakReference<out GameObject>>()

/**
 * The [Npc] that was last clicked on.
 */
val INTERACTING_NPC_ATTR = AttributeKey<WeakReference<Npc>>()

/**
 * The [Player] that was last clicked on.
 */
val INTERACTING_PLAYER_ATTR = AttributeKey<WeakReference<Player>>()

/**
 * The slot of the interacting item in its item container.
 */
val INTERACTING_ITEM_SLOT = AttributeKey<Int>()

/**
 * The id of the interacting item.
 */
val INTERACTING_ITEM_ID = AttributeKey<Int>()

/**
 * The id of the item a global equip requirement is currently being asked about.
 */
val EQUIP_REQUIREMENT_ITEM_ID = AttributeKey<Int>()

/**
 * Set during [org.alter.game.plugin.PluginRepository.executePlayerPreDeath] by a plugin that is
 * taking the death over - a duel, say, which sends the loser to the arena lobby rather than to a
 * respawn point. Cleared as it is read.
 */
val DEATH_HANDLED_ATTR = AttributeKey<Boolean>()

/**
 * The item pointer of the interacting item.
 */
val INTERACTING_ITEM = AttributeKey<WeakReference<Item>>()

/**
 * The slot index of any 'secondary' item being interacted with.
 */
val OTHER_ITEM_SLOT_ATTR = AttributeKey<Int>()

/**
 * The item id of any 'secondary' item being interacted with.
 */
val OTHER_ITEM_ID_ATTR = AttributeKey<Int>()

/**
 * The item pointer of any 'secondary' item being interacted with.
 */
val OTHER_ITEM_ATTR = AttributeKey<WeakReference<Item>>()

/**
 * Interacting interface parent id.
 */
val INTERACTING_COMPONENT_PARENT = AttributeKey<Int>()

/**
 * Interacting interface child id.
 */
val INTERACTING_COMPONENT_CHILD = AttributeKey<Int>()

/**
 * The skill id of the latest level up.
 */
val LEVEL_UP_SKILL_ID = AttributeKey<Int>()

/**
 * The amount of levels that have incremented in a skill level up.
 */
val LEVEL_UP_INCREMENT = AttributeKey<Int>()

/**
 * The previous skill XP of the latest level up.
 */
val LEVEL_UP_OLD_XP = AttributeKey<Double>()

val CHANGE_LOGGING = AttributeKey<Boolean>()

/**
 * Instead of running tp
 */
val CLIENT_KEY_COMBINATION = AttributeKey<Int>()

/**
 * Burn damage still owed by a pawn, and who owes it to them.
 *
 * The Varlamore demonbane weapons' burn, held as a counter that
 * [org.alter.game.model.timer.SPECIAL_ATTACK_BURN_TIMER] works through rather than as a run of
 * pre-queued hits, so the eclipse atlatl can cut it short and roll the remainder into its own hit.
 *
 * The source is weak: burn outlives the attack, and crediting damage to a player who has since
 * logged out would keep them on the target's damage map.
 */
val SPECIAL_ATTACK_BURN_ATTR = AttributeKey<Int>()
val SPECIAL_ATTACK_BURN_SOURCE_ATTR = AttributeKey<WeakReference<Pawn>>()

/**
 * Soul stacks on the soulreaper axe, and sunlight stacks on the sunlight spear.
 *
 * Both are the same idea - a counter the weapon builds up over ordinary attacks and its special
 * spends - and both are dropped when the weapon comes off.
 */
val SOUL_STACKS_ATTR = AttributeKey<Int>()
val SUNLIGHT_STACKS_ATTR = AttributeKey<Int>()

/**
 * Owner-only cheat: every attack this player lands on an npc kills it outright.
 *
 * Read in the one place all damage is dealt from - `Pawn.dealExactHit` - so it covers
 * specials and multi-hit attacks as well as ordinary swings. Deliberately npc-only, so
 * leaving it on cannot one-shot another player.
 */
val ONE_HIT_KILL_ATTR = AttributeKey<Boolean>(persistenceKey = "one_hit_kill")
