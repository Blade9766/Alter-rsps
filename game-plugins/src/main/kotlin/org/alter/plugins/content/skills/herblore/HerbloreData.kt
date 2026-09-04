package org.alter.plugins.content.skills.herblore

import org.alter.rscm.RSCM.getRSCM

/**
 * One herb, loaded from `data/cfg/herblore/herbs.json`: the grimy item it is picked as, the
 * clean item a "Clean" turns it into, and - for the fifteen herbs that have one - the
 * unfinished potion it makes in a vial of water.
 *
 * [unfinished] is null for the five Jungle Potion herbs (snake weed, ardrigal, sito foil,
 * volencia moss and rogue's purse). They are cleaned like any other herb but the cache has
 * no "<herb> potion (unf)" item for them, because in OSRS they aren't a potion base:
 * Relicym's balm mixes rogue's purse straight into a vial of water in one step.
 *
 * As elsewhere in this codebase, Gson allocates these without running the constructor, so
 * neither Kotlin defaults nor an `init` block would ever apply - a field missing from the
 * JSON silently arrives as 0/0.0. [HerbloreService] validates every field explicitly.
 */
data class HerbEntry(
    /** The clean herb's name, used in chat messages. */
    val name: String,
    val grimy: String,
    val clean: String,
    /** Herblore level needed to clean this herb. Boostable in OSRS, so checked against the current level. */
    val cleanLevel: Int,
    val cleanExperience: Double,
    /** The unfinished potion a vial of water makes, or null for the herbs that have none. */
    val unfinished: String? = null,
    /** Herblore level needed to make [unfinished]; ignored when there is no unfinished potion. */
    val unfinishedLevel: Int = 0,
) {
    @Transient
    var grimyItemId: Int = -1

    @Transient
    var cleanItemId: Int = -1

    @Transient
    var unfinishedItemId: Int = -1
}

/**
 * One mixing recipe, loaded from `data/cfg/herblore/potions.json`.
 *
 * A recipe binds a **base** and a **secondary** as an item-on-item pair. For most potions
 * the base is an unfinished potion and the secondary is the ingredient that finishes it -
 * ranarr potion (unf) plus snape grass makes a prayer potion. The same shape also covers
 * the steps that produce an unfinished potion in the first place where the base isn't a
 * vial of water: coconut milk plus toadflax makes antidote+ (unf), a vial of blood plus
 * cadantine makes cadantine blood potion (unf). Those carry `experience: 0.0`, which is
 * what OSRS pays for them.
 *
 * ### Three ways to name the base and the product
 *
 * [base]/[product] name one item each. That is the common case, and also how the recipes
 * that demand a specific dose are written - a super combat potion really does want three
 * *4-dose* supers, so its base is `item.super_attack4`.
 *
 * [baseFamily]/[productFamily] name a dose family, expanding to `<family>1` through
 * `<family>4`. This is for the potions OSRS lets you top up at any dose: an amylase
 * crystal per dose turns super energy into stamina, a lava scale shard per dose extends an
 * antifire, five of Zulrah's scales per dose turn antidote++ into anti-venom. Each dose
 * binds separately, and both the secondary count and the experience scale with it - which
 * is why [experience] is *per dose* for these and per potion for everything else.
 *
 * [baseDoses]/[productDoses] spell the four doses out in order 1, 2, 3, 4. Only
 * anti-venom's base needs it: the generated item names for antidote++ collide with
 * antidote+, so its doses carry the item id as a suffix and run backwards by id - the same
 * wrinkle [org.alter.plugins.content.items.consumables.potions.Potion] already works
 * around.
 *
 * ### Multi-ingredient recipes
 *
 * Item-on-item binds exactly two items, so a recipe wanting more carries the rest in
 * [extras] and is written out once per ingredient that can start it - three entries for a
 * super combat potion, three for a sanfew serum. Clicking any of them onto the base then
 * works, as it does in OSRS, and the whole ingredient list is consumed either way. This is
 * the same trick [org.alter.plugins.content.skills.cooking.RecipeEntry] uses for a cake.
 */
data class PotionRecipe(
    /** The product's name, for the "what would you like to make?" chatbox and messages. */
    val name: String,
    /** The item the secondary is used on. Exactly one of this, [baseFamily] or [baseDoses]. */
    val base: String? = null,
    /** A dose family for a per-dose recipe, expanded to `<family>1`..`<family>4`. */
    val baseFamily: String? = null,
    /** The four doses of the base, in order 1, 2, 3, 4, when their names don't share a prefix. */
    val baseDoses: List<String>? = null,
    /** The ingredient used on the base. Half of the bound pair. */
    val secondary: String,
    /** How many of [secondary] a whole potion costs. Zero (the JSON default) means one. */
    val secondaryAmount: Int = 0,
    /**
     * How many of [secondary] each dose costs, for a per-dose recipe: one amylase crystal,
     * one lava scale shard, five of Zulrah's scales, twenty ancient essence. Zero means
     * this isn't a per-dose recipe.
     */
    val secondaryPerDose: Int = 0,
    /** Further items consumed but not bound - the other two supers in a super combat potion. */
    val extras: List<String>? = null,
    val product: String? = null,
    val productFamily: String? = null,
    val productDoses: List<String>? = null,
    /** Herblore level needed. Boostable in OSRS, so checked against the current level. */
    val level: Int,
    /** Experience per potion, or **per dose** for a per-dose recipe. */
    val experience: Double,
) {
    /** Every concrete base/product pairing this recipe expands to; one entry unless it is per-dose. */
    @Transient
    var mixes: List<PotionMix> = emptyList()

    /** True when [baseFamily] or [baseDoses] was used, i.e. the recipe works at any dose. */
    val perDose: Boolean
        get() = baseFamily != null || baseDoses != null
}

/**
 * One concrete "use A on B to get C" that the engine can bind, after a [PotionRecipe] has
 * been expanded over its doses.
 *
 * Unfinished potions made from a vial of water don't come from `potions.json` at all -
 * [HerbloreService] builds one of these per herb straight out of `herbs.json` - so this,
 * rather than the recipe, is what [HerblorePlugin] actually works from.
 */
class PotionMix(
    val name: String,
    val baseId: Int,
    val secondaryId: Int,
    /** How many of [secondaryId] one product costs. */
    val secondaryAmount: Int,
    /** Further items consumed, one each, beyond the bound pair. */
    val extraIds: IntArray,
    val productId: Int,
    val level: Int,
    val experience: Double,
    /**
     * Ticks one mix takes. The wiki's recipe blocks give 1 for an unfinished potion and 2
     * for a finished one, and that gap is real: an inventory of unfinished potions is
     * noticeably quicker to make than the potions themselves.
     */
    val ticks: Int,
) {
    /** The unordered pair the engine binds this mix to. */
    val pair: Pair<Int, Int>
        get() = minOf(baseId, secondaryId) to maxOf(baseId, secondaryId)
}

/**
 * One pestle-and-mortar grinding, loaded from `data/cfg/herblore/grinding.json`.
 *
 * None of these need a Herblore level or pay any experience - grinding is a preparation
 * step, and the level is charged on the potion the dust ends up in. [minAmount] and
 * [maxAmount] cover the two that yield more than one: a crystal shard makes ten crystal
 * dust, and a lava scale makes between three and six shards.
 */
data class GrindEntry(
    val name: String,
    val input: String,
    val product: String,
    /** Zero (the JSON default) means one, which is every entry but crystal dust and lava scales. */
    val minAmount: Int = 0,
    val maxAmount: Int = 0,
) {
    @Transient
    var inputItemId: Int = -1

    @Transient
    var productItemId: Int = -1

    @Transient
    var low: Int = 1

    @Transient
    var high: Int = 1
}

/** Resolves an `item.` RSCM name, or -1 when it is null. */
internal fun String?.toItemId(): Int = this?.let { getRSCM(it) } ?: -1
