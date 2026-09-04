package org.alter.plugins.diag

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.alter.api.EquipmentType
import org.alter.game.Server
import org.alter.game.fs.ObjectExamineHolder
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.items.jewellery.ChargedJewellery
import org.alter.plugins.content.items.jewellery.ChargedJewelleryPlugin
import org.alter.plugins.content.items.jewellery.EnchantSpell
import org.alter.plugins.content.items.jewellery.JewelleryEnchantPlugin
import org.alter.plugins.content.items.jewellery.JewelleryRechargePlugin
import org.alter.plugins.content.items.jewellery.PerkJewellery
import org.alter.plugins.content.items.jewellery.PerkJewelleryPlugin
import org.alter.plugins.content.items.jewellery.PotionDoses
import org.alter.plugins.content.items.jewellery.RingOfWealth
import org.alter.plugins.content.items.jewellery.RingOfWealthPlugin
import org.alter.plugins.content.items.jewellery.SurvivalJewelleryPlugin
import org.alter.plugins.content.npcs.GemDropTable
import org.alter.plugins.content.npcs.RareDropTable
import org.alter.plugins.content.skills.smithing.BarEntry
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.alter.tools.CacheCollision
import org.junit.BeforeClass
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for the jewellery content, covering the four things that would leave it
 * silently broken rather than loudly failing:
 *
 * 1. an RSCM key that does not resolve, which throws while the plugin is being constructed and takes
 *    every registration in that plugin down with it;
 * 2. an item that does not carry the option a plugin binds by name - `ChargedJewelleryPlugin`
 *    deliberately *skips* a missing equipment option rather than throwing, so without this test a
 *    renamed cache option would just make a destination stop working;
 * 3. a teleport destination on a tile a player cannot stand on;
 * 4. a spell id whose cache params no longer describe the spell it is supposed to be, which would
 *    bind an enchant to the wrong spellbook button.
 */
class JewelleryVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
            ObjectExamineHolder.load()
        }

        /** Enough rolls that a table's Nothing row cannot be missed by chance. */
        private const val ROLLS = 2_000

        /**
         * The equipment slot an item is worn in, as the cache reports it, so the table's own slot
         * can be checked rather than trusted. Param 452 in `ObjectExamineHolder` terms is an
         * *option*; the worn slot lives in the item's `equipSlot`.
         */
        private val SLOT_BY_TYPE =
            mapOf(
                EquipmentType.AMULET to 2,
                EquipmentType.GLOVES to 9,
                EquipmentType.RING to 12,
            )
    }

    /** Every region any destination tile falls in, plus its neighbours, loaded once. */
    private val scene: CacheCollision.Scene by lazy {
        val regions = mutableSetOf<Int>()
        ChargedJewellery.values.forEach { entry ->
            entry.destinations.mapNotNull { it.tile }.forEach { tile ->
                for (dx in -1..1) {
                    for (dz in -1..1) {
                        regions.add(CacheCollision.regionOf(tile.x + dx * 64, tile.z + dz * 64))
                    }
                }
            }
        }
        CacheCollision.load(regions)
    }

    @Test
    fun `every enchant spell id still carries the spell it is named for`() {
        EnchantSpell.values.forEach { spell ->
            val params = CacheManager.getItem(spell.spellItem).params
            assertNotNull(params, "Cache item ${spell.spellItem} has no params; it cannot be ${spell.spellName}.")
            assertEquals(
                spell.spellName,
                params[601],
                "Cache item ${spell.spellItem} is no longer ${spell.spellName}.",
            )
            // Param 604 is the level requirement and 596 the spellbook component hash. Both are read
            // at runtime rather than hardcoded, so all this needs to prove is that they are there.
            assertNotNull(params[604], "${spell.spellName} has no level requirement param.")
            assertNotNull(params[596], "${spell.spellName} has no component hash param.")
        }
    }

    @Test
    fun `every enchant conversion resolves to a real item`() {
        EnchantSpell.values.forEach { spell ->
            spell.conversions.forEach { (from, to) ->
                val fromId = getRSCM(from)
                val toId = getRSCM(to)
                assertTrue(fromId > 0, "$from does not resolve (${spell.spellName}).")
                assertTrue(toId > 0, "$to does not resolve (${spell.spellName}).")
                assertTrue(
                    CacheManager.getItem(toId).name.isNotBlank() && CacheManager.getItem(toId).name != "null",
                    "$to (id $toId) is a nameless cache entry - probably a placeholder id (${spell.spellName}).",
                )
            }
        }
    }

    /** No item may be the input of two different enchants, or the binding would be ambiguous. */
    @Test
    fun `no unenchanted item is claimed by two enchant spells`() {
        val seen = mutableMapOf<String, EnchantSpell>()
        EnchantSpell.values.forEach { spell ->
            spell.conversions.keys.forEach { from ->
                val clash = seen.put(from, spell)
                assertTrue(clash == null, "$from is converted by both ${clash?.spellName} and ${spell.spellName}.")
            }
        }
    }

    @Test
    fun `every charged jewellery item resolves and sits in the slot the table claims`() {
        ChargedJewellery.values.forEach { entry ->
            val expectedSlot = SLOT_BY_TYPE.getValue(entry.slot)
            (entry.chargeItems + listOfNotNull(entry.uncharged)).forEach { key ->
                val id = getRSCM(key)
                assertTrue(id > 0, "$key does not resolve (${entry.displayName}).")
                val def = CacheManager.getItem(id)
                assertEquals(
                    expectedSlot,
                    def.equipSlot,
                    "$key ('${def.name}') is worn in slot ${def.equipSlot}, not ${entry.slot} (${entry.displayName}).",
                )
            }
        }
    }

    /**
     * The charge ladder has to be in order - index n is n+1 charges - or the plugin would hand back
     * the wrong item when it spends one. The cache names them "(1)".."(8)", which is checkable.
     */
    @Test
    fun `every charge ladder is in ascending charge order`() {
        ChargedJewellery.values.forEach { entry ->
            entry.chargeItems.forEachIndexed { index, key ->
                val name = CacheManager.getItem(getRSCM(key)).name
                // "(4)" on a plain piece, "(t4)" on the trimmed glory, "(i4)" on the imbued ring.
                val charges = Regex("\\([ti]?(\\d)\\)").find(name)?.groupValues?.get(1)?.toIntOrNull()
                assertEquals(
                    index + 1,
                    charges,
                    "$key is '$name' but sits at ladder index $index (${entry.displayName}).",
                )
            }
        }
    }

    @Test
    fun `every charged piece carries the worn options the plugin binds`() {
        ChargedJewellery.values.forEach { entry ->
            entry.chargeItems.forEach { key ->
                val menu = ObjectExamineHolder.EQUIPMENT_MENU.get(getRSCM(key))
                assertNotNull(menu, "$key has no equipment menu at all (${entry.displayName}).")
                entry.destinations.forEach { destination ->
                    assertTrue(
                        menu.equipmentMenu.any { it.equals(destination.option, ignoreCase = true) },
                        "$key has no worn option '${destination.option}' " +
                            "[options=${menu.equipmentMenu.filterNotNull()}] (${entry.displayName}).",
                    )
                }
            }
            entry.uncharged?.let { key ->
                val menu = ObjectExamineHolder.EQUIPMENT_MENU.get(getRSCM(key))
                assertNotNull(menu, "$key has no equipment menu at all (${entry.displayName}).")
                assertTrue(
                    menu.equipmentMenu.any { it.equals("Rub", ignoreCase = true) },
                    "Uncharged $key has no worn 'Rub' option (${entry.displayName}).",
                )
            }
        }
    }

    /** "Rub" is an *inventory* option on every piece, charged or not, and is bound by name. */
    @Test
    fun `every piece carries the inventory Rub option`() {
        ChargedJewellery.values.forEach { entry ->
            (entry.chargeItems + listOfNotNull(entry.uncharged)).forEach { key ->
                val def = CacheManager.getItem(getRSCM(key))
                assertTrue(
                    def.interfaceOptions.any { it.equals("Rub", ignoreCase = true) },
                    "$key ('${def.name}') has no inventory 'Rub' option " +
                        "[options=${def.interfaceOptions.filterNotNull()}] (${entry.displayName}).",
                )
            }
        }
    }

    @Test
    fun `every teleport destination lands on a tile a player can stand on`() {
        val unreachable = mutableListOf<String>()
        ChargedJewellery.values.forEach { entry ->
            entry.destinations.forEach { destination ->
                val tile = destination.tile ?: return@forEach
                if (!scene.canStandOn(tile)) {
                    unreachable += "${entry.displayName} -> ${destination.option} at $tile"
                }
            }
        }
        assertTrue(unreachable.isEmpty(), "Teleport destinations on blocked tiles:\n  ${unreachable.joinToString("\n  ")}")
    }

    /**
     * The one check that covers everything at once: build all five jewellery plugins against a real
     * [PluginRepository], as the server's own scan does.
     *
     * A [org.alter.game.plugin.KotlinPlugin] whose constructor throws registers *nothing* and the
     * real scan swallows the exception, so an unresolvable RSCM key, a missing option or - the one
     * that is easy to reintroduce - two plugins binding the same item option would take the whole
     * feature out with no message anywhere. Here it is an outright failure.
     */
    @Test
    fun `every jewellery plugin builds against a real plugin repository`() {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        val repo = PluginRepository(world)
        val server = Server()

        listOf(
            JewelleryEnchantPlugin::class.java,
            ChargedJewelleryPlugin::class.java,
            JewelleryRechargePlugin::class.java,
            SurvivalJewelleryPlugin::class.java,
            PerkJewelleryPlugin::class.java,
            RingOfWealthPlugin::class.java,
        ).forEach { type ->
            type.getConstructor(PluginRepository::class.java, World::class.java, Server::class.java)
                .newInstance(repo, world, server)
        }
    }

    /**
     * The options `SurvivalJewelleryPlugin`, `RingOfRecoilPlugin` and `JewelleryRechargePlugin` bind
     * by name. These three *do* use the throwing `onEquipmentOption`/`onItemOption`/`onObjOption`
     * directly, so a missing one takes the whole plugin's registrations down at boot.
     */
    @Test
    fun `every option the non-teleport jewellery plugins bind by name exists`() {
        fun invOptions(key: String) = CacheManager.getItem(getRSCM(key)).interfaceOptions.filterNotNull()
        fun wornOptions(key: String) =
            ObjectExamineHolder.EQUIPMENT_MENU.get(getRSCM(key))?.equipmentMenu?.filterNotNull().orEmpty()

        assertTrue(
            invOptions("item.ring_of_life").any { it.equals("Toggle-respawn", ignoreCase = true) },
            "The ring of life has no inventory 'Toggle-respawn' option [${invOptions("item.ring_of_life")}].",
        )
        listOf(
            "object.fountain_of_heroes_31881",
            "object.totem_pole_31879",
            "object.fountain_of_rune_31942",
        ).forEach { key ->
            val def = CacheManager.getObject(getRSCM(key))
            assertTrue(
                def.actions.filterNotNull().any { it.equals("Recharge-jewellery", ignoreCase = true) },
                "$key ('${def.name}') has no 'Recharge-jewellery' option [${def.actions.filterNotNull()}].",
            )
        }
    }

    /**
     * Every perk piece has to resolve, sit in the slot [JewelleryPerks] reads it out of, and hold a
     * sane charge count - a wrong slot means the effect silently never fires, because the check is
     * "is this item in this slot".
     */
    @Test
    fun `every perk jewellery item resolves and sits in the slot its effect checks`() {
        PerkJewellery.values.forEach { perk ->
            val id = getRSCM(perk.item)
            assertTrue(id > 0, "${perk.item} does not resolve.")
            val def = CacheManager.getItem(id)
            assertEquals(
                SLOT_BY_TYPE.getValue(perk.slot),
                def.equipSlot,
                "${perk.item} ('${def.name}') is worn in slot ${def.equipSlot}, not ${perk.slot}.",
            )
            assertTrue(perk.maxCharges > 0, "${perk.item} has no charges.")
        }
    }

    /** Two perks sharing a persistence key would silently share a charge pool. */
    @Test
    fun `no two perks share a charge attribute or an item`() {
        val keys = PerkJewellery.values.map { it.chargesAttr.persistenceKey }
        assertEquals(keys.size, keys.toSet().size, "two perks share a charge attribute: $keys")
        val items = PerkJewellery.values.map { it.item }
        assertEquals(items.size, items.toSet().size, "two perks name the same item: $items")
    }

    /**
     * `PerkJewelleryPlugin` binds Check and Break only where the cache has them, so this records
     * which pieces are expected to carry which - a cache change that drops one would otherwise just
     * make the option quietly stop working.
     */
    @Test
    fun `the perk pieces carry the Check and Break options the plugin expects`() {
        fun invOptions(key: String) = CacheManager.getItem(getRSCM(key)).interfaceOptions.filterNotNull()
        fun wornOptions(key: String) =
            ObjectExamineHolder.EQUIPMENT_MENU.get(getRSCM(key))?.equipmentMenu?.filterNotNull().orEmpty()

        // Everything but the amulet of chemistry, whose worn option is "Options" - a configuration
        // screen this project has no interface for - can be checked while worn.
        PerkJewellery.values
            .filter { it != PerkJewellery.AMULET_OF_CHEMISTRY }
            .forEach { perk ->
                assertTrue(
                    wornOptions(perk.item).any { it.equals("Check", ignoreCase = true) },
                    "${perk.item} has no worn 'Check' option [${wornOptions(perk.item)}].",
                )
            }

        // The bracelet of clay and the ring of recoil are the two without an inventory Check.
        listOf(
            PerkJewellery.DODGY_NECKLACE,
            PerkJewellery.EXPEDITIOUS_BRACELET,
            PerkJewellery.BRACELET_OF_SLAUGHTER,
        ).forEach { perk ->
            assertTrue(
                invOptions(perk.item).any { it.equals("Check", ignoreCase = true) },
                "${perk.item} has no inventory 'Check' option [${invOptions(perk.item)}].",
            )
        }

        // Break is what resets the pool; the bracelet of clay is the one piece that has none.
        PerkJewellery.values
            .filter { it != PerkJewellery.BRACELET_OF_CLAY }
            .forEach { perk ->
                assertTrue(
                    invOptions(perk.item).any { it.equals("Break", ignoreCase = true) },
                    "${perk.item} has no inventory 'Break' option [${invOptions(perk.item)}].",
                )
            }
    }

    /**
     * That the Check and Break options are bound to the ops the client actually sends.
     *
     * Worth asserting rather than assuming: this project has already had both offsets wrong at once
     * (see [InventoryOptionOpVerify]), and the equipment one put every *index 0* worn option on the
     * op the equipment interface intercepts as Remove - which is exactly where the ring of recoil's
     * "Check" sits. A wrong offset here binds silently and does nothing when clicked.
     */
    @Suppress("UNCHECKED_CAST")
    @Test
    fun `the perk options bind to the ops the client sends`() {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        val repo = PluginRepository(world)
        PerkJewelleryPlugin(repo, world, Server())

        val itemField = PluginRepository::class.java.getDeclaredField("itemPlugins")
        itemField.isAccessible = true
        val itemBindings = itemField.get(repo) as Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<Any>>

        val equipField = PluginRepository::class.java.getDeclaredField("equipmentOptionPlugins")
        equipField.isAccessible = true
        val equipBindings = equipField.get(repo) as Int2ObjectOpenHashMap<Any>

        PerkJewellery.values.forEach { perk ->
            val id = getRSCM(perk.item)
            val def = CacheManager.getItem(id)
            val worn = ObjectExamineHolder.EQUIPMENT_MENU.get(id)?.equipmentMenu.orEmpty()

            val wornCheck = worn.indexOfFirst { it.equals("Check", ignoreCase = true) }
            if (wornCheck != -1) {
                val hash = (id shl 16) or (KotlinPlugin.EQUIPMENT_OP_OFFSET + wornCheck)
                assertNotNull(
                    equipBindings[hash],
                    "${perk.item}: worn 'Check' at index $wornCheck bound nothing at op " +
                        "${KotlinPlugin.EQUIPMENT_OP_OFFSET + wornCheck}.",
                )
            }

            listOf("Check", "Break").forEach { option ->
                val index = def.interfaceOptions.indexOfFirst { it.equals(option, ignoreCase = true) }
                if (index == -1) {
                    return@forEach
                }
                val op = InventoryOptionOpVerify.INVENTORY_OP_OFFSET + index
                assertNotNull(
                    itemBindings[id]?.get(op),
                    "${perk.item}: inventory '$option' at index $index bound nothing at op $op " +
                        "[bound ops=${itemBindings[id]?.keys}].",
                )
            }
        }
    }

    /**
     * The amulet of chemistry is a no-op unless [PotionDoses] can pair three-dose products with
     * four-dose ones, and it builds that map out of `Potion`'s ladders - so an empty map means the
     * perk does nothing and nothing would say so.
     */
    @Test
    fun `the amulet of chemistry can find four-dose potions`() {
        assertTrue(PotionDoses.size() > 20, "Only ${PotionDoses.size()} three-to-four dose pairings resolved.")

        val attack3 = getRSCM("item.attack_potion3")
        val attack4 = getRSCM("item.attack_potion4")
        assertEquals(attack4, PotionDoses.fourDoseOf(attack3), "attack potion(3) does not map to (4).")
        assertEquals(null, PotionDoses.fourDoseOf(attack4), "a four-dose potion must not upgrade again.")
    }

    /**
     * The ring of forging keys off the iron bar's item id, so that id has to be the one the smelting
     * config actually produces. `SmithingVerify` already pins the iron recipe's 50% and its level;
     * this pins the item the two of them agree on.
     */
    @Test
    fun `the ring of forging targets the iron bar the smelting config makes`() {
        val bars: List<BarEntry> =
            Files.newBufferedReader(Paths.get("../data/cfg/smithing/bars.json")).use {
                Gson().fromJson(it, object : TypeToken<List<BarEntry>>() {}.type)
            }
        val iron = bars.single { it.name == "Iron bar" }
        assertEquals("item.iron_bar", iron.bar, "the iron recipe no longer makes item.iron_bar")
        assertTrue(iron.successChance < 1.0, "iron no longer fails, so the ring of forging is pointless")
        assertTrue(getRSCM("item.iron_bar") > 0, "item.iron_bar does not resolve")
    }

    /**
     * Every ring of wealth, plain and imbued, charged and not - the passive effects key off this set
     * and work "whether or not the ring is charged", so a missing id is an effect that silently does
     * not apply to that variant.
     */
    @Test
    fun `every ring of wealth resolves, is a ring, and carries the collection toggle`() {
        assertEquals(12, RingOfWealth.ringKeys.size, "the ring of wealth id set has changed size")
        assertEquals(RingOfWealth.ringKeys.size, RingOfWealth.ringIds.size, "two ring keys resolve to the same id")

        RingOfWealth.ringKeys.forEach { key ->
            val id = getRSCM(key)
            val def = CacheManager.getItem(id)
            assertEquals(
                SLOT_BY_TYPE.getValue(EquipmentType.RING),
                def.equipSlot,
                "$key ('${def.name}') is not worn in the ring slot.",
            )
            assertTrue(
                def.interfaceOptions.any { it.equals("Features", ignoreCase = true) },
                "$key ('${def.name}') has no inventory 'Features' option [${def.interfaceOptions.filterNotNull()}].",
            )
        }

        // The charged rings additionally carry the worn toggle; the uncharged two show "Rub" there.
        listOf("item.ring_of_wealth_5", "item.ring_of_wealth_i5").forEach { key ->
            val worn = ObjectExamineHolder.EQUIPMENT_MENU.get(getRSCM(key))?.equipmentMenu?.filterNotNull().orEmpty()
            assertTrue(
                worn.any { it.equals("Coin Collection", ignoreCase = true) },
                "$key has no worn 'Coin Collection' option [$worn].",
            )
        }
    }

    /**
     * The rare drop enhancement itself, which is the ring's headline effect and the one thing here
     * that is a behaviour rather than a table of ids.
     *
     * Asserted as the property the wiki states - wearing the ring "completely removes the chance to
     * receive nothing" from the shared tables - rather than as a rate, so it cannot go flaky. Both
     * directions matter: a flag that did nothing would pass the first half alone.
     */
    @Test
    fun `a ring of wealth removes the Nothing rows from the shared tables`() {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)

        // The rows the flag is supposed to be removing, at their published weights.
        assertEquals(
            63,
            GemDropTable.TABLE.single { it.item == null }.weight,
            "the gem table's Nothing row has moved or changed weight",
        )
        assertEquals(
            113,
            RareDropTable.MEGA_RARE.single { it.item == null }.weight,
            "the mega-rare table's Nothing row has moved or changed weight",
        )

        /*
         * A "Nothing" outcome is the row itself coming back with a null `item`, not a null roll -
         * `DropRoll.pick` returns the entry it landed on either way, and every caller unwraps it with
         * `picked.item?.let`. So it is the *item* that has to be non-null here.
         */
        repeat(ROLLS) {
            assertNotNull(
                GemDropTable.roll(world, wealth = true)?.item,
                "the gem table returned Nothing with a ring of wealth",
            )
            assertNotNull(
                RareDropTable.roll(world, wealth = true)?.item,
                "the rare table returned Nothing with a ring of wealth",
            )
        }

        // And without one it still does - otherwise the flag would be proving nothing above. The gem
        // table's Nothing row is roughly half of it, so a miss in 2000 rolls is impossible.
        assertTrue(
            (1..ROLLS).any { GemDropTable.roll(world)?.item == null },
            "the gem table never returned Nothing without a ring of wealth",
        )
    }

    /** The three currencies the ring collects have to be real, stackable, distinct items. */
    @Test
    fun `the collected currencies are real stackable items`() {
        assertEquals(3, RingOfWealth.currencyIds.size, "two currency keys resolve to the same id")
        RingOfWealth.currencyIds.forEach { id ->
            val def = CacheManager.getItem(id)
            assertTrue(def.name.isNotBlank() && def.name != "null", "currency id $id is a nameless cache entry")
            assertEquals(1, def.stacks, "${def.name} ($id) does not stack, so collecting it would fill the inventory")
        }
    }

    /** The bracelet of clay swaps one item for another; both have to exist and be different. */
    @Test
    fun `clay and soft clay are distinct real items`() {
        val clay = getRSCM("item.clay")
        val soft = getRSCM("item.soft_clay")
        assertTrue(clay > 0 && soft > 0 && clay != soft, "clay=$clay soft_clay=$soft")
        assertEquals("Soft clay", CacheManager.getItem(soft).name)
    }

    /** The three survival pieces have to be in the slots the plugin reads them out of. */
    @Test
    fun `the survival jewellery sits in the slots the plugin checks`() {
        mapOf(
            "item.phoenix_necklace" to EquipmentType.AMULET,
            "item.necklace_of_faith" to EquipmentType.AMULET,
            "item.ring_of_life" to EquipmentType.RING,
            "item.ring_of_recoil" to EquipmentType.RING,
        ).forEach { (key, slot) ->
            val def = CacheManager.getItem(getRSCM(key))
            assertEquals(
                SLOT_BY_TYPE.getValue(slot),
                def.equipSlot,
                "$key ('${def.name}') is worn in slot ${def.equipSlot}, not $slot.",
            )
        }
    }

    /**
     * A destination in a region the cache does not hold would "work" and drop the player into an
     * empty void, so the region has to exist as well as be walkable. [Tile.z] is the y axis here.
     */
    @Test
    fun `every teleport destination is in a region this cache actually holds`() {
        val missing = mutableListOf<String>()
        ChargedJewellery.values.forEach { entry ->
            entry.destinations.forEach { destination ->
                val tile = destination.tile ?: return@forEach
                val rx = tile.x shr 6
                val rz = tile.z shr 6
                if (CacheManager.cache.data(5, "m${rx}_$rz") == null) {
                    missing += "${entry.displayName} -> ${destination.option} at $tile (region ${rx}_$rz)"
                }
            }
        }
        assertTrue(missing.isEmpty(), "Teleport destinations outside this cache:\n  ${missing.joinToString("\n  ")}")
    }
}
