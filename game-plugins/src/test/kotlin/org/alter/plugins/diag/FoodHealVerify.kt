package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.plugins.content.items.consumables.DelayedHeal
import org.alter.plugins.content.items.consumables.HealPercent
import org.alter.plugins.content.items.consumables.HealRange
import org.alter.plugins.content.items.consumables.food.Food
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * What [Food] heals, and how much of the game's food it covers.
 *
 * [EatingVerify] proves each entry *binds*; this proves the numbers behind the bindings. Both
 * halves matter and neither implies the other - a food can be bound to the right option and still
 * heal the wrong amount, or nothing at all, and the only symptom is that eating it feels off.
 *
 * Heal amounts are the live game's, from the OSRS Wiki's all-food table and the item articles.
 */
class FoodHealVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /**
         * Items with an "Eat" option that [Food] deliberately leaves alone, because eating them
         * does nothing in the live game either or their effect is not a heal at all.
         */
        private val NOT_FOOD =
            setOf(
                // Cannot be eaten whole; they have to be cut up first.
                1851, 2114, 5982,
                // Only heal inside the Moons of Peril, which does not exist here.
                29077, 29217,
                // A random skill-boost table rather than a heal.
                7479,
                // A Halloween prop.
                27485,
                // Half-made gnome dishes, which are ingredients rather than meals.
                2173, 2179, 2181, 2183, 2189, 2193, 2197, 2207, 2211, 2215,
                2245, 2251, 2257, 2261, 2263, 2265, 2267, 2269, 2271, 2273,
                2275, 2279,
                // Recipe for Disaster servery props.
                13403, 13409, 13412, 13413, 13414, 13418,
                // Joke, rotten and poisoned food, none of which heals anything.
                464, 1984, 2379, 2398, 3146, 5733, 6202, 6206, 6768, 7509,
                7510, 11205, 27790, 29784,
                // Cooking ingredients that happen to carry an Eat option.
                2154, 2156, 2158, 2160, 3168, 7088,
                // Eels that are crafting materials, not food.
                13339, 21293,
                // Tithe Farm produce.
                13426, 13427, 13428,
                // Holiday sweets and candy, none of which the wiki's food list rates as healing.
                4558, 4559, 4560, 4562, 4563, 4564, 9475, 24980, 24981, 24982,
                24983, 24984, 24985, 24986, 24987, 24988,
                // Quest and event one-offs.
                26917, 28422,
            )
    }

    private fun food(item: Int): Food {
        val name = CacheManager.getItem(item).name
        return assertNotNull(
            Food.values.firstOrNull { getRSCM(it.item) == item },
            "item $item ($name) is edible but nothing in Food eats it",
        )
    }

    private fun heal(item: Int): Int = food(item).heal

    /**
     * Every id here is one that ate as a no-op before: `Food` covered 128 of the 345 items the
     * cache marks edible.
     */
    @Test
    fun `the food that had no entry at all now heals what it should`() {
        mapOf(
            337 to 6, // Giant carp
            2149 to 11, // Lava eel
            7568 to 15, // Cooked jubbly
            7579 to 20, // Stuffed snake
            23874 to 20, // Paddlefish
            11326 to 5, // Caviar
            11324 to 3, // Roe
            10971 to 10, // Eel sushi
            9527 to 11, // Fruit batta
            9553 to 15, // Chocolate bomb
            2343 to 14, // Cooked oomlie wrap
            6883 to 8, // Peach
            5972 to 8, // Papaya fruit
            22929 to 10, // Dragonfruit
            27351 to 20, // Honey locust
            20868 to 23, // Kyren fish, the top of the Gauntlet ladder
            20883 to 23, // Psykk bat, likewise
            25631 to 6, // Steak sandwich
            7530 to 11, // Cooked fishcake
            4016 to 11, // Banana stew
        ).forEach { (item, expected) ->
            assertEquals(expected, heal(item), CacheManager.getItem(item).name)
        }
    }

    /** The duplicate ids: the same food again under a minigame's or a raid's own item. */
    @Test
    fun `a food's other item ids heal the same as the original`() {
        listOf(385 to 6969, 385 to 20390).forEach { (original, copy) ->
            assertEquals(heal(original), heal(copy), "shark copy $copy")
        }
        assertEquals(heal(361), heal(26149), "tuna copy") // Tuna
        assertEquals(heal(7946), heal(20547), "monkfish copy") // Monkfish
        assertEquals(heal(2140), heal(4291), "cooked chicken copy")
        assertEquals(heal(2142), heal(4293), "cooked meat copy")
        assertEquals(heal(3144), heal(23533), "cooked karambwan copy")
        assertEquals(22, heal(24589), "Blighted manta ray")
        assertEquals(18, heal(24595), "Blighted karambwan")
        assertTrue(food(24595).comboFood, "a blighted karambwan is still a combo food")
        assertTrue(food(24592).overheal, "a blighted anglerfish still overheals")
    }

    /** Corrections to entries that were already there. */
    @Test
    fun `the heals that were wrong are right`() {
        assertEquals(22, heal(391), "Manta ray")
        assertEquals(3, heal(2140), "Cooked chicken")
        assertEquals(3, heal(2142), "Cooked meat")
        assertEquals(2, heal(5747), "Dwarven stout (m)")
        assertEquals(2, heal(5761), "Slayer's respite (m)")
        assertEquals(14, heal(7157), "Braindeath 'rum'")
    }

    /** Food that heals a spread rather than a figure. */
    @Test
    fun `random-range food carries a HealRange`() {
        mapOf(
            3369 to (5 to 7), // Thin snail meat
            3371 to (5 to 8), // Lean snail meat
            3373 to (7 to 9), // Fat snail meat
            3381 to (6 to 10), // Cooked slimy eel
            5004 to (3 to 6), // Frog spawn
            6297 to (7 to 10), // Spider on stick
            4561 to (1 to 3), // Purple sweets
        ).forEach { (item, range) ->
            val entry = food(item)
            val effect =
                assertNotNull(
                    entry.effects.filterIsInstance<HealRange>().firstOrNull(),
                    "${CacheManager.getItem(item).name} has no HealRange",
                )
            assertEquals(range.first, effect.min, "${CacheManager.getItem(item).name} minimum")
            assertEquals(range.second, effect.max, "${CacheManager.getItem(item).name} maximum")
            assertEquals(0, entry.heal, "${CacheManager.getItem(item).name} should heal only through the range")
        }
    }

    /** Food whose heal scales with the eater's hitpoints level. */
    @Test
    fun `level-scaled food carries a HealPercent on top of its flat heal`() {
        mapOf(
            1971 to (3 to 7), // Kebab
            4608 to (3 to 7), // Super kebab
            29900 to (4 to 10), // Varlamorian kebab
            5504 to (1 to 6), // Strawberry
            5984 to (1 to 5), // Watermelon slice
        ).forEach { (item, expected) ->
            val entry = food(item)
            val name = CacheManager.getItem(item).name
            assertEquals(expected.first, entry.heal, "$name flat heal")
            val percent = assertNotNull(entry.effects.filterIsInstance<HealPercent>().firstOrNull(), "$name has no HealPercent")
            assertEquals(expected.second, percent.percent, "$name percentage")
        }
    }

    /** The Varlamore hunter meats heal once immediately and again three seconds later. */
    @Test
    fun `hunter meat heals twice`() {
        mapOf(
            29128 to (4 to 4), // Cooked wild kebbit
            29131 to (7 to 5), // Cooked barb-tailed kebbit
            29134 to (13 to 10), // Cooked dashing kebbit
            29137 to (11 to 8), // Cooked pyre fox
            29140 to (12 to 9), // Cooked sunlight antelope
            29143 to (14 to 12), // Cooked moonlight antelope
            29146 to (6 to 5), // Cooked larupia
            29149 to (8 to 6), // Cooked graahk
            29152 to (9 to 8), // Cooked kyatt
        ).forEach { (item, expected) ->
            val entry = food(item)
            val name = CacheManager.getItem(item).name
            assertEquals(expected.first, entry.heal, "$name immediate heal")
            val delayed = assertNotNull(entry.effects.filterIsInstance<DelayedHeal>().firstOrNull(), "$name has no DelayedHeal")
            assertEquals(expected.second, delayed.amount, "$name delayed heal")
            assertEquals(5, delayed.delayTicks, "$name delay - the wiki gives three seconds for all of them")
        }
    }

    /** Recipe for Disaster's crab meat is five bites, each leaving the next portion behind. */
    @Test
    fun `crab meat walks down its five portions`() {
        val chain = listOf(7521, 7523, 7524, 7525, 7526)
        chain.forEachIndexed { index, item ->
            val entry = food(item)
            assertEquals(2, entry.heal, "crab meat bite ${5 - index}")
            val next = chain.getOrNull(index + 1)
            assertEquals(
                next,
                entry.replacement?.let { getRSCM(it) },
                "crab meat bite ${5 - index} should leave ${next ?: "nothing"} behind",
            )
        }
    }

    /**
     * The coverage check, and the reason all of the above exists: an edible item with no entry
     * eats as a silent no-op, which looks exactly like a working food that heals nothing.
     */
    @Test
    fun `every edible item in the cache is either eaten or deliberately excluded`() {
        val bound = Food.values.map { getRSCM(it.item) }.toSet()
        val unhandled = mutableListOf<String>()
        var edible = 0
        CacheManager.getItems().forEach { (_, item) ->
            val def = CacheManager.getItem(item.id)
            if (def.noteTemplateId > 0 || def.isPlaceholder) {
                return@forEach
            }
            if (def.interfaceOptions.none { it == "Eat" }) {
                return@forEach
            }
            edible++
            if (item.id in bound || item.id in NOT_FOOD) {
                return@forEach
            }
            unhandled += "${item.id} (${def.name})"
        }
        assertTrue(edible > 300, "only $edible edible items found; the cache did not load")
        assertEquals(
            emptyList(),
            unhandled.sorted(),
            "these edible items have no Food entry and are not on the known non-healing list",
        )
        assertTrue(
            bound.size > 400,
            "only ${bound.size} foods are wired; the list has lost entries",
        )
    }
}
