package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.plugins.content.npcs.dungeon.DungeonMonsters
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A whole-package audit of `content/npcs/dungeon`'s drop tables, guarding the two mistakes that
 * were found in every one of them.
 *
 * ## 1. A weighted table must add up to its published denominator
 *
 * [org.alter.plugins.content.npcs.DropRoll] picks one row per kill, so a row's real chance is its
 * weight over the **table's total**, not over the 128 its comment claims. Every table here was
 * short - 47, 50, 87, 101, 104, 123, 124, 127 - so every row in them was inflated by up to 2.7x
 * with nothing to say so. A `null`-item filler row is what makes the published denominator real.
 *
 * ## 2. A tertiary is not a table row
 *
 * Tertiaries are independent rolls at their own published chance. Folded into a weighted table at
 * `weight = 1` they become one-in-the-table-total instead, and where a table held *nothing but*
 * tertiaries the single row was picked on every kill. Measured before the fix:
 *
 * - Level 22/25/45 skeletons: `skeleton champion scroll` 1/5000 published, **1 kill in 2** - their
 *   tables held only the scroll and a looting bag.
 * - Ice spider: `tooth half of key` 1/964 published, 1/2 actual.
 * - Ogre chieftain: `long bone` 1/400 published, 1/3 actual.
 * - Lesser demon: champion scroll 1/5000 published, 1/127 actual.
 * - Poison scorpion, poison spider: their one tertiary dropped on **every kill**.
 *
 * And the same bug in reverse: every looting bag was rolled out of its monster's whole table -
 * 1/87 to 1/127 - rather than at its published 1/3 to 1/14, and dropped anywhere rather than only
 * in the Wilderness.
 */
class DungeonDropsVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /**
         * Items that are tertiary rolls on the wiki wherever they appear. If one of these turns up
         * as a weighted row again, its rate is wrong by whatever the table happens to total.
         */
        val TERTIARY_MARKERS =
            listOf(
                "looting_bag",
                "champion_scroll",
                "ensouled_",
                "long_bone",
                "curved_bone",
                "clue_scroll",
                "tooth_half_of_key",
            )

        /** The denominators these pages publish out of. */
        val ALLOWED_TOTALS = setOf(128, 500)
    }

    @Test
    fun `every weighted table adds up to a published denominator`() {
        val failures =
            DungeonMonsters.ALL.filter { it.table.isNotEmpty() }.mapNotNull { monster ->
                val total = monster.table.sumOf { it.weight }
                if (total in ALLOWED_TOTALS) {
                    null
                } else {
                    "${monster.name}: table totals $total, which is not one of $ALLOWED_TOTALS - " +
                        "every row in it is off by ${"%.2f".format(128.0 / total)}x"
                }
            }
        assertTrue(failures.isEmpty(), "Weighted tables that do not add up:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `no tertiary is folded into a weighted table`() {
        val names = HashMap<Int, String>()
        CacheManager.getItems().forEach { (id, def) -> def.name?.let { names[id] = it.lowercase() } }

        val failures = mutableListOf<String>()
        DungeonMonsters.ALL.forEach { monster ->
            monster.table.mapNotNull { it.item }.forEach { id ->
                val name = names[id] ?: return@forEach
                TERTIARY_MARKERS.forEach { marker ->
                    if (name.replace(' ', '_').contains(marker.trim('_'))) {
                        failures += "${monster.name}: '$name' ($id) is a weighted row but should be a tertiary"
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), "Tertiaries folded into weighted tables:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `every tertiary resolves and has a sane chance`() {
        val failures = mutableListOf<String>()
        DungeonMonsters.ALL.forEach { monster ->
            monster.tertiaryDrops.forEach { tertiary ->
                val id = getRSCM(tertiary.item)
                if (id <= 0 || CacheManager.getItem(id) == null) {
                    failures += "${monster.name}: ${tertiary.item} does not resolve to a cache item"
                }
                if (tertiary.chance <= 0.0 || tertiary.chance > 1.0) {
                    failures += "${monster.name}: ${tertiary.item} has chance ${tertiary.chance}"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    /**
     * Looting bags are Wilderness-only on every page that lists one. Getting this wrong is not
     * cosmetic - it hands a free bag to anyone killing these monsters in a dungeon.
     */
    @Test
    fun `every looting bag is Wilderness-only`() {
        val failures =
            DungeonMonsters.ALL.flatMap { monster ->
                monster.tertiaryDrops
                    .filter { it.item == "item.looting_bag" && !it.wildernessOnly }
                    .map { "${monster.name} drops a looting bag outside the Wilderness" }
            }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    /**
     * A monster with no weighted table and no tertiaries drops only its guaranteed items. That is
     * a real published state for some - but not for one whose table was emptied by this fix, so
     * this catches a tertiary list that was stripped out and never wired back in.
     */
    @Test
    fun `nothing lost its drops entirely`() {
        val stripped =
            listOf(
                "Skeleton (level 22)",
                "Skeleton (level 25)",
                "Skeleton (level 45)",
                "Poison scorpion",
                "Poison spider",
                "Ice spider",
                "Ogre chieftain",
            )
        stripped.forEach { name ->
            val monster = DungeonMonsters.ALL.firstOrNull { it.name == name } ?: return@forEach
            assertTrue(
                monster.tertiaryDrops.isNotEmpty(),
                "$name had its whole table moved to tertiaries, so it must have some",
            )
            assertTrue(monster.table.isEmpty(), "$name should have no weighted table left")
        }
    }

    /**
     * Sanity: the fix did not quietly drop a monster out of the roster.
     *
     * The floor was 25 until the second bestiary pass moved five rows out to packages of their own -
     * `Hellhound`, `Lesser demon` and `Greater demon` to `content/npcs/hellhound` and
     * `content/npcs/demon`, `Baby blue dragon` and `Baby black dragon` to `content/npcs/dragon`. That
     * was not a loss: each of those five now has every published version, every published location
     * and its own drop table per drop version, where this file only ever had the `Regular` one. It
     * also removed a real hazard - `PluginRepository.bindNpcDeath` overwrites rather than stacks, so
     * two plugins claiming one id would have let load order decide the drop table. `Bestiary2Verify`
     * asserts that no id is claimed twice.
     */
    @Test
    fun `the roster is intact`() {
        assertTrue(DungeonMonsters.ALL.size >= 22, "only ${DungeonMonsters.ALL.size} dungeon monsters defined")
        DungeonMonsters.ALL.forEach { monster ->
            assertTrue(monster.npcKeys.isNotEmpty(), "${monster.name} has no npc ids")
            monster.npcKeys.forEach { key ->
                assertNotNull(CacheManager.getNpc(getRSCM(key)), "${monster.name}: $key is not in this cache")
            }
        }
    }
}
