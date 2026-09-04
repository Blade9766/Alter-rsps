package org.alter.plugins.content.combat.specialattack

import org.alter.api.EquipmentType
import org.alter.api.ext.getEquipment
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player

/**
 * Per-weapon logic that runs on an ordinary attack, for the two specials whose resource is built up
 * by attacking rather than by the special attack bar.
 *
 * The soulreaper axe's Behead spends soul stacks and the sunlight spear's Sol Slam spends sunlight
 * stacks; both stacks are earned one per swing. Nothing else in this codebase needed a hook on the
 * ordinary attack, so there was none - `PluginRepository.setItemCombatLogic` looks like it should be
 * one, but `executeItemCombatLogic` has no caller anywhere in the server, so anything bound through
 * it never runs. This is deliberately a separate, narrow hook rather than a fix to that one: giving
 * the dead binding a call site would silently switch on whatever is already registered against it.
 *
 * Registered by the weapon's special attack name, the same key [SpecialAttacks.registerByName] uses,
 * so every variant of a weapon is covered by one registration.
 */
object WeaponPassives {
    private val onAttack = mutableMapOf<Int, (Player, Pawn) -> Unit>()

    fun registerByName(
        name: String,
        logic: (Player, Pawn) -> Unit,
    ) {
        val items = SpecialAttackDefs.itemsWith(name)
        check(items.isNotEmpty()) { "No item in this cache has a special attack called '$name'." }
        items.forEach { onAttack[it] = logic }
    }

    /** Called by [org.alter.plugins.content.combat.strategy.MeleeCombatStrategy] after each swing. */
    fun attacked(
        player: Player,
        target: Pawn,
    ) {
        val weapon = player.getEquipment(EquipmentType.WEAPON)?.id ?: return
        onAttack[weapon]?.invoke(player, target)
    }
}
