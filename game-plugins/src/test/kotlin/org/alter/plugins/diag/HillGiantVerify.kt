package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.TileData
import dev.openrune.cache.filestore.loadTerrain
import org.alter.plugins.content.npcs.dungeon.DungeonDrops
import org.alter.plugins.content.npcs.dungeon.DungeonMonsters
import org.alter.plugins.content.npcs.dungeon.HillGiantSpawns
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for `content/npcs/dungeon/HillGiantSpawns`.
 *
 * Hill giants are `size = 2` and most of their published locations are underground - Edgeville
 * Dungeon, Taverley, the Deep Wilderness Dungeon, the Giants' Den, the Catacombs - where the
 * walls are close and a pin that is clear on its own tile can still have a wall in the other
 * three squares of the giant's footprint. That is what these checks are for.
 */
class HillGiantVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** The wiki's `size = 2`, asserted against the cache below rather than assumed. */
        const val GIANT_SIZE = 2
    }

    private val terrain = HashMap<Int, Array<Array<Array<TileData>>>?>()

    private fun problem(
        x: Int,
        z: Int,
    ): String? {
        val rx = x shr 6
        val rz = z shr 6
        val tiles =
            terrain.getOrPut((rx shl 8) or rz) {
                CacheManager.cache.data(MAPS, "m${rx}_$rz")?.let { loadTerrain(it) }
            } ?: return "mapsquare ${rx}_$rz is not in the cache"
        val data = tiles[0][x - (rx shl 6)][z - (rz shl 6)]
        val hasFloor = data.overlayId.toInt() != 0 || data.underlayId.toInt() != 0
        val blocked = (data.settings.toInt() and 0x1) != 0
        return when {
            !hasFloor -> "no floor"
            blocked -> "flagged BLOCK_WALK"
            else -> null
        }
    }

    private val spawnedKeys: List<String>
        get() = HillGiantSpawns.CAMPS.flatMap { it.npcKeys }.distinct()

    @Test
    fun `every spawned id resolves and is a hill giant`() {
        spawnedKeys.forEach { key ->
            val id = getRSCM(key)
            assertTrue(id > 0, "$key did not resolve to an npc id.")
            val def = CacheManager.getNpc(id)
            assertNotNull(def, "$key ($id) is not in this cache.")
            assertEquals("Hill Giant", def.name, "$key ($id) is not named Hill Giant.")
            assertEquals(GIANT_SIZE, def.size, "$key ($id) is not size 2.")
            assertTrue(
                def.actions.filterNotNull().any { it.equals("Attack", ignoreCase = true) },
                "$key ($id) has no Attack option. Two ids carry the Hill Giant name and are " +
                    "deliberately not used: 11195 is level 0 with no options, and 11467's only " +
                    "option is 'Strike'.",
            )
        }
    }

    /**
     * A spawned id with no combat def inherits [org.alter.game.model.combat.NpcCombatDef.DEFAULT]
     * - 10 hitpoints, zeroed stats - which on a level 28 aggressive monster would be very
     * noticeable and still would not throw.
     */
    @Test
    fun `every spawned id carries a combat def`() {
        val defined = DungeonMonsters.ALL.flatMap { it.npcKeys }.toSet()
        val missing = spawnedKeys.filterNot { it in defined }
        assertTrue(missing.isEmpty(), "spawned hill giant ids with no combat def: $missing")
    }

    @Test
    fun `every spawn tile has a clear 2x2 footprint`() {
        val failures = mutableListOf<String>()
        HillGiantSpawns.CAMPS.forEach { camp ->
            camp.tiles.forEach { (x, z) ->
                for (dx in 0 until GIANT_SIZE) {
                    for (dz in 0 until GIANT_SIZE) {
                        problem(x + dx, z + dz)?.let { why ->
                            failures += "${camp.location} ($x, $z) -> (${x + dx}, ${z + dz}): $why"
                        }
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), "Unusable hill giant spawn tiles:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `no two giants overlap`() {
        val placed = HillGiantSpawns.CAMPS.flatMap { camp -> camp.tiles.map { camp.location to it } }
        val clashes = mutableListOf<String>()
        for (i in placed.indices) {
            for (j in i + 1 until placed.size) {
                val (locA, a) = placed[i]
                val (locB, b) = placed[j]
                if (kotlin.math.abs(a.first - b.first) < GIANT_SIZE &&
                    kotlin.math.abs(a.second - b.second) < GIANT_SIZE
                ) {
                    clashes += "$a in '$locA' overlaps $b in '$locB'"
                }
            }
        }
        assertTrue(clashes.isEmpty(), "Overlapping hill giant spawns:\n" + clashes.joinToString("\n"))
    }

    /**
     * The counts the wiki publishes. The Catacombs is the one row that is deliberately short -
     * 5 of its 13 pins, the rest being blocked in this cache - so it is stated as 5 here on
     * purpose rather than as the wiki's 13.
     */
    @Test
    fun `the camp sizes match the wiki`() {
        val expected =
            mapOf(
                "Edgeville Dungeon" to 12,
                "Lava Maze" to 4,
                "Bone Yard Hunter area" to 2,
                "Deep Wilderness Dungeon" to 6,
                "Giants' Plateau" to 7,
                "Taverley Dungeon" to 5,
                "Gnome Maze" to 3,
                "North of the Observatory" to 6,
                "South-west of Tree Gnome Stronghold" to 6,
                "Giant Pit" to 13,
                "Catacombs of Kourend" to 5,
                "Giants' Den" to 8,
                "Avium Savannah" to 6,
            )
        assertEquals(expected, HillGiantSpawns.CAMPS.associate { it.location to it.tiles.size })
        assertEquals(83, HillGiantSpawns.CAMPS.sumOf { it.tiles.size }, "total hill giants placed")
    }

    /**
     * Taverley's five giants were the whole species before this, and they lived in the area's own
     * spawn plugin. They have moved into [HillGiantSpawns] on the same tiles; if the old lines
     * come back, Taverley gets ten giants standing on five squares and nothing says so.
     */
    @Test
    fun `taverley does not spawn hill giants twice`() {
        val source =
            File("../game-plugins/src/main/kotlin/org/alter/plugins/content/areas/taverleydungeon/spawns/SpawnPlugin.kt")
        assertTrue(source.exists(), "the Taverley Dungeon spawn plugin has moved")
        val spawnLines =
            source.readLines().filter { it.contains("spawnNpc") && it.contains("hill_giant") }
        assertTrue(
            spawnLines.isEmpty(),
            "Taverley Dungeon spawns hill giants again, so they are doubled:\n" +
                spawnLines.joinToString("\n"),
        )
    }

    /**
     * The location split the wiki puts on four drop rows, which this table used to ignore by
     * carrying all four everywhere.
     */
    @Test
    fun `the Plateau drop split is exclusive on both sides`() {
        val medHelm = getRSCM("item.iron_med_helm")
        val fullHelm = getRSCM("item.iron_full_helm")
        val scimitar = getRSCM("item.steel_scimitar")
        val longsword = getRSCM("item.steel_longsword")

        val regular = DungeonDrops.HILL_GIANT.mapNotNull { it.item }
        assertTrue(fullHelm in regular, "the regular table lost its iron full helm")
        assertTrue(longsword in regular, "the regular table lost its steel longsword")
        assertTrue(medHelm !in regular, "iron med helm is Giants' Plateau only")
        assertTrue(scimitar !in regular, "steel scimitar is Giants' Plateau only")

        val plateau = DungeonDrops.HILL_GIANT_PLATEAU.mapNotNull { it.item }
        assertTrue(medHelm in plateau, "the Plateau table lost its iron med helm")
        assertTrue(scimitar in plateau, "the Plateau table lost its steel scimitar")
        assertTrue(fullHelm !in plateau, "iron full helm is not dropped on the Giants' Plateau")
        assertTrue(longsword !in plateau, "steel longsword is not dropped on the Giants' Plateau")

        // Same rarities on both sides of the split, so the two tables must weigh the same.
        assertEquals(
            DungeonDrops.HILL_GIANT.sumOf { it.weight },
            DungeonDrops.HILL_GIANT_PLATEAU.sumOf { it.weight },
            "the two hill giant tables no longer have the same total weight",
        )
    }

    /**
     * The weighted table is published out of 128 and must add up to it, filler included. If it
     * does not, every row's real rarity silently differs from the comment beside it.
     */
    @Test
    fun `both tables weigh exactly 128`() {
        assertEquals(128, DungeonDrops.HILL_GIANT.sumOf { it.weight }, "regular hill giant table")
        assertEquals(128, DungeonDrops.HILL_GIANT_PLATEAU.sumOf { it.weight }, "Giants' Plateau table")
    }

    /**
     * Tertiaries are independent rolls, not table rows. Folding a 1/5000 champion scroll into a
     * ~94-weight table made it roughly 1/94 - the bug this guards.
     */
    @Test
    fun `tertiaries are not also rows in the weighted table`() {
        val tableItems = (DungeonDrops.HILL_GIANT + DungeonDrops.HILL_GIANT_PLATEAU).mapNotNull { it.item }.toSet()
        val giant = DungeonMonsters.ALL.first { it.name == "Hill giant" }

        giant.tertiaryDrops.forEach { tertiary ->
            // The giant key is the one deliberate overlap: 1/128 in the table, plus a second
            // Wilderness-only 1/128 that reproduces the wiki's doubling.
            if (tertiary.item == "item.giant_key") {
                assertTrue(tertiary.wildernessOnly, "the extra giant key roll must be Wilderness-only")
                return@forEach
            }
            assertTrue(
                getRSCM(tertiary.item) !in tableItems,
                "${tertiary.item} is both a tertiary and a weighted row, so it drops twice as often as published",
            )
        }

        val expected =
            mapOf(
                "item.ensouled_giant_head" to 1.0 / 25.0,
                "item.clue_scroll_beginner" to 1.0 / 50.0,
                "item.long_bone" to 1.0 / 400.0,
                "item.giant_champion_scroll" to 1.0 / 5000.0,
                "item.curved_bone" to 1.0 / 5012.5,
                "item.looting_bag" to 1.0 / 5.0,
                "item.giant_key" to 1.0 / 128.0,
            )
        assertEquals(expected, giant.tertiaryDrops.associate { it.item to it.chance })
    }

    /** Every tertiary key must resolve, or it throws at the moment of the drop and not before. */
    @Test
    fun `every tertiary item resolves`() {
        DungeonMonsters.ALL
            .filter { it.name.startsWith("Hill giant") }
            .flatMap { it.tertiaryDrops }
            .forEach { tertiary ->
                val id = getRSCM(tertiary.item)
                assertTrue(id > 0, "${tertiary.item} did not resolve to an item id.")
                assertNotNull(CacheManager.getItem(id), "${tertiary.item} ($id) is not in this cache.")
            }
    }

    /** The herb and seed tables the wiki gives hill giants, on both entries. */
    @Test
    fun `both hill giant entries roll the herb and seed tables`() {
        DungeonMonsters.ALL.filter { it.name.startsWith("Hill giant") }.forEach { giant ->
            assertEquals(7.0 / 128.0, giant.herbTableChance, "${giant.name} herb table chance")
            val seed = giant.seedRoll
            assertNotNull(seed, "${giant.name} does not roll the seed table")
            assertEquals(18.0 / 128.0, seed.chance, "${giant.name} seed table chance")
        }
    }
}
