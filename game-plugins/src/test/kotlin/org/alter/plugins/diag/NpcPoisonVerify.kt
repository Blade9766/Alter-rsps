package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.api.NpcCombatBuilder
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.combat.NpcCombatDef
import org.alter.game.model.entity.Npc
import org.alter.game.plugin.PluginRepository
import org.alter.game.Server
import org.alter.plugins.content.mechanics.poison.CombatPoison
import org.alter.plugins.content.npcs.dungeon.DungeonMonsterPlugin
import org.alter.plugins.content.npcs.dungeon.DungeonMonsters
import org.alter.plugins.content.npcs.slayer.SlayerMonsterPlugin
import org.alter.plugins.content.npcs.slayer.SlayerMonsters
import org.alter.plugins.content.npcs.CowPlugin
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Checks on npc-inflicted poison: the two def fields, the builder rule that keeps them honest, and
 * the monsters that carry them.
 *
 * `NpcCombatDef.poisonChance` sat in this codebase for years being set by the DSL and read by
 * nothing, which is the exact failure this file exists to prevent recurring - a poisonous monster
 * that poisons nobody is indistinguishable from one that is simply rolling badly. There are two
 * halves to that: the field has to be *read* (see
 * [org.alter.plugins.content.mechanics.poison.CombatPoison]) and it has to be *set* to something
 * that can work, which is what the builder now refuses to let go wrong.
 */
class NpcPoisonVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        private const val ANY_HITPOINTS = 10
        private const val ANY_ATTACK_SPEED = 4
        private const val ANY_DEATH_ANIMATION = 836
        private const val ANY_RESPAWN_DELAY = 25

        /** The wiki's `poisonous = Yes (N)` for every poisonous monster this codebase defines. */
        private val PUBLISHED_DAMAGE =
            mapOf(
                "Cave crawler" to 8,
                "Poison scorpion" to 3,
                "Poison spider" to 6,
            )
    }

    private fun builder() =
        NpcCombatBuilder()
            .setHitpoints(ANY_HITPOINTS)
            .setAttackSpeed(ANY_ATTACK_SPEED)
            .setDeathAnimation(ANY_DEATH_ANIMATION)
            .setRespawnDelay(ANY_RESPAWN_DELAY)

    private fun npcWith(def: NpcCombatDef): Npc {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        val npc = Npc(id = 1, tile = Tile(3222, 3222), world = world)
        npc.combatDef = def
        return npc
    }

    /**
     * Damage alone is the ordinary way to declare poison, because the wiki gives a damage for every
     * poisonous monster and a rate for none of them.
     */
    @Test
    fun `poison damage alone takes the default chance`() {
        val def = builder().setPoisonDamage(8).build()
        assertEquals(8, def.poisonDamage, "poison damage")
        assertEquals(NpcCombatDef.DEFAULT_POISON_CHANCE, def.poisonChance, "unset chance takes the default")
    }

    @Test
    fun `an explicit chance is kept`() {
        val def = builder().setPoisonDamage(3).setPoisonChance(60.0).build()
        assertEquals(3, def.poisonDamage)
        assertEquals(60.0, def.poisonChance)
    }

    /**
     * The failure mode this whole change exists to close: a chance with no damage looks wired,
     * reads as wired, and poisons nobody. It is now a startup error rather than a silent nothing.
     */
    @Test
    fun `a chance with no damage is refused`() {
        val error = assertFailsWith<IllegalStateException> { builder().setPoisonChance(25.0).build() }
        assertTrue(
            error.message.orEmpty().contains("would never poison"),
            "the error should say why: ${error.message}",
        )
    }

    @Test
    fun `an npc that declares no poison stays harmless`() {
        val def = builder().build()
        assertEquals(0, def.poisonDamage, "no poison damage")
        assertEquals(0.0, def.poisonChance, "no poison chance")
        assertNull(CombatPoison.sourceFor(npcWith(def)), "an ordinary monster does not poison")
    }

    /**
     * The wiki: *"Monsters that can inflict poison may do so regardless of whether or not they
     * inflict any damage, even if protection prayers are used"*. That is what [
     * CombatPoison.Source.appliesOnMiss] carries, and it is the one rule where npc poison and
     * player poison genuinely differ.
     */
    @Test
    fun `an npc poisons from its def and does so even on a miss`() {
        val def = builder().setPoisonDamage(8).build()
        val source = assertNotNull(CombatPoison.sourceFor(npcWith(def)), "a poisonous npc poisons")
        assertEquals(8, source.damage, "initial damage comes from the def")
        assertEquals(NpcCombatDef.DEFAULT_POISON_CHANCE, source.chance, "chance comes from the def")
        assertTrue(source.appliesOnMiss, "npc poison does not need the attack to deal damage")
    }

    /**
     * A def built by hand rather than through the builder could still carry a chance with no
     * damage, so the read side refuses that combination too rather than trusting the builder.
     */
    @Test
    fun `a chance with no damage poisons nothing even if a def carries one`() {
        val def = builder().build().copy(poisonChance = 50.0, poisonDamage = 0)
        assertNull(CombatPoison.sourceFor(npcWith(def)), "a chance with no damage poisons nobody")
    }

    /**
     * The plugins that declare poison, built for real.
     *
     * Two things fail here and nowhere else. The builder now *throws* on a chance with no damage,
     * and a plugin whose constructor throws registers nothing at all - not just the bad npc, the
     * whole file - so a mistake in one monster's block would silently take every monster in it out
     * of the game. And these are the blocks that carry the poison values, so this is where a typo
     * in one would show up.
     */
    private fun registeredDefs(): Map<Int, NpcCombatDef> {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        val repo = PluginRepository(world)
        val server = Server()
        SlayerMonsterPlugin(repo, world, server)
        DungeonMonsterPlugin(repo, world, server)
        CowPlugin(repo, world, server)
        return repo.npcCombatDefs
    }

    @Test
    fun `the poison plugins build and their npcs carry the poison`() {
        val defs = registeredDefs()

        val poisonous =
            mapOf(
                "npc.cave_crawler_406" to 8,
                "npc.poison_scorpion" to 3,
                "npc.poison_spider" to 6,
            )
        poisonous.forEach { (key, damage) ->
            val def = assertNotNull(defs[getRSCM(key)], "$key registered no combat def")
            assertEquals(damage, def.poisonDamage, "$key initial poison damage")
            assertEquals(NpcCombatDef.DEFAULT_POISON_CHANCE, def.poisonChance, "$key poison chance")
        }

        // A cow is the control: it declares poisonChance = 0.0 and must stay harmless.
        val cow = assertNotNull(defs[getRSCM("npc.cow")], "the cow registered no combat def")
        assertEquals(0, cow.poisonDamage, "a cow does not poison")

        val poisoning = defs.values.count { it.poisonDamage > 0 }
        assertTrue(poisoning >= poisonous.size, "only $poisoning registered defs poison")
    }

    /**
     * Every poisonous monster in the game that this codebase actually defines, against the wiki.
     * The cave crawler is the one worth naming: its entry used to read `poisonChance = 1.0`, which
     * was neither its damage (8) nor a chance anything would have used.
     */
    @Test
    fun `the defined poisonous monsters carry their published damage`() {
        val found = HashMap<String, Int>()

        SlayerMonsters.ALL.filter { it.poisonDamage > 0 }.forEach { found[it.name] = it.poisonDamage }
        DungeonMonsters.ALL.filter { it.poisonDamage > 0 }.forEach { found[it.name] = it.poisonDamage }

        assertEquals(PUBLISHED_DAMAGE, found.toSortedMap().toMap(), "poisonous monsters and their initial damage")
    }
}
