package org.alter.plugins.content.areas.duelarena

import dev.openrune.cache.CacheManager.getItem
import org.alter.api.BonusSlot
import org.alter.api.EquipmentType
import org.alter.api.ext.getEquipment
import org.alter.api.ext.message
import org.alter.game.model.entity.Player
import org.alter.plugins.content.mechanics.prayer.Prayers

/**
 * The duel rules as questions other systems can ask.
 *
 * Each rule is enforced at the one place the action it forbids actually happens - eating at
 * `Foods.canEat`, prayer at `Prayers.toggle`, and so on - rather than by trying to police the
 * player from here. That keeps every rule a single early return in code that already exists, and
 * means a rule cannot be evaded through some second route into the same action.
 *
 * Every function returns true (allowed) when the player is not in a live duel, so callers can use
 * them unconditionally.
 */
object DuelRules {
    /**
     * The message the arena gives when a rule stops something. Phrased the way the duel screens
     * phrase their rules so the reason is obvious.
     */
    private fun refuse(
        player: Player,
        what: String,
    ) {
        player.message("The rules of this duel do not allow $what.")
    }

    fun canEat(player: Player): Boolean {
        if (!player.duelForbids(DuelRule.NO_FOOD)) return true
        refuse(player, "eating")
        return false
    }

    fun canDrink(player: Player): Boolean {
        if (!player.duelForbids(DuelRule.NO_DRINKS)) return true
        refuse(player, "drinking")
        return false
    }

    fun canPray(player: Player): Boolean {
        if (!player.duelForbids(DuelRule.NO_PRAYER)) return true
        refuse(player, "prayer")
        return false
    }

    fun canUseSpecialAttack(player: Player): Boolean {
        if (!player.duelForbids(DuelRule.NO_SPECIAL_ATTACKS)) return true
        refuse(player, "special attacks")
        return false
    }

    fun canForfeit(player: Player): Boolean {
        if (!player.duelForbids(DuelRule.NO_FORFEIT)) return true
        player.message("You can't forfeit this duel.")
        return false
    }

    /**
     * Whether [player] may attack using the given style. Called from the combat gate, which knows
     * which strategy is about to swing.
     */
    fun canAttackWith(
        player: Player,
        style: DuelStyle,
    ): Boolean {
        val duel = player.getActiveDuel() ?: return true

        val rule =
            when (style) {
                DuelStyle.MELEE -> DuelRule.NO_MELEE
                DuelStyle.RANGED -> DuelRule.NO_RANGED
                DuelStyle.MAGIC -> DuelRule.NO_MAGIC
            }
        if (duel.hasRule(rule)) {
            refuse(player, style.description)
            return false
        }

        // Fun Weapons is really a rule about what is in your hand, but the only moment it can be
        // enforced is the moment you try to hit someone with it - a player can be handed a weapon
        // by any number of routes, and the equip gate below only sees the ones that go through
        // equipping.
        if (duel.hasRule(DuelRule.FUN_WEAPONS) && !isFunWeapon(player)) {
            player.message("You can only use a fun weapon in this duel.")
            return false
        }
        return true
    }

    /**
     * Whether [player] may put [item] on right now.
     *
     * Covers all three equipment-flavoured rules: a slot the duel has locked, a weapon swap under
     * "No Weapon Switch", and anything that is not a fun weapon under "Fun Weapons".
     */
    fun canEquip(
        player: Player,
        item: Int,
    ): Boolean {
        val duel = player.getActiveDuel() ?: return true
        val slot = getItem(item).equipSlot
        if (slot < 0) return true

        if (duel.isSlotLocked(slot)) {
            val label = DuelSlot.bySlot(slot)?.label ?: "that slot"
            player.message("The rules of this duel do not allow anything in your $label slot.")
            return false
        }

        if (slot == EquipmentType.WEAPON.id) {
            if (duel.hasRule(DuelRule.NO_WEAPON_SWITCH)) {
                refuse(player, "changing weapon")
                return false
            }
            if (duel.hasRule(DuelRule.FUN_WEAPONS) && !isFunWeaponItem(item)) {
                player.message("You can only use a fun weapon in this duel.")
                return false
            }
        }

        // A two-handed weapon fills the shield slot as well, so a locked shield slot has to refuse
        // it too - otherwise "no shield" would quietly become "no shield unless it is a 2h".
        val secondary = getItem(item).equipType
        if (secondary >= 0 && secondary != slot && duel.isSlotLocked(secondary)) {
            val label = DuelSlot.bySlot(secondary)?.label ?: "that slot"
            player.message("The rules of this duel do not allow anything in your $label slot.")
            return false
        }
        return true
    }

    /**
     * A "fun weapon" is one with negative attack stats - the rubber chickens and flowers of the
     * world. Bare hands do not count, which is why an empty weapon slot fails.
     */
    fun isFunWeapon(player: Player): Boolean {
        val weapon = player.getEquipment(EquipmentType.WEAPON) ?: return false
        return isFunWeaponItem(weapon.id)
    }

    private fun isFunWeaponItem(item: Int): Boolean {
        val bonuses = getItem(item).bonuses
        val attack = ATTACK_BONUSES.map { bonuses.getOrElse(it) { 0 } }
        return attack.none { it > 0 } && attack.any { it < 0 }
    }

    fun stopAllPrayers(player: Player) {
        Prayers.deactivateAll(player)
    }

    private val ATTACK_BONUSES =
        listOf(
            BonusSlot.ATTACK_STAB.id,
            BonusSlot.ATTACK_SLASH.id,
            BonusSlot.ATTACK_CRUSH.id,
            BonusSlot.ATTACK_MAGIC.id,
            BonusSlot.ATTACK_RANGED.id,
        )
}

/**
 * The three ways a duel can be fought, as the rules name them.
 */
enum class DuelStyle(val description: String) {
    MELEE("melee attacks"),
    RANGED("ranged attacks"),
    MAGIC("magic attacks"),
}
