package org.alter.plugins.content.combat.specialattack

import org.alter.api.EquipmentType
import org.alter.api.ext.getEquipment
import org.alter.game.model.World
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.rscm.RSCM.getRSCM
import org.alter.plugins.content.interfaces.attack.AttackTab
import org.alter.plugins.content.areas.duelarena.DuelRules

/**
 * @author Tom <rspsmods@gmail.com>
 */
object SpecialAttacks {

    fun register(
        item: String,
        energy: Int,
        executeInstantly: Boolean = false,
        attack: CombatContext.() -> Unit,
    ) {
        register(
            getRSCM(item),
            energy,
            executeInstantly,
            attack
        )
    }

    fun register(
        item: Int,
        energy: Int,
        executeInstantly: Boolean = false,
        attack: CombatContext.() -> Unit,
    ) {
        attacks[item] = SpecialAttack(energy, executeInstantly, attack)
    }

    /**
     * Binds one special attack to every item in the cache that carries it, at each item's own cost.
     *
     * [name] is the special's name as the cache spells it - the part before the colon in enum 1739,
     * e.g. `"Puncture"` or `"Descent of Darkness"`. See [SpecialAttackDefs] for why binding by name
     * rather than by a hand-written list of ids is the point: one call covers the plain weapon, the
     * poisoned grades, the ornamented and `(cr)`/`(bh)`/deadman variants and anything a later cache
     * adds, all priced from enum 906 instead of from a constant that only matched the plain one.
     *
     * [extraItems] is for the few ids that carry a cost in enum 906 but no description in enum
     * 1739 - the ornamented and uncharged infernal harpoons, pickaxes and axes. Raw item ids
     * because that is what the enum gives; there is nothing else to identify them by.
     *
     * [matching] narrows the binding when two unrelated weapons happen to share a name. Only one
     * pair really does: the dragon warhammer and Statius's warhammer are both called **Smash** and
     * do different things, and the descriptions behind them differ where the names do not. Leave it
     * alone for everything else - variants of the same weapon are *meant* to share one binding even
     * when their descriptions differ in a number, which is why this filters on the description
     * rather than keying on it.
     */
    fun registerByName(
        name: String,
        executeInstantly: Boolean = false,
        extraItems: List<Int> = emptyList(),
        matching: (String) -> Boolean = { true },
        attack: CombatContext.() -> Unit,
    ) {
        val named = SpecialAttackDefs.itemsWith(name).filter { matching(SpecialAttackDefs.description(it).orEmpty()) }
        val items = named + extraItems
        check(items.isNotEmpty()) {
            "No item in this cache has a special attack called '$name' - enum 1739 has moved or been renamed."
        }
        items.forEach { item ->
            val energy = SpecialAttackDefs.cost(item) ?: return@forEach
            /*
             * Two plugins binding the same weapon is always a mistake, and a silent one: the map
             * would simply keep whichever loaded last, and plugin load order is not fixed. The one
             * name two different weapons genuinely share is "Smash", and both of its bindings use
             * [matching] to take only their own half.
             */
            check(item !in attacks) {
                "Item $item already has a special attack bound - '$name' collides with something else."
            }
            attacks[item] = SpecialAttack(energy, executeInstantly, attack)
        }
    }

    fun executeOnEnable(item: Int): Boolean {
        if (attacks.containsKey(item)) {
            return attacks[item]!!.executeOnSpecBar
        }
        return false
    }

    /**
     * Whether [item] has a special attack bound at all. The spec bar and the orb are drawn by
     * the client for anything it thinks is a spec weapon, so a click can perfectly well arrive
     * for a weapon nothing has registered - and telling that player they are out of energy,
     * as this used to, sends them off to wait for a bar that is already full.
     */
    fun hasSpecial(item: Int): Boolean = attacks.containsKey(item)

    /** The bar cost of [item]'s special, or `null` if it has none. */
    fun energyRequired(item: Int): Int? = attacks[item]?.energyRequired

    fun execute(
        player: Player,
        target: Pawn?,
        world: World,
    ): Boolean {
        val weaponItem = player.getEquipment(EquipmentType.WEAPON) ?: return false
        val special = attacks[weaponItem.id] ?: return false

        if (!DuelRules.canUseSpecialAttack(player)) {
            return false
        }

        if (AttackTab.getEnergy(player) < special.energyRequired) {
            return false
        }

        AttackTab.setEnergy(player, AttackTab.getEnergy(player) - special.energyRequired)

        val combatContext = CombatContext(world, player)
        target?.let { combatContext.target = it }
        special.attack(combatContext)

        return true
    }

    val attacks = mutableMapOf<Int, SpecialAttack>()
}
