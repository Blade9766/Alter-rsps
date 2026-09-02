package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire check for the Barbarian Village content: every RSCM key used by the
 * new plugins resolves, and every npc really carries the option string those plugins bind
 * with `onNpcOption`.
 */
class BarbarianVillageVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    private val itemKeys = listOf(
        "item.bronze_med_helm", "item.iron_med_helm", "item.steel_med_helm", "item.mithril_med_helm",
        "item.adamant_med_helm", "item.bronze_full_helm", "item.iron_full_helm", "item.steel_full_helm",
        "item.mithril_full_helm", "item.adamant_full_helm",
        "item.beer", "item.cooked_meat", "item.bronze_pickaxe", "item.pot",
        "item.bones", "item.bronze_axe", "item.staff", "item.iron_mace", "item.chaos_rune",
        "item.bronze_arrow", "item.earth_rune", "item.fire_rune", "item.mind_rune", "item.law_rune",
        "item.coins", "item.tin_ore", "item.bear_fur", "item.flyer", "item.ring_mould",
        "item.iron_axe", "item.bronze_battleaxe", "item.iron_arrow", "item.amulet_mould",
    )

    private val talkToNpcs = listOf(
        "npc.peksa", "npc.atlas", "npc.checkal", "npc.hunding", "npc.litara", "npc.sigurd",
        "npc.tassie_slipcast",
    )

    private val barbarianKeys = listOf(
        "npc.barbarian", "npc.barbarian_3056", "npc.barbarian_3057", "npc.barbarian_3058",
        "npc.barbarian_3059", "npc.barbarian_3060", "npc.barbarian_3061", "npc.barbarian_3062",
        "npc.barbarian_3064", "npc.barbarian_3065", "npc.barbarian_3066", "npc.barbarian_3067",
        "npc.barbarian_3068", "npc.barbarian_3069", "npc.barbarian_3070", "npc.barbarian_3071",
        "npc.barbarian_3072", "npc.gunthor_the_brave",
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
    fun `peksa has a trade option`() {
        val def = CacheManager.getNpcs()[getRSCM("npc.peksa")]!!
        assertTrue(def.actions.any { it?.lowercase() == "trade" }, "actions=${def.actions.toList()}")
    }

    @Test
    fun `every barbarian variant is attackable and has the expected combat level`() {
        val expectedLevels = mapOf(
            "npc.barbarian" to 17, "npc.barbarian_3056" to 10, "npc.barbarian_3057" to 17,
            "npc.barbarian_3058" to 17, "npc.barbarian_3059" to 10, "npc.barbarian_3060" to 10,
            "npc.barbarian_3061" to 10, "npc.barbarian_3062" to 17, "npc.barbarian_3064" to 10,
            "npc.barbarian_3065" to 10, "npc.barbarian_3066" to 10, "npc.barbarian_3067" to 10,
            "npc.barbarian_3068" to 15, "npc.barbarian_3069" to 17, "npc.barbarian_3070" to 10,
            "npc.barbarian_3071" to 10, "npc.barbarian_3072" to 9, "npc.gunthor_the_brave" to 29,
        )

        barbarianKeys.forEach { key ->
            val def = CacheManager.getNpcs()[getRSCM(key)]
            assertNotNull(def, "$key has no cache npc")
            assertTrue(
                def.actions.any { it?.lowercase() == "attack" },
                "$key is not attackable [actions=${def.actions.toList()}]",
            )
            assertTrue(
                def.combatLevel == expectedLevels.getValue(key),
                "$key cache level ${def.combatLevel} != wiki level ${expectedLevels.getValue(key)}",
            )
        }
    }

    @Test
    fun `no two village spawns share a tile`() {
        val tiles = org.alter.plugins.content.npcs.barbarian.BarbarianData.VILLAGE_VARIANTS
            .map { it.spawnX to it.spawnZ }
        assertTrue(tiles.size == tiles.distinct().size, "duplicate barbarian spawn tiles: $tiles")
    }
}
