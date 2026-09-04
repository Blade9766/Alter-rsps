package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.TileData
import dev.openrune.cache.filestore.loadTerrain
import org.alter.plugins.content.npcs.critters.ChickenSpawns
import org.alter.plugins.content.npcs.critters.Critters
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for the chicken spawns in
 * `org.alter.plugins.content.npcs.critters.ChickenSpawns`.
 *
 * The tiles are transcribed by hand off the OSRS Wiki's `LocLine` rows, so the thing worth
 * checking is that each one is somewhere a chicken can actually stand in *this* cache. Two
 * ways that fails silently at runtime, both of which this catches:
 *
 * - **The mapsquare is not in the cache.** Nothing throws; the npc spawns into a region that
 *   never loads and no player will ever see it. That is what happened to the Wyrmscraig row,
 *   which is why it is not in the table.
 * - **The tile is flagged BLOCK_WALK, or has no floor at all.** A chicken standing inside a
 *   coop wall or a windmill's roof beam. Aldarin's flock is the one at real risk here - it is
 *   the only one above ground level, on `plane = 3`.
 */
class ChickenVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    /** Terrain per map region, decoded once. */
    private val terrain = HashMap<Int, Array<Array<Array<TileData>>>?>()

    private fun tilesFor(
        rx: Int,
        rz: Int,
    ) = terrain.getOrPut((rx shl 8) or rz) {
        CacheManager.cache.data(MAPS, "m${rx}_$rz")?.let { loadTerrain(it) }
    }

    @Test
    fun `every chicken id resolves and is a chicken`() {
        val placed =
            ChickenSpawns.NORMAL_IDS + ChickenSpawns.MISCELLANIA_IDS +
                ChickenSpawns.FALADOR_FARM_IDS + ChickenSpawns.GORDON_AND_MARY_IDS
        placed.forEach { key ->
            val id = getRSCM(key)
            assertTrue(id > 0, "$key did not resolve to an npc id.")
            val def = CacheManager.getNpc(id)
            assertNotNull(def, "$key ($id) is not in this cache.")
            assertEquals("Chicken", def.name, "$key ($id) is not named Chicken.")
            assertTrue(
                def.actions.filterNotNull().any { it.equals("Attack", ignoreCase = true) },
                "$key ($id) has no Attack option, so it is scenery rather than a monster - " +
                    "10556 is the trap here, a level 0 'Chicken' with a different model and no options.",
            )
        }
    }

    /**
     * A spawned id with no combat def inherits [org.alter.game.model.combat.NpcCombatDef.DEFAULT]:
     * 10 hitpoints, zeroed stats, no drops. Chickens are the monster this is most likely to happen
     * to quietly, because they die in one hit either way.
     */
    @Test
    fun `every spawned id carries a combat def`() {
        val defined = Critters.CHICKEN_IDS.toSet()
        ChickenSpawns.FLOCKS.forEach { flock ->
            flock.npcKeys.forEach { key ->
                assertTrue(
                    key in defined,
                    "${flock.location} spawns $key, which Critters.CHICKEN_IDS does not define.",
                )
            }
        }
    }

    @Test
    fun `every spawn tile is walkable in this cache`() {
        val failures = mutableListOf<String>()

        ChickenSpawns.FLOCKS.forEach { flock ->
            flock.tiles.forEach { (x, z) ->
                val rx = x shr 6
                val rz = z shr 6
                val tiles = tilesFor(rx, rz)
                if (tiles == null) {
                    failures += "${flock.location} ($x, $z, ${flock.height}): mapsquare ${rx}_$rz is not in the cache"
                    return@forEach
                }

                val data = tiles[flock.height][x - (rx shl 6)][z - (rz shl 6)]
                val blocked = (data.settings.toInt() and 0x1) != 0
                val hasFloor = data.overlayId.toInt() != 0 || data.underlayId.toInt() != 0
                if (!hasFloor) {
                    failures += "${flock.location} ($x, $z, ${flock.height}): no floor at this height"
                } else if (blocked) {
                    failures += "${flock.location} ($x, $z, ${flock.height}): flagged BLOCK_WALK"
                }
            }
        }

        assertTrue(failures.isEmpty(), "Unusable chicken spawn tiles:\n" + failures.joinToString("\n"))
    }

    /** Two chickens on one tile is a transcription slip, not a wiki fact. */
    @Test
    fun `no two chickens share a tile`() {
        val seen = mutableMapOf<Triple<Int, Int, Int>, String>()
        ChickenSpawns.FLOCKS.forEach { flock ->
            flock.tiles.forEach { (x, z) ->
                val tile = Triple(x, z, flock.height)
                val previous = seen.put(tile, flock.location)
                assertTrue(
                    previous == null,
                    "($x, $z, ${flock.height}) is listed by both '$previous' and '${flock.location}'.",
                )
            }
        }
    }

    /**
     * The count the wiki publishes, so a dropped or duplicated row shows up as a number rather
     * than as a chicken nobody notices is missing.
     */
    @Test
    fun `the flock sizes match the wiki`() {
        val expected =
            mapOf(
                "Lumbridge West Farm" to 33,
                "North of Lumbridge West Farm" to 1,
                "Lumbridge East Farm" to 13,
                "Champions' Guild" to 5,
                "Wizards' Tower Basement" to 1,
                "South Falador Farm" to 10,
                "White Knights Castle Courtyard" to 1,
                "West of Warriors' Guild (Tenzing's House)" to 4,
                "Entrana" to 6,
                "Between Ardougne and Witchaven" to 4,
                "Entrance of the Ranging Guild" to 4,
                "Sinclair Mansion" to 3,
                "Barbarian Agility Course" to 3,
                "Rellekka" to 4,
                "Miscellania Castle Courtyard" to 4,
                "Miscellania Blacksmith area" to 4,
                "Etceteria Castle Courtyard" to 4,
                "North of Etceteria Castle" to 3,
                "Tai Bwo Wannai" to 4,
                "Tyras Camp" to 3,
                "Prifddinas, Crwys district" to 4,
                "Gordon and Mary's farm" to 8,
                "North of Land's End" to 5,
                "South of Kourend Castle" to 6,
                "South western Outer Fortis" to 1,
                "Aldarin windmill (3rd floor)" to 8,
                // Four, not five: the wiki's third pin is malformed - see ChickenSpawns.
                "Tal Teklan" to 4,
                "Isle of Souls" to 7,
            )

        val actual = ChickenSpawns.FLOCKS.associate { it.location to it.tiles.size }
        assertEquals(expected, actual)
    }
}
