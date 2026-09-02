package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.plugins.content.npcs.guard.CityGuards
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for the Edgeville content: every RSCM key resolves, every npc
 * really carries the option string its plugin binds, and - since both Edgeville shops are
 * multiplier shops - every price still equals the wiki's multiplier applied to this cache's
 * own item cost.
 */
class EdgevilleVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    private val itemKeys = listOf(
        // Edgeville General Store.
        "item.pot", "item.jug", "item.empty_jug_pack", "item.shears", "item.bucket",
        "item.empty_bucket_pack", "item.bowl", "item.cake_tin", "item.tinderbox",
        "item.chisel", "item.hammer", "item.knife", "item.newcomer_map", "item.security_book",
        // Oziach's Armour.
        "item.rune_platebody", "item.green_dhide_body", "item.antidragon_shield",
        // Ground spawns.
        "item.iron_mace", "item.leather_gloves",
    )

    private val talkToNpcs = listOf(
        "npc.shop_keeper_2821", "npc.shop_assistant_2822", "npc.oziach", "npc.doris",
        "npc.hari", "npc.abbot_langley", "npc.brother_jered", "npc.brother_althric",
    )

    private val tradeNpcs = listOf("npc.shop_keeper_2821", "npc.shop_assistant_2822", "npc.oziach")

    @Test
    fun `every item key resolves`() {
        itemKeys.forEach { key ->
            val id = getRSCM(key)
            assertNotNull(CacheManager.getItem(id), "$key -> $id has no cache item")
        }
    }

    /**
     * The wiki names the shield "Anti-dragon shield" but this cache's RSCM key has no
     * hyphen and no underscore between "anti" and "dragon" - `item.anti_dragon_shield` does
     * not resolve at all. Worth pinning, since the natural guess is wrong.
     */
    @Test
    fun `the anti-dragon shield key is the unhyphenated one`() {
        val def = CacheManager.getItem(getRSCM("item.antidragon_shield"))
        assertNotNull(def, "item.antidragon_shield does not resolve")
        assertEquals("Anti-dragon shield", def.name)
    }

    @Test
    fun `every talked-to npc really has a talk-to option`() {
        talkToNpcs.forEach { key ->
            val def = CacheManager.getNpcs()[getRSCM(key)]
            assertNotNull(def, "$key has no cache npc")
            assertTrue(
                def.actions.any { it?.lowercase() == "talk-to" },
                "$key has no Talk-to option [actions=${def.actions.toList()}]",
            )
        }
    }

    @Test
    fun `every shopkeeper really has a trade option`() {
        tradeNpcs.forEach { key ->
            val def = CacheManager.getNpcs()[getRSCM(key)]!!
            assertTrue(
                def.actions.any { it?.lowercase() == "trade" },
                "$key has no Trade option [actions=${def.actions.toList()}]",
            )
        }
    }

    /**
     * Both Edgeville shops publish `sellmultiplier=1300|buymultiplier=400`, with no per-item
     * price anywhere in the wikitext. If a cache re-dump changes an item's base cost, this
     * fails rather than letting the shop drift away from the wiki's real pricing.
     */
    @Test
    fun `every shop price matches the wiki multiplier applied to this cache's item cost`() {
        data class Priced(val key: String, val sell: Int, val buy: Int)

        val priced = listOf(
            // Edgeville General Store.
            Priced("item.pot", 1, 0),
            Priced("item.jug", 1, 0),
            Priced("item.empty_jug_pack", 182, 56),
            Priced("item.shears", 1, 0),
            Priced("item.bucket", 2, 0),
            Priced("item.empty_bucket_pack", 650, 200),
            Priced("item.bowl", 5, 1),
            Priced("item.cake_tin", 13, 4),
            Priced("item.tinderbox", 1, 0),
            Priced("item.chisel", 1, 0),
            Priced("item.hammer", 1, 0),
            Priced("item.knife", 7, 2),
            Priced("item.newcomer_map", 1, 0),
            Priced("item.security_book", 2, 0),
            // Oziach's Armour - the same 1300/400 schedule, which is why a rune platebody
            // sells above its 65,000 base value.
            Priced("item.rune_platebody", 84500, 26000),
            Priced("item.green_dhide_body", 10140, 3120),
            Priced("item.antidragon_shield", 26, 8),
        )

        priced.forEach { p ->
            val cost = CacheManager.getItem(getRSCM(p.key))!!.cost
            assertEquals(cost * 1300 / 1000, p.sell, "${p.key} sell price drifted from cost $cost")
            assertEquals(cost * 400 / 1000, p.buy, "${p.key} buy price drifted from cost $cost")
        }
    }

    /**
     * The town npcs must not land on the Edgeville guards from `content/npcs/guard`, nor on
     * each other. These mirror the coordinates the areas/edgeville plugins spawn on.
     */
    @Test
    fun `no edgeville npc or item spawn collides`() {
        val townTiles =
            listOf(
                Triple(3080, 3510, 0), // Shop keeper
                Triple(3081, 3510, 0), // Shop assistant
                Triple(3070, 3517, 0), // Oziach
                Triple(3079, 3492, 0), // Doris
                Triple(3132, 3509, 0), // Hari
                Triple(3052, 3490, 0), // Abbot Langley
                Triple(3052, 3491, 1), // Brother Jered
                Triple(3052, 3505, 0), // Brother Althric
            )

        val duplicates = townTiles.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(duplicates.isEmpty(), "two Edgeville npcs share a tile: $duplicates")

        val guardTiles = CityGuards.EDGEVILLE.spawns.map { Triple(it.x, it.z, it.height) }.toSet()
        val clashes = townTiles.filter { it in guardTiles }
        assertTrue(clashes.isEmpty(), "Edgeville npcs spawned on guard tiles: $clashes")

        val itemTiles = listOf(Triple(3111, 3517, 0), Triple(3097, 3486, 0))
        val itemClashes = itemTiles.filter { it in guardTiles || it in townTiles }
        assertTrue(itemClashes.isEmpty(), "ground items spawned under an npc: $itemClashes")
    }
}
