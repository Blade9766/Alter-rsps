package org.alter.plugins.content.items.blowpipe

import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.item.ItemAttribute
import org.alter.rscm.RSCM.getRSCM

/**
 * Dart and Zulrah's scale charges for the toxic blowpipe family.
 *
 * The storage this uses was already designed for exactly this item - [ItemAttribute]'s
 * own doc comments name the toxic blowpipe three times ("Toxic blowpipe can have darts
 * attached to it", and an attack counter "resetting every three attacks"). It just had
 * never been implemented; there were no blowpipe references anywhere in the codebase.
 *
 * Charges live on the [Item] instance rather than on the player, so two blowpipes hold
 * their own darts, and they survive logout because `Item.asDocument` already serialises
 * the attribute map. A side effect worth knowing: an item carrying any attribute stops
 * stacking and becomes untradeable, which is correct for a charged blowpipe anyway.
 *
 * Scales are consumed on roughly two shots in three - the wiki gives "a 1/3 chance to
 * not use scales when firing; thus, on average, two scales are used for every three
 * shots fired". That is a per-shot roll, not a fixed cycle, so no attack counter is
 * kept.
 *
 * Darts are ordinary ammo once fired: they are consumed per shot and follow the same
 * Ava's recover/break/drop rules as arrows, which is why the firing side lives in the
 * ranged ammo code rather than here.
 */
object Blowpipe {
    /** Both hold "up to 16,383 scales and 16,383 darts". */
    const val MAX_DARTS = 16_383
    const val MAX_SCALES = 16_383

    /** 1 in 3 shots costs no scale. */
    private const val SCALE_SAVE_DENOMINATOR = 3

    val TOXIC: Int by lazy { getRSCM("item.toxic_blowpipe") }
    val TOXIC_EMPTY: Int by lazy { getRSCM("item.toxic_blowpipe_empty") }
    val BLAZING: Int by lazy { getRSCM("item.blazing_blowpipe") }
    val BLAZING_EMPTY: Int by lazy { getRSCM("item.blazing_blowpipe_empty") }

    val SCALES: Int by lazy { getRSCM("item.zulrahs_scales") }

    /**
     * Only unpoisoned darts load into a blowpipe. These are the plain ids from
     * [org.alter.plugins.content.combat.strategy.ranged.ammo.Darts], whose arrays also
     * carry the (p), (p+) and (p++) variants.
     */
    val LOADABLE_DARTS: List<String> =
        listOf(
            "item.bronze_dart",
            "item.iron_dart",
            "item.steel_dart",
            "item.black_dart",
            "item.mithril_dart",
            "item.adamant_dart",
            "item.rune_dart",
            "item.dragon_dart",
        )

    fun isCharged(item: Item?): Boolean = item != null && (item.id == TOXIC || item.id == BLAZING)

    fun isEmpty(item: Item?): Boolean = item != null && (item.id == TOXIC_EMPTY || item.id == BLAZING_EMPTY)

    /** The charged id matching an empty shell, so a Blazing shell stays Blazing. */
    fun chargedFormOf(emptyId: Int): Int = if (emptyId == BLAZING_EMPTY) BLAZING else TOXIC

    /** The empty shell matching a charged blowpipe. */
    fun emptyFormOf(chargedId: Int): Int = if (chargedId == BLAZING) BLAZING_EMPTY else TOXIC_EMPTY

    fun dartId(blowpipe: Item): Int = blowpipe.getAttr(ItemAttribute.ATTACHED_ITEM_ID) ?: -1

    fun dartCount(blowpipe: Item): Int = blowpipe.getAttr(ItemAttribute.ATTACHED_ITEM_COUNT) ?: 0

    fun scaleCount(blowpipe: Item): Int = blowpipe.getAttr(ItemAttribute.CHARGES) ?: 0

    /** A blowpipe can only fire while it holds both a dart and a scale. */
    fun canFire(blowpipe: Item): Boolean = dartCount(blowpipe) > 0 && scaleCount(blowpipe) > 0

    fun setDarts(
        blowpipe: Item,
        dartId: Int,
        count: Int,
    ) {
        if (count <= 0) {
            blowpipe.attr.remove(ItemAttribute.ATTACHED_ITEM_ID)
            blowpipe.attr.remove(ItemAttribute.ATTACHED_ITEM_COUNT)
            return
        }
        blowpipe.putAttr(ItemAttribute.ATTACHED_ITEM_ID, dartId)
        blowpipe.putAttr(ItemAttribute.ATTACHED_ITEM_COUNT, count)
    }

    fun setScales(
        blowpipe: Item,
        count: Int,
    ) {
        if (count <= 0) {
            blowpipe.attr.remove(ItemAttribute.CHARGES)
            return
        }
        blowpipe.putAttr(ItemAttribute.CHARGES, count)
    }

    /** Removes one dart. Call only after deciding the dart is not recovered. */
    fun consumeDart(blowpipe: Item) = setDarts(blowpipe, dartId(blowpipe), dartCount(blowpipe) - 1)

    /**
     * Spends a scale on this shot, two times in three. Returns false only when the
     * blowpipe was already out of scales, which the caller should have prevented.
     */
    fun consumeScale(
        blowpipe: Item,
        world: World,
    ): Boolean {
        val scales = scaleCount(blowpipe)
        if (scales <= 0) {
            return false
        }
        if (world.chance(1, SCALE_SAVE_DENOMINATOR)) {
            return true // the free shot
        }
        setScales(blowpipe, scales - 1)
        return true
    }

    /** The blowpipe the player is currently wielding, if it is a charged one. */
    fun equipped(player: Player): Item? =
        player.equipment[org.alter.api.EquipmentType.WEAPON.id]?.takeIf { isCharged(it) }
}
