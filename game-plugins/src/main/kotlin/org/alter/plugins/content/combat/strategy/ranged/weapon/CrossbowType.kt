package org.alter.plugins.content.combat.strategy.ranged.weapon

import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.ADAMANT_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.BLURITE_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.BOLT_RACKS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.BONE_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.BROAD_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.BRONZE_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.DIAMOND_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.DRAGONSTONE_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.DRAGON_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.DRAGON_GEM_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.EMERALD_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.IRON_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.JADE_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.KEBBIT_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.MITHRIL_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.ONYX_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.OPAL_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.PEARL_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.RUBY_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.RUNITE_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.SAPPHIRE_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.STEEL_BOLTS
import org.alter.plugins.content.combat.strategy.ranged.ammo.Bolts.TOPAZ_BOLTS
import org.alter.rscm.RSCM.getRSCM

/*
 * Cumulative ammo tiers, following the wiki's ammunition table: each metal crossbow
 * adds its own metal bolt plus the gem bolts that sit at that tier. The gem entries
 * cover both the plain tipped bolt and its enchanted `(e)` form; the dragon gem bolts
 * are all dragon-tier and so are grouped into [DRAGON_GEM_BOLTS].
 *
 * Declared above the enum because top-level properties initialise in file order, and
 * the enum entries below read them in their constructor arguments.
 */
private val BRONZE_TIER = BRONZE_BOLTS + OPAL_BOLTS
private val IRON_TIER = BRONZE_TIER + IRON_BOLTS + PEARL_BOLTS + BLURITE_BOLTS + JADE_BOLTS
private val STEEL_TIER = IRON_TIER + STEEL_BOLTS + TOPAZ_BOLTS
private val MITHRIL_TIER = STEEL_TIER + MITHRIL_BOLTS + SAPPHIRE_BOLTS + EMERALD_BOLTS
private val ADAMANT_TIER = MITHRIL_TIER + ADAMANT_BOLTS + RUBY_BOLTS + DIAMOND_BOLTS
private val RUNE_TIER = ADAMANT_TIER + RUNITE_BOLTS + DRAGONSTONE_BOLTS + ONYX_BOLTS + BROAD_BOLTS
private val DRAGON_TIER = RUNE_TIER + DRAGON_BOLTS + DRAGON_GEM_BOLTS

/**
 * Which bolts each crossbow is allowed to fire.
 *
 * @author Tom <rspsmods@gmail.com>
 */
enum class CrossbowType(val item: Int, val ammo: Array<Int>) {
    PHOENIX_CROSSBOW(item = getRSCM("item.phoenix_crossbow"), ammo = BRONZE_TIER),
    CROSSBOW(item = getRSCM("item.crossbow"), ammo = BRONZE_TIER),

    BRONZE_CROSSBOW(item = getRSCM("item.bronze_crossbow"), ammo = BRONZE_TIER),
    IRON_CROSSBOW(item = getRSCM("item.iron_crossbow"), ammo = IRON_TIER),
    STEEL_CROSSBOW(item = getRSCM("item.steel_crossbow"), ammo = STEEL_TIER),
    MITHRIL_CROSSBOW(item = getRSCM("item.mithril_crossbow"), ammo = MITHRIL_TIER),
    ADAMANT_CROSSBOW(item = getRSCM("item.adamant_crossbow"), ammo = ADAMANT_TIER),
    RUNE_CROSSBOW(item = getRSCM("item.rune_crossbow"), ammo = RUNE_TIER),
    DRAGON_CROSSBOW(item = getRSCM("item.dragon_crossbow"), ammo = DRAGON_TIER),

    DRAGON_HUNTER_CROSSBOW(item = getRSCM("item.dragon_hunter_crossbow"), ammo = DRAGON_TIER),
    ARMADYL_CROSSBOW(item = getRSCM("item.armadyl_crossbow"), ammo = DRAGON_TIER),

    /*
     * Special-purpose crossbows that fire only their own ammo rather than a tier.
     */
    BLURITE_CROSSBOW(item = getRSCM("item.blurite_crossbow"), ammo = BRONZE_BOLTS + BLURITE_BOLTS + OPAL_BOLTS + JADE_BOLTS),
    DORGESHUUN_CROSSBOW(item = getRSCM("item.dorgeshuun_crossbow"), ammo = BONE_BOLTS),
    HUNTER_CROSSBOW(item = getRSCM("item.hunters_crossbow"), ammo = KEBBIT_BOLTS),

    KARIL_CROSSBOW(item = getRSCM("item.karils_crossbow"), ammo = BOLT_RACKS),
    KARIL_CROSSBOW_0(item = getRSCM("item.karils_crossbow_0"), ammo = BOLT_RACKS),
    KARIL_CROSSBOW_25(item = getRSCM("item.karils_crossbow_25"), ammo = BOLT_RACKS),
    KARIL_CROSSBOW_50(item = getRSCM("item.karils_crossbow_50"), ammo = BOLT_RACKS),
    KARIL_CROSSBOW_75(item = getRSCM("item.karils_crossbow_75"), ammo = BOLT_RACKS),
    KARIL_CROSSBOW_100(item = getRSCM("item.karils_crossbow_100"), ammo = BOLT_RACKS),
    ;

    companion object {
        val values = enumValues<CrossbowType>()
    }
}
