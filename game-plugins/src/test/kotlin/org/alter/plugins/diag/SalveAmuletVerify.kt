package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.api.EquipmentType
import org.alter.api.NpcSpecies
import org.alter.plugins.content.combat.SalveAmulet
import org.alter.plugins.content.npcs.slayer.SlayerMonsters
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks on the salve amulet's item table and on the one thing that makes it do anything at all:
 * some monster in the world being tagged undead.
 *
 * Both halves fail silently. An rscm key that does not resolve makes `hasEquipped` never match, so
 * the amulet quietly gives nothing; and with no npc carrying [NpcSpecies.UNDEAD] the undead check
 * added to the three combat formulas would be correct and permanently false, which looks exactly
 * like the amulet being broken.
 */
class SalveAmuletVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /**
         * The monsters on this server the wiki's Undead article actually lists.
         *
         * Pinned as a set rather than a minimum, so tagging something new as undead has to be a
         * deliberate edit here too. Bloodvelds, nechryael and abyssal demons are demons; gargoyles
         * are neither; the Barrows brothers are spectral. None of them belong in this list.
         */
        private val EXPECTED_UNDEAD =
            setOf(
                "Crawling Hand (level 7)",
                "Crawling Hand (level 8)",
                "Crawling Hand (level 12)",
                "Banshee",
                "Aberrant spectre",
            )
    }

    @Test
    fun `every salve amulet key resolves to a real amulet`() {
        val keys = SalveAmulet.ALL_KEYS
        assertTrue(keys.isNotEmpty(), "the salve table is empty")
        keys.forEach { key ->
            val id = runCatching { getRSCM(key) }.getOrNull()
            assertTrue(id != null, "'$key' does not resolve; the amulet would silently do nothing")
            val def = CacheManager.getItem(id!!)
            assertTrue(
                def.name.startsWith("Salve amulet"),
                "'$key' resolves to ${def.id} '${def.name}', which is not a salve amulet",
            )
            assertEquals(
                EquipmentType.AMULET.id,
                def.equipSlot,
                "'$key' (${def.name}) is not an amulet-slot item, so the equipped check can never match",
            )
        }
    }

    @Test
    fun `no key is listed twice`() {
        val keys = SalveAmulet.ALL_KEYS
        assertEquals(keys.size, keys.toSet().size, "a salve key appears in more than one tier: $keys")
    }

    /** Without this the undead check is correct and permanently false. */
    @Test
    fun `the roster tags exactly the monsters the wiki calls undead`() {
        val tagged =
            SlayerMonsters.ALL
                .filter { NpcSpecies.UNDEAD in it.species }
                .map { it.name }
                .toSet()
        assertEquals(EXPECTED_UNDEAD, tagged, "the undead roster has drifted from the wiki's list")
    }
}
