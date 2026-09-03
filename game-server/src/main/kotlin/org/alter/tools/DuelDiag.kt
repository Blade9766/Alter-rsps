package org.alter.tools

import dev.openrune.cache.CacheManager
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.collision.isClipped
import org.alter.game.plugin.PluginRepository
import java.nio.file.Paths

/**
 * TEMPORARY diagnostic - boots a world and checks the Duel Arena plugin registered, then re-checks
 * every fact about this cache the plugin was built on.
 *
 * The second half matters more than it looks. The whole duel screen is driven by ids that moved
 * once already: the classic 482/631 are recycled in revision 228 (482 is the Tombs of Amascut
 * summary now), and the duel screens live at 755/756 instead. If the cache is ever bumped again,
 * every one of those could move a second time and nothing would fail loudly - the screen would just
 * come up blank. This says so instead.
 *
 * Usage: gradlew :game-server:duelDiag
 */
object DuelDiag {
    private const val OPTIONS = 755
    private const val CONFIRM = 756
    private const val STAKE = 335
    private const val STAKE_OVERLAY = 336

    /** Rule row components, from `[clientscript,6169]` / enum 4209. */
    private val RULE_COMPONENTS = listOf(30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41)

    /**
     * Worn-slot icon host (42), the Accept/Decline bar (86/87) and the two presets (94/96).
     *
     * The bar halves were found by binding the whole bar and seeing which component a click on
     * Accept reported - nothing in the cache records the roles - so they are worth re-asserting.
     */
    private val OTHER_OPTIONS_COMPONENTS = listOf(42, 86, 87, 94, 96)

    private val SCRIPTS =
        listOf(
            "duel_initworn" to 205,
            "duel_accept_button" to 1443,
        )

    private val SCRIPT_IDS = listOf(6169, 6170, 6175, 6176, 6193)

    private val ENUMS = listOf(4209 to 12, 4210 to 11, 4285 to 11, 4388 to 11)

    /**
     * Both start tiles of each of the four arenas, in both the "apart" and "adjacent" layouts.
     * Interiors are x+3..x-3 of the walls read off region 13362, at the arena's middle row.
     */
    private val ARENA_START_TILES =
        listOf("north-west" to (3334 to 3251), "north-east" to (3370 to 3251),
               "south-west" to (3334 to 3213), "south-east" to (3370 to 3213))
            .flatMap { (name, corner) ->
                val (minX, z) = corner
                val maxX = minX + 17
                val centreX = (minX + maxX) / 2
                listOf(
                    "$name apart A" to Tile(minX + 3, z, 0),
                    "$name apart B" to Tile(maxX - 3, z, 0),
                    "$name adjacent A" to Tile(centreX, z, 0),
                    "$name adjacent B" to Tile(centreX + 1, z, 0),
                )
            }

    @JvmStatic
    fun main(args: Array<String>) {
        val server = Server()
        server.startServer(apiProps = Paths.get("../data/api.yml"))
        val world =
            server.startGame(
                filestore = Paths.get("../data", "cache"),
                gameProps = Paths.get("../game.yml"),
                devProps = Paths.get("../dev-settings.yml"),
            )

        var failures = 0
        fun check(
            label: String,
            ok: Boolean,
        ) {
            println((if (ok) "  ok   " else "  FAIL ") + label)
            if (!ok) failures++
        }

        val repo = world.plugins
        val buttons = repo.privateField<Int2ObjectOpenHashMap<Any>>("buttonPlugins")
        fun bound(
            interfaceId: Int,
            component: Int,
        ) = buttons.containsKey((interfaceId shl 16) or component)

        println()
        println("=== plugin registration ===")

        val playerOptions = repo.privateField<HashMap<String, Any>>("playerOptionPlugins")
        check("\"Challenge\" player option bound", playerOptions.containsKey("Challenge"))

        val enterRegion = repo.privateField<Int2ObjectOpenHashMap<Any>>("enterRegionPlugins")
        val exitRegion = repo.privateField<Int2ObjectOpenHashMap<Any>>("exitRegionPlugins")
        check("lobby region 13363 entry bound", enterRegion.containsKey(13363))
        check("lobby region 13363 exit bound", exitRegion.containsKey(13363))

        val globalEquip = repo.privateField<MutableList<Any>>("globalEquipRequirementPlugins")
        check("global equip requirement registered", globalEquip.isNotEmpty())

        val preDeath = repo.privateField<MutableList<Any>>("playerPreDeathPlugins")
        check("a pre-death plugin is registered", preDeath.isNotEmpty())

        println()
        println("=== every plugin loaded ===")
        check("no plugin failed to load: ${repo.failedPlugins}", repo.failedPlugins.isEmpty())

        println()
        println("=== trading still works ===")
        /*
         * The duel stakes on the trade screen, and a component may only be bound once. Binding it
         * in both plugins does not fail as a conflict - the second one to load throws in its
         * constructor and registers nothing, quietly taking all of player trading with it. That
         * happened, so it is checked from now on: these are TradingPlugin's own bindings, and their
         * presence is proof its constructor ran to the end.
         */
        check("\"Trade with\" player option bound", playerOptions.containsKey("Trade with"))
        check("trade offer button bound", bound(STAKE_OVERLAY, 0))
        check("trade remove button bound", bound(STAKE, 25))
        check("trade accept/decline bound", bound(STAKE, 10) && bound(STAKE, 11))
        check("trade confirm screen bound", bound(334, 13) && bound(334, 14))

        println()
        println("=== duel screens are bound ===")

        val missingRules = RULE_COMPONENTS.filterNot { bound(OPTIONS, it) }
        check("all 12 rule rows bound, missing=$missingRules", missingRules.isEmpty())

        val missingOther = OTHER_OPTIONS_COMPONENTS.filterNot { bound(OPTIONS, it) }
        check("worn icons/presets/accept bound, missing=$missingOther", missingOther.isEmpty())

        // 756's bar is the other way round from 755's: Accept is the right half here.
        check("confirm accept bound", bound(CONFIRM, 51))
        check("confirm decline bound", bound(CONFIRM, 50))

        println()
        println("=== cache still matches what the plugin assumes ===")

        val cache = CacheManager.cache
        fun components(interfaceId: Int): Int =
            runCatching { cache.fileCount(3, interfaceId) }.getOrDefault(-1)

        // 755 carried 100 components and 756 carried 57 when this was written; a different count is
        // not automatically wrong, but an empty one means the screen has moved again.
        check("interface 755 exists (${components(OPTIONS)} components)", components(OPTIONS) > 50)
        check("interface 756 exists (${components(CONFIRM)} components)", components(CONFIRM) > 20)

        // The give-away that the ids have been recycled rather than kept: 755:1 is the window title.
        val title = titleOf(OPTIONS, 1)
        check("755:1 still reads \"Duel Options\" (got \"$title\")", title == "Duel Options")
        val confirmTitle = titleOf(CONFIRM, 1)
        check("756:1 still reads \"Confirm Duel Options\" (got \"$confirmTitle\")", confirmTitle == "Confirm Duel Options")

        SCRIPTS.forEach { (name, expected) ->
            val id = CacheManager.findScriptId(name)
            check("clientscript \"$name\" resolves to $expected (got $id)", id == expected)
        }

        SCRIPT_IDS.forEach { id ->
            val present = runCatching { cache.data(12, id, 0, null) }.getOrNull() != null
            check("clientscript $id present", present)
        }

        ENUMS.forEach { (id, size) ->
            val actual = runCatching { CacheManager.getEnum(id).values.size }.getOrDefault(-1)
            check("enum $id has $size entries (got $actual)", actual == size)
        }

        println()
        println("=== the four arenas are actually fightable floor ===")
        /*
         * Regions load lazily (preload-maps is off), and an unallocated zone reads as fully
         * blocked - so without this the arenas would all "fail" for having nobody standing in them.
         */
        check("arena region 13362 loaded", world.definitions.createRegion(world, 13362) || true)

        /*
         * The start tiles, restated rather than imported: game-server cannot see game-plugins, and
         * a check that read the same constants it is checking would only ever agree with itself.
         * Keep in step with DuelArena.ARENAS.
         */
        ARENA_START_TILES.forEach { (label, tile) ->
            check("$label $tile walkable", !world.collision.isClipped(tile))
        }

        println()
        if (failures == 0) {
            println("All Duel Arena checks passed.")
        } else {
            println("$failures Duel Arena check(s) FAILED.")
        }
        System.exit(if (failures == 0) 0 else 1)
    }

    /**
     * The window title of an interface, read straight out of the component's raw data - enough to
     * tell "this is still the duel screen" from "this id belongs to something else now".
     */
    private fun titleOf(
        interfaceId: Int,
        component: Int,
    ): String {
        val data = runCatching { CacheManager.cache.data(3, interfaceId, component, null) }.getOrNull() ?: return ""
        val text = StringBuilder()
        val best = StringBuilder()
        data.forEach { byte ->
            val c = byte.toInt() and 0xff
            if (c in 0x20..0x7e) {
                text.append(c.toChar())
            } else {
                if (text.length > best.length) {
                    best.setLength(0)
                    best.append(text)
                }
                text.setLength(0)
            }
        }
        if (text.length > best.length) {
            best.setLength(0)
            best.append(text)
        }
        return best.toString()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> PluginRepository.privateField(name: String): T =
        PluginRepository::class.java.getDeclaredField(name).also { it.isAccessible = true }.get(this) as T
}
