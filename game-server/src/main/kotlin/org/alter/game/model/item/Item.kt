package org.alter.game.model.item

import dev.openrune.cache.CacheManager.getItem
import gg.rsmod.util.toStringHelper
import org.bson.Document

/**
 * @author Tom <rspsmods@gmail.com>
 */
class Item(val id: Int, var amount: Int = 1) {
    constructor(other: Item) : this(other.id, other.amount) {
        copyAttr(other)
    }

    constructor(other: Item, amount: Int) : this(other.id, amount) {
        copyAttr(other)
    }

    val attr = mutableMapOf<ItemAttribute, Int>()

    /**
     * Returns a <strong>new</strong> [Item] with the noted link as the item id.
     * If this item does not have a noted link item id, it will return a new [Item]
     * with the same [Item.id].
     */
    fun toNoted(): Item {
        val def = getDef()
        return if (def.noteTemplateId == -1 && def.noteLinkId > -1) Item(def.noteLinkId, amount).copyAttr(this) else Item(this).copyAttr(this)
    }

    /**
     * Returns a <strong>new</strong> [Item] with the unnoted link as the item id.
     * If this item does not have a unnoted link item id, it will return a new [Item]
     * with the same [Item.id].
     */
    fun toUnnoted(): Item {
        val def = getDef()
        return if (def.noteTemplateId > 0) Item(def.noteLinkId, amount).copyAttr(this) else Item(this).copyAttr(this)
    }

    /**
     * Returns a <strong>new</strong> [Item] with the item a bank placeholder stands for,
     * or the same id if this is not a placeholder.
     *
     * A placeholder is a separate item id whose only job is to hold a bank slot open. It
     * carries the real item's model and name through its template, so in an inventory it is
     * indistinguishable from the real thing - but it is a different id, so no content
     * recognises it. Item 19107 is the Bucket's placeholder: it looks like a bucket, and
     * using it on a fountain or a dairy cow does nothing at all.
     *
     * Anything that hands an item to a player from an id the player chose - the spawn
     * commands, the cheat menu - should pass it through here first, or that is exactly the
     * item they end up holding.
     */
    fun unwrapPlaceholder(): Item {
        val def = getDef()
        return if (def.placeholderTemplate > 0 && def.placeholderLink > 0) {
            Item(def.placeholderLink, amount).copyAttr(this)
        } else {
            Item(this).copyAttr(this)
        }
    }

    /**
     * Get the name of this item. If this item is noted this method will use
     * its un-noted template and get the name for said template.
     */
    fun getName(): String = toUnnoted().getDef().name

    fun getDef() = getItem(id)

    /**
     * Returns true if [attr] contains any value.
     */
    fun hasAnyAttr(): Boolean = attr.isNotEmpty()

    fun getAttr(attrib: ItemAttribute): Int? = attr[attrib]

    fun putAttr(
        attrib: ItemAttribute,
        value: Int,
    ): Item {
        attr[attrib] = value
        return this
    }

    /**
     * Copies the [Item.attr] map from [other] to this.
     */
    fun copyAttr(other: Item): Item {
        if (other.hasAnyAttr()) {
            attr.putAll(other.attr)
        }
        return this
    }

    override fun toString(): String = toStringHelper().add("id", id).add("amount", amount).toString()

    fun asDocument(): Document {
        return Document("id", id).apply {
            append("amount", amount)

            if (attr.isNotEmpty()) {
                val attributesDoc = attr.map { (attribute, value) ->
                    attribute.name to value
                }.toMap()
                append("attributes", attributesDoc)
            }
        }
    }

    companion object {
        fun fromDocument(doc: Document): Item {
            val id = doc.getInteger("id")
            val amount = doc.getInteger("amount")
            val item = Item(id, amount)

            val attributesDoc = doc.get("attributes", Document::class.java)
            attributesDoc?.forEach { (key, value) ->
                if (value is Int) {
                    val attribute = ItemAttribute.valueOf(key)
                    item.putAttr(attribute, value)
                }
            }

            return item
        }
    }

}
