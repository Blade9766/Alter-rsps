package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.TileData
import dev.openrune.cache.filestore.loadTerrain
import org.alter.plugins.content.npcs.imp.ImpDrops
import org.alter.plugins.content.npcs.imp.ImpSpawns
import org.alter.plugins.content.npcs.imp.Imps
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for `org.alter.plugins.content.npcs.imp`.
 *
 * The imp page is unusually easy to get wrong in three specific ways, and there is a test
 * here for each:
 *
 * - **The wrong id.** Four cache ids carry the word "Imp" and only two are this monster.
 *   5728 is the player transformation, with no options at all; 3355 is the Imp Champion.
 * - **The wrong plane.** The wiki writes `plane = 0` for the God Wars Dungeon because that
 *   is its own map layer. The tiles only exist on plane 2 in this cache, and a spawn on
 *   plane 0 would vanish silently into a floorless region.
 * - **A table that does not add up.** The five drop sub-tables are supposed to total 128.
 *   If a row is fat-fingered the roll still works, it is just quietly the wrong rarity
 *   forever, so the total is asserted rather than trusted.
 */
class ImpVerify {
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
    fun `both defined ids are attackable imps at the published levels`() {
        val expected = Imps.VARIANTS.associate { it.npcKey to it.combatLevel }

        expected.forEach { (key, level) ->
            val id = getRSCM(key)
            assertTrue(id > 0, "$key did not resolve to an npc id.")
            val def = CacheManager.getNpc(id)
            assertNotNull(def, "$key ($id) is not in this cache.")
            assertEquals("Imp", def.name, "$key ($id) is not named Imp.")
            assertEquals(level, def.combatLevel, "$key ($id) is not combat level $level in this cache.")
            assertTrue(
                def.actions.filterNotNull().any { it.equals("Attack", ignoreCase = true) },
                "$key ($id) has no Attack option, so it is not a monster - npc 5728 is the trap " +
                    "here, a level 1 'Imp' with no options that players are transformed into.",
            )
        }
    }

    /**
     * npc 5728 is a real cache entry named "Imp" and it must stay out of the combat defs:
     * it is the form a player takes, not something anything can fight.
     */
    @Test
    fun `the player-transformation imp is not defined`() {
        val defined = Imps.VARIANTS.map { getRSCM(it.npcKey) }.toSet()
        assertTrue(getRSCM("npc.imp_5728") !in defined, "npc.imp_5728 is the player transformation, not a monster.")
    }

    @Test
    fun `every spawn names a defined variant`() {
        val defined = Imps.VARIANTS.map { it.npcKey }.toSet()
        ImpSpawns.HAUNTS.forEach { haunt ->
            assertTrue(
                haunt.npcKey in defined,
                "${haunt.location} spawns ${haunt.npcKey}, which Imps.VARIANTS does not define. " +
                    "An undefined id inherits NpcCombatDef.DEFAULT - 10 hitpoints and no stats.",
            )
        }
    }

    @Test
    fun `every spawn tile is walkable in this cache`() {
        val failures = mutableListOf<String>()

        ImpSpawns.HAUNTS.forEach { haunt ->
            haunt.tiles.forEach { (x, z) ->
                val rx = x shr 6
                val rz = z shr 6
                val tiles = tilesFor(rx, rz)
                if (tiles == null) {
                    failures += "${haunt.location} ($x, $z, ${haunt.height}): mapsquare ${rx}_$rz is not in the cache"
                    return@forEach
                }

                val data = tiles[haunt.height][x - (rx shl 6)][z - (rz shl 6)]
                val blocked = (data.settings.toInt() and 0x1) != 0
                val hasFloor = data.overlayId.toInt() != 0 || data.underlayId.toInt() != 0
                if (!hasFloor) {
                    failures += "${haunt.location} ($x, $z, ${haunt.height}): no floor at this height"
                } else if (blocked) {
                    failures += "${haunt.location} ($x, $z, ${haunt.height}): flagged BLOCK_WALK"
                }
            }
        }

        assertTrue(failures.isEmpty(), "Unusable imp spawn tiles:\n" + failures.joinToString("\n"))
    }

    /** Two imps on one tile is a transcription slip, not a wiki fact. */
    @Test
    fun `no two imps share a tile`() {
        val seen = mutableMapOf<Triple<Int, Int, Int>, String>()
        ImpSpawns.HAUNTS.forEach { haunt ->
            haunt.tiles.forEach { (x, z) ->
                val previous = seen.put(Triple(x, z, haunt.height), haunt.location)
                assertTrue(
                    previous == null,
                    "($x, $z, ${haunt.height}) is listed by both '$previous' and '${haunt.location}'.",
                )
            }
        }
    }

    /**
     * The counts the wiki publishes, so a dropped or duplicated row shows up as a number
     * rather than as an imp nobody notices is missing. Two rows are deliberately short of
     * their `LocLine` - see [ImpSpawns] - and the numbers here say so.
     */
    @Test
    fun `the haunt sizes match the wiki`() {
        val expected =
            mapOf(
                "South of Stonecutter Outpost" to 6,
                "South-west Civitas illa Fortis" to 4,
                "North of Kourend Castle" to 1,
                "Around the Monk camp" to 10,
                "Outside the Chasm of Fire" to 6,
                "South of Port Piscarilius" to 2,
                "Draynor Village" to 1,
                "Rimmington" to 1,
                "Scattered around Varrock" to 8,
                "Edgeville" to 3,
                "East of Ardougne Monastery" to 12,
                "Lumbridge Castle" to 1,
                "North of Lumbridge" to 2,
                "Western Falador" to 2,
                "East of Kingstown" to 5,
                "Farming Guild" to 1,
                "Al Kharid mine" to 1,
                "South of Falador" to 3,
                "North of the Woodcutting Guild" to 3,
                "Karamja Volcano" to 8,
                "North of Yanille" to 8,
                "God Wars Dungeon" to 7,
                "Isle of Souls" to 4,
                "Sunset Coast" to 1,
                "Avium Savannah" to 2,
                "Outer Fortis" to 3,
                "Fortis Aqueduct" to 3,
                // Eight of nine: (1388, 3224) is BLOCK_WALK in this cache.
                "Gloomthorn Trail" to 8,
                "Citlalli's basement" to 4,
            )

        val actual = ImpSpawns.HAUNTS.associate { it.location to it.tiles.size }
        assertEquals(expected, actual)
        // The Wilderness God Wars Dungeon row is absent entirely; its mapsquare is not here.
        assertTrue(ImpSpawns.HAUNTS.none { it.location.startsWith("Wilderness") })
    }

    /** The God Wars imps are the level 7 version, and they are the only ones off plane 0. */
    @Test
    fun `the god wars dungeon row is the level 7 imp on plane 2`() {
        val gwd = ImpSpawns.HAUNTS.single { it.location == "God Wars Dungeon" }
        assertEquals(Imps.GWD_ID, gwd.npcKey)
        assertEquals(2, gwd.height)
        assertTrue(
            ImpSpawns.HAUNTS.filter { it.height != 0 } == listOf(gwd),
            "Only the God Wars Dungeon row should be above ground level.",
        )
        assertTrue(
            ImpSpawns.HAUNTS.filter { it.npcKey == Imps.GWD_ID } == listOf(gwd),
            "The level 7 imp belongs in the God Wars Dungeon and nowhere else.",
        )
    }

    /**
     * The five wiki sub-tables publish `x/128` numerators that add up to exactly 128 with
     * no "Nothing" row, which is what lets [ImpDrops] roll the wiki's real rarities instead
     * of rescaled ones. A typo in a weight would silently change every rate in the table.
     */
    @Test
    fun `the drop table weights total 128`() {
        assertEquals(ImpDrops.TABLE_WEIGHT, ImpDrops.TABLE.sumOf { it.weight })
        assertTrue(ImpDrops.TABLE.none { it.item == null }, "The imp table has no Nothing row.")
    }

    @Test
    fun `every drop resolves to the item the wiki names`() {
        val expected =
            mapOf(
                "item.fiendish_ashes" to "Fiendish ashes",
                "item.black_bead" to "Black bead",
                "item.red_bead" to "Red bead",
                "item.white_bead" to "White bead",
                "item.yellow_bead" to "Yellow bead",
                "item.bronze_bolts" to "Bronze bolts",
                "item.blue_wizard_hat" to "Blue wizard hat",
                "item.egg" to "Egg",
                "item.raw_chicken" to "Raw chicken",
                "item.burnt_bread" to "Burnt bread",
                "item.burnt_meat" to "Burnt meat",
                "item.cabbage" to "Cabbage",
                "item.bread_dough" to "Bread dough",
                "item.bread" to "Bread",
                "item.cooked_meat" to "Cooked meat",
                "item.hammer" to "Hammer",
                "item.tinderbox" to "Tinderbox",
                "item.shears" to "Shears",
                "item.bucket" to "Bucket",
                "item.bucket_of_water" to "Bucket of water",
                "item.jug" to "Jug",
                "item.jug_of_water" to "Jug of water",
                "item.pot" to "Pot",
                "item.pot_of_flour" to "Pot of flour",
                "item.ball_of_wool" to "Ball of wool",
                "item.mind_talisman" to "Mind talisman",
                "item.ashes" to "Ashes",
                "item.clay" to "Clay",
                "item.cadava_berries" to "Cadava berries",
                "item.grain" to "Grain",
                "item.chefs_hat" to "Chef's hat",
                "item.flyer" to "Flyer",
                "item.potion" to "Potion",
                "item.ensouled_imp_head" to "Ensouled imp head",
                "item.looting_bag" to "Looting bag",
                "item.imp_champion_scroll" to "Imp champion scroll",
            )

        expected.forEach { (key, name) ->
            val id = getRSCM(key)
            assertTrue(id > 0, "$key did not resolve to an item id.")
            assertEquals(name, CacheManager.getItem(id)?.name, "$key ($id) is not $name in this cache.")
        }

        // Every rolled row is one of the names above, so nothing can be added to the table
        // without being named here too.
        val known = expected.keys.map { getRSCM(it) }.toSet()
        ImpDrops.TABLE.forEach { drop ->
            assertTrue(drop.item in known, "Table row ${drop.item} is not one of the verified imp drops.")
        }
        ImpDrops.ALWAYS.forEach { key ->
            assertTrue(key in expected, "$key is dropped on every kill but is not verified here.")
        }
    }
}
