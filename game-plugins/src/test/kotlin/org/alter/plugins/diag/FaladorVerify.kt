package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire check for the Falador content: every RSCM key the new plugins use
 * resolves to a real cache entry, every npc really carries the option string those plugins
 * bind with `onNpcOption`, and - because all five Falador shops publish prices as
 * multipliers rather than flat numbers - every shop price is still exactly what the wiki's
 * multiplier produces from this cache's own item cost.
 */
class FaladorVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    private val itemKeys = listOf(
        // Falador General Store.
        "item.pot", "item.jug", "item.empty_jug_pack", "item.shears", "item.bucket",
        "item.empty_bucket_pack", "item.bowl", "item.cake_tin", "item.tinderbox",
        "item.chisel", "item.hammer", "item.newcomer_map", "item.security_book",
        // Cassie's Shield Shop.
        "item.wooden_shield", "item.bronze_sq_shield", "item.bronze_kiteshield",
        "item.iron_sq_shield", "item.iron_kiteshield", "item.steel_sq_shield",
        "item.steel_kiteshield", "item.mithril_sq_shield",
        // Wayne's Chains.
        "item.bronze_chainbody", "item.iron_chainbody", "item.steel_chainbody",
        "item.black_chainbody", "item.mithril_chainbody", "item.adamant_chainbody",
        // Flynn's Mace Market.
        "item.bronze_mace", "item.iron_mace", "item.steel_mace", "item.mithril_mace",
        "item.adamant_mace",
        // Herquin's Gems.
        "item.uncut_sapphire", "item.uncut_emerald", "item.uncut_ruby", "item.uncut_diamond",
        "item.sapphire", "item.emerald", "item.ruby", "item.diamond",
        // Dialogue rewards and ground spawns.
        "item.coins_995", "item.woad_leaf", "item.asgarnian_ale", "item.wizards_mind_bomb",
        "item.dwarven_stout", "item.spade", "item.bronze_axe", "item.cooked_chicken",
        "item.bronze_arrow",
    )

    private val talkToNpcs = listOf(
        "npc.shop_keeper_2819", "npc.shop_assistant_2820", "npc.cassie", "npc.wayne",
        "npc.flynn", "npc.herquin", "npc.sir_amik_varze", "npc.sir_tiffy_cashien",
        "npc.sir_vyvin", "npc.sir_renitee", "npc.wyson_the_gardener", "npc.party_pete",
        "npc.emily", "npc.kaylee",
    )

    private val tradeNpcs = listOf(
        "npc.shop_keeper_2819", "npc.shop_assistant_2820", "npc.cassie", "npc.wayne",
        "npc.flynn", "npc.herquin",
    )

    @Test
    fun `every item key resolves`() {
        itemKeys.forEach { key ->
            val id = getRSCM(key)
            assertNotNull(CacheManager.getItem(id), "$key -> $id has no cache item")
        }
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
     * Generic "Shop keeper"/"Shop assistant" npcs are numbered per shop, and an `onNpcOption`
     * handler is registered against an npc id globally - so if two towns bind the same id,
     * one town's counter opens the other town's shop. Falador surfaced exactly that: Al
     * Kharid's store had been wired to 2819/2820 (Falador's staff) and Varrock's swordshop to
     * 2817 (Al Kharid's keeper). These are the wiki-confirmed ids for each store; keeping them
     * distinct is the invariant that matters.
     */
    @Test
    fun `each general store's counter staff is a distinct npc id`() {
        val perShop = mapOf(
            "Varrock general store" to listOf("npc.shop_keeper_2815", "npc.shop_assistant_2816"),
            "Al Kharid general store" to listOf("npc.shop_keeper_2817", "npc.shop_assistant_2818"),
            "Falador general store" to listOf("npc.shop_keeper_2819", "npc.shop_assistant_2820"),
            "Varrock swordshop" to listOf("npc.shop_keeper_2884"),
        )

        val seen = mutableMapOf<Int, String>()
        perShop.forEach { (shop, keys) ->
            keys.forEach { key ->
                val id = getRSCM(key)
                val def = CacheManager.getNpcs()[id]
                assertNotNull(def, "$key has no cache npc")
                assertTrue(
                    def.actions.any { it?.lowercase() == "trade" },
                    "$key has no Trade option [actions=${def.actions.toList()}]",
                )
                val owner = seen.put(id, shop)
                assertTrue(owner == null, "npc $id backs both '$owner' and '$shop'")
            }
        }
    }

    /**
     * Each entry is item key -> (wiki sell multiplier, wiki buy multiplier, coded sell,
     * coded buy), with the multipliers as published in that shop's `{{StoreTableHead}}`.
     * If a future cache re-dump changes an item's base cost, this fails rather than letting
     * the shop quietly drift away from the wiki's real pricing.
     */
    @Test
    fun `every shop price matches the wiki multiplier applied to this cache's item cost`() {
        data class Priced(val key: String, val sellMul: Int, val buyMul: Int, val sell: Int, val buy: Int)

        val priced = listOf(
            // Falador General Store - sellmultiplier=1300, buymultiplier=400.
            Priced("item.pot", 1300, 400, 1, 0),
            Priced("item.jug", 1300, 400, 1, 0),
            Priced("item.empty_jug_pack", 1300, 400, 182, 56),
            Priced("item.shears", 1300, 400, 1, 0),
            Priced("item.bucket", 1300, 400, 2, 0),
            Priced("item.empty_bucket_pack", 1300, 400, 650, 200),
            Priced("item.bowl", 1300, 400, 5, 1),
            Priced("item.cake_tin", 1300, 400, 13, 4),
            Priced("item.tinderbox", 1300, 400, 1, 0),
            Priced("item.chisel", 1300, 400, 1, 0),
            Priced("item.hammer", 1300, 400, 1, 0),
            Priced("item.newcomer_map", 1300, 400, 1, 0),
            Priced("item.security_book", 1300, 400, 2, 0),
            // Cassie's Shield Shop - sellmultiplier=1000, buymultiplier=600.
            Priced("item.wooden_shield", 1000, 600, 20, 12),
            Priced("item.bronze_sq_shield", 1000, 600, 48, 28),
            Priced("item.bronze_kiteshield", 1000, 600, 68, 40),
            Priced("item.iron_sq_shield", 1000, 600, 168, 100),
            Priced("item.iron_kiteshield", 1000, 600, 238, 142),
            Priced("item.steel_sq_shield", 1000, 600, 600, 360),
            Priced("item.steel_kiteshield", 1000, 600, 850, 510),
            Priced("item.mithril_sq_shield", 1000, 600, 1560, 936),
            // Wayne's Chains - sellmultiplier=1000, buymultiplier=650.
            Priced("item.bronze_chainbody", 1000, 650, 60, 39),
            Priced("item.iron_chainbody", 1000, 650, 210, 136),
            Priced("item.steel_chainbody", 1000, 650, 750, 487),
            Priced("item.black_chainbody", 1000, 650, 1440, 936),
            Priced("item.mithril_chainbody", 1000, 650, 1950, 1267),
            Priced("item.adamant_chainbody", 1000, 650, 4800, 3120),
            // Flynn's Mace Market - sellmultiplier=1000, buymultiplier=600.
            Priced("item.bronze_mace", 1000, 600, 18, 10),
            Priced("item.iron_mace", 1000, 600, 63, 37),
            Priced("item.steel_mace", 1000, 600, 225, 135),
            Priced("item.mithril_mace", 1000, 600, 585, 351),
            Priced("item.adamant_mace", 1000, 600, 1440, 864),
            // Herquin's Gems - sellmultiplier=1000, buymultiplier=700.
            Priced("item.uncut_sapphire", 1000, 700, 25, 17),
            Priced("item.uncut_emerald", 1000, 700, 50, 35),
            Priced("item.uncut_ruby", 1000, 700, 100, 70),
            Priced("item.uncut_diamond", 1000, 700, 200, 140),
            Priced("item.sapphire", 1000, 700, 250, 175),
            Priced("item.emerald", 1000, 700, 500, 350),
            Priced("item.ruby", 1000, 700, 1000, 700),
            Priced("item.diamond", 1000, 700, 2000, 1400),
        )

        priced.forEach { p ->
            val cost = CacheManager.getItem(getRSCM(p.key))!!.cost
            assertEquals(cost * p.sellMul / 1000, p.sell, "${p.key} sell price drifted from cost $cost")
            assertEquals(cost * p.buyMul / 1000, p.buy, "${p.key} buy price drifted from cost $cost")
        }
    }
}
