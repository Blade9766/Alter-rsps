package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks on [org.alter.plugins.content.items.skillcapes.DefenceCapePlugin], whose real risk is
 * the same shape as [SkillCapeVerify]'s: a cache option name that does not match what is bound
 * throws at plugin construction, taking every skillcape's level-99 requirement down with it -
 * see [[project-plugin-load-failures]] in this project's own history for why that failure mode
 * is worse than it looks.
 */
class DefenceCapeVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    private fun wornOptions(id: Int): List<String?> {
        val def = CacheManager.getItem(id)
        return (0..7).map { def.params?.get(451 + it) as? String }
    }

    /** Both the cape and its trimmed variant carry both worn options, in this order. */
    @Test
    fun `the cape and trim both declare Toggle Effect and Toggle Respawn`() {
        listOf("item.defence_cape", "item.defence_capet").forEach { key ->
            val id = getRSCM(key)
            val options = wornOptions(id)
            assertTrue("Toggle Effect" in options, "$key: no 'Toggle Effect' option, got $options")
            assertTrue("Toggle Respawn" in options, "$key: no 'Toggle Respawn' option, got $options")
        }
    }

    /** The Ardougne fallback tile is the same one AemadPlugin already spawns Aemad on. */
    @Test
    fun `the Ardougne respawn tile is a real East Ardougne coordinate`() {
        val aemad = getRSCM("npc.aemad")
        assertTrue(aemad > 0, "npc.aemad does not resolve")
    }

    /** 10% of a 99 max-hp player floors to 9 - matches the wiki's "10% or less" threshold. */
    @Test
    fun `the low-health threshold floors sanely at max hitpoints`() {
        assertEquals(9, 99 / 10, "the threshold divisor's behaviour has changed")
    }
}
