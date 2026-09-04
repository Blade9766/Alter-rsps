package org.alter.plugins.diag

import com.fasterxml.jackson.databind.ObjectMapper
import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.TileData
import dev.openrune.cache.filestore.loadTerrain
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.combat.NpcCombatDef
import org.alter.game.model.entity.Npc
import org.alter.game.service.game.NpcMetadataService
import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin
import org.alter.plugins.content.npcs.bandit.BanditDrops
import org.alter.plugins.content.npcs.bandit.BanditPlugin
import org.alter.plugins.content.npcs.bandit.BanditSpawnPlugin
import org.alter.plugins.content.npcs.bandit.Bandits
import org.alter.plugins.content.npcs.dwarf.DwarfDrops
import org.alter.plugins.content.npcs.dwarf.DwarfPlugin
import org.alter.plugins.content.npcs.dwarf.DwarfSpawnPlugin
import org.alter.plugins.content.npcs.dwarf.Dwarves
import org.alter.plugins.content.npcs.ghost.GhostPlugin
import org.alter.plugins.content.npcs.ghost.GhostSpawnPlugin
import org.alter.plugins.content.npcs.ghost.Ghosts
import org.alter.plugins.content.npcs.hobgoblin.HobgoblinDrops
import org.alter.plugins.content.npcs.hobgoblin.HobgoblinPlugin
import org.alter.plugins.content.npcs.hobgoblin.HobgoblinSpawnPlugin
import org.alter.plugins.content.npcs.hobgoblin.Hobgoblins
import org.alter.plugins.content.npcs.mossgiant.MossGiantDrops
import org.alter.plugins.content.npcs.mossgiant.MossGiantPlugin
import org.alter.plugins.content.npcs.mossgiant.MossGiantSpawnPlugin
import org.alter.plugins.content.npcs.mossgiant.MossGiants
import org.alter.plugins.content.npcs.thief.ThiefDrops
import org.alter.plugins.content.npcs.thief.ThiefPlugin
import org.alter.plugins.content.npcs.thief.ThiefSpawnPlugin
import org.alter.plugins.content.npcs.thief.Thieves
import org.alter.plugins.content.npcs.unicorn.UnicornPlugin
import org.alter.plugins.content.npcs.unicorn.UnicornSpawnPlugin
import org.alter.plugins.content.npcs.unicorn.Unicorns
import org.alter.plugins.content.npcs.wolf.WolfPlugin
import org.alter.plugins.content.npcs.wolf.WolfSpawnPlugin
import org.alter.plugins.content.npcs.wolf.Wolves
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
 * Verify-before-wire checks for the eight bestiary packages added alongside `content/npcs/zombie` -
 * `dwarf`, `wolf`, `unicorn`, `thief`, `ghost`, `bandit`, `hobgoblin` and `mossgiant`.
 *
 * `ZombieVerify` does the same job for the zombies and this is deliberately its sibling rather than
 * eight more copies of it: every one of these packages is built on the same three load-bearing
 * decisions, and each of the three fails *silently* if it stops holding.
 *
 * - **The spawn tiles.** Around 640 of them, most in dungeons, sewers, crypts and mountain paths
 *   where a published wiki pin landing inside a wall is routine - the Catacombs cost
 *   `content/npcs/dungeon/HillGiantSpawns` eight of thirteen pins for exactly that reason. Four of
 *   these species are **size 2**, so the check is the whole footprint rather than one tile.
 * - **The drop tables summing to their published denominator.** That is the entire argument for
 *   reading the members coin rates rather than the free-to-play ones, and for treating the herb,
 *   seed and gem lines as rows rather than as independent rolls (see [MonsterDropTable]). If a table
 *   stops summing, one of those two decisions has quietly become wrong and every row's rate with it.
 * - **That `monsterStats.json` still carries the stats.** None of these packages writes a
 *   `setCombatDef`; they take that table as given. A monster falling out of it does not break
 *   loudly - it quietly becomes a 10-hitpoint, zero-defence creature with no species and no
 *   elemental weakness, *and* loses its animations to the human fallbacks.
 *
 * On top of those, the animation entries these packages corrected get their own checks, because the
 * bug they fix - `WOLF` holding the hellhound's animation set - was invisible for as long as no
 * wolf was spawned anywhere.
 */
class BestiaryVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        private val STATS_CONFIG = File("../data/cfg/npcs/monsterStats.json")
        private val TASKS_CONFIG = File("../data/cfg/slayer/tasks.json")
        /** `standAnim` of the `Ghost` model 21154 rig, whose actions are `ALT_GHOST`'s. */
        const val ALT_GHOST_STAND = 5538

        private val NAMED_MEDIA = File("src/main/resources/npc-animations/named-combat-media.json")
        private val ID_MEDIA = File("src/main/resources/npc-animations/id-combat-media.json")

        /** A world carrying nothing but the monster stats table, as [MonsterStatsVerify] builds it. */
        private val world: World by lazy {
            World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT).also {
                NpcMetadataService().loadMonsterStats(it, STATS_CONFIG)
            }
        }

        /** One published version of a monster: what the wiki says it is, and which cache ids it is. */
        private data class VariantSpec(
            val name: String,
            /** The `Infobox Monster` `name`, which is what the cache should call it. */
            val cacheName: String,
            val combatLevel: Int,
            val npcKeys: List<String>,
        )

        /** One camp, flattened out of whichever package's own `Camp` type it came from. */
        private data class CampSpec(
            val location: String,
            val plane: Int,
            val npcKeys: List<String>,
            val tiles: List<Pair<Int, Int>>,
        )

        private data class SpeciesSpec(
            val label: String,
            /** Cache footprint, in tiles. Wolves, unicorns and moss giants are 2. */
            val size: Int,
            val variants: List<VariantSpec>,
            /**
             * The package's own `ALL_KEYS`, taken rather than recomputed from [variants].
             *
             * `ZombieVerify` uses `Zombies.ALL_IDS` the same way, and for the same reason: the
             * combat-def check below is about *every* npc the package claims, so it should ask the
             * package rather than re-derive the answer and risk checking a different set.
             */
            val allKeys: List<String>,
            val camps: List<CampSpec>,
            /** The package's weighted tables, by the label their doc uses. */
            val tables: Map<String, MonsterDropTable> = emptyMap(),
            /** Item rscm keys the package drops outside a table - bones, clues, tertiaries. */
            val tertiaryKeys: List<String> = emptyList(),
        )

        private val SPECIES: List<SpeciesSpec> by lazy {
            listOf(
                SpeciesSpec(
                    label = "dwarf",
                    size = 1,
                    variants =
                        Dwarves.VARIANTS.map {
                            VariantSpec(it.name, "Dwarf", it.combatLevel, listOf(it.npcKey))
                        },
                    allKeys = Dwarves.ALL_KEYS,
                    camps = Dwarves.CAMPS.map { CampSpec(it.location, it.plane, it.npcKeys, it.tiles) },
                    tables = mapOf("dwarf" to DwarfDrops.TABLE),
                    tertiaryKeys = listOf("item.bones", "item.clue_scroll_beginner"),
                ),
                SpeciesSpec(
                    label = "wolf",
                    size = 2,
                    variants =
                        Wolves.VARIANTS.map {
                            VariantSpec(it.name, if (it.whiteWolf) "White wolf" else "Wolf", it.combatLevel, it.npcKeys)
                        },
                    allKeys = Wolves.ALL_KEYS,
                    camps = Wolves.CAMPS.map { CampSpec(it.location, 0, it.npcKeys, it.tiles) },
                    tertiaryKeys = listOf("item.wolf_bones", "item.looting_bag"),
                ),
                SpeciesSpec(
                    label = "unicorn",
                    size = 2,
                    variants =
                        Unicorns.VARIANTS.map {
                            VariantSpec(it.name, if (it.adult) "Unicorn" else "Unicorn Foal", it.combatLevel, it.npcKeys)
                        },
                    allKeys = Unicorns.ALL_KEYS,
                    camps = Unicorns.CAMPS.map { CampSpec(it.location, 0, it.npcKeys, it.tiles) },
                    tertiaryKeys = listOf("item.bones", "item.unicorn_horn", "item.ensouled_unicorn_head"),
                ),
                SpeciesSpec(
                    label = "thief",
                    size = 1,
                    variants =
                        Thieves.VARIANTS.map {
                            VariantSpec(it.name, "Thief", it.combatLevel, listOf(it.npcKey))
                        },
                    allKeys = Thieves.ALL_KEYS,
                    camps = Thieves.CAMPS.map { CampSpec(it.location, it.plane, it.npcKeys, it.tiles) },
                    tables = mapOf("thief" to ThiefDrops.TABLE),
                    tertiaryKeys = listOf("item.bones", "item.clue_scroll_beginner", "item.clue_scroll_easy"),
                ),
                SpeciesSpec(
                    label = "ghost",
                    size = 1,
                    variants =
                        Ghosts.VARIANTS.map {
                            VariantSpec(it.name, "Ghost", it.combatLevel, it.npcKeys)
                        },
                    allKeys = Ghosts.ALL_KEYS,
                    camps = Ghosts.CAMPS.map { CampSpec(it.location, it.plane, it.npcKeys, it.tiles) },
                    tertiaryKeys = listOf("item.looting_bag", "item.clue_scroll_beginner"),
                ),
                SpeciesSpec(
                    label = "bandit",
                    size = 1,
                    variants =
                        Bandits.VARIANTS.map {
                            VariantSpec(it.name, "Bandit", it.combatLevel, listOf(it.npcKey))
                        },
                    allKeys = Bandits.ALL_KEYS,
                    camps = Bandits.CAMPS.map { CampSpec(it.location, 0, it.npcKeys, it.tiles) },
                    tables = mapOf("level 22" to BanditDrops.LEVEL_22, "level 130" to BanditDrops.LEVEL_130),
                    tertiaryKeys = listOf("item.bones", "item.looting_bag", "item.clue_scroll_hard"),
                ),
                SpeciesSpec(
                    label = "hobgoblin",
                    size = 1,
                    variants =
                        Hobgoblins.VARIANTS.map {
                            VariantSpec(it.name, "Hobgoblin", it.combatLevel, listOf(it.npcKey))
                        },
                    allKeys = Hobgoblins.ALL_KEYS,
                    camps = Hobgoblins.CAMPS.map { CampSpec(it.location, it.plane, it.npcKeys, it.tiles) },
                    tables = mapOf("unarmed" to HobgoblinDrops.UNARMED, "armed" to HobgoblinDrops.ARMED),
                    tertiaryKeys =
                        listOf(
                            "item.bones", "item.hobgoblin_champion_scroll",
                            "item.looting_bag", "item.clue_scroll_beginner",
                        ),
                ),
                SpeciesSpec(
                    label = "moss giant",
                    size = 2,
                    variants =
                        MossGiants.VARIANTS.map {
                            VariantSpec(it.name, "Moss giant", it.combatLevel, it.npcKeys)
                        },
                    allKeys = MossGiants.ALL_KEYS,
                    camps = MossGiants.CAMPS.map { CampSpec(it.location, 0, it.npcKeys, it.tiles) },
                    tables = mapOf("moss giant" to MossGiantDrops.TABLE),
                    tertiaryKeys =
                        listOf(
                            "item.big_bones", "item.looting_bag", "item.ensouled_giant_head",
                            "item.clue_scroll_beginner", "item.mossy_key", "item.long_bone",
                            "item.giant_champion_scroll", "item.curved_bone",
                        ),
                ),
            )
        }
    }

    /** Line separator for the multi-line assertion messages below. */
    private val NEWLINE = System.lineSeparator()

    private val terrain = HashMap<Int, Array<Array<Array<TileData>>>?>()

    /**
     * Why [x], [z] on [plane] cannot hold a spawn, or null if it can.
     *
     * Both tests apply on **every** plane, which was worth checking before relying on: an earlier
     * version of this skipped the floor test above plane 0 on the assumption that upper floors are
     * built out of objects rather than terrain. They are not - Draynor Manor's first floor, the
     * Falador Mining Guild's and Lovakengj's all carry real terrain paint at their own plane (520,
     * 625 and 78 painted tiles in their respective mapsquares). So the strict test costs nothing and
     * catches a plane that is genuinely empty, which is how the God Wars Dungeon and The Warrens
     * turned out to be built on plane 2 rather than the plane 0 their `LocLine`s claim.
     */
    private fun problem(
        x: Int,
        z: Int,
        plane: Int,
    ): String? {
        val rx = x shr 6
        val rz = z shr 6
        val tiles =
            terrain.getOrPut((rx shl 8) or rz) {
                CacheManager.cache.data(MAPS, "m${rx}_$rz")?.let { loadTerrain(it) }
            } ?: return "mapsquare ${rx}_$rz is not in the cache"
        val data = tiles[plane][x - (rx shl 6)][z - (rz shl 6)]
        return when {
            data.overlayId.toInt() == 0 && data.underlayId.toInt() == 0 -> "no floor"
            (data.settings.toInt() and 0x1) != 0 -> "flagged BLOCK_WALK"
            else -> null
        }
    }

    /** Every tile a spawn of [size] rooted at [x], [z] occupies - its tile is the south-west corner. */
    private fun footprint(
        x: Int,
        z: Int,
        size: Int,
    ): List<Pair<Int, Int>> = (0 until size).flatMap { dx -> (0 until size).map { dz -> (x + dx) to (z + dz) } }

    // ------------------------------------------------------------------------------ npc ids

    @Test
    fun `every variant id is the right monster at its published combat level`() {
        val failures = mutableListOf<String>()
        SPECIES.forEach { species ->
            species.variants.forEach { variant ->
                variant.npcKeys.forEach { key ->
                    val id = getRSCM(key)
                    if (id <= 0) {
                        failures += "$key did not resolve to an npc id"
                        return@forEach
                    }
                    val def = CacheManager.getNpc(id)
                    if (def == null) {
                        failures += "$key ($id) is not in this cache"
                        return@forEach
                    }
                    if (!variant.cacheName.equals(def.name, ignoreCase = true)) {
                        failures += "$key ($id) is named '${def.name}', not '${variant.cacheName}'"
                    }
                    if (def.size != species.size) {
                        failures += "$key ($id) is size ${def.size}, not ${species.size}"
                    }
                    if (def.combatLevel != variant.combatLevel) {
                        failures += "$key ($id) is level ${def.combatLevel}, not ${variant.combatLevel} as '${variant.name}'"
                    }
                    if (def.actions.filterNotNull().none { it.equals("Attack", ignoreCase = true) }) {
                        failures += "$key ($id) has no Attack option"
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), "Bad bestiary npc ids:" + NEWLINE + failures.joinToString(NEWLINE))
    }

    @Test
    fun `no npc id is claimed by two variants of the same species`() {
        val failures = mutableListOf<String>()
        SPECIES.forEach { species ->
            val seen = HashMap<String, String>()
            species.variants.forEach { variant ->
                variant.npcKeys.forEach { key ->
                    seen.put(key, variant.name)?.let { previous ->
                        failures += "${species.label}: $key is in both '$previous' and '${variant.name}'"
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString(NEWLINE))
    }

    // ------------------------------------------------------------------------------- spawns

    @Test
    fun `every spawned id has a variant`() {
        val failures = mutableListOf<String>()
        SPECIES.forEach { species ->
            val defined = species.variants.flatMap { it.npcKeys }.toSet()
            species.camps.forEach { camp ->
                camp.npcKeys.filterNot { it in defined }.forEach {
                    failures += "${species.label}: ${camp.location} spawns $it, which has no variant"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString(NEWLINE))
    }

    @Test
    fun `every spawn footprint is standable`() {
        val failures = mutableListOf<String>()
        SPECIES.forEach { species ->
            species.camps.forEach { camp ->
                camp.tiles.forEach { (x, z) ->
                    footprint(x, z, species.size).forEach { (fx, fz) ->
                        problem(fx, fz, camp.plane)?.let { why ->
                            val at = if (species.size == 1) "($x, $z)" else "($x, $z) footprint tile ($fx, $fz)"
                            failures += "${species.label}: ${camp.location} $at plane ${camp.plane}: $why"
                        }
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), "Unusable bestiary spawn tiles:" + NEWLINE + failures.joinToString(NEWLINE))
    }

    /**
     * Across all eight packages at once, not just within one.
     *
     * These species share locations - the Varrock Sewers carry ghosts, moss giants and (already)
     * zombies; White Wolf Mountain carries grey wolves and white ones - so a duplicated coordinate
     * between two packages is a real possibility and would stack two monsters on one tile.
     */
    @Test
    fun `no two bestiary spawns share a tile`() {
        val placed =
            SPECIES.flatMap { species ->
                species.camps.flatMap { camp ->
                    camp.tiles.map { Triple(camp.plane, it, "${species.label} / ${camp.location}") }
                }
            }
        val clashes =
            placed.groupBy { it.first to it.second }
                .filterValues { it.size > 1 }
                .map { (where, rows) -> "$where is claimed by ${rows.joinToString { it.third }}" }
        assertTrue(clashes.isEmpty(), "Overlapping bestiary spawns:" + NEWLINE + clashes.joinToString(NEWLINE))
    }

    /**
     * Every id a camp names is actually placed somewhere.
     *
     * This is what [SpawnDealer] exists for, and it is the check that forced it. With a per-camp
     * `tileIndex % npcKeys.size` the wiki's twenty level 19 ghost ids were dealt against locations
     * of two, three and five pins, so npc 85 stood in all twelve of them and five other ghosts stood
     * nowhere at all. The dealer keys its cursor on the id **pool** instead, so the twelve camps
     * sharing `LEVEL_19_IDS` share one running count and all twenty get used.
     *
     * The dealer is walked here in the same camp order the spawn plugins walk it, so this asserts
     * what the plugins really do rather than a model of it.
     */
    @Test
    fun `every id a camp names is dealt to at least one pin`() {
        val failures = mutableListOf<String>()
        SPECIES.forEach { species ->
            val dealer = SpawnDealer()
            val dealt = species.camps.flatMap { camp -> camp.tiles.map { dealer.next(camp.npcKeys) } }.toSet()
            (species.camps.flatMap { it.npcKeys }.toSet() - dealt).forEach {
                failures += "${'$'}{species.label}: ${'$'}it is named by a camp but never placed"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString(NEWLINE))
    }

    // -------------------------------------------------------------------------- drop tables

    /**
     * The single most important check here. See the class doc: the exact sum is the whole basis for
     * reading the members coin rates and for counting the sub-table lines as rows.
     */
    @Test
    fun `every drop table sums to its published denominator`() {
        val failures = mutableListOf<String>()
        SPECIES.forEach { species ->
            species.tables.forEach { (name, table) ->
                if (table.total != table.denominator) {
                    failures +=
                        "${species.label} '$name' sums to ${table.total}, not ${table.denominator} - either a row is " +
                            "wrong or the members-rate reading no longer holds"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString(NEWLINE))
    }

    @Test
    fun `every drop row names a real item`() {
        val failures = mutableListOf<String>()
        SPECIES.forEach { species ->
            species.tables.forEach { (name, table) ->
                table.rows.forEach { row ->
                    val id = row.item ?: return@forEach
                    when {
                        id <= 0 -> failures += "${species.label} '$name' has an unresolved item id $id"
                        CacheManager.getItem(id) == null -> failures += "${species.label} '$name' drops item $id, not in this cache"
                    }
                    if (row.min !in 1..row.max) {
                        failures += "${species.label} '$name' has a row with quantity ${row.min}..${row.max}"
                    }
                    if (row.weight <= 0) {
                        failures += "${species.label} '$name' has a row with weight ${row.weight}"
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString(NEWLINE))
    }

    /** The tertiaries are hand-rolled rather than table rows, so their keys get their own check. */
    @Test
    fun `every tertiary drop names a real item`() {
        val failures = mutableListOf<String>()
        SPECIES.forEach { species ->
            species.tertiaryKeys.forEach { key ->
                val id = getRSCM(key)
                when {
                    id <= 0 -> failures += "${species.label}: $key did not resolve to an item id"
                    CacheManager.getItem(id) == null -> failures += "${species.label}: $key ($id) is not in this cache"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString(NEWLINE))
    }

    /**
     * A seed weight with no seed table, or the general seed table with no combat level, would throw
     * out of [MonsterDropTable]'s own `init` - but only when the object is first touched, which for
     * an `object` field is inside an `onNpcDeath` handler during a kill. Touching every table here
     * moves that failure to build time.
     */
    @Test
    fun `every table's sub-table configuration is consistent`() {
        SPECIES.forEach { species ->
            species.tables.forEach { (name, table) ->
                assertTrue(table.denominator > 0, "${species.label} '$name' has denominator ${table.denominator}")
                assertTrue(table.rows.isNotEmpty(), "${species.label} '$name' has no rows")
            }
        }
    }

    // ---------------------------------------------------------------------------- the stats

    /**
     * None of these packages declares a `setCombatDef`, so everything but respawn, aggression and
     * Slayer experience comes from `monsterStats.json`. This asserts it is all still there, through
     * the table's own loader rather than by re-reading the file.
     */
    @Test
    fun `monsterStats still carries every bestiary monster`() {
        val failures = mutableListOf<String>()
        SPECIES.forEach { species ->
            species.variants.forEach { variant ->
                variant.npcKeys.forEach { key ->
                    val id = getRSCM(key)
                    val def = world.npcStats[id]
                    if (def == null) {
                        failures += "${species.label}: $key ($id) has no row - it would spawn as NpcCombatDef.DEFAULT"
                        return@forEach
                    }
                    if (def.hitpoints <= 0) {
                        failures += "${species.label}: $key ($id) has ${def.hitpoints} hitpoints"
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString(NEWLINE))
    }

    /**
     * One id per species against the wiki infobox, so a wholesale change to the stats table cannot
     * pass the coverage check above unnoticed.
     */
    @Test
    fun `the published stat blocks are intact`() {
        assertStats("npc.dwarf_292", hitpoints = 26, attack = 15, strength = 16, defence = 16, speed = 6)
        assertStats("npc.wolf", hitpoints = 69, attack = 50, strength = 55, defence = 52, speed = 4)
        assertStats("npc.white_wolf_108", hitpoints = 44, attack = 30, strength = 31, defence = 32, speed = 4)
        assertStats("npc.unicorn", hitpoints = 19, attack = 11, strength = 13, defence = 13, speed = 4)
        assertStats("npc.unicorn_foal", hitpoints = 15, attack = 10, strength = 10, defence = 10, speed = 4)
        assertStats("npc.thief_3252", hitpoints = 17, attack = 14, strength = 13, defence = 12, speed = 4)
        assertStats("npc.ghost", hitpoints = 25, attack = 13, strength = 13, defence = 18, speed = 4)
        assertStats("npc.ghost_2527", hitpoints = 80, attack = 63, strength = 63, defence = 68, speed = 4)
        assertStats("npc.bandit_1026", hitpoints = 27, attack = 17, strength = 17, defence = 17, speed = 4)
        assertStats("npc.bandit_6605", hitpoints = 155, attack = 57, strength = 57, defence = 57, speed = 4)
        assertStats("npc.hobgoblin_3050", hitpoints = 49, attack = 33, strength = 31, defence = 36, speed = 6)
        assertStats("npc.moss_giant", hitpoints = 60, attack = 30, strength = 30, defence = 30, speed = 6)
        assertStats("npc.moss_giant_3851", hitpoints = 85, attack = 30, strength = 30, defence = 30, speed = 6)
    }

    private fun assertStats(
        key: String,
        hitpoints: Int,
        attack: Int,
        strength: Int,
        defence: Int,
        speed: Int,
    ) {
        val def = assertNotNull(world.npcStats[getRSCM(key)], "$key has no row in monsterStats.json.")
        assertEquals(hitpoints, def.hitpoints, "$key hitpoints")
        assertEquals(attack, def.attack, "$key attack")
        assertEquals(strength, def.strength, "$key strength")
        assertEquals(defence, def.defence, "$key defence")
        assertEquals(speed, def.attackSpeed, "$key attack speed")
    }

    // -------------------------------------------------------------------------- the plugins

    /**
     * Every plugin's `init` really runs here, and one monster of each is really taken through
     * `World.setNpcDefaults` and `PluginRepository.executeNpcSpawn` - the same two calls
     * `NpcDeathAction` makes on respawn.
     *
     * That is the only way to catch the failure this design is exposed to: a plugin whose
     * constructor throws registers *nothing*, silently, and the symptom would be monsters that spawn
     * and fight but are passive, respawn on the wrong timer and give no Slayer experience.
     */
    @Test
    fun `every plugin builds and patches its monsters at spawn`() {
        val world = buildWorld()
        val repo = world.plugins

        fun defOf(key: String): NpcCombatDef {
            val npc = Npc(getRSCM(key), Tile(3200, 3200), world)
            world.setNpcDefaults(npc)
            repo.executeNpcSpawn(npc)
            return npc.combatDef
        }

        // Dwarves: respawn and Slayer experience only - every version is `aggressive = No`.
        Dwarves.VARIANTS.forEach { variant ->
            val def = defOf(variant.npcKey)
            assertEquals(variant.respawnCycles, def.respawnDelay, "${variant.name} respawn delay")
            assertEquals(variant.slayerXp, def.slayerXp, "${variant.name} Slayer experience")
            assertEquals(0, def.aggressiveRadius, "${variant.name} should not be aggressive")
            // Not `> DEFAULT.hitpoints`: `Worker (8)` really does have ten, exactly the engine
            // default, so whether the patch kept the stat block has to be checked against the table
            // rather than against a magic number.
            assertEquals(
                world.npcStats[getRSCM(variant.npcKey)]?.hitpoints,
                def.hitpoints,
                "${variant.name} lost its monsterStats hitpoints to the patch",
            )
        }

        // Wolves: the two Stronghold of Security versions are the only non-aggressive ones.
        Wolves.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val def = defOf(key)
                assertEquals(Wolves.RESPAWN_CYCLES, def.respawnDelay, "$key respawn delay")
                assertEquals(variant.slayerXp, def.slayerXp, "$key Slayer experience")
                if (variant.aggressive) {
                    assertEquals(Wolves.AGGRO_RADIUS, def.aggressiveRadius, "$key aggression radius")
                    assertEquals(Wolves.AGGRO_TIMER, def.aggressiveTimer, "$key aggression timer")
                } else {
                    assertEquals(0, def.aggressiveRadius, "$key should not be aggressive")
                }
            }
        }

        Unicorns.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val def = defOf(key)
                assertEquals(Unicorns.RESPAWN_CYCLES, def.respawnDelay, "$key respawn delay")
                assertEquals(0, def.aggressiveRadius, "$key should not be aggressive")
            }
        }

        Thieves.VARIANTS.forEach { variant ->
            val def = defOf(variant.npcKey)
            assertEquals(variant.respawnCycles, def.respawnDelay, "${variant.name} respawn delay")
            assertEquals(0, def.aggressiveRadius, "${variant.name} should not be aggressive")
        }

        Ghosts.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val def = defOf(key)
                assertEquals(Ghosts.RESPAWN_CYCLES, def.respawnDelay, "$key respawn delay")
                assertEquals(Ghosts.AGGRO_RADIUS, def.aggressiveRadius, "$key aggression radius")
                assertEquals(Ghosts.AGGRO_TIMER, def.aggressiveTimer, "$key aggression timer")
                assertEquals(variant.slayerXp, def.slayerXp, "$key Slayer experience")
            }
        }

        Bandits.VARIANTS.forEach { variant ->
            val def = defOf(variant.npcKey)
            assertEquals(Bandits.RESPAWN_CYCLES, def.respawnDelay, "${variant.name} respawn delay")
            assertEquals(Bandits.AGGRO_TIMER, def.aggressiveTimer, "${variant.name} aggression timer")
            assertEquals(variant.slayerXp, def.slayerXp, "${variant.name} Slayer experience")
        }

        Hobgoblins.VARIANTS.forEach { variant ->
            val def = defOf(variant.npcKey)
            assertEquals(Hobgoblins.AGGRO_RADIUS, def.aggressiveRadius, "${variant.name} aggression radius")
            assertEquals(Hobgoblins.AGGRO_TIMER, def.aggressiveTimer, "${variant.name} aggression timer")
            assertEquals(variant.slayerXp, def.slayerXp, "${variant.name} Slayer experience")
            // The page publishes no respawn at all, so the engine default must survive untouched.
            assertEquals(NpcCombatDef.DEFAULT.respawnDelay, def.respawnDelay, "${variant.name} respawn delay")
        }

        MossGiants.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val def = defOf(key)
                assertEquals(MossGiants.RESPAWN_CYCLES, def.respawnDelay, "$key respawn delay")
                assertEquals(MossGiants.AGGRO_TIMER, def.aggressiveTimer, "$key aggression timer")
                assertEquals(variant.slayerXp, def.slayerXp, "$key Slayer experience")
            }
        }
    }

    /**
     * The load-bearing invariant of all eight packages: no bestiary monster may have a hand-written
     * combat def.
     *
     * One would take the npc out of `World.setNpcDefaults`' `monsterStats.json` tier *and* off
     * `MonsterAnimationsPlugin`'s media path, replacing wiki stats with whatever was typed here and
     * the monster's animations with the human 422/424/836 fallbacks - both silently.
     */
    @Test
    fun `no bestiary monster declares a combat def`() {
        val repo = buildWorld().plugins
        val declared =
            SPECIES.flatMap { species ->
                species.allKeys
                    .filter { repo.npcCombatDefs.containsKey(getRSCM(it)) }
                    .map { "${species.label}: $it" }
            }
        assertTrue(declared.isEmpty(), "These declare a combat def and would lose monsterStats.json:\n$declared")
    }

    private fun buildWorld(): World {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        NpcMetadataService().loadMonsterStats(world, STATS_CONFIG)
        val repo = world.plugins
        val server = Server()
        DwarfPlugin(repo, world, server)
        DwarfSpawnPlugin(repo, world, server)
        WolfPlugin(repo, world, server)
        WolfSpawnPlugin(repo, world, server)
        UnicornPlugin(repo, world, server)
        UnicornSpawnPlugin(repo, world, server)
        ThiefPlugin(repo, world, server)
        ThiefSpawnPlugin(repo, world, server)
        GhostPlugin(repo, world, server)
        GhostSpawnPlugin(repo, world, server)
        BanditPlugin(repo, world, server)
        BanditSpawnPlugin(repo, world, server)
        HobgoblinPlugin(repo, world, server)
        HobgoblinSpawnPlugin(repo, world, server)
        MossGiantPlugin(repo, world, server)
        MossGiantSpawnPlugin(repo, world, server)
        return world
    }

    // ----------------------------------------------------------------------- the animations

    /**
     * The `WOLF` entry held **6581 / 6574 / 6576** before this pass, which is the hellhound's set -
     * npc 3133 is the only npc in `openosrs-animations.json` ever observed playing any of the three.
     * Both sets are frame group 1662, so the wrong one would have played rather than failing
     * visibly, and nothing noticed because no wolf was spawned anywhere.
     *
     * This pins all four entries the bestiary pass touched. Each is checked to be a real sequence in
     * this cache as well as the expected id, since a typo would otherwise surface as a monster that
     * simply does not animate.
     */
    @Test
    fun `the corrected animation entries hold their audited values`() {
        val media = ObjectMapper().readTree(NAMED_MEDIA)

        fun assertEntry(
            key: String,
            attack: Int,
            block: Int,
            death: Int,
        ) {
            val entry = media.get(key)
            assertNotNull(entry, "named-combat-media.json has no '$key' entry.")
            assertEquals(attack, entry.path("attackAnimation").asInt(), "$key attack animation")
            assertEquals(block, entry.path("blockAnimation").asInt(), "$key block animation")
            assertEquals(death, entry.path("deathAnimation").asInt(), "$key death animation")
            listOf(attack, block, death).forEach {
                assertNotNull(CacheManager.getAnims()[it], "$key references animation $it, which is not in this cache.")
            }
        }

        assertEntry("WOLF", attack = 6559, block = 6557, death = 6558)
        assertEntry("UNICORN", attack = 6376, block = 6375, death = 6377)
        assertEntry("BANDIT", attack = 386, block = 388, death = 836)
        /*
         * 422 / 425, not the 425 / 422 this pass first wrote. `AnimationRoleAudit` caught it: 422 is
         * `HUMAN_PUNCH` and 425 is `HUMAN_DEFEND_COWARDLY`, so the original values had seven thief
         * ids parrying when they meant to punch. `content/npcs/chaosdruid` had already read the same
         * rig the right way round - npc 520's observed set is "[425, 710, 422, 836] - block, this,
         * punch, death" - which is the cross-check that settles it.
         */
        assertEntry("THIEF", attack = 422, block = 425, death = 836)
        assertEntry("ARMED_HUMAN", attack = 386, block = 388, death = 836)

        // Left alone by this pass, and asserted so a future edit cannot quietly move them.
        assertEntry("DWARF", attack = 99, block = 100, death = 102)
        assertEntry("GHOST", attack = 5532, block = 5533, death = 5534)
        assertEntry("ALT_GHOST", attack = 5540, block = 5541, death = 5542)
        assertEntry("HOBGOBLIN", attack = 164, block = 165, death = 167)
        assertEntry("MOSS_GIANT", attack = 4658, block = 4657, death = 4659)
    }

    /**
     * The id-keyed overrides must name a key that exists, or the lookup falls through to the name
     * map and the override is a no-op that reads as if it worked.
     */
    @Test
    fun `every id animation override names a real entry`() {
        val named = ObjectMapper().readTree(NAMED_MEDIA)
        val overrides = ObjectMapper().readTree(ID_MEDIA)
        assertTrue(overrides.size() > 0, "id-combat-media.json is empty.")
        overrides.fields().forEach { (id, key) ->
            assertNotNull(CacheManager.getNpc(id.toInt()), "id-combat-media.json names npc $id, not in this cache.")
            assertNotNull(named.get(key.asText()), "id-combat-media.json points npc $id at '${key.asText()}', which does not exist.")
        }

        // The two thieves that carry a weapon are exactly the ones mapped to ARMED_HUMAN; the other
        // seven must be left to the name map, which gives them the unarmed set.
        val armed =
            overrides.fields().asSequence()
                .filter { it.value.asText() == "ARMED_HUMAN" }
                .map { it.key.toInt() }
                .toSet()
        assertEquals(
            Thieves.ARMED_KEYS.map { getRSCM(it) }.toSet(),
            armed,
            "id-combat-media.json's ARMED_HUMAN ids no longer match Thieves.ARMED_KEYS.",
        )
    }

    /**
     * The evidence behind the ghost overrides, re-derived from the cache rather than trusted.
     *
     * A `Ghost` built from model 21154 has `standAnim` 5538 and belongs to the `ALT_GHOST` rig;
     * every other one has 5530 and belongs to `GHOST`'s. If a future cache moves an id between the
     * two, this fails rather than silently animating a ghost on the wrong rig.
     */
    @Test
    fun `the alt-rig ghost ids are exactly the ones with the alternate idle sequence`() {
        val overrides = ObjectMapper().readTree(ID_MEDIA)
        val overridden =
            overrides.fields().asSequence()
                .filter { it.value.asText() == "ALT_GHOST" }
                .map { it.key.toInt() }
                .toSet()

        val altRig = Ghosts.ALL_KEYS.map { getRSCM(it) }.filter { CacheManager.getNpc(it)?.standAnim == ALT_GHOST_STAND }
        assertTrue(altRig.isNotEmpty(), "No spawned ghost uses the alternate rig, so the override is pointless.")
        altRig.forEach {
            assertTrue(it in overridden, "Ghost $it uses standAnim $ALT_GHOST_STAND but has no ALT_GHOST override.")
        }

        Ghosts.ALL_KEYS.map { getRSCM(it) }
            .filter { it in overridden }
            .forEach {
                assertEquals(
                    ALT_GHOST_STAND,
                    CacheManager.getNpc(it)?.standAnim,
                    "Ghost $it is overridden to ALT_GHOST but does not use that rig's idle sequence.",
                )
            }

        assertEquals(
            Ghosts.ALT_RIG_IDS.map { getRSCM(it) }.toSet(),
            altRig.toSet(),
            "Ghosts.ALT_RIG_IDS no longer matches what the cache says.",
        )
    }

    /**
     * End to end: a ghost, a thief, a wolf and a unicorn are really taken through
     * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin]'s global spawn hook, and
     * come out with the animations this pass says they should have.
     *
     * The two checks above only prove the JSON is well formed and points somewhere real. This is the
     * one that proves the id-keyed override is consulted at all - which is the whole point of it,
     * since `ALT_GHOST` was well formed and pointed somewhere real for as long as it has existed and
     * was still unreachable.
     */
    @Test
    fun `the animation plugin really applies the audited media at spawn`() {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        NpcMetadataService().loadMonsterStats(world, STATS_CONFIG)
        val repo = world.plugins
        MonsterAnimationsPlugin(repo, world, Server())

        fun animationsOf(key: String): Triple<Int, Int, Int> {
            val npc = Npc(getRSCM(key), Tile(3200, 3200), world)
            world.setNpcDefaults(npc)
            // executeNpcSpawn runs the per-npc hooks and then the global ones; only
            // MonsterAnimationsPlugin is registered on this world, so this is its hook alone.
            repo.executeNpcSpawn(npc)
            val def = npc.combatDef
            return Triple(def.attackAnimation, def.blockAnimation, def.deathAnimation.first())
        }

        // The two ghost rigs, told apart by id rather than by the name they share.
        assertEquals(Triple(5540, 5541, 5542), animationsOf("npc.ghost"), "alt-rig ghost 85")
        assertEquals(Triple(5540, 5541, 5542), animationsOf("npc.ghost_2527"), "alt-rig ghost 2527")
        assertEquals(Triple(5532, 5533, 5534), animationsOf("npc.ghost_86"), "ordinary ghost 86")

        // The armed thieves against the unarmed ones.
        assertEquals(Triple(386, 388, 836), animationsOf("npc.thief_4247"), "armed thief 4247")
        assertEquals(Triple(386, 388, 836), animationsOf("npc.thief_7916"), "armed thief 7916")
        assertEquals(Triple(422, 425, 836), animationsOf("npc.thief_3252"), "unarmed thief 3252")

        // The corrected WOLF row, and that White wolf picks it up through the suffix match.
        assertEquals(Triple(6559, 6557, 6558), animationsOf("npc.wolf"), "wolf 106")
        assertEquals(Triple(6559, 6557, 6558), animationsOf("npc.white_wolf"), "white wolf 107")

        // The new UNICORN row, and that Unicorn Foal picks it up through the prefix match.
        assertEquals(Triple(6376, 6375, 6377), animationsOf("npc.unicorn"), "unicorn 2837")
        assertEquals(Triple(6376, 6375, 6377), animationsOf("npc.unicorn_foal"), "unicorn foal 3910")

        // And the four entries this pass audited but did not change.
        assertEquals(Triple(99, 100, 102), animationsOf("npc.dwarf_290"), "dwarf 290")
        assertEquals(Triple(164, 165, 167), animationsOf("npc.hobgoblin_3049"), "hobgoblin 3049")
        assertEquals(Triple(4658, 4657, 4659), animationsOf("npc.moss_giant"), "moss giant 2090")
        assertEquals(Triple(386, 388, 836), animationsOf("npc.bandit_1026"), "bandit 1026")
    }

    // ----------------------------------------------------------------------- the slayer tasks

    /**
     * Three of these categories - `Wolves`, `Hobgoblins` and `Moss giants` - shipped with an
     * **empty** monster list, which `SlayerService.markAvailable` reads as "not assignable", so the
     * masters that carry them could never hand them out. `Dwarves` and `Ghosts` already named the
     * right monsters and only needed the Slayer experience this pass adds.
     *
     * Nothing a task names may already be claimed by an earlier task: `SlayerService` maps an npc to
     * a task with `putIfAbsent`, so a duplicate name is a silent no-op decided by file order rather
     * than a second owner.
     */
    @Test
    fun `the bestiary slayer tasks are wired and claim no one else's monsters`() {
        val tasks = ObjectMapper().readTree(TASKS_CONFIG)
        val names = tasks.map { it.path("name").asText() }

        mapOf(
            "Dwarves" to "Dwarf",
            "Ghosts" to "Ghost",
            "Wolves" to "Wolf",
            "Hobgoblins" to "Hobgoblin",
            "Moss giants" to "Moss giant",
        ).forEach { (task, monster) ->
            val index = names.indexOf(task)
            assertTrue(index >= 0, "tasks.json has no '$task' category.")

            val monsters = tasks[index].path("monsters").map { it.asText() }
            assertTrue(monster in monsters, "The $task task does not name '$monster', so it can never be assigned.")

            val claimedEarlier =
                tasks.take(index)
                    .flatMap { other -> other.path("monsters").map { other.path("name").asText() to it.asText() } }
                    .filter { (_, named) -> monsters.any { it.equals(named, ignoreCase = true) } }
                    .map { (owner, named) -> "'$named' in '$task' is already claimed by '$owner'" }
            assertTrue(claimedEarlier.isEmpty(), claimedEarlier.joinToString(NEWLINE))
        }
    }

    /**
     * A task is only assignable if one of the npcs it resolves to is actually spawned, so each
     * category's names have to overlap what these packages put in the world.
     */
    @Test
    fun `every bestiary slayer task resolves to a spawned npc`() {
        val tasks = ObjectMapper().readTree(TASKS_CONFIG)
        val spawnedNames =
            SPECIES.flatMap { species -> species.camps.flatMap { it.npcKeys } }
                .distinct()
                .mapNotNull { CacheManager.getNpc(getRSCM(it))?.name?.lowercase() }
                .toSet()

        listOf("Dwarves", "Ghosts", "Wolves", "Hobgoblins", "Moss giants").forEach { task ->
            val row = tasks.first { it.path("name").asText() == task }
            val monsters = row.path("monsters").map { it.asText().lowercase() }
            assertTrue(
                monsters.any { it in spawnedNames },
                "The $task task names $monsters, none of which is spawned, so it can never become assignable.",
            )
        }
    }
}
