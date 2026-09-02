package org.alter.plugins.content.interfaces.gameframe.tabs.combat_options

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
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
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.plugins.content.interfaces.attack.AttackTab
import org.alter.plugins.content.interfaces.attack.AttackTab.ATTACK_STYLE_VARP
import org.alter.plugins.content.interfaces.attack.AttackTab.ATTACK_TAB_INTERFACE_ID
import org.alter.plugins.content.interfaces.attack.AttackTab.DISABLE_AUTO_RETALIATE_VARP
import org.alter.plugins.content.interfaces.attack.AttackTab.SPECIAL_ATTACK_VARP
import org.alter.plugins.content.interfaces.attack.AttackTab.setEnergy
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
         * Autocast / Defensive Autocast - real dedicated components (confirmed via the
         * server's unhandled-button debug log: `component=[593:22]` and
         * `component=[593:27]`), only meaningful with a magic staff equipped.
         *
         * The real client shows a dedicated Autocast interface (id 201, confirmed via
         * RuneLite's gameval `InterfaceID.AUTOCAST`/`InterfaceID.Autocast.SPELLS`) here,
         * but populating its spell grid turned out to be a genuine dead end: this
         * project has *zero* existing precedent anywhere for pushing an item array into
         * an arbitrary interface component without a real, known inventory-type id
         * (every other `sendItemContainer` call in the codebase uses an established key
         * like the bank/trade/equipment containers) - doing it anyway
         * (`inventoryId = 0`, not a real type) silently killed the connection with no
         * logged exception, which lines up with this server explicitly running with
         * `inventoryObjCheck`/`clientscriptVerification` enabled. Rather than guess
         * again at a live server's expense, this reuses the chatbox choice dialog
         * already proven safe elsewhere (Cheat Menu, Fishing) - less visually authentic
         * than the native grid, but it won't crash anything.
         */
        onButton(interfaceId = ATTACK_TAB_INTERFACE_ID, component = 22) {
            player.queue(TaskPriority.STANDARD) { chooseAutocastSpell(this, player, defensive = false) }
        }

        onButton(interfaceId = ATTACK_TAB_INTERFACE_ID, component = 27) {
            player.queue(TaskPriority.STANDARD) { chooseAutocastSpell(this, player, defensive = true) }
        }

        /**
         * Toggle auto-retaliate button.
         */
        onButton(interfaceId = ATTACK_TAB_INTERFACE_ID, component = 31) {
            player.toggleVarp(DISABLE_AUTO_RETALIATE_VARP)
        }

        onButton(interfaceId = 160, component = 35) {
            val weaponId = player.equipment[EquipmentType.WEAPON.id]!!.id
            if (SpecialAttacks.executeOnEnable(weaponId)) {
                if (!SpecialAttacks.execute(player, null, world)) {
                    player.message("You don't have enough power left.")
                }
            } else {
                player.toggleVarp(SPECIAL_ATTACK_VARP)
            }
        }

        /**
         * Toggle special attack.
         */
        onButton(interfaceId = ATTACK_TAB_INTERFACE_ID, component = 36) {
            val weaponId = player.equipment[EquipmentType.WEAPON.id]!!.id
            if (SpecialAttacks.executeOnEnable(weaponId)) {
                if (!SpecialAttacks.execute(player, null, world)) {
                    player.message("You don't have enough power left.")
                }
            } else {
                player.toggleVarp(SPECIAL_ATTACK_VARP)
            }
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
        }

        /**
         * Disable special attack on log-out.
         */
        onLogout {
            player.setVarp(SPECIAL_ATTACK_VARP, 0)
        }
    }

    /**
     * Whether the player's equipped weapon is a magic-capable staff (Staff of Air,
     * battlestaves, etc.) rather than a melee-only "staff" like a quarterstaff.
     *
     * Deliberately does **not** use [org.alter.api.ext.hasWeaponType] - that reads
     * `Varbit.WEAPON_TYPE_VARBIT` (357), which is only ever read anywhere in this
     * codebase, never written by anything. It's dead state: always 0, so
     * `hasWeaponType` can never return true for *any* weapon type, for *any* weapon,
     * server-wide - not just staves. That's the actual reason autocast never opened
     * anything: this check was always false. Reading the weapon's real cache-derived
     * `weaponType` directly (populated once at startup by `ItemMetadataService`, the
     * same value `hasWeaponType` was supposed to be checking) sidesteps the bug rather
     * than depending on it. The broader bug (affecting every other `hasWeaponType`
     * caller - ranged combat-class detection, weapon attack sounds, magic bonus checks)
     * is a separate, bigger fix and out of scope here.
     */
    private fun isWieldingMagicStaff(player: Player): Boolean =
        player.getEquipment(EquipmentType.WEAPON)?.getDef()?.weaponType == WeaponType.MAGIC_STAFF.id

    /**
     * Lists every Standard-spellbook combat spell the player's current Magic level
     * reaches as a chatbox choice, then sets [Combat.SELECTED_AUTOCAST_VARBIT] to the
     * chosen spell's autocast id - the engine's existing combat loop
     * (`CombatPlugin.cycle`) already re-selects that spell every attack on its own once
     * this varbit is non-zero, so nothing else is needed to make it actually repeat.
     * Curse spells are excluded (`autoCastId = -1` sentinel, they were never
     * autocastable in real OSRS either).
     */
    private suspend fun chooseAutocastSpell(
        task: QueueTask,
        player: Player,
        defensive: Boolean,
    ) {
        if (!isWieldingMagicStaff(player)) {
            player.message("You need a magic staff equipped to autocast spells.")
            return
        }

        val magicLevel = player.getSkills().getCurrentLevel(Skills.MAGIC)
        val eligible =
            CombatSpell.values
                .filter { spell ->
                    spell.autoCastId > 0 &&
                        MagicSpells.getMetadata(spell.id)?.let { it.spellbook == player.getSpellbook().id && it.lvl <= magicLevel } == true
                }.sortedBy { MagicSpells.getMetadata(it.id)?.lvl ?: 0 }

        if (eligible.isEmpty()) {
            player.message("You don't know any spells you can autocast yet.")
            return
        }

        val names = eligible.map { MagicSpells.getMetadata(it.id)?.name ?: it.name }
        val choice = task.pagedOptions(player, names, title = "Choose a spell to autocast")
        val chosen = eligible.getOrNull(choice - 1) ?: return

        player.setVarbit(Combat.SELECTED_AUTOCAST_VARBIT, chosen.autoCastId)
        player.setVarbit(Combat.DEFENSIVE_MAGIC_CAST_VARBIT, if (defensive) 1 else 0)
        player.setVarp(ATTACK_STYLE_VARP, if (defensive) 3 else 0)
        player.message("You will now autocast ${MagicSpells.getMetadata(chosen.id)?.name ?: chosen.name}.")
    }

    /**
     * Chatbox choice list, paginated 3-per-page beyond 5 total (the native chatbox
     * choice interface only comfortably fits 5 options at once) - same pattern already
     * used for the Cheat Menu's item/style pickers.
     */
    private suspend fun QueueTask.pagedOptions(
        player: Player,
        items: List<String>,
        title: String,
        pageSize: Int = 3,
    ): Int {
        if (items.size <= 5) {
            return options(player, *items.toTypedArray(), title = title)
        }
        val totalPages = (items.size + pageSize - 1) / pageSize
        var page = 0
        while (true) {
            val start = page * pageSize
            val end = minOf(start + pageSize, items.size)
            val pageItems = items.subList(start, end).toMutableList()
            val itemCount = pageItems.size
            if (page > 0) pageItems.add("<< Previous page")
            if (page < totalPages - 1) pageItems.add("Next page >>")
            val choice = options(player, *pageItems.toTypedArray(), title = "$title (${page + 1}/$totalPages)")
            if (choice == -1) return -1
            val idx = choice - 1
            when {
                idx < itemCount -> return start + idx + 1
                page > 0 && idx == itemCount -> page--
                else -> page++
            }
        }
    }
}
