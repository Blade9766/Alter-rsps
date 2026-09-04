package org.alter.plugins.diag

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dev.openrune.cache.CacheManager
import io.github.classgraph.ClassGraph
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.service.game.NpcMetadataService
import org.alter.plugins.content.combat.WeaponSounds
import org.alter.plugins.content.npcs.animations.MonsterAnimationResolver
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test

/**
 * Which monsters that are actually **placed in the world** end up with no combat sound.
 *
 * The count of silent entries in `named-combat-media.json` is not the interesting number - most of
 * that file describes monsters nobody spawns. This walks the real spawn list instead: every plugin
 * in `content` is constructed exactly as `PluginRepository`'s own scan constructs them, every npc
 * they place is resolved through the same three-source lookup `MonsterAnimationsPlugin` uses, and
 * anything that comes out with no attack sound is reported.
 *
 * It reports rather than asserts, for the reason `BestiaryAnimationAudit` does: some of these are
 * silent because `Sound` genuinely has no clip for them, and a test that failed on those would only
 * ever be suppressed. See `npc-animations/README.md` for which is which.
 *
 * ## The one blind spot, and why it is left in
 *
 * A monster that attacks through its own [org.alter.plugins.content.combat.strategy.CombatStrategy]
 * or an `onNpcCombat` loop never reads `defaultAttackSound` at all - it plays whatever its own code
 * plays - so this audit cannot see that it is audible and reports it as silent. The battle mages,
 * the Elder Chaos druid and the dark wizards are all in that group and all make noise in game.
 *
 * `CombatConfigs.npcStrategies` is private, and the `onNpcCombat` bindings are not enumerable at
 * all, so there is no honest way to subtract them here. Reading the list with that in mind is
 * cheaper than widening the engine's API for a diagnostic.
 */
class SpawnedMonsterSoundAudit {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        private val STATS_CONFIG = File("../data/cfg/npcs/monsterStats.json")
        /**
         * The four roots that contain a `spawnNpc` call, and no more.
         *
         * Scanning all of `content` was the first attempt and it poisoned the rest of the suite:
         * `SpecialAttacks` is a global `object` whose `register` throws on a duplicate, so building
         * the special-attack plugins here left the registry populated and `StrengthVerify` then
         * failed with "Item 1377 already has a special attack bound". `AggroVerify` scopes itself to
         * two of these roots for the same reason.
         */
        private val PACKAGES =
            arrayOf(
                "org.alter.plugins.content.npcs",
                "org.alter.plugins.content.areas",
                "org.alter.plugins.content.quests",
                "org.alter.plugins.content.skills",
            )
    }

    private val mapper = ObjectMapper()

    private val observed: Map<Int, List<Int>> by lazy {
        File("src/main/resources/npc-animations/openosrs-animations.json").inputStream().use {
            mapper.readValue(it, object : TypeReference<Map<String, List<Int>>>() {})
        }.mapKeys { (k, _) -> k.toInt() }
    }

    private val named: Map<String, Map<String, Int>> by lazy {
        File("src/main/resources/npc-animations/named-combat-media.json").inputStream().use {
            mapper.readValue(it, object : TypeReference<Map<String, Map<String, Int>>>() {})
        }
    }

    private val byId: Map<Int, String> by lazy {
        File("src/main/resources/npc-animations/id-combat-media.json").inputStream().use {
            mapper.readValue(it, object : TypeReference<Map<String, String>>() {})
        }.mapKeys { (k, _) -> k.toInt() }
    }

    /** `MonsterAnimationsPlugin.findCombatMedia`, replicated. */
    private fun namedFor(
        id: Int,
        name: String,
    ): Pair<String, Map<String, Int>>? {
        byId[id]?.let { key -> named[key]?.let { return key to it } }
        val normalized = name.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')
        named[normalized]?.let { return normalized to it }
        named.keys.filter { normalized == it || normalized.endsWith("_$it") }.maxByOrNull { it.length }
            ?.let { return it to named.getValue(it) }
        named.keys.filter { normalized.startsWith("${it}_") }.maxByOrNull { it.length }
            ?.let { return it to named.getValue(it) }
        return null
    }

    /** The repository, kept so the audit can ask whether a plugin declared a def for an npc. */
    private lateinit var repository: PluginRepository

    private fun everyPlacedNpcId(): Set<Int> {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        NpcMetadataService().loadMonsterStats(world, STATS_CONFIG)
        val repo = world.plugins
        repository = repo
        val server = Server()
        ClassGraph().enableClassInfo().acceptPackages(*PACKAGES).scan().use { result ->
            result.getSubclasses(KotlinPlugin::class.java.name).forEach { info ->
                try {
                    Class.forName(info.name)
                        .getConstructor(PluginRepository::class.java, World::class.java, Server::class.java)
                        .newInstance(repo, world, server)
                } catch (e: Exception) {
                    // Same contract as the real scan: a plugin that will not build places nothing.
                }
            }
        }
        /*
         * `PluginRepository.npcSpawns` is internal to game-server, so this reads it reflectively -
         * the same latitude `AggroVerify` takes for a private TimerKey. A diagnostic that reports on
         * the real spawn list is worth more than one that reports on a copy of it.
         */
        val field = PluginRepository::class.java.getDeclaredField("npcSpawns").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val spawns = field.get(repo) as List<org.alter.game.model.entity.Npc>
        return spawns.map { it.id }.toSet()
    }

    @Test
    fun `report every spawned monster with no combat sound`() {
        val ids = everyPlacedNpcId()
        val silent = sortedMapOf<String, MutableList<Int>>()
        var audible = 0
        var notAttackable = 0

        ids.forEach { id ->
            val def = CacheManager.getNpc(id) ?: return@forEach
            if (def.actions.filterNotNull().none { it.equals("Attack", ignoreCase = true) }) {
                notAttackable++
                return@forEach
            }
            /*
             * `MonsterAnimationsPlugin` runs for a plugin-declared def too - it just keeps the def's
             * own animations instead of resolving new ones (`replaceFallbackAnimations` is false)
             * and then fills the sounds from the same three sources. So the question is the same for
             * both kinds of npc; only where the attack animation comes from differs.
             */
            val declared = repository.npcCombatDefs[id]
            val hit = namedFor(id, def.name)

            // Source 0, declared defs only: an explicit `sound { }` block. Only Cyclops and Cow.
            if (declared != null && declared.defaultAttackSound > 0) {
                audible++
                return@forEach
            }
            // Source 1: an explicit sound on the name- or id-matched media entry.
            if (hit?.second?.get("attackSound") != null) {
                audible++
                return@forEach
            }
            // Sources 2 and 3 key off whichever attack animation the npc actually ends up with.
            val attackAnim =
                declared?.attackAnimation?.takeIf { it > 0 }
                    ?: hit?.second?.get("attackAnimation")
                    ?: observed[id]?.let { obs ->
                        MonsterAnimationResolver.resolve(def, obs) { CacheManager.getAnims()[it] }?.attack
                    }
            val fromCache = attackAnim?.let { CacheManager.getAnims()[it]?.sounds?.isNotEmpty() } == true
            val fromWeapon = attackAnim?.let { WeaponSounds.forAnimation(it) } != null
            if (fromCache || fromWeapon) {
                audible++
            } else {
                val how = if (declared != null) "declared def" else "media"
                silent.getOrPut("%s (anim %s, %s, entry %s)".format(def.name, attackAnim, how, hit?.first ?: "-")) { mutableListOf() }
                    .add(id)
            }
        }

        println("=".repeat(96))
        println("npc ids placed in the world : ${ids.size}")
        println("  not attackable            : $notAttackable")
        println("  audible in combat         : $audible")
        println("  SILENT                    : ${silent.values.sumOf { it.size }} ids / ${silent.size} rows")
        println("=".repeat(96))
        silent.forEach { (name, list) -> println("  %-58s %s".format(name, list.sorted().joinToString(", "))) }
        println("=".repeat(96))
    }
}
