package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.plugins.content.npcs.guard.CityGuards
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for the Varrock, Edgeville and Ardougne city guards, plus a
 * regression guard against the bug they replaced: `areas/varrock/spawns/SpawnPlugin` used
 * to spawn `guard_998/999/1000`, which are inert level-0 npcs with no options at all.
 *
 * Falador's guards are covered separately by `FaladorCombatVerify`, since they are the one
 * city whose versions split into several stat groups.
 */
class CityGuardVerify {
    companion object {
        /**
         * The shared human animation library. Derived from the cache, not assumed: these
         * three frame groups hold 277, 86 and 20 sequences and supply the stand animation
         * for 4298, 537 and 48 npcs respectively - including 808/819, the stand and walk
         * every guard in all four cities uses. Every other frame group is a small dedicated
         * rig (1056 is 15 sequences over 33 npcs; 1088 is 56 over 8).
         */
        val HUMAN_RIG_FRAME_GROUPS = setOf(207, 209, 219)

        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    @Test
    fun `every city guard is attackable and matches its wiki combat level`() {
        CityGuards.ALL.forEach { city ->
            city.npcKeys.forEach { key ->
                val def = CacheManager.getNpcs()[getRSCM(key)]
                assertNotNull(def, "$key (${city.city}) has no cache npc")
                assertTrue(
                    def.actions.any { it?.lowercase() == "attack" },
                    "$key (${city.city}) is not attackable [actions=${def.actions.toList()}]",
                )
                assertEquals(
                    city.combatLevel,
                    def.combatLevel,
                    "$key cache level ${def.combatLevel} != wiki ${city.combatLevel} for ${city.city}",
                )
            }
        }
    }

    /**
     * The three ids the old Varrock spawn plugin used. They are real cache entries but carry
     * combat level 0 and `actions=[null,null,null,null,null]` - scenery, not guards.
     * Asserting it documents precisely why they were replaced, and would fail loudly if a
     * future cache ever made them real.
     */
    @Test
    fun `the old varrock guard ids really are inert scenery`() {
        listOf("npc.guard_998", "npc.guard_999", "npc.guard_1000").forEach { key ->
            val def = CacheManager.getNpcs()[getRSCM(key)]!!
            assertEquals(0, def.combatLevel, "$key is combat level ${def.combatLevel}, not 0")
            assertTrue(
                def.actions.all { it == null },
                "$key has real options ${def.actions.toList()} - it may be a usable npc after all",
            )
        }
    }

    /** Those inert ids must not come back into the area spawn plugin. */
    @Test
    fun `varrock spawn plugin no longer spawns the inert guards`() {
        val source =
            Files.readString(
                Paths.get(
                    "src", "main", "kotlin", "org", "alter", "plugins", "content",
                    "areas", "varrock", "spawns", "SpawnPlugin.kt",
                ),
            )

        listOf("npc.guard_998", "npc.guard_999", "npc.guard_1000").forEach { key ->
            assertTrue(
                !source.contains("spawnNpc(npc = \"$key\""),
                "SpawnPlugin still spawns the inert $key",
            )
        }
    }

    @Test
    fun `spawn counts match the wiki's pin counts`() {
        // Varrock: 13 city + 2 walls + 7 castle ground + 4 castle first + 11 castle second.
        assertEquals(37, CityGuards.VARROCK.spawns.size, "Varrock guard spawn count")
        assertEquals(20, CityGuards.VARROCK.spawns.count { it.height == 0 }, "Varrock plane 0")
        assertEquals(6, CityGuards.VARROCK.spawns.count { it.height == 1 }, "Varrock plane 1")
        assertEquals(11, CityGuards.VARROCK.spawns.count { it.height == 2 }, "Varrock plane 2")

        assertEquals(6, CityGuards.EDGEVILLE.spawns.size, "Edgeville guard spawn count")
        assertEquals(10, CityGuards.ARDOUGNE.spawns.size, "Ardougne guard spawn count")
    }

    @Test
    fun `no two city guard spawns share a tile`() {
        val tiles = CityGuards.ALL.flatMap { it.spawns }.map { Triple(it.x, it.z, it.height) }
        val duplicates = tiles.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(duplicates.isEmpty(), "duplicate guard spawn tiles: $duplicates")
    }

    /** Mirrors the coordinates the areas/varrock plugins spawn their townsfolk on. */
    @Test
    fun `no guard spawn lands on a varrock town npc`() {
        val townTiles =
            setOf(
                Triple(3253, 3402, 0), // Aubury
                Triple(3217, 3415, 0), // Shop keeper
                Triple(3218, 3415, 0), // Shop assistant
                Triple(3230, 3437, 0), // Horvik
                Triple(3235, 3424, 0), // Lowe
                Triple(3206, 3399, 0), // Swordshop keeper
                Triple(3206, 3416, 0), // Thessalia
                Triple(3203, 3434, 0), // Zaff
                Triple(3214, 3422, 0), // Man
                Triple(3220, 3412, 0), // Woman
            )

        val clashes =
            CityGuards.VARROCK.spawns.map { Triple(it.x, it.z, it.height) }.filter { it in townTiles }
        assertTrue(clashes.isEmpty(), "guards spawned on town npc tiles: $clashes")
    }

    /**
     * The three cities' guards are not interchangeable, despite sharing a name and a drop
     * table. Pinning the differences stops a future edit from quietly copy-pasting one
     * city's stat line over another's.
     */
    @Test
    fun `each city's guards keep their own distinct stat line`() {
        assertEquals(
            org.alter.game.model.combat.CombatStyle.CRUSH,
            CityGuards.VARROCK.combatStyle,
            "Varrock guards are Crush",
        )
        assertEquals(
            org.alter.game.model.combat.CombatStyle.STAB,
            CityGuards.EDGEVILLE.combatStyle,
            "Edgeville guards are Stab",
        )
        assertEquals(
            org.alter.game.model.combat.CombatStyle.CRUSH,
            CityGuards.ARDOUGNE.combatStyle,
            "Ardougne guards are Crush",
        )

        // Ardougne is the odd one out: a level lower, slower, and the only guards anywhere
        // with positive magic defence.
        assertEquals(20, CityGuards.ARDOUGNE.combatLevel)
        assertEquals(5, CityGuards.ARDOUGNE.attackSpeed)
        assertTrue(
            CityGuards.ARDOUGNE.defenceMagic > 0,
            "Ardougne guards should have positive magic defence, got ${CityGuards.ARDOUGNE.defenceMagic}",
        )
        listOf(CityGuards.VARROCK, CityGuards.EDGEVILLE).forEach {
            assertEquals(21, it.combatLevel, "${it.city} guards are level 21")
            assertEquals(4, it.attackSpeed, "${it.city} guards are attack speed 4")
            assertTrue(it.defenceMagic < 0, "${it.city} guards should have negative magic defence")
        }

        // All three are modelled on the standard human rig, so all three die the human way.
        CityGuards.ALL.forEach {
            assertEquals(836, it.deathAnimation, "${it.city} guards use HUMAN_DEATH")
        }
    }

    /**
     * The bug this pins: Varrock's guards were wired with 6489/6488/6490, the
     * `VARROCK_GUARD_*` constants, taken from the historical id 3010 whose observed set in
     * `openosrs-animations.json` really is those three. But 3010 was replaced on
     * 9 November 2022 and its rig went with it - the live ids 11911-11917 are plain humans,
     * and playing a sequence from another rig on them deforms the model rather than
     * animating it.
     *
     * The rule asserted here is general: **an npc built on the shared human rig may only be
     * given animations from the shared human animation library.** [HUMAN_RIG_FRAME_GROUPS]
     * is that library, derived from the cache rather than assumed - see its comment. A
     * sequence's frame group is the high half of any of its frame ids.
     */
    @Test
    fun `city guard animations come from the same rig their models are built on`() {
        CityGuards.ALL.forEach { city ->
            city.npcKeys.forEach { key ->
                val def = CacheManager.getNpcs()[getRSCM(key)]!!
                val standGroup = frameGroupOf(def.standAnim)
                assertTrue(
                    standGroup in HUMAN_RIG_FRAME_GROUPS,
                    "$key (${city.city}) stands on frame group $standGroup, which is not the " +
                        "human rig - its combat animations need re-deriving, not this assertion relaxing",
                )
            }

            mapOf(
                "attack" to city.attackAnimation,
                "block" to city.blockAnimation,
                "death" to city.deathAnimation,
            ).forEach { (role, animation) ->
                val group = frameGroupOf(animation)
                assertTrue(
                    group in HUMAN_RIG_FRAME_GROUPS,
                    "${city.city}'s $role animation $animation lives in frame group $group, " +
                        "outside the human rig $HUMAN_RIG_FRAME_GROUPS that its guards are modelled on",
                )
            }
        }
    }

    /**
     * The specific sequences that caused it, kept as their own assertion so the reason
     * survives even if the city data is rewritten. Frame group 1056 is a closed rig: fifteen
     * sequences carrying their own stand, walk, attack, block and death, and only ~33 npcs -
     * the pre-2022 Guard, Trainee Guard, Captain, Sir Mordred, Lucien, Cleaner - stand on it.
     */
    @Test
    fun `the varrock guard animation constants belong to a rig no live guard uses`() {
        listOf(6488, 6489, 6490).forEach { animation ->
            assertEquals(
                1056,
                frameGroupOf(animation),
                "$animation was expected to sit in the old guard rig, frame group 1056",
            )
            assertTrue(
                frameGroupOf(animation) !in HUMAN_RIG_FRAME_GROUPS,
                "$animation is in the human rig after all - re-check why it deformed the model",
            )
        }

        val historical = CacheManager.getNpcs()[3010]!!
        assertEquals(1056, frameGroupOf(historical.standAnim), "npc 3010 stands on the old guard rig")
        assertTrue(
            CityGuards.VARROCK.npcKeys.none { getRSCM(it) == 3010 },
            "the historical id 3010 must not be spawned as a live Varrock guard",
        )
    }

    @Test
    fun `every city guard is pickpocketable and wired into the thieving config`() {
        val config = Files.readString(Paths.get("../data", "cfg", "thieving", "pickpockets.json"))

        CityGuards.ALL.forEach { city ->
            city.npcKeys.forEach { key ->
                val def = CacheManager.getNpcs()[getRSCM(key)]!!
                assertTrue(
                    def.actions.any { it?.lowercase() == "pickpocket" },
                    "$key (${city.city}) has no Pickpocket option [actions=${def.actions.toList()}]",
                )
                assertTrue(
                    config.contains("\"$key\""),
                    "$key is pickpocketable in the cache but missing from pickpockets.json",
                )
            }
        }
    }

    /**
     * The high half of a frame id is the frame group the sequence's frames live in;
     * a sequence only plays correctly on a model rigged for its group's family.
     */
    private fun frameGroupOf(animation: Int): Int {
        val sequence = assertNotNull(CacheManager.getAnims()[animation], "animation $animation has no cache sequence")
        val frames = assertNotNull(sequence.frameIDs, "animation $animation has no frame list")
        assertTrue(frames.isNotEmpty(), "animation $animation has no frames")
        return frames.first() shr 16
    }
}
