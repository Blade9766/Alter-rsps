package org.alter.plugins.content.items.jewellery

import org.alter.api.Skills
import org.alter.api.ext.getInteractingItemSlot
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.magic.MagicSpells
import org.alter.plugins.content.magic.SpellMetadata
import org.alter.rscm.RSCM.getRSCM

/**
 * Casting Lvl-1 through Lvl-7 Enchant on a piece of jewellery.
 *
 * None of this worked before, and not because the spells were unwritten: the engine's whole
 * spell-on-item path was dead. `IfButtonTHandler` read both ends of the packet as inventory slots,
 * so a spell - which carries no source item and sends slot -1 - fell out on a null check before any
 * plugin was consulted, and `PluginRepository.executeSpellOnItem` had no caller in the project at
 * all. That is fixed alongside this plugin, which is its first user.
 *
 * Requirements are read from the cache rather than restated here (see [EnchantSpell]), so the level,
 * the runes and the spellbook component this binds against are whatever this project's own cache
 * says they are. Only the conversion table and the experience live in code.
 */
class JewelleryEnchantPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        if (!MagicSpells.isLoaded()) {
            MagicSpells.loadSpellRequirements(world)
        }

        EnchantSpell.values.forEach { spell ->
            val metadata =
                MagicSpells.loadSpell(spell.spellItem)
                    ?: error("No spell params on cache item ${spell.spellItem} for ${spell.spellName}.")

            // Resolved once, at construction: an unresolvable RSCM key should take the plugin down
            // loudly at boot rather than throw the first time somebody casts the spell.
            val conversions = spell.conversions.entries.associate { getRSCM(it.key) to getRSCM(it.value) }

            onSpellOnItem(
                fromInterface = metadata.interfaceId,
                fromComponent = metadata.component,
                toInterface = INVENTORY_INTERFACE,
                toComponent = INVENTORY_COMPONENT,
            ) {
                enchant(player, spell, metadata, conversions)
            }
        }
    }

    private fun enchant(
        player: Player,
        spell: EnchantSpell,
        metadata: SpellMetadata,
        conversions: Map<Int, Int>,
    ) {
        val slot = player.getInteractingItemSlot()
        val target = player.inventory[slot] ?: return

        val enchanted = conversions[target.id]
        if (enchanted == null) {
            /*
             * Not the wording OSRS uses - the real message is not published anywhere this project
             * can verify it against - but it says the one thing a player needs to know, which is
             * which gems this particular spell is for.
             */
            player.message("You can only cast this spell on ${spell.gems} jewellery.")
            return
        }

        // Level, spellbook and rune checks, and their messages, all come from the shared spell
        // helper so an enchant fails for exactly the same reasons and with the same wording as
        // every other spell in the game.
        if (!MagicSpells.canCast(player, metadata.lvl, metadata.items, metadata.spellbook)) {
            return
        }

        player.animate(EnchantSpell.CAST_ANIMATION)
        player.graphic(spell.graphic)
        MagicSpells.removeRunes(player, metadata.items)

        /*
         * Removing the runes can compact the inventory, so the target is located again rather than
         * written back to the slot it was read from.
         */
        val targetSlot = if (player.inventory[slot]?.id == target.id) slot else player.inventory.getItemIndex(target.id, false)
        if (targetSlot == -1) {
            return
        }
        player.inventory[targetSlot] = Item(enchanted, target.amount)
        player.addXp(Skills.MAGIC, spell.xp)
    }

    private companion object {
        /**
         * The inventory's item container, which is what a spell dragged onto an inventory item
         * targets. Matches the canonical target `IfButtonTHandler` falls back to.
         */
        private const val INVENTORY_INTERFACE = 149
        private const val INVENTORY_COMPONENT = 0
    }
}
