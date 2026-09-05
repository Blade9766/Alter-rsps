package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.game.Server
import org.alter.game.message.handler.OpPlayerHandler
import org.alter.game.model.World
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.mechanics.follow.Follow
import org.alter.plugins.content.mechanics.follow.FollowPlugin
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the player "Follow" option end to end, because every link in it fails quietly.
 *
 * Follow was sent to the client on every login and had no handler bound to it for the life of the
 * server. Nothing reported that: [PluginRepository.executePlayerOption] returns false for an
 * option nobody bound, and the two callers answer a false by writing "Nothing interesting
 * happens." - the same thing they write for a genuinely inert interaction. The option appeared in
 * the right-click menu, the click reached the server, the server logged it, and the player was
 * told nothing was there.
 *
 * The three facts below are the ones that have to agree, and none of them is checked by anything
 * at runtime:
 *
 *  - the name [org.alter.plugins.content.OSRSPlugin] sends to the client,
 *  - the name [FollowPlugin] binds a handler under,
 *  - and the name `OpPlayerHandler` dispatches directly instead of handing to
 *    [org.alter.game.model.move.PawnPathAction].
 *
 * Renaming the option on any one side alone puts it straight back to "Nothing interesting
 * happens.", with a green build.
 */
class FollowVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(java.nio.file.Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** The slot `OSRSPlugin` sends Follow on. Only used to find the call, not to assert on. */
        private const val LOGIN_SOURCE = "src/main/kotlin/org/alter/plugins/content/OSRSPlugin.kt"
    }

    /**
     * The binding exists. This is the whole of the original bug: the option was live on the client
     * and dead on the server.
     */
    @Test
    fun `Follow binds a player option handler`() {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        val repo = PluginRepository(world)
        FollowPlugin(repo, world, Server())

        val field = PluginRepository::class.java.getDeclaredField("playerOptionPlugins")
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val bindings = field.get(repo) as HashMap<String, Any>

        assertTrue(
            bindings.containsKey(Follow.OPTION),
            "Nothing is bound to the \"${Follow.OPTION}\" player option, so clicking it writes " +
                "\"Nothing interesting happens.\" Bound options: ${bindings.keys.sorted()}",
        )
    }

    /**
     * The name the client is given is the name the handler is bound under.
     *
     * `executePlayerOption` looks its binding up by exact string, so these two agreeing is not
     * cosmetic - it is the lookup.
     */
    @Test
    fun `the option sent at login is the option that is bound`() {
        val source = File(LOGIN_SOURCE)
        assertTrue(source.isFile, "${source.absolutePath} is missing; this test cannot see the source tree.")

        val sent =
            Regex("""sendOption\(\s*"([^"]+)"\s*,\s*(\d+)""")
                .findAll(source.readText())
                .map { it.groupValues[1] }
                .toList()

        assertTrue(
            sent.contains(Follow.OPTION),
            "OSRSPlugin does not send a \"${Follow.OPTION}\" option at login, so the binding is " +
                "unreachable. It sends: $sent",
        )
    }

    /**
     * Follow is dispatched straight to its handler rather than being walked to the target first.
     *
     * This is not a preference. [org.alter.game.model.move.PawnPathAction] calls `resetFacePawn()`
     * on the way out of any option that did not leave combat focus set, and the follow loop reads
     * that same facing as its "still following" signal - so a Follow routed through walkPlugin is
     * cancelled the tick after it starts, and silently: the player walks to the target once and
     * simply stops, which looks like a follow that lost interest rather than a broken one.
     */
    @Test
    fun `Follow is dispatched without being walked to the target first`() {
        val field = OpPlayerHandler::class.java.getDeclaredField("SELF_APPROACHING_OPTIONS")
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val selfApproaching = (field.get(null) as Array<String>).toList()

        assertTrue(
            selfApproaching.contains(Follow.OPTION),
            "\"${Follow.OPTION}\" is not dispatched directly by OpPlayerHandler, so PawnPathAction " +
                "walks it to the target and then clears the facing the follow loop runs on. " +
                "Self-approaching options: $selfApproaching",
        )
    }

    /**
     * Attack has to stay on that same list. It is there for a different reason - a ranged or magic
     * attacker must not be walked into melee before its first shot - and adding Follow beside it
     * is exactly the kind of edit that could drop it.
     */
    @Test
    fun `Attack is still dispatched without being walked to the target first`() {
        val field = OpPlayerHandler::class.java.getDeclaredField("SELF_APPROACHING_OPTIONS")
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val selfApproaching = (field.get(null) as Array<String>).toList()

        assertEquals(
            listOf("Attack", Follow.OPTION).sorted(),
            selfApproaching.sorted(),
            "The set of options that walk themselves changed.",
        )
    }
}
