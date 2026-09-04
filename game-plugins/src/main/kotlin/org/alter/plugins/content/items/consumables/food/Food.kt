package org.alter.plugins.content.items.consumables.food

import org.alter.api.Skills
import org.alter.plugins.content.items.consumables.Boost
import org.alter.plugins.content.items.consumables.ConsumableEffect
import org.alter.plugins.content.items.consumables.DelayedHeal
import org.alter.plugins.content.items.consumables.HealRange
import org.alter.plugins.content.items.consumables.Drain
import org.alter.plugins.content.items.consumables.DamagePercent
import org.alter.plugins.content.items.consumables.HealPercent
import org.alter.plugins.content.items.consumables.OneOf
import org.alter.plugins.content.items.consumables.Outcome
import org.alter.plugins.content.items.consumables.RestoreEnergy
import org.alter.plugins.content.items.consumables.RestorePrayer

/**
 * What every pie is served in, and so what its second half leaves behind.
 */
private const val PIE_DISH = "item.pie_dish"

/**
 * What every ale and beer is served in, and so what drinking one hands back.
 */
private const val BEER_GLASS = "item.beer_glass"

/**
 * Brandy, gin, vodka and whisky share one effect: five hitpoints, a Strength boost of
 * "5% + 1" and an Attack drain of "2% + 3".
 */
private val SPIRIT_EFFECTS = listOf(Boost(Skills.STRENGTH, 1, 5), Drain(Skills.ATTACK, 3, 2))

/**
 * Everything a player can eat or drink.
 *
 * Food that comes in more than one portion is listed a portion at a time, each [replacement] naming
 * what the previous bite leaves in the inventory: the next portion down, then the dish, bowl, glass
 * or keg it was served in - or nothing at all, for food that leaves no container.
 *
 * Drink lives here rather than in [Potion][org.alter.plugins.content.items.consumables.potions.Potion]
 * because it heals: an ale, a tea and a cocktail are a piece of food that happens to carry a skill
 * modifier, while a potion is a modifier that happens to come in four doses.
 *
 * Coverage is checked against the cache, not against memory - every item whose inventory options
 * carry "Drink" or "Eat" was dumped from the item definitions and matched off against this list and
 * [Potion][org.alter.plugins.content.items.consumables.potions.Potion]. What is left over is:
 *
 * - **Cocktail-making intermediates** - the unfinished and odd cocktails. They are steps in a
 *   recipe, and the wiki publishes no effect for drinking one.
 * - **The tea flask**, which holds five cups as charges rather than as a dose chain, and would need
 *   a charge model that refilling at a tea urn also has to share.
 * - **Quest and event items** whose "effect" is a piece of quest script rather than a modifier:
 *   the cadava and bravery potions, blamish oil, shrink-me-quick, the goblin, inversion, sulphur,
 *   shielding, strange and magical cleaning potions, and the strangler serum.
 * - **Items that need a system that does not exist**: Nightmare Zone absorption potions (absorption
 *   points), liquid adrenaline (special attack cost) and rejuvenation potions (Wintertodt warmth).
 */
enum class Food(
    val item: String,
    val heal: Int = 0,
    val overheal: Boolean = false,
    val replacement: String? = null,
    val tickDelay: Int = 3,
    val comboFood: Boolean = false,
    val effects: List<ConsumableEffect> = emptyList(),
    /**
     * The inventory option that consumes it. Most of this list is eaten; everything from the ales
     * down is drunk.
     */
    val option: String = "eat",
    /**
     * What the player says instead of drinking it, for the handful of items that carry a Drink
     * option the live game refuses to honour - a quest delivery, an ingredient, a joke item. Set,
     * nothing is consumed and no effect is applied.
     */
    val refusal: String? = null,
) {
    /**
     * Sea food.
     */
    SHRIMP(item = "item.shrimps", heal = 3),
    ANCHOVIES(item = "item.anchovies", heal = 1),
    SARDINE(item = "item.sardine", heal = 4),
    HERRING(item = "item.herring", heal = 5),
    MACKEREL(item = "item.mackerel", heal = 6),
    TROUT(item = "item.trout", heal = 7),
    COD(item = "item.cod", heal = 7),
    PIKE(item = "item.pike", heal = 8),
    SALMON(item = "item.salmon", heal = 9),
    TUNA(item = "item.tuna", heal = 10),
    RAINBOW(item = "item.rainbow_fish", heal = 11),
    CAVEEEL(item = "item.cave_eel", heal = 9),
    LOBSTER(item = "item.lobster", heal = 12),
    BASS(item = "item.bass", heal = 13),
    SWORDFISH(item = "item.swordfish", heal = 14),
    MONKFISH(item = "item.monkfish", heal = 16),
    KARAMBWAN(item = "item.cooked_karambwan", heal = 18, comboFood = true),
    SHARK(item = "item.shark", heal = 20),
    SEATURTLE(item = "item.sea_turtle", heal = 21),
    MANTA_RAY(item = "item.manta_ray", heal = 22),
    DARK_CRAB(item = "item.dark_crab", heal = 22),
    ANGLERFISH(item = "item.anglerfish", overheal = true),

    /**
     * Meat.
     */
    CHICKEN(item = "item.cooked_chicken", heal = 3),
    MEAT(item = "item.cooked_meat", heal = 3),
    RABBIT(item = "item.cooked_rabbit", heal = 5),
    UGTHANKI_MEAT(item = "item.ugthanki_meat", heal = 3),

    /**
     * Spit-roasted over a fire. The iron spit comes back at the range, not in the mouth,
     * so none of these leave anything behind when eaten.
     */
    ROASTBIRDMEAT(item = "item.roast_bird_meat", heal = 6),
    ROASTRABBIT(item = "item.roast_rabbit", heal = 7),
    ROASTBEASTMEAT(item = "item.roast_beast_meat", heal = 8),
    COOKED_CHOMPY(item = "item.cooked_chompy", heal = 10),

    /**
     * The kebab is item 1885, not 1883. 1883 is the botched one the wiki calls "ugthanki
     * kebab (bad)" - its examine reads "A strange smelling kebab", against 1885's "A fresh
     * kebab" - and this entry used to point at it, which meant the failed kebab healed 19
     * and the real one did nothing at all.
     */
    KEBAB(item = "item.ugthanki_kebab_1885", heal = 19),

    /**
     * Pastries.
     */
    BREAD(item = "item.bread", heal = 5),

    CAKE(item = "item.cake", heal = 4, replacement = "item._23_cake"),
    TWO_THIRDS_CAKE(item = "item._23_cake", heal = 4, replacement = "item.slice_of_cake"),
    SLICE_OF_CAKE(item = "item.slice_of_cake", heal = 4),

    CHOCOLATE_CAKE(item = "item.chocolate_cake", heal = 5, replacement = "item._23_chocolate_cake"),
    TWO_THIRDS_CHOCOLATE_CAKE(item = "item._23_chocolate_cake", heal = 5, replacement = "item.chocolate_slice"),
    CHOCOLATE_SLICE(item = "item.chocolate_slice", heal = 5),

    /**
     * Pies. Both halves heal the same and carry the same boost; the second leaves the dish behind.
     */
    REDBERRY_PIE(item = "item.redberry_pie", heal = 5, replacement = "item.half_a_redberry_pie"),
    HALF_A_REDBERRY_PIE(item = "item.half_a_redberry_pie", heal = 5, replacement = PIE_DISH),

    MEAT_PIE(item = "item.meat_pie", heal = 6, replacement = "item.half_a_meat_pie"),
    HALF_A_MEAT_PIE(item = "item.half_a_meat_pie", heal = 6, replacement = PIE_DISH),

    APPLE_PIE(item = "item.apple_pie", heal = 7, replacement = "item.half_an_apple_pie"),
    HALF_AN_APPLE_PIE(item = "item.half_an_apple_pie", heal = 7, replacement = PIE_DISH),

    GARDEN_PIE(
        item = "item.garden_pie",
        heal = 6,
        replacement = "item.half_a_garden_pie",
        effects = listOf(Boost(Skills.FARMING, 3, 0)),
    ),
    HALF_A_GARDEN_PIE(
        item = "item.half_a_garden_pie",
        heal = 6,
        replacement = PIE_DISH,
        effects = listOf(Boost(Skills.FARMING, 3, 0)),
    ),

    FISH_PIE(
        item = "item.fish_pie",
        heal = 6,
        replacement = "item.half_a_fish_pie",
        effects = listOf(Boost(Skills.FISHING, 3, 0)),
    ),
    HALF_A_FISH_PIE(
        item = "item.half_a_fish_pie",
        heal = 6,
        replacement = PIE_DISH,
        effects = listOf(Boost(Skills.FISHING, 3, 0)),
    ),

    BOTANICAL_PIE(
        item = "item.botanical_pie",
        heal = 7,
        replacement = "item.half_a_botanical_pie",
        effects = listOf(Boost(Skills.HERBLORE, 4, 0)),
    ),
    HALF_A_BOTANICAL_PIE(
        item = "item.half_a_botanical_pie",
        heal = 7,
        replacement = PIE_DISH,
        effects = listOf(Boost(Skills.HERBLORE, 4, 0)),
    ),

    MUSHROOM_PIE(
        item = "item.mushroom_pie",
        heal = 8,
        replacement = "item.half_a_mushroom_pie",
        effects = listOf(Boost(Skills.CRAFTING, 4, 0)),
    ),
    HALF_A_MUSHROOM_PIE(
        item = "item.half_a_mushroom_pie",
        heal = 8,
        replacement = PIE_DISH,
        effects = listOf(Boost(Skills.CRAFTING, 4, 0)),
    ),

    ADMIRAL_PIE(
        item = "item.admiral_pie",
        heal = 8,
        replacement = "item.half_an_admiral_pie",
        effects = listOf(Boost(Skills.FISHING, 5, 0)),
    ),
    HALF_AN_ADMIRAL_PIE(
        item = "item.half_an_admiral_pie",
        heal = 8,
        replacement = PIE_DISH,
        effects = listOf(Boost(Skills.FISHING, 5, 0)),
    ),

    DRAGONFRUIT_PIE(
        item = "item.dragonfruit_pie",
        heal = 10,
        replacement = "item.half_a_dragonfruit_pie",
        effects = listOf(Boost(Skills.FLETCHING, 4, 0)),
    ),
    HALF_A_DRAGONFRUIT_PIE(
        item = "item.half_a_dragonfruit_pie",
        heal = 10,
        replacement = PIE_DISH,
        effects = listOf(Boost(Skills.FLETCHING, 4, 0)),
    ),

    WILD_PIE(
        item = "item.wild_pie",
        heal = 11,
        replacement = "item.half_a_wild_pie",
        effects = listOf(Boost(Skills.RANGED, 4, 0), Boost(Skills.SLAYER, 5, 0)),
    ),
    HALF_A_WILD_PIE(
        item = "item.half_a_wild_pie",
        heal = 11,
        replacement = PIE_DISH,
        effects = listOf(Boost(Skills.RANGED, 4, 0), Boost(Skills.SLAYER, 5, 0)),
    ),

    SUMMER_PIE(
        item = "item.summer_pie",
        heal = 11,
        replacement = "item.half_a_summer_pie",
        effects = listOf(Boost(Skills.AGILITY, 5, 0), RestoreEnergy(10)),
    ),
    HALF_A_SUMMER_PIE(
        item = "item.half_a_summer_pie",
        heal = 11,
        replacement = PIE_DISH,
        effects = listOf(Boost(Skills.AGILITY, 5, 0), RestoreEnergy(10)),
    ),

    /**
     * Pizzas. Two halves, and the second leaves nothing behind.
     */
    PLAIN_PIZZA(item = "item.plain_pizza", heal = 7, replacement = "item._12_plain_pizza"),
    HALF_A_PLAIN_PIZZA(item = "item._12_plain_pizza", heal = 7),

    MEAT_PIZZA(item = "item.meat_pizza", heal = 8, replacement = "item._12_meat_pizza"),
    HALF_A_MEAT_PIZZA(item = "item._12_meat_pizza", heal = 8),

    ANCHOVY_PIZZA(item = "item.anchovy_pizza", heal = 9, replacement = "item._12_anchovy_pizza"),
    HALF_AN_ANCHOVY_PIZZA(item = "item._12_anchovy_pizza", heal = 9),

    PINEAPPLE_PIZZA(item = "item.pineapple_pizza", heal = 11, replacement = "item._12_pineapple_pizza"),
    HALF_A_PINEAPPLE_PIZZA(item = "item._12_pineapple_pizza", heal = 11),

    /**
     * Served in a bowl.
     */
    STEW(item = "item.stew", heal = 11, replacement = "item.bowl"),
    CURRY(item = "item.curry", heal = 19, replacement = "item.bowl"),

    /**
     * The bowls on the way to a topped potato. Every one of them is edible in its own
     * right, and every one leaves the bowl behind - they are all served in one.
     *
     * Cooked sweetcorn is the exception on both counts: it is a cob rather than a bowl, and
     * the wiki gives its healing as a scaling "1-10" rather than a flat number. The scaling
     * is not modelled here; it heals the top of that range.
     */
    SPICY_SAUCE(item = "item.spicy_sauce", heal = 2, replacement = "item.bowl"),
    CHILLI_CON_CARNE(item = "item.chilli_con_carne", heal = 5, replacement = "item.bowl"),
    SCRAMBLED_EGG(item = "item.scrambled_egg", heal = 5, replacement = "item.bowl"),
    EGG_AND_TOMATO(item = "item.egg_and_tomato", heal = 8, replacement = "item.bowl"),
    COOKED_SWEETCORN(item = "item.cooked_sweetcorn", heal = 10),
    FRIED_ONIONS(item = "item.fried_onions", heal = 5, replacement = "item.bowl"),
    FRIED_MUSHROOMS(item = "item.fried_mushrooms", heal = 5, replacement = "item.bowl"),
    MUSHROOM_AND_ONION(item = "item.mushroom__onion", heal = 11, replacement = "item.bowl"),
    TUNA_AND_CORN(item = "item.tuna_and_corn", heal = 13, replacement = "item.bowl"),

    /**
     * Baked potatoes. None of these leave anything behind.
     */
    BAKED_POTATO(item = "item.baked_potato", heal = 4),
    POTATO_WITH_BUTTER(item = "item.potato_with_butter", heal = 14),
    CHILLI_POTATO(item = "item.chilli_potato", heal = 14),
    POTATO_WITH_CHEESE(item = "item.potato_with_cheese", heal = 16),
    EGG_POTATO(item = "item.egg_potato", heal = 16),
    MUSHROOM_POTATO(item = "item.mushroom_potato", heal = 20),
    TUNA_POTATO(item = "item.tuna_potato", heal = 22),

    /**
     * Gnome cooking. Crunchies, battas and gnomebowls are all "fast food" - they share the
     * karambwan's shorter delay, so one can follow another piece of food.
     */
    TOAD_CRUNCHIES(item = "item.toad_crunchies", heal = 8, comboFood = true),
    SPICY_CRUNCHIES(item = "item.spicy_crunchies", heal = 7, comboFood = true),
    WORM_CRUNCHIES(item = "item.worm_crunchies", heal = 8, comboFood = true),
    CHOCCHIP_CRUNCHIES(item = "item.chocchip_crunchies", heal = 7, comboFood = true),

    FRUIT_BATTA(item = "item.fruit_batta", heal = 11, comboFood = true),
    TOAD_BATTA(item = "item.toad_batta", heal = 11, comboFood = true),
    WORM_BATTA(item = "item.worm_batta", heal = 11, comboFood = true),
    VEGETABLE_BATTA(item = "item.vegetable_batta", heal = 11, comboFood = true),
    CHEESETOM_BATTA(item = "item.cheesetom_batta", heal = 11, comboFood = true),

    WORM_HOLE(item = "item.worm_hole", heal = 12, comboFood = true),
    VEG_BALL(item = "item.veg_ball", heal = 12, comboFood = true),
    TANGLED_TOADS_LEGS(item = "item.tangled_toads_legs", heal = 15, comboFood = true),
    CHOCOLATE_BOMB(item = "item.chocolate_bomb", heal = 15, comboFood = true),

    /**
     * Other.
     */
    ONION(item = "item.onion", heal = 1),

    /**
     * The only food in the game that boosts Strength: two hitpoints, a prayer point, +2 Attack and
     * +1 Strength for a Defence level.
     */
    JANGERBERRIES(
        item = "item.jangerberries",
        heal = 2,
        effects =
            listOf(
                RestorePrayer(flat = 1, percent = 0),
                Boost(Skills.ATTACK, 2, 0),
                Boost(Skills.STRENGTH, 1, 0),
                Drain(Skills.DEFENCE, 1, 0),
            ),
    ),

    /**
     * Gnome cocktails. Drunk rather than eaten, like the wine below. The glass they are
     * served in is not handed back - the wiki does not say it is, and inventing an item
     * that appears from nowhere is worse than losing a two-coin glass.
     */
    FRUIT_BLAST(item = "item.fruit_blast", heal = 9, replacement = COCKTAIL_GLASS, option = "drink"),
    PINEAPPLE_PUNCH(item = "item.pineapple_punch", heal = 9, replacement = COCKTAIL_GLASS, option = "drink"),
    WIZARD_BLIZZARD(
        item = "item.wizard_blizzard",
        heal = 5,
        replacement = COCKTAIL_GLASS,
        effects = WIZARD_BLIZZARD_EFFECTS,
        option = "drink",
    ),
    SHORT_GREEN_GUY(
        item = "item.short_green_guy",
        heal = 5,
        replacement = COCKTAIL_GLASS,
        effects = SHORT_GREEN_GUY_EFFECTS,
        option = "drink",
    ),
    DRUNK_DRAGON(
        item = "item.drunk_dragon",
        heal = 5,
        replacement = COCKTAIL_GLASS,
        effects = STRONG_COCKTAIL_EFFECTS,
        option = "drink",
    ),
    CHOC_SATURDAY(
        item = "item.choc_saturday",
        heal = 5,
        replacement = COCKTAIL_GLASS,
        effects = STRONG_COCKTAIL_EFFECTS,
        option = "drink",
    ),
    BLURBERRY_SPECIAL(
        item = "item.blurberry_special",
        heal = 7,
        replacement = COCKTAIL_GLASS,
        effects = STRONG_COCKTAIL_EFFECTS,
        option = "drink",
    ),

    /**
     * Alcohol.
     *
     * Every one of these trades a combat stat for something else, and the Strength-boosting
     * half of the family - beer, the two bitters, grog, the tankard, the keg and the four
     * spirits - is the only free-to-play Strength boost in the game.
     *
     * Boosts and drains are written the way the wiki's infoboxes give them, as "N% + M":
     * [Boost] takes the percentage off the base level, [Drain] off the current level, which
     * is exactly the pair of formulas the wiki quotes (a boost is floor(Level x N/100) + M,
     * a drain is floor(CurrentLevel x N/100) + M). So beer's "Strength +2% +1" is
     * `Boost(STRENGTH, 1, 2)` and its "Attack -6% -1" is `Drain(ATTACK, 1, 6)`.
     *
     * Drinking any of the glass-served ones hands back a beer glass; the keg, the tankard
     * and the spirits leave nothing behind.
     *
     * The mature (m) variants brewed in Keldagrim, and the four-pint kegs both come in, are listed
     * further down. Brewing itself does not exist yet, so nothing in game can produce them - but
     * they are real items with published numbers, and an item that cannot be drunk is a worse
     * answer than one that cannot yet be made.
     */
    BEER(
        item = "item.beer",
        heal = 1,
        replacement = BEER_GLASS,
        effects = BEER_EFFECTS,
        option = "drink",
    ),
    ASGARNIAN_ALE(
        item = "item.asgarnian_ale",
        heal = 1,
        replacement = BEER_GLASS,
        effects = ASGARNIAN_ALE_EFFECTS,
        option = "drink",
    ),
    DRAGON_BITTER(
        item = "item.dragon_bitter",
        heal = 1,
        replacement = BEER_GLASS,
        effects = DRAGON_BITTER_EFFECTS,
        option = "drink",
    ),
    GROG(
        item = "item.grog",
        heal = 3,
        replacement = BEER_GLASS,
        effects = listOf(Boost(Skills.STRENGTH, 1, 4), Drain(Skills.ATTACK, 3, 5)),
        option = "drink",
    ),

    /**
     * The skilling ales. Each buys a level or two in one skill with combat levels.
     */
    DWARVEN_STOUT(
        item = "item.dwarven_stout",
        heal = 1,
        replacement = BEER_GLASS,
        effects = DWARVEN_STOUT_EFFECTS,
        option = "drink",
    ),
    GREENMANS_ALE(
        item = "item.greenmans_ale",
        heal = 1,
        replacement = BEER_GLASS,
        effects = GREENMANS_ALE_EFFECTS,
        option = "drink",
    ),
    WIZARDS_MIND_BOMB(
        item = "item.wizards_mind_bomb",
        heal = 1,
        replacement = BEER_GLASS,
        effects = MIND_BOMB_EFFECTS,
        option = "drink",
    ),
    CHEFS_DELIGHT(
        item = "item.chefs_delight",
        heal = 1,
        replacement = BEER_GLASS,
        effects = CHEFS_DELIGHT_EFFECTS,
        option = "drink",
    ),
    SLAYERS_RESPITE(
        item = "item.slayers_respite",
        heal = 1,
        replacement = BEER_GLASS,
        effects = SLAYERS_RESPITE_EFFECTS,
        option = "drink",
    ),
    AXEMANS_FOLLY(
        item = "item.axemans_folly",
        heal = 1,
        replacement = BEER_GLASS,
        effects = AXEMANS_FOLLY_EFFECTS,
        option = "drink",
    ),
    CIDER(
        item = "item.cider",
        heal = 1,
        replacement = BEER_GLASS,
        effects = CIDER_EFFECTS,
        option = "drink",
    ),

    /**
     * The odd ones out: the brew that boosts Attack rather than draining it, and the mead
     * that heals better than any other ale and does nothing else at all.
     */
    BANDITS_BREW(
        item = "item.bandits_brew",
        heal = 1,
        replacement = BEER_GLASS,
        effects =
            listOf(
                Boost(Skills.THIEVING, 1, 0),
                Boost(Skills.ATTACK, 1, 0),
                Drain(Skills.STRENGTH, 3, 6),
                Drain(Skills.DEFENCE, 3, 6),
            ),
        option = "drink",
    ),
    MOONLIGHT_MEAD(item = "item.moonlight_mead", heal = 4, replacement = BEER_GLASS, option = "drink"),

    /**
     * Served in something the player does not get back. The keg's Attack drain is half the
     * current level on top of a flat five, which empties most accounts' Attack outright -
     * [Drain] already floors at zero, which is what the wiki says happens.
     *
     * The keg is 3801, not the 3711 that plain `item.keg_of_beer` resolves to. Both are
     * named "Keg of beer" in the cache, but only 3801 carries a Drink option; 3711 is the
     * scenery-ish one that can only be dropped, and binding to it would have thrown at
     * construction and taken eating away from every other food in the game with it.
     */
    BEER_TANKARD(
        item = "item.beer_tankard",
        heal = 4,
        effects = listOf(Boost(Skills.STRENGTH, 2, 4), Drain(Skills.ATTACK, 2, 10)),
        option = "drink",
    ),
    KEG_OF_BEER(
        item = "item.keg_of_beer_3801",
        heal = 15,
        effects = listOf(Boost(Skills.STRENGTH, 2, 10), Drain(Skills.ATTACK, 5, 50)),
        option = "drink",
    ),

    /**
     * Spirits. All four behave identically - the wiki spells the same "Strength +5% +1,
     * Attack -2% -3, heals 5" out on the brandy, gin and whisky pages, and vodka is the
     * fourth of the same set.
     */
    BRANDY(item = "item.brandy", heal = 5, effects = SPIRIT_EFFECTS, option = "drink"),
    GIN(item = "item.gin", heal = 5, effects = SPIRIT_EFFECTS, option = "drink"),
    VODKA(item = "item.vodka", heal = 5, effects = SPIRIT_EFFECTS, option = "drink"),
    WHISKY(item = "item.whisky", heal = 5, effects = SPIRIT_EFFECTS, option = "drink"),

    /**
     * Wine heals well for how cheap it is, at the cost of a couple of Attack levels.
     */
    JUG_OF_WINE(
        item = "item.jug_of_wine",
        heal = 11,
        replacement = "item.jug",
        effects = listOf(Drain(Skills.ATTACK, 2, 0)),
        option = "drink",
    ),

    /**
     * Ale kegs. A calquat keg brewed in Keldagrim holds four pints, drunk a pint at a time; the
     * wiki puts it plainly - the keg "has the exact same effects as drinking a glass" - so every
     * dose carries the same numbers as the pint above, and the fourth hands the keg back.
     *
     * (What the last pint leaves is the one part the wiki does not state. The calquat keg is the
     * vessel brewing fills and reuses, so that is what comes back here.)
     */
    DWARVEN_STOUT_KEG_4(
        item = "item.dwarven_stout4",
        heal = 1,
        replacement = "item.dwarven_stout3",
        effects = DWARVEN_STOUT_EFFECTS,
        option = "drink",
    ),
    DWARVEN_STOUT_KEG_3(
        item = "item.dwarven_stout3",
        heal = 1,
        replacement = "item.dwarven_stout2",
        effects = DWARVEN_STOUT_EFFECTS,
        option = "drink",
    ),
    DWARVEN_STOUT_KEG_2(
        item = "item.dwarven_stout2",
        heal = 1,
        replacement = "item.dwarven_stout1",
        effects = DWARVEN_STOUT_EFFECTS,
        option = "drink",
    ),
    DWARVEN_STOUT_KEG_1(
        item = "item.dwarven_stout1",
        heal = 1,
        replacement = CALQUAT_KEG,
        effects = DWARVEN_STOUT_EFFECTS,
        option = "drink",
    ),
    ASGARNIAN_ALE_KEG_4(
        item = "item.asgarnian_ale4",
        heal = 1,
        replacement = "item.asgarnian_ale3",
        effects = ASGARNIAN_ALE_EFFECTS,
        option = "drink",
    ),
    ASGARNIAN_ALE_KEG_3(
        item = "item.asgarnian_ale3",
        heal = 1,
        replacement = "item.asgarnian_ale2",
        effects = ASGARNIAN_ALE_EFFECTS,
        option = "drink",
    ),
    ASGARNIAN_ALE_KEG_2(
        item = "item.asgarnian_ale2",
        heal = 1,
        replacement = "item.asgarnian_ale1",
        effects = ASGARNIAN_ALE_EFFECTS,
        option = "drink",
    ),
    ASGARNIAN_ALE_KEG_1(
        item = "item.asgarnian_ale1",
        heal = 1,
        replacement = CALQUAT_KEG,
        effects = ASGARNIAN_ALE_EFFECTS,
        option = "drink",
    ),
    GREENMANS_ALE_KEG_4(
        item = "item.greenmans_ale4",
        heal = 1,
        replacement = "item.greenmans_ale3",
        effects = GREENMANS_ALE_EFFECTS,
        option = "drink",
    ),
    GREENMANS_ALE_KEG_3(
        item = "item.greenmans_ale3",
        heal = 1,
        replacement = "item.greenmans_ale2",
        effects = GREENMANS_ALE_EFFECTS,
        option = "drink",
    ),
    GREENMANS_ALE_KEG_2(
        item = "item.greenmans_ale2",
        heal = 1,
        replacement = "item.greenmans_ale1",
        effects = GREENMANS_ALE_EFFECTS,
        option = "drink",
    ),
    GREENMANS_ALE_KEG_1(
        item = "item.greenmans_ale1",
        heal = 1,
        replacement = CALQUAT_KEG,
        effects = GREENMANS_ALE_EFFECTS,
        option = "drink",
    ),
    WIZARDS_MIND_BOMB_KEG_4(
        item = "item.mind_bomb4",
        heal = 1,
        replacement = "item.mind_bomb3",
        effects = MIND_BOMB_EFFECTS,
        option = "drink",
    ),
    WIZARDS_MIND_BOMB_KEG_3(
        item = "item.mind_bomb3",
        heal = 1,
        replacement = "item.mind_bomb2",
        effects = MIND_BOMB_EFFECTS,
        option = "drink",
    ),
    WIZARDS_MIND_BOMB_KEG_2(
        item = "item.mind_bomb2",
        heal = 1,
        replacement = "item.mind_bomb1",
        effects = MIND_BOMB_EFFECTS,
        option = "drink",
    ),
    WIZARDS_MIND_BOMB_KEG_1(
        item = "item.mind_bomb1",
        heal = 1,
        replacement = CALQUAT_KEG,
        effects = MIND_BOMB_EFFECTS,
        option = "drink",
    ),
    DRAGON_BITTER_KEG_4(
        item = "item.dragon_bitter4",
        heal = 1,
        replacement = "item.dragon_bitter3",
        effects = DRAGON_BITTER_EFFECTS,
        option = "drink",
    ),
    DRAGON_BITTER_KEG_3(
        item = "item.dragon_bitter3",
        heal = 1,
        replacement = "item.dragon_bitter2",
        effects = DRAGON_BITTER_EFFECTS,
        option = "drink",
    ),
    DRAGON_BITTER_KEG_2(
        item = "item.dragon_bitter2",
        heal = 1,
        replacement = "item.dragon_bitter1",
        effects = DRAGON_BITTER_EFFECTS,
        option = "drink",
    ),
    DRAGON_BITTER_KEG_1(
        item = "item.dragon_bitter1",
        heal = 1,
        replacement = CALQUAT_KEG,
        effects = DRAGON_BITTER_EFFECTS,
        option = "drink",
    ),
    MOONLIGHT_MEAD_KEG_4(
        item = "item.moonlight_mead4",
        heal = 4,
        replacement = "item.moonlight_mead3",
        option = "drink",
    ),
    MOONLIGHT_MEAD_KEG_3(
        item = "item.moonlight_mead3",
        heal = 4,
        replacement = "item.moonlight_mead2",
        option = "drink",
    ),
    MOONLIGHT_MEAD_KEG_2(
        item = "item.moonlight_mead2",
        heal = 4,
        replacement = "item.moonlight_mead1",
        option = "drink",
    ),
    MOONLIGHT_MEAD_KEG_1(item = "item.moonlight_mead1", heal = 4, replacement = CALQUAT_KEG, option = "drink"),
    AXEMANS_FOLLY_KEG_4(
        item = "item.axemans_folly4",
        heal = 1,
        replacement = "item.axemans_folly3",
        effects = AXEMANS_FOLLY_EFFECTS,
        option = "drink",
    ),
    AXEMANS_FOLLY_KEG_3(
        item = "item.axemans_folly3",
        heal = 1,
        replacement = "item.axemans_folly2",
        effects = AXEMANS_FOLLY_EFFECTS,
        option = "drink",
    ),
    AXEMANS_FOLLY_KEG_2(
        item = "item.axemans_folly2",
        heal = 1,
        replacement = "item.axemans_folly1",
        effects = AXEMANS_FOLLY_EFFECTS,
        option = "drink",
    ),
    AXEMANS_FOLLY_KEG_1(
        item = "item.axemans_folly1",
        heal = 1,
        replacement = CALQUAT_KEG,
        effects = AXEMANS_FOLLY_EFFECTS,
        option = "drink",
    ),
    CHEFS_DELIGHT_KEG_4(
        item = "item.chefs_delight4",
        heal = 1,
        replacement = "item.chefs_delight3",
        effects = CHEFS_DELIGHT_EFFECTS,
        option = "drink",
    ),
    CHEFS_DELIGHT_KEG_3(
        item = "item.chefs_delight3",
        heal = 1,
        replacement = "item.chefs_delight2",
        effects = CHEFS_DELIGHT_EFFECTS,
        option = "drink",
    ),
    CHEFS_DELIGHT_KEG_2(
        item = "item.chefs_delight2",
        heal = 1,
        replacement = "item.chefs_delight1",
        effects = CHEFS_DELIGHT_EFFECTS,
        option = "drink",
    ),
    CHEFS_DELIGHT_KEG_1(
        item = "item.chefs_delight1",
        heal = 1,
        replacement = CALQUAT_KEG,
        effects = CHEFS_DELIGHT_EFFECTS,
        option = "drink",
    ),
    SLAYERS_RESPITE_KEG_4(
        item = "item.slayers_respite4",
        heal = 1,
        replacement = "item.slayers_respite3",
        effects = SLAYERS_RESPITE_EFFECTS,
        option = "drink",
    ),
    SLAYERS_RESPITE_KEG_3(
        item = "item.slayers_respite3",
        heal = 1,
        replacement = "item.slayers_respite2",
        effects = SLAYERS_RESPITE_EFFECTS,
        option = "drink",
    ),
    SLAYERS_RESPITE_KEG_2(
        item = "item.slayers_respite2",
        heal = 1,
        replacement = "item.slayers_respite1",
        effects = SLAYERS_RESPITE_EFFECTS,
        option = "drink",
    ),
    SLAYERS_RESPITE_KEG_1(
        item = "item.slayers_respite1",
        heal = 1,
        replacement = CALQUAT_KEG,
        effects = SLAYERS_RESPITE_EFFECTS,
        option = "drink",
    ),
    CIDER_KEG_4(
        item = "item.cider4",
        heal = 1,
        replacement = "item.cider3",
        effects = CIDER_EFFECTS,
        option = "drink",
    ),
    CIDER_KEG_3(
        item = "item.cider3",
        heal = 1,
        replacement = "item.cider2",
        effects = CIDER_EFFECTS,
        option = "drink",
    ),
    CIDER_KEG_2(
        item = "item.cider2",
        heal = 1,
        replacement = "item.cider1",
        effects = CIDER_EFFECTS,
        option = "drink",
    ),
    CIDER_KEG_1(item = "item.cider1", heal = 1, replacement = CALQUAT_KEG, effects = CIDER_EFFECTS, option = "drink"),

    /**
     * The mature ales brewed in Keldagrim, in the pint and the keg. Each is a stronger version of
     * the ale it is named after and carries its own published numbers rather than a multiple of
     * them - the mature slayer's respite doubles its boost and halves its drain, while the mature
     * mind bomb raises both.
     */
    MATURE_DWARVEN_STOUT(
        item = "item.dwarven_stoutm",
        heal = 2,
        replacement = BEER_GLASS,
        effects = MATURE_DWARVEN_STOUT_EFFECTS,
        option = "drink",
    ),
    MATURE_DWARVEN_STOUT_KEG_4(
        item = "item.dwarven_stoutm4",
        heal = 2,
        replacement = "item.dwarven_stoutm3",
        effects = MATURE_DWARVEN_STOUT_EFFECTS,
        option = "drink",
    ),
    MATURE_DWARVEN_STOUT_KEG_3(
        item = "item.dwarven_stoutm3",
        heal = 2,
        replacement = "item.dwarven_stoutm2",
        effects = MATURE_DWARVEN_STOUT_EFFECTS,
        option = "drink",
    ),
    MATURE_DWARVEN_STOUT_KEG_2(
        item = "item.dwarven_stoutm2",
        heal = 2,
        replacement = "item.dwarven_stoutm1",
        effects = MATURE_DWARVEN_STOUT_EFFECTS,
        option = "drink",
    ),
    MATURE_DWARVEN_STOUT_KEG_1(
        item = "item.dwarven_stoutm1",
        heal = 2,
        replacement = CALQUAT_KEG,
        effects = MATURE_DWARVEN_STOUT_EFFECTS,
        option = "drink",
    ),
    MATURE_ASGARNIAN_ALE(
        item = "item.asgarnian_alem",
        heal = 2,
        replacement = BEER_GLASS,
        effects = MATURE_ASGARNIAN_ALE_EFFECTS,
        option = "drink",
    ),
    MATURE_ASGARNIAN_ALE_KEG_4(
        item = "item.asgarnian_alem4",
        heal = 2,
        replacement = "item.asgarnian_alem3",
        effects = MATURE_ASGARNIAN_ALE_EFFECTS,
        option = "drink",
    ),
    MATURE_ASGARNIAN_ALE_KEG_3(
        item = "item.asgarnian_alem3",
        heal = 2,
        replacement = "item.asgarnian_alem2",
        effects = MATURE_ASGARNIAN_ALE_EFFECTS,
        option = "drink",
    ),
    MATURE_ASGARNIAN_ALE_KEG_2(
        item = "item.asgarnian_alem2",
        heal = 2,
        replacement = "item.asgarnian_alem1",
        effects = MATURE_ASGARNIAN_ALE_EFFECTS,
        option = "drink",
    ),
    MATURE_ASGARNIAN_ALE_KEG_1(
        item = "item.asgarnian_alem1",
        heal = 2,
        replacement = CALQUAT_KEG,
        effects = MATURE_ASGARNIAN_ALE_EFFECTS,
        option = "drink",
    ),
    MATURE_GREENMANS_ALE(
        item = "item.greenmans_alem",
        heal = 2,
        replacement = BEER_GLASS,
        effects = MATURE_GREENMANS_ALE_EFFECTS,
        option = "drink",
    ),
    MATURE_GREENMANS_ALE_KEG_4(
        item = "item.greenmans_alem4",
        heal = 2,
        replacement = "item.greenmans_alem3",
        effects = MATURE_GREENMANS_ALE_EFFECTS,
        option = "drink",
    ),
    MATURE_GREENMANS_ALE_KEG_3(
        item = "item.greenmans_alem3",
        heal = 2,
        replacement = "item.greenmans_alem2",
        effects = MATURE_GREENMANS_ALE_EFFECTS,
        option = "drink",
    ),
    MATURE_GREENMANS_ALE_KEG_2(
        item = "item.greenmans_alem2",
        heal = 2,
        replacement = "item.greenmans_alem1",
        effects = MATURE_GREENMANS_ALE_EFFECTS,
        option = "drink",
    ),
    MATURE_GREENMANS_ALE_KEG_1(
        item = "item.greenmans_alem1",
        heal = 2,
        replacement = CALQUAT_KEG,
        effects = MATURE_GREENMANS_ALE_EFFECTS,
        option = "drink",
    ),
    MATURE_MIND_BOMB(
        item = "item.mature_wmb",
        heal = 2,
        replacement = BEER_GLASS,
        effects = MATURE_MIND_BOMB_EFFECTS,
        option = "drink",
    ),
    MATURE_MIND_BOMB_KEG_4(
        item = "item.mind_bombm4",
        heal = 2,
        replacement = "item.mind_bombm3",
        effects = MATURE_MIND_BOMB_EFFECTS,
        option = "drink",
    ),
    MATURE_MIND_BOMB_KEG_3(
        item = "item.mind_bombm3",
        heal = 2,
        replacement = "item.mind_bombm2",
        effects = MATURE_MIND_BOMB_EFFECTS,
        option = "drink",
    ),
    MATURE_MIND_BOMB_KEG_2(
        item = "item.mind_bombm2",
        heal = 2,
        replacement = "item.mind_bombm1",
        effects = MATURE_MIND_BOMB_EFFECTS,
        option = "drink",
    ),
    MATURE_MIND_BOMB_KEG_1(
        item = "item.mind_bombm1",
        heal = 2,
        replacement = CALQUAT_KEG,
        effects = MATURE_MIND_BOMB_EFFECTS,
        option = "drink",
    ),
    MATURE_DRAGON_BITTER(
        item = "item.dragon_bitterm",
        heal = 2,
        replacement = BEER_GLASS,
        effects = MATURE_DRAGON_BITTER_EFFECTS,
        option = "drink",
    ),
    MATURE_DRAGON_BITTER_KEG_4(
        item = "item.dragon_bitterm4",
        heal = 2,
        replacement = "item.dragon_bitterm3",
        effects = MATURE_DRAGON_BITTER_EFFECTS,
        option = "drink",
    ),
    MATURE_DRAGON_BITTER_KEG_3(
        item = "item.dragon_bitterm3",
        heal = 2,
        replacement = "item.dragon_bitterm2",
        effects = MATURE_DRAGON_BITTER_EFFECTS,
        option = "drink",
    ),
    MATURE_DRAGON_BITTER_KEG_2(
        item = "item.dragon_bitterm2",
        heal = 2,
        replacement = "item.dragon_bitterm1",
        effects = MATURE_DRAGON_BITTER_EFFECTS,
        option = "drink",
    ),
    MATURE_DRAGON_BITTER_KEG_1(
        item = "item.dragon_bitterm1",
        heal = 2,
        replacement = CALQUAT_KEG,
        effects = MATURE_DRAGON_BITTER_EFFECTS,
        option = "drink",
    ),
    MATURE_MOONLIGHT_MEAD(item = "item.moonlight_meadm", heal = 6, replacement = BEER_GLASS, option = "drink"),
    MATURE_MOONLIGHT_MEAD_KEG_4(
        item = "item.moonlight_meadm4",
        heal = 6,
        replacement = "item.moonlight_meadm3",
        option = "drink",
    ),
    MATURE_MOONLIGHT_MEAD_KEG_3(
        item = "item.moonlight_meadm3",
        heal = 6,
        replacement = "item.moonlight_meadm2",
        option = "drink",
    ),
    MATURE_MOONLIGHT_MEAD_KEG_2(
        item = "item.moonlight_meadm2",
        heal = 6,
        replacement = "item.moonlight_meadm1",
        option = "drink",
    ),
    MATURE_MOONLIGHT_MEAD_KEG_1(
        item = "item.moonlight_meadm1",
        heal = 6,
        replacement = CALQUAT_KEG,
        option = "drink",
    ),
    MATURE_AXEMANS_FOLLY(
        item = "item.axemans_follym",
        heal = 2,
        replacement = BEER_GLASS,
        effects = MATURE_AXEMANS_FOLLY_EFFECTS,
        option = "drink",
    ),
    MATURE_AXEMANS_FOLLY_KEG_4(
        item = "item.axemans_follym4",
        heal = 2,
        replacement = "item.axemans_follym3",
        effects = MATURE_AXEMANS_FOLLY_EFFECTS,
        option = "drink",
    ),
    MATURE_AXEMANS_FOLLY_KEG_3(
        item = "item.axemans_follym3",
        heal = 2,
        replacement = "item.axemans_follym2",
        effects = MATURE_AXEMANS_FOLLY_EFFECTS,
        option = "drink",
    ),
    MATURE_AXEMANS_FOLLY_KEG_2(
        item = "item.axemans_follym2",
        heal = 2,
        replacement = "item.axemans_follym1",
        effects = MATURE_AXEMANS_FOLLY_EFFECTS,
        option = "drink",
    ),
    MATURE_AXEMANS_FOLLY_KEG_1(
        item = "item.axemans_follym1",
        heal = 2,
        replacement = CALQUAT_KEG,
        effects = MATURE_AXEMANS_FOLLY_EFFECTS,
        option = "drink",
    ),
    MATURE_CHEFS_DELIGHT(
        item = "item.chefs_delightm",
        heal = 2,
        replacement = BEER_GLASS,
        effects = MATURE_CHEFS_DELIGHT_EFFECTS,
        option = "drink",
    ),
    MATURE_CHEFS_DELIGHT_KEG_4(
        item = "item.chefs_delightm4",
        heal = 2,
        replacement = "item.chefs_delightm3",
        effects = MATURE_CHEFS_DELIGHT_EFFECTS,
        option = "drink",
    ),
    MATURE_CHEFS_DELIGHT_KEG_3(
        item = "item.chefs_delightm3",
        heal = 2,
        replacement = "item.chefs_delightm2",
        effects = MATURE_CHEFS_DELIGHT_EFFECTS,
        option = "drink",
    ),
    MATURE_CHEFS_DELIGHT_KEG_2(
        item = "item.chefs_delightm2",
        heal = 2,
        replacement = "item.chefs_delightm1",
        effects = MATURE_CHEFS_DELIGHT_EFFECTS,
        option = "drink",
    ),
    MATURE_CHEFS_DELIGHT_KEG_1(
        item = "item.chefs_delightm1",
        heal = 2,
        replacement = CALQUAT_KEG,
        effects = MATURE_CHEFS_DELIGHT_EFFECTS,
        option = "drink",
    ),
    MATURE_SLAYERS_RESPITE(
        item = "item.slayers_respitem",
        heal = 2,
        replacement = BEER_GLASS,
        effects = MATURE_SLAYERS_RESPITE_EFFECTS,
        option = "drink",
    ),
    MATURE_SLAYERS_RESPITE_KEG_4(
        item = "item.slayers_respitem4",
        heal = 2,
        replacement = "item.slayers_respitem3",
        effects = MATURE_SLAYERS_RESPITE_EFFECTS,
        option = "drink",
    ),
    MATURE_SLAYERS_RESPITE_KEG_3(
        item = "item.slayers_respitem3",
        heal = 2,
        replacement = "item.slayers_respitem2",
        effects = MATURE_SLAYERS_RESPITE_EFFECTS,
        option = "drink",
    ),
    MATURE_SLAYERS_RESPITE_KEG_2(
        item = "item.slayers_respitem2",
        heal = 2,
        replacement = "item.slayers_respitem1",
        effects = MATURE_SLAYERS_RESPITE_EFFECTS,
        option = "drink",
    ),
    MATURE_SLAYERS_RESPITE_KEG_1(
        item = "item.slayers_respitem1",
        heal = 2,
        replacement = CALQUAT_KEG,
        effects = MATURE_SLAYERS_RESPITE_EFFECTS,
        option = "drink",
    ),
    MATURE_CIDER(
        item = "item.mature_cider",
        heal = 2,
        replacement = BEER_GLASS,
        effects = MATURE_CIDER_EFFECTS,
        option = "drink",
    ),
    MATURE_CIDER_KEG_4(
        item = "item.ciderm4",
        heal = 2,
        replacement = "item.ciderm3",
        effects = MATURE_CIDER_EFFECTS,
        option = "drink",
    ),
    MATURE_CIDER_KEG_3(
        item = "item.ciderm3",
        heal = 2,
        replacement = "item.ciderm2",
        effects = MATURE_CIDER_EFFECTS,
        option = "drink",
    ),
    MATURE_CIDER_KEG_2(
        item = "item.ciderm2",
        heal = 2,
        replacement = "item.ciderm1",
        effects = MATURE_CIDER_EFFECTS,
        option = "drink",
    ),
    MATURE_CIDER_KEG_1(
        item = "item.ciderm1",
        heal = 2,
        replacement = CALQUAT_KEG,
        effects = MATURE_CIDER_EFFECTS,
        option = "drink",
    ),

    /**
     * The player-owned house ales. Same drink, own item ids, and they vanish from the inventory on
     * the way out of the house rather than leaving a glass behind.
     */
    POH_BEER(item = "item.beer_7740", heal = 1, effects = BEER_EFFECTS, option = "drink"),
    POH_ASGARNIAN_ALE(
        item = "item.asgarnian_ale_7744",
        heal = 1,
        effects = ASGARNIAN_ALE_EFFECTS,
        option = "drink",
    ),
    POH_GREENMANS_ALE(
        item = "item.greenmans_ale_7746",
        heal = 1,
        effects = GREENMANS_ALE_EFFECTS,
        option = "drink",
    ),
    POH_DRAGON_BITTER(
        item = "item.dragon_bitter_7748",
        heal = 1,
        effects = DRAGON_BITTER_EFFECTS,
        option = "drink",
    ),
    POH_MOONLIGHT_MEAD(item = "item.moonlight_mead_7750", heal = 4, option = "drink"),
    POH_CIDER(item = "item.cider_7752", heal = 1, effects = CIDER_EFFECTS, option = "drink"),
    POH_CHEFS_DELIGHT(
        item = "item.chefs_delight_7754",
        heal = 1,
        effects = CHEFS_DELIGHT_EFFECTS,
        option = "drink",
    ),

    /**
     * Tea. Three hitpoints and a small Attack boost, in whatever the tea was poured into - and the
     * cup comes back, which is most of the point of the porcelain ones.
     */
    CUP_OF_TEA(
        item = "item.cup_of_tea_1978",
        heal = 3,
        replacement = EMPTY_CUP,
        effects = TEA_EFFECTS,
        option = "drink",
    ),
    CUP_OF_TEA_UNOBTAINABLE(
        item = "item.cup_of_tea",
        heal = 3,
        replacement = EMPTY_CUP,
        effects = TEA_EFFECTS,
        option = "drink",
    ),
    CUP_OF_TEA_GHOSTS_AHOY(
        item = "item.cup_of_tea_4245",
        heal = 3,
        replacement = PORCELAIN_CUP,
        effects = TEA_EFFECTS,
        option = "drink",
    ),
    CUP_OF_TEA_GHOSTS_AHOY_MILKY(
        item = "item.cup_of_tea_4246",
        heal = 3,
        replacement = PORCELAIN_CUP,
        effects = TEA_EFFECTS,
        option = "drink",
    ),
    STRONG_CUP_OF_TEA(item = "item.strong_cup_of_tea", heal = 3, effects = TEA_EFFECTS, option = "drink"),

     /**
      * Nettle tea, in the bowl it is boiled in or decanted into a cup. The run energy only comes
      * back when the drinker is injured, which is not modelled - the five per cent is unconditional
      * here. Nettle-water is the unboiled half and does nothing at all.
      */
    NETTLE_TEA(
        item = "item.nettle_tea",
        heal = 3,
        replacement = BOWL,
        effects = NETTLE_TEA_EFFECTS,
        option = "drink",
    ),
    NETTLE_TEA_MILKY(
        item = "item.nettle_tea_4240",
        heal = 3,
        replacement = BOWL,
        effects = NETTLE_TEA_EFFECTS,
        option = "drink",
    ),
    CUP_OF_NETTLE_TEA(
        item = "item.cup_of_tea_4242",
        heal = 3,
        replacement = PORCELAIN_CUP,
        effects = NETTLE_TEA_EFFECTS,
        option = "drink",
    ),
    CUP_OF_NETTLE_TEA_MILKY(
        item = "item.cup_of_tea_4243",
        heal = 3,
        replacement = PORCELAIN_CUP,
        effects = NETTLE_TEA_EFFECTS,
        option = "drink",
    ),
    NETTLE_WATER(item = "item.nettlewater", heal = 1, replacement = BOWL, option = "drink"),

    /**
     * House tea, which buys a Construction level. The wiki publishes only that boost, so the three
     * hitpoints are the ordinary cup of tea's rather than a figure of their own.
     */
    CUP_OF_TEA_CLAY(
        item = "item.cup_of_tea_7730",
        heal = 3,
        replacement = "item.empty_cup_7728",
        effects = HOUSE_TEA_EFFECTS,
        option = "drink",
    ),
    CUP_OF_TEA_CLAY_MILKY(
        item = "item.cup_of_tea_7731",
        heal = 3,
        replacement = "item.empty_cup_7728",
        effects = HOUSE_TEA_EFFECTS,
        option = "drink",
    ),
    CUP_OF_TEA_PORCELAIN(
        item = "item.cup_of_tea_7733",
        heal = 3,
        replacement = "item.porcelain_cup_7732",
        effects = HOUSE_TEA_EFFECTS,
        option = "drink",
    ),
    CUP_OF_TEA_PORCELAIN_MILKY(
        item = "item.cup_of_tea_7734",
        heal = 3,
        replacement = "item.porcelain_cup_7732",
        effects = HOUSE_TEA_EFFECTS,
        option = "drink",
    ),
    CUP_OF_TEA_TRIMMED(
        item = "item.cup_of_tea_7736",
        heal = 3,
        replacement = "item.porcelain_cup_7735",
        effects = HOUSE_TEA_EFFECTS,
        option = "drink",
    ),
    CUP_OF_TEA_TRIMMED_MILKY(
        item = "item.cup_of_tea_7737",
        heal = 3,
        replacement = "item.porcelain_cup_7735",
        effects = HOUSE_TEA_EFFECTS,
        option = "drink",
    ),

    /**
     * The gnome cocktails again, under the item ids the Grand Tree's stock and the pre-made bottles
     * carry. Same drink, same glass back.
     */
    FRUIT_BLAST_GRAND_TREE(
        item = "item.fruit_blast_9514",
        heal = 9,
        replacement = COCKTAIL_GLASS,
        option = "drink",
    ),
    PINEAPPLE_PUNCH_GRAND_TREE(
        item = "item.pineapple_punch_9512",
        heal = 9,
        replacement = COCKTAIL_GLASS,
        option = "drink",
    ),
    WIZARD_BLIZZARD_GRAND_TREE(
        item = "item.wizard_blizzard_9508",
        heal = 5,
        replacement = COCKTAIL_GLASS,
        effects = WIZARD_BLIZZARD_EFFECTS,
        option = "drink",
    ),
    SHORT_GREEN_GUY_GRAND_TREE(
        item = "item.short_green_guy_9510",
        heal = 5,
        replacement = COCKTAIL_GLASS,
        effects = SHORT_GREEN_GUY_EFFECTS,
        option = "drink",
    ),
    DRUNK_DRAGON_GRAND_TREE(
        item = "item.drunk_dragon_9516",
        heal = 5,
        replacement = COCKTAIL_GLASS,
        effects = STRONG_COCKTAIL_EFFECTS,
        option = "drink",
    ),
    CHOC_SATURDAY_GRAND_TREE(
        item = "item.choc_saturday_9518",
        heal = 5,
        replacement = COCKTAIL_GLASS,
        effects = STRONG_COCKTAIL_EFFECTS,
        option = "drink",
    ),
    BLURBERRY_SPECIAL_GRAND_TREE(
        item = "item.blurberry_special_9520",
        heal = 7,
        replacement = COCKTAIL_GLASS,
        effects = STRONG_COCKTAIL_EFFECTS,
        option = "drink",
    ),
    PREMADE_FRUIT_BLAST(item = "item.premade_fr_blast", heal = 9, replacement = COCKTAIL_GLASS, option = "drink"),
    PREMADE_PINEAPPLE_PUNCH(
        item = "item.premade_p_punch",
        heal = 9,
        replacement = COCKTAIL_GLASS,
        option = "drink",
    ),
    PREMADE_WIZARD_BLIZZARD(
        item = "item.premade_wiz_blzd",
        heal = 5,
        replacement = COCKTAIL_GLASS,
        effects = WIZARD_BLIZZARD_EFFECTS,
        option = "drink",
    ),
    PREMADE_SHORT_GREEN_GUY(
        item = "item.premade_sgg",
        heal = 5,
        replacement = COCKTAIL_GLASS,
        effects = SHORT_GREEN_GUY_EFFECTS,
        option = "drink",
    ),
    PREMADE_DRUNK_DRAGON(
        item = "item.premade_dr_dragon",
        heal = 5,
        replacement = COCKTAIL_GLASS,
        effects = STRONG_COCKTAIL_EFFECTS,
        option = "drink",
    ),
    PREMADE_CHOC_SATURDAY(
        item = "item.premade_choc_sdy",
        heal = 5,
        replacement = COCKTAIL_GLASS,
        effects = STRONG_COCKTAIL_EFFECTS,
        option = "drink",
    ),
    PREMADE_BLURBERRY_SPECIAL(
        item = "item.premade_blurb_sp",
        heal = 7,
        replacement = COCKTAIL_GLASS,
        effects = STRONG_COCKTAIL_EFFECTS,
        option = "drink",
    ),

    /**
     * The Varlamore wines. Sixteen hitpoints, a level in one skill, and five Attack plus a level in
     * a neighbouring skill gone - one shape, a different pair of skills each.
     *
     * Principum red, Fortis ash white and Xochipaltic rose carry only the half the wiki publishes:
     * the heal and the Attack drain. Which skill each of the three lifts is not documented anywhere
     * yet, and a guess would be indistinguishable from a fact once it is in the file.
     */
    ECLIPSE_RED(
        item = "item.eclipse_red",
        heal = 16,
        effects = wine(Skills.WOODCUTTING, Skills.FLETCHING),
        option = "drink",
    ),
    BLACKBIRD_RED(
        item = "item.blackbird_red",
        heal = 16,
        effects = wine(Skills.HUNTER, Skills.SLAYER),
        option = "drink",
    ),
    CHILHUAC_RED(
        item = "item.chilhuac_red",
        heal = 16,
        effects = wine(Skills.FIREMAKING, Skills.COOKING),
        option = "drink",
    ),
    IXCOZTIC_WHITE(
        item = "item.ixcoztic_white",
        heal = 16,
        effects = wine(Skills.FARMING, Skills.HERBLORE),
        option = "drink",
    ),
    METZTONALLI_WHITE(
        item = "item.metztonalli_white",
        heal = 16,
        effects = wine(Skills.RUNECRAFTING, Skills.FIREMAKING),
        option = "drink",
    ),
    TONAMEYO_WHITE(
        item = "item.tonameyo_white",
        heal = 16,
        effects = wine(Skills.PRAYER, Skills.THIEVING),
        option = "drink",
    ),
    CHICHILIHUI_ROSE(
        item = "item.chichilihui_ros",
        heal = 16,
        effects = wine(Skills.HERBLORE, Skills.FARMING),
        option = "drink",
    ),
    IMPERIAL_ROSE(
        item = "item.imperial_ros",
        heal = 16,
        effects = wine(Skills.SLAYER, Skills.AGILITY),
        option = "drink",
    ),
    PRINCIPUM_RED(item = "item.principum_red", heal = 16, effects = UNDOCUMENTED_WINE_EFFECTS, option = "drink"),
    FORTIS_ASH_WHITE(
        item = "item.fortis_ash_white",
        heal = 16,
        effects = UNDOCUMENTED_WINE_EFFECTS,
        option = "drink",
    ),
    XOCHIPALTIC_ROSE(
        item = "item.xochipaltic_ros",
        heal = 16,
        effects = UNDOCUMENTED_WINE_EFFECTS,
        option = "drink",
    ),

    /**
     * The Varlamore liquors, and the rest of the drink that does not belong to a family.
     */
    MOON_LITE(
        item = "item.moonlite",
        heal = 5,
        effects = listOf(Boost(Skills.STRENGTH, 1, 5), Drain(Skills.ATTACK, 1, 5)),
        option = "drink",
    ),
    SUN_SHINE(
        item = "item.sunshine",
        heal = 5,
        effects = listOf(Boost(Skills.STRENGTH, 1, 5), Drain(Skills.ATTACK, 4, 0)),
        option = "drink",
    ),
    SUNBEAM_ALE(item = "item.sunbeam_ale", heal = 1, effects = SUNBEAM_ALE_EFFECTS, option = "drink"),
    STEAMFORGE_BREW(item = "item.steamforge_brew", heal = 1, effects = STEAMFORGE_BREW_EFFECTS, option = "drink"),
    TRAPPERS_TIPPLE(item = "item.trappers_tipple", heal = 1, effects = TRAPPERS_TIPPLE_EFFECTS, option = "drink"),
    KOVACS_GROG(item = "item.kovacs_grog", heal = 1, effects = KOVACS_GROG_EFFECTS, option = "drink"),
    LIZARDKICKER(item = "item.lizardkicker", effects = LIZARDKICKER_EFFECTS, option = "drink"),
    ELVEN_DAWN(
        item = "item.elven_dawn",
        heal = 1,
        effects = listOf(Boost(Skills.AGILITY, 1, 0), Drain(Skills.STRENGTH, 1, 0)),
        option = "drink",
    ),
    BLOOD_PINT(item = "item.blood_pint", effects = BLOOD_PINT_EFFECTS, option = "drink"),
    KARAMJAN_RUM(item = "item.karamjan_rum", heal = 5, effects = RUM_EFFECTS, option = "drink"),
    RUM(item = "item.rum_28896", heal = 5, effects = RUM_EFFECTS, option = "drink"),
    CHOCOLATEY_MILK(item = "item.chocolatey_milk", heal = 4, option = "drink"),
    LIGHT_BEER(item = "item.light_beer", option = "drink"),
    BLOODY_BRACER(
        item = "item.bloody_bracer",
        heal = 2,
        overheal = true,
        effects = listOf(Drain(Skills.PRAYER, 2, 4)),
        option = "drink",
    ),
    BOTTLE_OF_WINE(
        item = "item.bottle_of_wine",
        heal = 14,
        replacement = "item.empty_wine_bottle",
        effects = listOf(Drain(Skills.ATTACK, 3, 0)),
        option = "drink",
    ),
    HALF_FULL_WINE_JUG(
        item = "item.half_full_wine_jug",
        heal = 7,
        replacement = JUG,
        effects = listOf(Drain(Skills.ATTACK, 2, 0)),
        option = "drink",
    ),
    JUG_OF_BAD_WINE(
        item = "item.jug_of_bad_wine",
        replacement = JUG,
        effects = listOf(Drain(Skills.ATTACK, 3, 0)),
        option = "drink",
    ),

    /**
     * The dirty pint from Rum Deal: three levels of Strength and one of Mining, paid for out of
     * six other skills. The article only says it heals "some" hitpoints, but the wiki's own
     * all-food table puts the figure at 14 - the highest-healing drink in the game.
     */
    BRAINDEATH_RUM(
        item = "item.braindeath_rum",
        heal = 14,
        effects =
            listOf(
                Boost(Skills.STRENGTH, 3, 0),
                Boost(Skills.MINING, 1, 0),
                Drain(Skills.DEFENCE, 0, 10),
                Drain(Skills.ATTACK, 0, 5),
                Drain(Skills.PRAYER, 0, 5),
                Drain(Skills.RANGED, 0, 5),
                Drain(Skills.MAGIC, 0, 5),
                Drain(Skills.AGILITY, 0, 5),
                Drain(Skills.HERBLORE, 0, 5),
            ),
        option = "drink",
    ),

    /**
     * Christmas event drink. Both are drinkable and neither has a published effect beyond the
     * message, so neither has one here.
     */
    MULLED_PINE(item = "item.mulled_pine", option = "drink"),
    EGGNOG(item = "item.eggnog", option = "drink"),

    /**
     * The poison chalice, the only drink in the game whose result is rolled. Seven outcomes, each
     * as likely as the next, from a thirty per cent heal to half the drinker's hitpoints gone.
     *
     * The one liberty taken is the worst outcome's stat hit: the wiki gives it as one to three
     * levels and it is applied as three, because [Drain] takes a fixed amount.
     */
    POISON_CHALICE(
        item = "item.poison_chalice",
        effects = listOf(OneOf(POISON_CHALICE_OUTCOMES)),
        option = "drink",
    ),

    /**
     * Drink the live game will not let the player have. Each carries a Drink option the item
     * definition advertises and the game then refuses, so the refusal is the content: without it
     * these look like the bug this pass set out to fix.
     */
    KELDA_STOUT(
        item = "item.kelda_stout",
        option = "drink",
        refusal =
            "This stout seems absolutely vile and disgusting. Besides, I'm supposed to bring it " +
                "to my drunken, kebab obsessed friend.",
    ),
    ASGOLDIAN_ALE(
        item = "item.asgoldian_ale",
        option = "drink",
        refusal = "I don't think I'd like gold in beer thanks. Leave it for the dwarves.",
    ),
    DIRTY_BLAST(
        item = "item.dirty_blast",
        option = "drink",
        refusal = "No thanks, that looks DISGUSTING! I won't drink that!",
    ),
    SPECIAL_HOT_SAUCE(
        item = "item.special_hot_sauce",
        option = "drink",
        refusal = "I definitely do not want to drink any of this!",
    ),
    BIG_BUCKET_OF_CAMEL_MILK(
        item = "item.big_bucket_of_camel_milk",
        option = "drink",
        refusal = "I don't think I actually want to drink that. I need it for the ice cream!",
    ),
    TROUBLE_BREWING_RUM_RED(
        item = "item.rum",
        option = "drink",
        refusal = "It hardly seems worth drinking the 'rum' here.",
    ),
    TROUBLE_BREWING_RUM_BLUE(
        item = "item.rum_8941",
        option = "drink",
        refusal = "It hardly seems worth drinking the 'rum' here.",
    ),

    /**
     * Everything else the cache says is edible.
     *
     * `Food` covered 128 items; the rev-228 cache carries 345 with an "Eat" option, and the 217
     * that were left over did nothing at all when eaten - the whole Varlamore fish and bat ladder,
     * every Gnome batta and crunchy, the Dorgesh-Kaan menu, the Chambers and Gauntlet copies of
     * food that already worked under a different id, and most of the fruit and veg in the game.
     *
     * Heal amounts come from the OSRS Wiki's own all-food table
     * (https://oldschool.runescape.wiki/w/Food/All_food), read on 2026-09-03 and matched to these
     * ids by cache name. Deliberately still not here, because their "Eat" option does nothing in
     * the live game either: the whole pineapple, tenti pineapple and watermelon (all three have to
     * be cut up first), cooked bream and cooked moss lizard (which only heal inside the Moons of
     * Peril), the spicy stew (a random skill-boost table rather than a heal) and the bruised
     * banana. Neither are the unfinished gnome dishes, the joke items and the poisoned food, none
     * of which the wiki lists as healing anything.
     */
    /**
     * Fish and seafood.
     */
    BLIGHTED_ANGLERFISH(item = "item.blighted_anglerfish", overheal = true),
    BLIGHTED_KARAMBWAN(item = "item.blighted_karambwan", heal = 18, comboFood = true),
    BLIGHTED_MANTA_RAY(item = "item.blighted_manta_ray", heal = 22),
    BRAWK_FISH_3(item = "item.brawk_fish_3", heal = 14),
    CAVIAR(item = "item.caviar", heal = 5),
    CHOPPED_TUNA(item = "item.chopped_tuna", heal = 10),
    COOKED_CRAB_MEAT_5(item = "item.cooked_crab_meat", heal = 2, replacement = "item.cooked_crab_meat_7523"),
    COOKED_CRAB_MEAT_4(item = "item.cooked_crab_meat_7523", heal = 2, replacement = "item.cooked_crab_meat_7524"),
    COOKED_CRAB_MEAT_3(item = "item.cooked_crab_meat_7524", heal = 2, replacement = "item.cooked_crab_meat_7525"),
    COOKED_CRAB_MEAT_2(item = "item.cooked_crab_meat_7525", heal = 2, replacement = "item.cooked_crab_meat_7526"),
    COOKED_CRAB_MEAT_1(item = "item.cooked_crab_meat_7526", heal = 2),
    COOKED_FISHCAKE(item = "item.cooked_fishcake", heal = 11),
    COOKED_KARAMBWAN(item = "item.cooked_karambwan_23533", heal = 18, comboFood = true),
    COOKED_SLIMY_EEL(item = "item.cooked_slimy_eel", effects = listOf(HealRange(6, 10))),
    CORRUPTED_PADDLEFISH(item = "item.corrupted_paddlefish", heal = 16),
    CRYSTAL_PADDLEFISH(item = "item.crystal_paddlefish", heal = 16),
    EDIBLE_SEAWEED(item = "item.edible_seaweed", heal = 4),
    EEL_SUSHI(item = "item.eel_sushi", heal = 10),
    FRESH_MONKFISH(item = "item.fresh_monkfish_7943", heal = 1),
    GIANT_CARP(item = "item.giant_carp", heal = 6),
    GIANT_FROG_LEGS(item = "item.giant_frog_legs", heal = 6),
    KYREN_FISH_6(item = "item.kyren_fish_6", heal = 23),
    LAVA_EEL(item = "item.lava_eel", heal = 11),
    LECKISH_FISH_2(item = "item.leckish_fish_2", heal = 11),
    LOACH(item = "item.loach", heal = 3),
    MONKFISH_20547(item = "item.monkfish_20547", heal = 16),
    MYCIL_FISH_4(item = "item.mycil_fish_4", heal = 17),
    PADDLEFISH(item = "item.paddlefish", heal = 20),
    PYSK_FISH_0(item = "item.pysk_fish_0", heal = 5),
    ROE(item = "item.roe", heal = 3),
    ROQED_FISH_5(item = "item.roqed_fish_5", heal = 20),
    SHARK_6969(item = "item.shark_6969", heal = 20),
    SHARK_20390(item = "item.shark_20390", heal = 20),
    SUPHI_FISH_1(item = "item.suphi_fish_1", heal = 8),
    TUNA_26149(item = "item.tuna_26149", heal = 10),

    /**
     * Meat and hunter meat.
     */
    BAT_SHISH(item = "item.bat_shish", heal = 2),
    CHEESE_TOM_BATTA(item = "item.cheesetom_batta_9535", heal = 11),
    COATED_FROGS_LEGS(item = "item.coated_frogs_legs", heal = 2),
    COOKED_BARB_TAILED_KEBBIT(
        item = "item.cooked_barbtailed_kebbit",
        heal = 7,
        effects = listOf(DelayedHeal(5, HUNTER_MEAT_DELAY)),
    ),
    COOKED_CHICKEN(item = "item.cooked_chicken_4291", heal = 3),
    COOKED_CHOMPY_7228(item = "item.cooked_chompy_7228", heal = 10),
    COOKED_DASHING_KEBBIT(
        item = "item.cooked_dashing_kebbit",
        heal = 13,
        effects = listOf(DelayedHeal(10, HUNTER_MEAT_DELAY)),
    ),
    COOKED_GRAAHK(item = "item.cooked_graahk", heal = 8, effects = listOf(DelayedHeal(6, HUNTER_MEAT_DELAY))),
    COOKED_JUBBLY(item = "item.cooked_jubbly", heal = 15),
    COOKED_KYATT(item = "item.cooked_kyatt", heal = 9, effects = listOf(DelayedHeal(8, HUNTER_MEAT_DELAY))),
    COOKED_LARUPIA(item = "item.cooked_larupia", heal = 6, effects = listOf(DelayedHeal(5, HUNTER_MEAT_DELAY))),
    COOKED_MEAT(item = "item.cooked_meat_4293", heal = 3),
    COOKED_MOONLIGHT_ANTELOPE(
        item = "item.cooked_moonlight_antelope",
        heal = 14,
        effects = listOf(DelayedHeal(12, HUNTER_MEAT_DELAY)),
    ),
    COOKED_MYSTERY_MEAT(item = "item.cooked_mystery_meat", heal = 5),
    COOKED_OOMLIE_WRAP(item = "item.cooked_oomlie_wrap", heal = 14),
    COOKED_PYRE_FOX(item = "item.cooked_pyre_fox", heal = 11, effects = listOf(DelayedHeal(8, HUNTER_MEAT_DELAY))),
    COOKED_SUNLIGHT_ANTELOPE(
        item = "item.cooked_sunlight_antelope",
        heal = 12,
        effects = listOf(DelayedHeal(9, HUNTER_MEAT_DELAY)),
    ),
    COOKED_WILD_KEBBIT(
        item = "item.cooked_wild_kebbit",
        heal = 4,
        effects = listOf(DelayedHeal(4, HUNTER_MEAT_DELAY)),
    ),
    FAT_SNAIL_MEAT(item = "item.fat_snail_meat", effects = listOf(HealRange(7, 9))),
    FRUIT_BATTA_9527(item = "item.fruit_batta_9527", heal = 11),
    GIRAL_BAT_2(item = "item.giral_bat_2", heal = 11),
    GUANIC_BAT_0(item = "item.guanic_bat_0", heal = 5),
    HONEY_LOCUST(item = "item.honey_locust", heal = 20),
    KING_WORM(item = "item.king_worm", heal = 2),
    KRYKET_BAT_4(item = "item.kryket_bat_4", heal = 17),
    LEAN_SNAIL_MEAT(item = "item.lean_snail_meat", effects = listOf(HealRange(5, 8))),
    LOCUST_MEAT(item = "item.locust_meat", heal = 3),
    MINCED_MEAT(item = "item.minced_meat", heal = 13),
    MURNG_BAT_5(item = "item.murng_bat_5", heal = 20),
    PHLUXIA_BAT_3(item = "item.phluxia_bat_3", heal = 14),
    PRAEL_BAT_1(item = "item.prael_bat_1", heal = 8),
    PREMADE_C_T_BATTA(item = "item.premade_ct_batta", heal = 11),
    PREMADE_FR_T_BATTA(item = "item.premade_frt_batta", heal = 11),
    PREMADE_T_D_BATTA(item = "item.premade_td_batta", heal = 11),
    PREMADE_VEG_BATTA(item = "item.premade_veg_batta", heal = 11),
    PREMADE_W_M_BATTA(item = "item.premade_wm_batta", heal = 11),
    PREMADE_WORM_HOLE(item = "item.premade_worm_hole", heal = 12),
    PSYKK_BAT_6(item = "item.psykk_bat_6", heal = 23),
    SPICY_MINCED_MEAT(item = "item.spicy_minced_meat", heal = 3),
    STUFFED_SNAKE(item = "item.stuffed_snake", heal = 20),
    TANGLED_TOADS_LEGS_9551(item = "item.tangled_toads_legs_9551", heal = 15),
    THIN_SNAIL_MEAT(item = "item.thin_snail_meat", effects = listOf(HealRange(5, 7))),
    TOAD_BATTA_9529(item = "item.toad_batta_9529", heal = 11),
    TOAD_S_LEGS(item = "item.toads_legs", heal = 3),
    VEGETABLE_BATTA_9533(item = "item.vegetable_batta_9533", heal = 11),
    WORM_BATTA_9531(item = "item.worm_batta_9531", heal = 11),
    WORM_CRUNCHIES_9542(item = "item.worm_crunchies_9542", heal = 8),
    WORM_HOLE_9547(item = "item.worm_hole_9547", heal = 12),

    /**
     * Kebabs, whose heal scales with the eater.
     */
    KEBAB_1971(item = "item.kebab", heal = 3, effects = listOf(HealPercent(7))),
    SUPER_KEBAB(item = "item.super_kebab", heal = 3, effects = listOf(HealPercent(7))),
    UGTHANKI_KEBAB(item = "item.ugthanki_kebab", heal = 19),
    VARLAMORIAN_KEBAB(item = "item.varlamorian_kebab", heal = 4, effects = listOf(HealPercent(10))),

    /**
     * Gnome cookery, and the premade versions sold in the Grand Tree.
     */
    CHOCCHIP_CRUNCHIES_9544(item = "item.chocchip_crunchies_9544", heal = 7),
    CHOCOLATE_BOMB_9553(item = "item.chocolate_bomb_9553", heal = 15),
    PREMADE_CH_CRUNCH(item = "item.premade_ch_crunch", heal = 8),
    PREMADE_CHOC_BOMB(item = "item.premade_choc_bomb", heal = 15),
    PREMADE_S_Y_CRUNCH(item = "item.premade_sy_crunch", heal = 7),
    PREMADE_T_D_CRUNCH(item = "item.premade_td_crunch", heal = 8),
    PREMADE_TTL(item = "item.premade_ttl", heal = 15),
    PREMADE_VEG_BALL(item = "item.premade_veg_ball", heal = 12),
    PREMADE_W_M_CRUN(item = "item.premade_wm_crun", heal = 8),
    SPICY_CRUNCHIES_9540(item = "item.spicy_crunchies_9540", heal = 7),
    TOAD_CRUNCHIES_9538(item = "item.toad_crunchies_9538", heal = 8),
    VEG_BALL_9549(item = "item.veg_ball_9549", heal = 12),

    /**
     * Fruit and vegetables.
     */
    BANANA(item = "item.banana", heal = 2),
    BANANA_STEW(item = "item.banana_stew", heal = 11, replacement = BOWL),
    CABBAGE_1965(item = "item.cabbage", heal = 1),
    CABBAGE_1967(item = "item.cabbage_1967", heal = 1),
    CHEESE(item = "item.cheese", heal = 2),
    CHOCOLATE_STRAWBERRY(item = "item.chocolate_strawberry", heal = 2),
    CHOPPED_ONION(item = "item.chopped_onion", heal = 1),
    CHOPPED_TOMATO(item = "item.chopped_tomato", heal = 2),
    DRAGONFRUIT(item = "item.dragonfruit", heal = 10),
    DWELLBERRIES(item = "item.dwellberries", heal = 2),
    EASTER_EGG(item = "item.easter_egg", heal = 14),
    EQUA_LEAVES(item = "item.equa_leaves", heal = 1),
    GOUT_TUBER(item = "item.gout_tuber", heal = 12),
    LEMON(item = "item.lemon", heal = 2),
    LEMON_CHUNKS(item = "item.lemon_chunks", heal = 2),
    LEMON_SLICES(item = "item.lemon_slices", heal = 2),
    LIME(item = "item.lime", heal = 2),
    LIME_CHUNKS(item = "item.lime_chunks", heal = 2),
    LIME_SLICES(item = "item.lime_slices", heal = 2),
    MELTED_EASTER_EGG(item = "item.melted_easter_egg", heal = 1),
    MONKEY_BAR(item = "item.monkey_bar", heal = 5),
    MONKEY_NUTS(item = "item.monkey_nuts", heal = 4),
    ONION_TOMATO(item = "item.onion__tomato", heal = 3),
    ORANGE(item = "item.orange", heal = 2),
    ORANGE_CHUNKS(item = "item.orange_chunks", heal = 2),
    ORANGE_SLICES(item = "item.orange_slices", heal = 2),
    PAPAYA_FRUIT(item = "item.papaya_fruit", heal = 8),
    PEACH(item = "item.peach", heal = 8),
    PINEAPPLE_CHUNKS(item = "item.pineapple_chunks", heal = 2),
    PINEAPPLE_RING(item = "item.pineapple_ring", heal = 2),
    POT_OF_CREAM(item = "item.pot_of_cream", heal = 1),
    POTATO(item = "item.potato", heal = 1),
    PUMPKIN(item = "item.pumpkin", heal = 14),
    RED_BANANA(item = "item.red_banana", heal = 5),
    SLICED_BANANA(item = "item.sliced_banana", heal = 2),
    SLICED_RED_BANANA(item = "item.sliced_red_banana", heal = 5),
    SPICY_TOMATO(item = "item.spicy_tomato", heal = 2),
    SPINACH_ROLL(item = "item.spinach_roll", heal = 2),
    STRAWBERRY(item = "item.strawberry", heal = 1, effects = listOf(HealPercent(6))),
    TCHIKI_MONKEY_NUTS(item = "item.tchiki_monkey_nuts", heal = 5),
    TOMATO(item = "item.tomato", heal = 2),
    WATERMELON_SLICE(item = "item.watermelon_slice", heal = 1, effects = listOf(HealPercent(5))),
    WHITE_PEARL(item = "item.white_pearl", heal = 2),
    WHITE_TREE_FRUIT(item = "item.white_tree_fruit", heal = 3),

    /**
     * Sweets, cake and confectionery.
     */
    CHOC_ICE(item = "item.chocice", heal = 7),
    CHOCOLATE_BAR(item = "item.chocolate_bar", heal = 3),
    PURPLE_SWEETS_4561(item = "item.purple_sweets", effects = listOf(HealRange(1, 3), RestoreEnergy(10))),
    PURPLE_SWEETS_10476(item = "item.purple_sweets_10476", effects = listOf(HealRange(1, 3), RestoreEnergy(10))),
    SLICE_OF_BIRTHDAY_CAKE(item = "item.slice_of_birthday_cake", heal = 3),

    /**
     * Dorgesh-Kaan cave food.
     */
    FILLETS(item = "item.fillets", heal = 2),
    FINGERS(item = "item.fingers", heal = 2),
    FROGBURGER(item = "item.frogburger", heal = 2),
    FROGSPAWN_GUMBO(item = "item.frogspawn_gumbo", heal = 2),
    GREEN_GLOOP_SOUP(item = "item.green_gloop_soup", heal = 2),
    GRUBS_LA_MODE(item = "item.grubs__la_mode", heal = 2),
    MUSHROOMS(item = "item.mushrooms", heal = 2),
    ROAST_FROG(item = "item.roast_frog", heal = 2),

    /**
     * Bread and the rest.
     */
    BAGUETTE(item = "item.baguette", heal = 6),
    FIELD_RATION(item = "item.field_ration", heal = 10),
    FROG_SPAWN(item = "item.frog_spawn", effects = listOf(HealRange(3, 6))),
    ROLL(item = "item.roll", heal = 6),
    SPIDER_ON_SHAFT(item = "item.spider_on_shaft_6299", effects = listOf(HealRange(7, 10))),
    SPIDER_ON_STICK(item = "item.spider_on_stick_6297", effects = listOf(HealRange(7, 10))),
    SQUARE_SANDWICH(item = "item.square_sandwich", heal = 6),
    STEAK_SANDWICH(item = "item.steak_sandwich", heal = 6),
    TCHIKI_NUT_PASTE(item = "item.tchiki_nut_paste", heal = 5),
    TRIANGLE_SANDWICH(item = "item.triangle_sandwich", heal = 6),

    ;

    companion object {
        val values = enumValues<Food>()
    }
}

/**
 * The vessel a brewed ale keg is poured from, and so what its last pint hands back.
 */
private const val CALQUAT_KEG = "item.calquat_keg"

/**
 * What every gnome cocktail is served in.
 */
private const val COCKTAIL_GLASS = "item.cocktail_glass"

/** The cups and bowls tea comes in. */
private const val EMPTY_CUP = "item.empty_cup"
private const val PORCELAIN_CUP = "item.porcelain_cup"
private const val BOWL = "item.bowl"

/**
 * How long the second half of a Varlamore hunter meat takes to arrive. The wiki gives it as three
 * seconds for every one of them.
 */
private const val HUNTER_MEAT_DELAY = 5

/** What a jug of wine, bad or half drunk, leaves. */
private const val JUG = "item.jug"

/*
 * The ale modifiers, lifted out of the pints so that the four-pint kegs and the player-owned house
 * copies cannot drift away from the drink they are pouring.
 */
private val BEER_EFFECTS = listOf(Boost(Skills.STRENGTH, 1, 2), Drain(Skills.ATTACK, 1, 6))
private val ASGARNIAN_ALE_EFFECTS = listOf(Boost(Skills.STRENGTH, 2, 0), Drain(Skills.ATTACK, 2, 5))
private val DRAGON_BITTER_EFFECTS = ASGARNIAN_ALE_EFFECTS
private val DWARVEN_STOUT_EFFECTS =
    listOf(
        Boost(Skills.MINING, 1, 0),
        Boost(Skills.SMITHING, 1, 0),
        Drain(Skills.ATTACK, 2, 4),
        Drain(Skills.STRENGTH, 2, 4),
        Drain(Skills.DEFENCE, 2, 4),
    )
private val GREENMANS_ALE_EFFECTS =
    listOf(
        Boost(Skills.HERBLORE, 1, 0),
        Drain(Skills.ATTACK, 3, 0),
        Drain(Skills.STRENGTH, 3, 0),
        Drain(Skills.DEFENCE, 3, 0),
    )
private val MIND_BOMB_EFFECTS =
    listOf(
        Boost(Skills.MAGIC, 2, 2),
        Drain(Skills.ATTACK, 1, 5),
        Drain(Skills.STRENGTH, 1, 5),
        Drain(Skills.DEFENCE, 1, 5),
    )
private val CHEFS_DELIGHT_EFFECTS =
    listOf(Boost(Skills.COOKING, 1, 5), Drain(Skills.ATTACK, 2, 5), Drain(Skills.STRENGTH, 2, 5))
private val SLAYERS_RESPITE_EFFECTS =
    listOf(Boost(Skills.SLAYER, 2, 0), Drain(Skills.ATTACK, 2, 2), Drain(Skills.STRENGTH, 2, 2))
private val AXEMANS_FOLLY_EFFECTS =
    listOf(Boost(Skills.WOODCUTTING, 1, 0), Drain(Skills.ATTACK, 3, 0), Drain(Skills.STRENGTH, 3, 0))
private val CIDER_EFFECTS =
    listOf(Boost(Skills.FARMING, 1, 0), Drain(Skills.ATTACK, 2, 0), Drain(Skills.STRENGTH, 2, 0))

/*
 * The Keldagrim mature ales. Each was sourced from its own page rather than scaled off the ale it
 * matured from: the mature slayer's respite doubles its boost and *drops* the percentage out of its
 * drain, while the mature mind bomb raises both, so no single rule fits the family.
 */
private val MATURE_ASGARNIAN_ALE_EFFECTS = listOf(Boost(Skills.STRENGTH, 3, 0), Drain(Skills.ATTACK, 6, 0))
private val MATURE_DRAGON_BITTER_EFFECTS = MATURE_ASGARNIAN_ALE_EFFECTS
private val MATURE_DWARVEN_STOUT_EFFECTS =
    listOf(
        Boost(Skills.MINING, 2, 0),
        Boost(Skills.SMITHING, 2, 0),
        Drain(Skills.ATTACK, 3, 4),
        Drain(Skills.STRENGTH, 3, 4),
        Drain(Skills.DEFENCE, 3, 4),
    )
private val MATURE_GREENMANS_ALE_EFFECTS =
    listOf(
        Boost(Skills.HERBLORE, 2, 0),
        Drain(Skills.ATTACK, 2, 0),
        Drain(Skills.STRENGTH, 2, 0),
        Drain(Skills.DEFENCE, 2, 0),
    )
private val MATURE_MIND_BOMB_EFFECTS =
    listOf(
        Boost(Skills.MAGIC, 3, 2),
        Drain(Skills.ATTACK, 2, 5),
        Drain(Skills.STRENGTH, 2, 5),
        Drain(Skills.DEFENCE, 2, 5),
    )
private val MATURE_CHEFS_DELIGHT_EFFECTS =
    listOf(Boost(Skills.COOKING, 2, 5), Drain(Skills.ATTACK, 3, 5), Drain(Skills.STRENGTH, 3, 5))
private val MATURE_SLAYERS_RESPITE_EFFECTS =
    listOf(Boost(Skills.SLAYER, 4, 0), Drain(Skills.ATTACK, 2, 0), Drain(Skills.STRENGTH, 2, 0))
private val MATURE_AXEMANS_FOLLY_EFFECTS =
    listOf(Boost(Skills.WOODCUTTING, 2, 0), Drain(Skills.ATTACK, 4, 0), Drain(Skills.STRENGTH, 4, 0))
private val MATURE_CIDER_EFFECTS =
    listOf(Boost(Skills.FARMING, 2, 0), Drain(Skills.ATTACK, 5, 0), Drain(Skills.STRENGTH, 5, 0))

/*
 * Tea. The house cups add a Construction level on top; nettle tea trades the Attack boost for run
 * energy.
 */
private val TEA_EFFECTS = listOf(Boost(Skills.ATTACK, 2, 2))
private val HOUSE_TEA_EFFECTS = TEA_EFFECTS + Boost(Skills.CONSTRUCTION, 1, 0)
private val NETTLE_TEA_EFFECTS = listOf(RestoreEnergy(5))

/*
 * The gnome cocktails that do more than heal. All of them drain Attack the same way; only the
 * Strength boost separates them.
 */
private val COCKTAIL_ATTACK_DRAIN = Drain(Skills.ATTACK, 3, 2)
private val STRONG_COCKTAIL_EFFECTS = listOf(Boost(Skills.STRENGTH, 2, 5), COCKTAIL_ATTACK_DRAIN)
private val SHORT_GREEN_GUY_EFFECTS = listOf(Boost(Skills.STRENGTH, 1, 5), COCKTAIL_ATTACK_DRAIN)
private val WIZARD_BLIZZARD_EFFECTS = listOf(Boost(Skills.STRENGTH, 1, 6), COCKTAIL_ATTACK_DRAIN)

/*
 * The one-off drinks.
 */
private val RUM_EFFECTS = listOf(Boost(Skills.STRENGTH, 1, 5), Drain(Skills.ATTACK, 3, 2))
private val BLOOD_PINT_EFFECTS =
    listOf(
        Boost(Skills.ATTACK, 2, 4),
        Boost(Skills.STRENGTH, 2, 4),
        Drain(Skills.MAGIC, 2, 4),
        Drain(Skills.PRAYER, 2, 4),
    )
private val LIZARDKICKER_EFFECTS =
    listOf(Boost(Skills.RANGED, 4, 0), Drain(Skills.STRENGTH, 2, 4), Drain(Skills.MAGIC, 2, 4))
private val KOVACS_GROG_EFFECTS =
    listOf(
        Boost(Skills.SMITHING, 4, 0),
        Drain(Skills.ATTACK, 2, 0),
        Drain(Skills.RANGED, 2, 0),
        Drain(Skills.MAGIC, 2, 0),
    )
private val TRAPPERS_TIPPLE_EFFECTS =
    listOf(Boost(Skills.HUNTER, 2, 0), Drain(Skills.ATTACK, 2, 0), Drain(Skills.STRENGTH, 1, 0))
private val SUNBEAM_ALE_EFFECTS =
    listOf(Boost(Skills.AGILITY, 1, 0), Boost(Skills.STRENGTH, 1, 0), Drain(Skills.ATTACK, 1, 5))
private val STEAMFORGE_BREW_EFFECTS =
    listOf(
        Boost(Skills.MINING, 1, 0),
        Boost(Skills.MAGIC, 1, 0),
        Drain(Skills.ATTACK, 2, 5),
        Drain(Skills.DEFENCE, 2, 5),
    )

/**
 * A Varlamore wine: a level in one skill, five Attack and a level in another gone.
 */
private fun wine(
    boosted: Int,
    drained: Int,
): List<ConsumableEffect> =
    listOf(Boost(boosted, 1, 0), Drain(Skills.ATTACK, 5, 0), Drain(drained, 1, 0))

/**
 * The half of a Varlamore wine that holds for all of them, for the three whose boosted skill the
 * wiki has never published.
 */
private val UNDOCUMENTED_WINE_EFFECTS = listOf(Drain(Skills.ATTACK, 5, 0))

/**
 * The poison chalice's seven results, in the order the wiki lists them and each as likely as the
 * rest.
 */
private val POISON_CHALICE_OUTCOMES =
    listOf(
        Outcome("You feel a lot better.", listOf(HealPercent(15), Boost(Skills.THIEVING, 1, 0))),
        Outcome(
            "You feel a little strange.",
            listOf(
                Drain(Skills.ATTACK, 1, 0),
                Drain(Skills.STRENGTH, 1, 0),
                Drain(Skills.DEFENCE, 1, 0),
                Boost(Skills.CRAFTING, 1, 0),
            ),
        ),
        Outcome("It has a slight taste of apricot."),
        Outcome("It heals some health.", listOf(HealPercent(5))),
        Outcome("That tasted a bit dodgy. You feel a bit ill.", listOf(DamagePercent(5))),
        Outcome(
            "That tasted very dodgy. You feel very ill.",
            listOf(
                DamagePercent(50),
                Drain(Skills.ATTACK, 3, 0),
                Drain(Skills.STRENGTH, 3, 0),
                Drain(Skills.DEFENCE, 3, 0),
            ),
        ),
        Outcome(
            "Wow! That was amazing! You feel really invigorated.",
            listOf(
                HealPercent(30),
                Boost(Skills.ATTACK, 4, 0),
                Boost(Skills.STRENGTH, 4, 0),
                Boost(Skills.DEFENCE, 4, 0),
            ),
        ),
    )
