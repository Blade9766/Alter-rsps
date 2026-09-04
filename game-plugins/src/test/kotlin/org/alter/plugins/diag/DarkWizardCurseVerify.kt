package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.api.Skills
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.darkwizard.DarkWizardCombatPlugin
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The dark wizards' curse gate, which had to grow a second condition when their stat drain was
 * moved onto the landing of the spell rather than the casting of it.
 *
 * The drain used to be applied the instant the wizard cast, which at eight tiles is five cycles -
 * three seconds - before the projectile arrives, and applied it even if the target died or broke
 * off in between. It now rides the hit, the same place the impact sound already did and the same
 * place `MagicCombatStrategy` applies a real spell's `curseEffect`.
 *
 * That fix opens a hole on its own, and this file is mostly about the patch for it. A wizard
 * attacks every 4 cycles; a curse takes up to 5 to land. So the decision to cast the *next* spell
 * happens before the last one has drained anything, reads the target's stats as untouched, and
 * curses again - stacking two drains on a target the wiki says can carry only one. A curse in
 * flight therefore counts as an active drain.
 *
 * **What this does not cover:** [DarkWizardCombatPlugin.castCurse] itself cannot be called here.
 * It reaches `Player.graphic`, which resolves through `playerInfo` - a lateinit that only a real
 * connected client fills in - so a headless Player throws inside it for reasons that have nothing
 * to do with this change. The gate below is the new logic; that the hit's action fires on landing
 * is the engine mechanism `Pawn.hitsCycle` already provides and that `content/npcs/chaosdruid`
 * exercises against a live world.
 */
class DarkWizardCurseVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    private fun world() = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)

    /**
     * The gate is a private member function, so it needs a real plugin instance - which also means
     * the plugin's own `init` has to succeed for any of these tests to run, a small bonus check.
     */
    private val plugin: DarkWizardCombatPlugin by lazy {
        val world = world()
        DarkWizardCombatPlugin(PluginRepository(world), world, Server())
    }

    private fun hasNoActiveStatDrain(target: Player): Boolean =
        DarkWizardCombatPlugin::class.java
            .getDeclaredMethod("hasNoActiveStatDrain", Player::class.java)
            .also { it.isAccessible = true }
            .invoke(plugin, target) as Boolean

    /** `CURSE_IN_FLIGHT` lives in a private companion, which Kotlin emits as a static field. */
    private fun curseInFlightKey(): TimerKey =
        DarkWizardCombatPlugin::class.java
            .getDeclaredField("CURSE_IN_FLIGHT")
            .also { it.isAccessible = true }
            .get(null) as TimerKey

    @Test
    fun `an untouched target can be cursed`() {
        val player = Player(world())
        assertTrue(hasNoActiveStatDrain(player), "a target with no drain and nothing in flight is curseable")
    }

    /**
     * The wiki's wording is plural - "the opponent's stats haven't already been lowered" - so any
     * of the five combat stats being down blocks the curse, not just the one that spell drains.
     */
    @Test
    fun `any lowered combat stat blocks a curse`() {
        val stats = listOf(Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE, Skills.MAGIC, Skills.RANGED)
        stats.forEach { skill ->
            val player = Player(world())
            player.getSkills().alterCurrentLevel(skill, -1)
            assertFalse(hasNoActiveStatDrain(player), "a lowered stat ($skill) must block a curse")
        }
    }

    /**
     * The regression the timing fix would otherwise have caused: at four cycles between attacks and
     * up to five for a curse to arrive, the wizard would choose its next spell while the last was
     * still in the air.
     */
    @Test
    fun `a curse still in flight blocks a second curse`() {
        val player = Player(world())
        val inFlight = curseInFlightKey()

        assertTrue(hasNoActiveStatDrain(player), "precondition: curseable before anything is cast")

        // The longest hit delay a dark wizard can produce, at its full eight-tile attack range.
        player.timers[inFlight] = 5
        assertFalse(hasNoActiveStatDrain(player), "a curse in the air must count as an active drain")
    }

    /**
     * The claim made in `castCurse`'s comment: the marker cannot strand a target permanently
     * un-cursable if the hit never lands, because it counts itself down and is dropped.
     */
    @Test
    fun `the in-flight marker expires on its own`() {
        val player = Player(world())
        val inFlight = curseInFlightKey()
        player.timers[inFlight] = 3

        // One more cycle than the timer's value: it reaches zero, fires, and is removed.
        repeat(5) { player.timerCycle() }

        assertFalse(player.timers.has(inFlight), "the marker must not outlive its countdown")
        assertTrue(hasNoActiveStatDrain(player), "target is curseable again once nothing is in flight")
    }

    /** Transient by construction - a [TimerKey] with no persistence key is never written to a save. */
    @Test
    fun `the in-flight marker is never persisted`() {
        assertNull(curseInFlightKey().persistenceKey, "the in-flight marker must not be saved to the player file")

        val player = Player(world())
        player.timers[curseInFlightKey()] = 5
        assertEquals(0, player.timers.toPersistentTimers().size, "nothing about a curse in flight belongs in a save")
    }
}
