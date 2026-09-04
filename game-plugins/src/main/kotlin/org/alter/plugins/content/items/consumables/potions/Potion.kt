package org.alter.plugins.content.items.consumables.potions

import org.alter.api.Skills
import org.alter.plugins.content.items.consumables.Antifire
import org.alter.plugins.content.items.consumables.Boost
import org.alter.plugins.content.items.consumables.ConsumableEffect
import org.alter.plugins.content.items.consumables.CureDisease
import org.alter.plugins.content.items.consumables.CurePoison
import org.alter.plugins.content.items.consumables.Damage
import org.alter.plugins.content.items.consumables.DivineBoost
import org.alter.plugins.content.items.consumables.Drain
import org.alter.plugins.content.items.consumables.Heal
import org.alter.plugins.content.items.consumables.HealOnExpiry
import org.alter.plugins.content.items.consumables.HoldStats
import org.alter.plugins.content.items.consumables.PrayerRegen
import org.alter.plugins.content.items.consumables.Restore
import org.alter.plugins.content.items.consumables.RestoreEnergy
import org.alter.plugins.content.items.consumables.RestorePrayer
import org.alter.plugins.content.items.consumables.Stamina

/**
 * Every drinkable potion, with the item behind each of its doses and what a dose does.
 *
 * Boost and restore figures are the published Old School formulas: a flat amount plus a percentage
 * of the player's base level in that skill, floored. Drains work from the *current* level instead,
 * so repeated doses of a brew take progressively less.
 *
 * Coverage is checked against the cache rather than against memory: every item whose inventory
 * options carry "Drink" was dumped from the item definitions and matched off against this list.
 * What is deliberately *not* here is drink that is not a potion - ale, wine, tea and cocktails,
 * which are [Food][org.alter.plugins.content.items.consumables.food.Food]-shaped and belong there -
 * and a handful of items whose effect has no machinery behind it yet: Nightmare Zone absorption
 * potions (absorption points), liquid adrenaline (special attack cost), rejuvenation potions
 * (Wintertodt warmth) and the quest transformation potions.
 */
enum class Potion(
    val doses: List<String>,
    val effects: List<ConsumableEffect>,
    val emptied: String = "item.vial",
    /**
     * Hitpoints the player must have before a dose will go down. Set by the potions that cost
     * hitpoints to drink - the divine family and the overloads - because the game will not let a
     * dose kill.
     */
    val minHitpoints: Int = 0,
    /**
     * What to tell the player when a held effect - a divine boost, an overload, a Menaphite
     * remedy - runs out. Ignored by every potion that holds nothing.
     */
    val expiryMessage: String = "Your divine potion has expired.",
) {
    /**
     * Combat boosts.
     */
    ATTACK(doses("attack_potion"), ATTACK_EFFECTS),
    STRENGTH(doses("strength_potion"), STRENGTH_EFFECTS),
    DEFENCE(doses("defence_potion"), DEFENCE_EFFECTS),
    SUPER_ATTACK(doses("super_attack"), SUPER_ATTACK_EFFECTS),
    SUPER_STRENGTH(doses("super_strength"), SUPER_STRENGTH_EFFECTS),
    SUPER_DEFENCE(doses("super_defence"), SUPER_DEFENCE_EFFECTS),
    COMBAT(doses("combat_potion"), COMBAT_EFFECTS),
    SUPER_COMBAT(doses("super_combat_potion"), SUPER_COMBAT_EFFECTS),
    RANGING(doses("ranging_potion"), RANGING_EFFECTS),
    SUPER_RANGING(doses("super_ranging_"), listOf(Boost(Skills.RANGED, 5, 15))),
    MAGIC(doses("magic_potion"), MAGIC_EFFECTS),
    SUPER_MAGIC(underscored("super_magic_potion"), listOf(Boost(Skills.MAGIC, 5, 15))),

    /**
     * The two hybrid boosts: a ranging or magic potion welded to a super defence.
     */
    BASTION(doses("bastion_potion"), BASTION_EFFECTS),
    BATTLEMAGE(doses("battlemage_potion"), BATTLEMAGE_EFFECTS),

    /**
     * Divine potions. The same boost as the potion each is named after, held flat for five minutes
     * instead of decaying, bought with ten hitpoints a dose.
     */
    DIVINE_SUPER_ATTACK(
        doses("divine_super_attack_potion"),
        divine(DivineBoost(Skills.ATTACK, 5, 15)),
        minHitpoints = Divine.MIN_HITPOINTS,
    ),
    DIVINE_SUPER_STRENGTH(
        doses("divine_super_strength_potion"),
        divine(DivineBoost(Skills.STRENGTH, 5, 15)),
        minHitpoints = Divine.MIN_HITPOINTS,
    ),
    DIVINE_SUPER_DEFENCE(
        doses("divine_super_defence_potion"),
        divine(DivineBoost(Skills.DEFENCE, 5, 15)),
        minHitpoints = Divine.MIN_HITPOINTS,
    ),
    DIVINE_SUPER_COMBAT(
        doses("divine_super_combat_potion"),
        divine(
            DivineBoost(Skills.ATTACK, 5, 15),
            DivineBoost(Skills.STRENGTH, 5, 15),
            DivineBoost(Skills.DEFENCE, 5, 15),
        ),
        minHitpoints = Divine.MIN_HITPOINTS,
    ),
    DIVINE_RANGING(
        doses("divine_ranging_potion"),
        divine(DivineBoost(Skills.RANGED, 4, 10)),
        minHitpoints = Divine.MIN_HITPOINTS,
    ),
    DIVINE_MAGIC(
        doses("divine_magic_potion"),
        divine(DivineBoost(Skills.MAGIC, 4, 0)),
        minHitpoints = Divine.MIN_HITPOINTS,
    ),
    DIVINE_BASTION(
        doses("divine_bastion_potion"),
        divine(DivineBoost(Skills.RANGED, 4, 10), DivineBoost(Skills.DEFENCE, 5, 15)),
        minHitpoints = Divine.MIN_HITPOINTS,
    ),
    DIVINE_BATTLEMAGE(
        doses("divine_battlemage_potion"),
        divine(DivineBoost(Skills.MAGIC, 4, 0), DivineBoost(Skills.DEFENCE, 5, 15)),
        minHitpoints = Divine.MIN_HITPOINTS,
    ),

    /**
     * Brews. All of them trade one set of stats for another, and the Zamorak brew's hitpoints drain
     * stops at 1 rather than killing.
     */
    ZAMORAK_BREW(doses("zamorak_brew"), ZAMORAK_EFFECTS),
    SARADOMIN_BREW(doses("saradomin_brew"), SARADOMIN_EFFECTS),
    ANCIENT_BREW(doses("ancient_brew"), ANCIENT_BREW_EFFECTS),
    FORGOTTEN_BREW(doses("forgotten_brew"), FORGOTTEN_BREW_EFFECTS),

    /**
     * Restoratives. A prayer restore is worth more while a prayer-restoring item is worn.
     */
    PRAYER(doses("prayer_potion"), PRAYER_EFFECTS),
    RESTORE(doses("restore_potion"), RESTORE_EFFECTS),
    SUPER_RESTORE(doses("super_restore"), SUPER_RESTORE_EFFECTS),
    SANFEW_SERUM(doses("sanfew_serum"), SANFEW_EFFECTS),

    /**
     * Restores the player's combat stats every fifteen seconds for five minutes rather than once,
     * which the divine floor mechanic reproduces exactly: for the duration the restore cycle can no
     * longer take a drained stat below its base.
     *
     * The live potion also cancels divine boosts and neutralises brew drains outright; neither is
     * modelled here.
     */
    MENAPHITE_REMEDY(
        doses("menaphite_remedy"),
        listOf(
            Restore(6, 16) { it in COMBAT_STATS },
            HoldStats { it in COMBAT_STATS },
        ),
        expiryMessage = "Your Menaphite remedy has worn off.",
    ),

    /**
     * Run energy.
     */
    ENERGY(doses("energy_potion"), ENERGY_EFFECTS),
    SUPER_ENERGY(doses("super_energy"), SUPER_ENERGY_EFFECTS),
    STAMINA(doses("stamina_potion"), STAMINA_EFFECTS),

    /**
     * Non-combat skill boosts.
     */
    AGILITY(doses("agility_potion"), AGILITY_EFFECTS),
    FISHING(doses("fishing_potion"), FISHING_EFFECTS),
    HUNTER(doses("hunter_potion"), HUNTER_EFFECTS),
    MAGIC_ESSENCE(doses("magic_essence"), MAGIC_ESSENCE_EFFECTS),

    /**
     * Poison, venom and disease.
     */
    ANTIPOISON(doses("antipoison"), ANTIPOISON_EFFECTS),
    SUPERANTIPOISON(doses("superantipoison"), SUPERANTIPOISON_EFFECTS),
    ANTIDOTE_PLUS(doses("antidote"), ANTIDOTE_PLUS_EFFECTS),

    /**
     * The generated item names for antidote++ collide with antidote+, so its doses carry the item id
     * as a suffix.
     */
    ANTIDOTE_PLUS_PLUS(
        listOf("item.antidote1_5958", "item.antidote2_5956", "item.antidote3_5954", "item.antidote4_5952"),
        ANTIDOTE_PLUS_PLUS_EFFECTS,
    ),

    /**
     * The anti-venoms. Venom itself is not modelled - nothing inflicts it - so all three cure poison
     * and grant the twelve minutes of poison immunity the wiki gives them; the separate, much
     * shorter venom immunity has nothing to apply to yet.
     */
    ANTIVENOM(doses("antivenom"), ANTIVENOM_EFFECTS),
    ANTIVENOM_PLUS(
        listOf("item.antivenom1_12919", "item.antivenom2_12917", "item.antivenom3_12915", "item.antivenom4_12913"),
        ANTIVENOM_EFFECTS,
    ),
    EXTENDED_ANTIVENOM_PLUS(doses("extended_antivenom"), ANTIVENOM_EFFECTS),

    /**
     * Relicym's balm cures disease, which nothing inflicts - see [CureDisease].
     */
    RELICYMS_BALM(doses("relicyms_balm"), RELICYM_EFFECTS),

    /**
     * Dragonfire.
     */
    ANTIFIRE(doses("antifire_potion"), ANTIFIRE_EFFECTS),
    EXTENDED_ANTIFIRE(doses("extended_antifire"), EXTENDED_ANTIFIRE_EFFECTS),
    SUPER_ANTIFIRE(doses("super_antifire_potion"), SUPER_ANTIFIRE_EFFECTS),
    EXTENDED_SUPER_ANTIFIRE(doses("extended_super_antifire"), EXTENDED_SUPER_ANTIFIRE_EFFECTS),

    /**
     * Barbarian mixes: the base potion's effect plus a few hitpoints, in two doses instead of four.
     * Everything below 33 Herblore heals three, everything above heals six - see the Herblore table
     * in Barbarian Training.
     */
    ATTACK_MIX(mix("attack_mix"), ATTACK_EFFECTS + Heal(3)),
    ANTIPOISON_MIX(mix("antipoison_mix"), ANTIPOISON_EFFECTS + Heal(3)),
    RELICYMS_MIX(mix("relicyms_mix"), RELICYM_EFFECTS + Heal(3)),
    STRENGTH_MIX(mix("strength_mix"), STRENGTH_EFFECTS + Heal(3)),
    RESTORE_MIX(mix("restore_mix"), RESTORE_EFFECTS + Heal(3)),
    ENERGY_MIX(mix("energy_mix"), ENERGY_EFFECTS + Heal(3)),
    COMBAT_MIX(mix("combat_mix"), COMBAT_EFFECTS + Heal(3)),
    DEFENCE_MIX(mix("defence_mix"), DEFENCE_EFFECTS + Heal(6)),
    AGILITY_MIX(mix("agility_mix"), AGILITY_EFFECTS + Heal(6)),
    PRAYER_MIX(mix("prayer_mix"), PRAYER_EFFECTS + Heal(6)),
    SUPER_ATTACK_MIX(mix("superattack_mix"), SUPER_ATTACK_EFFECTS + Heal(6)),
    ANTIPOISON_SUPERMIX(mix("antipoison_supermix"), ANTIDOTE_PLUS_PLUS_EFFECTS + Heal(6)),
    FISHING_MIX(mix("fishing_mix"), FISHING_EFFECTS + Heal(6)),
    SUPER_ENERGY_MIX(mix("super_energy_mix"), SUPER_ENERGY_EFFECTS + Heal(6)),
    HUNTING_MIX(mix("hunting_mix"), HUNTER_EFFECTS + Heal(6)),
    SUPER_STRENGTH_MIX(mix("super_str_mix"), SUPER_STRENGTH_EFFECTS + Heal(6)),
    MAGIC_ESSENCE_MIX(mix("magic_essence_mix"), MAGIC_ESSENCE_EFFECTS + Heal(6)),
    SUPER_RESTORE_MIX(mix("super_restore_mix"), SUPER_RESTORE_EFFECTS + Heal(6)),
    SUPER_DEFENCE_MIX(mix("super_def_mix"), SUPER_DEFENCE_EFFECTS + Heal(6)),
    ANTIDOTE_PLUS_MIX(mix("antidote_mix"), ANTIDOTE_PLUS_EFFECTS + Heal(6)),
    ANTIFIRE_MIX(mix("antifire_mix"), ANTIFIRE_EFFECTS + Heal(6)),
    RANGING_MIX(mix("ranging_mix"), RANGING_EFFECTS + Heal(6)),
    MAGIC_MIX(mix("magic_mix"), MAGIC_EFFECTS + Heal(6)),
    ZAMORAK_MIX(mix("zamorak_mix"), ZAMORAK_EFFECTS + Heal(6)),
    STAMINA_MIX(mix("stamina_mix"), STAMINA_EFFECTS + Heal(6)),
    EXTENDED_ANTIFIRE_MIX(mix("extended_antifire_mix"), EXTENDED_ANTIFIRE_EFFECTS + Heal(6)),
    ANCIENT_MIX(mix("ancient_mix"), ANCIENT_BREW_EFFECTS + Heal(6)),
    SUPER_ANTIFIRE_MIX(mix("super_antifire_mix"), SUPER_ANTIFIRE_EFFECTS + Heal(6)),
    EXTENDED_SUPER_ANTIFIRE_MIX(mix("extended_super_antifire_mix"), EXTENDED_SUPER_ANTIFIRE_EFFECTS + Heal(6)),

    /**
     * Hunter's mixes, the Varlamore butterfly-jar equivalents of the barbarian mixes. These are not
     * a base potion plus healing - each is its own effect.
     */
    RUBY_HARVEST_MIX(underscored("ruby_harvest_mix", 2), listOf(Boost(Skills.ATTACK, 4, 15))),
    BLACK_WARLOCK_MIX(underscored("black_warlock_mix", 2), listOf(Boost(Skills.STRENGTH, 4, 15))),
    SAPPHIRE_GLACIALIS_MIX(underscored("sapphire_glacialis_mix", 2), listOf(Boost(Skills.DEFENCE, 4, 15))),
    SNOWY_KNIGHT_MIX(underscored("snowy_knight_mix", 2), listOf(Heal(8))),
    SUNLIGHT_MOTH_MIX(
        underscored("sunlight_moth_mix", 2),
        listOf(Restore(6, 20) { it != Skills.HITPOINTS && it != Skills.PRAYER }, Heal(8)),
    ),
    MOONLIGHT_MOTH_MIX(underscored("moonlight_moth_mix", 2), listOf(RestorePrayer(flat = 22, percent = 0))),

    /**
     * Varlamore's prayer regeneration potion: one prayer point every twelve ticks for eight
     * minutes, 66 points in all. [PrayerRegen] derives the interval from the total, so 66 points
     * over 800 ticks is exactly the twelve the wiki gives.
     */
    PRAYER_REGENERATION(doses("prayer_regeneration_potion"), listOf(PrayerRegen(800, 66, 0))),

    /**
     * Guthix rest is brewed in a cup rather than a vial, so the last dose hands one back.
     *
     * Its third effect - stepping venom down to poison, or poison down a level - has nothing to
     * apply to while venom is unmodelled and poison is all-or-nothing here.
     */
    GUTHIX_REST(
        doses("guthix_rest"),
        listOf(Heal(5, overheal = 5), RestoreEnergy(5)),
        emptied = "item.empty_cup",
    ),

    /**
     * Sq'irk juice is squeezed into a beer glass and comes in one dose only.
     */
    WINTER_SQIRKJUICE(listOf("item.winter_sqirkjuice"), listOf(RestoreEnergy(5)), emptied = "item.beer_glass"),
    SPRING_SQIRKJUICE(
        listOf("item.spring_sqirkjuice"),
        listOf(Boost(Skills.THIEVING, 1, 0), RestoreEnergy(10)),
        emptied = "item.beer_glass",
    ),
    AUTUMN_SQIRKJUICE(
        listOf("item.autumn_sqirkjuice"),
        listOf(Boost(Skills.THIEVING, 2, 0), RestoreEnergy(15)),
        emptied = "item.beer_glass",
    ),
    SUMMER_SQIRKJUICE(
        listOf("item.summer_sqirkjuice"),
        listOf(Boost(Skills.THIEVING, 3, 0), RestoreEnergy(20)),
        emptied = "item.beer_glass",
    ),

    /**
     * The Gauntlet's own potion: a prayer restore and a stamina dose in one.
     */
    EGNIOL(
        underscored("egniol_potion"),
        listOf(RestorePrayer(flat = 7, percent = 25), RestoreEnergy(40), Stamina(ticks = 200)),
    ),

    /**
     * Moonlight potion. The live potion scales every part of itself off the drinker's *Herblore*
     * level; these are the top-tier figures (Herblore 70+), and the lower brackets are not modelled.
     */
    MOONLIGHT(
        doses("moonlight_potion"),
        listOf(
            RestorePrayer(flat = 7, percent = 25),
            Boost(Skills.ATTACK, 5, 15),
            Boost(Skills.STRENGTH, 5, 15),
            Boost(Skills.DEFENCE, 7, 20),
        ),
    ),

    /**
     * Soul Wars' potion of power - a super combat, a ranging and magic boost, a super restore and a
     * prayer potion in one dose. Its Soul Wars activity bonus has no minigame to apply to.
     */
    POTION_OF_POWER(
        doses("potion_of_power"),
        listOf(
            Boost(Skills.ATTACK, 5, 15),
            Boost(Skills.STRENGTH, 5, 15),
            Boost(Skills.DEFENCE, 5, 15),
            Boost(Skills.RANGED, 5, 15),
            Boost(Skills.MAGIC, 5, 15),
            Restore(8, 25) { it != Skills.HITPOINTS && it != Skills.PRAYER },
            RestorePrayer(flat = 8, percent = 25, boostedPercent = 27),
        ),
    ),

    /**
     * Castle Wars' brew, which the wiki describes as a super combat, a ranging potion, a stamina
     * potion, a super restore and an imbued heart at once.
     */
    CASTLEWARS_BREW(
        doses("castlewars_brew"),
        listOf(
            Boost(Skills.ATTACK, 5, 15),
            Boost(Skills.STRENGTH, 5, 15),
            Boost(Skills.DEFENCE, 5, 15),
            Boost(Skills.RANGED, 4, 10),
            Boost(Skills.MAGIC, 1, 10),
            RestorePrayer(flat = 8, percent = 25, boostedPercent = 27),
            RestoreEnergy(20),
            Stamina(ticks = 200),
        ),
    ),

    /**
     * Chambers of Xeric supplies. Each comes in a weak, standard and strong tier; the boost tiers
     * run 4 + 10%, 5 + 13% and 6 + 16% across the whole family, which the Overload and Elder tables
     * both spell out.
     *
     * The revitalisation tiers below the strong one, and the prayer enhance tiers below it, are the
     * only figures here the wiki does not publish - they are interpolated on the same curve.
     */
    ELDER_MINUS(underscored("elder"), coxBoost(4, 10, Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE)),
    ELDER(underscored("elder_potion"), coxBoost(5, 13, Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE)),
    ELDER_PLUS(
        underscoredSuffixed("elder", 20921, 20922, 20923, 20924),
        coxBoost(6, 16, Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE),
    ),
    TWISTED_MINUS(underscored("twisted"), coxBoost(4, 10, Skills.RANGED, Skills.DEFENCE)),
    TWISTED(underscored("twisted_potion"), coxBoost(5, 13, Skills.RANGED, Skills.DEFENCE)),
    TWISTED_PLUS(
        underscoredSuffixed("twisted", 20933, 20934, 20935, 20936),
        coxBoost(6, 16, Skills.RANGED, Skills.DEFENCE),
    ),
    KODAI_MINUS(underscored("kodai"), coxBoost(4, 10, Skills.MAGIC)),
    KODAI(underscored("kodai_potion"), coxBoost(5, 13, Skills.MAGIC)),
    KODAI_PLUS(underscoredSuffixed("kodai", 20945, 20946, 20947, 20948), coxBoost(6, 16, Skills.MAGIC)),

    REVITALISATION_MINUS(underscored("revitalisation"), revitalisation(7, 20)),
    REVITALISATION(underscored("revitalisation_potion"), revitalisation(9, 25)),
    REVITALISATION_PLUS(
        underscoredSuffixed("revitalisation", 20957, 20958, 20959, 20960),
        revitalisation(11, 30),
    ),

    PRAYER_ENHANCE_MINUS(underscored("prayer_enhance"), listOf(PrayerRegen(458, 21, 50))),
    PRAYER_ENHANCE(
        underscoredSuffixed("prayer_enhance", 20965, 20966, 20967, 20968),
        listOf(PrayerRegen(458, 26, 50)),
    ),
    PRAYER_ENHANCE_PLUS(
        underscoredSuffixed("prayer_enhance", 20969, 20970, 20971, 20972),
        listOf(PrayerRegen(483, 31, 50)),
    ),

    XERICS_AID_MINUS(underscored("xerics_aid"), xericsAid(1, 7, 1, 14, 1, 7)),
    XERICS_AID(underscoredSuffixed("xerics_aid", 20977, 20978, 20979, 20980), xericsAid(2, 12, 2, 18, 2, 9)),
    XERICS_AID_PLUS(underscoredSuffixed("xerics_aid", 20981, 20982, 20983, 20984), xericsAid(5, 15, 5, 20, 4, 10)),

    COX_ANTIPOISON_MINUS(underscored("antipoison"), listOf(CurePoison(seconds = 90))),
    COX_ANTIPOISON(underscored("antipoison_potion"), listOf(CurePoison(seconds = 360))),
    COX_ANTIPOISON_PLUS(
        underscoredSuffixed("antipoison", 25762, 25763, 25764, 25765),
        listOf(CurePoison(seconds = 540)),
    ),

    /**
     * The overloads. All of them charge 50 hitpoints for the boost, hold every combat stat flat for
     * five minutes and hand the 50 back when the effect ends, so none can be drunk below 51.
     */
    COX_OVERLOAD_MINUS(underscoredSuffixed("overload", 20985, 20986, 20987, 20988), overload(4, 10), minHitpoints = OVERLOAD_MIN_HITPOINTS),
    COX_OVERLOAD(underscoredSuffixed("overload", 20989, 20990, 20991, 20992), overload(5, 13), minHitpoints = OVERLOAD_MIN_HITPOINTS),
    COX_OVERLOAD_PLUS(underscoredSuffixed("overload", 20993, 20994, 20995, 20996), overload(6, 16), minHitpoints = OVERLOAD_MIN_HITPOINTS),
    NMZ_OVERLOAD(underscored("overload"), overload(5, 15), minHitpoints = OVERLOAD_MIN_HITPOINTS),
    BLIGHTED_OVERLOAD(underscored("blighted_overload"), overload(5, 15), minHitpoints = OVERLOAD_MIN_HITPOINTS),

    /**
     * Tombs of Amascut supplies.
     *
     * Tears of Elidinis also restores prayer for anyone standing next to the drinker, which there is
     * no party mechanic to carry.
     */
    NECTAR(
        underscored("nectar"),
        listOf(
            Boost(Skills.HITPOINTS, 3, 15),
            Drain(Skills.ATTACK, 5, 5),
            Drain(Skills.STRENGTH, 5, 5),
            Drain(Skills.DEFENCE, 5, 5),
            Drain(Skills.RANGED, 5, 5),
            Drain(Skills.MAGIC, 5, 5),
        ),
    ),
    TEARS_OF_ELIDINIS(
        underscored("tears_of_elidinis"),
        listOf(
            Restore(3, 25) { it in COMBAT_STATS },
            RestorePrayer(flat = 10, percent = 25),
        ),
    ),
    AMBROSIA(
        underscored("ambrosia", 2),
        listOf(
            Boost(Skills.HITPOINTS, 2, 25),
            Boost(Skills.PRAYER, 5, 20),
            RestoreEnergy(100),
            CurePoison(seconds = 720),
        ),
    ),

    /**
     * Blighted supplies, and the Last Man Standing and Deadman copies of the ordinary potions. The
     * cache gives these their own item ids, so without their own entries they are inert items that
     * look exactly like the real thing.
     */
    BLIGHTED_SUPER_RESTORE(doses("blighted_super_restore"), SUPER_RESTORE_EFFECTS),
    LMS_PRAYER(suffixed("prayer_potion", 20396, 20395, 20394, 20393), PRAYER_EFFECTS),
    LMS_SUPER_ENERGY(suffixed("super_energy", 20551, 20550, 20549, 20548), SUPER_ENERGY_EFFECTS),
    LMS_SUPER_COMBAT(suffixed("super_combat_potion", 23549, 23547, 23545, 23543), SUPER_COMBAT_EFFECTS),
    LMS_RANGING(suffixed("ranging_potion", 23557, 23555, 23553, 23551), RANGING_EFFECTS),
    LMS_SANFEW_SERUM(suffixed("sanfew_serum", 23565, 23563, 23561, 23559), SANFEW_EFFECTS),
    LMS_SUPER_RESTORE(suffixed("super_restore", 23573, 23571, 23569, 23567), SUPER_RESTORE_EFFECTS),
    LMS_SARADOMIN_BREW(suffixed("saradomin_brew", 23581, 23579, 23577, 23575), SARADOMIN_EFFECTS),
    LMS_STAMINA(suffixed("stamina_potion", 23589, 23587, 23585, 23583), STAMINA_EFFECTS),
    LMS_COMBAT(suffixed("combat_potion", 26153, 26152, 26151, 26150), COMBAT_EFFECTS),
    ;

    companion object {
        val values = enumValues<Potion>()
    }
}

/**
 * The stats a restore potion brings back: everything used in combat bar hitpoints, which only food
 * and brews heal, and prayer, which needs a prayer potion.
 */
private val COMBAT_STATS =
    setOf(Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE, Skills.RANGED, Skills.MAGIC)

/**
 * The hitpoints an overload charges, and so the level below which one cannot be drunk.
 */
private const val OVERLOAD_HITPOINT_COST = 50
private const val OVERLOAD_MIN_HITPOINTS = OVERLOAD_HITPOINT_COST + 1

/*
 * Effect lists that more than one potion shares. A barbarian mix is its base potion plus healing, a
 * blighted or Last Man Standing potion is the same potion under a different item id, and a divine
 * potion mirrors the one it is named after - so the numbers live in one place and cannot drift.
 */
private val ATTACK_EFFECTS = listOf(Boost(Skills.ATTACK, 3, 10))
private val STRENGTH_EFFECTS = listOf(Boost(Skills.STRENGTH, 3, 10))
private val DEFENCE_EFFECTS = listOf(Boost(Skills.DEFENCE, 3, 10))
private val SUPER_ATTACK_EFFECTS = listOf(Boost(Skills.ATTACK, 5, 15))
private val SUPER_STRENGTH_EFFECTS = listOf(Boost(Skills.STRENGTH, 5, 15))
private val SUPER_DEFENCE_EFFECTS = listOf(Boost(Skills.DEFENCE, 5, 15))
private val COMBAT_EFFECTS = listOf(Boost(Skills.ATTACK, 3, 10), Boost(Skills.STRENGTH, 3, 10))
private val SUPER_COMBAT_EFFECTS =
    listOf(Boost(Skills.ATTACK, 5, 15), Boost(Skills.STRENGTH, 5, 15), Boost(Skills.DEFENCE, 5, 15))
private val RANGING_EFFECTS = listOf(Boost(Skills.RANGED, 4, 10))
private val MAGIC_EFFECTS = listOf(Boost(Skills.MAGIC, 4, 0))
private val AGILITY_EFFECTS = listOf(Boost(Skills.AGILITY, 3, 0))
private val FISHING_EFFECTS = listOf(Boost(Skills.FISHING, 3, 0))
private val HUNTER_EFFECTS = listOf(Boost(Skills.HUNTER, 3, 0))
private val MAGIC_ESSENCE_EFFECTS = listOf(Boost(Skills.MAGIC, 3, 0))

private val BASTION_EFFECTS = listOf(Boost(Skills.RANGED, 4, 10), Boost(Skills.DEFENCE, 5, 15))
private val BATTLEMAGE_EFFECTS = listOf(Boost(Skills.MAGIC, 4, 0), Boost(Skills.DEFENCE, 5, 15))

private val ZAMORAK_EFFECTS =
    listOf(
        Boost(Skills.ATTACK, 2, 20),
        Boost(Skills.STRENGTH, 2, 12),
        RestorePrayer(flat = 0, percent = 10),
        Drain(Skills.DEFENCE, 2, 10),
        Drain(Skills.HITPOINTS, 0, 12, floor = 1),
    )
private val SARADOMIN_EFFECTS =
    listOf(
        Boost(Skills.HITPOINTS, 2, 15),
        Boost(Skills.DEFENCE, 2, 20),
        Drain(Skills.ATTACK, 2, 10),
        Drain(Skills.STRENGTH, 2, 10),
        Drain(Skills.MAGIC, 2, 10),
        Drain(Skills.RANGED, 2, 10),
    )

/**
 * The two magic brews. Both raise Magic and restore prayer at the cost of the three melee stats;
 * the forgotten brew simply boosts harder.
 */
private val ANCIENT_BREW_EFFECTS = magicBrew(magicFlat = 2, magicPercent = 5)
private val FORGOTTEN_BREW_EFFECTS = magicBrew(magicFlat = 3, magicPercent = 8)

private val PRAYER_EFFECTS = listOf(RestorePrayer(flat = 7, percent = 25, boostedPercent = 27))
private val RESTORE_EFFECTS = listOf(Restore(10, 30) { it in COMBAT_STATS })
private val SUPER_RESTORE_EFFECTS =
    listOf(
        RestorePrayer(flat = 8, percent = 25, boostedPercent = 27),
        Restore(8, 25) { it != Skills.HITPOINTS && it != Skills.PRAYER },
    )
private val SANFEW_EFFECTS =
    listOf(
        RestorePrayer(flat = 4, percent = 30, boostedPercent = 32),
        Restore(4, 30) { it != Skills.HITPOINTS && it != Skills.PRAYER },
        CurePoison(seconds = 360),
    )

private val ENERGY_EFFECTS = listOf(RestoreEnergy(15))
private val SUPER_ENERGY_EFFECTS = listOf(RestoreEnergy(20))
private val STAMINA_EFFECTS = listOf(RestoreEnergy(20), Stamina(ticks = 200))

private val ANTIPOISON_EFFECTS = listOf(CurePoison(seconds = 90))
private val SUPERANTIPOISON_EFFECTS = listOf(CurePoison(seconds = 360))
private val ANTIDOTE_PLUS_EFFECTS = listOf(CurePoison(seconds = 540))
private val ANTIDOTE_PLUS_PLUS_EFFECTS = listOf(CurePoison(seconds = 720))
private val ANTIVENOM_EFFECTS = listOf(CurePoison(seconds = 720))
private val RELICYM_EFFECTS = listOf(CureDisease)

private val ANTIFIRE_EFFECTS = listOf(Antifire(ticks = 600, superAntifire = false))
private val EXTENDED_ANTIFIRE_EFFECTS = listOf(Antifire(ticks = 1200, superAntifire = false))
private val SUPER_ANTIFIRE_EFFECTS = listOf(Antifire(ticks = 300, superAntifire = true))
private val EXTENDED_SUPER_ANTIFIRE_EFFECTS = listOf(Antifire(ticks = 600, superAntifire = true))

/**
 * A Magic boost, a prayer restore and the matching melee drain - the shape both ancient and
 * forgotten brews take.
 */
private fun magicBrew(
    magicFlat: Int,
    magicPercent: Int,
): List<ConsumableEffect> =
    listOf(
        Boost(Skills.MAGIC, magicFlat, magicPercent),
        RestorePrayer(flat = 2, percent = 10),
        Drain(Skills.ATTACK, 2, 10),
        Drain(Skills.STRENGTH, 2, 10),
        Drain(Skills.DEFENCE, 2, 10),
    )

/**
 * A divine dose: the boosts it holds, plus the ten hitpoints it charges for them.
 */
private fun divine(vararg boosts: DivineBoost): List<ConsumableEffect> =
    boosts.toList() + Damage(Divine.HITPOINT_COST)

/**
 * A Chambers of Xeric combat boost, which behaves like a divine potion - held flat for five minutes
 * rather than decaying - but costs nothing to drink.
 */
private fun coxBoost(
    flat: Int,
    percent: Int,
    vararg skills: Int,
): List<ConsumableEffect> = skills.map { DivineBoost(it, flat, percent) }

/**
 * An overload: every combat stat held flat for five minutes, bought with 50 hitpoints that are
 * handed back when it ends.
 */
private fun overload(
    flat: Int,
    percent: Int,
): List<ConsumableEffect> =
    COMBAT_STATS.map { DivineBoost(it, flat, percent) } +
        listOf(Damage(OVERLOAD_HITPOINT_COST), HealOnExpiry(OVERLOAD_HITPOINT_COST))

/**
 * A revitalisation dose: drained combat stats and prayer both brought back by the same amount.
 */
private fun revitalisation(
    flat: Int,
    percent: Int,
): List<ConsumableEffect> =
    listOf(
        Restore(flat, percent) { it in COMBAT_STATS },
        RestorePrayer(flat = flat, percent = percent),
    )

/**
 * Xeric's aid, the raid's Saradomin brew: hitpoints and Defence up - both allowed over the base
 * level - at the cost of the four attacking stats.
 */
private fun xericsAid(
    healFlat: Int,
    healPercent: Int,
    defenceFlat: Int,
    defencePercent: Int,
    drainFlat: Int,
    drainPercent: Int,
): List<ConsumableEffect> =
    listOf(
        Boost(Skills.HITPOINTS, healFlat, healPercent),
        Boost(Skills.DEFENCE, defenceFlat, defencePercent),
        Drain(Skills.ATTACK, drainFlat, drainPercent),
        Drain(Skills.STRENGTH, drainFlat, drainPercent),
        Drain(Skills.RANGED, drainFlat, drainPercent),
        Drain(Skills.MAGIC, drainFlat, drainPercent),
    )

/**
 * The four doses of a potion whose item names are the family name followed by the dose count.
 */
private fun doses(family: String): List<String> = (1..4).map { "item.$family$it" }

/**
 * The two doses of a barbarian mix, which follow the same naming.
 */
private fun mix(family: String): List<String> = (1..2).map { "item.$family$it" }

/**
 * Doses of a potion whose item names separate the dose count with an underscore - the raid supplies
 * and everything else added since.
 */
private fun underscored(
    family: String,
    count: Int = 4,
): List<String> = (1..count).map { "item.${family}_$it" }

/**
 * Doses of a potion whose generated item names collide with another potion's, and so carry the item
 * id as a suffix. Listed lowest dose first, the order [Potion.doses] is read in.
 *
 * The dose count runs straight on to the family name, the way the older potions are named.
 */
private fun suffixed(
    family: String,
    vararg ids: Int,
): List<String> = ids.mapIndexed { index, id -> "item.$family${index + 1}_$id" }

/**
 * [suffixed] for the raid supplies, whose names separate the dose count with an underscore.
 */
private fun underscoredSuffixed(
    family: String,
    vararg ids: Int,
): List<String> = ids.mapIndexed { index, id -> "item.${family}_${index + 1}_$id" }
