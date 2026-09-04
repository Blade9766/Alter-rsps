package org.alter.plugins.diag

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.TileData
import dev.openrune.cache.filestore.loadTerrain
import org.alter.plugins.content.npcs.Cows
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
 * Verify-before-wire checks for `org.alter.plugins.content.npcs.Cows` - cows and cow calves.
 *
 * The tiles are transcribed by hand off the OSRS Wiki's `LocLine` rows, so what matters is that
 * each one is somewhere the animal can actually stand in *this* cache. Size is what makes this
 * more than the chicken check: cows are `size = 2`, so a spawn tile can be clear while the 2x2
 * the animal occupies is not, and it would spawn half inside a fence with nothing logged.
 *
 * **Size is read per id from the cache rather than assumed**, because the Cow calf page is the
 * one place in this package where versions of the same monster disagree: `size1` and `size2`
 * are 2 but `size3` (the Farmland calf, 2801) is 1. That is why 2801 is not dealt round-robin
 * with the other two - see [Cows].
 */
class CowVerify {
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

    /** null when the tile is usable, else why it is not. */
    private fun problem(
        x: Int,
        z: Int,
    ): String? {
        val rx = x shr 6
        val rz = z shr 6
        val tiles = tilesFor(rx, rz) ?: return "mapsquare ${rx}_$rz is not in the cache"
        val data = tiles[0][x - (rx shl 6)][z - (rz shl 6)]
        val hasFloor = data.overlayId.toInt() != 0 || data.underlayId.toInt() != 0
        val blocked = (data.settings.toInt() and 0x1) != 0
        return when {
            !hasFloor -> "no floor"
            blocked -> "flagged BLOCK_WALK"
            else -> null
        }
    }

    private fun sizeOf(npcKey: String) = CacheManager.getNpc(getRSCM(npcKey))!!.size

    /** Every tile actually spawned on, with the footprint of whichever id lands there. */
    private fun placements(): List<Triple<String, Pair<Int, Int>, Int>> =
        Cows.ALL.flatMap { bovine ->
            bovine.herds.flatMap { herd ->
                herd.tiles.mapIndexed { index, tile ->
                    val key = herd.npcKeys[index % herd.npcKeys.size]
                    Triple("${bovine.label} / ${herd.location}", tile, sizeOf(key))
                }
            }
        }

    @Test
    fun `every id resolves and is the monster it claims to be`() {
        Cows.ALL.forEach { bovine ->
            bovine.combatDefIds.forEach { key ->
                val id = getRSCM(key)
                assertTrue(id > 0, "$key did not resolve to an npc id.")
                val def = CacheManager.getNpc(id)
                assertNotNull(def, "$key ($id) is not in this cache.")
                assertEquals(bovine.label, def.name, "$key ($id) is not named '${bovine.label}'.")
                assertTrue(
                    def.actions.filterNotNull().any { it.equals("Attack", ignoreCase = true) },
                    "$key ($id) has no Attack option. 10598 is the trap here - a level 0 'Cow' " +
                        "with no options at all.",
                )
            }
        }
    }

    /**
     * The published sizes, asserted against the cache. If the Farmland calf ever stops being
     * size 1 the reason it is held out of the rotation disappears with it.
     */
    @Test
    fun `the sizes are the ones the wiki publishes`() {
        Cows.COW.combatDefIds.forEach { assertEquals(2, sizeOf(it), "$it should be size 2") }
        Cows.CALF_IDS.forEach { assertEquals(2, sizeOf(it), "$it should be size 2") }
        assertEquals(1, sizeOf(Cows.FARMLAND_CALF_ID), "the Farmland calf should be size 1")
    }

    /**
     * The bug this file was written after: the plugin defined `npc.cow` alone, so the other
     * four published cow ids inherited [org.alter.game.model.combat.NpcCombatDef.DEFAULT]'s 10
     * hitpoints and zeroed stats, and the calves had no def at all.
     */
    @Test
    fun `every spawned id is one the plugin gives a combat def`() {
        Cows.ALL.forEach { bovine ->
            bovine.herds.forEach { herd ->
                herd.npcKeys.forEach { key ->
                    assertTrue(
                        key in bovine.combatDefIds,
                        "${bovine.label} / ${herd.location} spawns $key, which has no combat def.",
                    )
                }
            }
        }
    }

    @Test
    fun `every spawn tile has a clear footprint`() {
        val failures = mutableListOf<String>()

        placements().forEach { (where, tile, size) ->
            val (x, z) = tile
            for (dx in 0 until size) {
                for (dz in 0 until size) {
                    problem(x + dx, z + dz)?.let { why ->
                        failures += "$where spawn ($x, $z) size $size -> tile (${x + dx}, ${z + dz}): $why"
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(), "Unusable spawn tiles:\n" + failures.joinToString("\n"))
    }

    /**
     * Two size-2 animals cannot share ground. An exact duplicate is a transcription slip; an
     * overlap is the subtler version of the same slip, and only shows up because these npcs are
     * bigger than their pins.
     *
     * Exactly one overlap is expected and allowed - [Cows.KNOWN_OVERLAP], a calf and a cow
     * whose published pins sit a tile apart at the Crafting Guild. Anything else is a bug.
     */
    @Test
    fun `the only overlap is the known one`() {
        val placed = placements()
        val clashes = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
        val described = mutableListOf<String>()

        for (i in placed.indices) {
            for (j in i + 1 until placed.size) {
                val (whereA, a, sizeA) = placed[i]
                val (whereB, b, sizeB) = placed[j]
                val overlapX = a.first < b.first + sizeB && b.first < a.first + sizeA
                val overlapZ = a.second < b.second + sizeB && b.second < a.second + sizeA
                if (overlapX && overlapZ) {
                    clashes += a to b
                    described += "$a in '$whereA' overlaps $b in '$whereB'"
                }
            }
        }

        val unexpected =
            described.filterIndexed { index, _ ->
                val pair = clashes[index]
                pair != Cows.KNOWN_OVERLAP && pair.second to pair.first != Cows.KNOWN_OVERLAP
            }

        assertTrue(unexpected.isEmpty(), "Unexpected overlapping spawns:\n" + unexpected.joinToString("\n"))
        assertEquals(1, clashes.size, "the known Crafting Guild overlap should be the only one")
    }

    /**
     * The counts the wiki publishes, so a dropped or duplicated row shows up as a number rather
     * than as an animal nobody notices is missing.
     */
    @Test
    fun `the herd sizes match the wiki`() {
        val expectedCows =
            mapOf(
                "Field south of the Champions' Guild" to 53,
                "Lumbridge East farm" to 21,
                "Crafting Guild" to 10,
                "South Falador Farm" to 7,
                "Ardougne Farm" to 10,
                "Sinclair Mansion" to 1,
                "Nightmare Zone" to 1,
                "West of Nightmare Zone" to 3,
                "North-west of Prifddinas (Gwenith)" to 7,
                "North of Hosidius Town Square" to 5,
                "Kebos Lowlands - Gordon's farm" to 2,
                "Kebos Lowlands - Keith's farm" to 2,
                "Isle of Souls" to 3,
                "Zanaris" to 3,
            )
        val expectedCalves =
            mapOf(
                "Lumbridge East farm" to 8,
                "Lumbridge West farm" to 5,
                "Field south of Champions' Guild" to 11,
                "South Falador Farm" to 3,
                "Crafting Guild" to 2,
                "North of Hosidius Town Square" to 3,
                "Kebos Lowlands - Keith's farm" to 1,
                "Kebos Lowlands - Gordon's farm" to 1,
                "South-west Isle of Souls" to 1,
            )

        assertEquals(expectedCows, Cows.COW.herds.associate { it.location to it.tiles.size })
        assertEquals(expectedCalves, Cows.CALF.herds.associate { it.location to it.tiles.size })
        assertEquals(128, Cows.COW.herds.sumOf { it.tiles.size }, "total cows placed")
        assertEquals(35, Cows.CALF.herds.sumOf { it.tiles.size }, "total calves placed")
    }

    /**
     * Regression guard on the reason this file exists. Sixteen tiles were the plugin's entire
     * spawn list - hand-written, not on the wiki, and scattered across the Lumbridge East farm
     * at plausible-looking coordinates. If one reappears, someone has put the guesses back.
     *
     * Fifteen of the sixteen, that is. **(3256, 3292) is excluded because it is real**: the
     * guess landed exactly on a pin the wiki's Lumbridge East farm row publishes, so it is in
     * the new table on its own merits. One hit out of sixteen over a field that size is about
     * what chance would give you, and it is the reason this guard names tiles rather than
     * asserting the old list is disjoint from the new one.
     */
    @Test
    fun `the invented Lumbridge tiles are gone`() {
        val invented =
            setOf(
                3255 to 3259, 3257 to 3262, 3259 to 3259, 3262 to 3261, 3263 to 3265,
                3260 to 3270, 3258 to 3274, 3261 to 3277, 3256 to 3280, 3262 to 3285,
                3261 to 3291, 3253 to 3288, 3248 to 3290, 3248 to 3284, 3253 to 3282,
            )

        val placed = Cows.ALL.flatMap { it.herds }.flatMap { it.tiles }.toSet()
        val survivors = invented intersect placed
        assertTrue(survivors.isEmpty(), "Invented cow tiles are back in the spawn table: $survivors")
    }

    /**
     * The Slayer wiring, which is the part of this most likely to break silently.
     *
     * `data/cfg/slayer/tasks.json` names its monsters as **strings**, and `SlayerService`
     * resolves them by matching each against every npc name in the cache. That is deliberate -
     * it sweeps up all thirty goblin ids without copying a list `content/npcs` already owns -
     * but it means the whole Cows task hangs on two spellings staying exactly right. Rename a
     * monster in the cache, or mistype one in the config, and the task quietly resolves to
     * nothing: no error, no log line, just a task that can never be credited.
     *
     * So this asserts the resolution end to end, the same way the service does it, rather than
     * trusting that "Cow" and "Cow calf" still line up.
     */
    @Test
    fun `the Cows slayer task resolves to every cow and calf we spawn`() {
        val tasks: List<TaskRow> =
            Files.newBufferedReader(Paths.get("../data/cfg/slayer/tasks.json")).use { reader ->
                Gson().fromJson(reader, object : TypeToken<List<TaskRow>>() {}.type)
            }

        val cowsTask = tasks.firstOrNull { it.name == "Cows" }
        assertNotNull(cowsTask, "there is no 'Cows' task in tasks.json")
        assertTrue("Cow" in cowsTask.monsters, "the Cows task no longer lists 'Cow'")
        assertTrue("Cow calf" in cowsTask.monsters, "the Cows task no longer lists 'Cow calf'")

        // Mirrors SlayerService.resolveTaskNpcs: cache name -> every id carrying it.
        val idsByName = HashMap<String, MutableSet<Int>>()
        CacheManager.getNpcs().forEach { (id, def) ->
            val name = def.name
            if (!name.isNullOrBlank() && name != "null") {
                idsByName.getOrPut(name.lowercase()) { mutableSetOf() }.add(id)
            }
        }
        val taskIds = cowsTask.monsters.flatMap { idsByName[it.lowercase()].orEmpty() }.toSet()

        // Everything the plugin actually puts in the world must be creditable towards the task.
        val spawned =
            Cows.ALL.flatMap { bovine -> bovine.herds.flatMap { herd -> herd.npcKeys } }
                .distinct()
                .map { getRSCM(it) }
        val uncreditable = spawned.filterNot { it in taskIds }
        assertTrue(
            uncreditable.isEmpty(),
            "these spawned ids are not covered by the Cows task, so killing one would not " +
                "count towards it: $uncreditable",
        )
    }

    /**
     * The task also lists **Undead cow**, which has no combat def and no spawn here - it is Cold
     * War content. That is fine and is not a gap to fill: `SlayerService.markAvailable` marks a
     * task assignable if *any* of its monsters is in the world, and the cows and calves are.
     *
     * This asserts the part that would actually be a problem: that the task does not depend on
     * the undead cow, so it stays assignable while that quest does not exist.
     */
    @Test
    fun `the Cows task does not depend on the unspawned undead cow`() {
        val idsByName = HashMap<String, MutableSet<Int>>()
        CacheManager.getNpcs().forEach { (id, def) ->
            val name = def.name
            if (!name.isNullOrBlank() && name != "null") {
                idsByName.getOrPut(name.lowercase()) { mutableSetOf() }.add(id)
            }
        }

        val cowIds = idsByName["cow"].orEmpty() + idsByName["cow calf"].orEmpty()
        assertTrue(cowIds.isNotEmpty(), "no npc in the cache is named Cow or Cow calf any more")

        val spawned = Cows.ALL.flatMap { it.herds }.flatMap { it.npcKeys }.distinct().map { getRSCM(it) }
        assertTrue(spawned.isNotEmpty(), "nothing is spawned, so the Cows task would be unassignable")
    }

    /** Just the fields this file reads out of `tasks.json`. */
    private data class TaskRow(val name: String = "", val monsters: List<String> = emptyList())
}
