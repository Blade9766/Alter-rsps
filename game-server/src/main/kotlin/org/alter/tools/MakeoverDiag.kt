package org.alter.tools

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.appearance.Appearance
import org.alter.game.model.appearance.Gender
import org.alter.game.plugin.PluginRepository
import java.nio.file.Paths

/**
 * TEMPORARY diagnostic - boots a world and checks the Makeover Mage content actually registered.
 *
 * A plugin whose constructor throws is only reported by one printed "Failed to load" line and
 * otherwise registers nothing, so "it compiles" says very little about whether the npc exists or
 * whether its buttons are bound. This walks the plugin repository after startup and says so
 * either way.
 *
 * Usage: gradlew :game-server:makeoverDiag
 */
object MakeoverDiag {

    private const val MAGE_ID = 1306
    private const val MAKEOVER_INTERFACE = 679

    /** Every component of interface 679 the server is meant to be listening to. */
    private val EXPECTED_BUTTONS =
        listOf(
            15, 16, 19, 20, 23, 24, 27, 28, 31, 32, 35, 36, 39, 40, // design steppers
            46, 47, 50, 51, 54, 55, 58, 59, 62, 63, // colour steppers
            68, 69, // body type A / B
            78, // the open pronoun list; the closed control at 72 is client side only
            74, // confirm
        )

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

        println()
        println("=== Makeover Mage ===")

        val mage = world.npcs.entries.filterNotNull().firstOrNull { it.id == MAGE_ID }
        check("npc $MAGE_ID spawned", mage != null)
        check("stands on (2918, 3322, 0), got ${mage?.tile}", mage?.tile == Tile(2918, 3322, 0))

        val repo = world.plugins
        val npcOptions = repo.privateField<Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<Any>>>("npcPlugins")[MAGE_ID]
        // Op numbers come from where each action sits in the npc def, not from the order the
        // client shows them: 1306 carries "Talk-to" in slot 0 and "Makeover" in slot 3.
        val actions = dev.openrune.cache.CacheManager.getNpc(MAGE_ID).actions
        listOf("Talk-to", "Makeover").forEach { action ->
            val op = actions.indexOfFirst { it?.lowercase() == action.lowercase() } + 1
            check("$action bound (option $op)", op > 0 && npcOptions?.containsKey(op) == true)
        }

        println()
        println("=== Makeover window (interface $MAKEOVER_INTERFACE) ===")
        val buttons = repo.privateField<Int2ObjectOpenHashMap<Any>>("buttonPlugins")
        val missing = EXPECTED_BUTTONS.filterNot { buttons.containsKey((MAKEOVER_INTERFACE shl 16) or it) }
        check("all ${EXPECTED_BUTTONS.size} components bound, missing=$missing", missing.isEmpty())

        println()
        println("=== Pronouns ===")
        // The dropdown does not read varp 387 - struct 994's param 1077 is a setting id, which
        // [clientscript,script3962] maps to this varbit. Getting that wrong is invisible: the list
        // opens, closes and relabels itself locally, and only the value never moves.
        val pronounVarbit = dev.openrune.cache.CacheManager.getVarbit(10988)
        val bits = pronounVarbit.endBit - pronounVarbit.startBit + 1
        val labels = dev.openrune.cache.CacheManager.getEnum(5500)
        val pronounClass = Class.forName("org.alter.plugins.content.mechanics.appearance.Pronoun")
        val ours = pronounClass.enumConstants.map { pronounClass.getMethod("getLabel").invoke(it) as String }

        check("varbit 10988 sits on varp ${pronounVarbit.varp}, the dropdown's redraw trigger", pronounVarbit.varp == 2855)
        check("it holds $bits bits, enough for ${ours.size} options", (1 shl bits) - 1 >= ours.size - 1)
        val cacheLabels = labels.values.keys.sorted().map { labels.getString(it) }
        check("enum 5500 labels $cacheLabels match ours $ours", cacheLabels == ours)

        println()
        println("=== Look slot mapping ===")
        // The one piece of the window that is easy to get quietly wrong: the two body types keep
        // their identikits in differently ordered arrays, so Makeover's idea of where a part lives
        // has to agree with Appearance.getLook's, and a body type switch has to land every part
        // inside its new table.
        val makeover = Class.forName("org.alter.plugins.content.mechanics.appearance.Makeover")
        val instance = makeover.getField("INSTANCE").get(null)
        val lookIndex = makeover.getMethod("lookIndex", Gender::class.java, Int::class.javaPrimitiveType)
        val looksFor = makeover.getMethod("looksFor", Gender::class.java, Int::class.javaPrimitiveType)
        val asBodyType = makeover.getMethod("asBodyType", Appearance::class.java, Gender::class.java)

        fun audit(
            label: String,
            appearance: Appearance,
        ) {
            val problems =
                (0..6).mapNotNull { part ->
                    val index = lookIndex.invoke(instance, appearance.gender, part) as Int
                    @Suppress("UNCHECKED_CAST")
                    val table = looksFor.invoke(instance, appearance.gender, part) as Array<Int>
                    val value = appearance.looks.getOrNull(index)
                    when {
                        table.isEmpty() -> "part $part has no identikits"
                        value == null || value !in table.indices -> "part $part index $value out of 0..${table.size - 1}"
                        appearance.getLook(part) != table[value] -> "part $part disagrees with Appearance.getLook"
                        else -> null
                    }
                }
            check("$label: $problems", problems.isEmpty())
        }

        audit("default male", Appearance.DEFAULT_MALE)
        audit("default female", Appearance.DEFAULT_FEMALE)
        audit("male -> B", asBodyType.invoke(instance, Appearance.DEFAULT_MALE, Gender.FEMALE) as Appearance)
        audit("female -> A", asBodyType.invoke(instance, Appearance.DEFAULT_FEMALE, Gender.MALE) as Appearance)
        // The widest look array on either body type, pushed the other way, is what would overflow.
        val maxedMale = Appearance.DEFAULT_MALE.also { a -> (0..6).forEach { part ->
            @Suppress("UNCHECKED_CAST")
            val table = looksFor.invoke(instance, Gender.MALE, part) as Array<Int>
            a.looks[lookIndex.invoke(instance, Gender.MALE, part) as Int] = table.size - 1
        } }
        audit("male at the end of every table", maxedMale)
        audit("that, switched to B", asBodyType.invoke(instance, maxedMale, Gender.FEMALE) as Appearance)

        println()
        println(if (failures == 0) "All checks passed." else "$failures check(s) failed.")
        Runtime.getRuntime().halt(if (failures == 0) 0 else 1)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> PluginRepository.privateField(name: String): T =
        PluginRepository::class.java.getDeclaredField(name).also { it.isAccessible = true }.get(this) as T
}
