package org.alter.plugins.content.skills.herblore

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import gg.rsmod.util.ServerProperties
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.alter.api.ext.appendToString
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.service.Service
import org.alter.rscm.RSCM.getRSCM
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Loads the three Herblore tables - herbs, mixing recipes and pestle-and-mortar grindings -
 * and expands them into the flat list of concrete item-on-item mixes [HerblorePlugin] binds.
 *
 * Validation is deliberately loud. `PluginRepository.bindItemOnItem` throws when a pair is
 * bound twice, and a plugin whose constructor throws registers *nothing at all* - so a
 * single duplicate pair in the config would silently take the whole skill offline. The
 * checks here fail with the offending recipe named instead.
 */
class HerbloreService : Service {
    private val gson = Gson()

    val herbs: ObjectArrayList<HerbEntry> = ObjectArrayList()

    val recipes: ObjectArrayList<PotionRecipe> = ObjectArrayList()

    val grindings: ObjectArrayList<GrindEntry> = ObjectArrayList()

    /** Every mix, including the vial-of-water unfinished potions built out of [herbs]. */
    val mixes: ObjectArrayList<PotionMix> = ObjectArrayList()

    private val herbsByGrimy: Int2ObjectOpenHashMap<HerbEntry> = Int2ObjectOpenHashMap()

    override fun init(
        server: Server,
        world: World,
        serviceProperties: ServerProperties,
    ) {
        val herbFile = Paths.get(serviceProperties.get("herblore_herbs") ?: "../data/cfg/herblore/herbs.json")
        val potionFile = Paths.get(serviceProperties.get("herblore_potions") ?: "../data/cfg/herblore/potions.json")
        val grindFile = Paths.get(serviceProperties.get("herblore_grinding") ?: "../data/cfg/herblore/grinding.json")

        Files.newBufferedReader(herbFile).use { reader ->
            val listType = object : TypeToken<List<HerbEntry>>() {}.type
            herbs.addAll(gson.fromJson<List<HerbEntry>>(reader, listType))
        }

        Files.newBufferedReader(potionFile).use { reader ->
            val listType = object : TypeToken<List<PotionRecipe>>() {}.type
            recipes.addAll(gson.fromJson<List<PotionRecipe>>(reader, listType))
        }

        Files.newBufferedReader(grindFile).use { reader ->
            val listType = object : TypeToken<List<GrindEntry>>() {}.type
            grindings.addAll(gson.fromJson<List<GrindEntry>>(reader, listType))
        }

        val vialOfWater = getRSCM(VIAL_OF_WATER)

        herbs.forEach { herb ->
            check(herb.cleanLevel in 1..99) { "Herb '${herb.grimy}' has an invalid clean level: ${herb.cleanLevel}." }
            check(herb.cleanExperience > 0.0) { "Herb '${herb.grimy}' pays no experience for cleaning." }

            herb.grimyItemId = getRSCM(herb.grimy)
            herb.cleanItemId = getRSCM(herb.clean)
            herb.unfinishedItemId = herb.unfinished.toItemId()

            check(herbsByGrimy.put(herb.grimyItemId, herb) == null) {
                "Two herb entries share the grimy item '${herb.grimy}'."
            }

            if (herb.unfinishedItemId == -1) {
                return@forEach
            }
            // The unfinished-potion level is never below the cleaning level in OSRS, and a
            // herb you could brew but not clean would be a table transcription error.
            check(herb.unfinishedLevel in herb.cleanLevel..99) {
                "Herb '${herb.grimy}' makes an unfinished potion at level ${herb.unfinishedLevel}; " +
                    "expected a level between ${herb.cleanLevel} and 99."
            }

            mixes +=
                PotionMix(
                    name = "${herb.name} potion (unf)",
                    baseId = vialOfWater,
                    secondaryId = herb.cleanItemId,
                    secondaryAmount = 1,
                    extraIds = intArrayOf(),
                    productId = herb.unfinishedItemId,
                    level = herb.unfinishedLevel,
                    // Unfinished potions pay nothing in OSRS; all of a potion's experience
                    // arrives when the secondary goes in.
                    experience = 0.0,
                    ticks = UNFINISHED_TICKS,
                )
        }

        recipes.forEach { recipe ->
            check(recipe.level in 1..99) { "Recipe '${recipe.name}' has an invalid level: ${recipe.level}." }
            check(recipe.experience >= 0.0) { "Recipe '${recipe.name}' has negative experience." }
            check(recipe.secondaryAmount >= 0 && recipe.secondaryPerDose >= 0) {
                "Recipe '${recipe.name}' has a negative secondary amount."
            }
            check(!(recipe.secondaryAmount > 0 && recipe.secondaryPerDose > 0)) {
                "Recipe '${recipe.name}' sets both a flat and a per-dose secondary amount."
            }

            val baseNames = recipe.doseNames(recipe.base, recipe.baseFamily, recipe.baseDoses, "base")
            val productNames = recipe.doseNames(recipe.product, recipe.productFamily, recipe.productDoses, "product")
            check(baseNames.size == productNames.size) {
                "Recipe '${recipe.name}' has ${baseNames.size} bases for ${productNames.size} products."
            }
            check(recipe.perDose == (baseNames.size == DOSES)) {
                "Recipe '${recipe.name}' mixes a per-dose base with a single-item product, or the reverse."
            }
            check(!recipe.perDose || recipe.secondaryPerDose > 0) {
                "Recipe '${recipe.name}' works at any dose but doesn't say how much secondary a dose costs."
            }

            val secondaryId = getRSCM(recipe.secondary)
            val extraIds = recipe.extras?.map { getRSCM(it) }?.toIntArray() ?: intArrayOf()
            check(secondaryId !in extraIds) { "Recipe '${recipe.name}' lists its secondary as an extra as well." }

            recipe.mixes =
                baseNames.indices.map { index ->
                    // Doses run 1..4, so a per-dose recipe's cost and payout both scale with
                    // the dose: a 4-dose stamina potion eats four amylase crystals and pays
                    // four times 25.5, exactly the 102 the wiki quotes.
                    val dose = index + 1
                    val baseId = getRSCM(baseNames[index])
                    val productId = getRSCM(productNames[index])
                    check(baseId != secondaryId) { "Recipe '${recipe.name}' uses its base as its own secondary." }
                    check(baseId !in extraIds) { "Recipe '${recipe.name}' lists its base as an extra as well." }

                    PotionMix(
                        name = recipe.name,
                        baseId = baseId,
                        secondaryId = secondaryId,
                        secondaryAmount =
                            if (recipe.perDose) {
                                recipe.secondaryPerDose * dose
                            } else {
                                maxOf(1, recipe.secondaryAmount)
                            },
                        extraIds = extraIds,
                        productId = productId,
                        level = recipe.level,
                        experience = if (recipe.perDose) recipe.experience * dose else recipe.experience,
                        ticks = MIX_TICKS,
                    )
                }

            mixes.addAll(recipe.mixes)
        }

        grindings.forEach { grind ->
            grind.inputItemId = getRSCM(grind.input)
            grind.productItemId = getRSCM(grind.product)
            check(grind.inputItemId != grind.productItemId) {
                "Grinding '${grind.name}' turns an item into itself."
            }
            grind.low = if (grind.minAmount == 0) 1 else grind.minAmount
            grind.high = if (grind.maxAmount == 0) grind.low else grind.maxAmount
            check(grind.low in 1..grind.high) {
                "Grinding '${grind.name}' yields ${grind.low}..${grind.high}, which is not a sane range."
            }
        }

        check(grindings.map { it.inputItemId }.distinct().size == grindings.size) {
            "Two grindings share the same input item."
        }

        // A pair with more than one mix behind it becomes a choice in the chatbox rather
        // than an error - the same way flour and water offers four doughs in Cooking. What
        // must not happen is two mixes behind one pair making the same thing, since one
        // would silently shadow the other, nor a pair that would make the engine's
        // bindItemOnItem throw and take the whole plugin down with it.
        mixesByPair().forEach { (pair, group) ->
            val products = group.map { it.productId }
            check(products.distinct().size == products.size) {
                "Mixes sharing the pair $pair make the same product twice: ${group.map { it.name }}"
            }
        }

        Server.logger.info {
            "Loaded ${herbs.size.appendToString("herb")}, ${mixes.size.appendToString("Herblore mixture")} and " +
                "${grindings.size.appendToString("grinding")}."
        }
    }

    fun herb(grimyItemId: Int): HerbEntry? = herbsByGrimy[grimyItemId]

    /** Mixes grouped by the unordered item pair that starts them. */
    fun mixesByPair(): Map<Pair<Int, Int>, List<PotionMix>> = mixes.groupBy { it.pair }

    /**
     * The one, or four, item names a recipe's base or product resolves to, whichever of the
     * three ways of naming it was used.
     */
    private fun PotionRecipe.doseNames(
        single: String?,
        family: String?,
        explicit: List<String>?,
        role: String,
    ): List<String> {
        val given = listOfNotNull(single, family, explicit).size
        check(given == 1) { "Recipe '$name' names its $role $given ways; expected exactly one." }
        explicit?.let {
            check(it.size == DOSES) { "Recipe '$name' lists ${it.size} $role doses; expected $DOSES." }
            return it
        }
        family?.let { prefix -> return (1..DOSES).map { "$prefix$it" } }
        return listOf(single!!)
    }

    private companion object {
        const val VIAL_OF_WATER = "item.vial_of_water"

        /** Potions come in one, two, three and four doses. */
        const val DOSES = 4

        /**
         * A herb dropped into a vial of water takes one tick, per that unfinished potion's
         * own wiki recipe block; everything else takes two. The one place that rounds up is
         * cadantine blood potion (unf), whose block also says one - not worth a per-recipe
         * field for a tick.
         */
        const val UNFINISHED_TICKS = 1
        const val MIX_TICKS = 2
    }
}
