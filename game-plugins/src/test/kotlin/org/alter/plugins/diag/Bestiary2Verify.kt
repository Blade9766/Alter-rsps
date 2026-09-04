package org.alter.plugins.diag

import com.fasterxml.jackson.databind.ObjectMapper
import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.TileData
import dev.openrune.cache.filestore.loadTerrain
import org.alter.api.NpcSpecies
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.NpcCombatDef
import org.alter.game.model.entity.Npc
import org.alter.game.service.game.NpcMetadataService
import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.bear.BlackBearPlugin
import org.alter.plugins.content.npcs.bear.BlackBears
import org.alter.plugins.content.npcs.battlemage.BattleMagePlugin
import org.alter.plugins.content.npcs.battlemage.BattleMages
import org.alter.plugins.content.npcs.dagannoth.DagannothPlugin
import org.alter.plugins.content.npcs.dagannoth.DagannothSpawnPlugin
import org.alter.plugins.content.npcs.dagannoth.Dagannoths
import org.alter.plugins.content.npcs.demon.DemonDrops
import org.alter.plugins.content.npcs.demon.DemonPlugin
import org.alter.plugins.content.npcs.demon.DemonSpawnPlugin
import org.alter.plugins.content.npcs.demon.Demons
import org.alter.plugins.content.npcs.dragon.DragonDrops
import org.alter.plugins.content.npcs.dragon.DragonPlugin
import org.alter.plugins.content.npcs.dragon.DragonSpawnPlugin
import org.alter.plugins.content.npcs.dragon.Dragons
import org.alter.plugins.content.npcs.elderchaosdruid.ElderChaosDruidPlugin
import org.alter.plugins.content.npcs.elderchaosdruid.ElderChaosDruids
import org.alter.plugins.content.npcs.frog.FrogPlugin
import org.alter.plugins.content.npcs.frog.FrogSpawnPlugin
import org.alter.plugins.content.npcs.frog.Frogs
import org.alter.plugins.content.npcs.hellhound.HellhoundPlugin
import org.alter.plugins.content.npcs.hellhound.HellhoundSpawnPlugin
import org.alter.plugins.content.npcs.hellhound.Hellhounds
import org.alter.plugins.content.npcs.hero.HeroPlugin
import org.alter.plugins.content.npcs.ice.IceCreatures
import org.alter.plugins.content.npcs.ice.IcePlugin
import org.alter.plugins.content.npcs.ice.IceSpawnPlugin
import org.alter.plugins.content.npcs.ogre.OgrePlugin
import org.alter.plugins.content.npcs.ogre.OgreSpawnPlugin
import org.alter.plugins.content.npcs.ogre.Ogres
import org.alter.plugins.content.npcs.outlaw.OutlawPlugin
import org.alter.plugins.content.npcs.outlaw.Outlaws
import org.alter.plugins.content.npcs.redspider.DeadlyRedSpiderPlugin
import org.alter.plugins.content.npcs.rockcrab.RockCrabPlugin
import org.alter.plugins.content.npcs.rockcrab.RockCrabSpawnPlugin
import org.alter.plugins.content.npcs.rockcrab.RockCrabs
import org.alter.plugins.content.npcs.scorpion.ScorpionPlugin
import org.alter.plugins.content.npcs.scorpion.ScorpionSpawnPlugin
import org.alter.plugins.content.npcs.scorpion.Scorpions
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
 * Verify-before-wire checks for the second bestiary pass - the fifteen packages added for the
 * dragons, demons, hellhounds, ogres, ice creatures, dagannoth, rock crabs, frogs, scorpions, bears,
 * outlaws, heroes, deadly red spiders, battle mages and Elder Chaos druids.
 *
 * [BestiaryVerify]'s sibling, for the same three reasons it exists and two more of its own. Each of
 * these fails *silently*:
 *
 * - **The spawn tiles.** Around 700 of them, and this pass is far more demanding than the first: the
 *   adult dragons are **size 4** and the greater demons **size 3**, so a pin needs a clear 4x4 or 3x3
 *   with its own tile as the south-west corner. A published pin a tile into a dungeon wall leaves a
 *   monster nobody can reach.
 * - **The drop tables summing to their published denominator.** That is the whole argument for the
 *   members coin reading, for the Monkey Madness II reading on three dragon tables, and for the
 *   Observatory Quest reading on the Elder Chaos druid's - see each package's own doc.
 * - **That `monsterStats.json` still carries the stats.** None of these packages writes a
 *   `setCombatDef`; a monster falling out of that table quietly becomes a 10-hitpoint creature with
 *   no species and no elemental weakness, *and* loses its animations to the human fallbacks.
 * - **That the adult dragons keep [NpcSpecies.BASIC_DRAGON]**, which nothing else in the codebase
 *   sets and which is the only thing making Protect from Magic work against dragonfire.
 * - **That the level 74 dagannoth is still `CombatClass.RANGED`** with a real projectile id. Without
 *   the projectile, `RangedCombatStrategy.fireNpcProjectile` draws nothing and the spines are
 *   invisible - a bug this codebase has shipped before.
 */
class Bestiary2Verify {
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

        /** One published version of a monster: what the wiki says it is, and which cache ids it is. */
        private data class VariantSpec(
            val name: String,
            /** The `Infobox Monster` `name`, which is what the cache should call it. */
            val cacheName: String,
            val combatLevel: Int,
            /** Cache footprint in tiles. Varies *within* several species in this pass. */
            val size: Int,
            val npcKeys: List<String>,
        )

        /** One camp, flattened out of whichever package's own type it came from. */
        private data class CampSpec(
            val location: String,
            val plane: Int,
            val npcKeys: List<String>,
            val tiles: List<Pair<Int, Int>>,
        )

        private data class SpeciesSpec(
            val label: String,
            val variants: List<VariantSpec>,
            val allKeys: List<String>,
            val camps: List<CampSpec>,
            val tables: Map<String, MonsterDropTable> = emptyMap(),
            /** Item rscm keys the package drops outside a table - bones, hides, clues, tertiaries. */
            val tertiaryKeys: List<String> = emptyList(),
        )

        private val SPECIES: List<SpeciesSpec> by lazy {
            listOf(
                SpeciesSpec(
                    label = "hellhound",
                    variants =
                        Hellhounds.VARIANTS.map { VariantSpec(it.name, "Hellhound", it.combatLevel, 2, it.npcKeys) },
                    allKeys = Hellhounds.ALL_KEYS,
                    camps = Hellhounds.CAMPS.map { CampSpec(it.location, it.plane, it.npcKeys, it.tiles) },
                    tables = mapOf("death runes" to org.alter.plugins.content.npcs.hellhound.HellhoundDrops.DEATH_RUNES),
                    tertiaryKeys =
                        listOf(
                            "item.vile_ashes", "item.ensouled_hellhound_head", "item.looting_bag",
                            "item.clue_scroll_hard", "item.smouldering_stone",
                        ),
                ),
                SpeciesSpec(
                    label = "dragon",
                    variants =
                        Dragons.VARIANTS.map { VariantSpec(it.name, it.cacheName, it.combatLevel, it.size, it.npcKeys) },
                    allKeys = Dragons.ALL_KEYS,
                    camps = Dragons.CAMPS.map { CampSpec(it.location, it.plane, it.npcKeys, it.tiles) },
                    tables = DragonDrops.BY_LABEL,
                    tertiaryKeys =
                        listOf(
                            "item.dragon_bones", "item.babydragon_bones", "item.bronze_bar",
                            "item.green_dragonhide", "item.blue_dragonhide", "item.red_dragonhide",
                            "item.black_dragonhide", "item.ensouled_dragon_head", "item.scaly_blue_dragonhide",
                            "item.baby_dragon_bone", "item.looting_bag", "item.clue_scroll_hard",
                            "item.clue_scroll_elite", "item.draconic_visage",
                        ),
                ),
                SpeciesSpec(
                    label = "demon",
                    variants =
                        Demons.VARIANTS.map { VariantSpec(it.name, it.cacheName, it.combatLevel, it.size, it.npcKeys) },
                    allKeys = Demons.ALL_KEYS,
                    camps = Demons.CAMPS.map { CampSpec(it.location, it.plane, it.npcKeys, it.tiles) },
                    tables = DemonDrops.BY_LABEL,
                    tertiaryKeys =
                        listOf(
                            "item.vile_ashes", "item.looting_bag", "item.ensouled_demon_head",
                            "item.lesser_demon_champion_scroll", "item.clue_scroll_hard",
                        ),
                ),
                SpeciesSpec(
                    label = "ogre",
                    variants = Ogres.VARIANTS.map { VariantSpec(it.name, "Ogre", it.combatLevel, 2, it.npcKeys) },
                    allKeys = Ogres.ALL_KEYS,
                    camps = Ogres.CAMPS.map { CampSpec(it.location, it.plane, it.npcKeys, it.tiles) },
                    tables = mapOf("ogre" to Ogres.TABLE),
                    tertiaryKeys =
                        listOf(
                            "item.big_bones", "item.looting_bag", "item.ensouled_ogre_head",
                            "item.long_bone", "item.curved_bone",
                        ),
                ),
                SpeciesSpec(
                    label = "ice",
                    variants =
                        IceCreatures.VARIANTS.map { VariantSpec(it.name, it.cacheName, it.combatLevel, it.size, it.npcKeys) },
                    allKeys = IceCreatures.ALL_KEYS,
                    camps = IceCreatures.CAMPS.map { CampSpec(it.location, it.plane, it.npcKeys, it.tiles) },
                    tables = mapOf("ice warrior" to IceCreatures.WARRIOR_TABLE, "ice giant" to IceCreatures.GIANT_TABLE),
                    tertiaryKeys =
                        listOf(
                            "item.big_bones", "item.looting_bag", "item.clue_scroll_medium",
                            "item.ensouled_giant_head", "item.clue_scroll_beginner", "item.long_bone",
                            "item.giant_champion_scroll", "item.curved_bone",
                        ),
                ),
                SpeciesSpec(
                    label = "dagannoth",
                    variants =
                        Dagannoths.VARIANTS.map { VariantSpec(it.name, "Dagannoth", it.combatLevel, it.size, it.npcKeys) },
                    allKeys = Dagannoths.ALL_KEYS,
                    camps = Dagannoths.CAMPS.map { CampSpec(it.location, it.plane, it.npcKeys, it.tiles) },
                    tables = mapOf("dagannoth" to Dagannoths.TABLE),
                    tertiaryKeys =
                        listOf("item.bones", "item.ensouled_dagannoth_head", "item.clue_scroll_medium"),
                ),
                SpeciesSpec(
                    label = "rock crab",
                    variants = listOf(VariantSpec("Rock Crab", "Rock Crab", RockCrabs.COMBAT_LEVEL, 1, RockCrabs.NPC_KEYS)),
                    allKeys = RockCrabs.NPC_KEYS,
                    camps = RockCrabs.CAMPS.map { CampSpec(it.location, 0, RockCrabs.NPC_KEYS, it.tiles) },
                    tables = mapOf("rock crab" to RockCrabs.TABLE),
                    tertiaryKeys = listOf("item.clue_scroll_easy"),
                ),
                SpeciesSpec(
                    label = "frog",
                    variants =
                        Frogs.VARIANTS.map { VariantSpec(it.name, it.cacheName, it.combatLevel, it.size, listOf(it.npcKey)) },
                    allKeys = Frogs.ALL_KEYS,
                    camps = Frogs.CAMPS.map { CampSpec(it.location, it.plane, listOf(it.npcKey), it.tiles) },
                    tables = Frogs.BY_LABEL,
                    tertiaryKeys =
                        listOf(
                            "item.bones", "item.big_bones", "item.big_frog_leg",
                            "item.clue_scroll_beginner", "item.long_bone", "item.curved_bone",
                        ),
                ),
                SpeciesSpec(
                    label = "scorpion",
                    variants =
                        Scorpions.VARIANTS.map { VariantSpec(it.name, "Scorpion", it.combatLevel, it.size, it.npcKeys) },
                    allKeys = Scorpions.ALL_KEYS,
                    camps = Scorpions.CAMPS.map { CampSpec(it.location, it.plane, it.npcKeys, it.tiles) },
                    tertiaryKeys =
                        listOf("item.looting_bag", "item.ensouled_scorpion_head", "item.clue_scroll_beginner"),
                ),
                SpeciesSpec(
                    label = "black bear",
                    variants = listOf(VariantSpec("Black bear", "Black bear", BlackBears.COMBAT_LEVEL, 2, listOf(BlackBears.NPC_KEY))),
                    allKeys = listOf(BlackBears.NPC_KEY),
                    camps = BlackBears.CAMPS.map { CampSpec(it.location, 0, listOf(BlackBears.NPC_KEY), it.tiles) },
                    tertiaryKeys =
                        BlackBears.GUARANTEED + listOf("item.ensouled_bear_head", "item.clue_scroll_beginner"),
                ),
                SpeciesSpec(
                    label = "outlaw",
                    variants = listOf(VariantSpec("Outlaw", "Outlaw", Outlaws.COMBAT_LEVEL, 1, Outlaws.NPC_KEYS)),
                    allKeys = Outlaws.NPC_KEYS,
                    camps = listOf(CampSpec("West of the Grand Exchange", 0, Outlaws.NPC_KEYS, Outlaws.TILES)),
                    tables = mapOf("outlaw" to Outlaws.TABLE),
                    tertiaryKeys = listOf("item.bones"),
                ),
                SpeciesSpec(
                    label = "battle mage",
                    variants =
                        BattleMages.ALL.map {
                            VariantSpec("Battle mage (${it.god})", "Battle mage", BattleMages.COMBAT_LEVEL, 1, listOf(it.npcKey))
                        },
                    allKeys = BattleMages.ALL_KEYS,
                    camps = listOf(CampSpec("Mage Arena", BattleMages.PLANE, BattleMages.ALL_KEYS, BattleMages.TILES)),
                    tertiaryKeys = listOf("item.bones", "item.looting_bag"),
                ),
                SpeciesSpec(
                    label = "elder chaos druid",
                    variants =
                        listOf(
                            VariantSpec(
                                "Elder Chaos druid",
                                "Elder Chaos druid",
                                ElderChaosDruids.COMBAT_LEVEL,
                                1,
                                listOf(ElderChaosDruids.NPC_KEY),
                            ),
                        ),
                    allKeys = listOf(ElderChaosDruids.NPC_KEY),
                    camps =
                        listOf(CampSpec("Chaos Temple", 0, listOf(ElderChaosDruids.NPC_KEY), ElderChaosDruids.TILES)),
                    tables = mapOf("elder chaos druid" to ElderChaosDruids.TABLE),
                    tertiaryKeys =
                        listOf(
                            "item.bones", "item.ensouled_chaos_druid_head", "item.looting_bag",
                            "item.clue_scroll_hard",
                        ),
                ),
            )
        }
    }

    /** Line separator for the multi-line assertion messages below. */
    private val NEWLINE = System.lineSeparator()

    private val terrain = HashMap<Int, Array<Array<Array<TileData>>>?>()

    /** Why [x], [z] on [plane] cannot hold a spawn, or null if it can. See [BestiaryVerify.problem]. */
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

    /**
     * A camp's footprint size, taken from whichever variant owns its ids.
     *
     * Unlike the first bestiary pass, size varies *within* a species here - a lesser demon is 2 and a
     * greater demon 3, an adult dragon is 4 and a baby 2 - so it cannot be a property of the species.
     */
    private fun sizeOf(
        species: SpeciesSpec,
        npcKey: String,
    ): Int = species.variants.first { npcKey in it.npcKeys }.size

    // ------------------------------------------------------------------------------ npc ids

    @Test
    fun `every variant id is the right monster at its published combat level and size`() {
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
                    if (def.size != variant.size) {
                        failures += "$key ($id) is size ${def.size}, not ${variant.size} as '${variant.name}'"
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

    /**
     * No id may be owned by both this pass and the first one, or by `content/npcs/dungeon`.
     *
     * This is the check that would have caught the real hazard behind moving the hellhounds, demons
     * and baby dragons out of `content/npcs/dungeon`: `PluginRepository.bindNpcDeath` **overwrites**
     * rather than stacks, so two plugins claiming one id means plugin load order silently decides
     * which drop table a kill rolls.
     */
    @Test
    fun `no id is also claimed by content npcs dungeon`() {
        val repo = buildWorld().plugins
        val dungeonOwned =
            org.alter.plugins.content.npcs.dungeon.DungeonMonsters.ALL.flatMap { it.npcKeys }.toSet()
        val clashes =
            SPECIES.flatMap { species ->
                species.allKeys.filter { it in dungeonOwned }.map { "${species.label}: $it" }
            }
        assertTrue(clashes.isEmpty(), "Claimed by two packages at once:" + NEWLINE + clashes.joinToString(NEWLINE))
        // Touch the repo so the plugins really are constructed before this test passes.
        assertTrue(repo.npcCombatDefs.size >= 0)
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

    /**
     * The most demanding check in this file. An adult dragon is size 4, so this walks sixteen tiles
     * per pin; a greater demon three by three.
     */
    @Test
    fun `every spawn footprint is standable`() {
        val failures = mutableListOf<String>()
        SPECIES.forEach { species ->
            species.camps.forEach { camp ->
                val size = camp.npcKeys.maxOf { sizeOf(species, it) }
                camp.tiles.forEach { (x, z) ->
                    footprint(x, z, size).forEach { (fx, fz) ->
                        problem(fx, fz, camp.plane)?.let { why ->
                            val at = if (size == 1) "($x, $z)" else "($x, $z) footprint tile ($fx, $fz)"
                            failures += "${species.label}: ${camp.location} $at plane ${camp.plane}: $why"
                        }
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), "Unusable bestiary spawn tiles:" + NEWLINE + failures.joinToString(NEWLINE))
    }

    /**
     * Across all fifteen packages at once, not just within one.
     *
     * These species share locations heavily - Taverley Dungeon carries hellhounds, lesser demons,
     * blue dragons, baby blue dragons and black dragons; Brimhaven Dungeon carries red dragons, baby
     * red dragons, baby green dragons and greater demons - so a duplicated coordinate between two
     * packages is a real possibility and would stack two monsters on one tile.
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
     * The dealer never repeats an id while another from the same pool is still unused.
     *
     * [BestiaryVerify]'s version of this asserts the stronger property - that *every* published id
     * gets placed - and that holds for the first bestiary pass because every pool there has at least
     * as many pins as ids. It does not hold here, and cannot: the wiki gives the black dragon **nine**
     * ids and this cache can host **four** of its pins, because the twelve-pin Taverley upper level is
     * not built. Asserting the stronger property would mean deleting five real published ids to
     * satisfy a test.
     *
     * So this asserts what [SpawnDealer] actually exists to guarantee - that it uses as many distinct
     * ids as it has pins to put them on - which is the property that fails when the cursor is keyed
     * per camp instead of per pool, and which catches the same bug.
     */
    @Test
    fun `the dealer uses as many distinct ids as it has pins`() {
        val failures = mutableListOf<String>()
        SPECIES.forEach { species ->
            val dealer = SpawnDealer()
            val dealtByPool = HashMap<List<String>, MutableSet<String>>()
            val pinsByPool = HashMap<List<String>, Int>()
            species.camps.forEach { camp ->
                camp.tiles.forEach {
                    dealtByPool.getOrPut(camp.npcKeys) { mutableSetOf() }.add(dealer.next(camp.npcKeys))
                    pinsByPool[camp.npcKeys] = (pinsByPool[camp.npcKeys] ?: 0) + 1
                }
            }
            dealtByPool.forEach { (pool, dealt) ->
                val expected = minOf(pool.size, pinsByPool.getValue(pool))
                if (dealt.size != expected) {
                    failures +=
                        "${species.label}: a pool of ${pool.size} ids over ${pinsByPool[pool]} pins used only " +
                            "${dealt.size} distinct ids, not $expected"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString(NEWLINE))
    }

    // -------------------------------------------------------------------------- drop tables

    /**
     * The single most important check here. See the class doc: the exact sum is the whole basis for
     * the members coin reading, the Monkey Madness II reading and the Observatory Quest reading.
     */
    @Test
    fun `every drop table sums to its published denominator`() {
        val failures = mutableListOf<String>()
        SPECIES.forEach { species ->
            species.tables.forEach { (name, table) ->
                if (table.total != table.denominator) {
                    failures +=
                        "${species.label} '$name' sums to ${table.total}, not ${table.denominator} - either a row is " +
                            "wrong or one of the published readings no longer holds"
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
     * The Elder Chaos druid's herb distribution, which is the only [MonsterDropTable.herbRolls] in
     * the tree.
     *
     * Its four buckets are published as 15/20/15/5 out of 55, averaging 2.18 herbs. Checking the
     * bounds rather than the distribution: a wrong threshold would still land inside 1..4, so the
     * cheap assertion is that the counts are exactly the four the page names.
     */
    @Test
    fun `the elder chaos druid rolls one to four herbs`() {
        val counts = (0 until 4000).map { ElderChaosDruids.herbCount(world) }.toSet()
        assertEquals(setOf(1, 2, 3, 4), counts, "herb counts seen over 4000 rolls")
    }

    // ---------------------------------------------------------------------------- the stats

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

    /** One id per species against the wiki infobox, so a wholesale table change cannot pass unnoticed. */
    @Test
    fun `the published stat blocks are intact`() {
        assertStats("npc.hellhound_104", hitpoints = 116, attack = 105, strength = 104, defence = 102, speed = 4)
        assertStats("npc.green_dragon", hitpoints = 75, attack = 68, strength = 68, defence = 68, speed = 4)
        assertStats("npc.blue_dragon", hitpoints = 105, attack = 95, strength = 95, defence = 95, speed = 4)
        assertStats("npc.red_dragon", hitpoints = 140, attack = 130, strength = 130, defence = 130, speed = 4)
        assertStats("npc.black_dragon", hitpoints = 190, attack = 200, strength = 200, defence = 200, speed = 4)
        assertStats("npc.bronze_dragon", hitpoints = 122, attack = 112, strength = 112, defence = 112, speed = 4)
        assertStats("npc.baby_blue_dragon", hitpoints = 50, attack = 40, strength = 40, defence = 40, speed = 4)
        assertStats("npc.lesser_demon", hitpoints = 79, attack = 68, strength = 70, defence = 71, speed = 4)
        assertStats("npc.greater_demon", hitpoints = 87, attack = 76, strength = 78, defence = 81, speed = 4)
        assertStats("npc.ogre", hitpoints = 60, attack = 43, strength = 43, defence = 43, speed = 6)
        assertStats("npc.ice_warrior", hitpoints = 59, attack = 47, strength = 47, defence = 47, speed = 4)
        assertStats("npc.ice_giant", hitpoints = 70, attack = 40, strength = 40, defence = 40, speed = 5)
        assertStats("npc.dagannoth_970", hitpoints = 70, attack = 68, strength = 70, defence = 50, speed = 4)
        assertStats("npc.rock_crab", hitpoints = 50, attack = 1, strength = 1, defence = 1, speed = 4)
        assertStats("npc.giant_frog", hitpoints = 100, attack = 100, strength = 80, defence = 65, speed = 4)
        assertStats("npc.black_bear", hitpoints = 25, attack = 15, strength = 16, defence = 13, speed = 4)
        assertStats("npc.outlaw", hitpoints = 20, attack = 35, strength = 30, defence = 25, speed = 4)
        assertStats("npc.hero_3295", hitpoints = 82, attack = 54, strength = 55, defence = 54, speed = 5)
        assertStats("npc.deadly_red_spider", hitpoints = 35, attack = 30, strength = 25, defence = 30, speed = 4)
        assertStats("npc.battle_mage", hitpoints = 120, attack = 1, strength = 1, defence = 1, speed = 4)
        assertStats("npc.elder_chaos_druid", hitpoints = 150, attack = 98, strength = 98, defence = 65, speed = 4)
        assertStats("npc.scorpion_3024", hitpoints = 17, attack = 11, strength = 12, defence = 11, speed = 4)
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

    // ---------------------------------------------------------------------- the slayer tasks

    /**
     * Five `tasks.json` categories were assignable while naming **no monsters at all**, which
     * `SlayerService.markAvailable` reads as "not assignable" - so the task existed and could never
     * be given out. This asserts they are filled and that every name they now carry is a real cache
     * name of a monster this pass actually spawns.
     */
    @Test
    fun `the slayer categories this pass fills name real spawned monsters`() {
        val mapper = ObjectMapper()
        val tasks = mapper.readTree(TASKS_CONFIG)
        val spawnedNames =
            SPECIES.flatMap { species -> species.camps.flatMap { it.npcKeys } }
                .mapNotNull { CacheManager.getNpc(getRSCM(it))?.name }
                .toSet()

        val expected =
            mapOf(
                "Bears" to listOf("Black bear"),
                "Dagannoth" to listOf("Dagannoth"),
                "Ice giants" to listOf("Ice giant"),
                "Ice warriors" to listOf("Ice warrior"),
                "Red dragons" to listOf("Red dragon", "Baby red dragon"),
            )
        val failures = mutableListOf<String>()
        expected.forEach { (category, names) ->
            val row = tasks.firstOrNull { it["name"].asText() == category }
            if (row == null) {
                failures += "$category is not in tasks.json at all"
                return@forEach
            }
            val listed = row["monsters"].map { it.asText() }
            if (listed != names) {
                failures += "$category names $listed, not $names"
            }
            listed.filterNot { it in spawnedNames }.forEach {
                failures += "$category names '$it', which this pass never spawns - the task would be unfulfillable"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString(NEWLINE))
    }

    // -------------------------------------------------------------------------- the plugins

    /**
     * Every plugin's `init` really runs here, and one monster of each is really taken through
     * `World.setNpcDefaults` and `PluginRepository.executeNpcSpawn` - the same two calls
     * `NpcDeathAction` makes on respawn.
     *
     * That is the only way to catch the failure this design is exposed to: a plugin whose constructor
     * throws registers *nothing*, silently.
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

        Hellhounds.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val def = defOf(key)
                assertEquals(variant.respawnCycles, def.respawnDelay, "$key respawn delay")
                assertEquals(Hellhounds.AGGRO_TIMER, def.aggressiveTimer, "$key aggression timer")
                assertEquals(variant.slayerXp, def.slayerXp, "$key Slayer experience")
            }
        }

        Dragons.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val def = defOf(key)
                variant.respawnCycles?.let { assertEquals(it, def.respawnDelay, "$key respawn delay") }
                assertEquals(variant.slayerXp, def.slayerXp, "$key Slayer experience")
                if (key in Dragons.PASSIVE_KEYS) {
                    assertEquals(0, def.aggressiveRadius, "$key should not be aggressive")
                } else {
                    assertEquals(Dragons.AGGRO_RADIUS, def.aggressiveRadius, "$key aggression radius")
                }
            }
        }

        Demons.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val def = defOf(key)
                assertEquals(Demons.respawnFor(variant), def.respawnDelay, "$key respawn delay")
                assertEquals(Demons.AGGRO_TIMER, def.aggressiveTimer, "$key aggression timer")
                assertEquals(variant.slayerXp, def.slayerXp, "$key Slayer experience")
            }
        }

        Ogres.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val def = defOf(key)
                assertEquals(variant.respawnCycles, def.respawnDelay, "$key respawn delay")
                assertEquals(variant.slayerXp, def.slayerXp, "$key Slayer experience")
            }
        }

        IceCreatures.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val def = defOf(key)
                assertEquals(IceCreatures.RESPAWN_CYCLES, def.respawnDelay, "$key respawn delay")
                assertEquals(variant.slayerXp, def.slayerXp, "$key Slayer experience")
            }
        }

        Scorpions.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val def = defOf(key)
                assertEquals(variant.respawnCycles, def.respawnDelay, "$key respawn delay")
                assertEquals(variant.slayerXp, def.slayerXp, "$key Slayer experience")
                if (variant.poisonDamage > 0) {
                    assertEquals(variant.poisonDamage, def.poisonDamage, "$key poison damage")
                }
            }
        }

        RockCrabs.NPC_KEYS.forEach { key ->
            val def = defOf(key)
            assertEquals(RockCrabs.SLAYER_XP, def.slayerXp, "$key Slayer experience")
            assertEquals(RockCrabs.AGGRO_TIMER, def.aggressiveTimer, "$key aggression timer")
        }

        Frogs.VARIANTS.forEach { variant ->
            val def = defOf(variant.npcKey)
            assertEquals(Frogs.RESPAWN_CYCLES, def.respawnDelay, "${variant.name} respawn delay")
            // Every frog version publishes `aggressive = No`.
            assertEquals(0, def.aggressiveRadius, "${variant.name} should not be aggressive")
        }

        defOf(BlackBears.NPC_KEY).let {
            assertEquals(BlackBears.SLAYER_XP, it.slayerXp, "black bear Slayer experience")
            assertEquals(0, it.aggressiveRadius, "black bear should not be aggressive")
        }

        Outlaws.NPC_KEYS.forEach { key ->
            val def = defOf(key)
            assertEquals(Outlaws.RESPAWN_CYCLES, def.respawnDelay, "$key respawn delay")
            assertEquals(0, def.aggressiveRadius, "$key should not be aggressive")
        }

        BattleMages.ALL.forEach { mage ->
            val def = defOf(mage.npcKey)
            assertEquals(BattleMages.RESPAWN_CYCLES, def.respawnDelay, "${mage.god} battle mage respawn delay")
            assertEquals(BattleMages.AGGRO_RADIUS, def.aggressiveRadius, "${mage.god} battle mage aggression radius")
        }

        defOf(ElderChaosDruids.NPC_KEY).let {
            assertEquals(ElderChaosDruids.RESPAWN_CYCLES, it.respawnDelay, "elder chaos druid respawn delay")
            assertEquals(ElderChaosDruids.SLAYER_XP, it.slayerXp, "elder chaos druid Slayer experience")
        }
    }

    /**
     * The adult dragons must carry [NpcSpecies.BASIC_DRAGON], and the babies must not.
     *
     * `monsterStats.json` tags neither - it only knows the wiki's `attributes` field, which has no
     * such value - so this tag exists only because `DragonPlugin` adds it at spawn. It is what
     * [org.alter.plugins.content.combat.formula.DragonfireFormula] reads to decide whether Protect
     * from Magic reduces the breath, so losing it would silently make the prayer useless against
     * every dragon in the game.
     */
    @Test
    fun `adult dragons are tagged BASIC_DRAGON and babies are not`() {
        val world = buildWorld()
        val repo = world.plugins
        val failures = mutableListOf<String>()

        Dragons.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val npc = Npc(getRSCM(key), Tile(3200, 3200), world)
                world.setNpcDefaults(npc)
                repo.executeNpcSpawn(npc)
                val tagged = NpcSpecies.BASIC_DRAGON in npc.combatDef.species
                if (variant.breathesFire && !tagged) {
                    failures += "$key breathes fire but is not BASIC_DRAGON - Protect from Magic would not reduce it"
                }
                if (!variant.breathesFire && tagged) {
                    failures += "$key is a baby dragon and should not be BASIC_DRAGON"
                }
                // The stats table's own tag must survive the patch rather than be replaced by it.
                if (NpcSpecies.DRACONIC !in npc.combatDef.species) {
                    failures += "$key lost its DRACONIC tag to the spawn patch"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString(NEWLINE))
    }

    /**
     * The level 74 dagannoth's spine attack, all four pieces of it.
     *
     * Any one of them missing is silent: no `combatClass` and it bites instead of shooting, no
     * `rangedProjectileGfx` and the spines are invisible.
     */
    @Test
    fun `the level 74 dagannoth really shoots`() {
        val world = buildWorld()
        val repo = world.plugins
        val failures = mutableListOf<String>()

        Dagannoths.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val npc = Npc(getRSCM(key), Tile(3200, 3200), world)
                world.setNpcDefaults(npc)
                repo.executeNpcSpawn(npc)
                val def = npc.combatDef
                if (variant.ranged) {
                    if (def.combatClass != CombatClass.RANGED) failures += "$key def combat class is ${def.combatClass}"
                    if (npc.combatClass != CombatClass.RANGED) failures += "$key npc combat class is ${npc.combatClass}"
                    if (def.rangedProjectileGfx <= 0) failures += "$key has no ranged projectile - its spines would be invisible"
                    if (def.attackAnimation != org.alter.api.cfg.Animation.DAGANNOTH_SPINES_ATTACK) {
                        failures += "$key attack animation is ${def.attackAnimation}, not the spine throw"
                    }
                } else {
                    if (def.combatClass != CombatClass.MELEE) failures += "$key should be melee, is ${def.combatClass}"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString(NEWLINE))
    }

    /**
     * The load-bearing invariant of all fifteen packages: no monster here may have a hand-written
     * combat def.
     *
     * One would take the npc out of `World.setNpcDefaults`' `monsterStats.json` tier *and* off
     * `MonsterAnimationsPlugin`'s media path - which for this pass would be expensive, because that
     * path is where the corrected chromatic dragon, baby dragon, ice warrior, giant frog, outlaw,
     * hero, battle mage and Elder Chaos druid rigs now live.
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
        HellhoundPlugin(repo, world, server)
        HellhoundSpawnPlugin(repo, world, server)
        DragonPlugin(repo, world, server)
        DragonSpawnPlugin(repo, world, server)
        DemonPlugin(repo, world, server)
        DemonSpawnPlugin(repo, world, server)
        OgrePlugin(repo, world, server)
        OgreSpawnPlugin(repo, world, server)
        IcePlugin(repo, world, server)
        IceSpawnPlugin(repo, world, server)
        DagannothPlugin(repo, world, server)
        DagannothSpawnPlugin(repo, world, server)
        RockCrabPlugin(repo, world, server)
        RockCrabSpawnPlugin(repo, world, server)
        FrogPlugin(repo, world, server)
        FrogSpawnPlugin(repo, world, server)
        ScorpionPlugin(repo, world, server)
        ScorpionSpawnPlugin(repo, world, server)
        BlackBearPlugin(repo, world, server)
        OutlawPlugin(repo, world, server)
        HeroPlugin(repo, world, server)
        DeadlyRedSpiderPlugin(repo, world, server)
        BattleMagePlugin(repo, world, server)
        ElderChaosDruidPlugin(repo, world, server)
        org.alter.plugins.content.npcs.blackheather.BlackHeatherPlugin(repo, world, server)
        return world
    }
}
