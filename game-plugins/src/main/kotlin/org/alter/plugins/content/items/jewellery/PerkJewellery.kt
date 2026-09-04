package org.alter.plugins.content.items.jewellery

import org.alter.api.EquipmentType
import org.alter.game.model.attr.AttributeKey
import org.alter.rscm.RSCM.getRSCM

/**
 * The enchanted jewellery whose effect is a *skill* perk paid for out of a pool of charges, rather
 * than a teleport ([ChargedJewellery]) or a one-shot save ([SurvivalJewelleryPlugin]).
 *
 * These all share one unusual rule, and it is the reason they get a table of their own: **the
 * charges belong to the player, not to the item.** The wiki says so for each of them in almost the
 * same words - "the charges stored on this necklace are specific to the player, not the item
 * itself", "swapping, dropping, or alching rings before 140 charges are used will not reset the
 * number of smelts", "all expeditious bracelets share the same pool of 30 charges". So there is no
 * charge ladder of item ids the way the glory has one; there is a single item id and a number kept
 * on the player, which is what [chargesAttr] holds.
 *
 * When the last charge goes the item is destroyed and the pool resets to full, so the next one the
 * player equips starts fresh - again the published behaviour, and the same thing the "Break" option
 * does deliberately.
 *
 * The [AttributeKey]s live here rather than in `Attributes.kt` because they are a mechanical detail
 * of this table: seven near-identical declarations over there would be seven more places to keep in
 * step with it. Their persistence keys are what the save file sees and must not change.
 */
enum class PerkJewellery(
    val item: String,
    val slot: EquipmentType,
    val maxCharges: Int,
    val displayName: String,
    /** What the "Check" option counts - "uses", "smelts", "soft clay", "damage". */
    val chargeNoun: String,
    val crumbleMessage: String,
    persistenceKey: String,
) {
    /**
     * 10% + 1 of damage received, reflected. The one perk here that is not tied to a skill; it sits
     * in this table because its charges work exactly the same way and it needs the same Check and
     * Break options.
     */
    RING_OF_RECOIL(
        item = "item.ring_of_recoil",
        slot = EquipmentType.RING,
        maxCharges = 40,
        displayName = "ring of recoil",
        chargeNoun = "damage",
        crumbleMessage = "Your ring of recoil has shattered.",
        persistenceKey = "ring_of_recoil_damage_left",
    ),

    /** Thieving: a 25% chance to avoid the stun and damage from a failed pickpocket. */
    DODGY_NECKLACE(
        item = "item.dodgy_necklace",
        slot = EquipmentType.AMULET,
        maxCharges = 10,
        displayName = "dodgy necklace",
        chargeNoun = "uses",
        crumbleMessage = "Your dodgy necklace has crumbled to dust.",
        persistenceKey = "dodgy_necklace_charges",
    ),

    /** Smithing: iron smelts at 100% instead of 50%. A charge goes on every iron ore smelted. */
    RING_OF_FORGING(
        item = "item.ring_of_forging",
        slot = EquipmentType.RING,
        maxCharges = 140,
        displayName = "ring of forging",
        chargeNoun = "smelts",
        crumbleMessage = "Your ring of forging has crumbled to dust.",
        persistenceKey = "ring_of_forging_charges",
    ),

    /** Mining: clay comes out of the rock already soft. */
    BRACELET_OF_CLAY(
        item = "item.bracelet_of_clay",
        slot = EquipmentType.GLOVES,
        maxCharges = 28,
        displayName = "bracelet of clay",
        chargeNoun = "soft clay",
        crumbleMessage = "Your bracelet of clay has crumbled to dust.",
        persistenceKey = "bracelet_of_clay_charges",
    ),

    /** Slayer: a 25% chance a kill counts twice, with no extra experience. */
    EXPEDITIOUS_BRACELET(
        item = "item.expeditious_bracelet",
        slot = EquipmentType.GLOVES,
        maxCharges = 30,
        displayName = "expeditious bracelet",
        chargeNoun = "uses",
        crumbleMessage = "Your expeditious bracelet has crumbled to dust.",
        persistenceKey = "expeditious_bracelet_charges",
    ),

    /** Slayer: a 25% chance a kill does not count, with the experience still paid. */
    BRACELET_OF_SLAUGHTER(
        item = "item.bracelet_of_slaughter",
        slot = EquipmentType.GLOVES,
        maxCharges = 30,
        displayName = "bracelet of slaughter",
        chargeNoun = "uses",
        crumbleMessage = "Your bracelet of slaughter has crumbled to dust.",
        persistenceKey = "bracelet_of_slaughter_charges",
    ),

    /** Herblore: a 5% chance a three-dose potion comes out with four doses. */
    AMULET_OF_CHEMISTRY(
        item = "item.amulet_of_chemistry",
        slot = EquipmentType.AMULET,
        maxCharges = 5,
        displayName = "amulet of chemistry",
        chargeNoun = "uses",
        crumbleMessage = "Your amulet of chemistry has crumbled to dust.",
        persistenceKey = "amulet_of_chemistry_charges",
    ),
    ;

    /**
     * How many charges this player has left on this perk. Absent means full - a player who has never
     * used one starts at [maxCharges], not at zero.
     */
    val chargesAttr = AttributeKey<Int>(persistenceKey = persistenceKey)

    /** Resolved lazily: `RSCM.init()` has not necessarily run when the enum class is loaded. */
    val itemId: Int by lazy { getRSCM(item) }

    companion object {
        val values = enumValues<PerkJewellery>()
    }
}
