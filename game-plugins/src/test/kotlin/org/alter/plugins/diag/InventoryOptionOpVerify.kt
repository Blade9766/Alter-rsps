package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.rscm.RSCM
import org.alter.game.plugin.KotlinPlugin
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The inventory's op numbering, which `KotlinPlugin.onItemOption` and `OSRSPlugin`'s login
 * op mask both have to agree on.
 *
 * Item options are not op1..op5. Interface 149's own ops share the numbering - op7 is Drop
 * and op10 Examine - and the item's five options start at **op2**, so the option at index
 * `i` arrives as op `2 + i`. Getting that wrong is silent in every direction: the binding
 * registers against an op the client never sends, no error is logged, and the option simply
 * does nothing when clicked.
 *
 * Two facts in the codebase pin the offset, and both are asserted here so a change to either
 * shows up as a failing test rather than as dead right-click options:
 *
 *  - `InventoryPlugin` routes op **3** to `EquipAction`, and "Wield"/"Wear" sits at index 1.
 *  - `MysteryBoxPlugin` binds op **2** by raw number, and its "Open" sits at index 0.
 */
class InventoryOptionOpVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /**
         * Delegates to `KotlinPlugin.inventoryOpOf` rather than restating the formula.
         *
         * This constant used to be a local copy of a flat `2 + index`, which is wrong for
         * indices 3 and 4 - op 5 is skipped, so they arrive as 6 and 7. The copy meant the test
         * agreed with the binding code and both disagreed with the client, so every option at
         * those indices was dead and nothing failed. Measured against two items across all five
         * indices; see the mapping table on `KotlinPlugin.inventoryOpOf`.
         */
        fun inventoryOpOf(index: Int) = KotlinPlugin.inventoryOpOf(index)

        const val INVENTORY_OP_OFFSET = KotlinPlugin.INVENTORY_OP_OFFSET

        /** The op `InventoryPlugin` hands to `EquipAction`. */
        const val EQUIP_OP = 3

        /** The op `MysteryBoxPlugin` binds by raw number. */
        const val MYSTERY_BOX_OP = 2
    }

    private fun opOf(
        item: String,
        option: String,
    ): Int {
        val def = CacheManager.getItem(getRSCM(item))!!
        val index = def.interfaceOptions.indexOfFirst { it.equals(option, ignoreCase = true) }
        assertEquals(true, index != -1, "$item has no '$option' option [${def.interfaceOptions.toList()}]")
        return inventoryOpOf(index)
    }

    @Test
    fun `wielding lands on the op InventoryPlugin routes to EquipAction`() {
        assertEquals(EQUIP_OP, opOf("item.bronze_sword", "Wield"))
        assertEquals(EQUIP_OP, opOf("item.amulet_of_glory4", "Wear"))
    }

    @Test
    fun `the mystery box lands on the op it is bound to by raw number`() {
        assertEquals(MYSTERY_BOX_OP, opOf("item.mystery_box", "Open"))
    }

    /**
     * Every container's "Empty" sits at index 3, which the client sends as op **6** - op 5 is
     * skipped. This asserted op5 while the binding code also computed 5, so the two agreed with
     * each other and neither agreed with the client, and emptying a container did nothing.
     * Measured; see the mapping table on `KotlinPlugin.inventoryOpOf`.
     */
    @Test
    fun `every container empties on op6`() {
        listOf(
            "item.bucket_of_water",
            "item.bucket_of_milk",
            "item.jug_of_water",
            "item.bowl_of_water",
            "item.vial_of_water",
        ).forEach { assertEquals(6, opOf(it, "Empty"), "$it does not empty on op6") }
    }

    /**
     * The measured mapping itself, pinned directly. Index 3 and 4 are the two that were wrong,
     * and they are the reason "Rub", "Empty", "Settings", "Destroy" and "Break" were all dead.
     */
    @Test
    fun `the inventory op mapping skips op5`() {
        assertEquals(listOf(2, 3, 4, 6, 7), (0..4).map { inventoryOpOf(it) })
    }

    @Test
    fun `eating and drinking land on op2`() {
        assertEquals(2, opOf("item.shrimps", "Eat"))
        assertEquals(2, opOf("item.jug_of_wine", "Drink"))
    }
}
