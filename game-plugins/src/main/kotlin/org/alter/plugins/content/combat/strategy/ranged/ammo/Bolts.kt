package org.alter.plugins.content.combat.strategy.ranged.ammo

import org.alter.rscm.RSCM.getRSCM
/**
 * @author Tom <rspsmods@gmail.com>
 */
object Bolts {
    val BRONZE_BOLTS = arrayOf(getRSCM("item.bronze_bolts"), getRSCM("item.bronze_bolts_p"), getRSCM("item.bronze_bolts_p_6061"), getRSCM("item.bronze_bolts_p_6062"))
    val IRON_BOLTS = arrayOf(getRSCM("item.iron_bolts"), getRSCM("item.iron_bolts_p"), getRSCM("item.iron_bolts_p_9294"), getRSCM("item.iron_bolts_p_9301"))
    val STEEL_BOLTS = arrayOf(getRSCM("item.steel_bolts"), getRSCM("item.steel_bolts_p"), getRSCM("item.steel_bolts_p_9295"), getRSCM("item.steel_bolts_p_9302"))
    val MITHRIL_BOLTS = arrayOf(getRSCM("item.mithril_bolts"), getRSCM("item.mithril_bolts_p"), getRSCM("item.mithril_bolts_p_9296"), getRSCM("item.mithril_bolts_p_9303"))
    val ADAMANT_BOLTS = arrayOf(getRSCM("item.adamant_bolts"), getRSCM("item.adamant_bolts_p"), getRSCM("item.adamant_bolts_p_9297"), getRSCM("item.adamant_bolts_p_9304"))
    val BROAD_BOLTS = arrayOf(getRSCM("item.broad_bolts"), getRSCM("item.amethyst_broad_bolts"))
    val RUNITE_BOLTS = arrayOf(getRSCM("item.runite_bolts"), getRSCM("item.runite_bolts_p"), getRSCM("item.runite_bolts_p_9298"), getRSCM("item.runite_bolts_p_9305"))
    val DRAGON_BOLTS = arrayOf(getRSCM("item.dragon_bolts"), getRSCM("item.dragon_bolts_p"), getRSCM("item.dragon_bolts_p_21926"), getRSCM("item.dragon_bolts_p_21928"))
    val BLURITE_BOLTS = arrayOf(getRSCM("item.blurite_bolts"), getRSCM("item.blurite_bolts_p"), getRSCM("item.blurite_bolts_p_9293"), getRSCM("item.blurite_bolts_p_9300"))
    val BONE_BOLTS = arrayOf(getRSCM("item.bone_bolts"))
    val KEBBIT_BOLTS = arrayOf(getRSCM("item.kebbit_bolts"), getRSCM("item.long_kebbit_bolts"))
    val BOLT_RACKS = arrayOf(getRSCM("item.bolt_rack"))

    /**
     * A gem bolt's regular ids - the plain tipped bolt and its enchanted `(e)` form.
     * Only the `(e)` form carries an effect (see [EnchantedBolt]); both are valid ammo.
     */
    private fun gem(gem: String) = arrayOf(getRSCM("item.${gem}_bolts"), getRSCM("item.${gem}_bolts_e"))

    /** The dragon-tier equivalents of [gem], which need a dragon crossbow to fire. */
    private fun dragonGem(gem: String) = arrayOf(getRSCM("item.${gem}_dragon_bolts"), getRSCM("item.${gem}_dragon_bolts_e"))

    val OPAL_BOLTS = gem("opal")
    val JADE_BOLTS = gem("jade")
    val PEARL_BOLTS = gem("pearl")
    val TOPAZ_BOLTS = gem("topaz")
    val SAPPHIRE_BOLTS = gem("sapphire")
    val EMERALD_BOLTS = gem("emerald")
    val RUBY_BOLTS = gem("ruby")
    val DIAMOND_BOLTS = gem("diamond")
    val DRAGONSTONE_BOLTS = gem("dragonstone")
    val ONYX_BOLTS = gem("onyx")

    /**
     * Every dragon gem bolt. Grouped as one array because they all share the same
     * crossbow requirement as plain dragon bolts.
     */
    val DRAGON_GEM_BOLTS =
        dragonGem("opal") + dragonGem("jade") + dragonGem("pearl") + dragonGem("topaz") + dragonGem("sapphire") +
            dragonGem("emerald") + dragonGem("ruby") + dragonGem("diamond") + dragonGem("dragonstone") + dragonGem("onyx")
}
