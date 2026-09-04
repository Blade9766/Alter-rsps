package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.api.BonusSlot
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.HerbDropTable
import org.alter.plugins.content.npcs.darkwarrior.DarkWarriorDrops
import org.alter.plugins.content.npcs.darkwarrior.DarkWarriorPlugin
import org.alter.plugins.content.npcs.darkwarrior.DarkWarriors
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for `content/npcs/darkwarrior`.
 *
 * Two things here are worth a test rather than a careful read. The first is the **id-to-level
 * mapping**: the wiki's infobox lists its five ids out of level order, so a transposition would
 * quietly give the level 37 the level 62's hitpoints and nothing at runtime would say so - the
 * cache's own combat levels catch it. The second is that all three drop tables **sum to 128**,
 * which is the only check that the transcription is complete rather than merely plausible; a
 * dropped row just silently makes everything else fractionally commoner.
 */
class DarkWarriorVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(java.nio.file.Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    private fun isAttackable(id: Int) =
        CacheManager.getNpcs()[id]?.actions?.any { it?.equals("Attack", ignoreCase = true) == true } == true

    @Test
    fun `every variant resolves to an attackable Dark warrior at its wiki combat level`() {
        DarkWarriors.VARIANTS.forEach { variant ->
            val id = getRSCM(variant.npcKey)
            assertTrue(id > 0, "${variant.npcKey} did not resolve to an npc id.")
            val def = assertNotNull(CacheManager.getNpcs()[id], "${variant.npcKey} ($id) is not in this cache.")
            assertEquals("Dark warrior", def.name, "${variant.npcKey} ($id) is named '${def.name}'.")
            assertTrue(
                isAttackable(id),
                "${variant.npcKey} ($id) has no Attack option [actions=${def.actions.toList()}].",
            )
            assertEquals(
                variant.combatLevel,
                def.combatLevel,
                "${variant.npcKey} ($id) is combat level ${def.combatLevel} in the cache but " +
                    "${variant.combatLevel} in the wiki infobox row it was transcribed from.",
            )
        }
    }

    /**
     * Completeness, the same property [CitizenVerify] checks: an attackable Dark warrior with no
     * combat def is a 10-hitpoint punching bag that drops nothing, and nothing says so at runtime.
     * The two `Elite dark warrior` ids in this cache are a different monster with a different name
     * and are correctly outside this filter.
     */
    @Test
    fun `every attackable Dark warrior in the cache is covered`() {
        val covered = DarkWarriors.VARIANTS.map { getRSCM(it.npcKey) }.toSet()
        val attackable =
            CacheManager.getNpcs()
                .filter { it.value.name == "Dark warrior" && isAttackable(it.key) }
                .keys
                .toSet()

        val missing = (attackable - covered).sorted()
        assertTrue(
            missing.isEmpty(),
            "Attackable Dark warrior ids with no combat def: " +
                missing.joinToString { "$it (lvl ${CacheManager.getNpcs()[it]?.combatLevel})" },
        )
    }

    @Test
    fun `no npc id is claimed by two variants`() {
        val seen = mutableMapOf<Int, Int>()
        DarkWarriors.VARIANTS.forEach { variant ->
            val previous = seen.put(getRSCM(variant.npcKey), variant.combatLevel)
            assertTrue(
                previous == null,
                "${variant.npcKey} is declared by both level $previous and level ${variant.combatLevel}; " +
                    "the later setCombatDef would silently win.",
            )
        }
    }

    /**
     * The wiki's numerators are used as relative weights, which is only faithful if they really do
     * add up to the 128 slots the tables are drawn from. All three do - the level 8 one only once
     * its free-to-play-only 10-coin row is dropped, which is why that exclusion is not cosmetic.
     */
    @Test
    fun `all three drop tables sum to the wiki's 128 slots`() {
        assertEquals(128, DarkWarriorDrops.LEVEL_8_TABLE.sumOf { it.weight }, "Level 8 drop weights")
        assertEquals(128, DarkWarriorDrops.KOUREND_TABLE.sumOf { it.weight }, "Great Kourend drop weights")
        assertEquals(128, DarkWarriorDrops.LEVEL_145_TABLE.sumOf { it.weight }, "Level 145 drop weights")
    }

    /**
     * [DarkWarriorDrops.HERB_ROW] is matched by identity, so it has to be the *same instance* in
     * every table it appears in - an accidental copy would roll as a silent "Nothing" instead of
     * reaching the herb table.
     */
    @Test
    fun `the herb row is one shared instance in all three tables`() {
        DarkWarriorDrops.TABLES.forEach { (table, rows) ->
            assertEquals(
                1,
                rows.count { it === DarkWarriorDrops.HERB_ROW },
                "$table does not carry exactly one shared herb row instance.",
            )
        }
        assertEquals(3, DarkWarriorDrops.HERB_ROW.weight, "The herb row is published at 3/128.")
    }

    @Test
    fun `every drop table row resolves to a real item`() {
        val rows = DarkWarriorDrops.TABLES.values.flatten() + HerbDropTable.TABLE
        rows.forEach { drop ->
            val item = drop.item ?: return@forEach
            assertNotNull(CacheManager.getItems()[item], "Drop table row $item is not an item in this cache.")
            assertTrue(drop.min in 1..drop.max, "Drop table row $item has an impossible quantity ${drop.min}..${drop.max}.")
        }
        listOf(
            "item.bones",
            "item.looting_bag",
            "item.clue_scroll_medium",
            "item.clue_scroll_hard",
        ).forEach { key ->
            assertNotNull(CacheManager.getItems()[getRSCM(key)], "$key is not an item in this cache.")
        }
    }

    /** Every spawn is on a real npc key, and the fortress reading is the members one. */
    @Test
    fun `spawns are placed on declared variants and the fortress ground floor is level 145`() {
        val declared = DarkWarriors.VARIANTS.map { it.npcKey }.toSet()
        DarkWarriors.SPAWNS.forEach { spawn ->
            assertTrue(spawn.npcKey in declared, "${spawn.npcKey} is spawned but has no combat def.")
        }

        val fortress = DarkWarriors.SPAWNS.filter { it.x in 3010..3040 && it.z in 3620..3646 }
        val ground = fortress.filter { it.height == 0 }
        val upper = fortress.filter { it.height == 1 }
        assertEquals(18, ground.size, "The wiki publishes eighteen level 145 pins on the fortress floor.")
        assertEquals(8, upper.size, "The wiki publishes eight level 8 pins on the fortress upper floor.")
        assertTrue(
            ground.all { it.npcKey == DarkWarriors.LEVEL_145_KEY },
            "The fortress ground floor is the level 145s in a members world.",
        )
        assertTrue(
            upper.all { it.npcKey == DarkWarriors.LEVEL_8_KEY },
            "The fortress upper floor is the level 8s.",
        )

        // No two warriors share a tile - the failure the members reading exists to avoid.
        val tiles = DarkWarriors.SPAWNS.map { Triple(it.x, it.z, it.height) }
        assertEquals(tiles.size, tiles.toSet().size, "Two dark warriors are spawned on the same tile.")
    }

    /**
     * The plugin's `init` really runs here, which is the only way to catch what a data-only test
     * cannot: `NpcCombatBuilder.setBonus`'s "Bonus already set" check throwing - a plugin whose
     * constructor throws registers *nothing*, silently - and numbers that do not survive the round
     * trip through the DSL.
     */
    @Test
    fun `the plugin builds and every variant carries its own combat def`() {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        val repo = PluginRepository(world)
        DarkWarriorPlugin(repo, world, Server())
        val defs = repo.npcCombatDefs

        DarkWarriors.VARIANTS.forEach { variant ->
            val def = assertNotNull(defs[getRSCM(variant.npcKey)], "${variant.npcKey} registered no combat def.")
            assertEquals(variant.hitpoints, def.hitpoints, "${variant.npcKey} hitpoints")
            assertEquals(variant.attack, def.attack, "${variant.npcKey} attack")
            assertEquals(variant.strength, def.strength, "${variant.npcKey} strength")
            assertEquals(variant.defence, def.defence, "${variant.npcKey} defence")
            assertEquals(variant.magic, def.magic, "${variant.npcKey} magic")
            assertEquals(DarkWarriors.ATTACK_SPEED, def.attackSpeed, "${variant.npcKey} attack speed")
            assertEquals(DarkWarriors.COMBAT_STYLE, def.combatStyle, "${variant.npcKey} combat style")
            assertEquals(DarkWarriors.RESPAWN_CYCLES, def.respawnDelay, "${variant.npcKey} respawn delay")
            assertEquals(DarkWarriors.ATTACK_ANIMATION, def.attackAnimation, "${variant.npcKey} attack animation")
            assertEquals(DarkWarriors.BLOCK_ANIMATION, def.blockAnimation, "${variant.npcKey} block animation")
            assertEquals(listOf(DarkWarriors.DEATH_ANIMATION), def.deathAnimation, "${variant.npcKey} death animation")
            assertEquals(variant.slayerXp, def.slayerXp, "${variant.npcKey} slayer xp")
            assertEquals(
                variant.defenceCrush,
                def.bonuses[BonusSlot.DEFENCE_CRUSH.id],
                "${variant.npcKey} crush defence bonus",
            )

            // NpcAggroPlugin only sweeps when both the radius and the search delay are > 0, and
            // only actually engages when the timer is set - all five pages say aggressive = Yes.
            assertTrue(def.aggressiveRadius > 0, "${variant.npcKey} is aggressive on the wiki")
            assertTrue(def.aggroTargetDelay > 0, "${variant.npcKey} needs a search delay to sweep")
            val expectedTimer = if (variant.wilderness) Int.MAX_VALUE else DarkWarriors.AGGRO_CYCLES
            assertEquals(expectedTimer, def.aggressiveTimer, "${variant.npcKey} aggro timer")
        }
    }
}
