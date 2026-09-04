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
    /**
     * An extra item handed back when this is cooked, on top of the food itself. Only the
     * cake tin: the wiki notes that "once baked, the cake tin will separate from the
     * cooked cake, unlike the pie dish, where it comes with the pie". Null everywhere else.
     */
    val returns: String? = null,
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
     * The mirror of [rangeOnly]: food only a fire can cook. Just the four spit-roasts,
     * whose wiki recipe blocks all read `facilities = Fire`. Setting both this and
     * [rangeOnly] would make a food uncookable, which [CookingService] rejects.
     */
    val fireOnly: Boolean = false,
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

    @Transient
    var returnItemId: Int = -1
}

/**
 * One item-on-item combination on the way to something cookable, loaded from
 * `data/cfg/cooking/recipes.json`: flour and water into dough, dough into a pie shell, a
 * filling into a shell, a bowl of water into a stew, three ingredients into a cake tin.
 *
 * [ingredients] holds everything consumed, and its **first two entries are the pair the
 * engine binds** - item-on-item takes exactly two items, so a cake, which wants a tin, an
 * egg, a bucket of milk and a pot of flour, is expressed as three recipes that each pair
 * the tin with a different one of the three and carry the rest behind it. Any of the three
 * clicked onto the tin then works, which is how OSRS behaves.
 *
 * Most of these have no requirement and give nothing: [level] 0 and [experience] 0.0 are
 * both the JSON default and the right answer for dough, shells and fillings, where the
 * gate is on baking the result rather than assembling it. The exceptions are real Cooking
 * actions that happen off the heat - a pizza base needs 35, and topping a cooked pizza or
 * chocolating a cake both need a level and pay out.
 *
 * As with [FoodEntry], Gson allocates these without running the constructor, so
 * [CookingService] validates rather than relying on defaults; [returns] is nullable for
 * that reason, since a missing JSON list arrives as null rather than as an empty one.
 */
data class RecipeEntry(
    /** The product's name, for the "what would you like to make?" chatbox. */
    val name: String,
    /** Everything consumed. The first two are the pair bound as item-on-item. */
    val ingredients: List<String>,
    /**
     * How many of each entry in [ingredients] is consumed, positionally aligned with it.
     * Null - the common case - means one of each. Only gnome cooking needs this: a
     * chocolate bomb wants four chocolate bars, a worm hole four king worms.
     *
     * Kept as a parallel list rather than folding an amount into [ingredients] so that the
     * eighty-odd recipes that consume one of everything stay readable, and so the whole
     * config did not have to be reshaped to add four dishes' worth of quantities.
     */
    val amounts: List<Int>? = null,
    val product: String,
    /** Items handed back, e.g. the emptied pot and bucket a dough leaves. */
    val returns: List<String>? = null,
    /**
     * Items that must be held but are not consumed - only ever a knife, which is what
     * chops an onion into a bowl or minces cooked meat. The wiki lists these as `tools`
     * rather than `mat`s in a recipe block, and that distinction is the whole point: a
     * knife consumed per chop would be absurd.
     */
    val tools: List<String>? = null,
    /**
     * The two items the engine binds this recipe to, when they aren't the first two
     * [ingredients]. Slicing a lemon needs it: the only items involved are the knife and
     * the fruit, and the knife is a tool rather than something consumed, so there is no
     * second ingredient to bind against.
     */
    val bind: List<String>? = null,
    /** Cooking level needed; 0 for the assembly steps that have no requirement. */
    val level: Int = 0,
    /**
     * What a failed attempt produces instead of [product]. Only the ugthanki kebab, which
     * the wiki says succeeds about 46% of the time at level 1 and always from
     * [neverFailsLevel]. Null means the recipe cannot fail.
     */
    val failProduct: String? = null,
    /** The level at which [failProduct] stops happening; 0 when the recipe can't fail. */
    val neverFailsLevel: Int = 0,
    /** Cooking experience paid out; 0.0 for the steps that give none. */
    val experience: Double = 0.0,
    val message: String,
) {
    @Transient
    var ingredientIds: IntArray = intArrayOf()

    @Transient
    var ingredientAmounts: IntArray = intArrayOf()

    @Transient
    var productItemId: Int = -1

    @Transient
    var returnIds: IntArray = intArrayOf()

    @Transient
    var toolIds: IntArray = intArrayOf()

    @Transient
    var failProductId: Int = -1

    @Transient
    var bindIds: IntArray = intArrayOf()

    /** The two items the engine binds this recipe to. */
    val boundPair: Pair<Int, Int>
        get() = if (bindIds.isNotEmpty()) bindIds[0] to bindIds[1] else ingredientIds[0] to ingredientIds[1]
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
