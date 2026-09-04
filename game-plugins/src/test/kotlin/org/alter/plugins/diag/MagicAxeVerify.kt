package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.plugins.content.npcs.dungeon.DungeonDrops
import org.alter.plugins.content.npcs.dungeon.DungeonMonsters
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The magic axe, whose two versions had been collapsed into one.
 *
 * The OSRS Wiki gives the magic axe **two separate drop sections** - `==Drops==` for the normal
 * axe (2844) and `==Catacombs drops==` for the Catacombs of Kourend one (7269) - and they have
 * nothing in common. The normal axe drops an iron battleaxe on every kill and rolls no table; the
 * Catacombs axe rolls its battleaxe out of five and drops nothing guaranteed.
 *
 * Both ids used to share one entry that had the guaranteed axe *and* the Catacombs table *and*
 * the Wilderness looting bag folded in as a table row, so a normal magic axe paid out an iron
 * battleaxe about twice per kill and a looting bag at 1/501 anywhere instead of 1/3 in the
 * Wilderness.
 */
class MagicAxeVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    private val normal get() = DungeonMonsters.ALL.first { it.name == "Magic axe" }
    private val catacombs get() = DungeonMonsters.ALL.first { it.name == "Magic axe (Catacombs of Kourend)" }

    @Test
    fun `the two versions are separate entries on their own ids`() {
        assertEquals(listOf("npc.magic_axe"), normal.npcKeys)
        assertEquals(listOf("npc.magic_axe_7269"), catacombs.npcKeys)

        listOf(normal, catacombs).forEach { axe ->
            axe.npcKeys.forEach { key ->
                val id = getRSCM(key)
                assertTrue(id > 0, "$key did not resolve to an npc id.")
                val def = CacheManager.getNpc(id)
                assertNotNull(def, "$key ($id) is not in this cache.")
                assertEquals("Magic axe", def.name, "$key ($id) is not named Magic axe.")
            }
        }
    }

    /** The page publishes one unversioned combat block, so the two must not drift apart. */
    @Test
    fun `the two versions share every combat number`() {
        assertEquals(normal.hitpoints, catacombs.hitpoints)
        assertEquals(normal.attack, catacombs.attack)
        assertEquals(normal.strength, catacombs.strength)
        assertEquals(normal.defence, catacombs.defence)
        assertEquals(normal.attackSpeed, catacombs.attackSpeed)
        assertEquals(normal.combatStyle, catacombs.combatStyle)
        assertEquals(normal.slayerXp, catacombs.slayerXp)
        assertEquals(normal.respawnCycles, catacombs.respawnCycles)
        assertEquals(normal.aggroRadius, catacombs.aggroRadius)
    }

    /**
     * The double-drop bug: the guaranteed iron battleaxe and the 475/500 iron battleaxe row were
     * both on the same monster.
     */
    @Test
    fun `the normal axe has its guaranteed drop and no table`() {
        assertEquals(listOf("item.iron_battleaxe"), normal.guaranteedDrops)
        assertTrue(
            normal.table.isEmpty(),
            "the normal magic axe has a weighted table again; the five-battleaxe roll is the " +
                "Catacombs version's, and with the guaranteed axe it pays out twice a kill",
        )
    }

    @Test
    fun `the Catacombs axe rolls the battleaxe table and guarantees nothing`() {
        assertTrue(catacombs.guaranteedDrops.isEmpty(), "the Catacombs axe has no 100% drop on the wiki")
        assertEquals(DungeonDrops.MAGIC_AXE_CATACOMBS, catacombs.table)
        assertEquals(500, catacombs.table.sumOf { it.weight }, "the battleaxe table is published out of 500")
        assertEquals(
            mapOf<Int?, Int>(
                getRSCM("item.iron_battleaxe") to 475,
                getRSCM("item.steel_battleaxe") to 10,
                getRSCM("item.mithril_battleaxe") to 10,
                getRSCM("item.adamant_battleaxe") to 4,
                getRSCM("item.rune_battleaxe") to 1,
            ),
            catacombs.table.associate { it.item to it.weight },
        )
    }

    /**
     * The looting bag was a weighted row at 1/501, dropped anywhere. It is Wilderness-only at 1/3,
     * and it belongs to the normal axe alone - the Catacombs is not in the Wilderness.
     */
    @Test
    fun `the tertiaries are on the right version, at the right rates`() {
        assertEquals(1, normal.tertiaryDrops.size)
        normal.tertiaryDrops.single().let { bag ->
            assertEquals("item.looting_bag", bag.item)
            assertEquals(1.0 / 3.0, bag.chance)
            assertTrue(bag.wildernessOnly, "looting bags are only dropped by those found in the Wilderness")
        }
        assertTrue(
            DungeonDrops.MAGIC_AXE_CATACOMBS.none { it.item == getRSCM("item.looting_bag") },
            "the looting bag is back in the weighted table, so it drops at 1/501 anywhere",
        )

        assertEquals(1, catacombs.tertiaryDrops.size)
        catacombs.tertiaryDrops.single().let { clue ->
            assertEquals("item.clue_scroll_medium", clue.item)
            assertEquals(1.0 / 256.0, clue.chance)
            assertTrue(!clue.wildernessOnly, "the Catacombs is not in the Wilderness")
        }
        assertNotNull(
            CacheManager.getItem(getRSCM("item.clue_scroll_medium")),
            "item.clue_scroll_medium is not in this cache",
        )
    }
}
