package org.alter.plugins.content.skills.cooking

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * Gnome cocktails: the seven drinks from Blurberry's bar.
 *
 * These do not go through [CookingRecipePlugin], because they are not item-on-item at all.
 * The cache spells the real interface out: a cocktail shaker carries a **Mix-cocktail**
 * inventory option, every mixed drink carries **Pour**, the two that need garnishing after
 * the glass carry **Add-ingreds**, and the finished drinks carry **Drink**. This plugin
 * follows that, so the flow is the one the client already expects.
 *
 * **The shape of a drink:**
 * - Mix-cocktail on the shaker picks one of seven from the game's own skill-multi chatbox
 *   and consumes the fruit or spirits, paying 30 experience. The shaker is a tool and is
 *   kept.
 * - Pour needs a cocktail glass. For five of the drinks it also takes the garnishes and
 *   produces the finished cocktail in one step, which is exactly how the wiki's recipe
 *   blocks read - a fruit blast is "mixed blast + lemon slices + cocktail glass".
 * - Drunk dragon and choc saturday are the awkward pair. Both pour into a plain glass
 *   first, then one is garnished and heated while the other is heated and then garnished.
 *   Their heating steps are ordinary range cooks and live in `food.json` alongside every
 *   other cook.
 *
 * **Experience** splits 30 for the mix and the remainder on the finishing step, which is
 * what makes the wiki's totals add up: a fruit blast is 30 + 20 = 50, a blurberry special
 * 30 + 150 = 180. Both halves are the wiki's own recipe-block figures, not a split I chose.
 *
 * **Cutting the garnishes** - slicing and dicing lemons, limes, oranges and pineapples -
 * is ordinary knife work and lives in `recipes.json` with the rest of the chopping.
 *
 * The legacy `unfinished_cocktail` items at ids 2042-2090 are **not** used: the wiki's own
 * disambiguation page lists exactly the eleven live intermediates, which are 9566-9576, and
 * those are what the cache gives Pour and Add-ingreds options to.
 */
class CocktailPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    /** One ingredient and how many of it. */
    private data class Part(val item: String, val amount: Int = 1) {
        val id: Int by lazy { getRSCM(item) }
    }

    /**
     * A drink from the shaker to the glass.
     *
     * [pourInto] is what pouring produces. For the five simple drinks that is the finished
     * cocktail and [garnish] goes in at the same time; for the two heated ones it is a
     * plain poured glass and the garnish arrives later, through [addIngreds].
     */
    private data class Cocktail(
        val name: String,
        val level: Int,
        val mixed: String,
        val mix: List<Part>,
        val pourInto: String,
        val garnish: List<Part> = emptyList(),
        val pourExperience: Double = 0.0,
        val addIngredsInto: String? = null,
        val addIngreds: List<Part> = emptyList(),
        val addExperience: Double = 0.0,
    ) {
        val mixedId: Int by lazy { getRSCM(mixed) }
        val pourIntoId: Int by lazy { getRSCM(pourInto) }
        val addIngredsIntoId: Int by lazy { getRSCM(addIngredsInto!!) }
    }

    init {
        onWorldInit {
            onItemOption(item = SHAKER, option = MIX_OPTION) {
                player.queue(TaskPriority.STANDARD) { mixMenu(this, player) }
            }

            DRINKS.forEach { drink ->
                onItemOption(item = drink.mixed, option = POUR_OPTION) {
                    pour(player, drink)
                }
                // Only the poured dragon and the *heated* saturday carry this option - so the
                // saturday is skipped here and bound below on the heated glass instead. Without
                // that exclusion this bound Add-ingreds on `mixed_saturday_9572`, whose only cache
                // action is "Drop", and `onItemOption`'s check threw - taking down not just this
                // plugin but every `onWorldInit` block registered after it, since one thrown
                // exception aborts `PluginRepository.executeWorldInit`'s whole loop.
                if (drink.addIngredsInto != null && drink.mixed != MIXED_SATURDAY) {
                    onItemOption(item = drink.pourInto, option = ADD_OPTION) {
                        addIngredients(player, drink, drink.pourIntoId, drink.addIngredsIntoId)
                    }
                }
            }

            // Choc saturday garnishes the *heated* glass rather than the poured one, so its
            // Add-ingreds sits on a different item than the one Pour produced.
            val saturday = DRINKS.first { it.mixed == MIXED_SATURDAY }
            onItemOption(item = HEATED_SATURDAY, option = ADD_OPTION) {
                addIngredients(player, saturday, getRSCM(HEATED_SATURDAY), getRSCM(saturday.addIngredsInto!!))
            }
        }
    }

    private suspend fun mixMenu(
        task: QueueTask,
        player: Player,
    ) {
        var chosen: Cocktail? = null
        var requested = 0

        task.produceItemBox(
            player,
            *DRINKS.map { it.mixedId }.toIntArray(),
            title = "What would you like to make?",
            maxProducable = player.inventory.capacity,
        ) { item, qty ->
            chosen = DRINKS.firstOrNull { it.mixedId == item }
            requested = qty
        }

        val drink = chosen ?: return
        if (requested <= 0) {
            return
        }
        if (player.getSkills().getCurrentLevel(Skills.COOKING) < drink.level) {
            player.message("You need a Cooking level of ${drink.level} to mix that.")
            return
        }

        repeat(requested) {
            if (!consume(player, drink.mix, drink.mixedId, MIX_EXPERIENCE)) {
                return
            }
            player.message("You mix the ingredients in the shaker.")
        }
    }

    private fun pour(
        player: Player,
        drink: Cocktail,
    ) {
        if (player.getSkills().getCurrentLevel(Skills.COOKING) < drink.level) {
            player.message("You need a Cooking level of ${drink.level} to make that.")
            return
        }
        // The glass and any garnish go in alongside the mixed drink itself.
        val needed = listOf(Part(drink.mixed), Part(GLASS)) + drink.garnish
        if (consume(player, needed, drink.pourIntoId, drink.pourExperience)) {
            player.message("You pour the cocktail into the glass.")
        }
    }

    private fun addIngredients(
        player: Player,
        drink: Cocktail,
        fromId: Int,
        intoId: Int,
    ) {
        if (player.getSkills().getCurrentLevel(Skills.COOKING) < drink.level) {
            player.message("You need a Cooking level of ${drink.level} to make that.")
            return
        }
        val needed = listOf(Part(itemOf(fromId))) + drink.addIngreds
        if (consume(player, needed, intoId, drink.addExperience)) {
            player.message("You add the last of the ingredients.")
        }
    }

    /**
     * Takes [parts] and gives one [product], paying [experience].
     *
     * Nothing is removed until everything is present, and anything already taken goes back
     * if a later removal fails - the same care [CookingRecipePlugin] takes, and for the
     * same reason: a half-applied recipe would quietly destroy a player's spirits.
     */
    private fun consume(
        player: Player,
        parts: List<Part>,
        product: Int,
        experience: Double,
    ): Boolean {
        val missing = parts.firstOrNull { player.inventory.getItemCount(it.id) < it.amount }
        if (missing != null) {
            player.message("You need ${missing.amount} ${itemName(missing.id)} to do that.")
            return false
        }

        val removed = mutableListOf<Part>()
        parts.forEach { part ->
            if (player.inventory.remove(item = part.id, amount = part.amount, assureFullRemoval = true).hasFailed()) {
                removed.forEach { player.inventory.add(item = it.id, amount = it.amount) }
                return false
            }
            removed += part
        }

        player.inventory.add(item = product, amount = 1)
        if (experience > 0.0) {
            player.addXp(Skills.COOKING, experience)
        }
        return true
    }

    private fun itemOf(id: Int): String = DRINK_ITEM_KEYS.getValue(id)

    private fun itemName(id: Int): String = dev.openrune.cache.CacheManager.getItem(id).name.lowercase()

    private companion object {
        const val SHAKER = "item.cocktail_shaker"
        const val GLASS = "item.cocktail_glass"
        const val MIX_OPTION = "mix-cocktail"
        const val POUR_OPTION = "pour"
        const val ADD_OPTION = "add-ingreds"

        /** Every mix pays the same, with the rest arriving when the drink is finished. */
        const val MIX_EXPERIENCE = 30.0

        const val MIXED_SATURDAY = "item.mixed_saturday"
        const val HEATED_SATURDAY = "item.mixed_saturday_9573"

        val DRINKS =
            listOf(
                Cocktail(
                    name = "Fruit blast",
                    level = 6,
                    mixed = "item.mixed_blast",
                    mix = listOf(Part("item.pineapple"), Part("item.lemon"), Part("item.orange")),
                    pourInto = "item.fruit_blast",
                    garnish = listOf(Part("item.lemon_slices")),
                    pourExperience = 20.0,
                ),
                Cocktail(
                    name = "Pineapple punch",
                    level = 8,
                    mixed = "item.mixed_punch",
                    mix = listOf(Part("item.pineapple", 2), Part("item.lemon"), Part("item.orange")),
                    pourInto = "item.pineapple_punch",
                    garnish =
                        listOf(
                            Part("item.lime_chunks"),
                            Part("item.pineapple_chunks"),
                            Part("item.orange_slices"),
                        ),
                    pourExperience = 40.0,
                ),
                Cocktail(
                    name = "Wizard blizzard",
                    level = 18,
                    mixed = "item.mixed_blizzard",
                    mix =
                        listOf(
                            Part("item.vodka", 2),
                            Part("item.gin"),
                            Part("item.lime"),
                            Part("item.lemon"),
                            Part("item.orange"),
                        ),
                    pourInto = "item.wizard_blizzard",
                    garnish = listOf(Part("item.pineapple_chunks"), Part("item.lime_slices")),
                    pourExperience = 80.0,
                ),
                Cocktail(
                    name = "Short green guy",
                    level = 20,
                    mixed = "item.mixed_sgg",
                    mix = listOf(Part("item.vodka"), Part("item.lime", 3)),
                    pourInto = "item.short_green_guy",
                    garnish = listOf(Part("item.lime_slices"), Part("item.equa_leaves")),
                    pourExperience = 90.0,
                ),
                Cocktail(
                    name = "Blurberry special",
                    level = 37,
                    mixed = "item.mixed_special",
                    mix =
                        listOf(
                            Part("item.vodka"),
                            Part("item.brandy"),
                            Part("item.gin"),
                            Part("item.lemon", 2),
                            Part("item.orange"),
                        ),
                    pourInto = "item.blurberry_special",
                    garnish =
                        listOf(
                            Part("item.lemon_chunks"),
                            Part("item.orange_chunks"),
                            Part("item.equa_leaves"),
                            Part("item.lime_slices"),
                        ),
                    pourExperience = 150.0,
                ),
                // Garnished first, then heated on a range - the cook lives in food.json.
                Cocktail(
                    name = "Drunk dragon",
                    level = 32,
                    mixed = "item.mixed_dragon",
                    mix = listOf(Part("item.vodka"), Part("item.gin"), Part("item.dwellberries")),
                    pourInto = "item.mixed_dragon_9575",
                    addIngredsInto = "item.mixed_dragon_9576",
                    addIngreds = listOf(Part("item.pineapple_chunks"), Part("item.pot_of_cream")),
                ),
                // Heated first, then garnished - the opposite order to the dragon.
                Cocktail(
                    name = "Choc saturday",
                    level = 33,
                    mixed = MIXED_SATURDAY,
                    mix =
                        listOf(
                            Part("item.whisky"),
                            Part("item.chocolate_bar"),
                            Part("item.equa_leaves"),
                            Part("item.bucket_of_milk"),
                        ),
                    pourInto = "item.mixed_saturday_9572",
                    addIngredsInto = "item.choc_saturday",
                    addIngreds = listOf(Part("item.chocolate_dust"), Part("item.pot_of_cream")),
                    addExperience = 140.0,
                ),
            )

        /** Reverse lookup so [addIngredients] can name the item it is consuming. */
        val DRINK_ITEM_KEYS: Map<Int, String> =
            (
                DRINKS.map { getRSCM(it.pourInto) to it.pourInto } +
                    listOf(getRSCM(HEATED_SATURDAY) to HEATED_SATURDAY)
            ).toMap()
    }
}
