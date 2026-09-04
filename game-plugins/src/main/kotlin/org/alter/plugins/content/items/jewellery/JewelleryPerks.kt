package org.alter.plugins.content.items.jewellery

import org.alter.api.ext.getEquipment
import org.alter.api.ext.message
import org.alter.game.model.entity.Player
import org.alter.rscm.RSCM.getRSCM

/**
 * The charge bookkeeping behind [PerkJewellery], and the handful of one-line questions the skills
 * ask it.
 *
 * Skill plugins call the named helpers at the bottom rather than the charge primitives, so a skill
 * never has to know how many charges a piece holds, whether it is worn, or what happens when it runs
 * out - `MiningPlugin` asks "what does this rock yield for this player", `Slayer` asks "how much does
 * this kill count for", and the answer arrives with the charge already spent.
 *
 * The messages a proc prints are this project's wording, not OSRS's. The wiki documents every one of
 * these effects and their charge counts but quotes none of the chat lines, so the rates, the charge
 * costs and what crumbles when are faithful while the sentences are ours.
 */
object JewelleryPerks {
    /** A quarter, the shared proc rate of the dodgy necklace and both Slayer bracelets. */
    private const val QUARTER = 0.25

    /** The amulet of chemistry's much rarer proc. */
    private const val CHEMISTRY_CHANCE = 0.05

    /** Charges left on [perk] for [player]. A player who has never used one has a full pool. */
    fun remaining(
        player: Player,
        perk: PerkJewellery,
    ): Int = player.attr[perk.chargesAttr] ?: perk.maxCharges

    /** True when [perk] is worn in its slot and still has a charge to spend. */
    fun isActive(
        player: Player,
        perk: PerkJewellery,
    ): Boolean = player.getEquipment(perk.slot)?.id == perk.itemId && remaining(player, perk) > 0

    /**
     * Spends [amount] charges. When the pool empties the worn piece is destroyed and the pool resets
     * to full, which is what makes the *next* piece the player equips a fresh one - the published
     * behaviour for every item in this table.
     */
    fun consume(
        player: Player,
        perk: PerkJewellery,
        amount: Int = 1,
    ) {
        val left = remaining(player, perk) - amount
        if (left > 0) {
            player.attr[perk.chargesAttr] = left
            return
        }
        player.attr[perk.chargesAttr] = perk.maxCharges
        if (player.getEquipment(perk.slot)?.id == perk.itemId) {
            player.equipment[perk.slot.id] = null
        }
        player.message("<col=7f007f>${perk.crumbleMessage}</col>")
    }

    /** Destroys the pool without an effect firing - the "Break" option. */
    fun reset(
        player: Player,
        perk: PerkJewellery,
    ) {
        player.attr[perk.chargesAttr] = perk.maxCharges
    }

    /**
     * Rolls [chance] against a worn, charged [perk] and spends a charge if it comes up. Returns
     * whether the effect fired.
     */
    private fun proc(
        player: Player,
        perk: PerkJewellery,
        chance: Double,
    ): Boolean {
        if (!isActive(player, perk)) {
            return false
        }
        if (player.world.randomDouble() > chance) {
            return false
        }
        consume(player, perk)
        return true
    }

    // ------------------------------------------------------------------------------------------
    // What the skills ask.
    // ------------------------------------------------------------------------------------------

    /**
     * Thieving. Whether a dodgy necklace absorbs the stun and damage from a failed pickpocket.
     *
     * A charge is spent only when it actually saves the player, not on every failure - "one charge
     * is consumed each time the necklace successfully prevents damage".
     */
    fun dodgyNecklaceSaves(player: Player): Boolean {
        if (!proc(player, PerkJewellery.DODGY_NECKLACE, QUARTER)) {
            return false
        }
        player.message("Your dodgy necklace protects you.")
        return true
    }

    /**
     * Smithing. Whether a ring of forging makes this smelt succeed regardless of the roll.
     *
     * Unlike the dodgy necklace, this spends a charge on *every* iron ore smelted while the ring is
     * worn, whether or not the smelt would have succeeded anyway - which is why the check is not
     * folded into the failure branch.
     *
     * Iron is named explicitly rather than inferred from "any recipe that can fail". Iron happens to
     * be the only smelt below 100% in this project today, but the ring is an iron-ore item in OSRS
     * and should not start guaranteeing some later recipe that also rolls.
     */
    fun ringOfForgingGuarantees(
        player: Player,
        barItemId: Int,
    ): Boolean {
        if (barItemId != ironBar || !isActive(player, PerkJewellery.RING_OF_FORGING)) {
            return false
        }
        consume(player, PerkJewellery.RING_OF_FORGING)
        return true
    }

    /**
     * Mining. The item a rock actually yields: soft clay in place of clay while a bracelet of clay is
     * worn, and [oreItemId] unchanged for everything else.
     */
    fun miningYield(
        player: Player,
        oreItemId: Int,
    ): Int {
        if (oreItemId != clay || !isActive(player, PerkJewellery.BRACELET_OF_CLAY)) {
            return oreItemId
        }
        consume(player, PerkJewellery.BRACELET_OF_CLAY)
        return softClay
    }

    /**
     * Slayer. How much this kill counts towards the task: two for an expeditious bracelet, zero for a
     * bracelet of slaughter, one otherwise.
     *
     * Both are gloves-slot, so they can never both apply; the expeditious bracelet is checked first
     * only because one of them has to be. Neither changes the Slayer experience - the expeditious
     * bracelet's extra kill grants none, and the slaughter bracelet's skipped kill still pays - so
     * this is called *after* the experience has been awarded.
     */
    fun slayerKillCount(player: Player): Int {
        if (proc(player, PerkJewellery.EXPEDITIOUS_BRACELET, QUARTER)) {
            player.message("Your expeditious bracelet helps you progress your task faster.")
            return 2
        }
        if (proc(player, PerkJewellery.BRACELET_OF_SLAUGHTER, QUARTER)) {
            player.message("Your bracelet of slaughter prevents your slayer count from decreasing.")
            return 0
        }
        return 1
    }

    /**
     * Herblore. The potion a mix actually produces: the four-dose version of [productId] when an
     * amulet of chemistry procs, and [productId] unchanged otherwise.
     *
     * Only a three-dose product can be upgraded. That falls out of [PotionDoses] rather than being
     * special-cased: a recipe whose product is not the three-dose item of some known potion - an
     * unfinished potion, or one of the mixes that already yields four doses - has no four-dose
     * counterpart to return, which is exactly the wiki's rule that the amulet "provides no benefit
     * when making potions that always output 4 doses".
     */
    fun potionYield(
        player: Player,
        productId: Int,
    ): Int {
        val fourDose = PotionDoses.fourDoseOf(productId) ?: return productId
        if (!proc(player, PerkJewellery.AMULET_OF_CHEMISTRY, CHEMISTRY_CHANCE)) {
            return productId
        }
        player.message("Your amulet of chemistry helps you create a 4-dose potion.")
        return fourDose
    }

    private val ironBar: Int by lazy { getRSCM("item.iron_bar") }
    private val clay: Int by lazy { getRSCM("item.clay") }
    private val softClay: Int by lazy { getRSCM("item.soft_clay") }
}
