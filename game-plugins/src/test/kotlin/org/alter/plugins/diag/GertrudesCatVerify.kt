package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import dev.openrune.cache.filestore.loadLocations
import org.alter.api.cfg.Varp
import org.alter.plugins.content.quests.Quest
import org.alter.plugins.content.quests.Quests
import org.alter.plugins.content.quests.gertrudescat.GertrudesCat
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for the quest framework and Gertrude's Cat.
 *
 * The same job the other `*Verify` suites do: every RSCM key the quest binds resolves to a real
 * cache entry, every option string it binds with `onNpcOption`/`onObjOption` really exists on that
 * definition, and the hand-transcribed data (tiles, ids, stage numbering) still says what it said
 * when it was sourced.
 *
 * The option assertions matter more here than usual because `KotlinPlugin.onNpcOption` resolves an
 * option *name* to a slot index at plugin-construction time and `check`s that it found one - so a
 * cache re-dump that renames "Pick-up" would not fail quietly, it would throw during plugin load
 * and take the whole content module's registration down with it.
 */
class GertrudesCatVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** Loc types that occupy a whole tile and block it. */
        val SOLID_TYPES = setOf(10, 11)

        /** Floor decoration - what the Lumber Yard's upper walkway is made of. */
        const val FLOOR_DECOR_TYPE = 22
    }

    private val itemKeys =
        listOf(
            "item.doogle_leaves", "item.raw_sardine", "item.seasoned_sardine", "item.fluffs_kitten",
            "item.bucket_of_milk", "item.bucket", "item.chocolate_cake", "item.stew", "item.coins_995",
        ) + GertrudesCat.KITTEN_COLOURS

    @Test
    fun `every item key resolves`() {
        itemKeys.forEach { key ->
            val id = getRSCM(key)
            assertNotNull(CacheManager.getItem(id), "$key -> $id has no cache item")
        }
    }

    /**
     * The ids the wiki's infoboxes gave, pinned to the names this cache gives them. If a re-dump
     * shifts any of these, the quest would silently bind to the wrong entity.
     */
    @Test
    fun `quest ids still name the things the wiki said they were`() {
        val expected =
            mapOf(
                "npc.gertrude" to (7284 to "Gertrude"),
                "npc.gertrude_7723" to (7723 to "Gertrude"),
                "npc.wilough" to (3503 to "Wilough"),
                "npc.shilop" to (3501 to "Shilop"),
                "npc.gertrudes_cat_3497" to (3497 to "Gertrude's cat"),
                "npc.crate_3499" to (3499 to "Crate"),
            )

        expected.forEach { (key, expect) ->
            val (id, name) = expect
            assertEquals(id, getRSCM(key), "$key no longer resolves to $id")
            assertEquals(name, CacheManager.getNpc(id).name, "npc $id is no longer '$name'")
        }

        val items =
            mapOf(
                "item.doogle_leaves" to (1573 to "Doogle leaves"),
                "item.seasoned_sardine" to (1552 to "Seasoned sardine"),
                "item.fluffs_kitten" to (1554 to "Fluffs' kitten"),
                "item.raw_sardine" to (327 to "Raw sardine"),
            )
        items.forEach { (key, expect) ->
            val (id, name) = expect
            assertEquals(id, getRSCM(key), "$key no longer resolves to $id")
            assertEquals(name, CacheManager.getItem(id).name, "item $id is no longer '$name'")
        }
    }

    @Test
    fun `every npc option the quest binds exists on that npc`() {
        val bindings =
            mapOf(
                "npc.gertrude" to listOf("talk-to"),
                "npc.wilough" to listOf("talk-to"),
                "npc.shilop" to listOf("talk-to"),
                "npc.gertrudes_cat_3497" to listOf("pick-up", "stroke", "talk-to"),
                "npc.crate_3499" to listOf("search"),
            )

        bindings.forEach { (key, options) ->
            val def = CacheManager.getNpcs()[getRSCM(key)]
            assertNotNull(def, "$key has no cache npc")
            options.forEach { option ->
                assertTrue(
                    def.actions.any { it?.lowercase() == option },
                    "$key has no '$option' option [actions=${def.actions.toList()}]",
                )
            }
        }
    }

    /**
     * The way into the Lumber Yard and the way up to Fluffs.
     *
     * Both were found by dumping region 13110's locations. The ladders are bound by
     * `content/objects/ladder/LadderPlugin` rather than the quest, but they are asserted here
     * because the quest is what depends on them: without a working ladder, Fluffs is unreachable
     * and the quest cannot be completed at all.
     */
    @Test
    fun `the yard's fence and ladders carry the options that are bound to them`() {
        val bindings =
            mapOf(
                "object.broken_fence_2618" to "climb-over",
                "object.ladder_11794" to "climb-up",
                "object.ladder_11795" to "climb-down",
                "object.ladder_11802" to "climb-down",
            )

        bindings.forEach { (key, option) ->
            val def = CacheManager.getObjects()[getRSCM(key)]
            assertNotNull(def, "$key has no cache object")
            assertTrue(
                def.actions.any { it?.lowercase() == option },
                "$key has no '$option' option [actions=${def.actions.toList()}]",
            )
        }
    }

    /**
     * The quest crates are npcs (3499). The Lumber Yard is also full of ordinary scenery crates -
     * objects 2620 and 5106, both with a real "Search" action - and binding those instead would
     * have turned every crate in the yard into a kitten lottery. This asserts the two stay
     * distinguishable: the quest's crate is an npc, and the scenery crates are objects.
     */
    @Test
    fun `the quest crate is an npc, not one of the yard's scenery crates`() {
        val questCrate = CacheManager.getNpc(getRSCM("npc.crate_3499"))
        assertEquals("Crate", questCrate.name)
        assertTrue(questCrate.actions.any { it?.lowercase() == "search" })

        listOf(2620, 5106).forEach { id ->
            val scenery = CacheManager.getObjects()[id]
            assertNotNull(scenery, "scenery crate object $id vanished from the cache")
            assertEquals("Crate", scenery.name, "object $id is no longer a Crate")
        }
    }

    @Test
    fun `the six crate tiles are the wiki's six, and all distinct`() {
        val expected =
            listOf(
                Triple(3298, 3514, 0),
                Triple(3315, 3515, 0),
                Triple(3303, 3506, 0),
                Triple(3307, 3507, 0),
                Triple(3305, 3500, 0),
                Triple(3310, 3499, 0),
            )

        assertEquals(6, GertrudesCat.CRATE_TILES.size, "the quest article pins exactly six crates")
        assertEquals(
            expected,
            GertrudesCat.CRATE_TILES.map { Triple(it.x, it.z, it.height) },
            "crate tiles no longer match the quest article's pin map",
        )
        assertEquals(
            GertrudesCat.CRATE_TILES.size,
            GertrudesCat.CRATE_TILES.distinct().size,
            "two crates share a tile, so one of them can never be searched",
        )
    }

    /**
     * Fluffs is on the floor above, which is the whole reason the ladder had to be wired up. A
     * height of 0 here would put her outside on the yard floor and quietly make the ladder
     * pointless.
     */
    @Test
    fun `Fluffs is upstairs`() {
        assertEquals(3310, GertrudesCat.FLUFFS_TILE.x)
        assertEquals(3508, GertrudesCat.FLUFFS_TILE.z)
        assertEquals(1, GertrudesCat.FLUFFS_TILE.height, "Fluffs must be on the Lumber Yard's first floor")
    }

    /**
     * She shipped hovering in mid-air once, on the wiki's pin (3310,3506), because that tile is off
     * the southern edge of the walkway.
     *
     * The trap: the Lumber Yard's first floor has **no terrain floor at all** - every tile at height
     * 1 in region 13110 has zero overlay and underlay - so the usual "does this tile have a floor"
     * check answers no everywhere and is useless. The walkway is built from type-22 floor-decoration
     * locs. This asserts there is one under her, which is the only thing that actually keeps her off
     * the skybox.
     */
    @Test
    fun `Fluffs is standing on the walkway, not thin air`() {
        val tile = GertrudesCat.FLUFFS_TILE
        val locs = locsAt(13110, tile.x, tile.z, tile.height)

        assertTrue(
            locs.any { it.type == FLOOR_DECOR_TYPE },
            "no floor-decoration loc under Fluffs at (${tile.x},${tile.z},${tile.height}) - she will float [locs=$locs]",
        )
        assertTrue(
            locs.none { it.type in SOLID_TYPES },
            "Fluffs is inside solid scenery [locs=${locs.filter { it.type in SOLID_TYPES }}]",
        )
    }

    /**
     * Gertrude shipped standing inside her own kitchen table, because her wiki marker is
     * `mtype=square|r=3` - an area - and its centre (3151,3409) is the eastern half of Table 2998
     * (2x1, impenetrable, anchored at 3150,3409).
     *
     * Asserts both halves of the lesson: the table really does cover the old tile, and the tile she
     * actually stands on carries no solid scenery.
     */
    @Test
    fun `Gertrude is not standing in the furniture`() {
        val table = locsAt(12597, 3150, 3409, 0).firstOrNull { it.id == 2998 }
        assertNotNull(table, "Table 2998 is no longer at (3150,3409) - re-check Gertrude's tile")
        assertEquals(2, table.sizeX, "Table 2998 is 2x1; if that changed, its footprint changed too")

        // The old tile, still covered by that 2x1 table - kept as the reason this test exists.
        assertTrue(
            3151 in table.x until (table.x + table.sizeX),
            "(3151,3409) should be under the table - that was the original bug",
        )

        val gertrude = locsAt(12597, 3151, 3410, 0)
        assertTrue(
            gertrude.none { it.type in SOLID_TYPES },
            "Gertrude's tile (3151,3410) has solid scenery on it [locs=${gertrude.filter { it.type in SOLID_TYPES }}]",
        )
    }

    private data class LocInfo(val id: Int, val type: Int, val x: Int, val z: Int, val sizeX: Int, val sizeY: Int)

    /**
     * Every loc on one tile, taking each object's rotated footprint into account so a multi-tile
     * object is reported on all the tiles it actually covers.
     */
    private fun locsAt(
        region: Int,
        x: Int,
        z: Int,
        height: Int,
    ): List<LocInfo> {
        val rx = region shr 8
        val ry = region and 0xFF
        val baseX = rx shl 6
        val baseZ = ry shl 6
        val found = mutableListOf<LocInfo>()

        val land = CacheManager.cache.data(5, "l${rx}_$ry") ?: return found
        loadLocations(land) { loc ->
            if (loc.height != height) return@loadLocations
            val def = runCatching { CacheManager.getObject(loc.id) }.getOrNull() ?: return@loadLocations
            val rotated = loc.orientation == 1 || loc.orientation == 3
            val sx = if (rotated) def.sizeY else def.sizeX
            val sy = if (rotated) def.sizeX else def.sizeY
            val ox = baseX + loc.localX
            val oz = baseZ + loc.localY
            if (x in ox until (ox + sx) && z in oz until (oz + sy)) {
                found += LocInfo(loc.id, loc.type, ox, oz, sx, sy)
            }
        }
        return found
    }


    /**
     * The six kitten colours, in the wiki's order. These are the *items* (1555-1560), not the
     * follower npcs (5591-5596) - an easy pair to transpose, and the npc ids would resolve to
     * perfectly real cache entries that simply are not items.
     */
    @Test
    fun `the kitten reward colours are the six pet kitten items`() {
        assertEquals(6, GertrudesCat.KITTEN_COLOURS.size)
        assertEquals(
            listOf(1555, 1556, 1557, 1558, 1559, 1560),
            GertrudesCat.KITTEN_COLOURS.map { getRSCM(it) },
        )
        GertrudesCat.KITTEN_COLOURS.forEach { key ->
            assertEquals("Pet kitten", CacheManager.getItem(getRSCM(key)).name, "$key is not a Pet kitten")
        }
    }

    /**
     * Stage numbering. These values are written straight into the quest's player-variable, so they
     * have to stay a strictly ascending run starting at 1, with `completedStage` as the last of
     * them - `questState` reads "at or above completedStage" as finished.
     */
    @Test
    fun `the stage ladder is strictly ascending and ends at the completion stage`() {
        val stages =
            listOf(
                GertrudesCat.STARTED,
                GertrudesCat.KNOWS_LOCATION,
                GertrudesCat.CAT_HAD_MILK,
                GertrudesCat.CAT_HAD_SARDINE,
                GertrudesCat.FOUND_KITTEN,
                GertrudesCat.REUNITED,
                GertrudesCat.COMPLETE,
            )

        assertEquals(1, stages.first(), "stage 0 is reserved for 'not started'")
        stages.zipWithNext().forEach { (a, b) ->
            assertTrue(b == a + 1, "stages must ascend one at a time, found $a then $b")
        }
        assertEquals(
            GertrudesCat.COMPLETE,
            GertrudesCat.QUEST.completedStage,
            "the quest's completedStage must be its last stage",
        )
    }

    /**
     * The player-variable ids, both cross-checked against RuneLite's generated `VarPlayerID`
     * (`QP = 101`, `FLUFFS = 180`). The stage *values* written into 180 are this server's own -
     * see `org.alter.plugins.content.quests.syncVarps` - but the ids are real and must not drift.
     */
    @Test
    fun `the quest uses the real player-variables`() {
        assertEquals(180, GertrudesCat.QUEST.varp, "Gertrude's Cat is varp 180")
        assertEquals(180, Varp.GERTRUDES_CAT)
        assertEquals(101, Varp.QUEST_POINTS)
    }

    /**
     * The persistence key is the one thing here that can never change once players have saves: it
     * is the literal key their progress is stored under.
     */
    @Test
    fun `the persistence key is stable`() {
        assertEquals("quest_gertrudes_cat", GertrudesCat.QUEST.attribute.persistenceKey)
        assertEquals("gertrudes_cat_crate", GertrudesCat.CHOSEN_CRATE_ATTR.persistenceKey)
    }

    /**
     * Two separately built [Quest] instances with the same id must share an attribute key, because
     * that is what lets the save layer reconstruct the key from a plain string on load. If
     * [org.alter.game.model.attr.AttributeKey] ever stopped keying on `persistenceKey`, every
     * player's quest progress would silently start reading back as zero.
     */
    @Test
    fun `quest attribute keys are equal by persistence key`() {
        val a = Quest("sample", "Sample", 1, 1, 1).attribute
        val b = Quest("sample", "Sample", 1, 1, 1).attribute
        assertEquals(a, b, "attribute keys must compare equal by persistence key")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `registering the same quest twice does not double-count its points`() {
        val before = Quests.size()
        val quest = Quest("verify_only", "Verify Only", 5, 4242, 1)
        Quests.register(quest)
        Quests.register(quest)
        assertEquals(before + 1, Quests.size(), "re-registering a quest must replace, not duplicate")
    }

    @Test
    fun `the quest awards the wiki's rewards`() {
        assertEquals(1, GertrudesCat.QUEST.questPoints, "Gertrude's Cat is worth 1 quest point")
        assertEquals(1_525.0, GertrudesCat.COOKING_XP, "the quest gives 1,525 Cooking experience")
        assertEquals(100, GertrudesCat.BRIBE, "Wilough and Shilop want 100 coins")
    }
}
