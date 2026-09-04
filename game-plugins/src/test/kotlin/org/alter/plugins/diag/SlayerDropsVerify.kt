package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.plugins.content.npcs.slayer.SlayerMonster
import org.alter.plugins.content.npcs.slayer.SlayerMonsters
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The audit [DungeonDropsVerify] applies to `content/npcs/dungeon`, applied to the Slayer Tower and
 * Fremennik Slayer Dungeon monsters - which needed it, because 14 of their 16 tables were short.
 *
 * ## Why a short table is a bug
 *
 * [org.alter.plugins.content.npcs.DropRoll] picks one row per kill weighted against the **table's
 * own total**, not against the published denominator. The gem, herb, seed and rare tables are rows
 * *inside* that denominator on the wiki, but
 * [org.alter.plugins.content.npcs.slayer.SlayerMonsterPlugin] rolls them separately and
 * unconditionally alongside the main roll. Leaving their share out of the main table therefore did
 * two things at once: every remaining row was inflated by the ratio, and the sub-table drop arrived
 * as an *extra* item rather than instead of one.
 *
 * Measured before the fix: the aberrant spectre's table summed to 26, so every row on it was
 * 128/26 - nearly five times - too common. The crawling hand summed to 80 and dropped leather
 * gloves at 21/80 (26%) against a published 21/128 (16%). Only the infernal mage added up.
 *
 * ## The two invariants
 *
 * `every table adds up to its published denominator` is the headline. `the null rows account for
 * exactly the published Nothing plus the separately-rolled share` is the one that stops the first
 * from being satisfied with any arbitrary number - it pins the filler to the sub-table rates the
 * monster actually declares.
 */
class SlayerDropsVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /**
         * Published denominators, which are **not** all 128 - the kurask's page publishes out of
         * 124, and the nechryael's out of 116 with every weight doubled here so that its two
         * fractional coin rows (10.5, 2.5) become integers.
         */
        val DENOMINATOR =
            mapOf("Kurask" to 124, "Nechryael" to 232).withDefault { 128 }

        /**
         * `Nothing` rows the wiki actually publishes, as opposed to the filler standing in for
         * rows rolled elsewhere. Only these four monsters have one.
         */
        val PUBLISHED_NOTHING =
            mapOf(
                "Infernal Mage" to 16,
                "Aberrant spectre" to 18,
                "Cave crawler" to 29,
                "Cockatrice" to 1,
            )

        /**
         * The crawling hand is the one page that does not publish a full denominator: its rows plus
         * its gem table come to 82/128 and it simply never says what the other 46 is, so its filler
         * is legitimately larger than its sub-table share. Every other page adds up.
         */
        val UNDER_PUBLISHED = mapOf("Crawling Hand" to 46)

        /** Borrowed from [DungeonDropsVerify]: things that are tertiary rolls wherever they appear. */
        val TERTIARY_MARKERS = listOf("champion_scroll", "ensouled_", "clue_scroll", "looting_bag")
    }

    /** Monsters share tables, so audit each distinct table once per monster that declares it. */
    private fun withTables(): List<SlayerMonster> = SlayerMonsters.ALL.filter { it.table.isNotEmpty() }

    /** The share of the denominator this monster rolls outside its main table, in denominator units. */
    private fun subTableShare(m: SlayerMonster): Double {
        val denom = DENOMINATOR.getValue(m.name)
        val chances =
            listOf(m.gemTableChance, m.herbTableChance, m.rareTableChance, m.seedRoll?.chance)
        return chances.filterNotNull().sumOf { it } * denom
    }

    @Test
    fun `every table adds up to its published denominator`() {
        val failures =
            withTables().mapNotNull { m ->
                val total = m.table.sumOf { it.weight }
                val denom = DENOMINATOR.getValue(m.name)
                if (total == denom) {
                    null
                } else {
                    "${m.name}: table totals $total, not $denom - every row on it is off by " +
                        "%.2fx".format(denom.toDouble() / total)
                }
            }
        assertTrue(failures.isEmpty(), "Slayer tables that do not add up:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `the null rows account for exactly the published Nothing plus the separately-rolled share`() {
        val failures =
            withTables().mapNotNull { m ->
                val nulls = m.table.filter { it.item == null }.sumOf { it.weight }
                val published = PUBLISHED_NOTHING[m.name] ?: 0
                val unpublished = UNDER_PUBLISHED.entries.firstOrNull { m.name.startsWith(it.key) }?.value ?: 0
                val expected = published + subTableShare(m) + unpublished
                if (kotlin.math.abs(nulls - expected) < 0.01) {
                    null
                } else {
                    "${m.name}: %d weight of no-drop rows, but published Nothing %d + sub-tables %.2f + unpublished %d = %.2f"
                        .format(nulls, published, subTableShare(m), unpublished, expected)
                }
            }
        assertTrue(failures.isEmpty(), "Filler rows that do not match their sub-table share:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `no tertiary is folded into a weighted table`() {
        val failures = mutableListOf<String>()
        withTables().forEach { m ->
            m.table.mapNotNull(WeightedDrop::item).forEach { id ->
                val name = CacheManager.getItem(id)?.name?.lowercase()?.replace(' ', '_') ?: return@forEach
                TERTIARY_MARKERS.forEach { marker ->
                    if (name.contains(marker.trim('_'))) {
                        failures += "${m.name}: '$name' ($id) is a weighted row but should be a tertiary"
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `every weighted row resolves to a cache item`() {
        val failures =
            withTables().flatMap { m ->
                m.table.mapNotNull(WeightedDrop::item)
                    .filter { it <= 0 || CacheManager.getItem(it) == null }
                    .map { "${m.name}: item id $it is not in this cache" }
            }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
