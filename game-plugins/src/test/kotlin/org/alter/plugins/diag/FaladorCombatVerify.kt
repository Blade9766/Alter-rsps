package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import dev.openrune.cache.SOUNDEFFECTS
import org.alter.game.model.combat.CombatStyle
import org.alter.plugins.content.npcs.faladorguard.FaladorGuardData
import org.alter.plugins.content.npcs.guard.CityGuard
import org.alter.plugins.content.npcs.whiteknight.WhiteKnightData
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
 * Verify-before-wire checks for Falador's combat NPCs - the White Knights and the city
 * guards. Every id must be a real, attackable cache npc whose combat level matches the
 * wiki's, every drop item must resolve, and no two of the ~61 new spawns may share a tile.
 */
class FaladorCombatVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    private val allCombatKeys: List<String>
        get() =
            WhiteKnightData.RANKS.flatMap { it.npcKeys } + FaladorGuardData.GROUPS.flatMap { it.npcKeys }

    @Test
    fun `every combat npc is attackable`() {
        allCombatKeys.forEach { key ->
            val def = CacheManager.getNpcs()[getRSCM(key)]
            assertNotNull(def, "$key has no cache npc")
            assertTrue(
                def.actions.any { it?.lowercase() == "attack" },
                "$key is not attackable [actions=${def.actions.toList()}]",
            )
        }
    }

    /**
     * The wiki's combat levels, per rank/group. A mismatch means an id was taken from the
     * wrong variant - the failure mode that put Falador's shopkeepers in Al Kharid.
     */
    @Test
    fun `every combat npc's cache level matches the wiki`() {
        WhiteKnightData.RANKS.forEach { rank ->
            rank.npcKeys.forEach { key ->
                val def = CacheManager.getNpcs()[getRSCM(key)]!!
                assertEquals(
                    rank.combatLevel,
                    def.combatLevel,
                    "$key (${rank.name}) cache level ${def.combatLevel} != wiki ${rank.combatLevel}",
                )
            }
        }

        FaladorGuardData.GROUPS.forEach { group ->
            group.npcKeys.forEach { key ->
                val def = CacheManager.getNpcs()[getRSCM(key)]!!
                assertEquals(
                    group.combatLevel,
                    def.combatLevel,
                    "$key (${group.name}) cache level ${def.combatLevel} != wiki ${group.combatLevel}",
                )
            }
        }
    }

    @Test
    fun `spawn counts match the wiki's pin counts`() {
        // 35 White Knight pins, one of which (2987,3332) carries no id annotation and is
        // deliberately omitted - see WhiteKnightData.SPAWNS.
        assertEquals(34, WhiteKnightData.SPAWNS.size, "White Knight spawn count")
        // 18 street pins (plane 0) plus 9 wall pins (plane 1).
        assertEquals(27, FaladorGuardData.SPAWNS.size, "Falador guard spawn count")
        assertEquals(18, FaladorGuardData.SPAWNS.count { it.height == 0 }, "street guards")
        assertEquals(9, FaladorGuardData.SPAWNS.count { it.height == 1 }, "wall guards")
    }

    /** The wiki's wall block is explicitly `levels = 22`, and every level-22 guard is ranged. */
    @Test
    fun `every wall guard is a ranged variant`() {
        FaladorGuardData.SPAWNS.filter { it.height == 1 }.forEach { spawn ->
            val group = FaladorGuardData.groupOf(spawn.npcKey)
            assertEquals(22, group.combatLevel, "wall guard ${spawn.npcKey} is level ${group.combatLevel}")
            assertEquals(
                CombatStyle.RANGED,
                group.combatStyle,
                "wall guard ${spawn.npcKey} (${group.name}) is not ranged",
            )
        }
    }

    /** A ranged group with no projectile fires invisibly; a melee group with one is a mistake. */
    @Test
    fun `only the ranged guard groups carry a projectile`() {
        FaladorGuardData.GROUPS.forEach { group ->
            if (group.combatStyle == CombatStyle.RANGED) {
                assertTrue(group.projectile != -1, "${group.name} is ranged but has no projectile")
            } else {
                assertEquals(-1, group.projectile, "${group.name} is melee but declares a projectile")
            }
        }
    }

    @Test
    fun `no two new combat spawns share a tile`() {
        val tiles =
            WhiteKnightData.SPAWNS.map { Triple(it.x, it.z, it.height) } +
                FaladorGuardData.SPAWNS.map { Triple(it.x, it.z, it.height) }

        val duplicates = tiles.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(duplicates.isEmpty(), "duplicate combat spawn tiles: $duplicates")
    }

    /**
     * The combat spawns must also not land on top of the town NPCs from
     * `content/areas/falador` - two npcs on one tile is the kind of thing only noticed
     * in game. These mirror the coordinates those plugins spawn on.
     */
    @Test
    fun `no combat spawn lands on a falador town npc`() {
        val townTiles =
            setOf(
                Triple(2958, 3387, 0), // Shop keeper
                Triple(2958, 3388, 0), // Shop assistant
                Triple(2976, 3384, 0), // Cassie
                Triple(2972, 3313, 0), // Wayne
                Triple(2950, 3387, 0), // Flynn
                Triple(2945, 3334, 0), // Herquin
                Triple(2960, 3337, 2), // Sir Amik Varze
                Triple(2997, 3373, 0), // Sir Tiffy Cashien
                Triple(2984, 3339, 2), // Sir Vyvin
                Triple(2982, 3341, 1), // Sir Renitee
                Triple(3027, 3379, 0), // Wyson
                Triple(3053, 3374, 0), // Party Pete
                Triple(2956, 3372, 0), // Emily
                Triple(2958, 3371, 0), // Kaylee
            )

        val combatTiles =
            WhiteKnightData.SPAWNS.map { Triple(it.x, it.z, it.height) } +
                FaladorGuardData.SPAWNS.map { Triple(it.x, it.z, it.height) }

        val clashes = combatTiles.filter { it in townTiles }
        assertTrue(clashes.isEmpty(), "combat npcs spawned on town npc tiles: $clashes")
    }

    @Test
    fun `every drop item key resolves and coins are the stackable ones`() {
        val dropKeys =
            listOf(
                "item.bones", "item.iron_longsword", "item.steel_sword", "item.steel_med_helm",
                "item.mind_rune", "item.nature_rune", "item.body_rune", "item.chaos_rune",
                "item.water_rune", "item.mithril_arrow", "item.adamant_arrow", "item.blood_rune",
                "item.law_rune", "item.iron_bar", "item.half_an_apple_pie", "item.iron_ore",
                "item.pot_of_flour", "item.iron_bolts", "item.steel_arrow", "item.bronze_arrow",
                "item.air_rune", "item.earth_rune", "item.fire_rune", "item.iron_dagger",
                "item.body_talisman", "item.grain", "item.coins_995",
            )

        dropKeys.forEach { key ->
            val id = getRSCM(key)
            assertNotNull(CacheManager.getItem(id), "$key -> $id has no cache item")
        }

        // item.coins (617) is a real cache item but is NOT stackable - dropping it gives a
        // coin pile that never merges with the player's money. item.coins_995 is the real
        // currency, and is what every drop table here uses.
        val coins = CacheManager.getItem(getRSCM("item.coins_995"))!!
        assertTrue(coins.stackable, "item.coins_995 should be the stackable coin item")
        val nonStacking = CacheManager.getItem(getRSCM("item.coins"))!!
        assertTrue(!nonStacking.stackable, "item.coins was expected to be the non-stacking variant")
    }

    /**
     * All eleven guard ids carry a "Pickpocket" cache option, so each must appear in the
     * Thieving config - otherwise the option is present in game but silently does nothing.
     */
    @Test
    fun `every falador guard is both pickpocketable and wired into the thieving config`() {
        val config = Files.readString(Paths.get("../data", "cfg", "thieving", "pickpockets.json"))

        FaladorGuardData.GROUPS.flatMap { it.npcKeys }.forEach { key ->
            val def = CacheManager.getNpcs()[getRSCM(key)]!!
            assertTrue(
                def.actions.any { it?.lowercase() == "pickpocket" },
                "$key has no Pickpocket option [actions=${def.actions.toList()}]",
            )
            assertTrue(
                config.contains("\"$key\""),
                "$key is pickpocketable in the cache but missing from pickpockets.json",
            )
        }
    }

    /**
     * Every combat sound id must exist as an archive in cache index 4. A sound id with no
     * archive behind it is written to the client and then dropped without a word, which is
     * indistinguishable in game from the npc having had no sound configured at all - so
     * this is the check that a "fixed" guard actually makes a noise.
     *
     * The ids also have to be stated in the first place. `MonsterAnimationsPlugin` resolves
     * combat audio by npc name (`named-combat-media.json`) and then by whatever sound is
     * baked into the resolved animation; guards are named the bare string `Guard`, which is
     * in no entry, and this cache carries no embedded sound data on any sequence at all -
     * the second assertion pins that down, since if a future cache ever did carry it the
     * explicit ids here would stop being the only thing keeping guards audible.
     */
    @Test
    fun `every guard combat sound resolves to a real cache archive`() {
        val archives = CacheManager.cache.archives(SOUNDEFFECTS).toSet()
        FaladorGuardData.GROUPS.forEach { group ->
            listOf(
                "attack" to group.attackSound,
                "block" to CityGuard.HUMAN_BLOCK_SOUND,
                "death" to CityGuard.HUMAN_DEATH_SOUND,
            ).forEach { (kind, sound) ->
                assertTrue(sound > 0, "the ${group.name} guards' $kind sound is unset, so nothing will play")
                assertTrue(sound in archives, "the ${group.name} guards' $kind sound $sound has no cache archive")
            }
        }
    }
}
