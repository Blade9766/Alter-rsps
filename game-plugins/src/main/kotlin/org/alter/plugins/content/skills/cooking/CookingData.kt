package org.alter.plugins.content.skills.cooking

/**
 * One cookable raw item, loaded from `data/cfg/cooking/food.json`.
 *
 * The four `*Level` fields are **stop-burning** levels, not requirements: the Cooking
 * level at which that heat source stops burning this food entirely. `-1` means "never
 * stops burning", which the OSRS Wiki's burn-level table writes as a dash - sharks,
 * sea turtles, dark crabs, manta rays and anglerfish all still burn at 99 without
 * cooking gauntlets or a Cooking cape.
 *
 * The wiki leaves the normal-range column blank ({{NA}}) for most foods. That does not
 * mean a range can't cook them - it means the range's stop-burning level is the same as
 * a fire's, while the burn *chance* below it is still lower. Those entries therefore
 * carry `rangeLevel == fireLevel`, and the lower chance is applied separately by
 * [CookingPlugin]'s per-source multiplier. Only cod, swordfish and monkfish actually
 * stop burning earlier on a range than on a fire.
 *
 * Gson allocates these without running the constructor, so neither Kotlin default values
 * nor an `init` block would ever apply - a field left out of the JSON silently arrives as
 * 0/0.0. [CookingService] validates every field explicitly for that reason.
 */
data class FoodEntry(
    /** The cooked item's name, used in chat messages. */
    val name: String,
    val raw: String,
    val cooked: String,
    val burnt: String,
    /** Cooking level needed to cook this at all. */
    val level: Int,
    val experience: Double,
    /**
     * True for food a fire can't cook at all - bread, pitta bread and every pie. The
     * wiki writes these as a blank Fire column and their item pages list "Facilities:
     * Cooking range". False is both the JSON default and the right answer for every fish
     * and every cut of meat, so only the range-only entries spell it out; [CookingVerify]
     * pins down exactly which those are.
     */
    val rangeOnly: Boolean,
    /**
     * Stop-burning level on an open fire; -1 = never. Meaningless when [rangeOnly] is
     * set, where it simply mirrors [rangeLevel].
     */
    val fireLevel: Int,
    /** Stop-burning level on a normal range; -1 = never. */
    val rangeLevel: Int,
    /** Stop-burning level on the Lumbridge Castle range; -1 = never. */
    val castleLevel: Int,
    /**
     * Stop-burning level while wearing cooking gauntlets; -1 = never. For the foods
     * gauntlets don't help (everything but lobster, swordfish, monkfish, shark and
     * anglerfish) this is simply set to [fireLevel], so taking the better of the two
     * numbers leaves those foods unchanged.
     */
    val gauntletLevel: Int,
) {
    @Transient
    var rawItemId: Int = -1

    @Transient
    var cookedItemId: Int = -1

    @Transient
    var burntItemId: Int = -1
}

/**
 * One item-on-item combination on the way to something cookable, loaded from
 * `data/cfg/cooking/recipes.json`: flour and water into dough, dough into a pie shell, a
 * filling into a shell, a potato into a bowl of water.
 *
 * None of these grant Cooking experience or carry a level requirement - in OSRS the gate
 * is on baking the result, not on assembling it, which is why a level 1 player can build
 * a raw summer pie they have no hope of cooking.
 *
 * [primary] and [secondary] are only labels for readability: the engine's item-on-item
 * binding is symmetric, so either may be the one clicked. Where several recipes share the
 * same pair - the three doughs - [CookingRecipePlugin] offers them in the game's own
 * skill-multi chatbox instead of picking one.
 *
 * As with [FoodEntry], Gson allocates these without running the constructor, so
 * [CookingService] validates rather than relying on defaults.
 */
data class RecipeEntry(
    /** The product's name, for the "what would you like to make?" chatbox. */
    val name: String,
    val primary: String,
    val secondary: String,
    val product: String,
    /** What [primary] leaves behind, e.g. a pot of flour leaving an empty pot. */
    val primaryReplacement: String? = null,
    /** What [secondary] leaves behind, e.g. a bucket of water leaving a bucket. */
    val secondaryReplacement: String? = null,
    val message: String,
) {
    @Transient
    var primaryItemId: Int = -1

    @Transient
    var secondaryItemId: Int = -1

    @Transient
    var productItemId: Int = -1

    @Transient
    var primaryReplacementId: Int = -1

    @Transient
    var secondaryReplacementId: Int = -1
}

/** Where the food is being cooked, which decides both stop-level and burn chance. */
enum class HeatSource {
    /** An open fire - the worst option, and the only one a player can make themselves. */
    FIRE,

    /** Any object carrying a real "Cook" action: ranges, ovens, stoves, clay ovens. */
    RANGE,

    /**
     * The Lumbridge Castle range (object 114, the "Cook-o-matic 100"), which the wiki
     * describes as having a "slightly lower chance to burn than both open fire and
     * standard ranges".
     */
    CASTLE_RANGE,
}
