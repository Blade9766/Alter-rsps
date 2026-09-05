package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.alter.game.DevContext
import org.alter.game.GameContext
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.plugin.PluginRepository
import org.alter.game.saving.formats.SaveFormatType
import org.alter.plugins.content.items.consumables.food.EatingPlugin
import org.alter.plugins.content.items.consumables.food.Food
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Builds `EatingPlugin` for real and checks what every entry in [Food] actually bound.
 *
 * Worth a test rather than a read-through because both ways this can fail are silent.
 * `onItemOption(item, option: String)` **throws** when the named option is not on the
 * item, and `PluginRepository.bindItem` **throws** on a repeated item/option pair - and a
 * plugin whose constructor throws is skipped whole, so one bad entry takes eating away
 * from every food in the game rather than from the food that caused it. The option index
 * is checked too: the client's op number for an inventory item is its position in
 * `interfaceOptions` plus one, so binding the wrong index leaves a plugin that loads
 * cleanly and then never fires.
 */
class EatingVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** Nothing here is read by the plugin; both only exist to build a [World]. */
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

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `every food binds its own eat option`() {
        // The plugin's init only binds through the repository, so a bare world and server
        // are enough and this stays off a full server boot.
        val world = World(GAME_CONTEXT, DEV_CONTEXT)
        val repo = PluginRepository(world)
        EatingPlugin(repo, world, Server())

        val field = PluginRepository::class.java.getDeclaredField("itemPlugins")
        field.isAccessible = true
        val bindings = field.get(repo) as Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<Any>>

        Food.values.forEach { food ->
            val id = getRSCM(food.item)
            val def = CacheManager.getItem(id)
            val opts = assertNotNull(bindings[id], "${food.item} (${def.name}) bound nothing")
            /*
             * The inventory's item options start at op2, not op1 - see [InventoryOptionOpVerify].
             * This read `+ 1` while `onItemOption` did too, so the pair agreed with each other
             * and not with the client, and every food was bound to an op no click ever sends.
             */
            val index = def.interfaceOptions.indexOfFirst { it?.lowercase() == food.option.lowercase() }
            val expected = InventoryOptionOpVerify.inventoryOpOf(index)
            assertEquals(
                setOf(expected),
                opts.keys.toSet(),
                "${food.item} (${def.name}) bound the wrong option; " +
                    "interfaceOptions=${def.interfaceOptions.filterNotNull().filter { it.isNotBlank() }}",
            )
        }
    }
}
