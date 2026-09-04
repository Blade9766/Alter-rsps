package org.alter.plugins.diag

import com.fasterxml.jackson.databind.ObjectMapper
import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.TileData
import dev.openrune.cache.filestore.loadTerrain
import org.alter.api.Elements
import org.alter.api.NpcSpecies
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.combat.NpcCombatDef
import org.alter.game.model.entity.Npc
import org.alter.game.service.game.NpcMetadataService
import org.alter.plugins.content.npcs.zombie.ZombieDrops
import org.alter.plugins.content.npcs.zombie.ZombiePlugin
import org.alter.plugins.content.npcs.zombie.ZombieSpawnPlugin
import org.alter.plugins.content.npcs.zombie.ZombieTable
import org.alter.plugins.content.npcs.zombie.Zombies
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
 * Verify-before-wire checks for `content/npcs/zombie`.
 *
 * Three things here are worth checking mechanically rather than by eye:
 *
 * - **The spawn tiles.** 105 of them, and all but 26 are in sewers, dungeons and tombs where a
 *   published pin landing inside a wall is routine - the Catacombs cost
 *   `content/npcs/dungeon/HillGiantSpawns` eight of thirteen pins for exactly that reason.
 * - **The drop tables summing to 128.** That is the whole argument for reading the members coin
 *   rates rather than the free-to-play ones (see [ZombieDrops]) and for treating the herb and gem
 *   lines as rows rather than as independent rolls. If a table stops summing to 128, one of those
 *   two decisions has silently become wrong.
 * - **That `monsterStats.json` still carries the stats.** The whole package is built on taking that
 *   table as given rather than restating it in a `setCombatDef`. If a zombie ever falls out of it,
 *   the zombie does not break loudly - it quietly becomes a 10-hitpoint, zero-defence monster with
 *   no species and no fire weakness.
 */
class ZombieVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        private val STATS_CONFIG = File("../data/cfg/npcs/monsterStats.json")
        private val TASKS_CONFIG = File("../data/cfg/slayer/tasks.json")

        /** A world carrying nothing but the monster stats table, as [MonsterStatsVerify] builds it. */
        private val world: World by lazy {
            World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT).also {
                NpcMetadataService().loadMonsterStats(it, STATS_CONFIG)
            }
        }
    }

    private val terrain = HashMap<Int, Array<Array<Array<TileData>>>?>()

    /** Why [x], [z] cannot hold a spawn, or null if it can. */
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
        return when {
            data.overlayId.toInt() == 0 && data.underlayId.toInt() == 0 -> "no floor"
            (data.settings.toInt() and 0x1) != 0 -> "flagged BLOCK_WALK"
            else -> null
        }
    }

    private val tables: Map<String, ZombieTable>
        get() =
            mapOf(
                "level 13" to ZombieDrops.LEVEL_13,
                "level 24" to ZombieDrops.LEVEL_24,
                "Wilderness" to ZombieDrops.WILDERNESS,
            )

    // ------------------------------------------------------------------------------ npc ids

    @Test
    fun `every variant id is a zombie at its published combat level`() {
        Zombies.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val id = getRSCM(key)
                assertTrue(id > 0, "$key did not resolve to an npc id.")
                val def = assertNotNull(CacheManager.getNpc(id), "$key ($id) is not in this cache.")
                assertEquals("Zombie", def.name, "$key ($id) is not named Zombie.")
                assertEquals(1, def.size, "$key ($id) is not size 1.")
                assertEquals(
                    variant.combatLevel,
                    def.combatLevel,
                    "$key ($id) is combat level ${def.combatLevel}, not ${variant.combatLevel} as ${variant.name}.",
                )
                assertTrue(
                    def.actions.filterNotNull().any { it.equals("Attack", ignoreCase = true) },
                    "$key ($id) has no Attack option.",
                )
            }
        }
    }

    @Test
    fun `no npc id is claimed by two variants`() {
        val seen = HashMap<String, String>()
        Zombies.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val previous = seen.put(key, variant.name)
                assertTrue(previous == null, "$key is in both '$previous' and '${variant.name}'.")
            }
        }
    }

    /**
     * 53 is `Zombie (Melzar's Maze)` and 64-68 are the level 25 Entrana Dungeon zombies - both
     * separate wiki pages with their own drop tables, and both correctly outside this package.
     */
    @Test
    fun `the other-page zombie ids are not claimed`() {
        val claimed = Zombies.ALL_IDS.toSet()
        listOf(53, 64, 65, 66, 67, 68).forEach { id ->
            assertTrue(
                "npc.zombie_$id" !in claimed,
                "npc.zombie_$id belongs to another Zombie page and should not be defined here.",
            )
        }
    }

    // ------------------------------------------------------------------------------- spawns

    @Test
    fun `every spawned id has a variant`() {
        val defined = Zombies.ALL_IDS.toSet()
        val missing = Zombies.CAMPS.flatMap { it.npcKeys }.distinct().filterNot { it in defined }
        assertTrue(missing.isEmpty(), "spawned zombie ids with no variant: $missing")
    }

    @Test
    fun `every spawn tile is standable`() {
        val failures = mutableListOf<String>()
        Zombies.CAMPS.forEach { camp ->
            camp.tiles.forEach { (x, z) ->
                problem(x, z)?.let { why -> failures += "${camp.location} ($x, $z): $why" }
            }
        }
        assertTrue(failures.isEmpty(), "Unusable zombie spawn tiles:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `no two zombies share a tile`() {
        val placed = Zombies.CAMPS.flatMap { camp -> camp.tiles.map { camp.location to it } }
        val clashes =
            placed.groupBy { it.second }
                .filterValues { it.size > 1 }
                .map { (tile, rows) -> "$tile is claimed by ${rows.joinToString { it.first }}" }
        assertTrue(clashes.isEmpty(), "Overlapping zombie spawns:\n" + clashes.joinToString("\n"))
    }

    /**
     * Round-robin id dealing is only meaningful when a camp has at least as many tiles as ids -
     * otherwise the tail of the id list is never used and the location silently spawns a narrower
     * mix than the wiki publishes.
     */
    @Test
    fun `every camp uses every id it names`() {
        val thin =
            Zombies.CAMPS
                .filter { it.tiles.size < it.npcKeys.size }
                .map { "${it.location}: ${it.tiles.size} tiles for ${it.npcKeys.size} ids" }
        assertTrue(thin.isEmpty(), "Camps that cannot deal all their ids:\n" + thin.joinToString("\n"))
    }

    // -------------------------------------------------------------------------- drop tables

    @Test
    fun `every drop table sums to exactly 128`() {
        tables.forEach { (name, table) ->
            val total = table.rows.sumOf { it.weight } + table.herbWeight + table.gemWeight
            assertEquals(
                ZombieDrops.TABLE_SIZE,
                total,
                "The $name zombie table sums to $total, not ${ZombieDrops.TABLE_SIZE}. Either a row " +
                    "is wrong or the members-rate reading in ZombieDrops no longer holds.",
            )
        }
    }

    @Test
    fun `every drop row names a real item`() {
        tables.forEach { (name, table) ->
            table.rows.forEach { row ->
                val id = row.item ?: return@forEach
                assertTrue(id > 0, "The $name table has a row with unresolved item id $id.")
                assertNotNull(CacheManager.getItem(id), "The $name table drops item $id, which is not in this cache.")
                assertTrue(row.min in 1..row.max, "The $name table has a row with quantity ${row.min}..${row.max}.")
            }
        }
    }

    /** The tertiaries are hand-rolled rather than table rows, so their keys get their own check. */
    @Test
    fun `the tertiary drops name real items`() {
        listOf("item.bones", "item.zombie_champion_scroll", "item.looting_bag").forEach { key ->
            val id = getRSCM(key)
            assertTrue(id > 0, "$key did not resolve to an item id.")
            assertNotNull(CacheManager.getItem(id), "$key ($id) is not in this cache.")
        }
    }

    // ---------------------------------------------------------------------------- the stats

    /**
     * The package declares no `setCombatDef`, so everything but respawn, aggression and Slayer
     * experience comes from `monsterStats.json`. This asserts it is all still there, through the
     * table's own loader rather than by re-reading the file.
     */
    @Test
    fun `monsterStats still carries every zombie`() {
        Zombies.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val id = getRSCM(key)
                val def =
                    assertNotNull(
                        world.npcStats[id],
                        "$key ($id) has no row in monsterStats.json - it would spawn as NpcCombatDef.DEFAULT.",
                    )
                assertTrue(def.hitpoints > NpcCombatDef.DEFAULT.hitpoints, "$key ($id) has default hitpoints.")
                assertTrue(NpcSpecies.UNDEAD in def.species, "$key ($id) is not UNDEAD, so the salve amulet ignores it.")
                assertEquals(
                    Elements.FIRE.ordinal,
                    def.elementalWeaknessElement,
                    "$key ($id) has lost its Fire elemental weakness.",
                )
                assertEquals(50, def.elementalWeaknessPercent, "$key ($id) has the wrong Fire weakness percentage.")
            }
        }
    }

    /**
     * One id per variant against the wiki infobox, so a wholesale change to the stats table cannot
     * pass the coverage check above unnoticed.
     */
    @Test
    fun `the published stat blocks are intact`() {
        assertStats("npc.zombie", hitpoints = 22, attack = 8, strength = 9, defence = 10, speed = 4, style = CombatStyle.CRUSH)
        assertStats("npc.zombie_42", hitpoints = 24, attack = 13, strength = 13, defence = 18, speed = 5, style = CombatStyle.SLASH)
        assertStats("npc.zombie_45", hitpoints = 24, attack = 13, strength = 13, defence = 18, speed = 5, style = CombatStyle.SLASH)
        assertStats("npc.zombie_49", hitpoints = 30, attack = 19, strength = 21, defence = 16, speed = 5, style = CombatStyle.SLASH)
        assertStats("npc.zombie_59", hitpoints = 30, attack = 19, strength = 21, defence = 16, speed = 5, style = CombatStyle.SLASH)
    }

    private fun assertStats(
        key: String,
        hitpoints: Int,
        attack: Int,
        strength: Int,
        defence: Int,
        speed: Int,
        style: CombatStyle,
    ) {
        val def = assertNotNull(world.npcStats[getRSCM(key)], "$key has no row in monsterStats.json.")
        assertEquals(hitpoints, def.hitpoints, "$key hitpoints")
        assertEquals(attack, def.attack, "$key attack")
        assertEquals(strength, def.strength, "$key strength")
        assertEquals(defence, def.defence, "$key defence")
        assertEquals(speed, def.attackSpeed, "$key attack speed")
        assertEquals(style, def.combatStyle, "$key combat style")
    }

    // --------------------------------------------------------------------------- the plugin

    /**
     * The plugins' `init` really runs here, and a zombie is really taken through
     * `World.setNpcDefaults` and `PluginRepository.executeNpcSpawn` - the same two calls
     * `NpcDeathAction` makes on respawn.
     *
     * That is the only way to catch the failure this design is exposed to: a plugin whose
     * constructor throws registers *nothing*, silently, and the symptom would be zombies that spawn
     * and fight but are passive, respawn on the wrong timer and give no Slayer experience.
     */
    @Test
    fun `the plugins build and patch every zombie at spawn`() {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        NpcMetadataService().loadMonsterStats(world, STATS_CONFIG)
        val repo = world.plugins
        ZombiePlugin(repo, world, Server())
        ZombieSpawnPlugin(repo, world, Server())

        Zombies.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val id = getRSCM(key)
                val npc = Npc(id, Tile(3200, 3200), world)
                world.setNpcDefaults(npc)
                repo.executeNpcSpawn(npc)
                val def = npc.combatDef

                assertEquals(Zombies.RESPAWN_CYCLES, def.respawnDelay, "$key respawn delay")
                assertEquals(Zombies.AGGRO_RADIUS, def.aggressiveRadius, "$key aggression radius")
                assertEquals(Zombies.AGGRO_SEARCH_DELAY, def.aggroTargetDelay, "$key aggro search delay")
                assertEquals(Zombies.AGGRO_TIMER, def.aggressiveTimer, "$key aggression timer")
                assertEquals(variant.slayerXp, def.slayerXp, "$key Slayer experience")

                // The patch must not have cost the monsterStats block it was layered onto.
                assertTrue(def.hitpoints > NpcCombatDef.DEFAULT.hitpoints, "$key lost its hitpoints to the patch.")
                assertTrue(NpcSpecies.UNDEAD in def.species, "$key lost UNDEAD to the patch.")
                assertEquals(Elements.FIRE.ordinal, def.elementalWeaknessElement, "$key lost its Fire weakness.")
            }
        }
    }

    /**
     * The load-bearing invariant of this package: no zombie may have a hand-written combat def.
     *
     * One would take the npc out of `World.setNpcDefaults`' `monsterStats.json` tier *and* off
     * `MonsterAnimationsPlugin`'s resolver path, replacing wiki stats with whatever was typed here
     * and the zombie animations with the human 422/424/836 fallbacks - both silently.
     */
    @Test
    fun `no zombie declares a combat def`() {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        val repo = world.plugins
        ZombiePlugin(repo, world, Server())
        ZombieSpawnPlugin(repo, world, Server())

        val declared = Zombies.ALL_IDS.filter { repo.npcCombatDefs.containsKey(getRSCM(it)) }
        assertTrue(declared.isEmpty(), "These zombies declare a combat def and would lose monsterStats.json: $declared")
    }

    // ---------------------------------------------------------------------------- the task

    /**
     * The `Zombies` Slayer category shipped with an empty monster list, which
     * `SlayerService.markAvailable` reads as "not assignable". This checks it names the zombies and
     * that nothing it names is already claimed by an earlier task - `SlayerService` maps an npc to
     * a task with `putIfAbsent`, so a duplicate name is a silent no-op rather than a second owner.
     */
    @Test
    fun `the Zombies slayer task is wired and claims no one else's monsters`() {
        val tasks = ObjectMapper().readTree(TASKS_CONFIG)
        val names = tasks.map { it.path("name").asText() }
        val zombieIndex = names.indexOf("Zombies")
        assertTrue(zombieIndex >= 0, "tasks.json has no 'Zombies' category.")

        val monsters = tasks[zombieIndex].path("monsters").map { it.asText() }
        assertTrue("Zombie" in monsters, "The Zombies task does not name 'Zombie', so it can never be assigned.")

        val claimedEarlier =
            tasks.take(zombieIndex)
                .flatMap { task -> task.path("monsters").map { task.path("name").asText() to it.asText() } }
                .filter { (_, monster) -> monster in monsters }
                .map { (task, monster) -> "'$monster' is already claimed by '$task'" }
        assertTrue(claimedEarlier.isEmpty(), claimedEarlier.joinToString("\n"))
    }

    /**
     * The task is only assignable if one of its monsters is actually spawned, so the ids the
     * category resolves to have to overlap what [Zombies.CAMPS] puts in the world.
     */
    @Test
    fun `the Zombies task resolves to spawned npcs`() {
        val zombieIds =
            CacheManager.getNpcs().entries
                .filter { (_, def) -> def.name == "Zombie" }
                .map { (id, _) -> id }
                .toSet()
        val spawned = Zombies.CAMPS.flatMap { camp -> camp.npcKeys.map { getRSCM(it) } }.toSet()
        assertTrue(
            spawned.any { it in zombieIds },
            "No spawned zombie is named 'Zombie' in the cache, so the Slayer task cannot become assignable.",
        )
    }
}
