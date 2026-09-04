package org.alter.plugins.content.interfaces.gameframe.tabs.combat_options

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.attr.INTERACTING_SLOT_ATTR
import org.alter.game.model.attr.NEW_ACCOUNT_ATTR
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.WeaponStyles
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.plugins.content.interfaces.attack.AttackTab
import org.alter.plugins.content.interfaces.attack.AttackTab.ATTACK_STYLE_VARP
import org.alter.plugins.content.interfaces.attack.AttackTab.ATTACK_TAB_INTERFACE_ID
import org.alter.plugins.content.interfaces.attack.AttackTab.DISABLE_AUTO_RETALIATE_VARP
import org.alter.plugins.content.interfaces.attack.AttackTab.SPECIAL_ATTACK_BAR_COMPONENT
import org.alter.plugins.content.interfaces.attack.AttackTab.SPECIAL_ATTACK_VARP
import org.alter.plugins.content.interfaces.attack.AttackTab.SPECIAL_ORB_COMPONENT
import org.alter.plugins.content.interfaces.attack.AttackTab.SPECIAL_ORB_INTERFACE_ID
import org.alter.plugins.content.interfaces.attack.AttackTab.setEnergy
import org.alter.plugins.content.magic.AutocastInterface
import org.alter.plugins.content.magic.MagicSpells

class AttackTabPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        /**
         * First log-in logic (when accounts have just been made).
         */
        onLogin {
            if (player.attr.getOrDefault(NEW_ACCOUNT_ATTR, false)) {
                setEnergy(player, 100)
            }
            AttackTab.resetRestorationTimer(player)
            syncAutocastWeapon(player)
        }

        onTimer(AttackTab.SPEC_RESTORE) {
            AttackTab.restoreEnergy(player)
            AttackTab.resetRestorationTimer(player)
        }

        /**
         * Plain melee/ranged attack style buttons. These are always present regardless
         * of weapon - the client shows separate, dedicated components for
         * Autocast/Defensive Autocast (see below) rather than relabelling these ones,
         * confirmed via the server's own unhandled-button debug log after the first
         * version of this wrongly assumed components 5/17 doubled as the autocast rows.
         */
        onButton(interfaceId = ATTACK_TAB_INTERFACE_ID, component = 5) {
            player.setVarp(ATTACK_STYLE_VARP, 0)
        }

        onButton(interfaceId = ATTACK_TAB_INTERFACE_ID, component = 9) {
            player.setVarp(ATTACK_STYLE_VARP, 1)
        }

        onButton(interfaceId = ATTACK_TAB_INTERFACE_ID, component = 13) {
            player.setVarp(ATTACK_STYLE_VARP, 2)
        }

        onButton(interfaceId = ATTACK_TAB_INTERFACE_ID, component = 17) {
            player.setVarp(ATTACK_STYLE_VARP, 3)
        }

        /**
         * The Autocast and Defensive Autocast rows - real, dedicated components of the
         * Combat Options tab, both carrying a "Choose spell" option. Verified straight
         * out of the cache: both are layers with clickMask 0x2 and a single "Choose
         * spell" action, and neither carries a clientscript listener, so the click comes
         * to us.
         *
         * Note which is which - [AutocastInterface.DEFENSIVE_ROW_COMPONENT] is the
         * *upper* row despite having the lower component number, and it is the defensive
         * one. Both rows tell the client which they are through their own clientscript
         * hook, so getting this backwards sets the spell correctly but highlights the
         * other row.
         *
         * Both open the client's own native spell grid, [AutocastInterface.INTERFACE_ID],
         * over the Combat Options tab - which it is sized for almost to the pixel: the
         * grid layer is 175 tall and the info panel below it is parentHeight - 180,
         * against the tab's 190x261. See [AutocastInterface] for why the server does not
         * have to populate that grid, and why the previous attempt at doing so killed
         * the connection.
         */
        onButton(interfaceId = ATTACK_TAB_INTERFACE_ID, component = AutocastInterface.DEFENSIVE_ROW_COMPONENT) {
            openAutocastPicker(player, defensive = true)
        }

        onButton(interfaceId = ATTACK_TAB_INTERFACE_ID, component = AutocastInterface.AUTOCAST_ROW_COMPONENT) {
            openAutocastPicker(player, defensive = false)
        }

        /**
         * A spell picked out of the native grid. Every icon is a dynamic child the
         * client created on [AutocastInterface.SPELL_GRID_COMPONENT], so the click is
         * reported against that one component with the child's id - the autocast spell
         * index - in [INTERACTING_SLOT_ATTR].
         */
        onButton(interfaceId = AutocastInterface.INTERFACE_ID, component = AutocastInterface.SPELL_GRID_COMPONENT) {
            val slot = player.attr[INTERACTING_SLOT_ATTR] ?: return@onButton
            selectAutocastSpell(player, slot)
        }

        /**
         * Toggle auto-retaliate button.
         */
        onButton(interfaceId = ATTACK_TAB_INTERFACE_ID, component = 31) {
            player.toggleVarp(DISABLE_AUTO_RETALIATE_VARP)
        }

        /**
         * The special attack bar, in both places the client draws one: the orb under the
         * minimap and the bar along the bottom of this tab.
         *
         * The tab's bar is component **38**, not 36. 36 is the "Toggle set effect" button
         * that sits beside Auto retaliate - read straight out of the cache, 593:36 carries
         * the op "Toggle set effect" and watches varbit 4157, while 593:38 is the only
         * component on the interface with the op "Use <col=00ff00>Special Attack</col>" and
         * has no clientscript of its own, so its click comes to us. Bound to 36, every
         * click on the real bar fell through to the unhandled-button branch and the special
         * was never armed - which is what made specials look completely dead in the tab.
         */
        onButton(interfaceId = SPECIAL_ORB_INTERFACE_ID, component = SPECIAL_ORB_COMPONENT) {
            toggleSpecialAttack(player)
        }

        onButton(interfaceId = ATTACK_TAB_INTERFACE_ID, component = SPECIAL_ATTACK_BAR_COMPONENT) {
            toggleSpecialAttack(player)
        }

        /**
         * Disable special attack when switching weapons, and drop autocast if the new
         * weapon can't cast at all (switching between two magic staves keeps it, same
         * as real OSRS).
         */
        onEquipToSlot(EquipmentType.WEAPON.id) {
            player.setVarp(SPECIAL_ATTACK_VARP, 0)
            if (!isWieldingMagicStaff(player)) {
                player.setVarbit(Combat.SELECTED_AUTOCAST_VARBIT, 0)
                player.setVarbit(Combat.DEFENSIVE_MAGIC_CAST_VARBIT, 0)
            }
            syncAutocastWeapon(player)
            resetStyleIfAbsent(player)
        }

        /**
         * Same as above for an empty weapon hand - the grid has to stop offering a
         * staff's spell set the moment the staff comes off.
         */
        onUnequipFromSlot(EquipmentType.WEAPON.id) {
            player.setVarbit(Combat.SELECTED_AUTOCAST_VARBIT, 0)
            player.setVarbit(Combat.DEFENSIVE_MAGIC_CAST_VARBIT, 0)
            syncAutocastWeapon(player)
        }

        /**
         * Disable special attack on log-out.
         */
        onLogout {
            player.setVarp(SPECIAL_ATTACK_VARP, 0)
        }
    }

    /**
     * One click of the special attack bar or orb.
     *
     * Most specials arm the next attack: the varp goes to 1, the client lights the bar up, and
     * [org.alter.plugins.content.combat.CombatPlugin] spends it on the next swing. A handful
     * (Rampage, the dragon pickaxe) hit nobody and are registered `executeInstantly`, so the
     * click fires them on the spot with a null target.
     *
     * Everything that used to go wrong here happens before either of those. The weapon was read
     * with `!!`, so a click while unarmed threw out of the button handler; a weapon with no
     * special registered, and a weapon whose bar was simply too low, both produced the same
     * "You don't have enough power left."; and arming a special the player could not pay for
     * left the bar lit until the next swing quietly turned it back off.
     */
    private fun toggleSpecialAttack(player: Player) {
        val weapon = player.equipment[EquipmentType.WEAPON.id]
        if (weapon == null) {
            player.setVarp(SPECIAL_ATTACK_VARP, 0)
            player.message("You need a weapon with a special attack to do that.")
            return
        }

        val cost = SpecialAttacks.energyRequired(weapon.id)
        if (cost == null) {
            player.setVarp(SPECIAL_ATTACK_VARP, 0)
            player.message("Your weapon has no special attack.")
            return
        }

        // Disarming never costs anything, so let it through before the energy check.
        if (!SpecialAttacks.executeOnEnable(weapon.id) && AttackTab.isSpecialEnabled(player)) {
            player.setVarp(SPECIAL_ATTACK_VARP, 0)
            return
        }

        if (AttackTab.getEnergy(player) < cost) {
            player.setVarp(SPECIAL_ATTACK_VARP, 0)
            player.message("You don't have enough power left.")
            return
        }

        if (SpecialAttacks.executeOnEnable(weapon.id)) {
            SpecialAttacks.execute(player, null, world)
        } else {
            player.setVarp(SPECIAL_ATTACK_VARP, 1)
        }
    }

    /**
     * Whether the player's equipped weapon is a magic-capable staff (Staff of Air,
     * battlestaves, etc.) rather than a melee-only staff or a powered staff, which have
     * their own weapon types and no autocast buttons.
     */
    private fun isWieldingMagicStaff(player: Player): Boolean = player.hasWeaponType(WeaponType.MAGIC_STAFF, WeaponType.STAFF_HALBERD)

    /**
     * Writes the equipped weapon into [AutocastInterface.AUTOCAST_WEAPON_VARP], which is
     * the single thing the client needs from us to render the spell grid: clientscript
     * 243 switches on it to decide which spells appear and where, and its default case
     * hides every one of them. Kept in sync on login and on every weapon change rather
     * than only when the grid opens, since the client re-renders the grid by itself
     * whenever worn equipment changes.
     */
    private fun syncAutocastWeapon(player: Player) {
        val weaponId = player.equipment[EquipmentType.WEAPON.id]?.id ?: -1
        player.setVarp(AutocastInterface.AUTOCAST_WEAPON_VARP, AutocastInterface.weaponVarpValue(weaponId))
    }

    /**
     * Drops the selected attack style back to the first button when the newly equipped
     * weapon's panel has no button at that index.
     *
     * Panels do not all have four buttons: a scimitar's third button is Lunge, but a
     * warhammer only has Pound, Pummel and Block, and a bulwark has one option. Switching
     * from the former to the latter leaves [ATTACK_STYLE_VARP] pointing at a button that no
     * longer exists, and [org.alter.plugins.content.combat.WeaponStyles] then has no style to
     * report - which means no attack type, no invisible level boost and no experience skill.
     * Real OSRS resets the selection the same way.
     */
    private fun resetStyleIfAbsent(player: Player) {
        if (WeaponStyles.get(player.getWeaponType(), player.getAttackStyle()) == null) {
            player.setVarp(ATTACK_STYLE_VARP, 0)
        }
    }

    /**
     * Replaces the Combat Options tab with the client's native autocast spell grid,
     * remembering whether the player asked for the plain or the defensive row so the
     * eventual pick can set [Combat.DEFENSIVE_MAGIC_CAST_VARBIT] accordingly.
     */
    private fun openAutocastPicker(
        player: Player,
        defensive: Boolean,
    ) {
        if (!isWieldingMagicStaff(player)) {
            player.message("You need a magic staff equipped to autocast spells.")
            return
        }
        player.attr[Combat.AWAITING_AUTOCAST_SELECTION] = defensive
        syncAutocastWeapon(player)
        player.openInterface(AutocastInterface.INTERFACE_ID, InterfaceDestination.ATTACK, false)
        player.setInterfaceEvents(
            interfaceId = AutocastInterface.INTERFACE_ID,
            component = AutocastInterface.SPELL_GRID_COMPONENT,
            AutocastInterface.SPELL_SLOTS,
            InterfaceEvent.ClickOp1,
        )
    }

    /**
     * Puts the Combat Options tab back where the spell grid was. There is no close
     * button on the grid itself - real OSRS makes you pick a spell or Cancel - so this
     * is the only way back out.
     */
    private fun closeAutocastPicker(player: Player) {
        player.attr.remove(Combat.AWAITING_AUTOCAST_SELECTION)
        player.openInterface(InterfaceDestination.ATTACK)
    }

    /**
     * Handles one pick out of the native grid. [slot] is the dynamic child's id, which
     * is the autocast spell index shared by enum 1986 and [CombatSpell.autoCastId] -
     * except for [AutocastInterface.CANCEL_SLOT], the "Cancel" row, which switches
     * autocast back off.
     *
     * The client decides what to draw but does not gate what can be chosen, so the
     * spellbook and Magic level are still checked here, from the same cache-driven
     * metadata the cast-requirement checks in
     * [org.alter.plugins.content.combat.strategy.MagicCombatStrategy] use.
     */
    private fun selectAutocastSpell(
        player: Player,
        slot: Int,
    ) {
        if (slot == AutocastInterface.CANCEL_SLOT) {
            player.setVarbit(Combat.SELECTED_AUTOCAST_VARBIT, 0)
            player.setVarbit(Combat.DEFENSIVE_MAGIC_CAST_VARBIT, 0)
            // Leaving the varp on a spell slot would strand the player on a Magic
            // attack style with no spell behind it, so drop back to the first button.
            player.setVarp(ATTACK_STYLE_VARP, 0)
            closeAutocastPicker(player)
            player.message("You will no longer autocast a spell.")
            return
        }

        if (!isWieldingMagicStaff(player)) {
            player.message("You need a magic staff equipped to autocast spells.")
            closeAutocastPicker(player)
            return
        }

        val spell = CombatSpell.values.firstOrNull { it.autoCastId == slot }
        val metadata = spell?.let { MagicSpells.getMetadata(it.id) }
        if (spell == null || metadata == null) {
            player.message("You can't autocast that spell yet.")
            return
        }

        if (metadata.spellbook != player.getSpellbook().id) {
            player.message("You need to be on a different spellbook to autocast that spell.")
            return
        }

        if (player.getSkills().getCurrentLevel(Skills.MAGIC) < metadata.lvl) {
            player.message("You need a Magic level of ${metadata.lvl} to autocast that spell.")
            return
        }

        val defensive = player.attr[Combat.AWAITING_AUTOCAST_SELECTION] ?: false
        player.setVarbit(Combat.SELECTED_AUTOCAST_VARBIT, spell.autoCastId)
        player.setVarbit(Combat.DEFENSIVE_MAGIC_CAST_VARBIT, if (defensive) 1 else 0)
        player.setVarp(ATTACK_STYLE_VARP, if (defensive) AutocastInterface.DEFENSIVE_AUTOCAST_STYLE else AutocastInterface.AUTOCAST_STYLE)
        closeAutocastPicker(player)
        player.message("You will now autocast ${metadata.name}.")
    }
}
