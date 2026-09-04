package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.alter.api.Skills
import org.alter.game.plugin.PluginRepository
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.saving.formats.SaveFormatType
import org.alter.plugins.content.items.consumables.Boost
import org.alter.plugins.content.items.consumables.Damage
import org.alter.plugins.content.items.consumables.DivineBoost
import org.alter.plugins.content.items.consumables.potions.Divine
import org.alter.plugins.content.items.consumables.potions.Potion
import org.alter.plugins.content.items.consumables.food.Food
import org.alter.plugins.content.items.consumables.potions.PotionPlugin
import org.alter.game.DevContext
import org.alter.game.GameContext
import org.alter.game.Server
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Builds `PotionPlugin` for real and checks what every dose in [Potion] actually bound.
 *
 * The same two silent failures `EatingVerify` guards against apply here and bite harder, because
 * [Potion] is by far the longer list: `onItemOption` throws when the named option is not on the
 * item, `bindItem` throws when two entries claim the same item, and a plugin whose constructor
 * throws registers *nothing* - so one bad dose takes away every potion in the game rather than the
 * one that caused it. That is what a "this potion does nothing" report looks like from the inside,
 * and it is indistinguishable from an item that was simply never listed.
 *
 * The divine family is called out separately because it was the second kind of failure: the items
 * resolved, the plugin loaded clean, and six of the eight divine potions were still inert because
 * nothing had ever listed them.
 */
class DrinkingVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** Mirrors the private `KotlinPlugin.INVENTORY_OP_OFFSET`; item ops arrive as 2 + index. */
        const val INVENTORY_OP_OFFSET = 2

        /**
         * Nothing here is read by the plugin; both only exist to build a [World]. Declared locally
         * rather than borrowed from a sibling verify so this file compiles on its own - a test that
         * reaches into another test's companion cannot be committed without dragging that file in
         * behind it.
         */
        val GAME_CONTEXT =
            GameContext(
                initialLaunch = false,
                name = "test",
                revision = 228,
                saveFormat = SaveFormatType.JSON,
                cycleTime = 600,
                playerLimit = 1,
                home = Tile(3222, 3222),
                skillCount = 23,
                npcStatCount = 6,
                runEnergy = true,
                gItemPublicDelay = 100,
                gItemDespawnDelay = 300,
                preloadMaps = false,
            )

        val DEV_CONTEXT =
            DevContext(
                debugExamines = false,
                debugObjects = false,
                debugButtons = false,
                debugItemActions = false,
                debugMagicSpells = false,
                debugPackets = false,
            )
    }

    @Suppress("UNCHECKED_CAST")
    private fun bindings(): Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<Any>> {
        val world = World(GAME_CONTEXT, DEV_CONTEXT)
        val repo = PluginRepository(world)
        PotionPlugin(repo, world, Server())

        val field = PluginRepository::class.java.getDeclaredField("itemPlugins")
        field.isAccessible = true
        return field.get(repo) as Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<Any>>
    }

    /**
     * The coverage check. A drinkable item nothing lists is not an error anywhere - clicking Drink
     * simply does nothing - so the only way to notice is to count the cache's drinkables against
     * what [Potion] and [Food] between them claim. Running this is what turned up six of the eight
     * divine potions, and a hundred-odd others, sitting inert.
     */
    @Test
    fun `every drinkable item in the cache is drunk by something`() {
        val bound = Potion.values.flatMap { it.doses.toList() }.map { getRSCM(it) }.toSet() +
            Food.values.filter { it.option == "drink" }.map { getRSCM(it.item) }.toSet()
        val unhandled = mutableListOf<String>()
        var drinkable = 0
        CacheManager.getItems().forEach { (_, item) ->
            val def = CacheManager.getItem(item.id)
            if (def.noteTemplateId > 0 || def.isPlaceholder) {
                return@forEach
            }
            if (def.interfaceOptions.none { it == "Drink" }) {
                return@forEach
            }
            drinkable++
            if (item.id !in bound && item.id !in INERT_ON_PURPOSE) {
                unhandled += "${item.id} (${def.name})"
            }
        }
        assertTrue(drinkable > 600, "only $drinkable drinkable items found; the cache did not load")
        assertEquals(
            emptyList(),
            unhandled.sorted(),
            "these drinkable items are inert and not on the known list",
        )
    }

    /**
     * Drinkable items nothing lists on purpose, because the effect behind them does not exist here
     * or is not an effect at all. Written out rather than counted so that adding one is a
     * deliberate act and losing a whole family of real potions is a failure.
     */
    private val INERT_ON_PURPOSE =
        setOf(
                // Cocktail ingredients: shaken but not yet a drink.
                2042, 2044, 2046, 2050, 2052, 2056, 2058, 2060, 2062, 2066,
                2068, 2070, 2072, 2076, 2078, 2082, 2086, 2088, 2090, 2094,
                2096, 2098,
                // Nightmare Zone absorption points, which are not modelled.
                11734, 11735, 11736, 11737,
                // Wintertodt warmth, likewise.
                20699, 20700, 20701, 20702,
                // The special attack bar, likewise.
                27339, 27341,
                // NPC aggression, likewise.
                30137, 30140, 30143, 30146,
                // Quest transformations and quest props.
                739, 756, 1582, 11204, 22589, 23806, 25812, 25813, 26581, 26583,
                26585, 26587, 26924, 28383, 28388,
                // A refillable flask and a refillable barrel, both of which need a charge count first.
                10859, 30000,
        )

    @Test
    fun `every dose binds its own drink option`() {
        val bindings = bindings()

        Potion.values.forEach { potion ->
            potion.doses.forEach { dose ->
                val id = getRSCM(dose)
                val def = CacheManager.getItem(id)
                val opts = assertNotNull(bindings[id], "$dose (${def.name}) bound nothing")

                val index = def.interfaceOptions.indexOfFirst { it?.lowercase() == "drink" }
                assertTrue(index >= 0, "$dose (${def.name}) has no Drink option in the cache")
                assertEquals(
                    setOf(INVENTORY_OP_OFFSET + index),
                    opts.keys.toSet(),
                    "$dose (${def.name}) bound the wrong option; " +
                        "interfaceOptions=${def.interfaceOptions.filterNotNull().filter { it.isNotBlank() }}",
                )
            }
        }
    }

    /**
     * Two entries claiming one item would throw out of `bindItem` and take the whole plugin with
     * it. Asserted directly so the failure names the pair rather than the plugin.
     */
    @Test
    fun `no item is claimed by two potions`() {
        val owners = HashMap<Int, String>()
        Potion.values.forEach { potion ->
            potion.doses.forEach { dose ->
                val previous = owners.put(getRSCM(dose), potion.name)
                assertTrue(
                    previous == null,
                    "$dose is listed by both $previous and ${potion.name}",
                )
            }
        }
    }

    /**
     * The dose chain has to walk down to the next-lowest dose and then to the container, so the
     * doses must be listed lowest first and the container must resolve. A four-entry list written
     * highest-first binds fine and then hands back the wrong item on every sip.
     */
    @Test
    fun `doses are listed lowest first and empty into a real container`() {
        Potion.values.forEach { potion ->
            getRSCM(potion.emptied)

            val names = potion.doses.map { CacheManager.getItem(getRSCM(it)).name }
            potion.doses.forEachIndexed { index, dose ->
                val name = names[index]
                // Single-dose drinks carry no "(n)" suffix at all, so there is nothing to check.
                if (potion.doses.size > 1) {
                    assertTrue(
                        name.contains("(${index + 1})") || name.contains("(${index + 1} "),
                        "${potion.name} lists $dose ($name) as dose ${index + 1}",
                    )
                }
            }
        }
    }

    /**
     * The report that started this: a divine super defence potion could not be drunk. All eight of
     * the family are in the cache and only two were ever listed.
     */
    @Test
    fun `every divine potion is drinkable and charges its ten hitpoints`() {
        val bindings = bindings()

        val divines =
            listOf(
                "divine_super_attack_potion" to Skills.ATTACK,
                "divine_super_strength_potion" to Skills.STRENGTH,
                "divine_super_defence_potion" to Skills.DEFENCE,
                "divine_super_combat_potion" to Skills.ATTACK,
                "divine_ranging_potion" to Skills.RANGED,
                "divine_magic_potion" to Skills.MAGIC,
                "divine_bastion_potion" to Skills.RANGED,
                "divine_battlemage_potion" to Skills.MAGIC,
            )

        divines.forEach { (family, boosted) ->
            val potion =
                assertNotNull(
                    Potion.values.firstOrNull { it.doses.contains("item.${family}4") },
                    "$family is not listed in Potion",
                )

            assertEquals(4, potion.doses.size, "${potion.name} does not carry four doses")
            potion.doses.forEach { dose ->
                assertNotNull(bindings[getRSCM(dose)], "$dose bound nothing")
            }

            assertTrue(
                potion.effects.filterIsInstance<DivineBoost>().any { it.skill == boosted },
                "${potion.name} does not hold the skill it is named after",
            )
            assertEquals(
                listOf(Damage(Divine.HITPOINT_COST)),
                potion.effects.filterIsInstance<Damage>(),
                "${potion.name} does not charge its ten hitpoints",
            )
            assertEquals(
                Divine.MIN_HITPOINTS,
                potion.minHitpoints,
                "${potion.name} would let a dose kill the drinker",
            )
        }
    }

    /**
     * A divine potion holds exactly the boost of the potion it is named after. Typed by hand from
     * the wiki twice over, so worth pinning: a divine super defence that quietly boosted by the
     * ranging potion's `4 + 10%` would bind, apply, and be wrong forever.
     */
    @Test
    fun `divine potions mirror the potion they are named after`() {
        fun boostOf(
            family: String,
            skill: Int,
        ): Pair<Int, Int> {
            val effects = Potion.values.first { it.doses.contains("item.${family}4") }.effects
            effects.filterIsInstance<Boost>().firstOrNull { it.skill == skill }?.let {
                return it.flat to it.percent
            }
            val divine = effects.filterIsInstance<DivineBoost>().first { it.skill == skill }
            return divine.flat to divine.percent
        }

        val pairs =
            listOf(
                Triple("super_attack", "divine_super_attack_potion", Skills.ATTACK),
                Triple("super_strength", "divine_super_strength_potion", Skills.STRENGTH),
                Triple("super_defence", "divine_super_defence_potion", Skills.DEFENCE),
                Triple("ranging_potion", "divine_ranging_potion", Skills.RANGED),
                Triple("magic_potion", "divine_magic_potion", Skills.MAGIC),
                Triple("bastion_potion", "divine_bastion_potion", Skills.RANGED),
                Triple("bastion_potion", "divine_bastion_potion", Skills.DEFENCE),
                Triple("battlemage_potion", "divine_battlemage_potion", Skills.MAGIC),
                Triple("battlemage_potion", "divine_battlemage_potion", Skills.DEFENCE),
            )

        pairs.forEach { (base, divine, skill) ->
            assertEquals(
                boostOf(base, skill),
                boostOf(divine, skill),
                "$divine does not boost skill $skill the way $base does",
            )
        }
    }

    /**
     * A four-pint keg has to walk down its own doses and end on the keg, not on a vial and not on
     * itself. A chain that loops or stops early is the kind of fault that only shows up as an item
     * quietly turning into the wrong thing three sips in.
     */
    @Test
    fun `every ale keg walks its doses down to the keg`() {
        val kegs = Food.values.filter { it.name.contains("_KEG_") }
        assertEquals(80, kegs.size, "expected ten ales in a plain and a mature keg, four doses each")

        kegs.forEach { keg ->
            val dose = keg.name.substringAfterLast('_').toInt()
            val expected =
                if (dose == 1) {
                    "item.calquat_keg"
                } else {
                    Food.values.first { it.name == keg.name.dropLast(1) + (dose - 1) }.item
                }
            assertEquals(expected, keg.replacement, "${keg.name} pours into the wrong thing")
            assertNotNull(CacheManager.getItem(getRSCM(keg.replacement!!)).name)
        }
    }

    /**
     * The mature keg has to pour the mature ale, not the ale it matured from. Both chains are named
     * off the same ten families and the pairs sit next to each other in the file, which is exactly
     * where a copied line goes unnoticed.
     */
    @Test
    fun `a keg pours the same drink as the pint it is named after`() {
        Food.values.filter { it.name.contains("_KEG_") }.forEach { keg ->
            val family = keg.name.substringBefore("_KEG_")
            val pint =
                assertNotNull(
                    Food.values.firstOrNull { it.name == family },
                    "${keg.name} has no pint called $family to pour",
                )
            assertEquals(pint.effects, keg.effects, "${keg.name} does not pour ${pint.name}")
            assertEquals(pint.heal, keg.heal, "${keg.name} does not heal what ${pint.name} does")
        }
    }

    /**
     * An item the live game refuses to let the player drink must consume nothing and do nothing -
     * otherwise the refusal is decoration over a drink that still went down.
     */
    @Test
    fun `refused drinks consume nothing`() {
        val refused = Food.values.filter { it.refusal != null }
        assertTrue(refused.isNotEmpty(), "no refusals are listed")

        refused.forEach { food ->
            assertEquals(0, food.heal, "${food.name} is refused but still heals")
            assertEquals(emptyList(), food.effects, "${food.name} is refused but still applies effects")
            assertEquals(null, food.replacement, "${food.name} is refused but still leaves something")
        }
    }

    /**
     * The overloads are the only other potions that charge hitpoints, and the only ones that hand
     * them back. Both halves have to agree, or an overload is a fifty-point tax.
     */
    @Test
    fun `every overload charges fifty hitpoints and refunds them`() {
        val overloads = Potion.values.filter { it.name.contains("OVERLOAD") }
        assertTrue(overloads.isNotEmpty(), "no overloads are listed")

        overloads.forEach { potion ->
            val damage = potion.effects.filterIsInstance<Damage>().single()
            val refund =
                potion.effects
                    .filterIsInstance<org.alter.plugins.content.items.consumables.HealOnExpiry>()
                    .single()
            assertEquals(damage.amount, refund.amount, "${potion.name} does not refund what it charges")
            assertEquals(
                damage.amount + 1,
                potion.minHitpoints,
                "${potion.name} can be drunk at a hitpoint level that would kill",
            )
            assertEquals(
                COMBAT_STATS,
                potion.effects.filterIsInstance<DivineBoost>().map { it.skill }.toSet(),
                "${potion.name} does not hold every combat stat",
            )
        }
    }
}

/** The five stats an overload holds. */
private val COMBAT_STATS =
    setOf(Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE, Skills.RANGED, Skills.MAGIC)
