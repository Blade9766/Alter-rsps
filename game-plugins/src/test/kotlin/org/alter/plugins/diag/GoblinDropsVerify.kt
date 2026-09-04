package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.game.DevContext
import org.alter.game.GameContext
import org.alter.game.Server
import org.alter.game.model.EntityType
import org.alter.game.model.PlayerUID
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.saving.formats.SaveFormatType
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.plugins.content.npcs.DropRoll
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.plugins.content.npcs.goblin.GoblinDrops
import org.alter.plugins.content.npcs.goblin.GoblinPlugin
import org.alter.plugins.content.npcs.goblin.Goblins
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.lang.ref.WeakReference
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end checks that killing a goblin actually drops something, plus the drop-table audit
 * [DungeonDropsVerify] already applies to `content/npcs/dungeon`.
 *
 * Goblins are worth their own file because they are the package that proved a
 * `ClassNotFoundException` on `GoblinDrops` takes the whole of [GoblinPlugin] down with it -
 * `GoblinVariant.dropTable` is a `GoblinDrops.Table`, so building `Goblins.VARIANTS` needs the
 * class, and without it not one goblin got a combat def, a death hook, or bones. Nothing in the
 * suite noticed. `every goblin id drops bones when it dies` is the check that would have.
 *
 * The kills here run the real path: [GoblinPlugin] registers through a live [World]'s
 * [org.alter.game.plugin.PluginRepository], the npc is killed with
 * [org.alter.game.plugin.PluginRepository.executeNpcDeath], and the drops are read back out of
 * the world's chunks as [GroundItem]s. No stubbing of the table or the roll.
 */
class GoblinDropsVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** Both goblin tables are published out of 128; the KDoc on each says so explicitly. */
        const val PUBLISHED_DENOMINATOR = 128

        /**
         * Items that are tertiary rolls wherever they appear, borrowed from [DungeonDropsVerify].
         * One of these as a weighted row means its rate is whatever the table happens to total.
         */
        val TERTIARY_MARKERS =
            listOf("champion_scroll", "ensouled_", "clue_scroll", "looting_bag")

        /**
         * Nothing here is read by the plugin; both only exist to build a [World]. Declared locally
         * rather than borrowed from a sibling verify so this file compiles on its own.
         */
        val GAME_CONTEXT =
            GameContext(
                initialLaunch = false,
                name = "test",
                revision = 228,
                saveFormat = SaveFormatType.JSON,
                cycleTime = 600,
                playerLimit = 1,
                home = Tile(3222, 3222),
                skillCount = 23,
                npcStatCount = 6,
                runEnergy = true,
                gItemPublicDelay = 100,
                gItemDespawnDelay = 300,
                preloadMaps = false,
            )

        val DEV_CONTEXT =
            DevContext(
                debugExamines = false,
                debugObjects = false,
                debugButtons = false,
                debugItemActions = false,
                debugMagicSpells = false,
                debugPackets = false,
            )
    }

    private fun buildWorld(): World {
        val world = World(GAME_CONTEXT, DEV_CONTEXT)
        GoblinPlugin(world.plugins, world, Server())
        return world
    }

    /**
     * A killer the drop code can own items to. A [Player] built outside a login never gets its
     * `uid` assigned, and [GroundItem]'s owner constructor reads it, so it is set here.
     */
    private fun testPlayer(world: World): Player = Player(world).apply { uid = PlayerUID("goblin-test") }

    /** Kills one goblin of [npcKey] on its own tile and returns what it left on the floor. */
    private fun killOn(
        world: World,
        npcKey: String,
        tile: Tile,
    ): List<GroundItem> {
        val killer = testPlayer(world)
        val npc = Npc(getRSCM(npcKey), tile, world)
        world.setNpcDefaults(npc)
        npc.attr[KILLER_ATTR] = WeakReference(killer)
        world.plugins.executeNpcDeath(npc)
        return world.chunks.getOrCreate(tile).getEntities(tile, EntityType.GROUND_ITEM)
    }

    /**
     * The regression test for the failure this package is named after. A goblin that registered
     * no death hook drops *nothing at all* - not even the guaranteed bones - and reads in game as
     * a content gap rather than a load failure.
     */
    @Test
    fun `every goblin id drops bones when it dies`() {
        val world = buildWorld()
        val bones = getRSCM("item.bones")

        val failures =
            Goblins.ALL_IDS.mapIndexedNotNull { i, npcKey ->
                val drops = killOn(world, npcKey, Tile(3200 + (i % 40), 3300 + (i / 40)))
                if (drops.any { it.item == bones }) null else "$npcKey dropped nothing on death"
            }
        assertTrue(failures.isEmpty(), "Goblins with no working death hook:\n" + failures.joinToString("\n"))
    }

    /** A drop nobody owns is a drop the killer cannot pick up. */
    @Test
    fun `drops are owned by the killer and land on the goblin's tile`() {
        val world = buildWorld()
        val tile = Tile(3250, 3250)
        val killer = testPlayer(world)
        val npc = Npc(getRSCM(Goblins.ALL_IDS.first()), tile, world)
        world.setNpcDefaults(npc)
        npc.attr[KILLER_ATTR] = WeakReference(killer)
        world.plugins.executeNpcDeath(npc)

        val drops = world.chunks.getOrCreate(tile).getEntities<GroundItem>(tile, EntityType.GROUND_ITEM)
        assertTrue(drops.isNotEmpty(), "no drops at all")
        drops.forEach { drop ->
            assertEquals(tile, drop.tile, "drop ${drop.item} landed away from the goblin")
            assertTrue(drop.isOwnedBy(killer), "drop ${drop.item} is not owned by the killer")
        }
    }

    /**
     * [DropRoll.pick] weights each row against the **table total**, so a table that does not add
     * up to its published denominator silently rescales every row in it. Table 2 totals 128; if
     * table 1 does not, its "Nothing" row and every item on it are off by that ratio.
     */
    @Test
    fun `each table adds up to its published denominator`() {
        val failures =
            listOf("TABLE_ONE" to GoblinDrops.TABLE_ONE, "TABLE_TWO" to GoblinDrops.TABLE_TWO)
                .mapNotNull { (name, table) ->
                    val total = table.sumOf { it.weight }
                    if (total == PUBLISHED_DENOMINATOR) {
                        null
                    } else {
                        "$name totals $total, not $PUBLISHED_DENOMINATOR - every row on it is off by " +
                            "%.3fx".format(PUBLISHED_DENOMINATOR.toDouble() / total)
                    }
                }
        assertTrue(failures.isEmpty(), "Goblin tables that do not add up:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `every weighted row resolves to a cache item`() {
        val failures = mutableListOf<String>()
        mapOf("TABLE_ONE" to GoblinDrops.TABLE_ONE, "TABLE_TWO" to GoblinDrops.TABLE_TWO)
            .forEach { (name, table) ->
                table.mapNotNull(WeightedDrop::item).forEach { id ->
                    if (id <= 0 || CacheManager.getItem(id) == null) {
                        failures += "$name: item id $id is not in this cache"
                    }
                }
            }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    /** Coins must be the stackable 995, not the non-stackable 617 the KDoc warns about. */
    @Test
    fun `coin rows use the stackable currency item`() {
        val coins = getRSCM("item.coins_995")
        assertTrue(CacheManager.getItem(coins)!!.stackable, "item.coins_995 is not stackable in this cache")

        val failures =
            mapOf("TABLE_ONE" to GoblinDrops.TABLE_ONE, "TABLE_TWO" to GoblinDrops.TABLE_TWO)
                .flatMap { (name, table) ->
                    table.mapNotNull(WeightedDrop::item)
                        .filter { CacheManager.getItem(it)?.name?.lowercase() == "coins" && it != coins }
                        .map { "$name: coin row uses $it, not the stackable $coins" }
                }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `no tertiary is folded into a weighted table`() {
        val failures = mutableListOf<String>()
        mapOf("TABLE_ONE" to GoblinDrops.TABLE_ONE, "TABLE_TWO" to GoblinDrops.TABLE_TWO)
            .forEach { (name, table) ->
                table.mapNotNull(WeightedDrop::item).forEach { id ->
                    val itemName = CacheManager.getItem(id)?.name?.lowercase()?.replace(' ', '_') ?: return@forEach
                    TERTIARY_MARKERS.forEach { marker ->
                        if (itemName.contains(marker.trim('_'))) {
                            failures += "$name: '$itemName' ($id) is a weighted row but should be a tertiary"
                        }
                    }
                }
            }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `every tertiary item resolves to a cache item`() {
        listOf(
            "item.ensouled_goblin_head",
            "item.clue_scroll_beginner",
            "item.clue_scroll_easy",
            "item.goblin_champion_scroll",
        ).forEach { key ->
            val id = getRSCM(key)
            assertTrue(id > 0, "$key does not resolve")
            assertNotNull(CacheManager.getItem(id), "$key ($id) is not in this cache")
        }
    }

    /**
     * Measures what [DropRoll.pick] really hands out, rather than trusting the weights. The
     * "Nothing" row is the one to watch: it is the largest single row on table 1, so a table that
     * does not total 128 shows up here first.
     */
    @Test
    fun `measured table rates match the published weights`() {
        val world = buildWorld()
        val rolls = 400_000

        listOf("TABLE_ONE" to GoblinDrops.TABLE_ONE, "TABLE_TWO" to GoblinDrops.TABLE_TWO)
            .forEach { (name, table) ->
                val counts = HashMap<Int?, Int>()
                repeat(rolls) {
                    val picked = DropRoll.pick(table, world)
                    counts[picked?.item] = (counts[picked?.item] ?: 0) + 1
                }
                assertEquals(rolls, counts.values.sum(), "$name: pick() returned null outside the Nothing row")

                val total = table.sumOf { it.weight }
                val nothingWeight = table.filter { it.item == null }.sumOf { it.weight }
                val measured = (counts[null] ?: 0).toDouble() / rolls
                val expected = nothingWeight.toDouble() / total
                assertTrue(
                    kotlin.math.abs(measured - expected) < 0.01,
                    "$name: Nothing measured at %.4f but the table implies %.4f".format(measured, expected),
                )
            }
    }

    /** Sanity: the fix that added this package did not lose a variant or an id. */
    @Test
    fun `the goblin roster is intact`() {
        assertTrue(Goblins.VARIANTS.isNotEmpty(), "no goblin variants defined")
        Goblins.ALL_IDS.forEach { key ->
            assertNotNull(CacheManager.getNpc(getRSCM(key)), "$key is not in this cache")
        }
        assertEquals(
            Goblins.ALL_IDS.size,
            Goblins.ALL_IDS.distinct().size,
            "the same goblin id appears in two variants, so one table silently wins",
        )
    }
}
