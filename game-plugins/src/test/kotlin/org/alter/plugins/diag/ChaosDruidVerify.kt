package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.chaosdruid.ChaosDruidPlugin
import org.alter.plugins.content.npcs.chaosdruid.ChaosDruids
import org.alter.plugins.content.npcs.dungeon.DungeonMonsters
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The chaos druid's own audit, and the reason it exists is that moving the monster out of
 * `content/npcs/dungeon` moved it out of [DungeonDropsVerify]'s reach - that test walks
 * `DungeonMonsters.ALL`, so the chaos druid silently lost the two guards every table in this
 * codebase is held to. This restores them and adds the ones only this monster needs.
 *
 * The two inherited guards, and why they matter, are written up in [DungeonDropsVerify]: a
 * weighted table must total its published denominator, or [org.alter.plugins.content.npcs.DropRoll]
 * inflates every row in it; and a tertiary must not be a weighted row, or its rate becomes
 * one-in-the-table-total instead of its own published chance.
 */
class ChaosDruidVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** Every rate on the page is published against this. */
        private const val DENOMINATOR = 128
    }

    /**
     * 21 runes + 14 coins + 13 other + 33 published `Nothing` + 46 herb + 1 gem = 128.
     *
     * The herb and gem rates are quoted as their own chances rather than as rows, and are rolled
     * separately, so their share of the denominator has to sit in the `Nothing` row for the rows
     * that *are* here to keep their published rarities.
     */
    @Test
    fun `the main table totals the published denominator`() {
        assertEquals(DENOMINATOR, ChaosDruids.TABLE.sumOf { it.weight }, "chaos druid table total")
    }

    @Test
    fun `the herb split is the published 35 and 11 out of 128`() {
        assertEquals(35, ChaosDruids.ONE_HERB_THRESHOLD, "one-herb rows")
        assertEquals(46, ChaosDruids.TWO_HERB_THRESHOLD, "one- plus two-herb rows")
        val twoHerbRows = ChaosDruids.TWO_HERB_THRESHOLD - ChaosDruids.ONE_HERB_THRESHOLD
        assertEquals(11, twoHerbRows, "two-herb rows")

        /*
         * The page states the consequence as well as the rates - "an average of 1.23 herbs per
         * roll, and 0.4453 herbs per druid kill" - which is worth asserting, because it is the one
         * number that would catch the thresholds being read as two independent chances instead of
         * one roll with two outcomes.
         */
        val perRoll = (ChaosDruids.ONE_HERB_THRESHOLD + 2.0 * twoHerbRows) / ChaosDruids.TWO_HERB_THRESHOLD
        val perKill = (ChaosDruids.ONE_HERB_THRESHOLD + 2.0 * twoHerbRows) / DENOMINATOR

        /*
         * Per kill is the exact one and is what pins the 35/11 split down: 57/128 is 0.4453125,
         * matching the published 0.4453 to every digit given. Per roll is checked loosely on
         * purpose - 57/46 is 1.2391, and the page's "1.23" is a truncation of it, not a rounding
         * (rounding would give 1.24). The tolerance is wide enough to accept the truncation and
         * still far too tight to accept a wrong numerator.
         */
        assertEquals(0.4453, perKill, 0.00005, "herbs per kill")
        assertEquals(1.2391, perRoll, 0.0005, "herbs per roll")
    }

    /**
     * A tertiary rolled as a weighted row would drop at one-in-the-table-total rather than its own
     * rate. The looting bag (1/11) and the ensouled head (1/35) are both tertiaries here.
     */
    @Test
    fun `no tertiary is a weighted row`() {
        val tertiaryIds = ChaosDruids.TERTIARY_DROPS.map { getRSCM(it.item) }.toSet()
        ChaosDruids.TABLE.forEach { row ->
            assertFalse(row.item in tertiaryIds, "tertiary item ${row.item} must not be a weighted row")
        }
        assertEquals(2, ChaosDruids.TERTIARY_DROPS.size, "tertiary count")
        assertTrue(
            ChaosDruids.TERTIARY_DROPS.single { it.item == "item.looting_bag" }.wildernessOnly,
            "the looting bag is Wilderness-only - only the Edgeville Dungeon druids drop it",
        )
    }

    /**
     * These are `object` fields, so a mistyped key would otherwise first surface inside a death
     * handler on somebody's first kill. See the same warning in
     * `org.alter.plugins.content.npcs.RareDropTable`.
     */
    @Test
    fun `every drop key resolves`() {
        ChaosDruids.GUARANTEED_DROPS.forEach { assertNotNull(getRSCM(it), it) }
        ChaosDruids.TERTIARY_DROPS.forEach { assertNotNull(getRSCM(it.item), it.item) }
        assertTrue(ChaosDruids.TABLE.any { it.item == null }, "the table needs its Nothing row")
        ChaosDruids.TABLE.mapNotNull { it.item }.forEach { assertTrue(it > 0, "resolved item id") }
    }

    /**
     * The stat block, read straight off the `Infobox Monster`, as it ends up in the live combat
     * def rather than as constants agreeing with themselves.
     */
    @Test
    fun `the plugin registers the wiki stat block`() {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        val repo = PluginRepository(world)
        ChaosDruidPlugin(repo, world, Server())

        val def = repo.npcCombatDefs[getRSCM(ChaosDruids.NPC_KEY)]
        assertNotNull(def, "chaos druid combat def")
        assertEquals(20, def.hitpoints, "hitpoints")
        assertEquals(8, def.attack, "attack")
        assertEquals(8, def.strength, "strength")
        assertEquals(12, def.defence, "defence")
        assertEquals(10, def.magic, "magic")
        assertEquals(4, def.attackSpeed, "attack speed")
        assertEquals(25, def.respawnDelay, "respawn")
        assertEquals(20.0, def.slayerXp, "slayer xp")
        assertEquals(422, def.attackAnimation, "punch animation")
        assertEquals(listOf(836), def.deathAnimation, "death animation")

        /*
         * `aggressive = Yes`. A radius alone is not enough to make an npc aggressive - the aggro
         * timer has to be positive too, or the default check gives up immediately and the monster
         * is silently passive.
         */
        assertTrue(def.aggressiveRadius > 0, "aggro radius")
        assertTrue(def.aggressiveTimer > 0, "aggro timer - a non-positive one is silently passive")
    }

    /**
     * The monster left `content/npcs/dungeon` and must not be declared in both: two plugins binding
     * the same npc id would register two death handlers and drop two lots of loot per kill, and
     * `PluginRepository.bindNpcCombat` would throw outright on the second combat binding.
     */
    @Test
    fun `the chaos druid is declared in exactly one place`() {
        val stillInDungeonPackage = DungeonMonsters.ALL.any { ChaosDruids.NPC_KEY in it.npcKeys }
        assertFalse(stillInDungeonPackage, "chaos druid must no longer be in DungeonMonsters.ALL")

        // The warrior is a different monster with a different page and stays there.
        assertTrue(
            DungeonMonsters.ALL.any { "npc.chaos_druid_warrior" in it.npcKeys },
            "the chaos druid warrior stays in content/npcs/dungeon",
        )
    }
}
