package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.game.model.item.Item
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `Item.unwrapPlaceholder`, and the reason it exists.
 *
 * A bank placeholder is a separate item id whose only job is to hold a bank slot open. It
 * borrows the real item's name and model through its template, so in an inventory it is
 * indistinguishable from the real thing - but it is a different id, so nothing binds to it:
 * an item-on-object plugin, a recipe, a quest check all just miss. The item-search dialog
 * behind the spawn commands and the cheat menu offers placeholders next to real items,
 * which is how one ends up in a player's inventory looking like the item they asked for.
 *
 * The bucket is the worked example: 19107 renders as a Bucket and is what you get if you
 * pick the wrong row when searching for one.
 */
class PlaceholderVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    @Test
    fun `the bucket placeholder resolves to a real bucket`() {
        val bucket = getRSCM("item.bucket")
        val placeholder = CacheManager.getItem(bucket)!!.placeholderLink
        assertEquals(19107, placeholder, "the bucket's placeholder id moved")

        val def = CacheManager.getItem(placeholder)!!
        // The decoder writes the literal string "null" for an item with no name, rather
        // than leaving it null.
        assertEquals("null", def.name, "19107 has a name now, so it is no longer a bare placeholder")
        assertEquals(bucket, def.placeholderLink, "19107 no longer points back at the bucket")

        assertEquals(bucket, Item(placeholder).unwrapPlaceholder().id)
    }

    @Test
    fun `unwrapping keeps the amount and leaves real items alone`() {
        val bucket = getRSCM("item.bucket")
        val placeholder = CacheManager.getItem(bucket)!!.placeholderLink

        assertEquals(7, Item(placeholder, 7).unwrapPlaceholder().amount)
        assertEquals(bucket, Item(bucket).unwrapPlaceholder().id, "a real item must be returned unchanged")
    }

    /**
     * The property the whole thing rests on: a placeholder is never itself something a
     * player can be given, and its link always points at a real, named item.
     */
    @Test
    fun `every placeholder in the cache resolves to a named item`() {
        val broken =
            CacheManager
                .getItems()
                .filter { (_, def) -> def.placeholderTemplate > 0 && def.placeholderLink > 0 }
                .filter { (_, def) -> CacheManager.getItem(def.placeholderLink)?.name.let { it == null || it == "null" } }
                .keys
        assertTrue(broken.isEmpty(), "placeholders pointing at unnamed items: $broken")
    }
}
