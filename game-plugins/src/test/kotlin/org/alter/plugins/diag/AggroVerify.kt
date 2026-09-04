package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import io.github.classgraph.ClassGraph
import org.alter.api.NpcCombatBuilder
import org.alter.api.dsl.NpcCombatDsl
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.combat.NpcCombatDef
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the three numbers that decide whether a monster declared as aggressive actually is one.
 *
 * [org.alter.plugins.content.mechanics.aggro.NpcAggroPlugin] reads all three, and two of them fail
 * *silently* and *separately* when left unset:
 *
 *  - `aggroTargetDelay` gates `onGlobalNpcSpawn`, which installs `npc.aggroCheck` and schedules the
 *    sweep timer only when the delay is `> 0`. Below that the npc never looks for a target at all.
 *  - `aggressiveTimer` is what the default check compares: it gives up once
 *    `abs(currentCycle - lastMapBuildTime) > aggressiveTimer`, and no absolute value is ever
 *    `<= -1`, so a negative timer rejects *every* player and the npc sweeps forever without ever
 *    engaging.
 *
 * Neither shows up anywhere at runtime - an aggressive monster that has quietly become a passive
 * one still walks, talks, fights back and dies exactly like the real thing, it just never starts
 * the fight. So the check is worth having in both directions: that the builder supplies a working
 * default for a block that names only a radius, and that no combat def anywhere in the game has
 * ended up in the broken state anyway.
 */
class AggroVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(java.nio.file.Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** The smallest def the builder will accept, so each test only varies its `aggro { }`. */
        private fun defOf(aggro: NpcCombatDsl.AggressivenessBuilder.() -> Unit): NpcCombatDef =
            NpcCombatDsl.Builder().apply {
                configs {
                    attackSpeed = 4
                    respawnDelay = 25
                }
                stats { hitpoints = 10 }
                anims { death = 836 }
                aggro(aggro)
            }.build()

        /**
         * A timer the aggro check can act on: a positive window, "always", or an explicit
         * "never". Anything else - and -1 in particular, which is what an unset timer used to
         * be - makes the check reject every player without saying so.
         */
        private fun usableTimer(timer: Int) =
            timer > 0 || timer == Int.MAX_VALUE || timer == Int.MIN_VALUE

        /** Every `setCombatDef` in the codebase is under one of these two. */
        private val COMBAT_DEF_PACKAGES =
            arrayOf("org.alter.plugins.content.npcs", "org.alter.plugins.content.areas")
    }

    @Test
    fun `an aggro block that names only a radius is still aggressive`() {
        val def = defOf { radius = 4 }

        assertEquals(
            NpcCombatBuilder.DEFAULT_AGGRO_SEARCH_DELAY,
            def.aggroTargetDelay,
            "A radius with no searchDelay must take the default, or the npc never sweeps.",
        )
        assertEquals(
            NpcCombatBuilder.DEFAULT_AGGRO_TIMER,
            def.aggressiveTimer,
            "A radius with no aggroTimer must take the default, or the npc sweeps but never engages.",
        )
        assertTrue(usableTimer(def.aggressiveTimer))
    }

    @Test
    fun `a declared searchDelay and aggroTimer are not overwritten by the defaults`() {
        val def =
            defOf {
                radius = 7
                searchDelay = 2
                aggroTimer = 300
            }

        assertEquals(7, def.aggressiveRadius)
        assertEquals(2, def.aggroTargetDelay)
        assertEquals(300, def.aggressiveTimer)
    }

    @Test
    fun `alwaysAggro and neverAggro survive the defaulting`() {
        assertEquals(Int.MAX_VALUE, defOf { radius = 4; alwaysAggro() }.aggressiveTimer)
        assertEquals(Int.MIN_VALUE, defOf { radius = 4; neverAggro() }.aggressiveTimer)
    }

    /**
     * `aggroMinutes` used to be the expression `aggroTimer * 1000`, which computes a value and
     * throws it away, so the property set the timer to nothing at all.
     */
    @Test
    fun `aggroMinutes converts to cycles`() {
        assertEquals(1000, defOf { radius = 4; aggroMinutes = 10 }.aggressiveTimer)
    }

    /**
     * The content-wide sweep. Every monster package is built the way the server builds it, and
     * every combat def it registers is checked - so a new package that declares a radius and
     * nothing else is caught here rather than by a player walking past it unmolested.
     */
    @Test
    fun `every aggressive npc in the game can sweep and engage`() {
        val defs = allCombatDefs()

        assertTrue(
            defs.size > 100,
            "Only ${defs.size} combat defs loaded - the plugin scan is broken and this test " +
                "would pass no matter what the aggro blocks said.",
        )

        val aggressive = defs.filterValues { it.aggressiveRadius > 0 }
        assertTrue(aggressive.isNotEmpty(), "No aggressive npc loaded at all.")

        val mute =
            aggressive.filterValues { it.aggroTargetDelay <= 0 }
                .map { (id, def) -> "${name(id)} [searchDelay=${def.aggroTargetDelay}]" }
        assertTrue(
            mute.isEmpty(),
            "These npcs declare an aggro radius but no usable searchDelay, so NpcAggroPlugin " +
                "never schedules a sweep for them and they are silently passive: ${mute.sorted()}",
        )

        val passive =
            aggressive.filterValues { !usableTimer(it.aggressiveTimer) }
                .map { (id, def) -> "${name(id)} [aggroTimer=${def.aggressiveTimer}]" }
        assertTrue(
            passive.isEmpty(),
            "These npcs declare an aggro radius but no usable aggroTimer, so the default aggro " +
                "check rejects every player and they never engage: ${passive.sorted()}",
        )
    }

    /**
     * Keeps [COMBAT_DEF_PACKAGES] honest. The sweep above only builds those two roots, so a
     * monster declared somewhere else would not be checked by it and nothing would say so -
     * which is the same class of silent gap this whole file exists to close.
     */
    @Test
    fun `no combat def is declared outside the packages the sweep builds`() {
        val source = File("src/main/kotlin/org/alter/plugins/content")
        assertTrue(source.isDirectory, "${source.absolutePath} is missing; this test cannot see the source tree.")

        val roots = COMBAT_DEF_PACKAGES.map { it.removePrefix("org.alter.plugins.content.") }
        val stray =
            source.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .filter { it.readText().contains("setCombatDef(") }
                .map { it.relativeTo(source).invariantSeparatorsPath }
                .filterNot { path -> roots.any { path.startsWith("$it/") } }
                .toList()

        assertTrue(
            stray.isEmpty(),
            "These files declare a combat def outside the roots AggroVerify builds, so their " +
                "aggro blocks go unchecked - add the package to COMBAT_DEF_PACKAGES: ${stray.sorted()}",
        )
    }

    private fun name(id: Int) = "${CacheManager.getNpcs()[id]?.name ?: "?"} ($id)"

    /**
     * Builds every plugin under the two package roots that register combat defs, rather than
     * naming the monster packages here - a list like that goes stale the moment someone adds one.
     *
     * It is deliberately *not* [PluginRepository]'s own whole-jar scan. Some plugins outside these
     * roots register into process-wide singletons - `SpecialAttacks.attacks` is one - which throw
     * on a second binding, so building the whole jar here would leave those objects populated for
     * every test class that runs after this one in the same JVM and break them instead.
     *
     * A plugin whose constructor throws registers nothing, exactly as it would on a real server;
     * the size floor above is what stops a scan that quietly built nothing from passing.
     */
    private fun allCombatDefs(): Map<Int, NpcCombatDef> {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        val repo = PluginRepository(world)
        val server = Server()

        ClassGraph().enableClassInfo().acceptPackages(*COMBAT_DEF_PACKAGES).scan().use { result ->
            result.getSubclasses(KotlinPlugin::class.java.name).forEach { info ->
                try {
                    Class.forName(info.name)
                        .getConstructor(PluginRepository::class.java, World::class.java, Server::class.java)
                        .newInstance(repo, world, server)
                } catch (e: Exception) {
                    // Same contract as the real scan: a plugin that will not build contributes
                    // no defs, and the floor above catches it if that becomes most of them.
                }
            }
        }

        return repo.npcCombatDefs.entries.associate { it.key.toInt() to it.value }
    }
}
