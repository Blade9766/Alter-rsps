package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.plugins.content.npcs.ardougne.ArdougneKnightData
import org.alter.plugins.content.npcs.guard.CityGuards
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for the East Ardougne content: shops, the knights and paladins,
 * and the ground spawns.
 */
class ArdougneVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    private val itemKeys = listOf(
        // Aemad's Adventuring Supplies.
        "item.vial_of_water", "item.waterfilled_vial_pack", "item.bronze_pickaxe",
        "item.iron_axe", "item.cooked_meat", "item.tinderbox", "item.ball_of_wool",
        "item.bronze_arrow", "item.rope", "item.papyrus", "item.knife",
        // Zenesha's Plate Mail Body Shop.
        "item.bronze_platebody", "item.iron_platebody", "item.steel_platebody",
        "item.black_platebody", "item.mithril_platebody",
        // Ardougne Fur Stall.
        "item.bear_fur", "item.grey_wolf_fur",
        // Paladin drops and ground spawns.
        "item.steel_sword", "item.steel_longsword", "item.steel_full_helm", "item.water_rune",
        "item.blood_rune", "item.iron_bar", "item.mithril_bar", "item.steel_bar",
        "item.bones", "item.coins_995", "item.chisel", "item.hammer",
    )

    @Test
    fun `every item key resolves`() {
        itemKeys.forEach { key ->
            val id = getRSCM(key)
            assertNotNull(CacheManager.getItem(id), "$key -> $id has no cache item")
        }
    }

    /**
     * `npc.zenesha` (4584) is her Ratcatchers quest variant, which stands in a mansion and
     * has no options at all - the shopkeeper is 8681. Pinning both halves so the plugin
     * can't be "simplified" back onto the obvious-looking key.
     */
    @Test
    fun `zenesha's shop npc is the 8681 variant, not the inert 4584`() {
        val inert = CacheManager.getNpcs()[getRSCM("npc.zenesha")]!!
        assertTrue(
            inert.actions.all { it == null },
            "npc.zenesha (4584) unexpectedly has options ${inert.actions.toList()}",
        )

        val shopkeeper = CacheManager.getNpcs()[getRSCM("npc.zenesha_8681")]!!
        assertTrue(
            shopkeeper.actions.any { it?.lowercase() == "trade" },
            "npc.zenesha_8681 has no Trade option [actions=${shopkeeper.actions.toList()}]",
        )
    }

    @Test
    fun `every shopkeeper has talk-to and trade`() {
        listOf("npc.aemad", "npc.kortan", "npc.zenesha_8681", "npc.fur_trader").forEach { key ->
            val def = CacheManager.getNpcs()[getRSCM(key)]
            assertNotNull(def, "$key has no cache npc")
            assertTrue(
                def.actions.any { it?.lowercase() == "talk-to" },
                "$key has no Talk-to [actions=${def.actions.toList()}]",
            )
            assertTrue(
                def.actions.any { it?.lowercase() == "trade" },
                "$key has no Trade [actions=${def.actions.toList()}]",
            )
        }
    }

    @Test
    fun `every knight and paladin is attackable and matches its wiki combat level`() {
        ArdougneKnightData.GROUPS.forEach { group ->
            group.npcKeys.forEach { key ->
                val def = CacheManager.getNpcs()[getRSCM(key)]
                assertNotNull(def, "$key has no cache npc")
                assertTrue(
                    def.actions.any { it?.lowercase() == "attack" },
                    "$key is not attackable [actions=${def.actions.toList()}]",
                )
                assertEquals(
                    group.combatLevel,
                    def.combatLevel,
                    "$key cache level ${def.combatLevel} != wiki ${group.combatLevel}",
                )
            }
        }
    }

    @Test
    fun `spawn counts match the wiki's pin counts`() {
        // 5 East Ardougne knight pins.
        assertEquals(5, ArdougneKnightData.SPAWNS.count { it.npcKey in ArdougneKnightData.KNIGHT.npcKeys })
        // 2 market + 7 castle ground + 13 castle first floor.
        val paladins = ArdougneKnightData.SPAWNS.filter { it.npcKey in ArdougneKnightData.PALADIN.npcKeys }
        assertEquals(22, paladins.size, "paladin spawn count")
        assertEquals(9, paladins.count { it.height == 0 }, "ground-floor paladins")
        assertEquals(13, paladins.count { it.height == 1 }, "first-floor paladins")
    }

    @Test
    fun `no two ardougne combat spawns share a tile`() {
        val tiles = ArdougneKnightData.SPAWNS.map { Triple(it.x, it.z, it.height) }
        val duplicates = tiles.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(duplicates.isEmpty(), "duplicate spawn tiles: $duplicates")
    }

    /** Mirrors the coordinates the areas/ardougne plugins spawn on, plus the city guards. */
    @Test
    fun `no combat spawn lands on a town npc or ground item`() {
        val townTiles =
            setOf(
                Triple(2614, 3293, 0), // Aemad
                Triple(2615, 3293, 0), // Kortan
                Triple(2653, 3294, 0), // Zenesha
                Triple(2664, 3296, 0), // Fur trader
                Triple(2683, 3318, 0), // Chisel spawn
                Triple(2684, 3318, 0), // Hammer spawn
            )

        val guardTiles = CityGuards.ARDOUGNE.spawns.map { Triple(it.x, it.z, it.height) }.toSet()
        val combatTiles = ArdougneKnightData.SPAWNS.map { Triple(it.x, it.z, it.height) }

        val clashes = combatTiles.filter { it in townTiles || it in guardTiles }
        assertTrue(clashes.isEmpty(), "knights/paladins spawned on an occupied tile: $clashes")
    }

    /**
     * The whole point of placing these two: `pickpockets.json` has carried level 55 and
     * level 70 entries for them all along with nothing in the world to pickpocket. Every id
     * that is pickpocketable in the cache must be listed there, or spawning it gives players
     * a menu option that silently does nothing.
     */
    @Test
    fun `every knight and paladin is pickpocketable and wired into the thieving config`() {
        val config = Files.readString(Paths.get("../data", "cfg", "thieving", "pickpockets.json"))

        ArdougneKnightData.GROUPS.flatMap { it.npcKeys }.forEach { key ->
            val def = CacheManager.getNpcs()[getRSCM(key)]!!
            assertTrue(
                def.actions.any { it?.lowercase() == "pickpocket" },
                "$key has no Pickpocket option [actions=${def.actions.toList()}]",
            )
            assertTrue(
                config.contains("\"$key\""),
                "$key is pickpocketable in the cache but missing from pickpockets.json",
            )
        }
    }

    /**
     * Both Ardougne shops that use a multiplier schedule, checked against this cache's own
     * item costs. The fur trader's 950 buy-back is the highest in the project and the most
     * likely to look like a typo later, so it is pinned explicitly.
     */
    @Test
    fun `every shop price matches the wiki multiplier applied to this cache's item cost`() {
        data class Priced(val key: String, val sellMul: Int, val buyMul: Int, val sell: Int, val buy: Int)

        val priced = listOf(
            // Aemad's - 1300/400.
            Priced("item.vial_of_water", 1300, 400, 2, 0),
            Priced("item.waterfilled_vial_pack", 1300, 400, 261, 80),
            Priced("item.bronze_pickaxe", 1300, 400, 1, 0),
            Priced("item.iron_axe", 1300, 400, 72, 22),
            Priced("item.cooked_meat", 1300, 400, 5, 1),
            Priced("item.tinderbox", 1300, 400, 1, 0),
            Priced("item.ball_of_wool", 1300, 400, 2, 0),
            Priced("item.bronze_arrow", 1300, 400, 1, 0),
            Priced("item.rope", 1300, 400, 23, 7),
            Priced("item.papyrus", 1300, 400, 13, 4),
            Priced("item.knife", 1300, 400, 7, 2),
            // Zenesha's - 1000/600.
            Priced("item.bronze_platebody", 1000, 600, 160, 96),
            Priced("item.iron_platebody", 1000, 600, 560, 336),
            Priced("item.steel_platebody", 1000, 600, 2000, 1200),
            Priced("item.black_platebody", 1000, 600, 3840, 2304),
            Priced("item.mithril_platebody", 1000, 600, 5200, 3120),
            // Fur trader - 1200/950, the most generous buy-back in the project.
            Priced("item.bear_fur", 1200, 950, 12, 9),
            Priced("item.grey_wolf_fur", 1200, 950, 60, 47),
        )

        priced.forEach { p ->
            val cost = CacheManager.getItem(getRSCM(p.key))!!.cost
            assertEquals(cost * p.sellMul / 1000, p.sell, "${p.key} sell price drifted from cost $cost")
            assertEquals(cost * p.buyMul / 1000, p.buy, "${p.key} buy price drifted from cost $cost")
        }
    }
}
