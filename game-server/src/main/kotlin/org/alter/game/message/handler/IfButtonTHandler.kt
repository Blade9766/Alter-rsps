package org.alter.game.message.handler

import net.rsprot.protocol.game.incoming.buttons.IfButtonT
import org.alter.game.message.MessageHandler
import org.alter.game.model.attr.*
import org.alter.game.model.entity.Client
import org.alter.game.model.entity.Entity
import java.lang.ref.WeakReference

/**
 * "Use component on component" - the packet behind both *item on item* and *spell on item*.
 *
 * The two look identical on the wire; only the source interface tells them apart. An inventory
 * item drags from the inventory interface (149) and carries a real slot and item id in
 * `selectedSub`/`selectedObj`; a spell drags from the spellbook (218 on the standard book) and
 * carries no item at all, so both of those fields are -1.
 *
 * This handler used to read `client.inventory[selectedSub]` unconditionally, which for a spell
 * meant `inventory[-1]` - a null that took the early return before anything ran. That is why
 * [org.alter.game.plugin.PluginRepository.executeSpellOnItem] had no caller anywhere in the
 * project and every spell that targets an inventory item - the seven jewellery enchants, the
 * bolt enchants, High/Low Alchemy, Superheat Item - did nothing when cast.
 */
class IfButtonTHandler : MessageHandler<IfButtonT> {
    override fun consume(
        client: Client,
        message: IfButtonT,
    ) {
        val fromInterfaceId = message.selectedInterfaceId
        val fromComponent = message.selectedComponentId
        val fromSlot = message.selectedSub
        val fromItemId = message.selectedObj

        val toInterfaceId = message.targetInterfaceId
        val toComponent = message.targetComponentId
        val toSlot = message.targetSub
        val toItemId = message.targetObj

        if (!client.lock.canItemInteract()) {
            return
        }

        if (fromInterfaceId != INVENTORY_INTERFACE) {
            consumeSpellOnItem(
                client,
                fromInterfaceId = fromInterfaceId,
                fromComponent = fromComponent,
                toInterfaceId = toInterfaceId,
                toComponent = toComponent,
                toSlot = toSlot,
                toItemId = toItemId,
            )
            return
        }

        val fromItem = client.inventory[fromSlot] ?: return
        val toItem = client.inventory[toSlot] ?: return

        if (fromItem.id != fromItemId || toItem.id != toItemId) {
            return
        }

        log(
            client,
            "ButtonT: from_component=[%d,%d], to_component=[%d,%d], from_item=%d, from_slot=%d, to_item=%d, to_slot=%d",
            fromInterfaceId,
            fromComponent,
            toInterfaceId,
            toComponent,
            fromItem.id,
            fromSlot,
            toItem.id,
            toSlot,
        )

        client.attr[INTERACTING_ITEM] = WeakReference(fromItem)
        client.attr[INTERACTING_ITEM_ID] = fromItem.id
        client.attr[INTERACTING_ITEM_SLOT] = fromSlot

        client.attr[OTHER_ITEM_ATTR] = WeakReference(toItem)
        client.attr[OTHER_ITEM_ID_ATTR] = toItem.id
        client.attr[OTHER_ITEM_SLOT_ATTR] = toSlot

        /**
         * @TODO Add support for (Any) Item on item <-- Example: Banker's note
         */
        var handled = client.world.plugins.executeItemOnItem(client, fromItem.id, toItem.id)

        /**
         * simple catchall registration to allow customizable fallback
         * for all other [on_item_on_item] interactions for a given [Item]
         * not explicitly registered
         *   Note| should be used with prejudice or for flavour
         */
        if (!handled) {
            handled = client.world.plugins.executeItemOnItem(client, fromItem.id, -1)
            if (handled && client.world.devContext.debugItemActions) {
                client.writeMessage(
                    "Unhandled item on item: [from_item=${fromItem.id}, to_item=${toItem.id}, from_slot=$fromSlot, to_slot=$toSlot, " +
                        "from_component=[$fromInterfaceId:$fromComponent], to_component=[$toInterfaceId:$toComponent]]",
                )
            }
        }

        if (!handled && client.world.devContext.debugItemActions) {
            client.writeMessage(
                "Unhandled item on item: [from_item=${fromItem.id}, to_item=${toItem.id}, from_slot=$fromSlot, to_slot=$toSlot, " +
                    "from_component=[$fromInterfaceId:$fromComponent], to_component=[$toInterfaceId:$toComponent]]",
            )
        }
    }

    /**
     * A spell dragged onto an inventory item.
     *
     * The *target* is the item, so it is the target that gets published through the interacting-item
     * attributes - the same ones an `onItemOption` plugin reads. There is no source item to publish:
     * which spell was cast is carried by the source component alone, which is exactly what the
     * binding in [org.alter.game.plugin.KotlinPlugin.onSpellOnItem] keys on.
     */
    private fun consumeSpellOnItem(
        client: Client,
        fromInterfaceId: Int,
        fromComponent: Int,
        toInterfaceId: Int,
        toComponent: Int,
        toSlot: Int,
        toItemId: Int,
    ) {
        val target = client.inventory[toSlot] ?: return
        if (target.id != toItemId) {
            return
        }

        log(
            client,
            "SpellOnItem: spell_component=[%d,%d], to_component=[%d,%d], to_item=%d, to_slot=%d",
            fromInterfaceId,
            fromComponent,
            toInterfaceId,
            toComponent,
            target.id,
            toSlot,
        )

        client.attr[INTERACTING_ITEM] = WeakReference(target)
        client.attr[INTERACTING_ITEM_ID] = target.id
        client.attr[INTERACTING_ITEM_SLOT] = toSlot

        val fromHash = (fromInterfaceId shl 16) or fromComponent
        val toHash = (toInterfaceId shl 16) or toComponent

        if (client.world.plugins.executeSpellOnItem(client, fromHash, toHash)) {
            return
        }

        /*
         * The item was resolved out of the player's inventory above, so whatever component the
         * client reported for it, it *is* an inventory item. Plugins that only care about "this
         * spell, cast on any inventory item" bind against the inventory's own component and are
         * reached here - which keeps them working whether the item was dragged from the inventory
         * tab, the bank's side panel, or any other view onto the same container.
         */
        if (toHash != INVENTORY_TARGET_HASH &&
            client.world.plugins.executeSpellOnItem(client, fromHash, INVENTORY_TARGET_HASH)
        ) {
            return
        }

        client.writeMessage(Entity.NOTHING_INTERESTING_HAPPENS)
        if (client.world.devContext.debugMagicSpells) {
            client.writeMessage(
                "Unhandled spell on item: [spell_component=[$fromInterfaceId:$fromComponent], " +
                    "to_item=${target.id}, to_component=[$toInterfaceId:$toComponent]]",
            )
        }
    }

    private companion object {
        /**
         * The inventory tab's own interface. Hardcoded rather than read from
         * `InterfaceDestination.INVENTORY`, which lives in `game-api` - a module that depends on
         * this one, not the other way around.
         */
        private const val INVENTORY_INTERFACE = 149

        /** The inventory tab's item container - the canonical "an inventory item" spell target. */
        private const val INVENTORY_TARGET_HASH = (INVENTORY_INTERFACE shl 16) or 0
    }
}
